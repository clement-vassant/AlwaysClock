package io.shodo.standbyclock

import android.content.pm.ActivityInfo

/**
 * Orientation demandée par l'app plein écran, d'après le même réglage que l'écran de veille.
 * Automatique : suit la rotation du téléphone, verrou de rotation compris (FULL_USER plutôt que SENSOR,
 * qui passerait outre le choix de l'utilisateur). Toujours paysage : les deux côtés paysage, pour poser
 * le téléphone dans un sens ou dans l'autre sur son chargeur.
 */
fun activityOrientation(mode: OrientationMode): Int = when (mode) {
    OrientationMode.AUTO -> ActivityInfo.SCREEN_ORIENTATION_FULL_USER
    OrientationMode.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
}
