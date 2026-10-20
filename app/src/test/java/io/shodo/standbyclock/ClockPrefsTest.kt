package io.shodo.standbyclock

import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClockPrefsTest {
    private val pkg = "io.shodo.standbyclock"

    private fun autoNight(time: String, start: String, end: String) =
        isNightAt(ThemeMode.AUTO, LocalTime.parse(time), minutes(start), minutes(end))

    private fun minutes(hhmm: String) = LocalTime.parse(hhmm).let { it.hour * 60 + it.minute }

    @Test
    fun `auto night across midnight`() {
        assertTrue(autoNight("22:00", "22:00", "07:00"))
        assertTrue(autoNight("23:59", "22:00", "07:00"))
        assertTrue(autoNight("00:00", "22:00", "07:00"))
        assertTrue(autoNight("06:59", "22:00", "07:00"))
        assertFalse(autoNight("07:00", "22:00", "07:00"))
        assertFalse(autoNight("14:15", "22:00", "07:00"))
        assertFalse(autoNight("21:59", "22:00", "07:00"))
    }

    @Test
    fun `auto night within the same day`() {
        assertFalse(autoNight("12:59", "13:00", "15:00"))
        assertTrue(autoNight("13:00", "13:00", "15:00"))
        assertTrue(autoNight("14:59", "13:00", "15:00"))
        assertFalse(autoNight("15:00", "13:00", "15:00"))
        assertFalse(autoNight("02:00", "13:00", "15:00"))
    }

    @Test
    fun `day and night modes ignore the schedule`() {
        assertFalse(isNightAt(ThemeMode.DAY, LocalTime.of(23, 0), 22 * 60, 7 * 60))
        assertTrue(isNightAt(ThemeMode.NIGHT, LocalTime.of(14, 0), 22 * 60, 7 * 60))
    }

    @Test
    fun `theme from launch extra`() {
        assertEquals(ThemeMode.DAY, themeFromExtra("day"))
        assertEquals(ThemeMode.NIGHT, themeFromExtra("night"))
        assertNull(themeFromExtra(null))
        assertNull(themeFromExtra("auto"))
    }

    @Test
    fun `theme from launcher alias`() {
        assertEquals(ThemeMode.DAY, themeFromAlias("$pkg.DayClock", pkg))
        assertEquals(ThemeMode.NIGHT, themeFromAlias("$pkg.NightClock", pkg))
        assertNull(themeFromAlias("$pkg.MainActivity", pkg))
        assertNull(themeFromAlias(null, pkg))
    }
}
