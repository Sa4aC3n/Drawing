package com.art.drawingkids

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * Robust, production-ready custom drawing canvas view.
 * Features:
 * - Anti-aliased smooth bezier curves
 * - High-precision pencil and eraser modes
 * - Full Undo / Redo history
 * - Direct bitmap export without deprecated drawingCache
 * - Clean white canvas without hardware-acceleration artifacts
 */
class PaintView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class Stroke(
        val path: Path,
        val color: Int,
        val strokeWidth: Float,
        val isEraser: Boolean
    )

    companion object {
        private const val TOUCH_TOLERANCE = 2f
        const val CANVAS_BG_COLOR = Color.WHITE
    }

    // Paint configurations
    private val currentPaint = Paint().apply {
        isAntiAlias = true
        isDither = true
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 6f
    }

    private val strokePaint = Paint().apply {
        isAntiAlias = true
        isDither = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    // Drawing state
    private val currentPath = Path()
    private var lastX = 0f
    private var lastY = 0f
    private var currentColor = Color.BLACK
    private var currentStrokeWidth = 6f
    private var isPencilMode = true

    // Undo & Redo stacks
    private val undoStack = mutableListOf<Stroke>()
    private val redoStack = mutableListOf<Stroke>()

    // Off-screen buffer for buttery smooth rendering
    private var bufferBitmap: Bitmap? = null
    private var bufferCanvas: Canvas? = null
    private val bufferPaint = Paint(Paint.DITHER_FLAG)

    // Listener for stroke changes (to update UI buttons like Undo)
    var onHistoryChangedListener: ((canUndo: Boolean, canRedo: Boolean) -> Unit)? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            val newBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val newCanvas = Canvas(newBmp)
            newCanvas.drawColor(CANVAS_BG_COLOR)
            bufferBitmap = newBmp
            bufferCanvas = newCanvas
            redrawAllStrokesToBuffer()
        }
    }

    private fun redrawAllStrokesToBuffer() {
        val canvas = bufferCanvas ?: return
        canvas.drawColor(CANVAS_BG_COLOR)
        for (stroke in undoStack) {
            strokePaint.color = stroke.color
            strokePaint.strokeWidth = stroke.strokeWidth
            canvas.drawPath(stroke.path, strokePaint)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Draw background and committed strokes
        bufferBitmap?.let {
            canvas.drawBitmap(it, 0f, 0f, bufferPaint)
        }

        // Draw the stroke currently in progress
        if (!currentPath.isEmpty) {
            currentPaint.color = if (isPencilMode) currentColor else CANVAS_BG_COLOR
            currentPaint.strokeWidth = currentStrokeWidth
            canvas.drawPath(currentPath, currentPaint)
        }
    }

    // ───────── Touch Handling with Quad Bezier Smoothing ─────────

    private fun touchStart(x: Float, y: Float) {
        currentPath.reset()
        currentPath.moveTo(x, y)
        lastX = x
        lastY = y
    }

    private fun touchMove(x: Float, y: Float) {
        val dx = Math.abs(x - lastX)
        val dy = Math.abs(y - lastY)
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            currentPath.quadTo(lastX, lastY, (x + lastX) / 2, (y + lastY) / 2)
            lastX = x
            lastY = y
        }
    }

    private fun touchUp() {
        currentPath.lineTo(lastX, lastY)

        // Commit stroke to buffer canvas
        val color = if (isPencilMode) currentColor else CANVAS_BG_COLOR
        strokePaint.color = color
        strokePaint.strokeWidth = currentStrokeWidth

        bufferCanvas?.drawPath(currentPath, strokePaint)

        // Save completed stroke to undo stack
        undoStack.add(
            Stroke(
                path = Path(currentPath),
                color = color,
                strokeWidth = currentStrokeWidth,
                isEraser = !isPencilMode
            )
        )
        redoStack.clear()
        currentPath.reset()

        notifyHistoryChanged()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStart(x, y)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                touchMove(x, y)
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                touchUp()
                invalidate()
            }
            MotionEvent.ACTION_CANCEL -> {
                currentPath.reset()
                invalidate()
            }
        }
        return true
    }

    // ───────── Public Control APIs ─────────

    fun togglePencil(pencilMode: Boolean) {
        isPencilMode = pencilMode
        if (pencilMode) {
            currentPaint.color = currentColor
        } else {
            currentPaint.color = CANVAS_BG_COLOR
        }
    }

    fun isPencil(): Boolean = isPencilMode

    fun setDrawColor(color: Int) {
        currentColor = color
        togglePencil(true)
    }

    fun getColor(): Int = currentColor

    fun setStrokeWidth(width: Float) {
        currentStrokeWidth = width.coerceAtLeast(2f)
        currentPaint.strokeWidth = currentStrokeWidth
    }

    fun getStrokeWidth(): Float = currentStrokeWidth

    fun clear() {
        if (undoStack.isEmpty() && currentPath.isEmpty) return
        undoStack.clear()
        redoStack.clear()
        currentPath.reset()
        bufferCanvas?.drawColor(CANVAS_BG_COLOR)
        invalidate()
        notifyHistoryChanged()
    }

    fun undo(): Boolean {
        if (undoStack.isEmpty()) return false
        val removed = undoStack.removeAt(undoStack.lastIndex)
        redoStack.add(removed)
        redrawAllStrokesToBuffer()
        invalidate()
        notifyHistoryChanged()
        return true
    }

    fun redo(): Boolean {
        if (redoStack.isEmpty()) return false
        val restored = redoStack.removeAt(redoStack.lastIndex)
        undoStack.add(restored)
        redrawAllStrokesToBuffer()
        invalidate()
        notifyHistoryChanged()
        return true
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun hasDrawnStrokes(): Boolean = undoStack.isNotEmpty()

    private fun notifyHistoryChanged() {
        onHistoryChangedListener?.invoke(canUndo(), canRedo())
    }

    /**
     * Exports a pristine, high-resolution Bitmap of the canvas contents.
     * Guaranteed non-null and independent of deprecated getDrawingCache.
     */
    fun exportBitmap(): Bitmap {
        val w = if (width > 0) width else 800
        val h = if (height > 0) height else 800
        val exportBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val exportCanvas = Canvas(exportBmp)

        // Draw solid background
        exportCanvas.drawColor(CANVAS_BG_COLOR)

        // Draw all saved strokes
        for (stroke in undoStack) {
            strokePaint.color = stroke.color
            strokePaint.strokeWidth = stroke.strokeWidth
            exportCanvas.drawPath(stroke.path, strokePaint)
        }

        return exportBmp
    }
}
