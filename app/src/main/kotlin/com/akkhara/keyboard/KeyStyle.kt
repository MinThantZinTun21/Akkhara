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
    ;

    /**
     * The corner radius, in pixels, of a key [height] pixels tall. [isEndKey]
     * is a key at either end of the bottom row.
     */
    fun cornerRadius(height: Float, density: Float, isEndKey: Boolean = false): Float = when (this) {
        Rounded -> if (isEndKey) height / 2 else ROUNDED_DP * density
        Square -> SQUARE_DP * density
        Pill -> height / 2
    }

    companion object {
        private const val ROUNDED_DP = 7f
        private const val SQUARE_DP = 2f

        /** The key style the keyboard draws: the one the user picked, or else the theme's own. */
        fun shown(chosen: KeyStyle?, theme: Theme): KeyStyle = chosen ?: theme.keyStyle

        /** The key style stored under [name], or null for the theme's own. */
        fun named(name: String?): KeyStyle? = entries.firstOrNull { it.name == name }
    }
}
