package com.akkhara.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import com.akkhara.keyboard.engine.Action
import com.akkhara.keyboard.engine.ActionKey
import com.akkhara.keyboard.engine.CharKey
import com.akkhara.keyboard.engine.Key
import com.akkhara.keyboard.engine.KeyboardEngine
import com.akkhara.keyboard.engine.ShiftState

/**
 * Draws the layer the engine is showing and reports touches back to it.
 * It holds no typing logic: what a key does is decided by the engine.
 */
@SuppressLint("ViewConstructor")
class KeyboardView(context: Context) : View(context) {

    private class Cell(val key: Key, val bounds: RectF)

    private var engine: KeyboardEngine? = null
    private var dispatch: ((KeyboardEngine.() -> Unit) -> Unit)? = null

    private val palette = Palette.Light
    private val density = resources.displayMetrics.density
    private val rowHeight = 54f * density
    private val keyGap = 3f * density
    private val keyRadius = 6f * density
    private val edgePadding = 4f * density

    private val cells = ArrayList<Cell>()
    private val held = LinkedHashMap<Int, Cell>() // pointer id -> the key under that finger

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

    fun bind(engine: KeyboardEngine, dispatch: ((KeyboardEngine.() -> Unit) -> Unit)) {
        this.engine = engine
        this.dispatch = dispatch
        onEngineChanged()
    }

    /** Call after anything that may have changed which keys are showing. */
    fun onEngineChanged() {
        layoutKeys()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val rowCount = engine?.rows?.size ?: 0
        val height = (rowCount * rowHeight + 2 * edgePadding).toInt()
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), height)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        layoutKeys()
    }

    private fun layoutKeys() {
        cells.clear()
        held.clear()
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
                    RectF(
                        x + keyGap / 2,
                        top + keyGap,
                        x + keyWidth - keyGap / 2,
                        top + rowHeight - keyGap,
                    ),
                )
                x += keyWidth
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(palette.background)
        labelPaint.color = palette.label
        smallLabelPaint.color = palette.label
        iconPaint.color = palette.label
        for (cell in cells) {
            keyPaint.color = when {
                held.containsValue(cell) -> palette.keyPressed
                cell.key is ActionKey && cell.key.action != Action.Space -> palette.actionKey
                else -> palette.key
            }
            canvas.drawRoundRect(cell.bounds, keyRadius, keyRadius, keyPaint)
            when (val key = cell.key) {
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
            Action.Space -> return
            Action.Symbols -> return drawLabel(canvas, "?123", bounds, smallLabelPaint)
            Action.MoreSymbols -> return drawLabel(canvas, "=\\<", bounds, smallLabelPaint)
            Action.Letters -> return drawLabel(canvas, "ABC", bounds, smallLabelPaint)
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
            Action.Enter -> {
                // down, then left, with an arrow head
                iconPath.moveTo(cx + s, cy - s * 0.7f)
                iconPath.lineTo(cx + s, cy + s * 0.3f)
                iconPath.lineTo(cx - s, cy + s * 0.3f)
                iconPath.moveTo(cx - s * 0.4f, cy - s * 0.3f)
                iconPath.lineTo(cx - s, cy + s * 0.3f)
                iconPath.lineTo(cx - s * 0.4f, cy + s * 0.9f)
            }
        }
        canvas.drawPath(iconPath, iconPaint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                // A second finger landing commits the first, so fast two-thumb typing never drops a key.
                releaseAll()
                val index = event.actionIndex
                cellAt(event.getX(index), event.getY(index))?.let {
                    held[event.getPointerId(index)] = it
                }
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                release(event.getPointerId(event.actionIndex))
                invalidate()
            }
            MotionEvent.ACTION_CANCEL -> {
                held.clear()
                invalidate()
            }
        }
        return true
    }

    private fun releaseAll() {
        for (pointerId in held.keys.toList()) release(pointerId)
    }

    private fun release(pointerId: Int) {
        val cell = held.remove(pointerId) ?: return
        dispatch?.invoke { press(cell.key) }
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
    }
}
