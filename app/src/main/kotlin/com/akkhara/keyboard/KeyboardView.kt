package com.akkhara.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import com.akkhara.keyboard.engine.Action
import com.akkhara.keyboard.engine.ActionKey
import com.akkhara.keyboard.engine.CharKey
import com.akkhara.keyboard.engine.EnterAction
import com.akkhara.keyboard.engine.Hold
import com.akkhara.keyboard.engine.InputLanguage
import com.akkhara.keyboard.engine.Key
import com.akkhara.keyboard.engine.KeyboardEngine
import com.akkhara.keyboard.engine.ShiftState
import kotlin.math.abs

/**
 * Draws the layer the engine is showing and reports touches back to it.
 * It holds no typing logic: what a key does, tapped or held, is decided by
 * the engine.
 */
@SuppressLint("ViewConstructor")
class KeyboardView(context: Context) : View(context) {

    /** What the View needs from the service that hosts it. */
    interface Host {
        /** Runs [work] on the engine as one edit of the text field, then redraws. */
        fun edit(work: KeyboardEngine.() -> Unit)

        fun showKeyboardPicker()
    }

    private class Cell(val key: Key, val bounds: RectF)

    /** One finger on the keyboard. */
    private class Touch(val cell: Cell, var x: Float, var y: Float) {
        var holdTimer: Runnable? = null
        var repeatTimer: Runnable? = null

        /** Non-null once the key's alternates are showing. */
        var alternates: List<String>? = null
        var selectedAlternate = -1

        /** Where the finger was when the alternates appeared; it must move from here to select one. */
        var alternatesX = 0f
        var alternatesY = 0f

        /** True once holding has already done the key's work, so lifting must not press it again. */
        var consumed = false
    }

    var hapticsEnabled = true
    var keyPreviewEnabled = true

    private var engine: KeyboardEngine? = null

    /** Never in a password field, whatever the setting says. */
    private val showsKeyPreview: Boolean
        get() = keyPreviewEnabled && engine?.allowsKeyPreview == true
    private var host: Host? = null

    private val palette = Palette.Light
    private val density = resources.displayMetrics.density
    private val rowHeight = 54f * density
    private val keyGap = 3f * density
    private val keyRadius = 6f * density
    private val edgePadding = 4f * density
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private val cells = ArrayList<Cell>()
    private val touches = LinkedHashMap<Int, Touch>() // by pointer id, oldest first
    private val bubble = KeyBubble(context, palette)

    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 22f * resources.displayMetrics.scaledDensity
    }
    private val smallLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 15f * resources.displayMetrics.scaledDensity
    }
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val iconPath = Path()

    fun bind(engine: KeyboardEngine, host: Host) {
        this.engine = engine
        this.host = host
        onEngineChanged()
    }

    /** Call after anything that may have changed which keys are showing. */
    fun onEngineChanged() {
        layoutKeys()
        invalidate()
    }

    // ---- layout ----

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val rowCount = engine?.rows?.size ?: 0
        val height = (rowCount * rowHeight + 2 * edgePadding).toInt()
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), height)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        layoutKeys()
    }

    /**
     * Rebuilds the cells from the engine's rows. Fingers already down keep
     * the key they landed on, so a layer change under a held key (shift, then
     * a letter with the other thumb) does not lose it.
     */
    private fun layoutKeys() {
        cells.clear()
        val rows = engine?.rows ?: return
        if (width == 0) return
        val unit = (width - 2 * edgePadding) / COLUMNS
        rows.forEachIndexed { rowIndex, row ->
            val rowWidth = row.sumOf { it.width.toDouble() }.toFloat() * unit
            var x = (width - rowWidth) / 2f
            val top = edgePadding + rowIndex * rowHeight
            for (key in row) {
                val keyWidth = key.width * unit
                cells += Cell(
                    key,
                    RectF(x + keyGap / 2, top + keyGap, x + keyWidth - keyGap / 2, top + rowHeight - keyGap),
                )
                x += keyWidth
            }
        }
    }

    // ---- drawing ----

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(palette.background)
        val shiftActive = engine?.shift != ShiftState.Off
        for (cell in cells) {
            val key = cell.key
            val isActiveShift = shiftActive && key is ActionKey && key.action == Action.Shift
            keyPaint.color = when {
                touches.values.any { it.cell.bounds == cell.bounds } -> palette.keyPressed
                isActiveShift -> palette.accent
                key is ActionKey && key.action != Action.Space -> palette.actionKey
                else -> palette.key
            }
            val ink = if (isActiveShift) palette.onAccent else palette.label
            labelPaint.color = ink
            smallLabelPaint.color = ink
            iconPaint.color = ink
            canvas.drawRoundRect(cell.bounds, keyRadius, keyRadius, keyPaint)
            when (key) {
                is CharKey -> drawLabel(canvas, key.char, cell.bounds, labelPaint)
                is ActionKey -> drawAction(canvas, key.action, cell.bounds)
            }
        }
    }

    private fun drawLabel(canvas: Canvas, text: String, bounds: RectF, paint: Paint) {
        val baseline = bounds.centerY() - (paint.descent() + paint.ascent()) / 2
        canvas.drawText(text, bounds.centerX(), baseline, paint)
    }

    private fun drawAction(canvas: Canvas, action: Action, bounds: RectF) {
        val cx = bounds.centerX()
        val cy = bounds.centerY()
        val s = 9f * density // half the icon's size
        iconPath.rewind()
        iconPaint.style = Paint.Style.STROKE
        when (action) {
            Action.Space ->
                return drawLabel(canvas, engine?.spaceLabel.orEmpty(), bounds, smallLabelPaint)
            Action.Symbols -> return drawLabel(canvas, "?123", bounds, smallLabelPaint)
            Action.MoreSymbols -> return drawLabel(canvas, "=\\<", bounds, smallLabelPaint)
            Action.Letters -> {
                val letters = if (engine?.inputLanguage == InputLanguage.Myanmar) "ကခဂ" else "ABC"
                return drawLabel(canvas, letters, bounds, smallLabelPaint)
            }
            Action.Globe -> {
                // a circle with a meridian and an equator
                iconPath.addCircle(cx, cy, s, Path.Direction.CW)
                iconPath.addOval(cx - s * 0.45f, cy - s, cx + s * 0.45f, cy + s, Path.Direction.CW)
                iconPath.moveTo(cx - s, cy)
                iconPath.lineTo(cx + s, cy)
            }
            Action.Shift -> {
                // an upward arrow: outlined when off, filled for one-shot shift, underlined for shift lock
                val shift = engine?.shift ?: ShiftState.Off
                iconPath.moveTo(cx, cy - s)
                iconPath.lineTo(cx + s, cy)
                iconPath.lineTo(cx + s * 0.45f, cy)
                iconPath.lineTo(cx + s * 0.45f, cy + s * 0.7f)
                iconPath.lineTo(cx - s * 0.45f, cy + s * 0.7f)
                iconPath.lineTo(cx - s * 0.45f, cy)
                iconPath.lineTo(cx - s, cy)
                iconPath.close()
                if (shift != ShiftState.Off) iconPaint.style = Paint.Style.FILL_AND_STROKE
                if (shift == ShiftState.Locked) {
                    canvas.drawLine(cx - s * 0.6f, cy + s * 1.15f, cx + s * 0.6f, cy + s * 1.15f, iconPaint)
                }
            }
            Action.Backspace -> {
                // a left-pointing tag with a cross in it
                iconPath.moveTo(cx - s * 1.3f, cy)
                iconPath.lineTo(cx - s * 0.5f, cy - s * 0.8f)
                iconPath.lineTo(cx + s * 1.2f, cy - s * 0.8f)
                iconPath.lineTo(cx + s * 1.2f, cy + s * 0.8f)
                iconPath.lineTo(cx - s * 0.5f, cy + s * 0.8f)
                iconPath.close()
                iconPath.moveTo(cx - s * 0.05f, cy - s * 0.35f)
                iconPath.lineTo(cx + s * 0.65f, cy + s * 0.35f)
                iconPath.moveTo(cx + s * 0.65f, cy - s * 0.35f)
                iconPath.lineTo(cx - s * 0.05f, cy + s * 0.35f)
            }
            Action.Enter -> enterIcon(engine?.enterAction ?: EnterAction.NewLine, cx, cy, s)
        }
        canvas.drawPath(iconPath, iconPaint)
    }

    /** Adds to [iconPath] the picture of what enter does in this text field. */
    private fun enterIcon(action: EnterAction, cx: Float, cy: Float, s: Float) {
        when (action) {
            EnterAction.NewLine -> {
                // down, then left, with an arrow head
                iconPath.moveTo(cx + s, cy - s * 0.7f)
                iconPath.lineTo(cx + s, cy + s * 0.3f)
                iconPath.lineTo(cx - s, cy + s * 0.3f)
                iconPath.moveTo(cx - s * 0.4f, cy - s * 0.3f)
                iconPath.lineTo(cx - s, cy + s * 0.3f)
                iconPath.lineTo(cx - s * 0.4f, cy + s * 0.9f)
            }
            EnterAction.Search -> {
                // a magnifying glass
                iconPath.addCircle(cx - s * 0.2f, cy - s * 0.2f, s * 0.7f, Path.Direction.CW)
                iconPath.moveTo(cx + s * 0.3f, cy + s * 0.3f)
                iconPath.lineTo(cx + s, cy + s)
            }
            EnterAction.Send -> {
                // a paper plane pointing right
                iconPath.moveTo(cx - s, cy - s * 0.8f)
                iconPath.lineTo(cx + s, cy)
                iconPath.lineTo(cx - s, cy + s * 0.8f)
                iconPath.lineTo(cx - s * 0.6f, cy)
                iconPath.close()
                iconPath.moveTo(cx - s * 0.6f, cy)
                iconPath.lineTo(cx + s, cy)
            }
            EnterAction.Go, EnterAction.Next -> {
                // an arrow pointing right
                iconPath.moveTo(cx - s, cy)
                iconPath.lineTo(cx + s, cy)
                iconPath.moveTo(cx + s * 0.3f, cy - s * 0.7f)
                iconPath.lineTo(cx + s, cy)
                iconPath.lineTo(cx + s * 0.3f, cy + s * 0.7f)
            }
            EnterAction.Done -> {
                // a tick
                iconPath.moveTo(cx - s, cy)
                iconPath.lineTo(cx - s * 0.3f, cy + s * 0.7f)
                iconPath.lineTo(cx + s, cy - s * 0.7f)
            }
        }
    }

    // ---- touch ----

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                // A second finger landing commits the first, so fast two-thumb typing never drops a key.
                liftAll()
                val index = event.actionIndex
                val x = event.getX(index)
                val y = event.getY(index)
                cellAt(x, y)?.let { touchDown(event.getPointerId(index), Touch(it, x, y)) }
            }
            MotionEvent.ACTION_MOVE ->
                for (index in 0 until event.pointerCount) {
                    val touch = touches[event.getPointerId(index)] ?: continue
                    slide(touch, event.getX(index), event.getY(index))
                }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> lift(event.getPointerId(event.actionIndex))
            MotionEvent.ACTION_CANCEL -> dropAll()
        }
        invalidate()
        return true
    }

    override fun onDetachedFromWindow() {
        dropAll()
        super.onDetachedFromWindow()
    }

    private fun touchDown(pointerId: Int, touch: Touch) {
        val cell = touch.cell
        touches[pointerId] = touch
        if (hapticsEnabled) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        if (showsKeyPreview && cell.key is CharKey) bubble.showPreview(this, cell.bounds, cell.key.char)
        touch.holdTimer = Runnable { onHeld(pointerId, touch) }.also { postDelayed(it, HOLD_MILLIS) }
    }

    /** The finger has stayed on the key long enough to count as holding it. */
    private fun onHeld(pointerId: Int, touch: Touch) {
        touch.holdTimer = null
        when (val hold = engine?.hold(touch.cell.key)) {
            Hold.KeyboardPicker -> {
                drop(pointerId)
                invalidate()
                host?.showKeyboardPicker()
            }
            Hold.Repeat -> {
                touch.consumed = true
                repeat(touch)
            }
            is Hold.Alternates -> {
                touch.alternates = hold.alternates
                touch.alternatesX = touch.x
                touch.alternatesY = touch.y
                bubble.showAlternates(this, touch.cell.bounds, hold.alternates)
            }
            Hold.None, null -> Unit
        }
    }

    /** Presses the held key now and again shortly, until the finger lifts. */
    private fun repeat(touch: Touch) {
        host?.edit { press(touch.cell.key) }
        touch.repeatTimer = Runnable { repeat(touch) }.also { postDelayed(it, REPEAT_MILLIS) }
    }

    /** The finger moved. Only matters while alternates are showing. */
    private fun slide(touch: Touch, x: Float, y: Float) {
        touch.x = x
        touch.y = y
        val alternates = touch.alternates ?: return
        if (touch.selectedAlternate < 0) {
            val moved = maxOf(abs(x - touch.alternatesX), abs(y - touch.alternatesY))
            if (moved < touchSlop) return // lifting without sliding types the key's own character
        }
        val index = ((x - bubble.left) / bubble.cellWidth).toInt().coerceIn(0, alternates.lastIndex)
        touch.selectedAlternate = index
        bubble.select(index)
    }

    private fun liftAll() {
        for (pointerId in touches.keys.toList()) lift(pointerId)
    }

    /** The finger lifted: the key under it, or the alternate it slid to, is typed. */
    private fun lift(pointerId: Int) {
        val touch = drop(pointerId) ?: return
        if (touch.consumed) return
        val key = touch.cell.key
        val alternate = touch.alternates?.getOrNull(touch.selectedAlternate)
        if (alternate != null && key is CharKey) {
            host?.edit { pressAlternate(key, alternate) }
        } else {
            host?.edit { press(key) }
        }
    }

    /** Forgets the finger without typing anything. */
    private fun drop(pointerId: Int): Touch? {
        val touch = touches.remove(pointerId) ?: return null
        touch.holdTimer?.let(::removeCallbacks)
        touch.repeatTimer?.let(::removeCallbacks)
        bubble.dismiss()
        return touch
    }

    private fun dropAll() {
        for (pointerId in touches.keys.toList()) drop(pointerId)
    }

    /** The key under a touch; a touch in the gap between keys goes to the nearest key in that row. */
    private fun cellAt(x: Float, y: Float): Cell? {
        val rowIndex = ((y - edgePadding) / rowHeight).toInt()
        val rowTop = edgePadding + rowIndex * rowHeight
        return cells
            .filter { it.bounds.centerY() > rowTop && it.bounds.centerY() < rowTop + rowHeight }
            .minByOrNull { cell ->
                when {
                    x < cell.bounds.left -> cell.bounds.left - x
                    x > cell.bounds.right -> x - cell.bounds.right
                    else -> 0f
                }
            }
    }

    private companion object {
        /** Width of the keyboard in ordinary character keys. */
        const val COLUMNS = 10f

        /** How long a finger must stay down to count as holding the key. */
        const val HOLD_MILLIS = 350L

        /** Time between presses of a held, repeating key. */
        const val REPEAT_MILLIS = 60L
    }
}
