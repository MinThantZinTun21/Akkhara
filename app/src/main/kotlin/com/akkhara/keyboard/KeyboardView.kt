package com.akkhara.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Build
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowInsets
import com.akkhara.keyboard.engine.Action
import com.akkhara.keyboard.engine.ActionKey
import com.akkhara.keyboard.engine.Blank
import com.akkhara.keyboard.engine.Cell
import com.akkhara.keyboard.engine.CharKey
import com.akkhara.keyboard.engine.EnterAction
import com.akkhara.keyboard.engine.Hold
import com.akkhara.keyboard.engine.Key
import com.akkhara.keyboard.engine.KeyGrid
import com.akkhara.keyboard.engine.KeyboardEngine
import com.akkhara.keyboard.engine.Layer
import com.akkhara.keyboard.engine.ShiftState
import com.akkhara.keyboard.engine.StripState
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Draws the layer the engine is showing and reports touches back to it.
 * It holds no typing logic and no layout logic: what a key does, tapped or
 * held, is decided by the engine, and where each key sits by [KeyGrid].
 */
@SuppressLint("ViewConstructor")
class KeyboardView(context: Context, val theme: Theme) : View(context) {

    /** What the View needs from the service that hosts it. */
    interface Host {
        /** Runs [work] on the engine as one edit of the text field, then redraws. */
        fun edit(work: KeyboardEngine.() -> Unit)

        fun showKeyboardPicker()
    }

    /** One finger on the keyboard. */
    private class Touch(val cell: Cell, var x: Float, var y: Float) {
        var holdTimer: Runnable? = null
        var repeatTimer: Runnable? = null

        /** Non-null once the key's alternates are showing. */
        var choice: AlternateChoice? = null

        /** True once holding has already done the key's work, so lifting must not press it again. */
        var consumed = false

        /** The key that holding this one offers above it, once it shows, and whether the finger is on it. */
        var offered: Hold? = null
        var onOfferedKey = false
    }

    /**
     * The alternates on offer for a held key. [startX] and [startY] are where
     * the finger was when they appeared.
     */
    private class AlternateChoice(val alternates: List<String>, val startX: Float, val startY: Float) {
        /** Null until the finger slides: lifting without sliding types the key's own character. */
        var selected: Int? = null
    }

    var hapticsEnabled = true
    var keyPreviewEnabled = true

    private var engine: KeyboardEngine? = null

    /** Never in a password field, whatever the setting says. */
    private val showsKeyPreview: Boolean
        get() = keyPreviewEnabled && engine?.allowsKeyPreview == true
    private var host: Host? = null

    // The service builds a new View when the theme to show changes, so this never changes.
    private val palette = theme.palette
    private val density = resources.displayMetrics.density
    private val fullRowHeight = FULL_ROW_DP * density

    /** Shorter than [fullRowHeight] when the screen is too short for it, as in landscape. */
    private var rowHeight = fullRowHeight

    /** How much the keys' labels and pictures are shrunk to fit a short screen. */
    private var keyScale = 1f

    /** Height of whatever the system draws over the bottom of the keyboard's window. */
    private var bottomInset = 0
    private val keyGap = 4.2f * density
    private val rowGap = 5.8f * density
    private val keyRadius = 7f * density
    private val edgePadding = 2f * density
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private var grid: KeyGrid? = null
    private val stripPainter = StripPainter(context, palette)

    /** Height of the translate strip above the keys; 0 while it is closed. */
    private var stripHeight = 0f

    /** The finger on the translate strip, if any, and the part of the strip it went down on. */
    private var stripPointer: Int? = null
    private var stripTarget: StripPainter.Target? = null
    private val touches = LinkedHashMap<Int, Touch>() // by pointer id, oldest first
    private val callout = KeyCallout(context, palette)

    private val emojiPanel = EmojiPanel(context, palette)

    /** The finger on the emoji panel's grid or tabs, if any. */
    private var panelPointer: Int? = null
    private var panelHoldTimer: Runnable? = null

    /** The emoji held in the panel and its skin tones on offer, once they are showing. */
    private var heldEmoji: EmojiPanel.Held? = null
    private var skinToneChoice: AlternateChoice? = null

    /** Which layer was showing when the View last looked, so the emoji panel opens at its start. */
    private var shownLayer: Layer? = null

    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val labelSize = 20f * resources.displayMetrics.scaledDensity
    private val smallLabelSize = 14f * resources.displayMetrics.scaledDensity
    private val hintSize = 10f * resources.displayMetrics.scaledDensity
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val smallLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.RIGHT
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
        val layer = engine?.layer
        if (layer == Layer.Emoji && shownLayer != Layer.Emoji) emojiPanel.scrollToStart()
        shownLayer = layer
        requestLayout()
        layoutKeys()
        invalidate()
    }

    // ---- layout ----

    /**
     * Key widths come from the width on offer and row heights from the height
     * of the screen, so one layout serves portrait and landscape: the
     * keyboard never takes more than [MAX_SCREEN_SHARE] of the screen, which
     * leaves the text field in view.
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val rowCount = shownRows()?.size ?: 0
        // A pad keeps the letters' height with fewer rows; the emoji panel has more rows than the engine's bar.
        val heightInRows = max(rowCount, engine?.heightInRows ?: 0)
        // The strip shows only while it has something to say, so the keyboard keeps its height otherwise.
        val hasStrip = engine?.strip.let { it != null && it != StripState.Idle }
        var scale = 1f
        if (rowCount > 0) {
            // The strip and the keys shrink together when the screen is too short for both.
            val tallest = resources.displayMetrics.heightPixels * MAX_SCREEN_SHARE - 2 * edgePadding
            val fullHeight = heightInRows * fullRowHeight + if (hasStrip) stripPainter.height(1f) else 0f
            scale = min(1f, tallest / fullHeight)
        }
        keyScale = scale
        rowHeight = fullRowHeight * scale * if (rowCount > 0) heightInRows.toFloat() / rowCount else 1f
        stripHeight = if (hasStrip) stripPainter.height(scale) else 0f
        labelPaint.textSize = labelSize * scale
        smallLabelPaint.textSize = smallLabelSize * scale
        hintPaint.textSize = hintSize * scale
        stripPainter.layout(MeasureSpec.getSize(widthMeasureSpec).toFloat(), scale, edgePadding)
        val height = (stripHeight + rowCount * rowHeight + 2 * edgePadding).toInt() + bottomInset
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), height)
    }

    /** Keeps the keys clear of a navigation bar drawn over the keyboard's window. */
    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        val bottom = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            insets.getInsets(WindowInsets.Type.systemBars()).bottom
        } else {
            @Suppress("DEPRECATION")
            insets.systemWindowInsetBottom
        }
        if (bottom != bottomInset) {
            bottomInset = bottom
            requestLayout()
        }
        return super.onApplyWindowInsets(insets)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        layoutKeys()
    }

    /**
     * Lays the engine's rows out afresh. Fingers already down keep the key
     * they landed on, so a layer change under a held key (shift, then a
     * letter with the other thumb) does not lose it.
     */
    private fun layoutKeys() {
        val rows = shownRows()
        grid = if (rows == null || width == 0) null else {
            KeyGrid(rows, width.toFloat(), rowHeight, edgePadding, keyGap, rowGap, top = stripHeight)
        }
        val engine = engine ?: return
        val tabs = grid?.cells?.firstOrNull { it.key === EMOJI_TABS } ?: return
        val gridTop = stripHeight + edgePadding
        emojiPanel.layout(
            engine.emojiCategories,
            grid = RectF(edgePadding, gridTop, width - edgePadding, gridTop + EMOJI_GRID_ROWS * rowHeight),
            tabs = RectF(tabs.left, tabs.top, tabs.right, tabs.bottom),
            rowHeight = rowHeight,
        )
    }

    /**
     * The rows to lay out. On the emoji panel, the grid takes the place of all
     * but the last row, and the category tabs fill the bar between its keys.
     */
    private fun shownRows(): List<List<Key>>? {
        val engine = engine ?: return null
        val rows = engine.rows
        if (engine.layer != Layer.Emoji) return rows
        val bar = rows.last()
        return List(EMOJI_GRID_ROWS) { emptyList<Key>() } + listOf(listOf(bar.first(), EMOJI_TABS) + bar.drop(1))
    }

    // ---- drawing ----

    /** Which colours a key is drawn in. */
    private enum class KeyStyle { Plain, Action, Accent }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(palette.background)
        val strip = engine?.strip
        if (strip != null && stripHeight > 0f) {
            val moving = stripPainter.draw(canvas, strip, stripTarget, SystemClock.uptimeMillis())
            if (moving) postInvalidateOnAnimation()
        }
        for (cell in grid?.cells.orEmpty()) {
            // A blank is drawn as nothing.
            if (cell.key !== EMOJI_TABS && cell.key !is Blank) drawKey(canvas, cell)
        }
        if (engine?.layer == Layer.Emoji && emojiPanel.draw(canvas)) postInvalidateOnAnimation()
    }

    private fun drawKey(canvas: Canvas, cell: Cell) {
        val key = cell.key
        val style = styleOf(cell)
        val isPressed = touches.values.any { it.cell.isAt(cell) }
        keyPaint.color = when {
            isPressed -> palette.keyPressed
            style == KeyStyle.Accent -> palette.accent
            style == KeyStyle.Action -> palette.actionKey
            else -> palette.key
        }
        val ink = when {
            isPressed -> palette.label
            style == KeyStyle.Accent -> palette.onAccent
            style == KeyStyle.Action -> palette.actionLabel
            else -> palette.label
        }
        labelPaint.color = ink
        smallLabelPaint.color = ink
        iconPaint.color = ink
        val radius = if (isPill(cell)) cell.height / 2 else keyRadius
        canvas.drawRoundRect(cell.left, cell.top, cell.right, cell.bottom, radius, radius, keyPaint)
        when (key) {
            is CharKey -> {
                val hint = key.hint
                if (hint == null) {
                    drawLabel(canvas, key.text, cell, labelPaint)
                } else {
                    // The label moves down a little to leave the corner to the hint.
                    drawLabel(canvas, key.text, cell, labelPaint, lowerBy = cell.height * HINT_SHIFT)
                    val inset = 3f * density
                    // On a pressed key the hint takes the label's colour, which keeps its contrast.
                    hintPaint.color = if (isPressed) palette.label else palette.hint
                    canvas.drawText(hint, cell.right - inset, cell.top + inset / 2 - hintPaint.ascent(), hintPaint)
                }
            }
            is ActionKey -> drawAction(canvas, key.action, cell)
            is Blank -> Unit
        }
    }

    private fun styleOf(cell: Cell): KeyStyle = when (val key = cell.key) {
        // The comma and full stop in the bottom row are coloured with the action keys; a pad's keys are all plain.
        is CharKey -> if (grid?.isInBottomRow(cell) == true && engine?.layer?.isPad != true) KeyStyle.Action else KeyStyle.Plain
        is Blank -> KeyStyle.Plain
        is ActionKey -> when (key.action) {
            Action.Space, Action.Globe, Action.Translate -> KeyStyle.Plain
            Action.Shift -> if (engine?.shift == ShiftState.Locked) KeyStyle.Accent else KeyStyle.Action
            Action.Enter -> if (engine?.enterAction == EnterAction.NewLine) KeyStyle.Action else KeyStyle.Accent
            Action.Backspace, Action.Symbols, Action.MoreSymbols, Action.Letters, Action.Emoji -> KeyStyle.Action
        }
    }

    /** The two keys at the ends of the bottom row are drawn with fully rounded ends. */
    private fun isPill(cell: Cell): Boolean {
        val key = cell.key
        val isEndKey = key is ActionKey &&
            (key.action == Action.Symbols || key.action == Action.Letters || key.action == Action.Enter)
        return isEndKey && grid?.isInBottomRow(cell) == true
    }

    /**
     * Draws [text] in the middle of [cell], smaller if it would not fit the
     * key's width. Myanmar is drawn a little smaller than Latin, because its
     * letters are wider and its stacked consonants taller.
     */
    private fun drawLabel(canvas: Canvas, text: String, cell: Cell, paint: Paint, lowerBy: Float = 0f) {
        val fullSize = paint.textSize
        if (text.any { it in MYANMAR }) paint.textSize = fullSize * MYANMAR_SCALE
        paint.shrinkToFit(text, room = cell.width - 2 * density)
        val baseline = (cell.top + cell.bottom) / 2 + lowerBy - (paint.descent() + paint.ascent()) / 2
        canvas.drawText(text, cell.centerX, baseline, paint)
        paint.textSize = fullSize
    }

    private fun drawAction(canvas: Canvas, action: Action, cell: Cell) {
        val cx = cell.centerX
        val cy = (cell.top + cell.bottom) / 2
        val half = 10f * density * keyScale // half the picture's size
        iconPath.rewind()
        iconPaint.style = Paint.Style.STROKE
        when (action) {
            Action.Space ->
                return drawLabel(canvas, engine?.spaceLabel.orEmpty(), cell, smallLabelPaint)
            Action.Symbols ->
                return drawLabel(canvas, engine?.symbolsLabel.orEmpty(), cell, smallLabelPaint)
            Action.MoreSymbols -> return drawLabel(canvas, "=\\<", cell, smallLabelPaint)
            Action.Letters ->
                return drawLabel(canvas, engine?.lettersLabel.orEmpty(), cell, smallLabelPaint)
            Action.Globe -> {
                // a circle with a meridian and an equator
                iconPath.addCircle(cx, cy, half, Path.Direction.CW)
                iconPath.addOval(cx - half * 0.45f, cy - half, cx + half * 0.45f, cy + half, Path.Direction.CW)
                iconPath.moveTo(cx - half, cy)
                iconPath.lineTo(cx + half, cy)
            }
            Action.Shift -> {
                // an upward arrow: outlined when off, filled for one-shot shift, underlined on the accent for shift lock
                val shift = engine?.shift ?: ShiftState.Off
                iconPath.moveTo(cx, cy - half)
                iconPath.lineTo(cx + half, cy)
                iconPath.lineTo(cx + half * 0.45f, cy)
                iconPath.lineTo(cx + half * 0.45f, cy + half * 0.7f)
                iconPath.lineTo(cx - half * 0.45f, cy + half * 0.7f)
                iconPath.lineTo(cx - half * 0.45f, cy)
                iconPath.lineTo(cx - half, cy)
                iconPath.close()
                if (shift != ShiftState.Off) iconPaint.style = Paint.Style.FILL_AND_STROKE
                if (shift == ShiftState.Locked) {
                    canvas.drawLine(cx - half * 0.6f, cy + half * 1.15f, cx + half * 0.6f, cy + half * 1.15f, iconPaint)
                }
            }
            Action.Backspace -> {
                // a left-pointing tag with a cross in it
                iconPath.moveTo(cx - half * 1.3f, cy)
                iconPath.lineTo(cx - half * 0.5f, cy - half * 0.8f)
                iconPath.lineTo(cx + half * 1.2f, cy - half * 0.8f)
                iconPath.lineTo(cx + half * 1.2f, cy + half * 0.8f)
                iconPath.lineTo(cx - half * 0.5f, cy + half * 0.8f)
                iconPath.close()
                iconPath.moveTo(cx - half * 0.05f, cy - half * 0.35f)
                iconPath.lineTo(cx + half * 0.65f, cy + half * 0.35f)
                iconPath.moveTo(cx + half * 0.65f, cy - half * 0.35f)
                iconPath.lineTo(cx - half * 0.05f, cy + half * 0.35f)
            }
            Action.Enter -> enterIcon(engine?.enterAction ?: EnterAction.NewLine, cx, cy, half)
            // a Latin A and a Myanmar letter with a two-way arrow between them
            Action.Translate -> return drawLabel(canvas, "A⇄က", cell, smallLabelPaint)
            Action.Emoji -> {
                // a round face with two eyes and a smile
                iconPath.addCircle(cx, cy, half, Path.Direction.CW)
                iconPath.addArc(cx - half * 0.5f, cy - half * 0.35f, cx + half * 0.5f, cy + half * 0.5f, 25f, 130f)
                iconPath.addCircle(cx - half * 0.35f, cy - half * 0.3f, half * 0.06f, Path.Direction.CW)
                iconPath.addCircle(cx + half * 0.35f, cy - half * 0.3f, half * 0.06f, Path.Direction.CW)
            }
        }
        canvas.drawPath(iconPath, iconPaint)
    }

    /** Adds to [iconPath] the picture of what enter does in this text field. */
    private fun enterIcon(action: EnterAction, cx: Float, cy: Float, half: Float) {
        when (action) {
            EnterAction.NewLine -> {
                // down, then left, with an arrow head
                iconPath.moveTo(cx + half, cy - half * 0.7f)
                iconPath.lineTo(cx + half, cy + half * 0.3f)
                iconPath.lineTo(cx - half, cy + half * 0.3f)
                iconPath.moveTo(cx - half * 0.4f, cy - half * 0.3f)
                iconPath.lineTo(cx - half, cy + half * 0.3f)
                iconPath.lineTo(cx - half * 0.4f, cy + half * 0.9f)
            }
            EnterAction.Search -> {
                // a magnifying glass
                iconPath.addCircle(cx - half * 0.2f, cy - half * 0.2f, half * 0.7f, Path.Direction.CW)
                iconPath.moveTo(cx + half * 0.3f, cy + half * 0.3f)
                iconPath.lineTo(cx + half, cy + half)
            }
            EnterAction.Send -> {
                // a paper plane pointing right
                iconPath.moveTo(cx - half, cy - half * 0.8f)
                iconPath.lineTo(cx + half, cy)
                iconPath.lineTo(cx - half, cy + half * 0.8f)
                iconPath.lineTo(cx - half * 0.6f, cy)
                iconPath.close()
                iconPath.moveTo(cx - half * 0.6f, cy)
                iconPath.lineTo(cx + half, cy)
            }
            EnterAction.Go -> {
                // an arrow pointing right
                iconPath.moveTo(cx - half, cy)
                iconPath.lineTo(cx + half, cy)
                iconPath.moveTo(cx + half * 0.3f, cy - half * 0.7f)
                iconPath.lineTo(cx + half, cy)
                iconPath.lineTo(cx + half * 0.3f, cy + half * 0.7f)
            }
            EnterAction.Next -> {
                // an arrow pointing right at a bar: on to the next text field
                iconPath.moveTo(cx - half, cy)
                iconPath.lineTo(cx + half * 0.6f, cy)
                iconPath.moveTo(cx - half * 0.1f, cy - half * 0.7f)
                iconPath.lineTo(cx + half * 0.6f, cy)
                iconPath.lineTo(cx - half * 0.1f, cy + half * 0.7f)
                iconPath.moveTo(cx + half, cy - half * 0.8f)
                iconPath.lineTo(cx + half, cy + half * 0.8f)
            }
            EnterAction.Done -> {
                // a tick
                iconPath.moveTo(cx - half, cy)
                iconPath.lineTo(cx - half * 0.3f, cy + half * 0.7f)
                iconPath.lineTo(cx + half, cy - half * 0.7f)
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
                dropPanelTouch()
                val index = event.actionIndex
                val x = event.getX(index)
                val y = event.getY(index)
                val cell = grid?.cellAt(x, y)
                when {
                    y < stripHeight -> stripDown(event.getPointerId(index), x, y)
                    engine?.layer == Layer.Emoji && (emojiPanel.contains(x, y) || cell?.key === EMOJI_TABS) -> {
                        panelPointer = event.getPointerId(index)
                        emojiPanel.down(event, index)
                        panelHoldTimer = Runnable { onEmojiHeld(x, y) }.also { postDelayed(it, HOLD_MILLIS) }
                    }
                    cell != null -> touchDown(event.getPointerId(index), Touch(cell, x, y))
                }
            }
            MotionEvent.ACTION_MOVE ->
                for (index in 0 until event.pointerCount) {
                    val pointerId = event.getPointerId(index)
                    if (pointerId == panelPointer) panelMove(event, index)
                    val touch = touches[pointerId] ?: continue
                    slide(touch, event.getX(index), event.getY(index))
                }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val index = event.actionIndex
                val pointerId = event.getPointerId(index)
                when (pointerId) {
                    stripPointer -> stripUp(event.getX(index), event.getY(index))
                    panelPointer -> panelUp(event, index, pointerId)
                    else -> lift(pointerId)
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                dropAll()
                dropPanelTouch()
                stripPointer = null
                stripTarget = null
            }
        }
        invalidate()
        return true
    }

    override fun onDetachedFromWindow() {
        dropAll()
        dropPanelTouch()
        super.onDetachedFromWindow()
    }

    /** The finger has stayed on an emoji: its skin tones are offered, if it has any. */
    private fun onEmojiHeld(x: Float, y: Float) {
        panelHoldTimer = null
        val held = emojiPanel.held() ?: return
        heldEmoji = held
        skinToneChoice = AlternateChoice(held.emoji.skinTones, x, y)
        val cell = held.cell
        callout.showAlternates(this, Cell(CharKey(held.emoji.text), cell.left, cell.top, cell.right, cell.bottom), held.emoji.skinTones)
        invalidate()
    }

    /** The finger moved on the emoji panel: it scrolls the grid, or picks a skin tone once they are offered. */
    private fun panelMove(event: MotionEvent, index: Int) {
        val choice = skinToneChoice ?: return emojiPanel.move(event, index)
        val x = event.getX(index)
        if (choice.selected == null) {
            val moved = maxOf(abs(x - choice.startX), abs(event.getY(index) - choice.startY))
            if (moved < touchSlop) return
        }
        choice.selected = callout.alternateAt(x)
        callout.select(choice.selected)
    }

    /**
     * The finger lifted from the emoji panel: an emoji tapped is typed. With
     * skin tones on offer, the one the finger slid to is typed, or the emoji
     * without a skin tone when it never slid.
     */
    private fun panelUp(event: MotionEvent, index: Int, pointerId: Int) {
        val held = heldEmoji
        val choice = skinToneChoice
        val lift = emojiPanel.up(event, index, pointerId)
        dropPanelTouch()
        val emoji = when {
            held != null && choice != null -> choice.selected?.let(choice.alternates::get) ?: held.emoji.text
            lift is EmojiPanel.Lift.Typed -> lift.emoji
            else -> return
        }
        if (hapticsEnabled) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        host?.edit { typeEmoji(emoji) }
    }

    /** Forgets the finger on the emoji panel without typing anything. */
    private fun dropPanelTouch() {
        if (panelPointer == null) return
        panelPointer = null
        panelHoldTimer?.let(::removeCallbacks)
        panelHoldTimer = null
        if (skinToneChoice != null) callout.dismiss()
        heldEmoji = null
        skinToneChoice = null
        emojiPanel.cancel()
    }

    /** A finger went down on the translate strip. Only the translation responds. */
    private fun stripDown(pointerId: Int, x: Float, y: Float) {
        val strip = engine?.strip ?: return
        val target = stripPainter.targetAt(x, y, strip) ?: return
        stripPointer = pointerId
        stripTarget = target
        if (hapticsEnabled) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    /** The finger lifted from the strip: the part it went down on acts if the finger is still on it. */
    private fun stripUp(x: Float, y: Float) {
        val target = stripTarget
        stripPointer = null
        stripTarget = null
        val strip = engine?.strip ?: return
        if (target == null || stripPainter.targetAt(x, y, strip) != target) return
        when (target) {
            StripPainter.Target.Replace -> host?.edit { replaceWithResult() }
        }
    }

    private fun touchDown(pointerId: Int, touch: Touch) {
        val cell = touch.cell
        touches[pointerId] = touch
        if (hapticsEnabled) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        val key = cell.key
        if (showsKeyPreview && key is CharKey) callout.showPreview(this, cell, key.text)
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
                touch.choice = AlternateChoice(hold.alternates, touch.x, touch.y)
                callout.showAlternates(this, touch.cell, hold.alternates)
            }
            Hold.GrammarKey -> offer(touch, hold, GRAMMAR_KEY_LABEL)
            Hold.EmojiKey -> offer(touch, hold, EMOJI_KEY_LABEL)
            Hold.None, null -> Unit
        }
    }

    /** Shows the key that [hold] offers above the held key. */
    private fun offer(touch: Touch, hold: Hold, label: String) {
        touch.offered = hold
        callout.showAlternates(this, touch.cell, listOf(label))
    }

    /** Presses the held key now and again shortly, until the finger lifts. */
    private fun repeat(touch: Touch) {
        host?.edit { press(touch.cell.key) }
        touch.repeatTimer = Runnable { repeat(touch) }.also { postDelayed(it, REPEAT_MILLIS) }
    }

    /** The finger moved. Only matters while alternates or an offered key are showing. */
    private fun slide(touch: Touch, x: Float, y: Float) {
        touch.x = x
        touch.y = y
        if (touch.offered != null) {
            // The offered key floats above the held key: the finger is on it once above the key.
            touch.onOfferedKey = y < touch.cell.top
            callout.select(if (touch.onOfferedKey) 0 else null)
            return
        }
        val choice = touch.choice ?: return
        if (choice.selected == null) {
            val moved = maxOf(abs(x - choice.startX), abs(y - choice.startY))
            if (moved < touchSlop) return
        }
        choice.selected = callout.alternateAt(x)
        callout.select(choice.selected)
    }

    private fun liftAll() {
        for (pointerId in touches.keys.toList()) lift(pointerId)
    }

    /** The finger lifted: the key under it, or the alternate it slid to, is typed. */
    private fun lift(pointerId: Int) {
        val touch = drop(pointerId) ?: return
        if (touch.consumed) return
        val offered = touch.offered
        if (offered != null) {
            // Lifting anywhere but on the offered key does nothing.
            if (touch.onOfferedKey) {
                host?.edit { if (offered == Hold.EmojiKey) showEmojiPanel() else checkGrammar() }
            }
            return
        }
        val key = touch.cell.key
        val alternate = touch.choice?.let { choice -> choice.selected?.let(choice.alternates::get) }
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
        callout.dismiss()
        return touch
    }

    private fun dropAll() {
        for (pointerId in touches.keys.toList()) drop(pointerId)
    }

    private companion object {
        /** Height of a row of keys where the screen has room for it. */
        const val FULL_ROW_DP = 45f

        /** The Myanmar block of Unicode, and how large its letters are drawn beside Latin ones. */
        val MYANMAR = '\u1000'..'\u109F'
        const val MYANMAR_SCALE = 0.88f

        /** How far down its key a label moves to make room for a hint, as a share of the key's height. */
        const val HINT_SHIFT = 0.07f

        /** The most of the screen's height the keys may take. */
        const val MAX_SCREEN_SHARE = 0.5f

        /** How long a finger must stay down to count as holding the key. */
        const val HOLD_MILLIS = 350L

        /** Time between presses of a held, repeating key. */
        const val REPEAT_MILLIS = 60L

        /** The grammar key, shown above the held translate key. */
        const val GRAMMAR_KEY_LABEL = "A✓"

        /** The emoji key, shown above the held comma key. */
        const val EMOJI_KEY_LABEL = "😀"

        /** Rows of the emoji panel's grid, above its bar. */
        const val EMOJI_GRID_ROWS = 4

        /**
         * Stands for the emoji panel's category tabs when the bar is laid out,
         * filling the row between the bar's two keys; it is never pressed or
         * drawn as a key.
         */
        val EMOJI_TABS = CharKey("", width = 7f)
    }
}
