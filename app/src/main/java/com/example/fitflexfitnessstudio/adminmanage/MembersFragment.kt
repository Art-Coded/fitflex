package com.example.fitflexfitnessstudio.adminmanage

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitflexfitnessstudio.R
import com.example.fitflexfitnessstudio.databinding.FragmentMembersBinding
import com.example.fitflexfitnessstudio.databinding.ItemMemberBinding
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MembersFragment : Fragment() {

    private var _binding: FragmentMembersBinding? = null
    private val binding get() = _binding!!
    private val members = mutableListOf<Member>()
    private lateinit var adapter: MemberAdapter
    private val db = FirebaseFirestore.getInstance()
    private var membersListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMembersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadMembersFromFirestore()
        setupAddButton()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = MemberAdapter(members) { member ->
            showMemberDetails(member)
        }
        binding.recyclerView.adapter = adapter
    }

    private fun loadMembersFromFirestore() {
        membersListener = db.collection("users")
            .orderBy("fullName", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(requireContext(), "Error loading members", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                members.clear()
                snapshot?.documents?.forEach { document ->
                    val memberId = document.id
                    val basicInfo = document.toObject<Member>()?.copy(
                        id = memberId,
                        fullname = document.getString("fullName") ?: "",
                        email = document.getString("email") ?: "",
                        phone = document.getString("phone") ?: ""
                    ) ?: Member(id = memberId)

                    // Fetch memberships to calculate days left
                    db.collection("users").document(memberId)
                        .collection("memberships")
                        .orderBy("endDate", Query.Direction.DESCENDING)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { membershipSnapshot ->
                            val memberships = mutableListOf<Membership>()
                            var daysLeft = 0L

                            if (!membershipSnapshot.isEmpty) {
                                val latestMembershipDoc = membershipSnapshot.documents[0]
                                val endDate = latestMembershipDoc.getDate("endDate")
                                if (endDate != null) {
                                    val now = Date()
                                    daysLeft = if (endDate.after(now)) {
                                        TimeUnit.MILLISECONDS.toDays(endDate.time - now.time)
                                    } else {
                                        0L
                                    }
                                }

                                // Add all memberships for display
                                membershipSnapshot.documents.forEach { doc ->
                                    memberships.add(
                                        Membership(
                                            type = doc.getString("type") ?: "",
                                            endDate = doc.getDate("endDate") ?: Date(),
                                            durationDays = doc.getLong("durationDays")?.toInt() ?: 0,
                                            addedDate = doc.getDate("addedDate")
                                        )
                                    )
                                }
                            }

                            val member = basicInfo.copy(
                                daysLeft = daysLeft,
                                membershipType = if (daysLeft > 0) "Premium" else "Standard",
                                memberships = memberships
                            )

                            members.add(member)
                            adapter.notifyDataSetChanged()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Error loading memberships: ${e.message}", Toast.LENGTH_SHORT).show()
                            members.add(basicInfo.copy(daysLeft = 0, membershipType = "Standard"))
                            adapter.notifyDataSetChanged()
                        }
                }
            }
    }

    private fun setupAddButton() {
        binding.addButton.setOnClickListener {
            showAddMemberDialog()
        }
    }

    private fun showAddMemberDialog() {
        val dialog = Dialog(requireContext()).apply {
            setContentView(R.layout.dialog_renew_member)
            window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            window?.setBackgroundDrawableResource(android.R.color.white)
            setCancelable(true)
        }

        val nameAutoComplete = dialog.findViewById<AutoCompleteTextView>(R.id.nameAutoComplete)
        val membershipSpinner = dialog.findViewById<Spinner>(R.id.membershipSpinner)
        val cancelButton = dialog.findViewById<Button>(R.id.cancelButton)
        val addButton = dialog.findViewById<Button>(R.id.addButton)

        // Setup name suggestions
        val names = members.map { it.fullname }
        val nameAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, names)
        nameAutoComplete.setAdapter(nameAdapter)

        // Setup membership options with days mapping
        val membershipOptions = listOf(
            Pair("1 Month", 30),
            Pair("3 Months", 90),
            Pair("6 Months", 180),
            Pair("1 Year", 365)
        )

        val displayOptions = membershipOptions.map { it.first }
        val membershipAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            displayOptions
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        membershipSpinner.adapter = membershipAdapter

        cancelButton.setOnClickListener { dialog.dismiss() }
        addButton.setOnClickListener {
            val selectedName = nameAutoComplete.text.toString()
            val selectedOptionIndex = membershipSpinner.selectedItemPosition
            val selectedDuration = membershipOptions[selectedOptionIndex].second

            val member = members.find { it.fullname == selectedName }
            if (member != null) {
                addMembershipToUser(member.id, selectedDuration, displayOptions[selectedOptionIndex])
                dialog.dismiss()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Member not found",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        dialog.show()
    }

    private fun addMembershipToUser(userId: String, durationDays: Int, packageName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val endDate = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, durationDays)
                }.time

                // First add the new membership
                val membership = hashMapOf(
                    "type" to packageName,
                    "endDate" to endDate,
                    "durationDays" to durationDays,
                    "addedDate" to FieldValue.serverTimestamp()
                )

                db.collection("users").document(userId)
                    .collection("memberships")
                    .add(membership)
                    .await()

                // Then update the daysLeft field based on the new end date
                val now = Date()
                val daysLeft = if (endDate.after(now)) {
                    TimeUnit.MILLISECONDS.toDays(endDate.time - now.time)
                } else {
                    0L
                }

                db.collection("users").document(userId)
                    .update("daysLeft", daysLeft)
                    .await()

                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(
                        requireContext(),
                        "Membership added successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadMembersFromFirestore() // Refresh the list
                }
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(
                        requireContext(),
                        "Error adding membership: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showMemberDetails(member: Member) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("${member.fullname}'s Details")

        val details = buildString {
            append("Name: ${member.fullname}\n")
            append("Email: ${member.email}\n")
            append("Phone: ${member.phone}\n")
            append("Membership: ${member.membershipType}\n")
            append("Days Left: ${member.daysLeft}\n\n")

            if (member.memberships.isNotEmpty()) {
                append("Memberships:\n\n")
                member.memberships.forEach { membership ->
                    append("${membership.type}\n")
                    append("End Date: ${SimpleDateFormat("MMM dd, yyyy").format(membership.endDate)}\n")
                    append("Days Left: ${daysBetween(Date(), membership.endDate)}\n\n")
                }
            } else {
                append("No memberships found")
            }
        }

        builder.setMessage(details)
        builder.setPositiveButton("OK", null)
        builder.show()
    }

    private fun daysBetween(startDate: Date, endDate: Date): Long {
        val diffInMillis = endDate.time - startDate.time
        return TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        membersListener?.remove()
        _binding = null
    }

    data class Member(
        val id: String = "",
        val fullname: String = "",
        val email: String = "",
        val phone: String = "",
        val daysLeft: Long = 0,
        val membershipType: String = "Standard",
        val memberships: List<Membership> = emptyList()
    )

    data class Membership(
        val type: String = "",
        val endDate: Date = Date(),
        val durationDays: Int = 0,
        val addedDate: Date? = null
    )

    inner class MemberAdapter(
        private val members: List<Member>,
        private val onItemClick: (Member) -> Unit
    ) : RecyclerView.Adapter<MemberAdapter.MemberViewHolder>() {

        inner class MemberViewHolder(val binding: ItemMemberBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
            return MemberViewHolder(
                ItemMemberBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }

        override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
            with(holder.binding) {
                val member = members[position]
                nameTextView.text = member.fullname
                daysLeftTextView.text = "${member.daysLeft} days left"

                root.setOnClickListener {
                    onItemClick(member)
                }
            }
        }

        override fun getItemCount() = members.size
    }

    companion object {
        @JvmStatic
        fun newInstance() = MembersFragment()
    }
}