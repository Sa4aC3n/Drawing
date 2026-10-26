package com.art.drawingkids

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawingStorageManagerTest {

    @Test
    fun testGenerateFileNameSanitization() {
        val fileName = DrawingStorageManager.generateFileName("image_car")
        assertTrue("Filename should start with Drawing_", fileName.startsWith("Drawing_image_car_"))
        assertTrue("Filename should end with .jpg", fileName.endsWith(".jpg"))
        assertFalse("Filename should not contain spaces", fileName.contains(" "))
    }

    @Test
    fun testGenerateFileNameWithSpecialCharacters() {
        val fileName = DrawingStorageManager.generateFileName("image@#_dino$ 123")
        assertTrue("Filename should strip special characters", fileName.startsWith("Drawing_image_dino123_"))
        assertTrue("Filename should end with .jpg", fileName.endsWith(".jpg"))
    }
}
