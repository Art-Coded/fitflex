package com.example.fitflexfitnessstudio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.fragment.app.Fragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*

class NameFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_name, container, false)

        // Birthday Field
        val birthdayInput = view.findViewById<TextInputEditText>(R.id.birthday)
        birthdayInput.setOnClickListener {
            showDatePicker(birthdayInput)
        }

        // Gender Drop-Down
        val genderInput = view.findViewById<AutoCompleteTextView>(R.id.gender)
        val genders = listOf("Male", "Female", "Other")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, genders)
        genderInput.setAdapter(adapter)

        return view
    }

    private fun showDatePicker(birthdayInput: TextInputEditText) {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Birthday")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        datePicker.addOnPositiveButtonClickListener { selectedDate ->
            val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
            val date = dateFormat.format(Date(selectedDate))
            birthdayInput.setText(date)
        }

        datePicker.show(parentFragmentManager, "DATE_PICKER")
    }
}