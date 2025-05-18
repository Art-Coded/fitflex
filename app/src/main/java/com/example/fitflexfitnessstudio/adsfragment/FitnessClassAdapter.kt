package com.example.fitflexfitnessstudio.adsfragment


import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.fitflexfitnessstudio.FitnessClass
import com.example.fitflexfitnessstudio.R

class FitnessClassAdapter(
    private var classList: List<FitnessClass>,
    private val onBookClick: (FitnessClass) -> Unit,
    private val onCancelClick: (String) -> Unit, // classId
    private val onViewClick: (String) -> Unit, // classId
    private val bookedClassIds: Set<String> = emptySet() // Track booked classes
) : RecyclerView.Adapter<FitnessClassAdapter.ClassViewHolder>() {

    fun updateClasses(newClasses: List<FitnessClass>) {
        classList = newClasses
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClassViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_class, parent, false)
        return ClassViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClassViewHolder, position: Int) {
        val fitnessClass = classList[position]
        val isBooked = bookedClassIds.contains(fitnessClass.id)

        holder.tvClassName.text = fitnessClass.name
        holder.tvClassSpecialty.text = "Specialty: ${fitnessClass.specialty}"
        holder.tvClassSchedule.text = "Schedule: ${fitnessClass.schedule}"
        holder.tvClassLocation.text = "Location: ${fitnessClass.location}"

        if (!fitnessClass.coachName.isNullOrEmpty()) {
            holder.tvCoachName.text = "Coach: ${fitnessClass.coachName}"
            holder.tvCoachName.visibility = View.VISIBLE
        } else {
            holder.tvCoachName.visibility = View.GONE
        }

        // Update button states based on booking status
        if (isBooked) {
            holder.btnBookClass.visibility = View.GONE
            holder.btnCancelBooking.visibility = View.VISIBLE
            holder.btnViewBooking.visibility = View.VISIBLE
        } else {
            holder.btnBookClass.visibility = View.VISIBLE
            holder.btnCancelBooking.visibility = View.GONE
            holder.btnViewBooking.visibility = View.GONE
        }

        holder.btnBookClass.setOnClickListener {
            showConfirmationDialog(holder.itemView.context, fitnessClass)
        }

        holder.btnCancelBooking.setOnClickListener {
            onCancelClick(fitnessClass.id)
        }

        holder.btnViewBooking.setOnClickListener {
            onViewClick(fitnessClass.id)
        }
    }

    private fun showConfirmationDialog(context: Context, fitnessClass: FitnessClass) {
        AlertDialog.Builder(context, R.style.CustomAlertDialog)
            .setTitle("Confirm Booking")
            .setMessage("Book ${fitnessClass.name} class?")
            .setPositiveButton("Book") { dialog, _ ->
                onBookClick(fitnessClass)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(context, android.R.color.black))
                    getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(context, android.R.color.black))
                }
                show()
            }
    }

    override fun getItemCount(): Int = classList.size

    class ClassViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
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