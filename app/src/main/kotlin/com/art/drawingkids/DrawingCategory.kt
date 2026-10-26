package com.art.drawingkids

/**
 * Data class representing a drawing category shown in the main list.
 */
data class DrawingCategory(
    val name: String,
    val description: String,
    val iconResId: Int,
    val imageArrayKey: String
)
