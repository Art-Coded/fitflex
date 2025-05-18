package com.example.fitflexfitnessstudio.adsfragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.fitflexfitnessstudio.R
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class RecommendedPromosFragment : Fragment() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private lateinit var adapter: FitnessClassAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private val bookedClassIds = mutableSetOf<String>()
    private var bookingsListener: ListenerRegistration? = null
    private val classList = mutableListOf<FitnessClass>()
    private var isBookingsLoaded = false

    // Filter state and buttons
    enum class FilterState { ALL, AVAILABLE, BOOKED }
    private var currentFilter = FilterState.ALL
    private lateinit var btnAll: Button
    private lateinit var btnAvailable: Button
    private lateinit var btnBooked: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_recommended_promos, container, false)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)

        // Initialize filter buttons
        btnAll = view.findViewById(R.id.btnAll)
        btnAvailable = view.findViewById(R.id.btnAvailable)
        btnBooked = view.findViewById(R.id.btnBooked)

        setupFilterButtons()
        setupRecyclerView(view)
        loadClasses()
        return view
    }

    override fun onStart() {
        super.onStart()
        loadBookingsIfNeeded()
    }

    override fun onStop() {
        super.onStop()
        bookingsListener?.remove()
        isBookingsLoaded = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bookingsListener?.remove()
    }

    private fun loadBookingsIfNeeded() {
        if (!isBookingsLoaded) {
            setupBookingsListener()
            isBookingsLoaded = true
        }
    }

    private fun setupFilterButtons() {
        btnAll.setOnClickListener {
            currentFilter = FilterState.ALL
            updateButtonStates()
            adapter.applyFilter(currentFilter)
        }

        btnAvailable.setOnClickListener {
            currentFilter = FilterState.AVAILABLE
            updateButtonStates()
            adapter.applyFilter(currentFilter)
        }

        btnBooked.setOnClickListener {
            currentFilter = FilterState.BOOKED
            updateButtonStates()
            adapter.applyFilter(currentFilter)
        }

        updateButtonStates()
    }

    private fun updateButtonStates() {
        // Reset all buttons
        btnAll.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.colorPrimary)
        btnAvailable.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.colorPrimary)
        btnBooked.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.colorPrimary)

        // Highlight selected button
        when(currentFilter) {
            FilterState.ALL -> btnAll.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.colorAccent)
            FilterState.AVAILABLE -> btnAvailable.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.colorAccent)
            FilterState.BOOKED -> btnBooked.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.colorAccent)
        }
    }

    private fun setupRecyclerView(view: View) {
        view.findViewById<RecyclerView>(R.id.classesRecyclerView).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = FitnessClassAdapter(
                emptyList(),
                onBookClick = { bookClass(it) },
                onCancelClick = { cancelBooking(it) },
                onViewClick = { viewBookings(it) },
                bookedClassIds = bookedClassIds
            ).also { this@RecommendedPromosFragment.adapter = it }
        }

        swipeRefresh.setOnRefreshListener {
            refreshData()
        }
    }

    private fun refreshData() {
        lifecycleScope.launch {
            try {
                loadClasses()
                setupBookingsListener(forceRefresh = true)
            } finally {
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun loadClasses() {
        lifecycleScope.launch {
            try {
                val classes = withContext(Dispatchers.IO) {
                    db.collection("classes").get().await()
                        .documents.map { doc ->
                            FitnessClass(
                                id = doc.id,
                                name = doc.getString("name").orEmpty(),
                                specialty = doc.getString("specialty").orEmpty(),
                                schedule = doc.getString("schedule").orEmpty(),
                                location = doc.getString("location").orEmpty(),
                                coachName = doc.getString("coachName").orEmpty()
                            )
                        }
                }
                updateClassList(classes)
            } catch (e: Exception) {
                showToast("Error loading classes: ${e.localizedMessage}")
            }
        }
    }

    private fun updateClassList(newClasses: List<FitnessClass>) {
        classList.clear()
        classList.addAll(newClasses)
        adapter.updateClasses(classList)
        adapter.applyFilter(currentFilter)
    }

    private fun setupBookingsListener(forceRefresh: Boolean = false) {
        if (forceRefresh) {
            bookingsListener?.remove()
            bookedClassIds.clear()
        }

        auth.currentUser?.uid?.let { userId ->
            bookingsListener = db.collection("users")
                .document(userId)
                .collection("bookings")
                .whereEqualTo("status", "booked")
                .addSnapshotListener { snapshots, error ->
                    error?.let {
                        showToast("Bookings error: ${it.message}")
                        return@addSnapshotListener
                    }

                    val newBookedIds = snapshots?.documents
                        ?.mapNotNull { it.getString("classId") }
                        ?.toSet() ?: emptySet()

                    if (bookedClassIds != newBookedIds) {
                        bookedClassIds.clear()
                        bookedClassIds.addAll(newBookedIds)
                        adapter.updateBookedIds(bookedClassIds)
                    }
                }
        }
    }

    private fun bookClass(fitnessClass: FitnessClass) {
        lifecycleScope.launch {
            try {
                val user = auth.currentUser ?: run {
                    showToast("Please sign in to book classes")
                    return@launch
                }

                val bookingData = hashMapOf(
                    "userId" to user.uid,
                    "classId" to fitnessClass.id,
                    "className" to fitnessClass.name,
                    "bookingTime" to System.currentTimeMillis(),
                    "status" to "booked"
                )

                withContext(Dispatchers.IO) {
                    val bookingRef = db.collection("classes")
                        .document(fitnessClass.id)
                        .collection("bookings")
                        .add(bookingData)
                        .await()

                    db.collection("users")
                        .document(user.uid)
                        .collection("bookings")
                        .document(bookingRef.id)
                        .set(bookingData)
                        .await()
                }

                showToast("Successfully booked ${fitnessClass.name}")
            } catch (e: Exception) {
                showToast("Booking failed: ${e.localizedMessage}")
            }
        }
    }

    private fun cancelBooking(classId: String) {
        lifecycleScope.launch {
            try {
                val user = auth.currentUser ?: return@launch

                withContext(Dispatchers.IO) {
                    val userBookings = db.collection("users")
                        .document(user.uid)
                        .collection("bookings")
                        .whereEqualTo("classId", classId)
                        .whereEqualTo("status", "booked")
                        .get()
                        .await()

                    userBookings.documents.forEach { doc ->
                        val bookingId = doc.id

                        db.collection("users")
                            .document(user.uid)
                            .collection("bookings")
                            .document(bookingId)
                            .delete()
                            .await()

                        db.collection("classes")
                            .document(classId)
                            .collection("bookings")
                            .document(bookingId)
                            .update("status", "cancelled")
                            .await()
                    }
                }

                showToast("Booking cancelled")
            } catch (e: Exception) {
                showToast("Cancellation failed: ${e.localizedMessage}")
            }
        }
    }

    private fun viewBookings(classId: String) {
        lifecycleScope.launch {
            try {
                val user = auth.currentUser ?: return@launch
                val fitnessClass = classList.find { it.id == classId } ?: run {
                    showToast("Class not found")
                    return@launch
                }

                val bookings = withContext(Dispatchers.IO) {
                    db.collection("classes")
                        .document(classId)
                        .collection("bookings")
                        .whereEqualTo("userId", user.uid)
                        .whereEqualTo("status", "booked")
                        .get()
                        .await()
                }

                showBookingsDialog(fitnessClass, bookings.documents)
            } catch (e: Exception) {
                showToast("Failed to load bookings: ${e.localizedMessage}")
            }
        }
    }

    private fun showBookingsDialog(fitnessClass: FitnessClass, documents: List<com.google.firebase.firestore.DocumentSnapshot>) {
        val dialog = BottomSheetDialog(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_class_bookings, null).apply {
            findViewById<TextView>(R.id.tvDialogTitle).text = fitnessClass.name
            findViewById<TextView>(R.id.tvDialogSchedule).text = "Schedule: ${fitnessClass.schedule}"
            findViewById<TextView>(R.id.tvDialogLocation).text = "Location: ${fitnessClass.location}"
            findViewById<TextView>(R.id.tvDialogSpecialty).text = "Specialty: ${fitnessClass.specialty}"

            findViewById<RecyclerView>(R.id.bookingsRecyclerView).apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = BookingAdapter(
                    documents.map {
                        Booking(
                            bookingTime = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                                .format(Date(it.getLong("bookingTime") ?: 0))
                        )
                    }
                )
            }

            findViewById<Button>(R.id.btnCloseDialog).setOnClickListener { dialog.dismiss() }
        }

        dialog.setContentView(dialogView)
        dialog.show()
    }

    private fun showToast(message: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    data class FitnessClass(
        val id: String = "",
        val name: String,
        val specialty: String,
        val schedule: String,
        val location: String,
        val coachName: String = ""
    ) {
        fun isBooked(bookedIds: Set<String>) = bookedIds.contains(id)
    }

    data class Booking(val bookingTime: String)

    class FitnessClassAdapter(
        private var classes: List<FitnessClass>,
        private val onBookClick: (FitnessClass) -> Unit,
        private val onCancelClick: (String) -> Unit,
        private val onViewClick: (String) -> Unit,
        private val bookedClassIds: Set<String>
    ) : RecyclerView.Adapter<FitnessClassAdapter.ClassViewHolder>() {

        private var filteredClasses: List<FitnessClass> = classes
        private var currentFilter: FilterState = FilterState.ALL

        fun updateClasses(newClasses: List<FitnessClass>) {
            classes = newClasses
            applyFilter(currentFilter)
        }

        fun updateBookedIds(newBookedIds: Set<String>) {
            applyFilter(currentFilter)
        }

        fun applyFilter(filterState: FilterState) {
            currentFilter = filterState
            filteredClasses = when(filterState) {
                FilterState.ALL -> classes
                FilterState.AVAILABLE -> classes.filterNot { it.isBooked(bookedClassIds) }
                FilterState.BOOKED -> classes.filter { it.isBooked(bookedClassIds) }
            }
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClassViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_class, parent, false)
            return ClassViewHolder(view)
        }

        override fun onBindViewHolder(holder: ClassViewHolder, position: Int) {
            val fitnessClass = filteredClasses[position]
            val isBooked = fitnessClass.isBooked(bookedClassIds)

            with(holder) {
                tvClassName.text = fitnessClass.name
                tvClassSpecialty.text = "Specialty: ${fitnessClass.specialty}"
                tvClassSchedule.text = "Schedule: ${fitnessClass.schedule}"
                tvClassLocation.text = "Location: ${fitnessClass.location}"
                tvCoachName.text = fitnessClass.coachName.takeIf { it.isNotEmpty() }?.let { "Coach: $it" } ?: ""

                btnBookClass.visibility = if (isBooked) View.GONE else View.VISIBLE
                btnCancelBooking.visibility = if (isBooked) View.VISIBLE else View.GONE
                btnViewBooking.visibility = if (isBooked) View.VISIBLE else View.GONE

                btnBookClass.setOnClickListener { onBookClick(fitnessClass) }
                btnCancelBooking.setOnClickListener { onCancelClick(fitnessClass.id) }
                btnViewBooking.setOnClickListener { onViewClick(fitnessClass.id) }
            }
        }

        override fun getItemCount(): Int = filteredClasses.size

        inner class ClassViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvClassName: TextView = itemView.findViewById(R.id.tvClassName)
            val tvClassSpecialty: TextView = itemView.findViewById(R.id.tvClassSpecialty)
            val tvClassSchedule: TextView = itemView.findViewById(R.id.tvClassSchedule)
            val tvClassLocation: TextView = itemView.findViewById(R.id.tvClassLocation)
            val tvCoachName: TextView = itemView.findViewById(R.id.tvCoachName)
            val btnBookClass: Button = itemView.findViewById(R.id.btnBookClass)
            val btnCancelBooking: Button = itemView.findViewById(R.id.btnCancelBooking)
            val btnViewBooking: Button = itemView.findViewById(R.id.btnViewBooking)
        }
    }

    class BookingAdapter(private val bookings: List<Booking>) : RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = BookingViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_booking, parent, false)
        )

        override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
            holder.tvBookingTime.text = bookings[position].bookingTime
        }

        override fun getItemCount() = bookings.size

        class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvBookingTime: TextView = itemView.findViewById(R.id.tvBookingTime)
        }
    }
}