package io.shodo.standbyclock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.shodo.standbyclock.ClockPrefs
import io.shodo.standbyclock.ClockStyle
import io.shodo.standbyclock.NowPlaying
import io.shodo.standbyclock.rememberNowPlaying
import io.shodo.standbyclock.ThemeMode
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.delay
import java.time.Clock
import java.time.ZonedDateTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Palette d'une horloge : en mode nuit tout passe en rouge sombre (préserve la vision nocturne). */
data class ClockColors(val primary: Color, val secondary: Color, val accent: Color)

val DayColors = ClockColors(
    primary = Color(0xFFBDBDBD),   // gris clair plutôt que blanc : moins d'usure OLED
    secondary = Color(0xFF8E8E93),
    accent = Color(0xFFFF9F0A),
)
val NightColors = ClockColors(
    primary = Color(0xFF8B1A1A),
    secondary = Color(0xFF4A0E0E),
    accent = Color(0xFFA32020),
)

@Composable
fun StandByScreen(
    prefs: ClockPrefs,
    forcedTheme: ThemeMode?,
    onNightModeChanged: (Boolean) -> Unit,
    onOpenSettings: (() -> Unit)?,
    // Injectables pour les tests : heure fixe, musique simulée.
    clock: Clock = Clock.systemDefaultZone(),
    nowPlayingSource: @Composable (enabled: Boolean) -> NowPlaying? = { rememberNowPlaying(it).value },
) {
    // La nuit, rien n'affiche les secondes : on ne se réveille qu'une fois par minute (économie d'énergie).
    var minuteTicks by remember { mutableStateOf(false) }
    val now by rememberTicker(everyMinute = minuteTicks, clock = clock)
    // Les réglages peuvent changer pendant que l'horloge est en arrière-plan : on les relit à chaque reprise.
    var resumes by remember { mutableStateOf(0) }
    LifecycleResumeEffect(Unit) {
        resumes++
        onPauseOrDispose { }
    }
    val themeMode = remember(forcedTheme, resumes) { forcedTheme ?: prefs.themeMode }
    var showCalendar by remember(resumes) { mutableStateOf(prefs.showCalendar) }
    val showMedia = remember(resumes) { prefs.showMedia }
    // Relancé à chaque reprise : l'accès aux notifications a pu être accordé entre-temps.
    val nowPlaying = key(resumes) { nowPlayingSource(showMedia) }

    // Un toucher inverse jour/nuit jusqu'à la prochaine bascule programmée.
    val scheduledNight = prefs.isNight(themeMode, now.toLocalTime())
    var manualNight by remember(scheduledNight, themeMode) { mutableStateOf<Boolean?>(null) }
    val night = manualNight ?: scheduledNight
    val currentNight by rememberUpdatedState(night)
    LaunchedEffect(night) {
        minuteTicks = night
        onNightModeChanged(night)
    }
    LaunchedEffect(showCalendar) { prefs.showCalendar = showCalendar }

    val styles = ClockStyle.entries
    val pager = rememberPagerState(initialPage = prefs.style.ordinal) { styles.size }
    LaunchedEffect(resumes) {
        if (prefs.style.ordinal != pager.currentPage) pager.scrollToPage(prefs.style.ordinal)
    }
    LaunchedEffect(pager) {
        snapshotFlow { pager.currentPage }.collect { prefs.style = styles[it] }
    }

    // Anti-marquage OLED : tout le contenu suit une orbite lente (nouvelle position chaque minute)
    // et l'horloge échange sa place avec le calendrier chaque heure.
    val epochMinute = now.toEpochSecond() / 60
    val shift = remember(epochMinute) { orbitOffset(epochMinute) }
    val clockFirst = clockOnLeft(now.hour)

    // Musique en lecture (ou en pause depuis peu) : barre sous le calendrier, ou tout le panneau s'il est masqué.
    val media = nowPlaying?.takeIf { it.isRecent(clock.millis()) }
    val sidePane = media != null || showCalendar

    val colors = if (night) NightColors else DayColors
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag(TAG_SCREEN)
            .semantics { stateDescription = if (night) "nuit" else "jour" }
            // Toucher : jour/nuit. Appui long : calendrier. Double toucher : réglages (dans l'app seulement).
            .pointerInput(onOpenSettings) {
                detectTapGestures(
                    onTap = { manualNight = !currentNight },
                    onLongPress = { showCalendar = !showCalendar },
                    onDoubleTap = if (onOpenSettings != null) { _: Offset -> onOpenSettings() } else null,
                )
            },
    ) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 40.dp, vertical = 24.dp)
                    .offset { IntOffset((shift.x * density).roundToInt(), (shift.y * density).roundToInt()) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (sidePane && !clockFirst) SidePane(now, colors, media, night, showCalendar)
                Box(Modifier.weight(1.4f).fillMaxHeight().testTag(clockTag(styles[page])), contentAlignment = Alignment.Center) {
                    when (styles[page]) {
                        ClockStyle.DIGITAL -> DigitalClock(now, colors, showSeconds = !night)
                        ClockStyle.ANALOG -> AnalogClockWithDate(
                            now, colors, showDate = !sidePane, showSeconds = !night,
                            // Barre musique sous le calendrier : il ne prend plus que 1/1.28 de la hauteur.
                            calendarHeight = if (media != null && showCalendar) 1f / 1.28f else 1f,
                        )
                        ClockStyle.MINIMAL -> MinimalClock(now, colors)
                    }
                }
                if (sidePane && clockFirst) SidePane(now, colors, media, night, showCalendar)
            }
        }
    }
}

/**
 * Panneau à côté de l'horloge : le calendrier, avec la musique en cours en barre compacte dessous.
 * Calendrier masqué (appui long) : la musique a tout le panneau.
 */
@Composable
private fun RowScope.SidePane(now: ZonedDateTime, colors: ClockColors, media: NowPlaying?, night: Boolean, showCalendar: Boolean) {
    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
        when {
            media == null -> MonthCalendar(now, colors)
            !showCalendar -> Box(Modifier.testTag(TAG_MEDIA_CARD)) { MediaCard(media, colors, night) }
            else -> Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { MonthCalendar(now, colors) }
                Box(Modifier.weight(0.28f).fillMaxWidth().testTag(TAG_MEDIA_BAR), contentAlignment = Alignment.Center) {
                    MediaBar(media, colors, night)
                }
            }
        }
    }
}

/** Heures paires : horloge à gauche ; heures impaires : à droite (anti-marquage). */
fun clockOnLeft(hour: Int) = hour % 2 == 0

/**
 * Position anti-marquage (en dp) à la minute [epochMinute] : 12 positions par tour, le rayon
 * va de 4 à 20 dp puis revient en 4 tours, pour ne jamais rester deux minutes au même endroit.
 */
internal fun orbitOffset(epochMinute: Long): Offset {
    val perTurn = 12
    val steps = perTurn * 4
    val phase = (epochMinute % (2L * steps)).toInt()
    val progress = if (phase < steps) phase else 2 * steps - phase
    val radius = 4f + 16f * progress / steps
    val angle = 2 * PI * (epochMinute % perTurn) / perTurn
    return Offset((radius * cos(angle)).toFloat(), (radius * sin(angle)).toFloat())
}

/** Attente jusqu'au début de la prochaine seconde, ou de la prochaine minute si [everyMinute]. */
fun tickDelayMillis(everyMinute: Boolean, nowMillis: Long): Long {
    val period = if (everyMinute) 60_000L else 1000L
    return period - nowMillis % period
}

/** L'heure courante, rafraîchie au début de chaque seconde, ou de chaque minute si [everyMinute]. */
@Composable
fun rememberTicker(everyMinute: Boolean = false, clock: Clock = Clock.systemDefaultZone()) =
    produceState(initialValue = ZonedDateTime.now(clock), everyMinute, clock) {
        while (true) {
            value = ZonedDateTime.now(clock)
            delay(tickDelayMillis(everyMinute, clock.millis()))
        }
    }
