package com.example.fitflexfitnessstudio.adminmanage

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitflexfitnessstudio.R
import com.example.fitflexfitnessstudio.databinding.FragmentManageNotifBinding
import com.example.fitflexfitnessstudio.databinding.ItemNotificationBinding
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class ManageNotifFragment : Fragment() {

    private var _binding: FragmentManageNotifBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: NotificationAdapter
    private val db = FirebaseFirestore.getInstance()
    private val notificationPresetsCollection = db.collection("notificationPresets")
    private val notificationsCollection = db.collection("notifications")
    private var firestoreListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageNotifBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize RecyclerView
        adapter = NotificationAdapter(emptyList()) { notification, action ->
            when (action) {
                Action.EDIT -> showEditNotificationDialog(notification)
                Action.SEND -> sendNotification(notification)
                Action.DELETE -> deleteNotification(notification.id)
            }
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ManageNotifFragment.adapter
        }

        // Set up add button
        binding.addButton.setOnClickListener {
            showAddNotificationDialog()
        }

        // Set up Firestore listener for real-time updates
        setupFirestoreListener()
    }

    private fun setupFirestoreListener() {
        firestoreListener = notificationPresetsCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(context, "Error loading notifications", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val notifications = snapshot?.documents?.mapNotNull { doc ->
                    Notification(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        body = doc.getString("content") ?: ""
                    )
                } ?: emptyList()

                adapter.updateNotifications(notifications)
            }
    }

    private fun showAddNotificationDialog() {
        val dialog = Dialog(requireContext()).apply {
            setContentView(R.layout.dialog_add_notification)
            window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setCancelable(true)
        }

        val titleInput = dialog.findViewById<EditText>(R.id.titleEditText)
        val bodyInput = dialog.findViewById<EditText>(R.id.bodyEditText)
        val cancelBtn = dialog.findViewById<Button>(R.id.cancelButton)
        val saveBtn = dialog.findViewById<Button>(R.id.saveButton)

        cancelBtn.setOnClickListener { dialog.dismiss() }

        saveBtn.setOnClickListener {
            val title = titleInput.text.toString()
            val body = bodyInput.text.toString()

            if (title.isBlank() || body.isBlank()) {
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else {
                addNotificationToPresets(title, body)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun addNotificationToPresets(title: String, content: String) {
        val notificationData = hashMapOf(
            "title" to title,
            "content" to content
        )

        notificationPresetsCollection.add(notificationData)
            .addOnSuccessListener {
                Toast.makeText(context, "Notification preset added", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error adding notification: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showEditNotificationDialog(notification: Notification) {
        val dialog = Dialog(requireContext()).apply {
            setContentView(R.layout.dialog_add_notification)
            window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setCancelable(true)
        }

        val titleInput = dialog.findViewById<EditText>(R.id.titleEditText).apply {
            setText(notification.title)
        }
        val bodyInput = dialog.findViewById<EditText>(R.id.bodyEditText).apply {
            setText(notification.body)
        }
        val saveBtn = dialog.findViewById<Button>(R.id.saveButton).apply {
            text = "Update"
        }

        dialog.findViewById<Button>(R.id.cancelButton).setOnClickListener { dialog.dismiss() }

        saveBtn.setOnClickListener {
            val newTitle = titleInput.text.toString()
            val newBody = bodyInput.text.toString()

            if (newTitle.isBlank() || newBody.isBlank()) {
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else {
                updateNotificationPreset(notification.id, newTitle, newBody)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun updateNotificationPreset(id: String, title: String, content: String) {
        val updates = hashMapOf<String, Any>(
            "title" to title,
            "content" to content
        )

        notificationPresetsCollection.document(id)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(context, "Notification updated", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error updating notification: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendNotification(notification: Notification) {
        val notificationData = hashMapOf<String, Any>(
            "title" to notification.title,
            "content" to notification.body,
            "timestamp" to FieldValue.serverTimestamp()
        )

        notificationsCollection.add(notificationData)
            .addOnSuccessListener {
                Toast.makeText(context, "Notification sent to users", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error sending notification: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteNotification(id: String) {
        notificationPresetsCollection.document(id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(context, "Notification deleted", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error deleting notification: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        firestoreListener?.remove()
        _binding = null
    }

    data class Notification(
        val id: String = "",
        val title: String,
        val body: String
    )

    enum class Action {
        EDIT, SEND, DELETE
    }

    inner class NotificationAdapter(
        private var notifications: List<Notification>,
        private val onActionClick: (Notification, Action) -> Unit
    ) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

        inner class NotificationViewHolder(val binding: ItemNotificationBinding) :
            RecyclerView.ViewHolder(binding.root)

        fun updateNotifications(newNotifications: List<Notification>) {
            this.notifications = newNotifications
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
            return NotificationViewHolder(
                ItemNotificationBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }

        override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
            val notification = notifications[position]
            with(holder.binding) {
                titleTextView.text = notification.title
                bodyTextView.text = notification.body

                editButton.setOnClickListener {
                    onActionClick(notification, Action.EDIT)
                }
                sendButton.setOnClickListener {
                    onActionClick(notification, Action.SEND)
                }
                deleteButton.setOnClickListener {
                    onActionClick(notification, Action.DELETE)
                }
            }
        }

        override fun getItemCount() = notifications.size
    }

    companion object {
        @JvmStatic
        fun newInstance() = ManageNotifFragment()
    }
}