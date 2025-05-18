package com.example.fitflexfitnessstudio

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.*

class PlusFragment : Fragment() {

    private lateinit var tvSelectedDate: TextView
    private lateinit var etExerciseNote: EditText
    private lateinit var btnSaveNote: Button

    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault()) // For day name (e.g., Monday)
    private val monthFormat = SimpleDateFormat("MMM", Locale.getDefault()) // For month name (e.g., Feb)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_plus, container, false)

        // Initialize views
        tvSelectedDate = view.findViewById(R.id.tvSelectedDate)
        etExerciseNote = view.findViewById(R.id.etExerciseNote)
        btnSaveNote = view.findViewById(R.id.btnSaveNote)

        // Set initial date
        updateSelectedDate()

        // Date Text Click Listener
        tvSelectedDate.setOnClickListener {
            showDatePicker()
        }

        // Save Note Button Click
        btnSaveNote.setOnClickListener {
            saveNote()
        }

        return view
    }

    private fun showDatePicker() {
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateSelectedDate()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun updateSelectedDate() {
        val amPm = if (calendar.get(Calendar.HOUR_OF_DAY) < 12) "AM" else "PM"
        val month = monthFormat.format(calendar.time)
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val dayOfWeek = dayFormat.format(calendar.time)

        val formattedDate = "$month $dayOfMonth\n$dayOfWeek"
        tvSelectedDate.text = formattedDate
    }

    private fun saveNote() {
        val note = etExerciseNote.text.toString().trim()
        if (note.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a note", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedDate = dateFormat.format(calendar.time)
        val noteWithDate = "$selectedDate: $note"

        // Save the note (you can use SharedPreferences, Room Database, etc.)
        // For now, just display a toast
        Toast.makeText(requireContext(), "Note Saved: $noteWithDate", Toast.LENGTH_LONG).show()

        // Clear the EditText
        etExerciseNote.text.clear()
    }
}