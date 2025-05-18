package com.example.fitflexfitnessstudio.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitflexfitnessstudio.R
import com.example.fitflexfitnessstudio.admin.adapter.AdminMembershipAdapter
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class AdminMembershippriceFragment : Fragment() {

    private lateinit var adapter: AdminMembershipAdapter
    private val membershipItems = mutableListOf<MembershipItem>()
    private val documentIds = mutableListOf<String>() // Stores Firestore document IDs

    private val db = FirebaseFirestore.getInstance()
    private val membershipTypesCollection = db.collection("MembershipTypes")
    private val notificationsCollection = db.collection("notifications")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin_membershipprice, container, false)

        val recyclerView = view.findViewById<RecyclerView>(R.id.membershipRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = AdminMembershipAdapter(membershipItems) { position, editedItem ->
            updateMembershipInFirestore(position, editedItem)
        }

        recyclerView.adapter = adapter
        loadMembershipPlans()

        return view
    }

    private fun loadMembershipPlans() {
        membershipTypesCollection.orderBy("duration").get()
            .addOnSuccessListener { documents ->
                membershipItems.clear()
                documentIds.clear()

                for (document in documents) {
                    val name = document.getString("name") ?: ""
                    val priceDisplay = document.getString("priceDisplay") ?: ""
                    val description = document.getString("description") ?: ""

                    membershipItems.add(MembershipItem(name, priceDisplay, description))
                    documentIds.add(document.id)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                showToast("Failed to load plans: ${e.message}")
            }
    }

    private fun updateMembershipInFirestore(position: Int, editedItem: MembershipItem) {
        if (position >= documentIds.size) {
            showToast("Error: Invalid position")
            return
        }

        val priceValue = try {
            editedItem.price.replace("₱", "").toInt()
        } catch (e: NumberFormatException) {
            showToast("Invalid price format. Use ₱ followed by numbers")
            return
        }

        val updates = hashMapOf<String, Any>(
            "price" to priceValue,
            "priceDisplay" to editedItem.price,
            "description" to editedItem.description
        )

        membershipTypesCollection.document(documentIds[position]).update(updates)
            .addOnSuccessListener {
                membershipItems[position] = editedItem
                adapter.notifyItemChanged(position)
                showToast("${editedItem.duration} updated successfully")
                recordPriceChangeNotification(editedItem)
            }
            .addOnFailureListener { e ->
                showToast("Update failed: ${e.message}")
            }
    }

    private fun recordPriceChangeNotification(editedItem: MembershipItem) {
        val notificationData = hashMapOf<String, Any>(
            "type" to "membership_update",
            "title" to "Membership Update",
            "content" to "A membership price has been updated. Check now!",
            "membershipName" to editedItem.duration,
            "newPrice" to editedItem.price,
            "timestamp" to FieldValue.serverTimestamp(),
            "read" to false
        )

        notificationsCollection.add(notificationData)
            .addOnFailureListener { e ->
                showToast("Failed to record notification: ${e.message}")
            }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        fun newInstance() = AdminMembershippriceFragment()
    }
}