package com.akkhara.keyboard.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class TypingHistoryTest {
    private val saved = mutableListOf<HistoryEntry>()
    private val clock = FakeClock(now = 1_000L)
    private val recorder = TypingHistoryRecorder(store = { saved += it }, clock = clock, ownApp = AKKHARA).apply { isOn = true }

    @Test
    fun `leaving a field saves its whole text, with the app and the time`() {
        recorder.startField(Field(), NOTES)
        recorder.textChanged("Buy rice")
        recorder.textChanged("Buy rice and eggs")
        clock.now = 5_000L
        recorder.leaveField()

        assertEquals(listOf(HistoryEntry("Buy rice and eggs", NOTES, savedAt = 5_000L)), saved)
    }

    @Test
    fun `nothing is saved while typing history is off`() {
        recorder.isOn = false
        recorder.startField(Field(), NOTES)
        recorder.textChanged("Buy rice")
        recorder.leaveField()

        assertEquals(emptyList<HistoryEntry>(), saved)
    }

    @Test
    fun `turning typing history off in a field forgets the text it held`() {
        recorder.startField(Field(), NOTES)
        recorder.textChanged("Buy rice")
        recorder.isOn = false
        recorder.textChanged("Buy rice and eggs")
        recorder.isOn = true
        recorder.leaveField()

        assertEquals(emptyList<HistoryEntry>(), saved)
    }

    @Test
    fun `email and web address fields are saved like text`() {
        typeAndLeave(Field(kind = FieldKind.Address), "mya@example.com")

        assertEquals(listOf("mya@example.com"), saved.map { it.text })
    }

    @Test
    fun `password, number, phone, date and time fields are never saved`() {
        typeAndLeave(Field(isPassword = true), "hunter2")
        typeAndLeave(Field(kind = FieldKind.Number()), "4821")
        typeAndLeave(Field(kind = FieldKind.Phone), "0912345678")
        typeAndLeave(Field(kind = FieldKind.Date), "20/11/2026")
        typeAndLeave(Field(kind = FieldKind.Time), "14:30")
        typeAndLeave(Field(kind = FieldKind.DateAndTime), "20/11/2026 14:30")

        assertEquals(emptyList<HistoryEntry>(), saved)
    }

    @Test
    fun `a field the app marks as private is never saved`() {
        typeAndLeave(Field(mayLearn = false), "secret search")

        assertEquals(emptyList<HistoryEntry>(), saved)
    }

    @Test
    fun `nothing typed in the Akkhara app itself is saved`() {
        typeAndLeave(Field(), "rice", app = SourceApp(AKKHARA, "Akkhara"))

        assertEquals(emptyList<HistoryEntry>(), saved)
    }

    @Test
    fun `a long field keeps its last 5,000 characters`() {
        typeAndLeave(Field(), "a".repeat(10) + "b".repeat(5_000))

        assertEquals("b".repeat(5_000), saved.single().text)
    }

    @Test
    fun `the cap never splits an emoji in half`() {
        typeAndLeave(Field(), "a" + "😀" + "b".repeat(4_999))

        assertEquals("b".repeat(4_999), saved.single().text)
    }

    @Test
    fun `Myanmar text is kept exactly as typed`() {
        typeAndLeave(Field(), "မင်္ဂလာပါ")

        assertEquals("မင်္ဂလာပါ", saved.single().text)
    }

    @Test
    fun `starting another field finishes the one before`() {
        recorder.startField(Field(), NOTES)
        recorder.textChanged("Buy rice")
        recorder.startField(Field(), NOTES)

        assertEquals(listOf("Buy rice"), saved.map { it.text })
    }

    @Test
    fun `pressing the action key saves the field's text`() {
        recorder.startField(Field(enterAction = EnterAction.Search), BROWSER)
        recorder.textChanged("rice price")
        recorder.actionKeyPressed()

        assertEquals(listOf("rice price"), saved.map { it.text })
    }

    @Test
    fun `the app emptying the field saves the text it held`() {
        recorder.startField(Field(), CHAT)
        recorder.textChanged("See you at 6")
        recorder.textChanged("")

        assertEquals(listOf("See you at 6"), saved.map { it.text })
    }

    @Test
    fun `text the user deletes with the keyboard is never saved`() {
        recorder.startField(Field(), CHAT)
        recorder.textChanged("Never mind")
        recorder.textChanged("", byKeyboard = true)
        recorder.leaveField()

        assertEquals(emptyList<HistoryEntry>(), saved)
    }

    @Test
    fun `a message sent and then left is saved once`() {
        recorder.startField(Field(enterAction = EnterAction.Send), CHAT)
        recorder.textChanged("See you at 6")
        recorder.actionKeyPressed()
        recorder.textChanged("")
        recorder.leaveField()

        assertEquals(listOf("See you at 6"), saved.map { it.text })
    }

    @Test
    fun `empty, space-only and one-character text is not saved`() {
        typeAndLeave(Field(), "   ")
        typeAndLeave(Field(), "k")
        typeAndLeave(Field(), " 👍 ")

        assertEquals(emptyList<HistoryEntry>(), saved)
    }

    @Test
    fun `text identical to the app's last entry is not saved again`() {
        typeAndLeave(Field(), "Buy rice")
        typeAndLeave(Field(), "Buy rice")
        typeAndLeave(Field(), "Buy rice", app = CHAT)

        assertEquals(listOf(NOTES, CHAT), saved.map { it.app })
    }

    private fun typeAndLeave(field: Field, text: String, app: SourceApp = NOTES) {
        recorder.startField(field, app)
        recorder.textChanged(text)
        recorder.leaveField()
    }

    private companion object {
        val NOTES = SourceApp("com.example.notes", "Notes")
        val CHAT = SourceApp("com.example.chat", "Chat")
        val BROWSER = SourceApp("com.example.browser", "Browser")
        val AKKHARA = "com.akkhara.keyboard"
    }
}
