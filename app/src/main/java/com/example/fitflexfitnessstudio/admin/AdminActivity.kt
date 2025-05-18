package com.example.fitflexfitnessstudio.admin

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.fitflexfitnessstudio.R
import com.example.fitflexfitnessstudio.adminmanage.AdminFragment
import com.example.fitflexfitnessstudio.databinding.ActivityAdminBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup bottom navigation
        setupBottomNavigation()

        // Load default fragment
        if (savedInstanceState == null) {
            replaceFragment(AdminDashboardFragment())
        }

        // Start listening for payment approvals
        setupPaymentApprovalListener()
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_admin_home -> replaceFragment(AdminDashboardFragment())
                R.id.nav_admin_promos -> replaceFragment(AdminPromosFragment())
                R.id.nav_admin_manage -> replaceFragment(AdminFragment())
            }
            true
        }
    }

    private fun setupPaymentApprovalListener() {
        db.collection("pendingPayments")
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null) return@addSnapshotListener

                for (change in snapshots.documentChanges) {
                    if (change.type == DocumentChange.Type.ADDED) {
                        showPaymentApprovalDialog(change.document)
                    }
                }
            }
    }

    private fun showPaymentApprovalDialog(paymentDocument: DocumentSnapshot) {
        val userName = paymentDocument.getString("userName") ?: "Unknown User"

        AlertDialog.Builder(this)
            .setTitle("Payment Approval")
            .setMessage("Has $userName paid the walk-in payment?")
            .setPositiveButton("Approve") { _, _ ->
                updatePaymentStatus(paymentDocument.id, "approved")
            }
            .setNegativeButton("Reject") { _, _ ->
                updatePaymentStatus(paymentDocument.id, "rejected")
            }
            .setCancelable(false)
            .show()
    }

    private fun updatePaymentStatus(paymentId: String, status: String) {
        val currentAdmin = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("pendingPayments").document(paymentId)
            .update(
                mapOf(
                    "status" to status,
                    "adminId" to currentAdmin
                )
            )
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
