package com.akkhara.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration
import android.widget.OverScroller
import androidx.core.graphics.withClip
import com.akkhara.keyboard.engine.Emoji
import com.akkhara.keyboard.engine.EmojiCategory
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Draws the emoji panel's grid and category tabs, scrolls the grid, and works
 * out what a touch on them means. Like [KeyboardView] it holds no typing
 * logic: an emoji the user taps is handed back for the engine to type.
 *
 * The grid is one list that scrolls through every category, each starting on
 * a new row; a tab jumps to its category, and the tab of the category in view
 * is marked. Only emoji the phone's font can draw are shown.
 *
 * The recent emoji come first, so the panel opens on them when there are any.
 */
class EmojiPanel(context: Context, private val palette: Palette, private val keyStyle: KeyStyle) {

    /** What a finger lifting from the panel did. */
    sealed interface Lift {
        data class Typed(val emoji: String) : Lift

        data object Nothing : Lift
    }

    /** The emoji a finger is holding, and the cell it fills in the View's coordinates. */
    class Held(val emoji: Emoji, val cell: RectF)

    private val density = context.resources.displayMetrics.density
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val minFling = ViewConfiguration.get(context).scaledMinimumFlingVelocity
    private val maxFling = ViewConfiguration.get(context).scaledMaximumFlingVelocity
    private val scroller = OverScroller(context)

    private val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val tabPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val markPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var categories: List<EmojiCategory> = emptyList()

    /** The grid row each category starts on, and the number of rows in all. */
    private var firstRows = IntArray(0)
    private var rowCount = 0

    private val grid = RectF()
    private val tabs = RectF()
    private var columns = 1
    private var cellWidth = 1f
    private var cellHeight = 1f

    /** How far the grid is scrolled, in pixels from its top. */
    private var scrollY = 0f
    private val maxScroll: Float get() = max(0f, rowCount * cellHeight - grid.height())

    private var velocity: VelocityTracker? = null
    private var downX = 0f
    private var downY = 0f
    private var lastY = 0f
    private var isDragging = false

    /** The tab the finger went down on, or the grid cell's emoji; null while scrolling. */
    private var downTab: Int? = null
    private var downEmoji: Int? = null

    /**
     * Fits the panel into [grid] and the [tabs] row of the bar. [rowHeight] is
     * the height of a row of keys, which sets the size of the emoji.
     */
    fun layout(categories: List<EmojiCategory>, grid: RectF, tabs: RectF, rowHeight: Float) {
        this.categories = drawable(categories)
        this.grid.set(grid)
        this.tabs.set(tabs)
        cellHeight = grid.height() / GRID_ROWS
        columns = max(MIN_COLUMNS, floor(grid.width() / (rowHeight * CELL_WIDTH_SHARE)).toInt())
        cellWidth = grid.width() / columns
        emojiPaint.textSize = min(cellWidth, cellHeight) * EMOJI_SIZE_SHARE
        tabPaint.textSize = min(tabs.width() / max(1, this.categories.size), tabs.height()) * TAB_SIZE_SHARE
        firstRows = IntArray(this.categories.size)
        var row = 0
        this.categories.forEachIndexed { i, category ->
            firstRows[i] = row
            row += ceil(category.emoji.size / columns.toFloat()).toInt()
        }
        rowCount = row
        scrollY = scrollY.coerceIn(0f, maxScroll)
    }

    /** Shows the start of the first category, as when the panel opens. */
    fun scrollToStart() {
        scroller.forceFinished(true)
        scrollY = 0f
    }

    /** Whether a touch at ([x], [y]) is the panel's: on the grid or on the tabs. */
    fun contains(x: Float, y: Float): Boolean = grid.contains(x, y) || tabs.contains(x, y)

    // ---- drawing ----

    /** Draws the grid and the tabs. Returns true while a fling is still moving the grid. */
    fun draw(canvas: Canvas): Boolean {
        val moving = scroller.computeScrollOffset()
        if (moving) scrollY = scroller.currY.toFloat().coerceIn(0f, maxScroll)
        drawGrid(canvas)
        drawTabs(canvas)
        return moving
    }

    private fun drawGrid(canvas: Canvas) = canvas.withClip(grid) {
        val first = floor(scrollY / cellHeight).toInt()
        val last = ceil((scrollY + grid.height()) / cellHeight).toInt()
        val baselineOffset = -(emojiPaint.descent() + emojiPaint.ascent()) / 2
        for (row in first..last) {
            val category = categoryOfRow(row) ?: continue
            val emoji = categories[category].emoji
            val start = (row - firstRows[category]) * columns
            for (column in 0 until columns) {
                val index = start + column
                if (index >= emoji.size) break
                val left = grid.left + column * cellWidth
                val top = grid.top + row * cellHeight - scrollY
                if (!isDragging && downEmoji == indexInPanel(category, index)) {
                    markPaint.color = palette.keyPressed
                    // Inside withClip, a bare `density` would be the Canvas's own, not the screen's.
                    val density = this@EmojiPanel.density
                    val inset = 2f * density
                    // The pressed emoji's mark follows the key style's shape.
                    val radius = keyStyle.cornerRadius(cellHeight - 2 * inset, density)
                    drawRoundRect(
                        left + inset, top + inset, left + cellWidth - inset, top + cellHeight - inset,
                        radius, radius, markPaint,
                    )
                }
                drawText(emoji[index].text, left + cellWidth / 2, top + cellHeight / 2 + baselineOffset, emojiPaint)
            }
        }
    }

    private fun drawTabs(canvas: Canvas) {
        if (categories.isEmpty()) return
        val width = tabs.width() / categories.size
        val shown = categoryInView()
        val baselineOffset = -(tabPaint.descent() + tabPaint.ascent()) / 2
        categories.forEachIndexed { i, category ->
            val left = tabs.left + i * width
            if (i == shown) {
                // The category in view is underlined in the accent colour.
                markPaint.color = palette.accent
                val bar = 2.5f * density
                canvas.drawRoundRect(
                    left + width * 0.2f, tabs.bottom - bar, left + width * 0.8f, tabs.bottom,
                    bar / 2, bar / 2, markPaint,
                )
            }
            canvas.drawText(TAB_ICONS.getValue(category.kind), left + width / 2, tabs.centerY() + baselineOffset, tabPaint)
        }
    }

    // ---- touch ----

    fun down(event: MotionEvent, index: Int) {
        val x = event.getX(index)
        val y = event.getY(index)
        scroller.forceFinished(true)
        velocity?.recycle()
        velocity = VelocityTracker.obtain().apply { addMovement(event) }
        downX = x
        downY = y
        lastY = y
        isDragging = false
        downTab = if (tabs.contains(x, y)) tabAt(x) else null
        downEmoji = if (grid.contains(x, y)) emojiAt(x, y) else null
    }

    fun move(event: MotionEvent, index: Int) {
        val x = event.getX(index)
        val y = event.getY(index)
        velocity?.addMovement(event)
        if (downTab != null) return
        if (!isDragging && max(abs(x - downX), abs(y - downY)) > touchSlop) isDragging = true
        if (isDragging) scrollY = (scrollY + lastY - y).coerceIn(0f, maxScroll)
        lastY = y
    }

    /** The finger lifted: a tap on a tab jumps to its category, and a tap on an emoji types it. */
    fun up(event: MotionEvent, index: Int, pointerId: Int): Lift {
        val x = event.getX(index)
        val y = event.getY(index)
        val tracker = velocity
        var lift: Lift = Lift.Nothing
        val tab = downTab
        val emoji = downEmoji
        when {
            tab != null -> if (tabs.contains(x, y) && tabAt(x) == tab) jumpTo(tab)
            isDragging && tracker != null -> {
                tracker.addMovement(event)
                tracker.computeCurrentVelocity(1000, maxFling.toFloat())
                val speed = -tracker.getYVelocity(pointerId)
                if (abs(speed) > minFling) {
                    scroller.fling(0, scrollY.toInt(), 0, speed.toInt(), 0, 0, 0, maxScroll.toInt())
                }
            }
            emoji != null && emojiAt(x, y) == emoji -> lift = Lift.Typed(emojiOf(emoji).text)
        }
        cancel()
        return lift
    }

    /**
     * The emoji the finger has stayed on, if it has skin tones to offer. Null
     * once the finger has scrolled the grid, and for an emoji with none.
     */
    fun held(): Held? {
        val place = downEmoji ?: return null
        val emoji = emojiOf(place)
        if (isDragging || emoji.skinTones.isEmpty()) return null
        val category = place shr 16
        val index = place and 0xFFFF
        val left = grid.left + index % columns * cellWidth
        val top = grid.top + (firstRows[category] + index / columns) * cellHeight - scrollY
        return Held(emoji, RectF(left, top, left + cellWidth, top + cellHeight))
    }

    /** Forgets the finger without doing anything. */
    fun cancel() {
        velocity?.recycle()
        velocity = null
        isDragging = false
        downTab = null
        downEmoji = null
    }

    private fun jumpTo(category: Int) {
        scroller.forceFinished(true)
        scrollY = (firstRows[category] * cellHeight).coerceIn(0f, maxScroll)
    }

    // ---- where things are ----

    private fun tabAt(x: Float): Int? {
        if (categories.isEmpty()) return null
        return floor((x - tabs.left) / (tabs.width() / categories.size)).toInt().coerceIn(0, categories.lastIndex)
    }

    /** The category the top of the grid shows; the last one once the grid is scrolled to its end. */
    private fun categoryInView(): Int {
        if (scrollY >= maxScroll && maxScroll > 0f) return categories.lastIndex
        return categoryOfRow(floor(scrollY / cellHeight + 0.01f).toInt()) ?: 0
    }

    private fun categoryOfRow(row: Int): Int? {
        if (row < 0 || row >= rowCount) return null
        return firstRows.indexOfLast { it <= row }.takeIf { it >= 0 }
    }

    /** The emoji under ([x], [y]) as its place in the whole panel, or null for an empty cell. */
    private fun emojiAt(x: Float, y: Float): Int? {
        val row = floor((y - grid.top + scrollY) / cellHeight).toInt()
        val column = floor((x - grid.left) / cellWidth).toInt().coerceIn(0, columns - 1)
        val category = categoryOfRow(row) ?: return null
        val index = (row - firstRows[category]) * columns + column
        if (index >= categories[category].emoji.size) return null
        return indexInPanel(category, index)
    }

    /** One number for an emoji's place: its category's in the high bits, its own in the low. */
    private fun indexInPanel(category: Int, index: Int) = category shl 16 or index

    private fun emojiOf(place: Int): Emoji = categories[place shr 16].emoji[place and 0xFFFF]

    private companion object {
        /** Rows of emoji in view at once; with the bar they are as tall as the keys they replace. */
        const val GRID_ROWS = 4

        const val MIN_COLUMNS = 6

        /** How wide an emoji's cell is, at most, as a share of a row of keys' height. */
        const val CELL_WIDTH_SHARE = 0.95f

        const val EMOJI_SIZE_SHARE = 0.62f
        const val TAB_SIZE_SHARE = 0.5f

        /** The emoji that stands for each category on its tab. */
        val TAB_ICONS = mapOf(
            EmojiCategory.Kind.Recent to "🕒",
            EmojiCategory.Kind.SmileysAndPeople to "😀",
            EmojiCategory.Kind.AnimalsAndNature to "🐻",
            EmojiCategory.Kind.FoodAndDrink to "🍔",
            EmojiCategory.Kind.TravelAndPlaces to "🚗",
            EmojiCategory.Kind.Activities to "⚽",
            EmojiCategory.Kind.Objects to "💡",
            EmojiCategory.Kind.Symbols to "🔣",
            EmojiCategory.Kind.Flags to "🏁",
        )

        /**
         * Unicode's categories with only the emoji the phone's font can draw,
         * worked out once: neither the list nor the font changes while the
         * keyboard runs.
         */
        private val drawableCache = HashMap<EmojiCategory.Kind, EmojiCategory>()

        /**
         * [categories] without the emoji and skin tones the font cannot draw,
         * and without a category left empty. The recent emoji were all chosen
         * from the panel, so they are kept as they are, even when there are none.
         */
        fun drawable(categories: List<EmojiCategory>): List<EmojiCategory> {
            val paint = Paint()
            return categories.mapNotNull { category ->
                if (category.kind == EmojiCategory.Kind.Recent) return@mapNotNull category
                drawableCache.getOrPut(category.kind) {
                    val emoji = category.emoji
                        .filter { paint.hasGlyph(it.text) }
                        .map { it.copy(skinTones = it.skinTones.filter(paint::hasGlyph)) }
                    category.copy(emoji = emoji)
                }.takeIf { it.emoji.isNotEmpty() }
            }
        }
    }
}
