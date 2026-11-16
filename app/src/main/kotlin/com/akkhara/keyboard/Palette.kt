package com.akkhara.keyboard

/**
 * The fixed colours of the keyboard surface, as ARGB ints. There are exactly
 * two, and neither takes anything from the wallpaper.
 *
 * In both, [label] has a contrast ratio of at least 5.2:1 against [key],
 * [keyPressed], [actionKey] and [callout], [actionLabel] at least 7.4:1
 * against [actionKey], [hint] at least 6:1 against [key] and [onAccent] at
 * least 6.4:1 against [accent], so small Myanmar marks stay readable (the
 * floor is 4.5:1).
 */
data class Palette(
    val background: Int,
    val key: Int,
    val keyPressed: Int,
    /** The keys that act instead of typing a letter, and the punctuation beside the space bar. */
    val actionKey: Int,
    val label: Int,
    /** Labels and pictures on [actionKey]. */
    val actionLabel: Int,
    /** The small character in a key's corner. */
    val hint: Int,
    /** Background of the key preview and of the row of alternates. */
    val callout: Int,
    /** The selected alternate, shift lock, and an enter key that does more than start a new line. */
    val accent: Int,
    val onAccent: Int,
) {
    companion object {
        val Light = Palette(
            background = 0xFFE6E9E3.toInt(),
            key = 0xFFFFFFFF.toInt(),
            keyPressed = 0xFFB4BAB2.toInt(),
            actionKey = 0xFFD0E2CC.toInt(),
            label = 0xFF1F1F1F.toInt(),
            actionLabel = 0xFF1B3A20.toInt(),
            hint = 0xFF5C605D.toInt(),
            callout = 0xFFFFFFFF.toInt(),
            accent = 0xFF2E6B34.toInt(),
            onAccent = 0xFFFFFFFF.toInt(),
        )

        val Dark = Palette(
            background = 0xFF1F1F1F.toInt(),
            key = 0xFF3A3A3A.toInt(),
            keyPressed = 0xFF5E5E5E.toInt(),
            actionKey = 0xFF37493A.toInt(),
            label = 0xFFE8E8E8.toInt(),
            actionLabel = 0xFFD3E8CF.toInt(),
            hint = 0xFFBDBDBD.toInt(),
            callout = 0xFF4C4C4C.toInt(),
            accent = 0xFFA5D6A0.toInt(),
            onAccent = 0xFF0E2A12.toInt(),
        )

        /** The palette for the system's light or dark setting. */
        fun forSystem(isDark: Boolean): Palette = if (isDark) Dark else Light
    }
}
