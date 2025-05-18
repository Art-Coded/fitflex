package com.example.fitflexfitnessstudio

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.fitflexfitnessstudio.admin.AdminActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Regular login button
        findViewById<MaterialButton>(R.id.btn_login).setOnClickListener {
            val email = findViewById<TextInputEditText>(R.id.email).text.toString().trim()
            val password = findViewById<TextInputEditText>(R.id.password).text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginUser(email, password)
        }

        // Sign up text
        findViewById<TextView>(R.id.tv_signup).setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }


    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Check if user is admin
                    checkAdminStatus(auth.currentUser?.uid)
                } else {
                    Toast.makeText(
                        this,
                        "Authentication failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun checkAdminStatus(userId: String?) {
        if (userId == null) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
            return
        }

        // First check Firebase Auth custom claims (more secure)
        auth.currentUser?.getIdToken(true)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val isAdmin = task.result?.claims?.get("admin") == true ||
                        task.result?.claims?.get("isAdmin") == true

                if (isAdmin) {
                    redirectToAdmin()
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
                if (document.exists()) {
                    val isAdmin = document.getBoolean("isAdmin") ?: false
                    if (isAdmin) {
                        redirectToAdmin()
                    } else {
                        redirectToMain()
                    }
                } else {
                    // User document doesn't exist
                    redirectToMain()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error checking user status: ${e.message}", Toast.LENGTH_SHORT).show()
                redirectToMain()
            }
    }

    private fun redirectToAdmin() {
        startActivity(Intent(this, AdminActivity::class.java))
        finish()
    }

    private fun redirectToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }


}