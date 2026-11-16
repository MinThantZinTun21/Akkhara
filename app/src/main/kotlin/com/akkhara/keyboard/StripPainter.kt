package com.akkhara.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import com.akkhara.keyboard.engine.StripState
import com.akkhara.keyboard.engine.TranslateStrip
import com.akkhara.keyboard.engine.TranslationFailure

/**
 * Lays out and draws the translate strip above the keys, and says which of
 * its parts a touch lands on. It holds no state of the strip's own: what the
 * strip shows comes from the engine each time it is drawn.
 *
 * Top to bottom: the translation direction with a close button, the typed
 * text beside the Translate button, then the translation, which is tapped to
 * insert it.
 */
internal class StripPainter(context: Context, private val palette: Palette) {

    /** The parts of the strip a finger can press. */
    enum class Target { Close, Translate, Insert }

    private val density = context.resources.displayMetrics.density
    private val scaledDensity = context.resources.displayMetrics.scaledDensity
    private val translateLabel = context.getString(R.string.translate_strip_translate)
    private val translatingText = context.getString(R.string.translate_strip_translating)
    private val hintText = context.getString(R.string.translate_strip_hint)
    private val failedText = context.getString(R.string.translate_strip_failed)
    private val notInstalledText = context.getString(R.string.translate_strip_not_installed)

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
        strokeCap = Paint.Cap.ROUND
    }
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val smallPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val buttonPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }

    private val header = RectF()
    private val close = RectF()
    private val input = RectF()
    private val translate = RectF()
    private val result = RectF()
    private val radius = 7f * density
    private val inset = 10f * density

    /** Height of the strip at [scale], the share of the full row height the keys are drawn at. */
    fun height(scale: Float): Float = FULL_HEIGHT_DP * density * scale

    /** Places the strip's parts across [width] pixels at [scale]. */
    fun layout(width: Float, scale: Float, edge: Float) {
        val unit = density * scale
        textPaint.textSize = TEXT_SP * scaledDensity * scale
        smallPaint.textSize = SMALL_SP * scaledDensity * scale
        buttonPaint.textSize = SMALL_SP * scaledDensity * scale
        var top = edge + GAP_DP * unit
        header.set(edge, top, width - edge, top + HEADER_DP * unit)
        close.set(width - edge - HEADER_DP * unit * 1.4f, header.top, width - edge, header.bottom)
        top = header.bottom + GAP_DP * unit
        val buttonWidth = BUTTON_DP * unit
        input.set(edge, top, width - edge - buttonWidth - GAP_DP * unit, top + INPUT_DP * unit)
        translate.set(width - edge - buttonWidth, top, width - edge, input.bottom)
        top = input.bottom + GAP_DP * unit
        result.set(edge, top, width - edge, top + RESULT_DP * unit)
    }

    /** The part of the strip at ([x], [y]), or null if none is there. */
    fun targetAt(x: Float, y: Float, strip: TranslateStrip): Target? = when {
        close.contains(x, y) -> Target.Close
        translate.contains(x, y) -> Target.Translate
        result.contains(x, y) && strip.state is StripState.Translated -> Target.Insert
        else -> null
    }

    fun draw(canvas: Canvas, strip: TranslateStrip, pressed: Target?, directionLabel: String) {
        drawHeader(canvas, directionLabel, pressed == Target.Close)
        drawInput(canvas, strip.text)
        drawTranslateButton(canvas, strip.state, pressed == Target.Translate)
        drawResult(canvas, strip.state, pressed == Target.Insert)
    }

    private fun drawHeader(canvas: Canvas, directionLabel: String, isPressed: Boolean) {
        smallPaint.color = palette.label
        val baseline = header.centerY() - (smallPaint.descent() + smallPaint.ascent()) / 2
        canvas.drawText(directionLabel, header.left + inset / 2, baseline, smallPaint)
        if (isPressed) {
            fillPaint.color = palette.keyPressed
            canvas.drawRoundRect(close, radius, radius, fillPaint)
        }
        // a cross
        val half = close.height() * 0.22f
        val cx = close.centerX()
        val cy = close.centerY()
        linePaint.color = palette.label
        canvas.drawLine(cx - half, cy - half, cx + half, cy + half, linePaint)
        canvas.drawLine(cx + half, cy - half, cx - half, cy + half, linePaint)
    }

    /** The typed text, or a hint while there is none. When too long, its end is shown, where typing goes. */
    private fun drawInput(canvas: Canvas, text: String) {
        fillPaint.color = palette.key
        canvas.drawRoundRect(input, radius, radius, fillPaint)
        val room = input.width() - 2 * inset
        val shown = if (text.isEmpty()) hintText else tailThatFits(text, room)
        textPaint.color = if (text.isEmpty()) palette.hint else palette.label
        val baseline = input.centerY() - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(shown, input.left + inset, baseline, textPaint)
    }

    private fun tailThatFits(text: String, room: Float): String {
        if (textPaint.measureText(text) <= room) return text
        val ellipsis = "…"
        val fits = textPaint.breakText(text, false, room - textPaint.measureText(ellipsis), null)
        var start = text.length - fits
        // Never start on a mark: it would be drawn without its consonant.
        while (start < text.length && text[start].isMark()) start++
        return ellipsis + text.substring(start)
    }

    private fun drawTranslateButton(canvas: Canvas, state: StripState, isPressed: Boolean) {
        val isBusy = state == StripState.Translating
        fillPaint.color = when {
            isPressed || isBusy -> palette.keyPressed
            else -> palette.accent
        }
        canvas.drawRoundRect(translate, radius, radius, fillPaint)
        buttonPaint.color = if (isPressed || isBusy) palette.label else palette.onAccent
        val fullSize = buttonPaint.textSize
        buttonPaint.shrinkToFit(translateLabel, translate.width() - inset)
        val baseline = translate.centerY() - (buttonPaint.descent() + buttonPaint.ascent()) / 2
        canvas.drawText(translateLabel, translate.centerX(), baseline, buttonPaint)
        buttonPaint.textSize = fullSize
    }

    /** The translation (tapped to insert it), or what is happening instead. */
    private fun drawResult(canvas: Canvas, state: StripState, isPressed: Boolean) {
        val (text, isTranslation) = when (state) {
            StripState.Idle -> return
            StripState.Translating -> translatingText to false
            is StripState.Translated -> state.translation to true
            is StripState.Failed -> when (state.reason) {
                TranslationFailure.NotInstalled -> notInstalledText
                TranslationFailure.Failed -> failedText
            } to false
        }
        fillPaint.color = when {
            isPressed -> palette.keyPressed
            isTranslation -> palette.actionKey
            else -> palette.key
        }
        canvas.drawRoundRect(result, radius, radius, fillPaint)
        textPaint.color = if (isTranslation && !isPressed) palette.actionLabel else palette.label
        val width = (result.width() - 2 * inset).toInt().coerceAtLeast(1)
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, textPaint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setMaxLines(RESULT_LINES)
            .setEllipsize(TextUtils.TruncateAt.END)
            .setIncludePad(false)
            .build()
        canvas.save()
        canvas.translate(result.left + inset, result.centerY() - layout.height / 2f)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun Char.isMark(): Boolean {
        val type = Character.getType(this).toByte()
        return type == Character.NON_SPACING_MARK || type == Character.COMBINING_SPACING_MARK
    }

    private companion object {
        const val HEADER_DP = 30f
        const val INPUT_DP = 42f
        const val RESULT_DP = 52f
        const val BUTTON_DP = 96f
        const val GAP_DP = 4f
        const val FULL_HEIGHT_DP = HEADER_DP + INPUT_DP + RESULT_DP + 4 * GAP_DP
        const val TEXT_SP = 17f
        const val SMALL_SP = 14f

        /** The translation takes at most this many lines; a longer one ends in an ellipsis. */
        const val RESULT_LINES = 2
    }
}
