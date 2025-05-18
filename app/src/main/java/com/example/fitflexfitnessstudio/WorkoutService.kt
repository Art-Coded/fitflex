package com.example.fitflexfitnessstudio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class WorkoutService : Service() {
    private var startTimeElapsed: Long = 0
    private var startTimeWallClock: Long = 0
    private var isRunning = false
    private val executor = Executors.newSingleThreadScheduledExecutor()
    private var notificationManager: NotificationManager? = null

    companion object {
        const val CHANNEL_ID = "WorkoutChannel"
        const val NOTIFICATION_ID = 1
        const val ACTION_STOP = "STOP_WORKOUT"
    }

    inner class LocalBinder : Binder() {
        fun getService(): WorkoutService = this@WorkoutService
    }

    private val binder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action) {
            ACTION_STOP -> {
                stopWorkout()
                stopSelf()
            }
            else -> startWorkout()
        }
        return START_NOT_STICKY
    }

    private fun startWorkout() {
        if (isRunning) return

        startTimeElapsed = SystemClock.elapsedRealtime()
        startTimeWallClock = System.currentTimeMillis()
        isRunning = true

        executor.scheduleAtFixedRate({
            updateNotification()
        }, 0, 1, TimeUnit.SECONDS)
    }

    private fun updateNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!notificationManager!!.areNotificationsEnabled()) {
                stopWorkout()
                return
            }
        }

        val elapsedTime = SystemClock.elapsedRealtime() - startTimeElapsed
        val hours = TimeUnit.MILLISECONDS.toHours(elapsedTime)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedTime) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(elapsedTime) % 60

        val stopIntent = Intent(this, WorkoutService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Working out...")
            .setContentText(String.format("%02d:%02d:%02d", hours, minutes, seconds))
            .setSmallIcon(R.drawable.ic_workout)
            .addAction(
                R.drawable.ic_stop,
                "Stop Workout",
                stopPendingIntent
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

        try {
            startForeground(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            stopWorkout()
        }
    }

    private fun stopWorkout() {
        if (!isRunning) return

        executor.shutdown()
        val endTimeElapsed = SystemClock.elapsedRealtime()
        val duration = (endTimeElapsed - startTimeElapsed) / 1000 // in seconds

        saveWorkoutSession(duration)
        isRunning = false

        stopForeground(true)
        stopSelf()

        sendBroadcast(Intent("WORKOUT_STOPPED").apply {
            putExtra("duration", duration)
        })
    }private fun saveWorkoutSession(duration: Long) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()
        val currentTime = System.currentTimeMillis()

        // Save to users collection (existing logic)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateId = dateFormat.format(Date()) // Date when workout was stopped

        val usersWorkoutData = hashMapOf(
            "startTime" to startTimeElapsed,
            "duration" to duration,
            "timestamp" to currentTime
        )

        db.collection("users")
            .document(currentUser.uid)
            .collection("workouts")
            .document(dateId)
            .collection("sessions")
            .add(usersWorkoutData)
            .addOnSuccessListener {
                // First update the workout stats
                db.collection("users")
                    .document(currentUser.uid)
                    .update(
                        "totalDuration", FieldValue.increment(duration),
                        "totalWorkouts", FieldValue.increment(1),
                        "last_workout_timestamp", currentTime // Add this field
                    )
                    .addOnSuccessListener {
                        // Now check and update streak
                        updateStreak(currentUser.uid, currentTime)
                    }

                // Save to attendance collection (existing logic)
                db.collection("users")
                    .document(currentUser.uid)
                    .get()
                    .addOnSuccessListener { userDoc ->
                        val displayName = userDoc.getString("fullName") ?: "Unknown"

                        // Format date and time based on workout start
                        val attendanceDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val attendanceDateId = attendanceDateFormat.format(Date(startTimeWallClock))

                        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                        val startTimeStr = timeFormat.format(Date(startTimeWallClock))

                        val attendanceData = hashMapOf(
                            "fullName" to displayName,
                            "startTime" to startTimeStr,
                            "duration" to duration,
                            "timestamp" to startTimeWallClock
                        )

                        db.collection("attendance")
                            .document(attendanceDateId)
                            .collection("sessions")
                            .add(attendanceData)
                    }
            }
    }

    private fun updateStreak(userId: String, currentWorkoutTime: Long) {
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(userId)

        userRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val lastWorkoutTimestamp = document.getLong("last_workout_timestamp") ?: 0L
                val currentStreak = document.getLong("streak") ?: 0L

                val calendar = Calendar.getInstance()

                // Get yesterday's date
                calendar.timeInMillis = currentWorkoutTime
                calendar.add(Calendar.DATE, -1)
                val yesterday = calendar.timeInMillis

                // Check if last workout was yesterday or today
                val isConsecutiveDay = isSameDay(lastWorkoutTimestamp, currentWorkoutTime) ||
                        isSameDay(lastWorkoutTimestamp, yesterday)

                val newStreak = if (isConsecutiveDay) {
                    // If last workout was yesterday or today, increment streak
                    currentStreak + 1
                } else {
                    // Otherwise reset to 1 (current workout)
                    1L
                }

                // Update streak in Firestore
                userRef.update("streak", newStreak)
                    .addOnSuccessListener {
                        // Optional: Check if this is a new milestone (e.g., 7, 30, 100 days)
                        checkStreakMilestone(newStreak)
                    }
            }
        }
    }

    private fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
        val calendar1 = Calendar.getInstance()
        calendar1.timeInMillis = timestamp1

        val calendar2 = Calendar.getInstance()
        calendar2.timeInMillis = timestamp2

        return calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR) &&
                calendar1.get(Calendar.DAY_OF_YEAR) == calendar2.get(Calendar.DAY_OF_YEAR)
    }

    private fun checkStreakMilestone(streak: Long) {
        // You can implement notifications or rewards for streak milestones
        when (streak) {
            3L -> showStreakNotification("3-day streak! Keep going!")
            7L -> showStreakNotification("1-week streak! Amazing!")
            30L -> showStreakNotification("1-month streak! You're crushing it!")
            // Add more milestones as needed
        }
    }

    private fun showStreakNotification(message: String) {
        // Implement notification to celebrate streak milestones
        // You can use the same notification channel as your workout notifications
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Workout Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows current workout duration"
                setShowBadge(false)
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }
}