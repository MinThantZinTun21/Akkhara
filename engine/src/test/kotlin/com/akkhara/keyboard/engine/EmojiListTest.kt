package com.akkhara.keyboard.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmojiListTest {
    private val engine = KeyboardEngine(FakeEditor(), FakeClock())
    private val categories = engine.emojiCategories

    private fun emojiIn(kind: EmojiCategory.Kind) = categories.single { it.kind == kind }.emoji.map { it.text }

    private fun skinTonesOf(emoji: String) = categories.flatMap { it.emoji }.first { it.text == emoji }.skinTones

    @Test
    fun `the emoji panel has Unicode's categories in Unicode's order`() {
        assertEquals(
            listOf(
                EmojiCategory.Kind.Recent,
                EmojiCategory.Kind.SmileysAndPeople,
                EmojiCategory.Kind.AnimalsAndNature,
                EmojiCategory.Kind.FoodAndDrink,
                EmojiCategory.Kind.TravelAndPlaces,
                EmojiCategory.Kind.Activities,
                EmojiCategory.Kind.Objects,
                EmojiCategory.Kind.Symbols,
                EmojiCategory.Kind.Flags,
            ),
            categories.map { it.kind },
        )
    }

    @Test
    fun `smileys and people come together, in Unicode's order`() {
        val smileys = emojiIn(EmojiCategory.Kind.SmileysAndPeople)

        assertEquals(listOf("😀", "😃", "😄"), smileys.take(3))
        assertTrue("👋" in smileys)
    }

    @Test
    fun `each category holds its own emoji`() {
        assertTrue("🐶" in emojiIn(EmojiCategory.Kind.AnimalsAndNature))
        assertTrue("🍜" in emojiIn(EmojiCategory.Kind.FoodAndDrink))
        assertTrue("🚗" in emojiIn(EmojiCategory.Kind.TravelAndPlaces))
        assertTrue("⚽" in emojiIn(EmojiCategory.Kind.Activities))
        assertTrue("💡" in emojiIn(EmojiCategory.Kind.Objects))
        assertTrue("#️⃣" in emojiIn(EmojiCategory.Kind.Symbols))
        assertTrue("🇲🇲" in emojiIn(EmojiCategory.Kind.Flags))
    }

    @Test
    fun `only fully-qualified emoji are offered, and no lone skin tones`() {
        val all = categories.flatMap { it.emoji }.map { it.text }

        assertTrue("☺️" in all)
        assertFalse("☺" in all)
        assertFalse("🏻" in all)
    }

    @Test
    fun `an emoji showing a person offers its five skin tones`() {
        assertEquals(listOf("👋🏻", "👋🏼", "👋🏽", "👋🏾", "👋🏿"), skinTonesOf("👋"))
        assertEquals(listOf("☝🏻", "☝🏼", "☝🏽", "☝🏾", "☝🏿"), skinTonesOf("☝️"))
        assertEquals(listOf("🏃🏻‍♂️", "🏃🏼‍♂️", "🏃🏽‍♂️", "🏃🏾‍♂️", "🏃🏿‍♂️"), skinTonesOf("🏃‍♂️"))
    }

    @Test
    fun `two people together are offered in the five skin tones they share`() {
        assertEquals(listOf("🧑🏻‍🤝‍🧑🏻", "🧑🏼‍🤝‍🧑🏼", "🧑🏽‍🤝‍🧑🏽", "🧑🏾‍🤝‍🧑🏾", "🧑🏿‍🤝‍🧑🏿"), skinTonesOf("🧑‍🤝‍🧑"))
    }

    @Test
    fun `an emoji that shows no person offers no skin tones`() {
        assertEquals(emptyList<String>(), skinTonesOf("😀"))
        assertEquals(emptyList<String>(), skinTonesOf("🇲🇲"))
    }

    @Test
    fun `skin-tone versions are not in the grid on their own`() {
        val skinTones = 0x1F3FB..0x1F3FF
        val toned = categories.flatMap { it.emoji }.map { it.text }
            .filter { text -> text.codePoints().anyMatch { it in skinTones } }

        assertEquals(emptyList<String>(), toned)
    }
}
