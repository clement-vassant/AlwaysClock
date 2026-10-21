package io.shodo.standbyclock

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.database.Cursor
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.provider.CalendarContract.Attendees
import android.provider.CalendarContract.Calendars
import android.provider.CalendarContract.Instances
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.withContext

/** Lecture de l'agenda d'Android (CalendarContract) : il réunit les comptes Google, Samsung, Exchange... */
object DeviceCalendar {
    fun hasAccess(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    /** Les agendas affichés dans l'app Agenda de l'appareil ; vide sans autorisation. */
    fun calendars(context: Context): List<CalendarInfo> {
        // Vérification explicite ici : Android Lint veut la voir dans la fonction qui lit l'agenda.
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return emptyList()
        }
        val projection = arrayOf(Calendars._ID, Calendars.CALENDAR_DISPLAY_NAME, Calendars.ACCOUNT_NAME)
        return try {
            context.contentResolver.query(Calendars.CONTENT_URI, projection, "${Calendars.VISIBLE} = 1", null, null)?.use { c ->
                buildList {
                    while (c.moveToNext()) {
                        val id = c.getLong(c.getColumnIndexOrThrow(Calendars._ID))
                        val name = c.getString(c.getColumnIndexOrThrow(Calendars.CALENDAR_DISPLAY_NAME)).orEmpty()
                        add(CalendarInfo(id, name, c.getString(c.getColumnIndexOrThrow(Calendars.ACCOUNT_NAME)).orEmpty()))
                    }
                }
            }.orEmpty().sortedBy { it.label().lowercase() }
        } catch (_: SecurityException) {
            // Autorisation retirée entre la vérification et la lecture.
            emptyList()
        }
    }

    /**
     * Les occurrences autour de [day] (récurrences comprises). On lit un jour de plus de chaque côté :
     * les événements « toute la journée » sont rangés en UTC, [todayAgenda] fait le tri exact.
     */
    fun eventsAround(context: Context, day: LocalDate, zone: ZoneId = ZoneId.systemDefault()): List<CalendarEvent> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return emptyList()
        }
        val from = day.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val to = day.plusDays(2).atStartOfDay(zone).toInstant().toEpochMilli()
        val projection = arrayOf(
            Instances.TITLE,
            Instances.BEGIN,
            Instances.END,
            Instances.ALL_DAY,
            Instances.CALENDAR_ID,
            Instances.SELF_ATTENDEE_STATUS
        )
        return try {
            Instances.query(context.contentResolver, projection, from, to)?.use { c ->
                buildList { while (c.moveToNext()) add(c.toEvent()) }
            }.orEmpty()
        } catch (_: SecurityException) {
            emptyList()
        }
    }
}

private fun Cursor.toEvent() = CalendarEvent(
    title = getString(getColumnIndexOrThrow(Instances.TITLE)).orEmpty(),
    begin = getLong(getColumnIndexOrThrow(Instances.BEGIN)),
    end = getLong(getColumnIndexOrThrow(Instances.END)),
    allDay = getInt(getColumnIndexOrThrow(Instances.ALL_DAY)) != 0,
    calendarId = getLong(getColumnIndexOrThrow(Instances.CALENDAR_ID)),
    declined = getInt(getColumnIndexOrThrow(Instances.SELF_ATTENDEE_STATUS)) == Attendees.ATTENDEE_STATUS_DECLINED
)

/**
 * Les événements autour de [day], relus quand l'agenda change (synchronisation, ajout...).
 * Vide si [enabled] est faux ou sans autorisation de lire l'agenda.
 */
@Composable
fun rememberCalendarEvents(enabled: Boolean, day: LocalDate): State<List<CalendarEvent>> {
    val context = LocalContext.current
    return produceState(initialValue = emptyList(), enabled, day) {
        if (!enabled || !DeviceCalendar.hasAccess(context)) {
            value = emptyList()
            return@produceState
        }
        // Pas d'interrogation régulière : Android prévient quand l'agenda change (économie d'énergie).
        val changes = Channel<Unit>(Channel.CONFLATED)
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                changes.trySend(Unit)
            }
        }
        val resolver = context.contentResolver
        try {
            resolver.registerContentObserver(CalendarContract.CONTENT_URI, true, observer)
        } catch (_: SecurityException) {
            // Sans observateur, on lit au moins une fois.
        }
        try {
            changes.trySend(Unit)
            changes.receiveAsFlow().collect {
                value = withContext(Dispatchers.IO) { DeviceCalendar.eventsAround(context, day) }
            }
        } finally {
            resolver.unregisterContentObserver(observer)
        }
    }
}
