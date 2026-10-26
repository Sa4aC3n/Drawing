package com.art.drawingkids

/**
 * Filter groups for organizing the expanding drawing catalog.
 */
enum class CategoryGroup(val titleResId: Int) {
    ALL(R.string.group_all),
    ANIMALS(R.string.group_animals),
    VEHICLES(R.string.group_vehicles),
    CHARACTERS(R.string.group_characters),
    NATURE(R.string.group_nature)
}

/**
 * Data model representing a step-by-step drawing lesson.
 */
data class DrawingCategory(
    val id: String,
    val name: String,
    val description: String,
    val iconResId: Int,
    val imageArrayKey: String,
    val group: CategoryGroup = CategoryGroup.ALL,
    val stepsCount: Int = 10
)
