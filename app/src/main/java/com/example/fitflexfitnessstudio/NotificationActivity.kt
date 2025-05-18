
package com.example.fitflexfitnessstudio

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.*

class NotificationActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotificationAdapter
    private val notificationList = mutableListOf<NotificationModel>()
    private val db = FirebaseFirestore.getInstance()
    private var listener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = NotificationAdapter(notificationList)
        recyclerView.adapter = adapter

        loadNotifications()
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