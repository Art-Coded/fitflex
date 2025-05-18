package com.example.fitflexfitnessstudio.adsfragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.fitflexfitnessstudio.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class BookedClassesFragment : Fragment() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private lateinit var adapter: BookedClassesAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private val bookedClasses = mutableListOf<BookedClass>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_booked_classes, container, false)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        setupRecyclerView(view)
        loadBookedClasses()
        return view
    }

    private fun setupRecyclerView(view: View) {
        view.findViewById<RecyclerView>(R.id.classesRecyclerView).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = BookedClassesAdapter(
                bookedClasses,
                onCancelClick = { cancelBooking(it) },
                onViewClick = { viewDetails(it) }
            ).also { this@BookedClassesFragment.adapter = it }
        }

        swipeRefresh.setOnRefreshListener {
            loadBookedClasses()
        }
    }

    private fun loadBookedClasses() {
        lifecycleScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: run {
                    showToast("Please sign in to view bookings")
                    return@launch
                }

                val bookings = withContext(Dispatchers.IO) {
                    db.collectionGroup("bookings")
                        .whereEqualTo("userId", userId)
                        .whereEqualTo("status", "booked")
                        .get()
                        .await()
                }

                bookedClasses.clear()
                bookings.documents.forEach { booking ->
                    val classId = booking.getString("classId") ?: return@forEach
                    val classDoc = withContext(Dispatchers.IO) {
                        db.collection("classes").document(classId).get().await()
                    }

                    bookedClasses.add(BookedClass(
                        id = booking.id,
                        classId = classId,
                        name = classDoc.getString("name") ?: "Unknown Class",
                        schedule = classDoc.getString("schedule") ?: "",
                        location = classDoc.getString("location") ?: "",
                        bookingTime = booking.getLong("bookingTime") ?: 0
                    ))
                }

                adapter.notifyDataSetChanged()
            } catch (e: Exception) {
                showToast("Error loading bookings: ${e.localizedMessage}")
            } finally {
                swipeRefresh.isRefreshing = false
            }
        }
    }
    private fun cancelBooking(bookingId: String) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val querySnapshot = db.collectionGroup("bookings")
                        .whereEqualTo(FieldPath.documentId(), bookingId)
                        .get()
                        .await()

                    for (document in querySnapshot.documents) {
                        document.reference.update("status", "cancelled").await()
                    }
                }

                // Remove from local list and update UI
                bookedClasses.removeAll { it.id == bookingId }
                adapter.notifyDataSetChanged()
                showToast("Booking cancelled")
            } catch (e: Exception) {
                Log.e("CancelBooking", "Error cancelling booking", e)
                showToast("Cancellation failed: ${e.localizedMessage}")
            }
        }
    }

    private fun viewDetails(bookedClass: BookedClass) {
        // Implement view details logic
        showToast("Viewing details for ${bookedClass.name}")
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    data class BookedClass(
        val id: String,
        val classId: String,
        val name: String,
        val schedule: String,
        val location: String,
        val bookingTime: Long
    )

    class BookedClassesAdapter(
        private val bookedClasses: List<BookedClass>,
        private val onCancelClick: (String) -> Unit,
        private val onViewClick: (BookedClass) -> Unit
    ) : RecyclerView.Adapter<BookedClassesAdapter.BookedClassViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = BookedClassViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_booked_class, parent, false)
        )

        override fun onBindViewHolder(holder: BookedClassViewHolder, position: Int) {
            val bookedClass = bookedClasses[position]
            with(holder) {
                tvClassName.text = bookedClass.name
                tvClassSchedule.text = "Schedule: ${bookedClass.schedule}"
                tvClassLocation.text = "Location: ${bookedClass.location}"

                btnCancel.setOnClickListener { onCancelClick(bookedClass.id) }
                btnViewDetails.setOnClickListener { onViewClick(bookedClass) }
            }
        }

        override fun getItemCount() = bookedClasses.size

        class BookedClassViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvClassName: TextView = itemView.findViewById(R.id.tvClassName)
            val tvClassSchedule: TextView = itemView.findViewById(R.id.tvClassSchedule)
            val tvClassLocation: TextView = itemView.findViewById(R.id.tvClassLocation)
            val btnCancel: TextView = itemView.findViewById(R.id.btnCancel)
            val btnViewDetails: TextView = itemView.findViewById(R.id.btnViewDetails)
        }
    }
}