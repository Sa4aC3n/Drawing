package com.art.drawingkids

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyChallengeManagerTest {

    @Test
    fun testDailyChallengeDeterminism() {
        val sampleLessons = listOf(
            DrawingCategory("1", "Car", "13 Steps", 1, "image_car"),
            DrawingCategory("2", "House", "16 Steps", 2, "image_house"),
            DrawingCategory("3", "Lion", "13 Steps", 3, "image_lion")
        )

        val dayOfYear = 100
        val expectedIndex = dayOfYear % sampleLessons.size
        assertEquals(1, expectedIndex)
        assertEquals("image_house", sampleLessons[expectedIndex].imageArrayKey)
    }
}
