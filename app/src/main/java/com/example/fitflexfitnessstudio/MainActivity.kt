package com.example.fitflexfitnessstudio

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.work.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private var isAdmin = false
    private val auth = FirebaseAuth.getInstance()
    private lateinit var notificationManager: NotificationManager

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
        private const val STREAK_CHANNEL_ID = "streak_channel"
        private const val STREAK_WARNING_HOURS = 18 // Warn user at 18 hours
        private const val STREAK_RESET_HOURS = 24   // Reset streak at 24 hours

        fun getStreakWarningHours() = STREAK_WARNING_HOURS
        fun getStreakResetHours() = STREAK_RESET_HOURS
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize notification system
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()

        // Request notification permission
        requestNotificationPermission()

        // Check authentication
        if (auth.currentUser == null) {
            redirectToLogin()
            return
        }

        // Check admin status
        isAdmin = intent.getBooleanExtra("IS_ADMIN", false) ||
                (auth.currentUser?.getIdToken(false)?.result?.claims?.get("admin") == true)

        // Setup bottom navigation and load initial fragment
        setupBottomNavigation()

        // Start streak monitoring
        scheduleStreakCheck()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                STREAK_CHANNEL_ID,
                "Workout Streak",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications about your workout streak"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Set admin-specific menu items
        if (isAdmin) {
            bottomNav.menu.findItem(R.id.nav_ads).title = "Promos"
            bottomNav.menu.findItem(R.id.nav_notifications).title = "Admin"
        }

        // Set navigation listener
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> replaceFragment(HomeFragment())
                R.id.nav_ads -> replaceFragment( AdsFragment())
                R.id.nav_notifications -> replaceFragment(NotificationsFragment())
                R.id.nav_profile -> replaceFragment(ProfileFragment.newInstance(isAdmin))
            }
            true
        }

        // Set Home as default selected item and load fragment
        bottomNav.selectedItemId = R.id.nav_home
        replaceFragment(HomeFragment())
    }

    private fun scheduleStreakCheck() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val streakCheckRequest = PeriodicWorkRequestBuilder<StreakCheckWorker>(
            6, TimeUnit.HOURS, // Check every 6 hours
            15, TimeUnit.MINUTES // Flexible window
        ).setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "streakCheckWork",
            ExistingPeriodicWorkPolicy.KEEP,
            streakCheckRequest
        )
    }

    fun showStreakNotification(title: String, message: String) {
        val notification = NotificationCompat.Builder(this, STREAK_CHANNEL_ID)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                }
                ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) -> {
                    Toast.makeText(
                        this,
                        "Notifications help track your workout streak",
                        Toast.LENGTH_LONG
                    ).show()
                    requestPermission()
                }
                else -> {
                    requestPermission()
                }
            }
        }
    }

    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun redirectToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(
                        this,
                        "Streak notifications disabled",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}