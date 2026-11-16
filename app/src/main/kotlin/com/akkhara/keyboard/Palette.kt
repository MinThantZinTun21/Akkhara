package com.akkhara.keyboard

/** The fixed colours of the keyboard surface, as ARGB ints. */
data class Palette(
    val background: Int,
    val key: Int,
    val keyPressed: Int,
    val actionKey: Int,
    val label: Int,
) {
    companion object {
        val Light = Palette(
            background = 0xFFE8EAED.toInt(),
            key = 0xFFFFFFFF.toInt(),
            keyPressed = 0xFFC4C7CC.toInt(),
            actionKey = 0xFFCCD0D5.toInt(),
            label = 0xFF1F1F1F.toInt(),
        )
    }
}
