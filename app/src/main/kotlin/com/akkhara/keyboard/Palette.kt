package com.akkhara.keyboard

/**
 * The fixed colours of the keyboard surface, as ARGB ints. There are exactly
 * two, and neither takes anything from the wallpaper.
 *
 * In both, [label] has a contrast ratio of at least 5.8:1 against [key],
 * [keyPressed], [actionKey] and [callout], and [onAccent] at least 6.6:1
 * against [accent], so small Myanmar marks stay readable (the floor is 4.5:1).
 */
data class Palette(
    val background: Int,
    val key: Int,
    val keyPressed: Int,
    val actionKey: Int,
    val label: Int,
    /** Background of the key preview and of the row of alternates. */
    val callout: Int,
    /** The selected alternate, and the shift key while the shift layer is active. */
    val accent: Int,
    val onAccent: Int,
) {
    companion object {
        val Light = Palette(
            background = 0xFFE8EAED.toInt(),
            key = 0xFFFFFFFF.toInt(),
            keyPressed = 0xFF9AA0A8.toInt(),
            actionKey = 0xFFCCD0D5.toInt(),
            label = 0xFF1F1F1F.toInt(),
            callout = 0xFFFFFFFF.toInt(),
            accent = 0xFF00695C.toInt(),
            onAccent = 0xFFFFFFFF.toInt(),
        )

        val Dark = Palette(
            background = 0xFF1B1C1E.toInt(),
            key = 0xFF3A3D41.toInt(),
            keyPressed = 0xFF5A5E64.toInt(),
            actionKey = 0xFF2A2C2F.toInt(),
            label = 0xFFF1F3F4.toInt(),
            callout = 0xFF4A4D52.toInt(),
            accent = 0xFF80CBC4.toInt(),
            onAccent = 0xFF00201C.toInt(),
        )

        /** The palette for the system's light or dark setting. */
        fun forSystem(isDark: Boolean): Palette = if (isDark) Dark else Light
    }
}
