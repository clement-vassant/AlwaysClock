package io.shodo.standbyclock.ui

/** Repères pour les tests d'interface. */
const val TAG_SCREEN = "standby"
const val TAG_CALENDAR = "calendar"
const val TAG_DIAL = "dial"
const val TAG_MEDIA_BAR = "media-bar"
const val TAG_MEDIA_CARD = "media-card"
const val TAG_CALENDAR_SWITCH = "calendar-switch"

fun clockTag(style: io.shodo.standbyclock.ClockStyle) = "clock-$style"
