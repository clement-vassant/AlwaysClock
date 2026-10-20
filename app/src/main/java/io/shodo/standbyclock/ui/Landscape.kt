package io.shodo.standbyclock.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints

/** Quart de tour (degrés) appliqué au contenu pour le passer de portrait en paysage. */
const val QUARTER_TURN_DEGREES = 90f

/** Un écran plus haut que large est en portrait : horloge et calendrier s'empilent, ou l'horloge est tournée. */
fun isPortrait(width: Int, height: Int) = height > width

/**
 * Affiche toujours le contenu en paysage (réglage « Toujours paysage »). Un écran de veille n'est pas
 * une activité : il ne peut pas demander l'orientation paysage, et reste en portrait si la rotation
 * automatique est coupée.
 * En portrait, on mesure donc le contenu avec largeur et hauteur échangées, puis on le tourne d'un quart
 * de tour dans le sens horaire : il se lit téléphone couché, haut de l'écran vers la gauche.
 */
fun Modifier.forceLandscape(): Modifier = layout { measurable, constraints ->
    val bounded = constraints.hasBoundedWidth && constraints.hasBoundedHeight
    if (!bounded || !isPortrait(constraints.maxWidth, constraints.maxHeight)) {
        val placeable = measurable.measure(constraints)
        return@layout layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }
    val width = constraints.maxWidth
    val height = constraints.maxHeight
    val placeable = measurable.measure(Constraints.fixed(width = height, height = width))
    layout(width, height) {
        // Centré puis tourné autour de son centre : le contenu couché occupe exactement l'écran.
        placeable.placeWithLayer(x = (width - height) / 2, y = (height - width) / 2) {
            rotationZ = QUARTER_TURN_DEGREES
        }
    }
}
