package com.art.drawingkids

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryGroupTest {

    @Test
    fun testCategoryGroupEnumValues() {
        val groups = CategoryGroup.values()
        assertEquals(5, groups.size)
        assertTrue(groups.contains(CategoryGroup.ALL))
        assertTrue(groups.contains(CategoryGroup.ANIMALS))
        assertTrue(groups.contains(CategoryGroup.VEHICLES))
        assertTrue(groups.contains(CategoryGroup.CHARACTERS))
        assertTrue(groups.contains(CategoryGroup.NATURE))
    }

    @Test
    fun testCategoryGroupResourceIds() {
        for (group in CategoryGroup.values()) {
            assertTrue("Resource ID should be valid non-zero", group.titleResId != 0)
        }
    }
}
