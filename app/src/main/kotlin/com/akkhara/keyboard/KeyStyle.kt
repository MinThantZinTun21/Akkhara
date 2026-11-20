package com.akkhara.keyboard

import androidx.annotation.StringRes

/**
 * The shape every key is drawn with. A key style never changes a key's place
 * or size, the layout or the font. Every [Theme] has its own; the user can
 * pick one for every theme instead. See GLOSSARY.md, "Key style", and ADR 0007.
 */
enum class KeyStyle(@StringRes val nameRes: Int) {
    /** Today's keys: softly rounded, with fully rounded ends on the keys at the ends of the bottom row. */
    Rounded(R.string.key_style_rounded),

    /** Small corners. */
    Square(R.string.key_style_square),

    /** Fully rounded ends on every key. */
    Pill(R.string.key_style_pill),

    /** No fill, only a thin border; a pressed key fills with the pressed colour. */
    Outline(R.string.key_style_outline),

    /** A darker lip under each key, which a pressed key sinks onto. */
    Raised(R.string.key_style_raised),

    /** A see-through fill over the background, lit along its top edge. */
    Glass(R.string.key_style_glass),

    /** No fill and no border; a pressed key shows the pressed colour. */
    Borderless(R.string.key_style_borderless),
    ;

    /**
     * True when a key's own colour shows little or none of it, so its label
     * sits on the background. In these styles an action key is drawn in the
     * letter keys' colours; enter and a locked shift keep their solid accent.
     */
    val isSeeThrough: Boolean get() = this == Outline || this == Glass || this == Borderless

    /**
     * The colour a label sits on, on a key that is not pressed, whose own
     * colour is [key], over a keyboard [background]. A pressed key is always
     * filled with the pressed colour.
     */
    fun face(key: Int, background: Int): Int = when (this) {
        Rounded, Square, Pill, Raised -> key
        Glass -> blend(key, GLASS_ALPHA, over = background)
        Outline, Borderless -> background
    }

    /**
     * The corner radius, in pixels, of a key [height] pixels tall. [isEndKey]
     * is a key at either end of the bottom row.
     */
    fun cornerRadius(height: Float, density: Float, isEndKey: Boolean = false): Float = when (this) {
        Rounded -> if (isEndKey) height / 2 else ROUNDED_DP * density
        Square -> SQUARE_DP * density
        Pill -> height / 2
        Outline, Raised, Glass, Borderless -> ROUNDED_DP * density
    }

    companion object {
        private const val ROUNDED_DP = 7f
        private const val SQUARE_DP = 2f

        /** How much of a key's own colour a [Glass] key shows over the background. */
        const val GLASS_ALPHA = 0.55f

        /** The key style the keyboard draws: the one the user picked, or else the theme's own. */
        fun shown(chosen: KeyStyle?, theme: Theme): KeyStyle = chosen ?: theme.keyStyle

        /** The key style stored under [name], or null for the theme's own. */
        fun named(name: String?): KeyStyle? = entries.firstOrNull { it.name == name }
    }
}

/** [colour] drawn at [alpha] over the opaque colour [over], as one opaque ARGB colour. */
fun blend(colour: Int, alpha: Float, over: Int): Int {
    fun channel(shift: Int): Int {
        val top = colour shr shift and 0xFF
        val under = over shr shift and 0xFF
        return Math.round(top * alpha + under * (1 - alpha))
    }
    return (0xFF shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
}
