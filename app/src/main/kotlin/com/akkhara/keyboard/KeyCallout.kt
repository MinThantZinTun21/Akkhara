package com.akkhara.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.Gravity
import android.view.View
import android.widget.PopupWindow
import com.akkhara.keyboard.engine.Cell
import com.akkhara.keyboard.engine.SPACE_SIGN

/**
 * What floats above a pressed key: either the key preview (one character) or
 * the key's alternates (a row, one of which may be selected). It has its own
 * window because it must draw above the top row, outside the keyboard View.
 * It never takes touches: the keyboard View keeps the finger.
 */
class KeyCallout(context: Context, private val palette: Palette, private val keyStyle: KeyStyle) {

    private val density = context.resources.displayMetrics.density
    private val content = Content(context)
    private val window = PopupWindow(content).apply {
        isTouchable = false
        isFocusable = false
        isClippingEnabled = false
        animationStyle = 0
        elevation = 6f * density
    }

    private var texts: List<String> = emptyList()

    /** Which of [texts] is highlighted, if any. */
    private var selected: Int? = null

    /** Left edge of the first text and the width given to each, in the anchor's coordinates. */
    private var left = 0f
    private var textWidth = 0f

    /** Shows [text] enlarged above [key], which is in [anchor]'s coordinates. */
    fun showPreview(anchor: View, key: Cell, text: String) = show(anchor, key, listOf(text))

    /** Shows [alternates] in a row above [key], none selected. */
    fun showAlternates(anchor: View, key: Cell, alternates: List<String>) = show(anchor, key, alternates)

    /** Which of the alternates showing is nearest to [x], in the anchor's coordinates. */
    fun alternateAt(x: Float): Int = ((x - left) / textWidth).toInt().coerceIn(0, texts.lastIndex)

    /** Highlights the alternate at [index]; null highlights none. */
    fun select(index: Int?) {
        if (index == selected) return
        selected = index
        content.invalidate()
    }

    fun dismiss() {
        if (window.isShowing) window.dismiss()
    }

    private fun show(anchor: View, key: Cell, texts: List<String>) {
        if (!anchor.isAttachedToWindow) return
        this.texts = texts
        selected = null
        textWidth = maxOf(key.width, MIN_TEXT_WIDTH_DP * density)
        val width = textWidth * texts.size
        val height = key.height * 1.15f
        // Centred on the key, but kept inside the keyboard's width.
        left = (key.centerX - width / 2).coerceIn(0f, maxOf(0f, anchor.width - width))
        val top = key.top - height - GAP_DP * density

        val anchorInWindow = IntArray(2).also(anchor::getLocationInWindow)
        val x = anchorInWindow[0] + left.toInt()
        val y = anchorInWindow[1] + top.toInt()
        content.invalidate()
        if (window.isShowing) {
            window.update(x, y, width.toInt(), height.toInt())
        } else {
            window.width = width.toInt()
            window.height = height.toInt()
            window.showAtLocation(anchor, Gravity.NO_GRAVITY, x, y)
        }
    }

    @SuppressLint("ViewConstructor")
    private inner class Content(context: Context) : View(context) {
        private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = 24f * resources.displayMetrics.scaledDensity
        }
        private val bounds = RectF()

        override fun onDraw(canvas: Canvas) {
            bounds.set(0f, 0f, width.toFloat(), height.toFloat())
            // A little rounder than a key, as the callout is bigger; it follows the key style's shape.
            val radius = keyStyle.cornerRadius(height.toFloat(), density) + density
            fill.color = palette.callout
            canvas.drawRoundRect(bounds, radius, radius, fill)
            if (texts.isEmpty()) return
            val each = width.toFloat() / texts.size
            val fullSize = label.textSize
            texts.forEachIndexed { index, typed ->
                // A space would draw as nothing, so it shows as the sign for one.
                val text = if (typed == " ") SPACE_SIGN else typed
                label.textSize = fullSize
                // A key that types several characters may be wider than its share of the callout.
                label.shrinkToFit(text, room = each - 4f * density)
                val baseline = height / 2f - (label.descent() + label.ascent()) / 2
                val isSelected = index == selected
                if (isSelected) {
                    fill.color = palette.accent
                    bounds.set(index * each, 0f, (index + 1) * each, height.toFloat())
                    canvas.drawRoundRect(bounds, radius, radius, fill)
                }
                label.color = if (isSelected) palette.onAccent else palette.label
                canvas.drawText(text, (index + 0.5f) * each, baseline, label)
            }
            label.textSize = fullSize
        }
    }

    private companion object {
        const val MIN_TEXT_WIDTH_DP = 40f
        const val GAP_DP = 6f
    }
}

