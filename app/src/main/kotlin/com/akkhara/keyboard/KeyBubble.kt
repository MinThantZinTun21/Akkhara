package com.akkhara.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.Gravity
import android.view.View
import android.widget.PopupWindow

/**
 * The bubble that floats above a pressed key. It shows either the key preview
 * (one character) or the key's alternates (a row, one of which may be
 * selected). It never takes touches: the keyboard View keeps the finger.
 */
class KeyBubble(context: Context, private val palette: Palette) {

    private val density = context.resources.displayMetrics.density
    private val content = Content(context)
    private val popup = PopupWindow(content).apply {
        isTouchable = false
        isFocusable = false
        isClippingEnabled = false
        animationStyle = 0
        elevation = 6f * density
    }

    private var cells: List<String> = emptyList()
    private var selected = -1

    /** Left edge of the first cell, in the anchor's coordinates. Valid while showing. */
    var left = 0f
        private set

    /** Width of one cell. Valid while showing. */
    var cellWidth = 0f
        private set

    val isShowing: Boolean get() = popup.isShowing

    /** Shows [text] enlarged above the key at [key] (in [anchor]'s coordinates). */
    fun showPreview(anchor: View, key: RectF, text: String) = show(anchor, key, listOf(text), selected = -1)

    /** Shows [alternates] in a row above the key, none selected. */
    fun showAlternates(anchor: View, key: RectF, alternates: List<String>) =
        show(anchor, key, alternates, selected = -1)

    /** Highlights the alternate at [index]; -1 highlights none. */
    fun select(index: Int) {
        if (index == selected) return
        selected = index
        content.invalidate()
    }

    fun dismiss() {
        if (popup.isShowing) popup.dismiss()
    }

    private fun show(anchor: View, key: RectF, cells: List<String>, selected: Int) {
        if (!anchor.isAttachedToWindow) return
        this.cells = cells
        this.selected = selected
        cellWidth = maxOf(key.width(), MIN_CELL_DP * density)
        val width = cellWidth * cells.size
        val height = key.height() * 1.15f
        // Centred on the key, but kept inside the keyboard's width.
        left = (key.centerX() - width / 2).coerceIn(0f, maxOf(0f, anchor.width - width))
        val top = key.top - height - GAP_DP * density

        val window = IntArray(2).also(anchor::getLocationInWindow)
        val x = window[0] + left.toInt()
        val y = window[1] + top.toInt()
        content.invalidate()
        if (popup.isShowing) {
            popup.update(x, y, width.toInt(), height.toInt())
        } else {
            popup.width = width.toInt()
            popup.height = height.toInt()
            popup.showAtLocation(anchor, Gravity.NO_GRAVITY, x, y)
        }
    }

    @SuppressLint("ViewConstructor")
    private inner class Content(context: Context) : View(context) {
        private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = 26f * resources.displayMetrics.scaledDensity
        }
        private val bounds = RectF()
        private val radius = 8f * density

        override fun onDraw(canvas: Canvas) {
            bounds.set(0f, 0f, width.toFloat(), height.toFloat())
            fill.color = palette.bubble
            canvas.drawRoundRect(bounds, radius, radius, fill)
            if (cells.isEmpty()) return
            val cell = width.toFloat() / cells.size
            val baseline = height / 2f - (label.descent() + label.ascent()) / 2
            cells.forEachIndexed { index, text ->
                if (index == selected) {
                    fill.color = palette.accent
                    bounds.set(index * cell, 0f, (index + 1) * cell, height.toFloat())
                    canvas.drawRoundRect(bounds, radius, radius, fill)
                }
                label.color = if (index == selected) palette.onAccent else palette.label
                canvas.drawText(text, (index + 0.5f) * cell, baseline, label)
            }
        }
    }

    private companion object {
        const val MIN_CELL_DP = 40f
        const val GAP_DP = 6f
    }
}
