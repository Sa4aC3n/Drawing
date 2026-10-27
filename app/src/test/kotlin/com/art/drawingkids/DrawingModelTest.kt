package com.art.drawingkids

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawingModelTest {

    @Test
    fun testDrawingCategoryCreation() {
        val category = DrawingCategory(
            id = "car",
            name = "Car",
            description = "13 Steps",
            iconResId = 12345,
            imageArrayKey = "image_car"
        )

        assertEquals("car", category.id)
        assertEquals("Car", category.name)
        assertEquals("13 Steps", category.description)
        assertEquals(12345, category.iconResId)
        assertEquals("image_car", category.imageArrayKey)
    }

    @Test
    fun testCategoryListIntegrity() {
        val keys = listOf(
            "image_car", "image_house", "image_spider", "image_gun",
            "image_boat", "image_bird", "image_elephant", "image_flower",
            "image_frog", "image_horse", "image_lion", "image_mickey_mouse",
            "image_dog", "image_turtle", "image_princess"
        )

        assertEquals("Should have exactly 15 default categories", 15, keys.size)
        assertTrue(keys.contains("image_car"))
        assertTrue(keys.contains("image_princess"))
    }
}
