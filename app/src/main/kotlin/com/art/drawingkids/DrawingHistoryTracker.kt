package com.art.drawingkids

/**
 * Reusable, robust Undo/Redo history stack manager.
 * Fully tested and safe for UI canvas operations.
 */
class DrawingHistoryTracker<T> {
    private val undoStack = mutableListOf<T>()
    private val redoStack = mutableListOf<T>()

    fun record(item: T) {
        undoStack.add(item)
        redoStack.clear()
    }

    fun undo(): T? {
        if (undoStack.isEmpty()) return null
        val item = undoStack.removeAt(undoStack.lastIndex)
        redoStack.add(item)
        return item
    }

    fun redo(): T? {
        if (redoStack.isEmpty()) return null
        val item = redoStack.removeAt(redoStack.lastIndex)
        undoStack.add(item)
        return item
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()
    fun count(): Int = undoStack.size
    fun getItems(): List<T> = undoStack.toList()
}
