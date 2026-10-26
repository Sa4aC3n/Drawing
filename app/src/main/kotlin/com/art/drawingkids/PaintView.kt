package com.art.drawingkids

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * Brush modes for kid-friendly magic effects.
 */
enum class BrushMode(val titleResId: Int) {
    PENCIL(R.string.brush_pencil),
    ERASER(R.string.brush_eraser),
    NEON_GLOW(R.string.brush_neon),
    RAINBOW(R.string.brush_rainbow)
}

/**
 * Robust, production-ready custom drawing canvas view.
 * Features:
 * - Anti-aliased smooth bezier curves
 * - Multi-mode brush system: Classic Pencil, Clean Eraser, Neon Glow, Rainbow Magic
 * - Full Undo / Redo history powered by tested DrawingHistoryTracker
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
        val brushMode: BrushMode
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

    private val glowPaint = Paint().apply {
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
    private var currentBrushMode = BrushMode.PENCIL
    private var rainbowHue = 0f

    // Undo & Redo tracker
    private val historyTracker = DrawingHistoryTracker<Stroke>()

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
        for (stroke in historyTracker.getItems()) {
            drawSingleStroke(canvas, stroke)
        }
    }

    private fun drawSingleStroke(canvas: Canvas, stroke: Stroke) {
        when (stroke.brushMode) {
            BrushMode.ERASER -> {
                strokePaint.maskFilter = null
                strokePaint.color = CANVAS_BG_COLOR
                strokePaint.strokeWidth = stroke.strokeWidth * 1.5f
                canvas.drawPath(stroke.path, strokePaint)
            }
            BrushMode.NEON_GLOW -> {
                // Outer glowing blur
                glowPaint.color = stroke.color
                glowPaint.strokeWidth = stroke.strokeWidth * 1.8f
                glowPaint.maskFilter = BlurMaskFilter(stroke.strokeWidth * 0.8f, BlurMaskFilter.Blur.NORMAL)
                canvas.drawPath(stroke.path, glowPaint)

                // Inner core line
                strokePaint.maskFilter = null
                strokePaint.color = Color.WHITE
                strokePaint.strokeWidth = stroke.strokeWidth * 0.6f
                canvas.drawPath(stroke.path, strokePaint)
            }
            BrushMode.RAINBOW, BrushMode.PENCIL -> {
                strokePaint.maskFilter = null
                strokePaint.color = stroke.color
                strokePaint.strokeWidth = stroke.strokeWidth
                canvas.drawPath(stroke.path, strokePaint)
            }
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
            val effectiveWidth = if (currentBrushMode == BrushMode.ERASER) currentStrokeWidth * 1.5f else currentStrokeWidth
            val effectiveColor = when (currentBrushMode) {
                BrushMode.ERASER -> CANVAS_BG_COLOR
                BrushMode.RAINBOW -> Color.HSVToColor(floatArrayOf(rainbowHue, 0.95f, 1f))
                BrushMode.NEON_GLOW, BrushMode.PENCIL -> currentColor
            }

            if (currentBrushMode == BrushMode.NEON_GLOW) {
                glowPaint.color = effectiveColor
                glowPaint.strokeWidth = effectiveWidth * 1.8f
                glowPaint.maskFilter = BlurMaskFilter(effectiveWidth * 0.8f, BlurMaskFilter.Blur.NORMAL)
                canvas.drawPath(currentPath, glowPaint)

                currentPaint.maskFilter = null
                currentPaint.color = Color.WHITE
                currentPaint.strokeWidth = effectiveWidth * 0.6f
                canvas.drawPath(currentPath, currentPaint)
            } else {
                currentPaint.maskFilter = null
                currentPaint.color = effectiveColor
                currentPaint.strokeWidth = effectiveWidth
                canvas.drawPath(currentPath, currentPaint)
            }
        }
    }

    // ───────── Touch Handling with Quad Bezier Smoothing ─────────

    private fun touchStart(x: Float, y: Float) {
        currentPath.reset()
        currentPath.moveTo(x, y)
        lastX = x
        lastY = y
        if (currentBrushMode == BrushMode.RAINBOW) {
            rainbowHue = (rainbowHue + 15f) % 360f
        }
    }

    private fun touchMove(x: Float, y: Float) {
        val dx = Math.abs(x - lastX)
        val dy = Math.abs(y - lastY)
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            currentPath.quadTo(lastX, lastY, (x + lastX) / 2, (y + lastY) / 2)
            lastX = x
            lastY = y
            if (currentBrushMode == BrushMode.RAINBOW) {
                rainbowHue = (rainbowHue + 2f) % 360f
            }
        }
    }

    private fun touchUp() {
        currentPath.lineTo(lastX, lastY)

        val strokeColor = when (currentBrushMode) {
            BrushMode.ERASER -> CANVAS_BG_COLOR
            BrushMode.RAINBOW -> Color.HSVToColor(floatArrayOf(rainbowHue, 0.95f, 1f))
            BrushMode.NEON_GLOW, BrushMode.PENCIL -> currentColor
        }

        val completedStroke = Stroke(
            path = Path(currentPath),
            color = strokeColor,
            strokeWidth = currentStrokeWidth,
            brushMode = currentBrushMode
        )

        // Commit stroke to buffer canvas
        bufferCanvas?.let { canvas ->
            drawSingleStroke(canvas, completedStroke)
        }

        // Save completed stroke to history tracker
        historyTracker.record(completedStroke)
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

    fun setBrushMode(mode: BrushMode) {
        currentBrushMode = mode
    }

    fun getBrushMode(): BrushMode = currentBrushMode

    fun togglePencil(pencilMode: Boolean) {
        currentBrushMode = if (pencilMode) BrushMode.PENCIL else BrushMode.ERASER
    }

    fun isPencil(): Boolean = currentBrushMode != BrushMode.ERASER

    fun setDrawColor(color: Int) {
        currentColor = color
        if (currentBrushMode == BrushMode.ERASER) {
            currentBrushMode = BrushMode.PENCIL
        }
    }

    fun getColor(): Int = currentColor

    fun setStrokeWidth(width: Float) {
        currentStrokeWidth = width.coerceAtLeast(2f)
        currentPaint.strokeWidth = currentStrokeWidth
    }

    fun getStrokeWidth(): Float = currentStrokeWidth

    fun clear() {
        if (!hasDrawnStrokes() && currentPath.isEmpty) return
        historyTracker.clear()
        currentPath.reset()
        bufferCanvas?.drawColor(CANVAS_BG_COLOR)
        invalidate()
        notifyHistoryChanged()
    }

    fun undo(): Boolean {
        if (!historyTracker.canUndo()) return false
        historyTracker.undo()
        redrawAllStrokesToBuffer()
        invalidate()
        notifyHistoryChanged()
        return true
    }

    fun redo(): Boolean {
        if (!historyTracker.canRedo()) return false
        historyTracker.redo()
        redrawAllStrokesToBuffer()
        invalidate()
        notifyHistoryChanged()
        return true
    }

    fun canUndo(): Boolean = historyTracker.canUndo()
    fun canRedo(): Boolean = historyTracker.canRedo()

    fun hasDrawnStrokes(): Boolean = historyTracker.canUndo()

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
        for (stroke in historyTracker.getItems()) {
            drawSingleStroke(exportCanvas, stroke)
        }

        return exportBmp
    }
}
