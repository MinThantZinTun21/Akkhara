package com.akkhara.keyboard

/** The fixed colours of the keyboard surface, as ARGB ints. */
data class Palette(
    val background: Int,
    val key: Int,
    val keyPressed: Int,
    val actionKey: Int,
    val label: Int,
    /** Background of the key preview and alternates bubble. */
    val bubble: Int,
    /** The selected alternate, and the shift key while the shift layer is active. */
    val accent: Int,
    val onAccent: Int,
) {
    companion object {
        val Light = Palette(
            background = 0xFFE8EAED.toInt(),
            key = 0xFFFFFFFF.toInt(),
            keyPressed = 0xFFC4C7CC.toInt(),
            actionKey = 0xFFCCD0D5.toInt(),
            label = 0xFF1F1F1F.toInt(),
            bubble = 0xFFFFFFFF.toInt(),
            accent = 0xFF00695C.toInt(),
            onAccent = 0xFFFFFFFF.toInt(),
        )
    }
}
