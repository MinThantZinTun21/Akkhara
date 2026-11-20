package com.akkhara.keyboard.engine

import kotlin.math.floor

/** A key and the rectangle it fills on the keyboard, in pixels from the keyboard's top left. */
data class Cell(
    val key: Key,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2

    /** Whether [other] fills the same rectangle, whatever key it holds. */
    fun isAt(other: Cell): Boolean =
        left == other.left && top == other.top && right == other.right && bottom == other.bottom
}

/**
 * Where each key of a layer sits and which key a touch lands on.
 *
 * Key widths come from the [width] on offer, so the same layout serves
 * portrait and landscape. Rows narrower than the keyboard are centred.
 * [padding] is the empty band along each edge of the keyboard, [gap] the
 * space between neighbouring keys in a row and [rowGap] the space between rows.
 * [top] is how far down the keyboard the first row starts, leaving room above
 * it for the translate strip.
 */
class KeyGrid(
    rows: List<List<Key>>,
    width: Float,
    private val rowHeight: Float,
    private val padding: Float,
    gap: Float,
    rowGap: Float,
    private val top: Float = 0f,
) {
    private val rows: List<List<Cell>> = run {
        val unit = (width - 2 * padding) / COLUMNS
        rows.mapIndexed { rowIndex, row ->
            val rowWidth = row.sumOf { it.width.toDouble() }.toFloat() * unit
            var x = (width - rowWidth) / 2f
            val rowTop = top + padding + rowIndex * rowHeight
            row.map { key ->
                val keyWidth = key.width * unit
                Cell(key, x + gap / 2, rowTop + rowGap / 2, x + keyWidth - gap / 2, rowTop + rowHeight - rowGap / 2)
                    .also { x += keyWidth }
            }
        }
    }

    val cells: List<Cell> = this.rows.flatten()

    /** Whether [cell] is in the last row, the one with the space bar. */
    fun isInBottomRow(cell: Cell): Boolean = rows.lastOrNull()?.any { it.isAt(cell) } == true

    /** Height of all the rows with the padding above and below them. */
    val height: Float = rows.size * rowHeight + 2 * padding

    /**
     * The key a touch at ([x], [y]) lands on. A touch in the space beside or
     * between keys, or on an empty cell, goes to the nearest key in its row, so no touch on the
     * keyboard is lost. Null only above or below the keyboard.
     */
    fun cellAt(x: Float, y: Float): Cell? {
        if (rows.isEmpty() || y < top || y > top + height) return null
        val rowIndex = floor((y - top - padding) / rowHeight).toInt().coerceIn(0, rows.lastIndex)
        return rows[rowIndex].filter { it.key !is Blank }.minByOrNull { cell ->
            when {
                x < cell.left -> cell.left - x
                x > cell.right -> x - cell.right
                else -> 0f
            }
        }
    }

    private companion object {
        /** Width of the keyboard in ordinary character keys. */
        const val COLUMNS = 10f
    }
}
