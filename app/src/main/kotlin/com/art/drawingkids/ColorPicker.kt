package com.art.drawingkids

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ComposeShader
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * HSV colour-wheel + value-slider picker widget.
 * Ported 1:1 from the original ColorPicker.java.
 */
class ColorPicker @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val paramOuterPadding = 2
    private val paramInnerPadding = 5
    private val paramValueSliderWidth = 10
    private val paramArrowPointerSize = 4

    private val colorWheelPaint = Paint().apply { isAntiAlias = true; isDither = true }
    private val valueSliderPaint = Paint().apply { isAntiAlias = true; isDither = true }
    private val colorViewPaint = Paint().apply { isAntiAlias = true }
    private val colorPointerPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        setARGB(128, 0, 0, 0)
    }
    private val valuePointerPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val valuePointerArrowPaint = Paint()

    private val outerWheelRect = RectF()
    private val innerWheelRect = RectF()
    private val colorPointerCoords = RectF()
    private val colorViewPath = Path()
    private val valueSliderPath = Path()
    private val arrowPointerPath = Path()

    private var colorWheelBitmap: Bitmap? = null
    private var valueSliderWidth = 0
    private var innerPadding = 0
    private var outerPadding = 0
    private var arrowPointerSize = 0
    private var outerWheelRadius = 0
    private var innerWheelRadius = 0
    private var colorWheelRadius = 0
    private var gradientRotationMatrix: Matrix? = null

    /** Currently selected colour in HSV */
    private var colorHSV = floatArrayOf(0f, 0f, 1f)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = minOf(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec))
        setMeasuredDimension(size, size)
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        val cx = width / 2
        val cy = height / 2

        colorWheelBitmap?.let { canvas.drawBitmap(it, (cx - colorWheelRadius).toFloat(), (cy - colorWheelRadius).toFloat(), null) }

        colorViewPaint.color = Color.HSVToColor(colorHSV)
        canvas.drawPath(colorViewPath, colorViewPaint)

        val hsv = floatArrayOf(colorHSV[0], colorHSV[1], 1f)
        val sweep = SweepGradient(cx.toFloat(), cy.toFloat(),
            intArrayOf(Color.BLACK, Color.HSVToColor(hsv), Color.WHITE), null)
        gradientRotationMatrix?.let { sweep.setLocalMatrix(it) }
        valueSliderPaint.shader = sweep
        canvas.drawPath(valueSliderPath, valueSliderPaint)

        val hueAngle = Math.toRadians(colorHSV[0].toDouble())
        val cpx = (-Math.cos(hueAngle) * colorHSV[1] * colorWheelRadius + cx).toInt()
        val cpy = (-Math.sin(hueAngle) * colorHSV[1] * colorWheelRadius + cy).toInt()
        val pointerRadius = 0.075f * colorWheelRadius
        colorPointerCoords.set(
            cpx - pointerRadius / 2, cpy - pointerRadius / 2,
            cpx + pointerRadius / 2, cpy + pointerRadius / 2
        )
        canvas.drawOval(colorPointerCoords, colorPointerPaint)

        valuePointerPaint.color = Color.HSVToColor(floatArrayOf(0f, 0f, 1f - colorHSV[2]))
        val valueAngle = (colorHSV[2] - 0.5f) * Math.PI
        val vax = Math.cos(valueAngle).toFloat()
        val vay = Math.sin(valueAngle).toFloat()
        canvas.drawLine(
            vax * innerWheelRadius + cx, vay * innerWheelRadius + cy,
            vax * outerWheelRadius + cx, vay * outerWheelRadius + cy,
            valuePointerPaint
        )

        if (arrowPointerSize > 0) drawPointerArrow(canvas)
    }

    private fun drawPointerArrow(canvas: Canvas) {
        val cx = width / 2
        val cy = height / 2
        val tipAngle = (colorHSV[2] - 0.5f) * Math.PI
        val leftAngle = tipAngle + Math.PI / 96
        val rightAngle = tipAngle - Math.PI / 96

        arrowPointerPath.reset()
        arrowPointerPath.moveTo(
            (Math.cos(tipAngle) * outerWheelRadius + cx).toFloat(),
            (Math.sin(tipAngle) * outerWheelRadius + cy).toFloat()
        )
        arrowPointerPath.lineTo(
            (Math.cos(leftAngle) * (outerWheelRadius + arrowPointerSize) + cx).toFloat(),
            (Math.sin(leftAngle) * (outerWheelRadius + arrowPointerSize) + cy).toFloat()
        )
        arrowPointerPath.lineTo(
            (Math.cos(rightAngle) * (outerWheelRadius + arrowPointerSize) + cx).toFloat(),
            (Math.sin(rightAngle) * (outerWheelRadius + arrowPointerSize) + cy).toFloat()
        )
        arrowPointerPath.close()

        valuePointerArrowPaint.color = Color.HSVToColor(colorHSV)
        valuePointerArrowPaint.style = Paint.Style.FILL
        canvas.drawPath(arrowPointerPath, valuePointerArrowPaint)

        valuePointerArrowPaint.style = Paint.Style.STROKE
        valuePointerArrowPaint.strokeJoin = Paint.Join.ROUND
        valuePointerArrowPaint.color = Color.BLACK
        canvas.drawPath(arrowPointerPath, valuePointerArrowPaint)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val cx = w / 2
        val cy = h / 2

        innerPadding = (paramInnerPadding * w / 100)
        outerPadding = (paramOuterPadding * w / 100)
        arrowPointerSize = (paramArrowPointerSize * w / 100)
        valueSliderWidth = (paramValueSliderWidth * w / 100)

        outerWheelRadius = w / 2 - outerPadding - arrowPointerSize
        innerWheelRadius = outerWheelRadius - valueSliderWidth
        colorWheelRadius = innerWheelRadius - innerPadding

        outerWheelRect.set(
            (cx - outerWheelRadius).toFloat(), (cy - outerWheelRadius).toFloat(),
            (cx + outerWheelRadius).toFloat(), (cy + outerWheelRadius).toFloat()
        )
        innerWheelRect.set(
            (cx - innerWheelRadius).toFloat(), (cy - innerWheelRadius).toFloat(),
            (cx + innerWheelRadius).toFloat(), (cy + innerWheelRadius).toFloat()
        )

        colorWheelBitmap = createColorWheelBitmap(colorWheelRadius * 2, colorWheelRadius * 2)
        gradientRotationMatrix = Matrix().apply { preRotate(270f, (w / 2).toFloat(), (h / 2).toFloat()) }

        colorViewPath.arcTo(outerWheelRect, 270f, -180f)
        colorViewPath.arcTo(innerWheelRect, 90f, 180f)
        valueSliderPath.arcTo(outerWheelRect, 270f, 180f)
        valueSliderPath.arcTo(innerWheelRect, 90f, -180f)
    }

    private fun createColorWheelBitmap(w: Int, h: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val colorCount = 12
        val step = 360 / colorCount
        val colors = IntArray(colorCount + 1)
        val hsv = floatArrayOf(0f, 1f, 1f)
        for (i in colors.indices) {
            hsv[0] = ((i * step + 180) % 360).toFloat()
            colors[i] = Color.HSVToColor(hsv)
        }
        colors[colorCount] = colors[0]

        val sweep = SweepGradient((w / 2).toFloat(), (h / 2).toFloat(), colors, null)
        val radial = RadialGradient(
            (w / 2).toFloat(), (h / 2).toFloat(), colorWheelRadius.toFloat(),
            0xFFFFFFFF.toInt(), 0x00FFFFFF, Shader.TileMode.CLAMP
        )
        colorWheelPaint.shader = ComposeShader(sweep, radial, PorterDuff.Mode.SRC_OVER)
        val canvas = Canvas(bitmap)
        canvas.drawCircle((w / 2).toFloat(), (h / 2).toFloat(), colorWheelRadius.toFloat(), colorWheelPaint)
        return bitmap
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.action
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
            val x = event.x.toInt()
            val y = event.y.toInt()
            val cx = x - width / 2
            val cy = y - height / 2
            val d = Math.sqrt((cx * cx + cy * cy).toDouble())

            if (d <= colorWheelRadius) {
                colorHSV[0] = (Math.toDegrees(Math.atan2(cy.toDouble(), cx.toDouble())) + 180f).toFloat()
                colorHSV[1] = maxOf(0f, minOf(1f, (d / colorWheelRadius).toFloat()))
                invalidate()
            } else if (x >= width / 2 && d >= innerWheelRadius) {
                colorHSV[2] = maxOf(0.0, minOf(1.0, Math.atan2(cy.toDouble(), cx.toDouble()) / Math.PI + 0.5)).toFloat()
                invalidate()
            }
            return true
        }
        return super.onTouchEvent(event)
    }

    fun setColor(color: Int) {
        Color.colorToHSV(color, colorHSV)
    }

    fun getColor(): Int = Color.HSVToColor(colorHSV)

    override fun onSaveInstanceState(): Parcelable {
        val state = Bundle()
        state.putFloatArray("color", colorHSV)
        state.putParcelable("super", super.onSaveInstanceState())
        return state
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is Bundle) {
            colorHSV = state.getFloatArray("color") ?: colorHSV
            @Suppress("DEPRECATION")
            super.onRestoreInstanceState(state.getParcelable("super"))
        } else {
            super.onRestoreInstanceState(state)
        }
    }
}
