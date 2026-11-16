package com.akkhara.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.provider.Settings
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.withClip
import androidx.core.graphics.withScale
import androidx.core.graphics.withTranslation
import com.akkhara.keyboard.engine.StripState
import com.akkhara.keyboard.engine.TranslationFailure
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos

/**
 * Lays out and draws the translate strip, the row above the keys that shows
 * the translation (tapped to put it in place of the text) or what is
 * happening instead. What the strip shows comes from the engine each time it
 * is drawn; the painter keeps only the clock of the translating animation.
 */
internal class StripPainter(private val context: Context, private val palette: Palette) {

    /** The parts of the strip a finger can press. */
    enum class Target { Replace }

    private val density = context.resources.displayMetrics.density
    private val scaledDensity = context.resources.displayMetrics.scaledDensity
    private val translatingText = context.getString(R.string.translate_strip_translating)
    private val failedText = context.getString(R.string.translate_strip_failed)
    private val notInstalledText = context.getString(R.string.translate_strip_not_installed)
    private val tooLongText = context.getString(R.string.translate_strip_too_long)

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.accent }
    private val tileTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = palette.onAccent
        textAlign = Paint.Align.CENTER
    }
    private val shimmerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shimmerMatrix = Matrix()
    private val glyphBounds = Rect()

    private val result = RectF()
    private val radius = 7f * density
    private val inset = 10f * density
    private var unit = density

    /** When the translating animation started, or null while nothing is being translated. */
    private var translatingSince: Long? = null

    /** Whether the translating animation moves; it stands still when the system's animations are off. */
    private var animates = true

    /** Height of the strip at [scale], the share of the full row height the keys are drawn at. */
    fun height(scale: Float): Float = (ROW_DP + 2 * GAP_DP) * density * scale

    /** Places the strip's parts across [width] pixels at [scale], [edge] pixels in from each side. */
    fun layout(width: Float, scale: Float, edge: Float) {
        unit = density * scale
        textPaint.textSize = TEXT_SP * scaledDensity * scale
        tileTextPaint.textSize = TILE_TEXT_SP * scaledDensity * scale
        val top = edge + GAP_DP * unit
        result.set(edge, top, width - edge, top + ROW_DP * unit)
        val band = result.width() * SHIMMER_SHARE
        val glow = ColorUtils.setAlphaComponent(palette.accent, SHIMMER_ALPHA)
        shimmerPaint.shader = LinearGradient(
            -band, 0f, band, 0f,
            intArrayOf(Color.TRANSPARENT, glow, Color.TRANSPARENT), null, Shader.TileMode.CLAMP,
        )
    }

    /** The part of the strip at ([x], [y]), or null if none is there. */
    fun targetAt(x: Float, y: Float, state: StripState): Target? =
        if (result.contains(x, y) && state is StripState.Translated) Target.Replace else null

    /**
     * Draws the translation, or what is happening instead, at [now] in
     * milliseconds. Nothing while there is nothing to say. Returns whether the
     * strip is moving, so it must be drawn again on the next frame.
     */
    fun draw(canvas: Canvas, state: StripState, pressed: Target?, now: Long): Boolean {
        if (state != StripState.Translating) translatingSince = null
        val isPressed = pressed == Target.Replace
        val (text, isTranslation) = when (state) {
            StripState.Idle -> return false
            StripState.Translating -> return drawTranslating(canvas, now)
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
        return false
    }

    /**
     * While translating: a light sweeps across the strip, and a round tile
     * beside "Translating…" turns over and over between A and က, like a card.
     */
    private fun drawTranslating(canvas: Canvas, now: Long): Boolean {
        val since = translatingSince ?: now.also {
            translatingSince = it
            animates = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
        }
        val elapsed = if (animates) now - since else 0L

        fillPaint.color = palette.key
        canvas.drawRoundRect(result, radius, radius, fillPaint)
        if (animates) {
            val band = result.width() * SHIMMER_SHARE
            val sweep = (elapsed % SHIMMER_MILLIS) / SHIMMER_MILLIS.toFloat()
            shimmerMatrix.setTranslate(result.left - band + sweep * (result.width() + 2 * band), 0f)
            shimmerPaint.shader.setLocalMatrix(shimmerMatrix)
            canvas.drawRoundRect(result, radius, radius, shimmerPaint)
        }

        // The tile shows one face, then turns: it narrows to an edge and opens on the other face.
        val face = (elapsed / FACE_MILLIS % 2).toInt()
        val turning = ((elapsed % FACE_MILLIS) - (FACE_MILLIS - TURN_MILLIS)).coerceAtLeast(0L) / TURN_MILLIS.toFloat()
        val glyph = TILE_FACES[if (turning < 0.5f) face else 1 - face]
        val width = abs(cos(PI * turning)).toFloat().coerceAtLeast(MIN_TILE_WIDTH)
        val tileRadius = TILE_DP / 2 * unit
        val cx = result.left + inset + tileRadius
        val cy = result.centerY()
        canvas.withScale(width, 1f, cx, cy) {
            drawCircle(cx, cy, tileRadius, tilePaint)
            tileTextPaint.getTextBounds(glyph, 0, glyph.length, glyphBounds)
            drawText(glyph, cx, cy - glyphBounds.exactCenterY(), tileTextPaint)
        }

        textPaint.color = palette.label
        val textLeft = cx + tileRadius + inset
        canvas.withClip(textLeft, result.top, result.right - inset, result.bottom) {
            val baseline = cy - (textPaint.ascent() + textPaint.descent()) / 2
            drawText(translatingText, textLeft, baseline, textPaint)
        }
        return animates
    }

    private companion object {
        const val ROW_DP = 48f
        const val GAP_DP = 4f
        const val TEXT_SP = 15f
        const val TILE_DP = 28f
        const val TILE_TEXT_SP = 15f

        /** The two faces of the translating tile: one letter of each input language. */
        val TILE_FACES = arrayOf("A", "က")

        /** How long the tile shows each face, the last [TURN_MILLIS] of it spent turning over. */
        const val FACE_MILLIS = 900L
        const val TURN_MILLIS = 320L

        /** Edge-on, the tile keeps this share of its width so it never vanishes. */
        const val MIN_TILE_WIDTH = 0.06f

        /** One pass of the light across the strip, its width as a share of the strip's, and its strength. */
        const val SHIMMER_MILLIS = 1400L
        const val SHIMMER_SHARE = 0.3f
        const val SHIMMER_ALPHA = 0x33

        /** The translation takes at most this many lines; a longer one ends in an ellipsis. */
        const val RESULT_LINES = 2
    }
}
