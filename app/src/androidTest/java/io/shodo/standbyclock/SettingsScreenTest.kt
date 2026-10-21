package io.shodo.standbyclock

import android.content.Context
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.shodo.standbyclock.ui.TAG_CALENDAR_SWITCH
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var prefs: ClockPrefs

    @Before
    fun showSettings() {
        context.getSharedPreferences("standby_clock", Context.MODE_PRIVATE).edit().clear().commit()
        prefs = ClockPrefs(context)
        rule.setContent {
            SettingsScreen(
                prefs = prefs,
                pickTime = { _, _ -> },
                onLaunch = {},
                onOpenScreenSaver = {},
                hasMediaAccess = { true },
                onOpenNotificationAccess = {}
            )
        }
    }

    @Test
    fun calendarSwitchSaysWhatItDoes() {
        // Le libellé dit « afficher », pas « à droite » : le calendrier change de côté chaque heure.
        rule.onNodeWithText("Afficher le calendrier").assertExists()
        rule.onNodeWithText("à droite", substring = true).assertDoesNotExist()
        rule.onNodeWithTag(TAG_CALENDAR_SWITCH).assertIsOn()
    }

    @Test
    fun orientationCanBeForcedToLandscape() {
        assertEquals(OrientationMode.AUTO, prefs.orientation)
        rule.onNodeWithText("Toujours paysage").performScrollTo().performClick()
        assertEquals(OrientationMode.LANDSCAPE, prefs.orientation)
    }

    @Test
    fun calendarSwitchHidesCalendar() {
        rule.onNodeWithTag(TAG_CALENDAR_SWITCH).performClick()
        assertFalse(prefs.showCalendar)
    }
}
