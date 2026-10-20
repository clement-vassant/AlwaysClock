package io.shodo.standbyclock

import android.view.Window

/**
 * Demande le mode le plus lent de l'écran courant (60 Hz au lieu de 120 sur le Fold) :
 * une horloge n'a pas besoin de plus, et l'écran consomme moins.
 * À rappeler quand l'écran change (Fold plié ou déplié).
 */
fun Window.preferLowestRefreshRate() {
    val display = decorView.display ?: return
    val current = display.mode
    val slowest = display.supportedModes
        .filter { it.physicalWidth == current.physicalWidth && it.physicalHeight == current.physicalHeight }
        .minByOrNull { it.refreshRate } ?: return
    attributes = attributes.apply {
        preferredDisplayModeId = slowest.modeId
        preferredRefreshRate = slowest.refreshRate
    }
}
