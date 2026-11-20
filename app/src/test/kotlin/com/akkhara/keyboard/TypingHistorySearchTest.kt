package com.akkhara.keyboard

import org.junit.Assert.assertEquals
import org.junit.Test

class TypingHistorySearchTest {
    @Test
    fun `a search finds entries that contain the words anywhere`() {
        assertEquals("%see you%", TypingHistoryDatabase.containsPattern("see you"))
        assertEquals("%မင်္ဂလာ%", TypingHistoryDatabase.containsPattern("မင်္ဂလာ"))
    }

    @Test
    fun `percent, underscore and backslash in a search match only themselves`() {
        assertEquals("""%50\% off\_now\\%""", TypingHistoryDatabase.containsPattern("""50% off_now\"""))
    }
}
