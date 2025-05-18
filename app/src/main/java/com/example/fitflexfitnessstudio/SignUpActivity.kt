package com.example.fitflexfitnessstudio

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.fitflexfitnessstudio.admin.AdminActivity
import com.example.fitflexfitnessstudio.databinding.ActivitySignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val ADMIN_SECRET_CODE = "fitnessfit"
    private val DEFAULT_PROFILE_IMAGE = "https://cdn-icons-png.flaticon.com/512/219/219986.png"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        binding.btnRegister.setOnClickListener {
            registerUser()
        }

        binding.tvLogin.setOnClickListener {
            finish()
        }
    }

    private fun registerUser() {
        val email = binding.email.text.toString().trim()
        val password = binding.password.text.toString().trim()
        val confirmPassword = binding.confirmPassword.text.toString().trim()
        val fullName = binding.fullname.text.toString().trim()
        val adminCode = binding.adminCode.text.toString().trim()

        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || fullName.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords don't match", Toast.LENGTH_SHORT).show()
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val isAdmin = adminCode == ADMIN_SECRET_CODE

                    if (isAdmin) {
                        setAdminCustomClaims(user)
                    } else {
                        saveUserData(user, fullName, email, false)
                    }
                } else {
                    if (adminCode.isNotEmpty() && adminCode != ADMIN_SECRET_CODE) {
                        showWrongCodeDialog()
                    } else {
                        Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    private fun setAdminCustomClaims(user: FirebaseUser?) {
        user?.getIdToken(true)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                saveUserData(user, binding.fullname.text.toString().trim(),
                    binding.email.text.toString().trim(), true)
            } else {
                Toast.makeText(this, "Error verifying user: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveUserData(user: FirebaseUser?, fullName: String, email: String, isAdmin: Boolean) {
        val userData = hashMapOf(
            "fullName" to fullName,
            "email" to email,
            "isAdmin" to isAdmin,
            "profileImageUrl" to DEFAULT_PROFILE_IMAGE,
            "totalWorkouts" to 0,
            "totalDuration" to 0,
            "membershipType" to "Standard", // Set default membership type
            "streak" to 0, // Initialize streak
            "daysLeft" to 0 // Initialize days left
        )

        db.collection("users").document(user!!.uid)
            .set(userData)
            .addOnSuccessListener {
                if (isAdmin) {
                    Toast.makeText(this, "Admin account created successfully!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, AdminActivity::class.java))
                } else {
                    Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                }
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error saving user data: ${e.message}", Toast.LENGTH_SHORT).show()
                user.delete()
            }
    }
    private fun showWrongCodeDialog() {
        AlertDialog.Builder(this)
            .setTitle("Wrong Admin Code")
            .setMessage("The secret code to become admin is wrong. Continue and sign up as a normal member?")
            .setPositiveButton("Continue") { dialog, _ ->
                binding.adminCode.setText("")
                registerUser()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}