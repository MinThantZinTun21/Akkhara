package com.akkhara.keyboard

/**
 * The colours one [Theme] draws the keyboard in, as ARGB ints. Nothing in a
 * palette is taken from the wallpaper.
 *
 * In every theme, [label] has a contrast ratio of at least 4.5:1 against [key],
 * [keyPressed] and [callout], [actionLabel] against [actionKey], [hint]
 * against [key] and [onAccent] against [accent], so small Myanmar marks stay
 * readable. [accent] has at least 3:1 against [background], where it draws
 * marks that are not text. ThemesTest checks both floors.
 */
data class Palette(
    val background: Int,
    val key: Int,
    val keyPressed: Int,
    /**
     * Shift, backspace, the layer keys and an enter that starts a new line, and
     * the comma and full stop in the bottom row. Space and globe are drawn as [key].
     */
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
)

/**
 * A look the user can pick for the keyboard: one fixed [palette], light or
 * dark. See GLOSSARY.md, "Theme". The list is built into the app; ThemesTest
 * holds every theme to the palette's contrast floors.
 */
enum class Theme(val isDark: Boolean, val palette: Palette) {
    Light(
        isDark = false,
        Palette(
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
        ),
    ),

    Dark(
        isDark = true,
        Palette(
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
        ),
    ),
    ;

    companion object {
        /**
         * The theme the keyboard is drawn in: the one the user [chosen], or,
         * while following the system, Light or Dark from the phone's dark mode.
         */
        fun shown(chosen: Theme?, systemIsDark: Boolean): Theme = chosen ?: if (systemIsDark) Dark else Light
    }
}
