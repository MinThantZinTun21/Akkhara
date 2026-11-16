package com.akkhara.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.graphics.withTranslation
import com.akkhara.keyboard.engine.StripState
import com.akkhara.keyboard.engine.TranslationFailure

/**
 * Lays out and draws the translate strip, the row above the keys, and says
 * which of its parts a touch lands on. It holds no state of its own: what the
 * strip shows comes from the engine each time it is drawn.
 *
 * Left to right: the Translate button, then the translation (tapped to put it
 * in place of the text), or what is happening instead.
 */
internal class StripPainter(context: Context, private val palette: Palette) {

    /** The parts of the strip a finger can press. */
    enum class Target { Translate, Replace }

    private val density = context.resources.displayMetrics.density
    private val scaledDensity = context.resources.displayMetrics.scaledDensity
    private val translateLabel = context.getString(R.string.translate_strip_translate)
    private val translatingText = context.getString(R.string.translate_strip_translating)
    private val failedText = context.getString(R.string.translate_strip_failed)
    private val notInstalledText = context.getString(R.string.translate_strip_not_installed)
    private val tooLongText = context.getString(R.string.translate_strip_too_long)

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val buttonPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }

    private val translate = RectF()
    private val result = RectF()
    private val radius = 7f * density
    private val inset = 10f * density

    /** Height of the strip at [scale], the share of the full row height the keys are drawn at. */
    fun height(scale: Float): Float = (ROW_DP + 2 * GAP_DP) * density * scale

    /** Places the strip's parts across [width] pixels at [scale], [edge] pixels in from each side. */
    fun layout(width: Float, scale: Float, edge: Float) {
        val unit = density * scale
        textPaint.textSize = TEXT_SP * scaledDensity * scale
        buttonPaint.textSize = TEXT_SP * scaledDensity * scale
        val top = edge + GAP_DP * unit
        val bottom = top + ROW_DP * unit
        translate.set(edge, top, edge + BUTTON_DP * unit, bottom)
        result.set(translate.right + GAP_DP * unit, top, width - edge, bottom)
    }

    /** The part of the strip at ([x], [y]), or null if none is there. */
    fun targetAt(x: Float, y: Float, state: StripState): Target? = when {
        translate.contains(x, y) -> Target.Translate
        result.contains(x, y) && state is StripState.Translated -> Target.Replace
        else -> null
    }

    fun draw(canvas: Canvas, state: StripState, pressed: Target?) {
        drawTranslateButton(canvas, state, pressed == Target.Translate)
        drawResult(canvas, state, pressed == Target.Replace)
    }

    private fun drawTranslateButton(canvas: Canvas, state: StripState, isPressed: Boolean) {
        val isBusy = state == StripState.Translating
        fillPaint.color = if (isPressed || isBusy) palette.keyPressed else palette.accent
        val pill = translate.height() / 2
        canvas.drawRoundRect(translate, pill, pill, fillPaint)
        buttonPaint.color = if (isPressed || isBusy) palette.label else palette.onAccent
        val fullSize = buttonPaint.textSize
        buttonPaint.shrinkToFit(translateLabel, translate.width() - inset)
        val baseline = translate.centerY() - (buttonPaint.descent() + buttonPaint.ascent()) / 2
        canvas.drawText(translateLabel, translate.centerX(), baseline, buttonPaint)
        buttonPaint.textSize = fullSize
    }

    /** The translation, or what is happening instead. Nothing while there is nothing to say. */
    private fun drawResult(canvas: Canvas, state: StripState, isPressed: Boolean) {
        val (text, isTranslation) = when (state) {
            StripState.Idle -> return
            StripState.Translating -> translatingText to false
            is StripState.Translated -> state.translation to true
            is StripState.Failed -> when (state.reason) {
                TranslationFailure.NotInstalled -> notInstalledText
                TranslationFailure.TooLong -> tooLongText
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
        canvas.withTranslation(result.left + inset, result.centerY() - layout.height / 2f) { layout.draw(this) }
    }

    private companion object {
        const val ROW_DP = 48f
        const val BUTTON_DP = 108f
        const val GAP_DP = 4f
        const val TEXT_SP = 15f

        /** The translation takes at most this many lines; a longer one ends in an ellipsis. */
        const val RESULT_LINES = 2
    }
}
