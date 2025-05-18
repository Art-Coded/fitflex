package com.example.fitflexfitnessstudio.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.fitflexfitnessstudio.R


class AdminGridPromoAdapter(
    private val items: List<String>,
    private val onDeleteClick: (position: Int) -> Unit
) : RecyclerView.Adapter<AdminGridPromoAdapter.GridViewHolder>() {

    class GridViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.iv_grid_item)
        val deleteButton: ImageView = view.findViewById(R.id.iv_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GridViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_grid_promo, parent, false)
        return GridViewHolder(view)
    }

    override fun onBindViewHolder(holder: GridViewHolder, position: Int) {
        val bitmap = AdminPromoFragment().decompressImage(items[position])
        holder.imageView.setImageBitmap(bitmap)

        holder.deleteButton.setOnClickListener {
            onDeleteClick(position)
        }
    }

    override fun getItemCount() = items.size
}