package com.example.fitflexfitnessstudio.profilefrag

import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.fitflexfitnessstudio.R
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.text.SimpleDateFormat
import java.util.*

class SecondFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_second, container, false)

        val barChart: BarChart = view.findViewById(R.id.barChart)
        val weekContainer: LinearLayout = view.findViewById(R.id.weekContainer)

        // Get the current date and find the start of the week (Sunday)
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.SUNDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)

        val dayLabels = listOf("S", "M", "T", "W", "T", "F", "S")
        val dateFormat = SimpleDateFormat("d", Locale.getDefault())

        for (i in 0 until 7) {
            val dayLayout = LinearLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
            }

            // Day label (S, M, T...)
            val dayText = TextView(requireContext()).apply {
                text = dayLabels[i]
                textSize = 14f
                setTextColor(Color.DKGRAY)
            }

            // Date number
            val dateText = TextView(requireContext()).apply {
                text = dateFormat.format(calendar.time)
                textSize = 18f
                setTextColor(Color.BLACK)
                setPadding(8, 8, 8, 8)

                // Highlight today's date
                if (calendar.get(Calendar.DAY_OF_MONTH) == Calendar.getInstance().get(Calendar.DAY_OF_MONTH)) {
                    setBackgroundResource(android.R.drawable.dialog_holo_light_frame)
                }
            }

            // Move to the next day
            calendar.add(Calendar.DAY_OF_MONTH, 1)

            dayLayout.addView(dayText)
            dayLayout.addView(dateText)
            weekContainer.addView(dayLayout)
        }


        // Generate Weekly View

        // Generate Bar Chart
        setupBarChart(barChart)

        return view
    }

    private fun setupBarChart(barChart: BarChart) {
        val calendar = Calendar.getInstance()
        val entries = ArrayList<BarEntry>()
        val labels = mutableListOf<String>()

        // Generate last 8 weeks dynamically
        for (i in 7 downTo 0) {
            calendar.add(Calendar.WEEK_OF_YEAR, -1)
            val label = SimpleDateFormat("d MMM", Locale.getDefault()).format(calendar.time)
            labels.add(0, label)  // Insert at the beginning

            val workoutCount = (0..3).random().toFloat()  // Sample random data (replace with actual)
            entries.add(0, BarEntry(i.toFloat(), workoutCount))
        }

        val barDataSet = BarDataSet(entries, "Workouts").apply {
            color = ContextCompat.getColor(requireContext(), R.color.blue)
            valueTextSize = 14f
            valueTextColor = Color.BLACK
        }

        val data = BarData(barDataSet)
        barChart.data = data

        // Configure X-axis
        barChart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(labels)
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(false)
            granularity = 1f
            labelRotationAngle = -45f
        }

        // Configure chart appearance
        barChart.apply {
            axisLeft.axisMinimum = 0f
            axisRight.isEnabled = false
            description.isEnabled = false
            legend.isEnabled = false
            invalidate()  // Refresh chart
        }
    }
}
