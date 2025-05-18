package com.example.fitflexfitnessstudio.admin

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.fitflexfitnessstudio.R
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream

class AdminPromoFragment : Fragment() {

    private lateinit var rvSlider: RecyclerView
    private lateinit var rvGrid: RecyclerView
    private lateinit var sliderAdapter: AdminSliderAdapter
    private lateinit var gridAdapter: AdminGridPromoAdapter

    private var currentUploadTarget: String? = null // "slider" or "grid"
    private val db = FirebaseFirestore.getInstance()
    private val sliderItems = mutableListOf<String>() // Base64 strings
    private val gridItems = mutableListOf<String>() // Base64 strings
    private val sliderDocIds = mutableListOf<String>()
    private val gridDocIds = mutableListOf<String>()

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                showUploadConfirmationDialog(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_admin_promo, container, false)

        rvSlider = view.findViewById(R.id.rv_slider)
        rvGrid = view.findViewById(R.id.rv_grid)

        // Setup slider RecyclerView
        sliderAdapter = AdminSliderAdapter(sliderItems) { position ->
            showDeleteConfirmationDialog(position, sliderDocIds[position], "sliders")
        }
        rvSlider.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rvSlider.adapter = sliderAdapter
        PagerSnapHelper().attachToRecyclerView(rvSlider)

        // Setup grid RecyclerView
        gridAdapter = AdminGridPromoAdapter(gridItems) { position ->
            showDeleteConfirmationDialog(position, gridDocIds[position], "gridspromo")
        }
        rvGrid.layoutManager = GridLayoutManager(requireContext(), 2)
        rvGrid.adapter = gridAdapter

        // Set up upload buttons
        view.findViewById<ImageView>(R.id.ic_upload_slider).setOnClickListener {
            currentUploadTarget = "slider"
            openImagePicker()
        }

        view.findViewById<ImageView>(R.id.ic_upload_grid).setOnClickListener {
            currentUploadTarget = "grid"
            openImagePicker()
        }

        loadPromoData()

        return view
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun showUploadConfirmationDialog(uri: Uri) {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_upload_confirmation, null)

        val imagePreview = view.findViewById<ImageView>(R.id.iv_preview)
        imagePreview.setImageURI(uri)

        view.findViewById<View>(R.id.btn_save).setOnClickListener {
            dialog.dismiss()
            uploadImage(uri)
        }

        view.findViewById<View>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.setContentView(view)
        dialog.show()
    }

    private fun showDeleteConfirmationDialog(position: Int, documentId: String, collection: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Promo")
            .setMessage("Are you sure you want to delete this promo?")
            .setPositiveButton("Delete") { _, _ ->
                deleteImage(position, documentId, collection)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun uploadImage(uri: Uri) {
        try {
            val inputStream = requireActivity().contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            val compressedBitmap = compressImage(bitmap)
            val base64String = bitmapToBase64(compressedBitmap)

            when (currentUploadTarget) {
                "slider" -> {
                    db.collection("promotions").document("sliders")
                        .collection("items").add(hashMapOf("image" to base64String))
                        .addOnSuccessListener {
                            loadSliderData()
                            Toast.makeText(requireContext(), "Slider image uploaded", Toast.LENGTH_SHORT).show()
                        }
                }
                "grid" -> {
                    db.collection("promotions").document("gridspromo")
                        .collection("items").add(hashMapOf("image" to base64String))
                        .addOnSuccessListener {
                            loadGridData()
                            Toast.makeText(requireContext(), "Grid image uploaded", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error uploading image", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun deleteImage(position: Int, documentId: String, collection: String) {
        db.collection("promotions").document(collection)
            .collection("items").document(documentId)
            .delete()
            .addOnSuccessListener {
                when (collection) {
                    "sliders" -> {
                        sliderItems.removeAt(position)
                        sliderDocIds.removeAt(position)
                        sliderAdapter.notifyItemRemoved(position)
                    }
                    "gridspromo" -> {
                        gridItems.removeAt(position)
                        gridDocIds.removeAt(position)
                        gridAdapter.notifyItemRemoved(position)
                    }
                }
                Toast.makeText(requireContext(), "Promo deleted", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to delete promo", Toast.LENGTH_SHORT).show()
            }
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
                sliderDocIds.clear()
                for (document in documents) {
                    document.getString("image")?.let { sliderItems.add(it) }
                    sliderDocIds.add(document.id)
                }
                sliderAdapter.notifyDataSetChanged()
            }
    }

    private fun loadGridData() {
        db.collection("promotions").document("gridspromo")
            .collection("items").get()
            .addOnSuccessListener { documents ->
                gridItems.clear()
                gridDocIds.clear()
                for (document in documents) {
                    document.getString("image")?.let { gridItems.add(it) }
                    gridDocIds.add(document.id)
                }
                gridAdapter.notifyDataSetChanged()
            }
    }

    private fun compressImage(bitmap: Bitmap): Bitmap {
        val maxDimension = 1024
        val width = bitmap.width
        val height = bitmap.height

        val scale = when {
            width > height -> maxDimension.toFloat() / width
            else -> maxDimension.toFloat() / height
        }

        return Bitmap.createScaledBitmap(
            bitmap,
            (width * scale).toInt(),
            (height * scale).toInt(),
            true
        )
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    fun decompressImage(base64String: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = AdminPromoFragment()
    }
}