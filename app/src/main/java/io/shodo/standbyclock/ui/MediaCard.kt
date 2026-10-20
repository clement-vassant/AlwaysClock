package io.shodo.standbyclock.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import io.shodo.standbyclock.NowPlaying

/** Opacité de la pochette : atténuée le jour, encore plus la nuit (pas de grande image vive sur l'OLED). */
private const val ART_ALPHA_DAY = 0.6f
private const val ART_ALPHA_NIGHT = 0.35f

/** Carte « en cours d'écoute » quand le calendrier est masqué : pochette, titre, artiste et contrôles. */
@Composable
fun MediaCard(media: NowPlaying, colors: ClockColors, night: Boolean, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val button = min(maxWidth / 5.5f, maxHeight / 6f)
        val art = min(maxWidth * 0.6f, maxHeight - button * 3.2f).coerceAtLeast(0.dp)
        val text = (button * 0.32f).asSp()
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(button * 0.12f)) {
            AlbumArt(media, night, Modifier.size(art).clip(RoundedCornerShape(art * 0.06f)))
            TitleAndArtist(media, colors, text, Alignment.CenterHorizontally)
            MediaButtons(media, colors, button, Modifier.fillMaxWidth(), Arrangement.SpaceEvenly)
        }
    }
}

/** Version compacte, sous le calendrier : petite pochette, titre et artiste, puis les boutons, sur une ligne. */
@Composable
fun MediaBar(media: NowPlaying, colors: ClockColors, night: Boolean, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val button = min(maxHeight * 0.8f, maxWidth / 9f)
        val text = (button * 0.32f).asSp()
        val gap = button * 0.15f
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(gap)) {
            AlbumArt(media, night, Modifier.size(button).clip(RoundedCornerShape(button * 0.12f)))
            TitleAndArtist(media, colors, text, Alignment.Start, Modifier.weight(1f))
            MediaButtons(media, colors, button, horizontalArrangement = Arrangement.spacedBy(gap))
        }
    }
}

/** Pochette atténuée, en niveaux de gris la nuit ; rien si le lecteur n'en fournit pas. */
@Composable
private fun AlbumArt(media: NowPlaying, night: Boolean, modifier: Modifier = Modifier) {
    val bitmap = remember(media.art) { media.art?.asImageBitmap() } ?: return
    val grayscale = remember(night) { if (night) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null }
    Image(
        bitmap,
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Crop,
        alpha = if (night) ART_ALPHA_NIGHT else ART_ALPHA_DAY,
        colorFilter = grayscale
    )
}

@Composable
private fun TitleAndArtist(
    media: NowPlaying,
    colors: ClockColors,
    textSize: TextUnit,
    alignment: Alignment.Horizontal,
    modifier: Modifier = Modifier
) {
    Column(modifier, horizontalAlignment = alignment) {
        Text(
            media.title,
            color = colors.primary,
            fontSize = textSize * 1.2f,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(media.artist, color = colors.secondary, fontSize = textSize, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Précédent, lecture/pause, suivant, et « j'aime » si le lecteur le propose. */
@Composable
private fun MediaButtons(
    media: NowPlaying,
    colors: ClockColors,
    size: Dp,
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start
) {
    Row(modifier, horizontalArrangement = horizontalArrangement, verticalAlignment = Alignment.CenterVertically) {
        MediaButton(size, colors.secondary, media.previous) { c -> skip(c, forward = false) }
        MediaButton(size, colors.accent, media.playPause, outlined = true) { c -> if (media.isPlaying) pause(c) else play(c) }
        MediaButton(size, colors.secondary, media.next) { c -> skip(c, forward = true) }
        media.liked?.let { liked -> MediaButton(size, colors.accent, media.toggleLike) { c -> heart(c, filled = liked) } }
    }
}

/** Bouton rond : le clic est consommé ici, il ne bascule donc pas jour/nuit. */
@Composable
private fun MediaButton(size: Dp, color: Color, onClick: () -> Unit, outlined: Boolean = false, icon: DrawScope.(Color) -> Unit) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .then(if (outlined) Modifier.border(2.dp, color, CircleShape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(size * 0.45f)) { icon(color) }
    }
}

// Icônes dessinées à la main (pas de dépendance aux icônes Material étendues).

@Suppress("MagicNumber") // coordonnées de dessin de l'icône
private fun DrawScope.play(c: Color) {
    drawPath(
        Path().apply {
            moveTo(size.width * 0.15f, 0f)
            lineTo(size.width, size.height / 2)
            lineTo(size.width * 0.15f, size.height)
            close()
        },
        c
    )
}

@Suppress("MagicNumber") // coordonnées de dessin de l'icône
private fun DrawScope.pause(c: Color) {
    val w = size.width * 0.3f
    drawRect(c, Offset(size.width * 0.1f, 0f), Size(w, size.height))
    drawRect(c, Offset(size.width * 0.6f, 0f), Size(w, size.height))
}

@Suppress("MagicNumber") // coordonnées de dessin de l'icône
private fun DrawScope.skip(c: Color, forward: Boolean) {
    val bar = size.width * 0.14f
    val tri = Path().apply {
        if (forward) {
            moveTo(0f, 0f)
            lineTo(size.width - bar, size.height / 2)
            lineTo(0f, size.height)
        } else {
            moveTo(size.width, 0f)
            lineTo(bar, size.height / 2)
            lineTo(size.width, size.height)
        }
        close()
    }
    drawPath(tri, c)
    drawRect(c, Offset(if (forward) size.width - bar else 0f, 0f), Size(bar, size.height))
}

@Suppress("MagicNumber") // coordonnées de dessin de l'icône
private fun DrawScope.heart(c: Color, filled: Boolean) {
    val w = size.width
    val h = size.height
    val path = Path().apply {
        moveTo(w / 2, h * 0.95f)
        cubicTo(w * -0.1f, h * 0.55f, w * 0.05f, h * 0.0f, w / 2, h * 0.25f)
        cubicTo(w * 0.95f, h * 0.0f, w * 1.1f, h * 0.55f, w / 2, h * 0.95f)
        close()
    }
    drawPath(path, c, style = if (filled) Fill else Stroke(width = w * 0.1f))
}
