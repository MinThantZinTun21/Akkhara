package com.akkhara.keyboard.engine

/**
 * One emoji in the emoji panel. [skinTones] are its five skin-tone versions,
 * lightest first, offered when it is held; empty for an emoji that has none.
 * See GLOSSARY.md, "Skin tone".
 */
data class Emoji(val text: String, val skinTones: List<String> = emptyList())

/** One emoji category and its emoji, in Unicode's order. See GLOSSARY.md, "Emoji category". */
data class EmojiCategory(val kind: Kind, val emoji: List<Emoji>) {
    enum class Kind {
        /** The recent emoji; the other categories are Unicode's. */
        Recent,
        SmileysAndPeople,
        AnimalsAndNature,
        FoodAndDrink,
        TravelAndPlaces,
        Activities,
        Objects,
        Symbols,
        Flags,
    }
}

/**
 * Every emoji Unicode recommends for keyboards, read from Unicode's own
 * emoji-test.txt, which is built into the app unchanged. Adding the emoji of
 * a new Unicode version means replacing that file.
 */
internal object EmojiList {

    /** Read the first time the emoji panel needs it, not when the keyboard starts. */
    val categories: List<EmojiCategory> by lazy {
        val file = EmojiList::class.java.getResourceAsStream(FILE) ?: error("$FILE is missing")
        parse(file.bufferedReader().use { it.readText() })
    }

    /** Which of Unicode's groups go in which category. Unicode's Component group is left out. */
    private val CATEGORY_OF_GROUP = mapOf(
        "Smileys & Emotion" to EmojiCategory.Kind.SmileysAndPeople,
        "People & Body" to EmojiCategory.Kind.SmileysAndPeople,
        "Animals & Nature" to EmojiCategory.Kind.AnimalsAndNature,
        "Food & Drink" to EmojiCategory.Kind.FoodAndDrink,
        "Travel & Places" to EmojiCategory.Kind.TravelAndPlaces,
        "Activities" to EmojiCategory.Kind.Activities,
        "Objects" to EmojiCategory.Kind.Objects,
        "Symbols" to EmojiCategory.Kind.Symbols,
        "Flags" to EmojiCategory.Kind.Flags,
    )

    /** An emoji being read, gathering its skin tones from the lines that follow it. */
    private class Entry(val text: String) {
        val skinTones = mutableListOf<String>()
    }

    /**
     * Lines look like `1F44D 1F3FD ; fully-qualified # 👍🏽 E1.0 thumbs up: medium skin tone`,
     * under `# group: People & Body`. Only fully-qualified emoji are kept: the
     * others are the same emoji with a code point missing.
     *
     * A skin-tone version is not an emoji of its own: it joins the emoji it is
     * a version of. For emoji showing two people, only the five versions where
     * both have the same skin tone are kept.
     */
    private fun parse(text: String): List<EmojiCategory> {
        val emoji = linkedMapOf<EmojiCategory.Kind, MutableList<Entry>>()
        val byShape = hashMapOf<List<Int>, Entry>()
        var category: EmojiCategory.Kind? = null
        for (line in text.lineSequence()) {
            if (line.startsWith(GROUP)) {
                category = CATEGORY_OF_GROUP[line.removePrefix(GROUP).trim()]
                continue
            }
            if (category == null || line.startsWith("#")) continue
            val fields = line.substringBefore('#').split(';')
            if (fields.size != 2 || fields[1].trim() != "fully-qualified") continue
            val codePoints = fields[0].trim().split(' ').map { it.toInt(16) }
            val typed = String(codePoints.toIntArray(), 0, codePoints.size)
            val skinTones = codePoints.filter { it in SKIN_TONES }.distinct()
            when (skinTones.size) {
                0 -> Entry(typed).also {
                    emoji.getOrPut(category) { mutableListOf() } += it
                    byShape[shapeOf(codePoints)] = it
                }
                1 -> byShape[shapeOf(codePoints)]?.skinTones?.add(typed)
                else -> Unit // two people with different skin tones
            }
        }
        return emoji.map { (kind, entries) -> EmojiCategory(kind, entries.map { Emoji(it.text, it.skinTones) }) }
    }

    /**
     * What an emoji and its skin-tone versions have in common: its code points
     * without the skin tones, and without the emoji presentation selector that
     * a skin tone takes the place of.
     */
    private fun shapeOf(codePoints: List<Int>): List<Int> =
        codePoints.filter { it !in SKIN_TONES && it != EMOJI_PRESENTATION }

    private val SKIN_TONES = 0x1F3FB..0x1F3FF
    private const val EMOJI_PRESENTATION = 0xFE0F

    private const val FILE = "emoji-test.txt"
    private const val GROUP = "# group:"
}
