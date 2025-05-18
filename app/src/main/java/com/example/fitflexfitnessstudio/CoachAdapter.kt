package com.example.fitflexfitnessstudio

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.app.AlertDialog
import android.content.Context
import androidx.core.content.ContextCompat

class CoachAdapter(
    private val coachList: List<Coach>,
    private val onItemClick: (Coach) -> Unit
) : RecyclerView.Adapter<CoachAdapter.CoachViewHolder>() {

    private val bookedCoaches = mutableSetOf<Int>() // Track booked coaches by position

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CoachViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_coach, parent, false)
        return CoachViewHolder(view)
    }

    override fun onBindViewHolder(holder: CoachViewHolder, position: Int) {
        val coach = coachList[position]
        holder.tvCoachName.text = coach.name
        holder.tvCoachSpecialty.text = "Specialty: ${coach.specialty}"
        holder.tvCoachSchedule.text = "Schedule: ${coach.schedule}"

        // Set click listener for the item
        holder.itemView.setOnClickListener {
            onItemClick(coach)
        }

        // Check if this coach is booked
        val isBooked = bookedCoaches.contains(position)
        updateButtonState(holder, isBooked, position)
    }

    private fun updateButtonState(holder: CoachViewHolder, isBooked: Boolean, position: Int) {
        if (isBooked) {
            holder.btnBookCoach.text = "Booked"
            holder.btnBookCoach.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.dark_blue))
            holder.btnBookCoach.isEnabled = false
        } else {
            holder.btnBookCoach.text = "Book"
            holder.btnBookCoach.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.blue))
            holder.btnBookCoach.isEnabled = true
        }

        holder.btnBookCoach.setOnClickListener {
            showConfirmationDialog(holder.itemView.context, coachList[position], position)
        }
    }

    private fun showConfirmationDialog(context: Context, coach: Coach, position: Int) {
        val dialog = AlertDialog.Builder(context, R.style.CustomAlertDialog)
            .setTitle("Confirm Booking")
            .setMessage("Are you sure you want to book ${coach.name}?")
            .setPositiveButton("Yes") { dialog, _ ->
                bookedCoaches.add(position)
                notifyItemChanged(position)
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        // To ensure text colors are black (some devices might override)
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(context, android.R.color.black))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(context, android.R.color.black))
        }

        dialog.show()
    }
    override fun getItemCount(): Int = coachList.size

    class CoachViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCoachName: TextView = itemView.findViewById(R.id.tvCoachName)
        val tvCoachSpecialty: TextView = itemView.findViewById(R.id.tvCoachSpecialty)
        val tvCoachSchedule: TextView = itemView.findViewById(R.id.tvCoachSchedule)
        val btnBookCoach: Button = itemView.findViewById(R.id.btnBookCoach)
    }
}