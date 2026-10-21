package io.shodo.standbyclock.ui

/** Repères pour les tests d'interface. */
const val TAG_SCREEN = "standby"
const val TAG_CALENDAR = "calendar"
const val TAG_DIAL = "dial"
const val TAG_MEDIA_BAR = "media-bar"
const val TAG_MEDIA_CARD = "media-card"
const val TAG_CALENDAR_SWITCH = "calendar-switch"
const val TAG_AGENDA = "agenda"
const val TAG_EVENTS_SWITCH = "events-switch"

fun calendarChoiceTag(id: Long) = "calendar-choice-$id"

fun clockTag(style: io.shodo.standbyclock.ClockStyle) = "clock-$style"
