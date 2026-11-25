package com.mydreamlinks.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

/** Same known dates as the site's tests/unit/MoonPhaseTest.php. */
class MoonTest {
    private fun name(date: String) = Moon.NAMES[Moon.index(Moon.phase(date))]

    @Test fun knownPhases() {
        assertEquals("Full moon", name("2024-04-23"))
        assertEquals("New moon", name("2024-04-08"))
        assertEquals("First quarter", name("2024-04-15"))
        assertEquals("Last quarter", name("2024-05-01"))
        assertEquals("Full moon", name("2026-11-24"))
    }

    @Test fun indexWraps() {
        assertEquals(0, Moon.index(0.99))
        assertEquals(4, Moon.index(0.5))
    }
}
