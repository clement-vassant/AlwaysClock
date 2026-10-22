package fr.clementvassant.standbyclock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NowPlayingTest {

    @Test
    fun `spotify like labels`() {
        assertEquals(false, likeLabelState("Ajouter à la collection"))
        assertEquals(true, likeLabelState("Supprimer de la collection"))
        assertEquals(false, likeLabelState("Like"))
        assertEquals(false, likeLabelState("Add to library"))
        assertEquals(false, likeLabelState("Heart"))
        assertEquals(true, likeLabelState("Remove from library"))
    }

    @Test
    fun `other custom actions are not a like`() {
        assertNull(likeLabelState("Lancer la radio"))
        assertNull(likeLabelState("Shuffle"))
        assertNull(likeLabelState("Repeat"))
    }

    @Test
    fun `media card shows while playing or recently paused`() {
        val now = 1_000_000_000L
        assertTrue(isRecentMedia(isPlaying = true, pausedAt = 0L, nowMillis = now))
        assertTrue(isRecentMedia(isPlaying = false, pausedAt = now - 9 * 60_000L, nowMillis = now))
        assertFalse(isRecentMedia(isPlaying = false, pausedAt = now - 10 * 60_000L, nowMillis = now))
        assertFalse(isRecentMedia(isPlaying = false, pausedAt = now - 60 * 60_000L, nowMillis = now))
    }
}
