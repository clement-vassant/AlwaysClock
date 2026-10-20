package io.shodo.standbyclock

import android.content.Context
import androidx.core.content.edit
import java.time.LocalTime

/** Les styles d'horloge disponibles, dans l'ordre du balayage horizontal. */
enum class ClockStyle { DIGITAL, ANALOG, MINIMAL }

/** Jour ou nuit : AUTO suit la plage horaire de nuit réglée dans l'app. */
enum class ThemeMode { AUTO, DAY, NIGHT }

/** Orientation de l'écran de veille : AUTO suit l'écran, LANDSCAPE tourne l'horloge si l'écran est en portrait. */
enum class OrientationMode { AUTO, LANDSCAPE }

/** Préférences partagées entre l'activité, l'écran de veille et les réglages. */
class ClockPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("standby_clock", Context.MODE_PRIVATE)

    var style: ClockStyle
        get() = ClockStyle.entries.getOrElse(prefs.getInt("style", 0)) { ClockStyle.DIGITAL }
        set(value) = prefs.edit { putInt("style", value.ordinal) }

    var showCalendar: Boolean
        get() = prefs.getBoolean("calendar", true)
        set(value) = prefs.edit { putBoolean("calendar", value) }

    var themeMode: ThemeMode
        get() = ThemeMode.entries.getOrElse(prefs.getInt("theme", 0)) { ThemeMode.AUTO }
        set(value) = prefs.edit { putInt("theme", value.ordinal) }

    /** Début de la nuit, en minutes depuis minuit (22:00 par défaut). */
    var nightStart: Int
        get() = prefs.getInt("night_start", 22 * 60)
        set(value) = prefs.edit { putInt("night_start", value) }

    /** Fin de la nuit, en minutes depuis minuit (07:00 par défaut). */
    var nightEnd: Int
        get() = prefs.getInt("night_end", 7 * 60)
        set(value) = prefs.edit { putInt("night_end", value) }

    /** Carte « musique en cours » à la place du calendrier (si l'accès aux notifications est accordé). */
    var showMedia: Boolean
        get() = prefs.getBoolean("media", true)
        set(value) = prefs.edit { putBoolean("media", value) }

    var orientation: OrientationMode
        get() = OrientationMode.entries.getOrElse(prefs.getInt("orientation", 0)) { OrientationMode.AUTO }
        set(value) = prefs.edit { putInt("orientation", value.ordinal) }

    /** Délai avant extinction automatique, en minutes ; 0 = jamais (2 h par défaut). */
    var autoOffMinutes: Int
        get() = prefs.getInt("auto_off", 120)
        set(value) = prefs.edit { putInt("auto_off", value) }

    fun isNight(mode: ThemeMode, time: LocalTime): Boolean = isNightAt(mode, time, nightStart, nightEnd)
}

/** Nuit selon le mode ; en AUTO, la plage [start, end[ (minutes depuis minuit) peut passer minuit. */
fun isNightAt(mode: ThemeMode, time: LocalTime, start: Int, end: Int): Boolean = when (mode) {
    ThemeMode.DAY -> false

    ThemeMode.NIGHT -> true

    ThemeMode.AUTO -> {
        val t = time.hour * MINUTES_PER_HOUR + time.minute
        if (start <= end) t in start until end else t >= start || t < end
    }
}

/** Les entrées « Horloge jour » et « Horloge nuit » sont des alias de l'activité principale. */
fun themeFromAlias(className: String?, packageName: String): ThemeMode? = when (className) {
    "$packageName.DayClock" -> ThemeMode.DAY
    "$packageName.NightClock" -> ThemeMode.NIGHT
    else -> null
}

/** Nom d'extra des raccourcis : "day" ou "night" force le mode pour ce lancement. */
const val EXTRA_THEME = "theme"

fun themeFromExtra(value: String?): ThemeMode? = when (value) {
    "day" -> ThemeMode.DAY
    "night" -> ThemeMode.NIGHT
    else -> null
}

fun formatMinutes(minutes: Int) = "%02d:%02d".format(minutes / MINUTES_PER_HOUR, minutes % MINUTES_PER_HOUR)
