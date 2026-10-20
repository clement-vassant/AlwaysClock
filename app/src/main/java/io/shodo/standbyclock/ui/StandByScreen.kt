package io.shodo.standbyclock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.lifecycle.compose.LifecycleResumeEffect
import io.shodo.standbyclock.ClockPrefs
import io.shodo.standbyclock.ClockStyle
import io.shodo.standbyclock.MILLIS_PER_MINUTE
import io.shodo.standbyclock.MILLIS_PER_SECOND
import io.shodo.standbyclock.NowPlaying
import io.shodo.standbyclock.SECONDS_PER_MINUTE
import io.shodo.standbyclock.ThemeMode
import io.shodo.standbyclock.rememberNowPlaying
import java.time.Clock
import java.time.ZonedDateTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay

/** Largeur relative de l'horloge ; le panneau d'à côté (calendrier, musique) a une largeur de 1. */
const val CLOCK_PANE_WEIGHT = 1.4f

/** Hauteur relative de la barre musique sous le calendrier (le calendrier a une hauteur de 1). */
const val MEDIA_BAR_WEIGHT = 0.28f

/** Orbite anti-marquage : positions par tour, tours pour aller du centre au bord, rayons min et max (dp). */
private const val ORBIT_POSITIONS_PER_TURN = 12
private const val ORBIT_TURNS = 4
private const val ORBIT_MIN_RADIUS_DP = 4f
private const val ORBIT_MAX_RADIUS_DP = 20f

/**
 * L'écran de l'horloge, partagé par l'app plein écran et l'écran de veille : horloge (3 styles au balayage),
 * calendrier du mois et musique en cours, jour ou nuit, avec les protections anti-marquage.
 *
 * @param onOpenSettings double toucher ; null dans l'écran de veille, qui a son propre bouton de réglages.
 * @param clock heure utilisée (injectable pour les tests).
 * @param nowPlayingSource musique en cours (injectable pour les tests).
 */
@Composable
fun StandByScreen(
    prefs: ClockPrefs,
    forcedTheme: ThemeMode?,
    onNightModeChange: (Boolean) -> Unit,
    onOpenSettings: (() -> Unit)?,
    modifier: Modifier = Modifier,
    clock: Clock = Clock.systemDefaultZone(),
    nowPlayingSource: @Composable (enabled: Boolean) -> NowPlaying? = { rememberNowPlaying(it).value }
) {
    // La nuit, rien n'affiche les secondes : on ne se réveille qu'une fois par minute (économie d'énergie).
    var minuteTicks by remember { mutableStateOf(false) }
    val now by rememberTicker(everyMinute = minuteTicks, clock = clock)
    // Les réglages peuvent changer pendant que l'horloge est en arrière-plan : on les relit à chaque reprise.
    val resumes = rememberResumeCount()
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
    val currentOnNightModeChange by rememberUpdatedState(onNightModeChange)
    LaunchedEffect(night) {
        minuteTicks = night
        currentOnNightModeChange(night)
    }
    LaunchedEffect(showCalendar) { prefs.showCalendar = showCalendar }

    val pager = rememberStylePager(prefs, resumes)

    // Musique en lecture (ou en pause depuis peu) : barre sous le calendrier, ou tout le panneau s'il est masqué.
    val media = nowPlaying?.takeIf { it.isRecent(clock.millis()) }
    val state = ScreenState(now, if (night) NightColors else DayColors, night, showCalendar, media)
    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag(TAG_SCREEN)
            .semantics { stateDescription = if (night) "nuit" else "jour" }
            // Toucher : jour/nuit. Appui long : calendrier. Double toucher : réglages (dans l'app seulement).
            .pointerInput(onOpenSettings) {
                detectTapGestures(
                    onTap = { manualNight = !currentNight },
                    onLongPress = { showCalendar = !showCalendar },
                    onDoubleTap = if (onOpenSettings != null) { _: Offset -> onOpenSettings() } else null
                )
            }
    ) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
            ClockAndSidePane(ClockStyle.entries[page], state)
        }
    }
}

/** Ce que l'écran affiche à un instant donné. */
private class ScreenState(
    val now: ZonedDateTime,
    val colors: ClockColors,
    val night: Boolean,
    val showCalendar: Boolean,
    val media: NowPlaying?
) {
    val hasSidePane get() = media != null || showCalendar
}

/** Nombre de reprises de l'écran (onResume) : sert de clé pour relire les réglages. */
@Composable
private fun rememberResumeCount(): Int {
    var resumes by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        resumes++
        onPauseOrDispose { }
    }
    return resumes
}

/** Pager des styles d'horloge, synchronisé avec le style enregistré. */
@Composable
private fun rememberStylePager(prefs: ClockPrefs, resumes: Int): PagerState {
    val styles = ClockStyle.entries
    val pager = rememberPagerState(initialPage = prefs.style.ordinal) { styles.size }
    LaunchedEffect(resumes) {
        if (prefs.style.ordinal != pager.currentPage) pager.scrollToPage(prefs.style.ordinal)
    }
    LaunchedEffect(pager) {
        snapshotFlow { pager.currentPage }.collect { prefs.style = styles[it] }
    }
    return pager
}

/**
 * Une page du pager : l'horloge et son panneau, côte à côte en paysage, l'un au-dessus de l'autre en portrait.
 * Anti-marquage : le tout suit une orbite lente (nouvelle position chaque minute) et l'horloge échange
 * sa place avec le panneau chaque heure.
 */
@Composable
private fun ClockAndSidePane(style: ClockStyle, state: ScreenState) {
    val epochMinute = state.now.toEpochSecond() / SECONDS_PER_MINUTE
    val shift = remember(epochMinute) { orbitOffset(epochMinute) }
    val clockFirst = clockOnLeft(state.now.hour)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val portrait = isPortrait(constraints.maxWidth, constraints.maxHeight)
        val orbit = Modifier
            .fillMaxSize()
            .padding(horizontal = if (portrait) 24.dp else 40.dp, vertical = if (portrait) 40.dp else 24.dp)
            .offset { IntOffset((shift.x * density).roundToInt(), (shift.y * density).roundToInt()) }
        if (portrait) {
            Column(orbit, horizontalAlignment = Alignment.CenterHorizontally) {
                if (state.hasSidePane && !clockFirst) SidePane(state, Modifier.weight(1f).fillMaxWidth())
                ClockPane(style, state, Modifier.weight(CLOCK_PANE_WEIGHT).fillMaxWidth())
                if (state.hasSidePane && clockFirst) SidePane(state, Modifier.weight(1f).fillMaxWidth())
            }
        } else {
            Row(orbit, verticalAlignment = Alignment.CenterVertically) {
                if (state.hasSidePane && !clockFirst) SidePane(state, Modifier.weight(1f).fillMaxHeight())
                ClockPane(style, state, Modifier.weight(CLOCK_PANE_WEIGHT).fillMaxHeight())
                if (state.hasSidePane && clockFirst) SidePane(state, Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}

@Composable
private fun ClockPane(style: ClockStyle, state: ScreenState, modifier: Modifier = Modifier) {
    Box(modifier.testTag(clockTag(style)), contentAlignment = Alignment.Center) { ClockFace(style, state) }
}

@Composable
private fun ClockFace(style: ClockStyle, state: ScreenState) {
    val (now, colors, night) = Triple(state.now, state.colors, state.night)
    when (style) {
        ClockStyle.DIGITAL -> DigitalClock(now, colors, showSeconds = !night)

        ClockStyle.ANALOG -> AnalogClockWithDate(
            now,
            colors,
            showDate = !state.hasSidePane,
            showSeconds = !night,
            // Barre musique sous le calendrier : il perd la hauteur de la barre.
            calendarHeight = if (state.media != null && state.showCalendar) 1f / (1f + MEDIA_BAR_WEIGHT) else 1f
        )

        ClockStyle.MINIMAL -> MinimalClock(now, colors)
    }
}

/**
 * Panneau à côté de l'horloge (ou dessous en portrait) : le calendrier, avec la musique en cours en barre
 * compacte dessous. Calendrier masqué (appui long) : la musique a tout le panneau.
 */
@Composable
private fun SidePane(state: ScreenState, modifier: Modifier = Modifier) {
    val media = state.media
    Box(modifier, contentAlignment = Alignment.Center) {
        when {
            media == null -> MonthCalendar(state.now, state.colors)

            !state.showCalendar -> MediaCard(media, state.colors, state.night, Modifier.testTag(TAG_MEDIA_CARD))

            else -> Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { MonthCalendar(state.now, state.colors) }
                Box(Modifier.weight(MEDIA_BAR_WEIGHT).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    MediaBar(media, state.colors, state.night, Modifier.testTag(TAG_MEDIA_BAR))
                }
            }
        }
    }
}

/** Heures paires : horloge à gauche (en haut en portrait) ; heures impaires : à droite (en bas) (anti-marquage). */
fun clockOnLeft(hour: Int) = hour % 2 == 0

/**
 * Position anti-marquage (en dp) à la minute [epochMinute] : [ORBIT_POSITIONS_PER_TURN] positions par tour,
 * le rayon va de [ORBIT_MIN_RADIUS_DP] à [ORBIT_MAX_RADIUS_DP] puis revient en [ORBIT_TURNS] tours,
 * pour ne jamais rester deux minutes au même endroit.
 */
internal fun orbitOffset(epochMinute: Long): Offset {
    val steps = ORBIT_POSITIONS_PER_TURN * ORBIT_TURNS
    val phase = (epochMinute % (2L * steps)).toInt()
    val progress = if (phase < steps) phase else 2 * steps - phase
    val radius = ORBIT_MIN_RADIUS_DP + (ORBIT_MAX_RADIUS_DP - ORBIT_MIN_RADIUS_DP) * progress / steps
    val angle = 2 * PI * (epochMinute % ORBIT_POSITIONS_PER_TURN) / ORBIT_POSITIONS_PER_TURN
    return Offset((radius * cos(angle)).toFloat(), (radius * sin(angle)).toFloat())
}

/** Attente jusqu'au début de la prochaine seconde, ou de la prochaine minute si [everyMinute]. */
fun tickDelayMillis(everyMinute: Boolean, nowMillis: Long): Long {
    val period = if (everyMinute) MILLIS_PER_MINUTE else MILLIS_PER_SECOND
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
