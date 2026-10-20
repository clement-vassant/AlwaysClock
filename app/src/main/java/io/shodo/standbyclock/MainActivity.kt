package io.shodo.standbyclock

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import io.shodo.standbyclock.ui.StandByScreen
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    /** Mode imposé par le lancement (raccourci ou entrée « Horloge jour/nuit »), sinon les réglages. */
    private val forcedTheme = mutableStateOf<ThemeMode?>(null)

    private lateinit var prefs: ClockPrefs

    /** Instant du lancement, pour l'extinction automatique (le délai se relit à chaque reprise). */
    private var launchedAt = 0L
    private var autoOff: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        launchedAt = SystemClock.elapsedRealtime()
        forcedTheme.value = themeFromLaunch(intent)
        prefs = ClockPrefs(this)
        setContent {
            StandByScreen(
                prefs = prefs,
                forcedTheme = forcedTheme.value,
                onNightModeChanged = ::applyBrightness,
                onOpenSettings = { startActivity(Intent(this, SettingsActivity::class.java)) },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        forcedTheme.value = themeFromLaunch(intent)
        launchedAt = SystemClock.elapsedRealtime()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        window.preferLowestRefreshRate()
    }

    // Fold plié ou déplié : on change d'écran, donc de modes d'affichage.
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        window.preferLowestRefreshRate()
    }

    override fun onResume() {
        super.onResume()
        val minutes = prefs.autoOffMinutes
        if (minutes <= 0) return
        autoOff = lifecycleScope.launch {
            delay(launchedAt + minutes * 60_000L - SystemClock.elapsedRealtime())
            // Pas d'image fixe pendant des heures : on laisse l'écran s'éteindre et on ferme l'horloge.
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            finish()
        }
    }

    override fun onPause() {
        autoOff?.cancel()
        super.onPause()
    }

    /** Les entrées « Horloge jour » et « Horloge nuit » sont des alias de cette activité. */
    private fun themeFromLaunch(intent: Intent): ThemeMode? =
        themeFromExtra(intent.getStringExtra(EXTRA_THEME)) ?: when (intent.component?.className) {
            "$packageName.DayClock" -> ThemeMode.DAY
            "$packageName.NightClock" -> ThemeMode.NIGHT
            else -> null
        }

    /** Luminosité de la fenêtre : minimale la nuit, plafonnée à 35 % le jour (usure OLED). */
    private fun applyBrightness(night: Boolean) {
        window.attributes = window.attributes.apply {
            screenBrightness = if (night) 0.01f else 0.35f
        }
    }
}
