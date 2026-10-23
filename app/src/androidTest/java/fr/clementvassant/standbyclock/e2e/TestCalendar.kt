package fr.clementvassant.standbyclock.e2e

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.graphics.Color
import android.net.Uri
import android.provider.CalendarContract
import android.provider.CalendarContract.Calendars
import android.provider.CalendarContract.Events
import androidx.test.platform.app.InstrumentationRegistry
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Agenda local créé pour un parcours, puis supprimé : jamais de compte réel.
 * L'app ne demande que la lecture ; l'écriture se fait avec les droits du shell, le temps de l'écriture seulement,
 * pour que l'app reste sans accès tant que l'utilisateur ne l'a pas accordé dans la boîte de dialogue.
 */
internal class TestCalendar private constructor(val id: Long) {

    /** Événement « toute la journée » aujourd'hui : affiché quelle que soit l'heure du run. */
    fun addAllDayEventToday(title: String) = asShell {
        val today = LocalDate.now()
        val values = ContentValues().apply {
            put(Events.CALENDAR_ID, id)
            put(Events.TITLE, title)
            // Android range les journées entières de minuit à minuit UTC.
            put(Events.DTSTART, today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
            put(Events.DTEND, today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
            put(Events.ALL_DAY, 1)
            put(Events.EVENT_TIMEZONE, "UTC")
        }
        checkNotNull(E2e.context.contentResolver.insert(Events.CONTENT_URI.asSyncAdapter(), values)) { "Événement non créé" }
    }

    fun delete() = asShell {
        E2e.context.contentResolver.delete(ContentUris.withAppendedId(Calendars.CONTENT_URI, id).asSyncAdapter(), null, null)
    }

    companion object {
        private const val ACCOUNT = "standbyclock-e2e"

        fun create(name: String): TestCalendar = asShell {
            // Un run interrompu (émulateur du Mac) a pu laisser son agenda : on repart de zéro.
            E2e.context.contentResolver.delete(Calendars.CONTENT_URI.asSyncAdapter(), "${Calendars.ACCOUNT_NAME} = ?", arrayOf(ACCOUNT))
            val values = ContentValues().apply {
                put(Calendars.ACCOUNT_NAME, ACCOUNT)
                put(Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
                put(Calendars.NAME, name)
                put(Calendars.CALENDAR_DISPLAY_NAME, name)
                put(Calendars.CALENDAR_COLOR, Color.BLUE)
                put(Calendars.CALENDAR_ACCESS_LEVEL, Calendars.CAL_ACCESS_OWNER)
                put(Calendars.OWNER_ACCOUNT, ACCOUNT)
                put(Calendars.VISIBLE, 1)
                put(Calendars.SYNC_EVENTS, 1)
            }
            val uri = checkNotNull(E2e.context.contentResolver.insert(Calendars.CONTENT_URI.asSyncAdapter(), values)) { "Agenda non créé" }
            TestCalendar(ContentUris.parseId(uri))
        }

        private fun Uri.asSyncAdapter(): Uri = buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(Calendars.ACCOUNT_NAME, ACCOUNT)
            .appendQueryParameter(Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            .build()

        private fun <T> asShell(block: () -> T): T {
            val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
            automation.adoptShellPermissionIdentity(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
            return try {
                block()
            } finally {
                automation.dropShellPermissionIdentity()
            }
        }
    }
}
