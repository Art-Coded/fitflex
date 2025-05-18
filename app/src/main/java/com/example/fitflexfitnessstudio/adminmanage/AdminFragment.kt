package com.example.fitflexfitnessstudio.adminmanage

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.fitflexfitnessstudio.R
import com.example.fitflexfitnessstudio.OnBoardingActivity
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class AdminFragment : Fragment() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up ViewPager and Tabs
        val tabLayout: TabLayout = view.findViewById(R.id.tabLayout)
        val viewPager: ViewPager2 = view.findViewById(R.id.viewPager)

        val adapter = AdminManageAdapter(this)
        viewPager.adapter = adapter
        viewPager.isUserInputEnabled = false

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Members"
                1 -> "Attendance"
                2 -> "Notifs"
                else -> "Tab $position"
            }
        }.attach()

        // Set up logout button
        view.findViewById<View>(R.id.btnScan).setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Log Out")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Yes") { _, _ ->
                signOutAndNavigate()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun signOutAndNavigate() {
        auth.signOut() // Firebase sign out
        clearSessionData() // Clear any additional session data
        navigateToOnBoarding()
    }

    private fun clearSessionData() {
        // Clear any shared preferences or other session data here
        // Example:
        // val sharedPref = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        // sharedPref.edit().clear().apply()
    }

    private fun navigateToOnBoarding() {
        val intent = Intent(requireActivity(), OnBoardingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }

    companion object {
        fun newInstance() = AdminFragment()
    }
}