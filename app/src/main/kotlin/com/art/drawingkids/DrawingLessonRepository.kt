package com.art.drawingkids

import android.content.Context

/**
 * Central repository managing the drawing lessons catalog.
 * Decouples lesson metadata from Activities and enables future dynamic/cloud pack loading.
 */
class DrawingLessonRepository(private val context: Context) {

    private val allLessons: List<DrawingCategory> by lazy {
        loadCatalog()
    }

    private fun loadCatalog(): List<DrawingCategory> {
        val names = context.resources.getStringArray(R.array.names)
        val descriptions = context.resources.getStringArray(R.array.descriptions)

        val metadata = listOf(
            LessonMeta("car", "image_car", R.drawable.car_icon, CategoryGroup.VEHICLES, 13),
            LessonMeta("house", "image_house", R.drawable.house_icon, CategoryGroup.NATURE, 16),
            LessonMeta("spider", "image_spider", R.drawable.spider_icon, CategoryGroup.ANIMALS, 14),
            LessonMeta("gun", "image_gun", R.drawable.gun_icon, CategoryGroup.NATURE, 10),
            LessonMeta("boat", "image_boat", R.drawable.boat_icon, CategoryGroup.VEHICLES, 13),
            LessonMeta("bird", "image_bird", R.drawable.bird_icon, CategoryGroup.ANIMALS, 11),
            LessonMeta("elephant", "image_elephant", R.drawable.elephant_icon, CategoryGroup.ANIMALS, 22),
            LessonMeta("flower", "image_flower", R.drawable.flower_icon, CategoryGroup.NATURE, 10),
            LessonMeta("frog", "image_frog", R.drawable.frog_icon, CategoryGroup.ANIMALS, 16),
            LessonMeta("horse", "image_horse", R.drawable.horse_icon, CategoryGroup.ANIMALS, 15),
            LessonMeta("lion", "image_lion", R.drawable.lion_icon, CategoryGroup.ANIMALS, 13),
            LessonMeta("mickey", "image_mickey_mouse", R.drawable.mickey_icon, CategoryGroup.CHARACTERS, 16),
            LessonMeta("dog", "image_dog", R.drawable.dog_icon, CategoryGroup.ANIMALS, 17),
            LessonMeta("turtle", "image_turtle", R.drawable.turtle_icon, CategoryGroup.ANIMALS, 25),
            LessonMeta("princess", "image_princess", R.drawable.princess_icon, CategoryGroup.CHARACTERS, 13)
        )

        return metadata.mapIndexed { index, meta ->
            val name = if (index < names.size) names[index] else meta.id.replaceFirstChar { it.uppercase() }
            val desc = if (index < descriptions.size) descriptions[index] else "${meta.steps} Steps"

            DrawingCategory(
                id = meta.id,
                name = name,
                description = desc,
                iconResId = meta.iconResId,
                imageArrayKey = meta.key,
                group = meta.group,
                stepsCount = meta.steps
            )
        }
    }

    private data class LessonMeta(
        val id: String,
        val key: String,
        val iconResId: Int,
        val group: CategoryGroup,
        val steps: Int
    )

    fun getLessons(group: CategoryGroup = CategoryGroup.ALL): List<DrawingCategory> {
        return if (group == CategoryGroup.ALL) {
            allLessons
        } else {
            allLessons.filter { it.group == group }
        }
    }

    fun searchLessons(query: String): List<DrawingCategory> {
        if (query.isBlank()) return allLessons
        val cleanQuery = query.trim().lowercase()
        return allLessons.filter {
            it.name.lowercase().contains(cleanQuery) || it.description.lowercase().contains(cleanQuery)
        }
    }

    fun getLessonById(id: String): DrawingCategory? {
        return allLessons.find { it.id == id || it.imageArrayKey == id }
    }

    fun getTotalLessonsCount(): Int = allLessons.size
}
