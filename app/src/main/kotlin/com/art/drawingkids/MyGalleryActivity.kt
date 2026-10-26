package com.art.drawingkids

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class MyGalleryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var txtEmpty: TextView
    private lateinit var storageManager: DrawingStorageManager
    private lateinit var adapter: GalleryAdapter
    private var drawingFiles = mutableListOf<File>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_gallery)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        storageManager = DrawingStorageManager(this)
        recyclerView = findViewById(R.id.recyclerViewGallery)
        txtEmpty = findViewById(R.id.txtEmptyGallery)

        recyclerView.layoutManager = GridLayoutManager(this, 2)
        adapter = GalleryAdapter(drawingFiles,
            onShare = { file ->
                val result = storageManager.prepareShareIntentForFile(file, getString(R.string.share_image))
                if (result.success && result.shareIntent != null) {
                    startActivity(result.shareIntent)
                } else {
                    Toast.makeText(this, getString(R.string.sharing_error), Toast.LENGTH_SHORT).show()
                }
            },
            onDelete = { file, position ->
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.delete_drawing))
                    .setMessage(getString(R.string.delete_drawing_confirm))
                    .setPositiveButton(getString(R.string.delete)) { _, _ ->
                        if (storageManager.deleteDrawing(file)) {
                            drawingFiles.removeAt(position)
                            adapter.notifyItemRemoved(position)
                            checkEmptyState()
                            Toast.makeText(this, getString(R.string.deleted_success), Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show()
            }
        )
        recyclerView.adapter = adapter

        loadDrawings()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun loadDrawings() {
        drawingFiles.clear()
        drawingFiles.addAll(storageManager.getSavedDrawings())
        adapter.notifyDataSetChanged()
        checkEmptyState()
    }

    private fun checkEmptyState() {
        if (drawingFiles.isEmpty()) {
            txtEmpty.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            txtEmpty.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    class GalleryAdapter(
        private val files: List<File>,
        private val onShare: (File) -> Unit,
        private val onDelete: (File, Int) -> Unit
    ) : RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val thumbnail: ImageView = view.findViewById(R.id.imgThumbnail)
            val btnShare: Button = view.findViewById(R.id.btnShare)
            val btnDelete: Button = view.findViewById(R.id.btnDelete)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_gallery_drawing, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val file = files[position]
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            holder.thumbnail.setImageBitmap(bitmap)

            holder.btnShare.setOnClickListener { onShare(file) }
            holder.btnDelete.setOnClickListener { onDelete(file, holder.adapterPosition) }
        }

        override fun getItemCount(): Int = files.size
    }
}
