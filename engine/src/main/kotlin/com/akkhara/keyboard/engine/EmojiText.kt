package com.akkhara.keyboard.engine

/**
 * Finds the emoji at the end of some text, so that backspace deletes a whole
 * emoji: a flag, a keycap, an emoji with a skin tone, or several emoji joined
 * into one by zero-width joiners, such as a family.
 *
 * Pure, like [MyanmarReordering]: it is given the text before the cursor.
 */
internal object EmojiText {

    /** How much text before the cursor to pass in. Longer than any emoji Unicode lists. */
    const val WINDOW = 32

    private const val ZERO_WIDTH_JOINER = 0x200D
    private const val EMOJI_PRESENTATION = 0xFE0F
    private const val KEYCAP = 0x20E3
    private const val CANCEL_TAG = 0xE007F
    private val TAGS = 0xE0020..0xE007E
    private val SKIN_TONES = 0x1F3FB..0x1F3FF
    private val REGIONAL_INDICATORS = 0x1F1E6..0x1F1FF
    private val KEYCAP_BASES = "0123456789#*".map { it.code }

    /** Blocks that hold nothing but pictographs, so a character in them is an emoji even on its own. */
    private val PICTOGRAPHS = listOf(0x1F000..0x1FAFF, 0x2600..0x27BF, 0x2300..0x23FF, 0x2B00..0x2BFF)

    /** How many UTF-16 units the emoji at the end of [before] takes, or 0 when [before] does not end in one. */
    fun lengthAtEnd(before: String): Int {
        val last = codePointBefore(before, before.length) ?: return 0
        if (last in REGIONAL_INDICATORS) return flagLength(before)
        var start = elementStart(before, before.length) ?: return 0
        // Emoji joined by zero-width joiners are one emoji.
        while (start > 0 && before[start - 1].code == ZERO_WIDTH_JOINER) {
            start = elementStart(before, start - 1) ?: break
        }
        return before.length - start
    }

    /**
     * A flag is a pair of regional indicators. In a run of them, the last one
     * is half a flag when the run has an odd number.
     */
    private fun flagLength(before: String): Int {
        var count = 0
        var end = before.length
        while (true) {
            val cp = codePointBefore(before, end) ?: break
            if (cp !in REGIONAL_INDICATORS) break
            count++
            end -= Character.charCount(cp)
        }
        return if (count % 2 == 0) 4 else 2
    }

    /**
     * Where the single emoji ending at [end] starts: a pictograph and whatever
     * follows it to change how it is drawn. Null when the text there is not an emoji.
     */
    private fun elementStart(text: String, end: Int): Int? {
        var at = end
        fun peek() = codePointBefore(text, at)
        fun take(): Int = peek()!!.also { at -= Character.charCount(it) }

        val isKeycap = peek() == KEYCAP
        if (isKeycap) take()
        var isTagged = false
        if (peek() == CANCEL_TAG) {
            take()
            while (peek()?.let { it in TAGS } == true) take()
            isTagged = true
        }
        val isPresentation = peek() == EMOJI_PRESENTATION
        if (isPresentation) take()
        val hasSkinTone = peek()?.let { it in SKIN_TONES } == true
        if (hasSkinTone) take()
        val base = peek() ?: return null
        val isEmoji = when {
            isKeycap -> base in KEYCAP_BASES
            else -> isTagged || isPresentation || hasSkinTone || PICTOGRAPHS.any { base in it }
        }
        if (!isEmoji) return null
        take()
        return at
    }

    private fun codePointBefore(text: String, end: Int): Int? =
        if (end <= 0) null else Character.codePointBefore(text, end)
}
