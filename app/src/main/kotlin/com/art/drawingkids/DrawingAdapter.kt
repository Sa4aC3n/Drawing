package com.art.drawingkids

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * RecyclerView adapter for displaying drawing categories in MainActivity.
 */
class DrawingAdapter(
    private val categories: List<DrawingCategory>,
    private val onItemClick: (DrawingCategory) -> Unit
) : RecyclerView.Adapter<DrawingAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.item_image)
        val title: TextView = view.findViewById(R.id.item_title)
        val description: TextView = view.findViewById(R.id.item_description)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_drawing_category, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = categories[position]
        holder.title.text = category.name
        holder.description.text = category.description
        holder.icon.setImageResource(category.iconResId)
        holder.itemView.setOnClickListener { onItemClick(category) }
    }

    override fun getItemCount(): Int = categories.size
}
