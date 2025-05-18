package com.example.fitflexfitnessstudio.admin.promo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.fitflexfitnessstudio.R
import com.example.fitflexfitnessstudio.admin.AdminPromoFragment

class GridPromoAdapter(private val items: List<String>) : RecyclerView.Adapter<GridPromoAdapter.GridViewHolder>() {

    class GridViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.iv_grid_item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GridViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_grid_promo, parent, false)
        return GridViewHolder(view)
    }

    override fun onBindViewHolder(holder: GridViewHolder, position: Int) {
        val bitmap = AdminPromoFragment().decompressImage(items[position])
        holder.imageView.setImageBitmap(bitmap)
    }

    override fun getItemCount() = items.size
}