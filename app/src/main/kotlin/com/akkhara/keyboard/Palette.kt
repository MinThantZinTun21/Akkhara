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
