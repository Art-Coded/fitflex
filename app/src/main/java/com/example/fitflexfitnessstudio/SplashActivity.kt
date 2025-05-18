package com.example.fitflexfitnessstudio

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.fitflexfitnessstudio.admin.AdminActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SplashActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val splashDelay: Long = 5000 // Reduced to 2 seconds for better UX

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        // Initialize Firebase components
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        Handler(Looper.getMainLooper()).postDelayed({
            checkUserStatus()
        }, splashDelay)
    }

    private fun checkUserStatus() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // User not logged in, go to onboarding
            startActivity(Intent(this, OnBoardingActivity::class.java))
            finish()
        } else {
            // User is logged in, check if admin
            checkAdminStatus(currentUser.uid)
        }
    }

    private fun checkAdminStatus(userId: String) {
        // First check custom claims (more secure)
        auth.currentUser?.getIdToken(true)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val isAdmin = task.result?.claims?.get("admin") == true ||
                        task.result?.claims?.get("isAdmin") == true

                if (isAdmin) {
                    startActivity(Intent(this, AdminActivity::class.java))
                } else {
                    // If no custom claims, check Firestore as fallback
                    checkFirestoreAdminStatus(userId)
                }
            } else {
                // If token refresh fails, check Firestore
                checkFirestoreAdminStatus(userId)
            }
        }
    }

    private fun checkFirestoreAdminStatus(userId: String) {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val isAdmin = document.getBoolean("isAdmin") ?: false
                val destination = if (isAdmin) AdminActivity::class.java else MainActivity::class.java
                startActivity(Intent(this, destination))

                finish()
            }
            .addOnFailureListener {
                // Default to MainActivity if there's an error
                startActivity(Intent(this, MainActivity::class.java))

                finish()
            }
    }
}