package io.shodo.standbyclock

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.Rating
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** Ce qui est en cours d'écoute sur le téléphone, avec de quoi le piloter. */
class NowPlaying(
    val title: String,
    val artist: String,
    val art: Bitmap?,
    val isPlaying: Boolean,
    /** En pause : instant de la pause (horloge [SystemClock.elapsedRealtime]). */
    val pausedAt: Long,
    /** null si le lecteur ne propose pas de « j'aime ». */
    val liked: Boolean?,
    private val controller: MediaController,
    private val likeAction: PlaybackState.CustomAction?,
) {
    /** En lecture, ou en pause depuis moins de 10 minutes. */
    fun isRecent(): Boolean = isPlaying || SystemClock.elapsedRealtime() - pausedAt < 10 * 60_000L

    fun previous() = controller.transportControls.skipToPrevious()
    fun next() = controller.transportControls.skipToNext()
    fun playPause() = if (isPlaying) controller.transportControls.pause() else controller.transportControls.play()

    fun toggleLike() {
        if (likeAction != null) {
            controller.transportControls.sendCustomAction(likeAction, null)
        } else if (controller.ratingType == Rating.RATING_HEART) {
            controller.transportControls.setRating(Rating.newHeartRating(liked != true))
        }
    }
}

private val likeWords = listOf("like", "heart", "favorite", "favourite", "love", "library", "save", "collection", "favori", "j'aime")
private val unlikeWords = listOf("remove", "unlike", "unfavorite", "unsave", "dislike", "supprimer", "retirer")

/** Spotify et d'autres exposent le « j'aime » comme action personnalisée (« Ajouter à la collection » / « Supprimer de la collection » chez Spotify en français) ; son libellé dit s'il ajoute ou retire. */
private fun PlaybackState.likeAction(): PlaybackState.CustomAction? = customActions.firstOrNull { a ->
    val text = "${a.action} ${a.name}".lowercase()
    likeWords.any { it in text }
}

private fun MediaController.toNowPlaying(): NowPlaying? {
    val meta = metadata ?: return null
    val state = playbackState
    val title = meta.getString(MediaMetadata.METADATA_KEY_TITLE) ?: return null
    val artist = meta.getString(MediaMetadata.METADATA_KEY_ARTIST)
        ?: meta.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST) ?: ""
    val art = meta.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
        ?: meta.getBitmap(MediaMetadata.METADATA_KEY_ART)
        ?: meta.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
    val playing = state?.state == PlaybackState.STATE_PLAYING || state?.state == PlaybackState.STATE_BUFFERING
    val like = state?.likeAction()
    val liked = when {
        like != null -> unlikeWords.any { it in "${like.action} ${like.name}".lowercase() }
        ratingType == Rating.RATING_HEART -> meta.getRating(MediaMetadata.METADATA_KEY_USER_RATING)?.hasHeart() == true
        else -> null
    }
    return NowPlaying(
        title = title,
        artist = artist,
        art = art,
        isPlaying = playing,
        pausedAt = state?.lastPositionUpdateTime?.takeIf { it > 0 } ?: SystemClock.elapsedRealtime(),
        liked = liked,
        controller = this,
        likeAction = like,
    )
}

/**
 * Suit la session média active (en priorité celle qui joue). null si [enabled] est faux,
 * si l'accès aux notifications n'est pas accordé, ou si rien n'est en cours.
 */
@Composable
fun rememberNowPlaying(enabled: Boolean): State<NowPlaying?> {
    val context = LocalContext.current
    val state = remember { mutableStateOf<NowPlaying?>(null) }
    DisposableEffect(enabled) {
        if (!enabled || !MediaListenerService.isGranted(context)) {
            state.value = null
            return@DisposableEffect onDispose { }
        }
        val tracker = NowPlayingTracker(context) { state.value = it }
        onDispose { tracker.close() }
    }
    return state
}

private class NowPlayingTracker(context: Context, private val onChange: (NowPlaying?) -> Unit) {
    private val manager = context.getSystemService(MediaSessionManager::class.java)
    private val component = MediaListenerService.component(context)
    private val handler = Handler(Looper.getMainLooper())
    private var current: MediaController? = null

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
        override fun onPlaybackStateChanged(state: PlaybackState?) = pick(manager.safeSessions())
        override fun onSessionDestroyed() = pick(manager.safeSessions())
    }
    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { pick(it.orEmpty()) }

    init {
        try {
            manager.addOnActiveSessionsChangedListener(sessionsListener, component, handler)
        } catch (e: SecurityException) {
            // Accès retiré entre-temps : on n'affiche rien.
        }
        pick(manager.safeSessions())
    }

    private fun MediaSessionManager.safeSessions(): List<MediaController> =
        try { getActiveSessions(component) } catch (e: SecurityException) { emptyList() }

    private fun pick(sessions: List<MediaController>) {
        val best = sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: sessions.firstOrNull()
        if (best?.sessionToken != current?.sessionToken) {
            current?.unregisterCallback(callback)
            current = best
            best?.registerCallback(callback, handler)
        }
        publish()
    }

    private fun publish() = onChange(current?.toNowPlaying())

    fun close() {
        current?.unregisterCallback(callback)
        manager.removeOnActiveSessionsChangedListener(sessionsListener)
    }
}
