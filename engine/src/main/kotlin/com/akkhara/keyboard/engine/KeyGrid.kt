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
 * [padding] is the empty band along each edge of the keyboard and [gap] the
 * space between neighbouring keys.
 */
class KeyGrid(
    rows: List<List<Key>>,
    width: Float,
    private val rowHeight: Float,
    private val padding: Float,
    gap: Float,
) {
    private val rows: List<List<Cell>> = run {
        val unit = (width - 2 * padding) / COLUMNS
        rows.mapIndexed { rowIndex, row ->
            val rowWidth = row.sumOf { it.width.toDouble() }.toFloat() * unit
            var x = (width - rowWidth) / 2f
            val top = padding + rowIndex * rowHeight
            row.map { key ->
                val keyWidth = key.width * unit
                Cell(key, x + gap / 2, top + gap, x + keyWidth - gap / 2, top + rowHeight - gap)
                    .also { x += keyWidth }
            }
        }
    }

    val cells: List<Cell> = this.rows.flatten()

    /** Height of all the rows with the padding above and below them. */
    val height: Float = rows.size * rowHeight + 2 * padding

    /**
     * The key a touch at ([x], [y]) lands on. A touch in the space beside or
     * between keys goes to the nearest key in its row, so no touch on the
     * keyboard is lost. Null only above or below the keyboard.
     */
    fun cellAt(x: Float, y: Float): Cell? {
        if (rows.isEmpty() || y < 0f || y > height) return null
        val rowIndex = floor((y - padding) / rowHeight).toInt().coerceIn(0, rows.lastIndex)
        return rows[rowIndex].minByOrNull { cell ->
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
