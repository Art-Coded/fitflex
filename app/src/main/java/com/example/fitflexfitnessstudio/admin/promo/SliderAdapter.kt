package com.example.fitflexfitnessstudio.admin.promo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.fitflexfitnessstudio.R
import com.example.fitflexfitnessstudio.admin.AdminPromoFragment

class SliderAdapter(private val items: List<String>) : RecyclerView.Adapter<SliderAdapter.SliderViewHolder>() {

    class SliderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.iv_slide)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SliderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_slider, parent, false)
        return SliderViewHolder(view)
    }

    override fun onBindViewHolder(holder: SliderViewHolder, position: Int) {
        val bitmap = AdminPromoFragment().decompressImage(items[position])
        holder.imageView.setImageBitmap(bitmap)
    }

    override fun getItemCount() = items.size
}