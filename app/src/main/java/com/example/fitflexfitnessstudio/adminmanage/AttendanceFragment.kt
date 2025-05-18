package com.example.fitflexfitnessstudio.adminmanage

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitflexfitnessstudio.R
import com.example.fitflexfitnessstudio.databinding.FragmentAttendanceBinding
import com.example.fitflexfitnessstudio.databinding.ItemCheckInBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AttendanceFragment : Fragment() {

    private var _binding: FragmentAttendanceBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private var selectedDate = Calendar.getInstance()

    // Date formatters
    private val displayDateFormat = SimpleDateFormat("EEE, MMM d yyyy", Locale.getDefault())
    private val firestoreDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAttendanceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupDateNavigation()
        loadAttendanceForDate(selectedDate.time)
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = AttendanceAdapter(emptyList())
        }
    }

    private fun setupDateNavigation() {
        updateDateDisplay()

        binding.prevDateButton.setOnClickListener {
            selectedDate.add(Calendar.DATE, -1)
            updateDateDisplay()
            loadAttendanceForDate(selectedDate.time)
        }

        binding.nextDateButton.setOnClickListener {
            selectedDate.add(Calendar.DATE, 1)
            updateDateDisplay()
            loadAttendanceForDate(selectedDate.time)
        }

        binding.dateTextView.setOnClickListener {
            showDatePicker()
        }
    }

    private fun updateDateDisplay() {
        binding.dateTextView.text = displayDateFormat.format(selectedDate.time)
    }

    private fun showDatePicker() {
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                selectedDate.set(year, month, day)
                updateDateDisplay()
                loadAttendanceForDate(selectedDate.time)
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun loadAttendanceForDate(date: Date) {
        val dateId = firestoreDateFormat.format(date)
        binding.progressBar.visibility = View.VISIBLE

        db.collection("attendance")
            .document(dateId)
            .collection("sessions")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val attendanceList = snapshot.documents.mapNotNull { doc ->
                    AttendanceSession(
                        fullName = doc.getString("fullName") ?: "Unknown User",
                        startTime = doc.getString("startTime") ?: "N/A"
                    )
                }
                updateAttendanceList(attendanceList)
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(context, "Failed to load attendance", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateAttendanceList(sessions: List<AttendanceSession>) {
        binding.progressBar.visibility = View.GONE
        (binding.recyclerView.adapter as AttendanceAdapter).updateData(sessions)
        binding.emptyState.visibility = if (sessions.isEmpty()) View.VISIBLE else View.GONE
    }

    data class AttendanceSession(
        val fullName: String,
        val startTime: String
    )

    inner class AttendanceAdapter(private var items: List<AttendanceSession>) :
        RecyclerView.Adapter<AttendanceAdapter.ViewHolder>() {

        inner class ViewHolder(val binding: ItemCheckInBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(
                ItemCheckInBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.binding.apply {
                nameTextView.text = item.fullName
                timeTextView.text = item.startTime
                durationTextView.visibility = View.GONE
            }
        }

        override fun getItemCount() = items.size

        fun updateData(newItems: List<AttendanceSession>) {
            items = newItems
            notifyDataSetChanged()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}