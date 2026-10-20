package io.shodo.standbyclock.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.shodo.standbyclock.ClockStyle
import io.shodo.standbyclock.SECONDS_PER_MINUTE
import kotlin.math.roundToInt

/**
 * Une page du pager : l'horloge et son panneau, côte à côte en paysage, l'un au-dessus de l'autre en portrait.
 * Anti-marquage : le tout suit une orbite lente (nouvelle position chaque minute) et l'horloge échange
 * sa place avec le panneau chaque heure.
 */
@Composable
internal fun ClockAndSidePane(style: ClockStyle, state: ScreenState) {
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
                ClockPane(style, state, portrait = true, modifier = Modifier.weight(CLOCK_PANE_WEIGHT).fillMaxWidth())
                if (state.hasSidePane && clockFirst) SidePane(state, Modifier.weight(1f).fillMaxWidth())
            }
        } else {
            Row(orbit, verticalAlignment = Alignment.CenterVertically) {
                if (state.hasSidePane && !clockFirst) SidePane(state, Modifier.weight(1f).fillMaxHeight())
                ClockPane(style, state, portrait = false, modifier = Modifier.weight(CLOCK_PANE_WEIGHT).fillMaxHeight())
                if (state.hasSidePane && clockFirst) SidePane(state, Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}

@Composable
private fun ClockPane(style: ClockStyle, state: ScreenState, portrait: Boolean, modifier: Modifier = Modifier) {
    Box(modifier.testTag(clockTag(style)), contentAlignment = Alignment.Center) { ClockFace(style, state, portrait) }
}

@Composable
private fun ClockFace(style: ClockStyle, state: ScreenState, portrait: Boolean) {
    val (now, colors, night) = Triple(state.now, state.colors, state.night)
    when (style) {
        ClockStyle.DIGITAL -> DigitalClock(now, colors, showSeconds = !night)

        ClockStyle.ANALOG -> AnalogClockWithDate(
            now,
            colors,
            showDate = !state.hasSidePane,
            showSeconds = !night,
            calendarWidth = sidePaneWidth(portrait),
            calendarHeight = sidePaneHeight(portrait, withMediaBar = state.media != null && state.showCalendar)
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

/** Largeur du calendrier en fraction de celle de l'horloge : moins large à côté, aussi large dessous. */
internal fun sidePaneWidth(portrait: Boolean) = if (portrait) 1f else 1f / CLOCK_PANE_WEIGHT

/** Hauteur du calendrier en fraction de celle de l'horloge ; la barre musique sous le calendrier lui prend sa hauteur. */
internal fun sidePaneHeight(portrait: Boolean, withMediaBar: Boolean): Float {
    val pane = if (portrait) 1f / CLOCK_PANE_WEIGHT else 1f
    return if (withMediaBar) pane / (1f + MEDIA_BAR_WEIGHT) else pane
}
