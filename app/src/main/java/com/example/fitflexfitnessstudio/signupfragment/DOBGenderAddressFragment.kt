package com.example.fitflexfitnessstudio.signupfragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.fitflexfitnessstudio.MainActivity
import com.example.fitflexfitnessstudio.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class DOBGenderAddressFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_d_o_b_gender_address, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find views
        val otpInput = view.findViewById<TextInputEditText>(R.id.otp_input)
        val resendOtp = view.findViewById<TextView>(R.id.resend_otp)
        val verifyButton = view.findViewById<MaterialButton>(R.id.btn_verify)

        // Handle Resend OTP click
        resendOtp.setOnClickListener {
            Toast.makeText(requireContext(), "Resending OTP...", Toast.LENGTH_SHORT).show()
            // Add logic to resend OTP
        }

        // Handle Verify OTP click
        verifyButton.setOnClickListener {
            val otp = otpInput.text.toString()

            // Validate OTP
            if (otp.length == 6) {
                // Simulate OTP verification (replace with your actual verification logic)
                if (isOtpValid(otp)) {
                    // OTP is valid, navigate to MainActivity
                    navigateToMainActivity()
                } else {
                    Toast.makeText(requireContext(), "Invalid OTP. Please try again.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Please enter a valid 6-digit OTP.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Simulate OTP validation (replace with your actual OTP verification logic).
     */
    private fun isOtpValid(otp: String): Boolean {
        // Replace this with your actual OTP verification logic (e.g., API call)
        return otp == "123456" // Example: Hardcoded OTP for testing
    }

    /**
     * Navigate to MainActivity.
     */
    private fun navigateToMainActivity() {
        val intent = Intent(requireContext(), MainActivity::class.java)
        startActivity(intent)
        requireActivity().finish() // Optional: Close the current activity
    }
}