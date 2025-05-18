// NotificationAdapter.kt
package com.example.fitflexfitnessstudio

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fitflexfitnessstudio.R
import java.text.SimpleDateFormat
import java.util.*

class NotificationAdapter(private val notifications: List<NotificationModel>) :
    RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iconImage: ImageView = view.findViewById(R.id.iconImage)
        val titleText: TextView = view.findViewById(R.id.titleText)
        val messageText: TextView = view.findViewById(R.id.messageText)
        val timeText: TextView = view.findViewById(R.id.timeText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.notification_item, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]
        holder.iconImage.setImageResource(R.drawable.logo) // Keep same icon
        holder.titleText.text = notification.title
        holder.messageText.text = notification.message

        // Format timestamp to display
        val dateFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        holder.timeText.text = notification.timestamp?.let { dateFormat.format(it) } ?: "Just now"
    }

    override fun getItemCount(): Int = notifications.size
}
