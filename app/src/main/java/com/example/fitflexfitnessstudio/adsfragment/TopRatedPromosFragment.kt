package com.example.fitflexfitnessstudio.adsfragment
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitflexfitnessstudio.PromoAdapter
import com.example.fitflexfitnessstudio.PromoItem
import com.example.fitflexfitnessstudio.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration


// TopRatedPromosFragment.kt
class TopRatedPromosFragment : Fragment() {
    private lateinit var adapter: PromoAdapter
    private var firestoreListener: ListenerRegistration? = null
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_top_rated_promos, container, false)
        adapter = PromoAdapter()
        view.findViewById<RecyclerView>(R.id.promoRecyclerView).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@TopRatedPromosFragment.adapter
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadPromosFromFirestore()
    }

    private fun loadPromosFromFirestore() {
        if (!isAdded || isDetached) return

        firestoreListener?.remove()
        firestoreListener = db.collection("MembershipTypes")
            .orderBy("duration")
            .addSnapshotListener { snapshot, error ->
                if (!isAdded || isDetached) return@addSnapshotListener

                when {
                    error != null -> {
                        // Optional: Show error state in UI instead of Toast
                        // e.g., show empty state view or retry button
                    }
                    snapshot != null && !snapshot.isEmpty -> {
                        val items = snapshot.documents.mapNotNull { doc ->
                            PromoItem(
                                duration = doc.getString("name") ?: "",
                                price = doc.getString("priceDisplay") ?: "",
                                description = doc.getString("description") ?: ""
                            )
                        }
                        adapter.updatePromos(items)
                    }
                    else -> {
                        // Empty state - you might want to show a "No promotions available" view
                        adapter.updatePromos(emptyList())
                    }
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        firestoreListener?.remove()
    }
}