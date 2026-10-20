package io.shodo.standbyclock.ui

import android.content.Context
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.swipeLeft
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.shodo.standbyclock.ClockPrefs
import io.shodo.standbyclock.ClockStyle
import io.shodo.standbyclock.NowPlaying
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.ZoneId
import java.time.ZonedDateTime

@RunWith(AndroidJUnit4::class)
class StandByScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var prefs: ClockPrefs

    // Mardi 20 octobre 2026, 14:15 : heure paire (horloge à gauche), en plein jour.
    private val clock = Clock.fixed(
        ZonedDateTime.of(2026, 10, 20, 14, 15, 0, 0, ZoneId.of("Europe/Paris")).toInstant(),
        ZoneId.of("Europe/Paris"),
    )
    private val music = NowPlaying(
        title = "One More Night", artist = "Maroon 5", art = null,
        isPlaying = true, pausedAt = 0L, liked = false,
    )

    @Before
    fun resetPrefs() {
        context.getSharedPreferences("standby_clock", Context.MODE_PRIVATE).edit().clear().commit()
        prefs = ClockPrefs(context)
    }

    private fun show(media: NowPlaying? = null) = rule.setContent {
        StandByScreen(
            prefs = prefs, forcedTheme = null, onNightModeChanged = {}, onOpenSettings = null,
            clock = clock, nowPlayingSource = { media },
        )
    }

    private fun screenIs(state: String) =
        rule.onNodeWithTag(TAG_SCREEN).assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, state))

    @Test
    fun calendarIsShownByDefault() {
        show()
        rule.onNodeWithTag(TAG_CALENDAR).assertIsDisplayed()
        rule.onNodeWithTag(clockTag(ClockStyle.DIGITAL)).assertIsDisplayed()
    }

    // Non-régression : la musique en cours ne doit plus faire disparaître le calendrier.
    @Test
    fun calendarAndMusicAreShownTogether() {
        show(media = music)
        rule.onNodeWithTag(TAG_CALENDAR).assertIsDisplayed()
        rule.onNodeWithTag(TAG_MEDIA_BAR).assertIsDisplayed()
    }

    @Test
    fun analogDialIsNotTallerThanCalendar() {
        prefs.style = ClockStyle.ANALOG
        show()
        assertDialFitsCalendar()
    }

    @Test
    fun analogDialIsNotTallerThanCalendarWithMusic() {
        prefs.style = ClockStyle.ANALOG
        show(media = music)
        assertDialFitsCalendar()
    }

    private fun assertDialFitsCalendar() {
        val dial = rule.onNodeWithTag(TAG_DIAL).getBoundsInRoot()
        val calendar = rule.onNodeWithTag(TAG_CALENDAR).getBoundsInRoot()
        val dialHeight = dial.bottom - dial.top
        val calendarHeight = calendar.bottom - calendar.top
        assertTrue("cadran $dialHeight > calendrier $calendarHeight", dialHeight <= calendarHeight)
    }

    @Test
    fun tapTogglesNightMode() {
        show()
        screenIs("jour")
        rule.onNodeWithTag(TAG_SCREEN).performTouchInput { click(center) }
        screenIs("nuit")
        rule.onNodeWithTag(TAG_SCREEN).performTouchInput { click(center) }
        screenIs("jour")
    }

    @Test
    fun longPressHidesCalendar() {
        show()
        rule.onNodeWithTag(TAG_CALENDAR).assertIsDisplayed()
        rule.onNodeWithTag(TAG_SCREEN).performTouchInput { longClick(center) }
        rule.onNodeWithTag(TAG_CALENDAR).assertDoesNotExist()
    }

    @Test
    fun swipeChangesStyle() {
        show()
        rule.onNodeWithTag(clockTag(ClockStyle.DIGITAL)).assertIsDisplayed()
        rule.onNodeWithTag(TAG_SCREEN).performTouchInput { swipeLeft() }
        rule.waitForIdle()
        rule.onNodeWithTag(clockTag(ClockStyle.ANALOG)).assertIsDisplayed()
        rule.onNodeWithTag(TAG_SCREEN).performTouchInput { swipeLeft() }
        rule.waitForIdle()
        rule.onNodeWithTag(clockTag(ClockStyle.MINIMAL)).assertIsDisplayed()
    }
}
