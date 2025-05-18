package com.example.fitflexfitnessstudio

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class StreakCheckWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override suspend fun doWork(): Result {
        val userId = auth.currentUser?.uid ?: return Result.success()

        try {
            val document = firestore.collection("users").document(userId).get().await()
            if (document.exists()) {
                val lastWorkout = document.getLong("last_workout_timestamp") ?: 0L
                val currentStreak = document.getLong("streak") ?: 0L
                val currentTime = System.currentTimeMillis()

                val hoursSinceLastWorkout = (currentTime - lastWorkout) / (1000 * 60 * 60)
                val warningThreshold = MainActivity.getStreakWarningHours()
                val resetThreshold = MainActivity.getStreakResetHours()

                when {
                    hoursSinceLastWorkout >= resetThreshold && currentStreak > 0 -> {
                        resetStreak(userId, currentStreak)
                    }
                    hoursSinceLastWorkout >= warningThreshold && currentStreak > 0 -> {
                        sendWarningNotification(currentStreak, hoursSinceLastWorkout, resetThreshold)
                    }
                }
            }
            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }

    private suspend fun resetStreak(userId: String, currentStreak: Long) {
        firestore.collection("users").document(userId)
            .update("streak", 0)
            .await()

        (applicationContext as MainActivity).showStreakNotification(
            "Streak Ended",
            "Your $currentStreak-day streak has ended. Start a new one today!"
        )
    }

    private fun sendWarningNotification(currentStreak: Long, hoursPassed: Long, resetThreshold: Int) {
        val hoursLeft = resetThreshold - hoursPassed
        (applicationContext as MainActivity).showStreakNotification(
            "Streak About to End!",
            "You have $hoursLeft hours left to maintain your $currentStreak-day streak!"
        )
    }
}