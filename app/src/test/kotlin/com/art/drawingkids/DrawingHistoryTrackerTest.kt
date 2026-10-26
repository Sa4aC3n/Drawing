package com.art.drawingkids

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DrawingHistoryTrackerTest {

    private lateinit var tracker: DrawingHistoryTracker<String>

    @Before
    fun setUp() {
        tracker = DrawingHistoryTracker()
    }

    @Test
    fun testInitialState() {
        assertFalse(tracker.canUndo())
        assertFalse(tracker.canRedo())
        assertEquals(0, tracker.count())
        assertNull(tracker.undo())
        assertNull(tracker.redo())
    }

    @Test
    fun testRecordAndUndo() {
        tracker.record("Stroke 1")
        tracker.record("Stroke 2")

        assertTrue(tracker.canUndo())
        assertFalse(tracker.canRedo())
        assertEquals(2, tracker.count())

        val undone = tracker.undo()
        assertEquals("Stroke 2", undone)
        assertEquals(1, tracker.count())
        assertTrue(tracker.canUndo())
        assertTrue(tracker.canRedo())
    }

    @Test
    fun testRedoRestoresState() {
        tracker.record("Stroke A")
        tracker.undo()

        val redone = tracker.redo()
        assertEquals("Stroke A", redone)
        assertEquals(1, tracker.count())
        assertFalse(tracker.canRedo())
        assertTrue(tracker.canUndo())
    }

    @Test
    fun testNewActionClearsRedoStack() {
        tracker.record("Stroke 1")
        tracker.record("Stroke 2")
        tracker.undo() // undone Stroke 2, can redo

        assertTrue(tracker.canRedo())

        // Recording a new action must drop the redo branch
        tracker.record("Stroke 3")
        assertFalse(tracker.canRedo())
        assertEquals(2, tracker.count())

        val undone = tracker.undo()
        assertEquals("Stroke 3", undone)
    }

    @Test
    fun testClear() {
        tracker.record("Stroke 1")
        tracker.record("Stroke 2")
        tracker.clear()

        assertFalse(tracker.canUndo())
        assertFalse(tracker.canRedo())
        assertEquals(0, tracker.count())
    }
}
