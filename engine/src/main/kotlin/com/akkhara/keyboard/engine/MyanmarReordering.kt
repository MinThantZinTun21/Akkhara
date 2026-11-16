package com.akkhara.keyboard.engine

/** A change to make at the cursor: delete [delete] UTF-16 units before it, then insert [insert]. */
internal data class Edit(val delete: Int, val insert: String)

/** Kinzi: nga, asat and virama, drawn as a small mark above the consonant that follows. */
internal const val KINZI = "\u1004\u103A\u1039"

/**
 * Puts the syllable being typed into canonical order.
 *
 * Every function here is pure: it is given the text before the cursor and what
 * was typed, and returns the edit to make. Nothing is remembered between key
 * presses, so the result is the same after the cursor moves or the app changes
 * the text.
 *
 * Typing is visual order only. An e-vowel is written as a zero-width space
 * followed by the vowel, which makes it stand alone on screen; the consonant
 * typed next takes the zero-width space's place.
 */
internal object MyanmarReordering {

    /** How much text before the cursor the engine should pass in. Longer than any real syllable. */
    const val WINDOW = 32

    private const val ZERO_WIDTH_SPACE = '​'
    private const val E_VOWEL = 'ေ'
    private const val ASAT = '်'
    private const val VIRAMA = '္'
    private const val DOT_BELOW = '့'

    /** The edit that types [typed] after [before]. */
    fun type(before: String, typed: String): Edit = when {
        typed == E_VOWEL.toString() -> Edit(0, "$ZERO_WIDTH_SPACE$E_VOWEL")
        typed == KINZI || (typed.length == 1 && isBase(typed[0])) -> typeBase(before, typed)
        typed.length == 1 && isMark(typed[0]) -> typeMark(before, typed[0])
        typed.any { isBase(it) || isMark(it) } -> typeSequence(before, typed)
        else -> typeOther(before, typed)
    }

    /**
     * A key that types several Myanmar characters at once, such as ော် or ဏ္ဍ.
     * Its marks belong to the consonant already typed: only an e-vowel typed
     * by its own key stands ahead of its consonant.
     */
    private fun typeSequence(before: String, typed: String): Edit {
        var text = before
        for (c in typed) {
            val edit = when {
                isBase(c) -> typeBase(text, c.toString())
                isMark(c) -> typeMark(text, c)
                else -> typeOther(text, c.toString())
            }
            text = text.dropLast(edit.delete) + edit.insert
        }
        return replace(before, text)
    }

    /**
     * The edit to make when typing stops with [before] at the cursor: it takes
     * away the zero-width space of an e-vowel whose consonant never came.
     */
    fun finish(before: String): Edit = typeOther(before, "")

    /** How many UTF-16 units one press of backspace deletes from the end of [before]. */
    fun backspace(before: String): Int {
        if (before.isEmpty()) return 0
        val last = before.length - 1
        val isSurrogatePair = last > 0 && Character.isSurrogatePair(before[last - 1], before[last])
        if (isSurrogatePair) return 2
        // The last mark typed ahead of its consonant goes together with the zero-width space before it.
        val leavesZeroWidthSpace = isMark(before[last]) && last > 0 && before[last - 1] == ZERO_WIDTH_SPACE
        return if (leavesZeroWidthSpace) 2 else 1
    }

    /** A consonant, independent vowel or kinzi. */
    private fun typeBase(before: String, typed: String): Edit {
        val text = before.substring(syllableStart(before))
        val syllable = parse(text)
        if (syllable != null && syllable.head.length > 1 && syllable.head.endsWith(VIRAMA)) {
            // A consonant or kinzi is waiting for the consonant that goes with its virama.
            return replace(text, syllable.head + typed + syllable.marks)
        }
        val waiting = marksWaitingForBase(before)
        if (waiting != null) {
            // Marks typed in visual order, ahead of their consonant.
            return Edit(waiting.length + 1, typed + waiting)
        }
        return Edit(0, typed)
    }

    private fun typeMark(before: String, mark: Char): Edit {
        val text = before.substring(syllableStart(before))
        val syllable = parse(text) ?: return Edit(0, mark.toString())
        return replace(text, syllable.with(mark))
    }

    /** Anything that is not part of a Myanmar syllable: it ends whatever was being typed. */
    private fun typeOther(before: String, typed: String): Edit {
        val waiting = marksWaitingForBase(before) ?: return Edit(0, typed)
        // The consonant never came; do not leave the zero-width space behind.
        return Edit(waiting.length + 1, waiting + typed)
    }

    /** The edit that turns [text], which ends at the cursor, into [reordered]. */
    private fun replace(text: String, reordered: String): Edit {
        val unchanged = text.commonPrefixWith(reordered).length
        return Edit(text.length - unchanged, reordered.substring(unchanged))
    }

    /**
     * The marks at the end of [text] that were typed ahead of their consonant:
     * a run of marks with a zero-width space in front. Null when there is none.
     */
    private fun marksWaitingForBase(text: String): String? {
        var start = text.length
        while (start > 0 && isMark(text[start - 1])) start--
        val hasMarks = start < text.length
        val afterZeroWidthSpace = start > 0 && text[start - 1] == ZERO_WIDTH_SPACE
        return if (hasMarks && afterZeroWidthSpace) text.substring(start) else null
    }

    /** Where the syllable that ends at the end of [text] begins. */
    private fun syllableStart(text: String): Int {
        var i = text.length
        while (i > 0) {
            val c = text[i - 1]
            when {
                isMark(c) -> i--
                isBase(c) -> {
                    i--
                    val isStackedOrUnderKinzi = i > 0 && text[i - 1] == VIRAMA
                    if (!isStackedOrUnderKinzi) return i
                }
                else -> return i
            }
        }
        return i
    }

    /**
     * A syllable split into its [head], whose order is fixed (kinzi, the
     * consonant, any asat that sits directly on it, stacked consonants, and a
     * virama still waiting for its consonant), and its [marks], which are
     * kept in canonical order.
     */
    private class Syllable(val head: String, val marks: String) {

        /** This syllable with [mark] added, as text. */
        fun with(mark: Char): String = when {
            mark == VIRAMA && head == "င" && marks == ASAT.toString() -> KINZI
            mark == VIRAMA -> head + VIRAMA + marks
            sitsAfterAsat(mark) && marks == ASAT.toString() && head.isNotEmpty() -> head + ASAT + mark
            else -> {
                val rank = rank(mark)
                val at = marks.indexOfFirst { rank(it) > rank }.let { if (it < 0) marks.length else it }
                head + marks.substring(0, at) + mark + marks.substring(at)
            }
        }
    }

    /** Null when [text] is not shaped like a syllable, in which case it is left alone. */
    private fun parse(text: String): Syllable? {
        var i = 0
        if (text.startsWith(KINZI)) {
            i = KINZI.length
            if (i < text.length && isBase(text[i])) i++ else return syllable(text, i)
        } else if (text.isNotEmpty() && isBase(text[0])) {
            i = 1
        }
        val hasBase = i > 0
        if (hasBase && i + 1 < text.length && text[i] == ASAT && sitsAfterAsat(text[i + 1])) {
            i++ // an asat directly on the consonant, as in ယောက်ျား
        }
        while (i < text.length && text[i] == VIRAMA) {
            i++
            if (i < text.length && isBase(text[i])) i++ else break
        }
        return syllable(text, i)
    }

    private fun syllable(text: String, headLength: Int): Syllable? {
        val marks = text.substring(headLength)
        val allOrderable = marks.all { isMark(it) && it != VIRAMA }
        return if (allOrderable) Syllable(text.substring(0, headLength), marks) else null
    }

    /** Consonants, independent vowels and great sa: anything a mark can attach to. */
    private fun isBase(c: Char): Boolean = c in 'က'..'ဪ' || c == 'ဿ'

    private fun isMark(c: Char): Boolean = c in 'ါ'..'ဲ' || c in 'ံ'..'ှ'

    /**
     * Whether [mark], typed straight after an asat, stays behind it. Medials
     * and vowels do, because the asat then belongs to the consonant
     * (ကျွန်ုပ်). Dot below does not: it is always stored before asat.
     */
    private fun sitsAfterAsat(mark: Char): Boolean = rank(mark) < rank(DOT_BELOW)

    /** Position in canonical order; lower comes first. */
    private fun rank(mark: Char): Int = when (mark) {
        'ျ' -> 1 // medial ya
        'ြ' -> 2 // medial ra
        'ွ' -> 3 // medial wa
        'ှ' -> 4 // medial ha
        E_VOWEL -> 5
        'ိ', 'ီ', 'ဲ' -> 6 // upper vowels
        'ု', 'ူ' -> 7 // lower vowels
        'ါ', 'ာ' -> 8 // aa vowels
        'ံ' -> 9 // anusvara
        DOT_BELOW -> 10
        ASAT -> 11
        'း' -> 12 // visarga
        else -> 13
    }
}
