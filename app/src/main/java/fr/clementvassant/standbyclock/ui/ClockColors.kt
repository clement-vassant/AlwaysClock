package fr.clementvassant.standbyclock.ui

import androidx.compose.ui.graphics.Color

/** Palette d'une horloge : en mode nuit tout passe en rouge sombre (préserve la vision nocturne). */
data class ClockColors(val primary: Color, val secondary: Color, val accent: Color)

val DayColors = ClockColors(
    primary = Color(0xFFBDBDBD), // gris clair plutôt que blanc : moins d'usure OLED
    secondary = Color(0xFF8E8E93),
    accent = Color(0xFFFF9F0A)
)
val NightColors = ClockColors(
    primary = Color(0xFF8B1A1A),
    secondary = Color(0xFF4A0E0E),
    accent = Color(0xFFA32020)
)
