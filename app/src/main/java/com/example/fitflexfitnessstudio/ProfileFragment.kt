package com.example.fitflexfitnessstudio

import android.app.ActivityManager
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.fitflexfitnessstudio.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.util.Date
import java.util.concurrent.TimeUnit

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var firestoreListener: ListenerRegistration? = null
    private var isAdmin = false

    private val qrScannerLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents == "FitFlex Fitness Studio") {
            if (isWorkoutServiceRunning()) {
                Toast.makeText(requireContext(), "Workout already in progress", Toast.LENGTH_SHORT).show()
            } else {
                checkMembershipAndStartWorkout()
            }
        } else {
            Toast.makeText(requireContext(), "Invalid QR code", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkMembershipAndStartWorkout() {
        val daysLeft = binding.daysLeftCount.text.toString().toLongOrNull() ?: 0

        if (daysLeft > 0) {
            startWorkoutService()
        } else {
            // For users with 0 days left, create a payment request
            createPaymentRequest()
        }
    }

    private fun createPaymentRequest() {
        val currentUser = auth.currentUser ?: return

        // Show loading state
        showPaymentPendingOverlay(true)

        // Create payment request in Firestore
        val paymentData = hashMapOf(
            "userId" to currentUser.uid,
            "userName" to binding.userName.text.toString(),
            "timestamp" to FieldValue.serverTimestamp(),
            "status" to "pending"
        )

        db.collection("pendingPayments")
            .add(paymentData)
            .addOnSuccessListener {
                // Listen for payment status changes
                setupPaymentStatusListener(it.id)
            }
            .addOnFailureListener {
                showPaymentPendingOverlay(false)
                Toast.makeText(requireContext(), "Failed to create payment request", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupPaymentStatusListener(paymentId: String) {
        db.collection("pendingPayments").document(paymentId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    showPaymentPendingOverlay(false)
                    return@addSnapshotListener
                }

                when (snapshot.getString("status")) {
                    "approved" -> {
                        showPaymentPendingOverlay(false)
                        startWorkoutService()
                    }
                    "rejected" -> {
                        showPaymentPendingOverlay(false)
                        Toast.makeText(requireContext(), "You haven't paid yet", Toast.LENGTH_SHORT).show()
                    }
                    // else still pending - do nothing
                }
            }
    }

    private fun showPaymentPendingOverlay(show: Boolean) {
        if (show) {
            // Show your overlay with "Waiting for admin to confirm payment"
            binding.paymentPendingOverlay.visibility = View.VISIBLE
        } else {
            binding.paymentPendingOverlay.visibility = View.GONE
        }
    }

    companion object {
        fun newInstance(isAdmin: Boolean): ProfileFragment {
            val fragment = ProfileFragment()
            fragment.arguments = Bundle().apply {
                putBoolean("IS_ADMIN", isAdmin)
            }
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            isAdmin = it.getBoolean("IS_ADMIN", false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (auth.currentUser == null) {
            redirectToLogin()
            return
        }

        loadUserData()
        setupAdminFeatures()

        binding.logoutButton.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        binding.qricon.setOnClickListener {
            launchQRScanner()
        }

        binding.editProfile.setOnClickListener {
            showEditProfileDialog()
        }
    }
    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Log Out")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Log Out") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        FirebaseAuth.getInstance().signOut()

        // Clear any ongoing workout service
        val serviceIntent = Intent(requireContext(), WorkoutService::class.java)
        requireContext().stopService(serviceIntent)

        // Redirect to OnBoardingActivity and clear back stack
        val intent = Intent(requireContext(), OnBoardingActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)

        // Finish the current activity if needed
        requireActivity().finish()
    }
    private fun showEditProfileDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_profile, null)
        val etName = dialogView.findViewById<EditText>(R.id.etName)
        val etEmail = dialogView.findViewById<EditText>(R.id.etEmail)
        val etPhone = dialogView.findViewById<EditText>(R.id.etPhone)
        val etAddress = dialogView.findViewById<EditText>(R.id.etAddress)

        etName.setText(binding.userName.text.toString())
        etEmail.setText(binding.emailText.text.toString())
        etPhone.setText(binding.phoneText.text.toString())
        etAddress.setText(binding.addressText.text.toString())

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Profile")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                saveProfileChanges(etName, etEmail, etPhone, etAddress)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveProfileChanges(
        etName: EditText,
        etEmail: EditText,
        etPhone: EditText,
        etAddress: EditText
    ) {
        if (!isAdded || isDetached) return

        if (etName.text.toString().trim().isEmpty()) {
            Toast.makeText(requireContext(), "Name required", Toast.LENGTH_SHORT).show()
            return
        }

        val updates = hashMapOf<String, Any>(
            "fullName" to etName.text.toString(),
            "email" to etEmail.text.toString(),
            "phone" to etPhone.text.toString(),
            "address" to etAddress.text.toString()
        )

        auth.currentUser?.uid?.let { uid ->
            db.collection("users").document(uid)
                .update(updates)
                .addOnSuccessListener {
                    if (isAdded && !isDetached) {
                        binding.userName.text = etName.text.toString()
                        binding.emailText.text = etEmail.text.toString()
                        binding.phoneText.text = etPhone.text.toString()
                        binding.addressText.text = etAddress.text.toString()
                        Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    if (isAdded && !isDetached) {
                        Toast.makeText(requireContext(), "Update failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun updateMembershipUI(daysLeft: Long) {
        if (daysLeft > 0) {
            binding.membershipText.text = "Premium"
            binding.medalIcon.setImageResource(R.drawable.medalgold)
        } else {
            binding.membershipText.text = "Standard"
            binding.medalIcon.setImageResource(R.drawable.medalsilver)
        }
    }

    private fun isWorkoutServiceRunning(): Boolean {
        val manager = requireContext().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Integer.MAX_VALUE)
            .any { it.service.className == WorkoutService::class.java.name }
    }

    private fun startWorkoutService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (!notificationManager.areNotificationsEnabled()) {
                Toast.makeText(
                    requireContext(),
                    "Please enable notifications to track workouts",
                    Toast.LENGTH_LONG
                ).show()
                return
            }
        }

        val serviceIntent = Intent(requireContext(), WorkoutService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requireContext().startForegroundService(serviceIntent)
            } else {
                requireContext().startService(serviceIntent)
            }
            Toast.makeText(requireContext(), "Workout started!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Failed to start workout tracking: ${e.localizedMessage}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun launchQRScanner() {
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt("Scan FitFlex QR Code")
            setCameraId(0)
            setBeepEnabled(true)
            setOrientationLocked(true)
        }
        qrScannerLauncher.launch(options)
    }

    private fun redirectToLogin() {
        startActivity(Intent(requireContext(), LoginActivity::class.java))
        requireActivity().finish()
    }

    private fun setupAdminFeatures() {
        if (isAdmin) {
            // Show admin-specific UI elements if needed
        }
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser ?: return

        if (!isAdded || isDetached) return

        firestoreListener?.remove()

        firestoreListener = db.collection("users").document(currentUser.uid)
            .addSnapshotListener { document, error ->
                if (!isAdded || isDetached) return@addSnapshotListener

                when {
                    error != null -> {
                        Toast.makeText(requireContext(), "Error loading profile", Toast.LENGTH_SHORT).show()
                    }
                    document != null && document.exists() -> {
                        binding.userName.text = document.getString("fullName") ?: "N/A"
                        binding.userHandle.text = document.getString("username") ?: "N/A"
                        binding.emailText.text = document.getString("email") ?: "N/A"
                        binding.phoneText.text = document.getString("phone") ?: "N/A"
                        binding.addressText.text = document.getString("address") ?: "N/A"

                        // Get the most recent membership end date from subcollection
                        db.collection("users").document(currentUser.uid)
                            .collection("memberships")
                            .orderBy("endDate", Query.Direction.DESCENDING)
                            .limit(1)
                            .get()
                            .addOnSuccessListener { querySnapshot ->
                                val daysLeft = if (!querySnapshot.isEmpty) {
                                    val latestMembership = querySnapshot.documents[0]
                                    val endDate = latestMembership.getDate("endDate")
                                    if (endDate != null) {
                                        val now = Date()
                                        if (endDate.after(now)) {
                                            TimeUnit.MILLISECONDS.toDays(endDate.time - now.time)
                                        } else {
                                            0L
                                        }
                                    } else {
                                        0L
                                    }
                                } else {
                                    0L
                                }

                                binding.daysLeftCount.text = daysLeft.toString()
                                updateMembershipUI(daysLeft)
                            }
                            .addOnFailureListener { e ->
                                binding.daysLeftCount.text = "0"
                                updateMembershipUI(0L)
                            }

                        binding.streakText.text = "${document.getLong("streak") ?: 0} Days Streak"
                    }
                    else -> createInitialUserDocument(currentUser)
                }
            }
    }
    private fun createInitialUserDocument(user: com.google.firebase.auth.FirebaseUser) {
        val userData = hashMapOf(
            "fullName" to (user.displayName ?: "New User"),
            "email" to user.email,
            "username" to user.email,
            "phone" to "",
            "address" to "",
            "streak" to 0,
            "daysLeft" to 0,
            "profileImageUrl" to "https://cdn-icons-png.flaticon.com/512/219/219986.png",
            "isAdmin" to false
        )

        db.collection("users").document(user.uid)
            .set(userData)
            .addOnSuccessListener {
                if (isAdded && !isDetached) {
                    loadUserData()
                }
            }
            .addOnFailureListener { e ->
                if (isAdded && !isDetached) {
                    Toast.makeText(requireContext(), "Error creating profile", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        firestoreListener?.remove()
        _binding = null
    }
}