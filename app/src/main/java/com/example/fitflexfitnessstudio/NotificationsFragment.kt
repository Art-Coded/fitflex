package com.example.fitflexfitnessstudio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class NotificationsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotificationAdapter
    private val notificationList = mutableListOf<NotificationModel>()
    private val db = FirebaseFirestore.getInstance()
    private var listener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notifications, container, false)

        // Initialize views
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = NotificationAdapter(notificationList)
        recyclerView.adapter = adapter

        loadNotifications()

        return view
    }

    private fun loadNotifications() {
        listener = db.collection("notifications")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) return@addSnapshotListener

                notificationList.clear()
                snapshots?.documents?.forEach { doc ->
                    val notification = NotificationModel(
                        icon = R.drawable.profile,
                        title = doc.getString("title") ?: "Membership Update",
                        message = doc.getString("content") ?: "A membership has been updated",
                        timestamp = doc.getDate("timestamp")
                    )
                    notificationList.add(notification)
                }
                adapter.notifyDataSetChanged()
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        listener?.remove()
    }
}
