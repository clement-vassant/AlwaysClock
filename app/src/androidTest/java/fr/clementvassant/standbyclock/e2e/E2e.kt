package fr.clementvassant.standbyclock.e2e

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiAutomatorTestScope
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import fr.clementvassant.standbyclock.ClockPrefs
import fr.clementvassant.standbyclock.ClockStyle
import fr.clementvassant.standbyclock.PREFS_FILE
import fr.clementvassant.standbyclock.ThemeMode
import fr.clementvassant.standbyclock.ui.TAG_CALENDAR
import fr.clementvassant.standbyclock.ui.TAG_SCREEN
import fr.clementvassant.standbyclock.ui.clockTag
import org.junit.Assert.assertFalse

/** Outils partagés par les parcours de bout en bout (UI Automator). */
internal object E2e {
    const val PACKAGE = "fr.clementvassant.standbyclock"

    /**
     * Captures de chaque parcours, lisibles par adb : la CI les publie en artefact et l'agent qa les récupère.
     * Hors du dossier de l'app, parce que Gradle désinstalle l'app (et ses fichiers) à la fin des tests.
     */
    const val CAPTURES_DIR = "/sdcard/Download/standbyclock-e2e"

    /** Délai d'apparition d'un écran : l'émulateur de la CI, sans accélération graphique, est lent. */
    const val SCREEN_TIMEOUT_MS = 15_000L

    val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext

    /**
     * Ouvre un écran de l'app avec le contexte de l'app. Les `startApp` / `startActivity` d'UI Automator 2.4 passent
     * par un serveur shell sur socket local, refusé (EPERM) : les tests tournent dans le processus de l'app, qui n'a
     * pas la permission INTERNET et n'a pas à l'avoir.
     */
    fun open(intent: Intent) {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun open(activity: Class<out Activity>) = open(Intent(context, activity))

    /** Comme l'icône du lanceur : reprend la tâche de l'app si elle existe déjà. */
    fun openFromLauncher() = open(checkNotNull(context.packageManager.getLaunchIntentForPackage(PACKAGE)))

    /**
     * Point de départ commun : écran d'accueil (rien d'un parcours précédent à l'écran) et réglages d'un premier
     * lancement, en mode Jour forcé : l'horloge de la CI est en UTC et la plage de nuit
     * par défaut (22:00-07:00) masquerait les événements du jour selon l'heure du run.
     */
    fun freshStart(): ClockPrefs {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressHome()
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE).edit().clear().commit()
        return ClockPrefs(context).apply {
            themeMode = ThemeMode.DAY
            style = ClockStyle.DIGITAL
            showMedia = false
        }
    }
}

/** L'écran de l'horloge, dans l'app ou dans l'écran de veille. */
internal fun UiAutomatorTestScope.clockScreen(): UiObject2 = onElement(E2e.SCREEN_TIMEOUT_MS) { viewIdResourceName == TAG_SCREEN }

/** Un nœud repéré par son testTag (exposé comme identifiant, voir `testTagsAsResourceId`). */
internal fun UiAutomatorTestScope.byTag(tag: String): UiObject2 = onElement(E2e.SCREEN_TIMEOUT_MS) { viewIdResourceName == tag }

internal fun UiAutomatorTestScope.byText(text: String): UiObject2 = onElement(E2e.SCREEN_TIMEOUT_MS) { this.text?.toString() == text }

/** L'écran de l'horloge une fois passé en jour ou en nuit (`STATE_DAY`, `STATE_NIGHT` : description d'état, lue par Android 11+). */
internal fun UiAutomatorTestScope.clockScreenIn(dayOrNight: String): UiObject2 = onElement(E2e.SCREEN_TIMEOUT_MS) {
    viewIdResourceName == TAG_SCREEN && stateDescription?.toString() == dayOrNight
}

/** Les cadres visibles de l'horloge numérique et du calendrier, tous deux à l'écran et sans chevauchement. */
internal fun UiAutomatorTestScope.clockAndCalendar(): Pair<Rect, Rect> {
    val clock = byTag(clockTag(ClockStyle.DIGITAL)).visibleBounds
    val calendar = byTag(TAG_CALENDAR).visibleBounds
    assertFalse("Horloge hors de l'écran", clock.isEmpty)
    assertFalse("Calendrier hors de l'écran", calendar.isEmpty)
    assertFalse("Horloge et calendrier se chevauchent : $clock / $calendar", Rect.intersects(clock, calendar))
    return clock to calendar
}

/** Relit l'écran tant qu'un nœud disparaît sous nos pieds pendant une recomposition, quelques fois au plus. */
internal fun <T> retryWhileStale(attempts: Int = STALE_ATTEMPTS, block: () -> T): T {
    repeat(attempts - 1) {
        try {
            return block()
        } catch (_: StaleObjectException) {
            Thread.sleep(STALE_PAUSE_MS)
        }
    }
    return block()
}

private const val STALE_ATTEMPTS = 3
private const val STALE_PAUSE_MS = 500L

/** Capture de tout l'écran, nommée d'après le parcours (pas d'attente de stabilité : les secondes défilent). */
internal fun UiAutomatorTestScope.capture(name: String) {
    device.executeShellCommand("mkdir -p ${E2e.CAPTURES_DIR}")
    device.executeShellCommand("screencap -p ${E2e.CAPTURES_DIR}/$name.png")
}
