package com.example.fitflexfitnessstudio.adsfragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.fitflexfitnessstudio.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AvailableClassesFragment : Fragment() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private lateinit var adapter: AvailableClassesAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private val classList = mutableListOf<FitnessClass>()
    private val bookedClassIds = mutableSetOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_available_classes, container, false)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        setupRecyclerView(view)
        loadData()
        return view
    }

    private fun setupRecyclerView(view: View) {
        view.findViewById<RecyclerView>(R.id.classesRecyclerView).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = AvailableClassesAdapter(
                classList,
                bookedClassIds,
                onBookClick = { bookClass(it) }
            ).also { this@AvailableClassesFragment.adapter = it }
        }

        swipeRefresh.setOnRefreshListener {
            loadData()
        }
    }

    private fun loadData() {
        lifecycleScope.launch {
            try {
                // Load classes and bookings in parallel
                val (classes, bookings) = withContext(Dispatchers.IO) {
                    val classesDeferred = db.collection("classes").get()
                    val bookingsDeferred = auth.currentUser?.uid?.let { userId ->
                        db.collectionGroup("bookings")
                            .whereEqualTo("userId", userId)
                            .whereEqualTo("status", "booked")
                            .get()
                    }

                    val classes = classesDeferred.await()
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

                    val bookedIds = bookingsDeferred?.await()?.documents
                        ?.mapNotNull { it.getString("classId") }
                        ?.toSet() ?: emptySet()

                    Pair(classes, bookedIds)
                }

                bookedClassIds.clear()
                bookedClassIds.addAll(bookings)
                updateClassList(classes)
            } catch (e: Exception) {
                showToast("Error loading data: ${e.localizedMessage}")
            } finally {
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun updateClassList(newClasses: List<FitnessClass>) {
        // Filter out booked classes
        val availableClasses = newClasses.filterNot { it.id in bookedClassIds }

        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = classList.size
            override fun getNewListSize() = availableClasses.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int) =
                classList[oldPos].id == availableClasses[newPos].id
            override fun areContentsTheSame(oldPos: Int, newPos: Int) =
                classList[oldPos] == availableClasses[newPos]
        })

        classList.clear()
        classList.addAll(availableClasses)
        diffResult.dispatchUpdatesTo(adapter)
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
                    db.collection("classes")
                        .document(fitnessClass.id)
                        .collection("bookings")
                        .add(bookingData)
                        .await()
                }

                // Update local state
                bookedClassIds.add(fitnessClass.id)
                updateClassList(classList)
                showToast("Successfully booked ${fitnessClass.name}")
            } catch (e: Exception) {
                showToast("Booking failed: ${e.localizedMessage}")
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    data class FitnessClass(
        val id: String = "",
        val name: String,
        val specialty: String,
        val schedule: String,
        val location: String,
        val coachName: String = ""
    )

    class AvailableClassesAdapter(
        private var classes: List<FitnessClass>,
        private val bookedClassIds: Set<String>,
        private val onBookClick: (FitnessClass) -> Unit
    ) : RecyclerView.Adapter<AvailableClassesAdapter.ClassViewHolder>() {

        fun updateClasses(newClasses: List<FitnessClass>) {
            classes = newClasses
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ClassViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_class, parent, false)
        )

        override fun onBindViewHolder(holder: ClassViewHolder, position: Int) {
            val fitnessClass = classes[position]
            with(holder) {
                tvClassName.text = fitnessClass.name
                tvClassSpecialty.text = "Specialty: ${fitnessClass.specialty}"
                tvClassSchedule.text = "Schedule: ${fitnessClass.schedule}"
                tvClassLocation.text = "Location: ${fitnessClass.location}"
                tvCoachName.text = fitnessClass.coachName.takeIf { it.isNotEmpty() }?.let { "Coach: $it" } ?: ""

                itemView.setOnClickListener { onBookClick(fitnessClass) }
            }
        }

        override fun getItemCount() = classes.size

        class ClassViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvClassName: TextView = itemView.findViewById(R.id.tvClassName)
            val tvClassSpecialty: TextView = itemView.findViewById(R.id.tvClassSpecialty)
            val tvClassSchedule: TextView = itemView.findViewById(R.id.tvClassSchedule)
            val tvClassLocation: TextView = itemView.findViewById(R.id.tvClassLocation)
            val tvCoachName: TextView = itemView.findViewById(R.id.tvCoachName)
        }
    }
}