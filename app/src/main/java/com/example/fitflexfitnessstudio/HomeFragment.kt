package com.example.fitflexfitnessstudio

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.max

class HomeFragment : Fragment() {

    private lateinit var weekRecyclerView: RecyclerView
    private lateinit var weekAdapter: WeekAdapter
    private val db = FirebaseFirestore.getInstance()
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val weekFormat = SimpleDateFormat("w", Locale.getDefault())

    // UI Components
    private lateinit var todayMinutes: TextView
    private lateinit var weeklyAverage: TextView
    private lateinit var totalWorkouts: TextView
    private lateinit var totalDuration: TextView
    private lateinit var totalVolume: TextView
    private lateinit var barChart: BarChart
    private lateinit var daysLeftTextView: TextView
    private lateinit var streakTextView: TextView
    private lateinit var sessionsTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Initialize UI components
        daysLeftTextView = view.findViewById(R.id.daysleft)
        streakTextView = view.findViewById(R.id.Streak)
        sessionsTextView = view.findViewById(R.id.Sessions)

        todayMinutes = view.findViewById(R.id.todayMinutes)
        weeklyAverage = view.findViewById(R.id.weeklyAverage)
        totalWorkouts = view.findViewById(R.id.totalWorkouts)
        totalDuration = view.findViewById(R.id.totalTime)
        totalVolume = view.findViewById(R.id.totalVolume)
        barChart = view.findViewById(R.id.barChart)

        setupWeekCalendar(view)
        loadWorkoutData()
        loadMembershipData()

        return view
    }

    private fun loadWorkoutData() {
        currentUser?.uid?.let { uid ->
            loadTodayDuration(uid)
            loadWeeklyStats(uid)
            loadTotalStats(uid)
            loadChartData(uid)
            loadMembershipData() // Add this line
        }
    }
    private fun loadMembershipData() {
        currentUser?.uid?.let { uid ->
            // Get the most recent membership end date from subcollection
            db.collection("users").document(uid)
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

                    daysLeftTextView.text = daysLeft.toString()

                    // Show infinite sessions if days left > 0
                    if (daysLeft > 0) {
                        sessionsTextView.visibility = View.VISIBLE
                        sessionsTextView.text = "∞"
                    } else {
                        sessionsTextView.text = "0"
                    }
                }
                .addOnFailureListener { e ->
                    daysLeftTextView.text = "0"
                    sessionsTextView.visibility = View.GONE
                }

            // Load streak from user document
            db.collection("users").document(uid)
                .get()
                .addOnSuccessListener { document ->
                    val streak = document.getLong("streak") ?: 0
                    streakTextView.text = "$streak Days Streak"
                }
        }
    }


    private fun loadTodayDuration(uid: String) {
        val today = dateFormat.format(Date())
        db.collection("users").document(uid)
            .collection("workouts").document(today)
            .collection("sessions").get()
            .addOnSuccessListener { snapshot ->
                val totalSeconds = snapshot.documents.sumOf { it.getLong("duration") ?: 0L }
                val minutes = max(totalSeconds / 60, 0)
                todayMinutes.text = minutes.toString()
            }
    }

    private fun loadWeeklyStats(uid: String) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
        }
        val weekStart = calendar.timeInMillis
        calendar.add(Calendar.WEEK_OF_YEAR, 1)
        val weekEnd = calendar.timeInMillis

        db.collectionGroup("sessions")
            .whereGreaterThanOrEqualTo("timestamp", weekStart)
            .whereLessThan("timestamp", weekEnd)
            .get()
            .addOnSuccessListener { snapshot ->
                val totalSeconds = snapshot.documents.sumOf { it.getLong("duration") ?: 0L }
                val weeklyMin = max(totalSeconds / 60, 0)
                weeklyAverage.text = weeklyMin.toString()
            }
    }
    private fun loadTotalStats(uid: String) {
        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val totalWorkoutsCount = document.getLong("totalWorkouts") ?: 0L
                    val totalDurationMinutes = document.getLong("totalDuration") ?: 0L

                    totalWorkouts.text = totalWorkoutsCount.toString()

                    val minutes = totalDurationMinutes.floorDiv(60)
                    val hours = totalDurationMinutes.floorDiv(3600)

                    totalDuration.text = hours.toString()
                    totalVolume.text = minutes.toString()
                }
            }
    }

    private fun updateTotalTimeViews(totalSeconds: Long) {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        totalDuration.text = hours.toString()
        totalVolume.text = minutes.toString()
    }

    private fun loadChartData(uid: String) {
        val entries = ArrayList<BarEntry>()
        val labels = mutableListOf<String>()
        val calendar = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.SUNDAY
        }

        // Generate last 8 weeks data
        for (i in 7 downTo 0) {
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            calendar.add(Calendar.WEEK_OF_YEAR, -i)
            val weekStart = calendar.timeInMillis
            calendar.add(Calendar.WEEK_OF_YEAR, 1)
            val weekEnd = calendar.timeInMillis

            db.collectionGroup("sessions")
                .whereGreaterThanOrEqualTo("timestamp", weekStart)
                .whereLessThan("timestamp", weekEnd)
                .get()
                .addOnSuccessListener { snapshot ->
                    val totalSeconds = snapshot.documents.sumOf { it.getLong("duration") ?: 0L }
                    val weekLabel = SimpleDateFormat("w", Locale.getDefault()).format(calendar.time)

                    entries.add(BarEntry((7 - i).toFloat(), (totalSeconds / 60).toFloat()))
                    labels.add("Week $weekLabel")

                    if (entries.size == 8) {
                        setupWorkoutChart(entries, labels)
                    }
                }
        }
    }

    private fun setupWorkoutChart(entries: List<BarEntry>, labels: List<String>) {
        BarDataSet(entries, "Workouts").apply {
            color = ContextCompat.getColor(requireContext(), R.color.blue)
            valueTextSize = 14f
            valueTextColor = Color.BLACK
            barChart.data = BarData(this)
        }

        barChart.apply {
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(labels)
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                labelRotationAngle = -45f
            }
            axisLeft.axisMinimum = 0f
            axisRight.isEnabled = false
            description.isEnabled = false
            legend.isEnabled = false
            animateY(1000)
            invalidate()
        }
    }

    private fun setupWeekCalendar(view: View) {
        weekRecyclerView = view.findViewById(R.id.weekRecyclerView)
        weekAdapter = WeekAdapter()

        weekRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = weekAdapter
            setHasFixedSize(true)
        }

        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(weekRecyclerView)
        weekRecyclerView.scrollToPosition(Int.MAX_VALUE / 2)
    }

    inner class WeekAdapter : RecyclerView.Adapter<WeekAdapter.WeekViewHolder>() {
        private val dayLabels = listOf("S", "M", "T", "W", "T", "F", "S")

        inner class WeekViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val weekContainer: LinearLayout = itemView.findViewById(R.id.weekContainer)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeekViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_week, parent, false)
            return WeekViewHolder(view)
        }

        override fun onBindViewHolder(holder: WeekViewHolder, position: Int) {
            val weekOffset = position - (Int.MAX_VALUE / 2)
            val calendar = Calendar.getInstance().apply {
                add(Calendar.WEEK_OF_YEAR, weekOffset)
                set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            }

            holder.weekContainer.removeAllViews()
            val dateFormat = SimpleDateFormat("d", Locale.getDefault())
            val today = Calendar.getInstance()

            repeat(7) { i ->
                val dayLayout = LinearLayout(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                }

                TextView(requireContext()).apply {
                    text = dayLabels[i]
                    textSize = 14f
                    setTextColor(Color.DKGRAY)
                    dayLayout.addView(this)
                }

                TextView(requireContext()).apply {
                    text = dateFormat.format(calendar.time)
                    textSize = 18f
                    setTextColor(Color.BLACK)
                    setPadding(16, 8, 16, 8)

                    val isToday = calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                            calendar.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                            calendar.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH)

                    if (isToday) {
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                        background = ContextCompat.getDrawable(requireContext(), R.drawable.today_background)
                    }

                    dayLayout.addView(this)
                }

                calendar.add(Calendar.DAY_OF_MONTH, 1)
                holder.weekContainer.addView(dayLayout)
            }
        }

        override fun getItemCount(): Int = Int.MAX_VALUE
    }
}