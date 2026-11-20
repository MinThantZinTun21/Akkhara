package com.akkhara.keyboard

/**
 * The colours one [Theme] draws the keyboard in, as ARGB ints. Nothing in a
 * palette is taken from the wallpaper.
 *
 * In every theme, [label] has a contrast ratio of at least 4.5:1 against [key],
 * [keyPressed] and [callout], [actionLabel] against [actionKey], [hint]
 * against [key] and [onAccent] against [accent], so small Myanmar marks stay
 * readable. [accent] has at least 3:1 against the background, top to bottom, where it draws
 * marks that are not text. ThemesTest checks both floors.
 */
data class Palette(
    /** The keyboard's background, or the top of it when [backgroundBottom] differs. */
    val background: Int,
    /** The bottom of the background, which fades into it from [background]. The same colour for a one-colour background. */
    val backgroundBottom: Int = background,
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
) {
    /** True when the background fades from [background] at the top to [backgroundBottom]. */
    val isGradient: Boolean get() = backgroundBottom != background

    /**
     * The colours an action key is drawn in, in [style]: its own, or in a
     * see-through style the letter keys', so its label stays readable on the background.
     */
    fun actionColoursIn(style: KeyStyle): KeyColours =
        if (style.isSeeThrough) KeyColours(key, label) else KeyColours(actionKey, actionLabel)

    /** The colours to check marks on the background against: its top, middle and bottom, or its one colour. */
    val backgroundsAlongHeight: List<Int>
        get() = if (isGradient) listOf(background, blend(background, 0.5f, over = backgroundBottom), backgroundBottom) else listOf(background)
}

/** A key's fill and the colour of the label on it. */
data class KeyColours(val key: Int, val label: Int)

/** [colour] drawn at [alpha] over the opaque colour [over], as one opaque ARGB colour. */
fun blend(colour: Int, alpha: Float, over: Int): Int {
    fun channel(shift: Int): Int {
        val top = colour shr shift and 0xFF
        val under = over shr shift and 0xFF
        return Math.round(top * alpha + under * (1 - alpha))
    }
    return (0xFF shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
}
