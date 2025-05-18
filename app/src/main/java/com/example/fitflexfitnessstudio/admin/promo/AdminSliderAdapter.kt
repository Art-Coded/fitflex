package com.example.fitflexfitnessstudio.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.fitflexfitnessstudio.R

class AdminSliderAdapter(
    private val items: List<String>,
    private val onDeleteClick: (position: Int) -> Unit
) : RecyclerView.Adapter<AdminSliderAdapter.SliderViewHolder>() {

    class SliderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.iv_slide)
        val deleteButton: ImageView = view.findViewById(R.id.iv_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SliderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_slider, parent, false)
        return SliderViewHolder(view)
    }

    override fun onBindViewHolder(holder: SliderViewHolder, position: Int) {
        val bitmap = AdminPromoFragment().decompressImage(items[position])
        holder.imageView.setImageBitmap(bitmap)

        holder.deleteButton.setOnClickListener {
            onDeleteClick(position)
        }
    }

    override fun getItemCount() = items.size
}