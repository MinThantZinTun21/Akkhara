package com.akkhara.keyboard.engine

/** One emoji category and its emoji, in Unicode's order. See GLOSSARY.md, "Emoji category". */
data class EmojiCategory(val kind: Kind, val emoji: List<String>) {
    enum class Kind {
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

    /**
     * Lines look like `1F44D 1F3FD ; fully-qualified # 👍🏽 E1.0 thumbs up: medium skin tone`,
     * under `# group: People & Body`. Only fully-qualified emoji are kept: the
     * others are the same emoji with a code point missing.
     */
    private fun parse(text: String): List<EmojiCategory> {
        val emoji = linkedMapOf<EmojiCategory.Kind, MutableList<String>>()
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
            emoji.getOrPut(category) { mutableListOf() } += String(codePoints.toIntArray(), 0, codePoints.size)
        }
        return emoji.map { (kind, list) -> EmojiCategory(kind, list) }
    }

    private const val FILE = "emoji-test.txt"
    private const val GROUP = "# group:"
}
