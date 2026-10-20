package io.shodo.standbyclock.ui

import java.time.LocalDate
import kotlin.math.hypot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutLogicTest {

    @Test
    fun `month grid starts on monday`() {
        // Octobre 2026 : le 1er est un jeudi, 31 jours, 5 semaines.
        assertEquals(MonthGrid(offset = 3, days = 31, weeks = 5), monthGrid(LocalDate.of(2026, 10, 20)))
        // Juin 2026 : le 1er est un lundi.
        assertEquals(MonthGrid(offset = 0, days = 30, weeks = 5), monthGrid(LocalDate.of(2026, 6, 15)))
        // Mars 2026 : le 1er est un dimanche, 31 jours, 6 semaines.
        assertEquals(MonthGrid(offset = 6, days = 31, weeks = 6), monthGrid(LocalDate.of(2026, 3, 1)))
    }

    @Test
    fun `february in leap and common years`() {
        // Février 2027 : commence un lundi, 28 jours, tient en 4 semaines.
        assertEquals(MonthGrid(offset = 0, days = 28, weeks = 4), monthGrid(LocalDate.of(2027, 2, 10)))
        // Février 2028 (bissextile) : commence un mardi, 29 jours, 5 semaines.
        assertEquals(MonthGrid(offset = 1, days = 29, weeks = 5), monthGrid(LocalDate.of(2028, 2, 29)))
    }

    @Test
    fun `screen saver turns only a portrait screen`() {
        assertTrue(needsLandscapeRotation(width = 1080, height = 2520))
        assertFalse(needsLandscapeRotation(width = 2520, height = 1080))
        // Écran carré : rien à tourner.
        assertFalse(needsLandscapeRotation(width = 1000, height = 1000))
    }

    @Test
    fun `clock side alternates every hour`() {
        assertTrue(clockOnLeft(0))
        assertFalse(clockOnLeft(1))
        assertTrue(clockOnLeft(14))
        assertFalse(clockOnLeft(23))
    }

    @Test
    fun `anti burn-in orbit stays within 20 dp and always moves`() {
        val start = 29_000_000L
        var previous = orbitOffset(start - 1)
        for (m in start until start + 2 * 24 * 60) {
            val p = orbitOffset(m)
            assertTrue("minute $m: $p", hypot(p.x, p.y) <= 20.001f)
            assertNotEquals("minute $m", previous, p)
            previous = p
        }
    }

    @Test
    fun `ticker waits for next minute at night and next second by day`() {
        val minuteStart = 1_700_000_040_000L // multiple de 60 000
        val now = minuteStart + 12_345L
        assertEquals(47_655L, tickDelayMillis(everyMinute = true, nowMillis = now))
        assertEquals(655L, tickDelayMillis(everyMinute = false, nowMillis = now))
        assertEquals(60_000L, tickDelayMillis(everyMinute = true, nowMillis = minuteStart))
    }
}
