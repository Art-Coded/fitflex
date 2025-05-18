package com.example.fitflexfitnessstudio.admin.adapter

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fitflexfitnessstudio.R
import com.example.fitflexfitnessstudio.admin.MembershipItem
import com.google.android.material.textfield.TextInputEditText

class AdminMembershipAdapter(
    private val items: List<MembershipItem>,
    private val onItemEdited: (Int, MembershipItem) -> Unit
) : RecyclerView.Adapter<AdminMembershipAdapter.MembershipViewHolder>() {

    inner class MembershipViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val durationText: TextView = itemView.findViewById(R.id.tvDuration)
        val priceText: TextView = itemView.findViewById(R.id.tvPrice)
        val descriptionText: TextView = itemView.findViewById(R.id.tvDescription)
        val editButton: Button = itemView.findViewById(R.id.btnEdit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MembershipViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_membership, parent, false)
        return MembershipViewHolder(view)
    }

    override fun onBindViewHolder(holder: MembershipViewHolder, position: Int) {
        val item = items[position]
        holder.durationText.text = item.duration
        holder.priceText.text = item.price
        holder.descriptionText.text = item.description

        holder.editButton.setOnClickListener {
            showEditDialog(holder.itemView.context, position, item)
        }
    }

    private fun showEditDialog(context: android.content.Context, position: Int, item: MembershipItem) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_membership, null)

        val etPrice = dialogView.findViewById<TextInputEditText>(R.id.etPrice).apply {
            setText(item.price.replace("₱", ""))
        }
        val etDescription = dialogView.findViewById<TextInputEditText>(R.id.etDescription).apply {
            setText(item.description)
        }

        val dialog = AlertDialog.Builder(context, R.style.AdminDialogTheme)
            .setTitle("Edit ${item.duration} Pricing")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newPrice = "₱${etPrice.text.toString()}"
                val newDescription = etDescription.text.toString()

                if (newPrice.isNotEmpty() && newDescription.isNotEmpty()) {
                    val editedItem = item.copy(
                        price = newPrice,
                        description = newDescription
                    )
                    onItemEdited(position, editedItem)
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(context.getColor(R.color.blue))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(context.getColor(R.color.blue))
        }

        dialog.show()
    }

    override fun getItemCount() = items.size
}