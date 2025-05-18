package com.example.fitflexfitnessstudio.admin

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitflexfitnessstudio.R
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.*

class AdminClassFragment : Fragment() {

    private lateinit var classesRecyclerView: RecyclerView
    private lateinit var adapter: ClassAdapter
    private lateinit var addClassButton: MaterialButton
    private var firestoreListener: ListenerRegistration? = null
    private val db = FirebaseFirestore.getInstance()

    private val availableSpecialties = listOf(
        "Yoga", "Weightlifting", "Cardio", "Pilates",
        "CrossFit", "Zumba", "Boxing", "Swimming", "HIIT"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_admin_class, container, false)

        classesRecyclerView = view.findViewById(R.id.classesRecyclerView)
        addClassButton = view.findViewById(R.id.btnAddClass)

        setupRecyclerView()
        setupAddClassButton()
        loadClassesFromFirestore()

        return view
    }

    private fun setupRecyclerView() {
        classesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = ClassAdapter(emptyList(),
            onViewClick = { gymClass -> showBookingsDialog(gymClass) },
            onDeleteClick = { gymClass -> showDeleteConfirmationDialog(gymClass) }
        )
        classesRecyclerView.adapter = adapter
    }

    private fun showDeleteConfirmationDialog(gymClass: GymClass) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Class")
            .setMessage("Are you sure you want to delete \"${gymClass.name}\" class?")
            .setPositiveButton("Delete") { _, _ ->
                deleteClass(gymClass)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteClass(gymClass: GymClass) {
        db.collection("classes").document(gymClass.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Class deleted successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error deleting class: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupAddClassButton() {
        addClassButton.setOnClickListener {
            showAddClassDialog()
        }
    }

    private fun loadClassesFromFirestore() {
        if (!isAdded || isDetached) return

        firestoreListener?.remove()
        firestoreListener = db.collection("classes")
            .addSnapshotListener { snapshot, error ->
                if (!isAdded || isDetached) return@addSnapshotListener

                when {
                    error != null -> {
                        Toast.makeText(
                            requireContext(),
                            "Error loading classes",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    snapshot != null -> {
                        val classes = snapshot.documents.mapNotNull { doc ->
                            GymClass(
                                id = doc.id,
                                name = doc.getString("name") ?: "",
                                schedule = doc.getString("schedule") ?: "",
                                location = doc.getString("location") ?: "",
                                specialty = doc.getString("specialty") ?: "General"
                            )
                        }
                        adapter.updateClasses(classes)
                    }
                }
            }
    }

    private fun showAddClassDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_class, null)

        val etName = dialogView.findViewById<TextInputEditText>(R.id.etClassName)
        val etSchedule = dialogView.findViewById<TextInputEditText>(R.id.etClassSchedule)
        val etLocation = dialogView.findViewById<TextInputEditText>(R.id.etClassLocation)
        val etSpecialty = dialogView.findViewById<TextInputEditText>(R.id.etClassSpecialty)
        val btnDatePicker = dialogView.findViewById<Button>(R.id.btnDatePicker)
        val btnTimePicker = dialogView.findViewById<Button>(R.id.btnTimePicker)

        // Setup specialty selection
        etSpecialty.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select Specialty")
                .setItems(availableSpecialties.toTypedArray()) { _, which ->
                    etSpecialty.setText(availableSpecialties[which])
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Date Picker
        btnDatePicker.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    val selectedDate = Calendar.getInstance()
                    selectedDate.set(year, month, day)
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    val datePart = dateFormat.format(selectedDate.time)

                    // Append or set the date part
                    val currentText = etSchedule.text.toString()
                    if (currentText.contains(" at ")) {
                        val timePart = currentText.substringAfter(" at ")
                        etSchedule.setText("$datePart at $timePart")
                    } else {
                        etSchedule.setText(datePart)
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Time Picker
        btnTimePicker.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(
                requireContext(),
                { _, hour, minute ->
                    val selectedTime = Calendar.getInstance()
                    selectedTime.set(Calendar.HOUR_OF_DAY, hour)
                    selectedTime.set(Calendar.MINUTE, minute)
                    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                    val timePart = timeFormat.format(selectedTime.time)

                    // Append or set the time part
                    val currentText = etSchedule.text.toString()
                    if (currentText.contains(" at ")) {
                        val datePart = currentText.substringBefore(" at ")
                        etSchedule.setText("$datePart at $timePart")
                    } else {
                        etSchedule.setText("$currentText at $timePart")
                    }
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
            ).show()
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add New Class")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = etName.text.toString().trim()
                val schedule = etSchedule.text.toString().trim()
                val location = etLocation.text.toString().trim()
                val specialty = etSpecialty.text.toString().trim()

                if (validateClassInput(name, schedule, location, specialty)) {
                    saveClassToFirestore(name, schedule, location, specialty)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun validateClassInput(
        name: String,
        schedule: String,
        location: String,
        specialty: String
    ): Boolean {
        return when {
            name.isEmpty() -> {
                Toast.makeText(requireContext(), "Class name required", Toast.LENGTH_SHORT).show()
                false
            }
            schedule.isEmpty() -> {
                Toast.makeText(requireContext(), "Schedule required", Toast.LENGTH_SHORT).show()
                false
            }
            location.isEmpty() -> {
                Toast.makeText(requireContext(), "Location required", Toast.LENGTH_SHORT).show()
                false
            }
            specialty.isEmpty() -> {
                Toast.makeText(requireContext(), "Specialty required", Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }

    private fun saveClassToFirestore(
        name: String,
        schedule: String,
        location: String,
        specialty: String
    ) {
        val classData = hashMapOf(
            "name" to name,
            "schedule" to schedule,
            "location" to location,
            "specialty" to specialty
        )

        db.collection("classes")
            .add(classData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Class added successfully", Toast.LENGTH_SHORT)
                    .show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Error adding class: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun showBookingsDialog(gymClass: GymClass) {
        val dialog = BottomSheetDialog(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_class_bookings, null)

        dialogView.findViewById<TextView>(R.id.tvDialogTitle).text = gymClass.name
        dialogView.findViewById<TextView>(R.id.tvDialogSchedule).text = "Schedule: ${gymClass.schedule}"
        dialogView.findViewById<TextView>(R.id.tvDialogLocation).text = "Location: ${gymClass.location}"
        dialogView.findViewById<TextView>(R.id.tvDialogSpecialty).text = "Specialty: ${gymClass.specialty}"

        val bookingsRecyclerView = dialogView.findViewById<RecyclerView>(R.id.bookingsRecyclerView)
        bookingsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        db.collection("classes")
            .document(gymClass.id)
            .collection("bookings")
            .get()
            .addOnSuccessListener { documents ->
                val bookings = documents.map { doc ->
                    Booking(
                        memberName = doc.getString("userName") ?: "Anonymous",
                        bookingTime = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                            .format(Date(doc.getLong("bookingTime") ?: System.currentTimeMillis()))
                    )
                }
                bookingsRecyclerView.adapter = BookingAdapter(bookings)
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Error loading bookings: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }

        dialogView.findViewById<Button>(R.id.btnCloseDialog).setOnClickListener {
            dialog.dismiss()
        }

        dialog.setContentView(dialogView)
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        firestoreListener?.remove()
    }

    // Data classes
    data class GymClass(
        val id: String = "",
        val name: String,
        val schedule: String,
        val location: String,
        val specialty: String = "General"
    )

    data class Booking(
        val memberName: String,
        val bookingTime: String
    )

    class ClassAdapter(
        private var classes: List<GymClass>,
        private val onViewClick: (GymClass) -> Unit,
        private val onDeleteClick: (GymClass) -> Unit
    ) : RecyclerView.Adapter<ClassAdapter.ClassViewHolder>() {

        fun updateClasses(newClasses: List<GymClass>) {
            this.classes = newClasses
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClassViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_admin_class, parent, false)
            return ClassViewHolder(view)
        }

        override fun onBindViewHolder(holder: ClassViewHolder, position: Int) {
            val gymClass = classes[position]
            holder.tvClassName.text = gymClass.name
            holder.tvClassSchedule.text = "Schedule: ${gymClass.schedule}"
            holder.tvClassLocation.text = "Location: ${gymClass.location}"
            holder.tvClassSpecialty.text = "Specialty: ${gymClass.specialty}"

            holder.btnView.setOnClickListener { onViewClick(gymClass) }
            holder.btnDelete.setOnClickListener { onDeleteClick(gymClass) }
        }

        override fun getItemCount() = classes.size

        class ClassViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvClassName: TextView = itemView.findViewById(R.id.tvClassName)
            val tvClassSchedule: TextView = itemView.findViewById(R.id.tvClassSchedule)
            val tvClassLocation: TextView = itemView.findViewById(R.id.tvClassLocation)
            val tvClassSpecialty: TextView = itemView.findViewById(R.id.tvClassSpecialty)
            val btnView: Button = itemView.findViewById(R.id.btnView)
            val btnDelete: ImageView = itemView.findViewById(R.id.ivDelete)
        }
    }

    class BookingAdapter(private val bookings: List<Booking>) :
        RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_booking, parent, false)
            return BookingViewHolder(view)
        }

        override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
            val booking = bookings[position]
            holder.tvMemberName.text = booking.memberName
            holder.tvBookingTime.text = booking.bookingTime
        }

        override fun getItemCount() = bookings.size

        class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvMemberName: TextView = itemView.findViewById(R.id.tvMemberName)
            val tvBookingTime: TextView = itemView.findViewById(R.id.tvBookingTime)
        }
    }
}