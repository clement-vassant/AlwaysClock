package io.shodo.standbyclock

import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect

/** Réglages : style, calendrier, jour/nuit, plage horaire de nuit et extinction automatique. Ouvert aussi par le ⚙ de l'écran de veille. */
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = ClockPrefs(this)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFFFF9F0A), background = Color.Black, surface = Color.Black)) {
                Surface(Modifier.fillMaxSize()) {
                    SettingsScreen(
                        prefs = prefs,
                        pickTime = ::pickTime,
                        onLaunch = { startActivity(Intent(this, MainActivity::class.java)) },
                        onOpenScreenSaver = ::openScreenSaverSettings,
                        hasMediaAccess = { MediaListenerService.isGranted(this) },
                        onOpenNotificationAccess = { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
                    )
                }
            }
        }
    }

    private fun pickTime(initial: Int, onPicked: (Int) -> Unit) {
        TimePickerDialog(this, { _, h, m -> onPicked(h * 60 + m) }, initial / 60, initial % 60, true).show()
    }

    private fun openScreenSaverSettings() {
        try {
            startActivity(Intent(Settings.ACTION_DREAM_SETTINGS))
        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS))
        }
    }
}

@Composable
private fun SettingsScreen(
    prefs: ClockPrefs,
    pickTime: (Int, (Int) -> Unit) -> Unit,
    onLaunch: () -> Unit,
    onOpenScreenSaver: () -> Unit,
    hasMediaAccess: () -> Boolean,
    onOpenNotificationAccess: () -> Unit
) {
    var style by remember { mutableStateOf(prefs.style) }
    var calendar by remember { mutableStateOf(prefs.showCalendar) }
    var theme by remember { mutableStateOf(prefs.themeMode) }
    var nightStart by remember { mutableStateOf(prefs.nightStart) }
    var nightEnd by remember { mutableStateOf(prefs.nightEnd) }
    var autoOff by remember { mutableStateOf(prefs.autoOffMinutes) }
    var media by remember { mutableStateOf(prefs.showMedia) }
    // Revérifié au retour des réglages Android.
    var mediaAccess by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        mediaAccess = hasMediaAccess()
        onPauseOrDispose { }
    }

    Column(
        Modifier
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("StandBy Clock", style = MaterialTheme.typography.headlineMedium)

        Section("Style d'horloge") {
            Choices(
                options = listOf(ClockStyle.DIGITAL to "Digital", ClockStyle.ANALOG to "Analogique", ClockStyle.MINIMAL to "Minimal"),
                selected = style
            ) {
                style = it
                prefs.style = it
            }
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Calendrier à droite de l'horloge", Modifier.weight(1f))
            Switch(checked = calendar, onCheckedChange = {
                calendar = it
                prefs.showCalendar = it
            })
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Afficher la musique en cours", Modifier.weight(1f))
            Switch(checked = media, onCheckedChange = {
                media = it
                prefs.showMedia = it
            })
        }
        if (media && !mediaAccess) {
            Text(
                "Pour lire et piloter la musique (Spotify...), StandBy Clock a besoin de l'accès aux notifications.",
                style = MaterialTheme.typography.bodyMedium
            )
            OutlinedButton(onClick = onOpenNotificationAccess) { Text("Autoriser l'accès aux notifications") }
        }

        Section("Jour ou nuit") {
            Choices(
                options = listOf(ThemeMode.AUTO to "Auto", ThemeMode.DAY to "Jour", ThemeMode.NIGHT to "Nuit"),
                selected = theme
            ) {
                theme = it
                prefs.themeMode = it
            }
            if (theme == ThemeMode.AUTO) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Nuit de")
                    OutlinedButton(onClick = {
                        pickTime(nightStart) {
                            nightStart = it
                            prefs.nightStart = it
                        }
                    }) { Text(formatMinutes(nightStart)) }
                    Text("à")
                    OutlinedButton(onClick = {
                        pickTime(nightEnd) {
                            nightEnd = it
                            prefs.nightEnd = it
                        }
                    }) { Text(formatMinutes(nightEnd)) }
                }
            }
        }

        Section("Éteindre après") {
            Choices(
                options = listOf(60 to "1 h", 120 to "2 h", 240 to "4 h", 480 to "8 h", 0 to "Jamais"),
                selected = autoOff
            ) {
                autoOff = it
                prefs.autoOffMinutes = it
            }
        }

        Button(onClick = onLaunch, modifier = Modifier.fillMaxWidth()) { Text("Lancer l'horloge") }

        Section("Lancement automatique") {
            Text(stringResource(R.string.help_screen_saver), style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = onOpenScreenSaver) { Text("Ouvrir les réglages d'écran de veille") }
            Text(stringResource(R.string.help_routines), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        content()
    }
}

@Composable
private fun <T> Choices(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for ((value, label) in options) {
            FilterChip(selected = value == selected, onClick = { onSelect(value) }, label = { Text(label) })
        }
    }
}
