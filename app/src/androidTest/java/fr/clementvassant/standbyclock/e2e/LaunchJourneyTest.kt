package fr.clementvassant.standbyclock.e2e

import android.content.Intent
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import androidx.test.uiautomator.uiAutomator
import fr.clementvassant.standbyclock.ClockPrefs
import fr.clementvassant.standbyclock.ClockStyle
import fr.clementvassant.standbyclock.SettingsActivity
import fr.clementvassant.standbyclock.ThemeMode
import fr.clementvassant.standbyclock.ui.STATE_DAY
import fr.clementvassant.standbyclock.ui.STATE_NIGHT
import fr.clementvassant.standbyclock.ui.TAG_DIAL
import fr.clementvassant.standbyclock.ui.TAG_SETTINGS
import fr.clementvassant.standbyclock.ui.clockTag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Lancer l'horloge comme le ferait l'utilisateur : depuis le lanceur, les réglages ou une routine. */
@RunWith(AndroidJUnit4::class)
class LaunchJourneyTest {
    @Before
    fun freshStart() {
        E2e.freshStart()
    }

    @Test
    fun launcherOpensTheClockAndASwipeChangesTheStyleForGood() = uiAutomator {
        E2e.openFromLauncher()
        byTag(clockTag(ClockStyle.DIGITAL))
        capture("lancement-digital")

        // Balayage vers la gauche : style suivant, enregistré et retrouvé au retour dans l'app.
        clockScreen().swipe(Direction.LEFT, SWIPE_FRACTION)
        byTag(TAG_DIAL)
        pressHome()
        E2e.openFromLauncher()
        byTag(TAG_DIAL)
        assertEquals(ClockStyle.ANALOG, ClockPrefs(E2e.context).style)
        capture("lancement-analogique-garde")
    }

    @Test
    fun styleChosenInSettingsIsTheOneLaunched() = uiAutomator {
        E2e.open(SettingsActivity::class.java)
        byTag(TAG_SETTINGS)
        byText("Minimal").click()
        // Le bouton est sous la ligne de flottaison sur un petit écran : on fait défiler jusqu'à lui.
        val launch = byTag(TAG_SETTINGS).scrollUntil(Direction.DOWN, Until.findObject(By.text("Lancer l'horloge")))
        assertNotNull("Bouton « Lancer l'horloge » introuvable", launch)
        launch.click()
        byTag(clockTag(ClockStyle.MINIMAL))
        capture("reglages-minimal")
    }

    // Les entrées « Horloge jour » et « Horloge nuit » que proposent les routines (Modes et routines, Tasker...).
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    fun nightAndDayEntriesForceTheirMode() = uiAutomator {
        E2e.open(aliasIntent("NightClock"))
        clockScreenIn(STATE_NIGHT)
        capture("entree-horloge-nuit")

        // Un toucher inverse jour et nuit jusqu'à la prochaine bascule.
        clockScreen().click()
        clockScreenIn(STATE_DAY)

        // Réglages en Nuit : seule l'entrée « Horloge jour » peut ramener le jour.
        ClockPrefs(E2e.context).themeMode = ThemeMode.NIGHT
        E2e.open(aliasIntent("DayClock"))
        clockScreenIn(STATE_DAY)
    }

    private fun aliasIntent(alias: String) = Intent(Intent.ACTION_MAIN)
        .setClassName(E2e.PACKAGE, "${E2e.PACKAGE}.$alias")

    private companion object {
        const val SWIPE_FRACTION = 0.8f
    }
}
