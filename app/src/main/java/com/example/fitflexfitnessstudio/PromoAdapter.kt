package com.example.fitflexfitnessstudio

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// PromoAdapter.kt
class PromoAdapter : RecyclerView.Adapter<PromoAdapter.PromoViewHolder>() {
    private val promoItems = mutableListOf<PromoItem>()

    fun updatePromos(newItems: List<PromoItem>) {
        promoItems.clear()
        promoItems.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PromoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_promo_banner, parent, false)
        return PromoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PromoViewHolder, position: Int) {
        val item = promoItems[position]
        holder.tvDuration.text = item.duration
        holder.tvPrice.text = item.price
        holder.tvDescription.text = item.description
    }

    override fun getItemCount() = promoItems.size

    class PromoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
        val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
    }
}