package com.example.fitflexfitnessstudio.adsfragment

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.fitflexfitnessstudio.R
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Base64
import android.graphics.BitmapFactory

class AllPromosFragment : Fragment() {

    private lateinit var rvSlider: RecyclerView
    private lateinit var rvGrid: RecyclerView
    private lateinit var sliderAdapter: SliderAdapter
    private lateinit var gridAdapter: GridPromoAdapter

    private val db = FirebaseFirestore.getInstance()
    private val sliderItems = mutableListOf<String>() // Base64 strings
    private val gridItems = mutableListOf<String>() // Base64 strings

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_all_promos, container, false)

        rvSlider = view.findViewById(R.id.rv_slider)
        rvGrid = view.findViewById(R.id.rv_grid)

        // Setup slider RecyclerView
        sliderAdapter = SliderAdapter(sliderItems)
        rvSlider.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rvSlider.adapter = sliderAdapter
        PagerSnapHelper().attachToRecyclerView(rvSlider) // For snap effect

        // Setup grid RecyclerView
        gridAdapter = GridPromoAdapter(gridItems)
        rvGrid.layoutManager = GridLayoutManager(requireContext(), 2)
        rvGrid.adapter = gridAdapter

        loadPromoData()

        return view
    }

    private fun loadPromoData() {
        loadSliderData()
        loadGridData()
    }

    private fun loadSliderData() {
        db.collection("promotions").document("sliders")
            .collection("items").get()
            .addOnSuccessListener { documents ->
                sliderItems.clear()
                for (document in documents) {
                    document.getString("image")?.let { sliderItems.add(it) }
                }
                sliderAdapter.notifyDataSetChanged()
            }
    }

    private fun loadGridData() {
        db.collection("promotions").document("gridspromo")
            .collection("items").get()
            .addOnSuccessListener { documents ->
                gridItems.clear()
                for (document in documents) {
                    document.getString("image")?.let { gridItems.add(it) }
                }
                gridAdapter.notifyDataSetChanged()
            }
    }

    private fun decompressImage(base64String: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Inner adapter classes
    inner class SliderAdapter(private val items: List<String>) : RecyclerView.Adapter<SliderAdapter.SliderViewHolder>() {

        inner class SliderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imageView: ImageView = view.findViewById(R.id.iv_slide)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SliderViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_slider, parent, false)
            return SliderViewHolder(view)
        }

        override fun onBindViewHolder(holder: SliderViewHolder, position: Int) {
            val bitmap = decompressImage(items[position])
            holder.imageView.setImageBitmap(bitmap)
        }

        override fun getItemCount() = items.size
    }

    inner class GridPromoAdapter(private val items: List<String>) : RecyclerView.Adapter<GridPromoAdapter.GridViewHolder>() {

        inner class GridViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imageView: ImageView = view.findViewById(R.id.iv_grid_item)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GridViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_grid_promo, parent, false)
            return GridViewHolder(view)
        }

        override fun onBindViewHolder(holder: GridViewHolder, position: Int) {
            val bitmap = decompressImage(items[position])
            holder.imageView.setImageBitmap(bitmap)
        }

        override fun getItemCount() = items.size
    }
}