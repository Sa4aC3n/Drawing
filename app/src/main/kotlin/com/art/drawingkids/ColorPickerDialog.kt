package com.art.drawingkids

import android.content.Context
import android.view.Gravity
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog

/**
 * Dialog wrapping the ColorPicker wheel with responsive sizing and Material styling.
 */
class ColorPickerDialog(
    context: Context,
    initialColor: Int,
    private val onColorSelected: (Int) -> Unit
) : AlertDialog(context) {

    private val colorPickerView: ColorPicker

    init {
        setTitle(context.getString(R.string.color_picker))

        val density = context.resources.displayMetrics.density
        val pickerSize = (260 * density).toInt()

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding((16 * density).toInt(), (8 * density).toInt(), (16 * density).toInt(), (8 * density).toInt())
        }

        colorPickerView = ColorPicker(context).apply {
            setColor(initialColor)
            layoutParams = LinearLayout.LayoutParams(pickerSize, pickerSize).apply {
                gravity = Gravity.CENTER
            }
        }
        container.addView(colorPickerView)

        setButton(BUTTON_POSITIVE, context.getString(R.string.ok)) { _, _ ->
            onColorSelected(colorPickerView.getColor())
        }
        setButton(BUTTON_NEGATIVE, context.getString(R.string.cancel)) { dialog, _ ->
            dialog.dismiss()
        }

        setView(container)
    }
}
