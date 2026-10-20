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
    /** En pause : instant de la pause (heure murale, en ms). */
    val pausedAt: Long,
    /** null si le lecteur ne propose pas de « j'aime ». */
    val liked: Boolean?,
    val previous: () -> Unit = {},
    val next: () -> Unit = {},
    val playPause: () -> Unit = {},
    val toggleLike: () -> Unit = {},
) {
    fun isRecent(nowMillis: Long): Boolean = isRecentMedia(isPlaying, pausedAt, nowMillis)
}

/** La carte musique s'affiche en lecture, ou en pause depuis moins de 10 minutes. */
fun isRecentMedia(isPlaying: Boolean, pausedAt: Long, nowMillis: Long): Boolean =
    isPlaying || nowMillis - pausedAt < 10 * 60_000L

private val likeWords = listOf("like", "heart", "favorite", "favourite", "love", "library", "save", "collection", "favori", "j'aime")
private val unlikeWords = listOf("remove", "unlike", "unfavorite", "unsave", "dislike", "supprimer", "retirer")

/**
 * Spotify et d'autres exposent le « j'aime » comme action personnalisée (chez Spotify en français :
 * « Ajouter à la collection » / « Supprimer de la collection »).
 * null si le libellé n'est pas un « j'aime » ; sinon true s'il retire (morceau déjà aimé), false s'il ajoute.
 */
fun likeLabelState(label: String): Boolean? {
    val text = label.lowercase()
    if (likeWords.none { it in text }) return null
    return unlikeWords.any { it in text }
}

private fun PlaybackState.CustomAction.label() = "$action $name"

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
    val likeAction = state?.customActions?.firstOrNull { likeLabelState(it.label()) != null }
    val heartRating = ratingType == Rating.RATING_HEART
    val liked = when {
        likeAction != null -> likeLabelState(likeAction.label())
        heartRating -> meta.getRating(MediaMetadata.METADATA_KEY_USER_RATING)?.hasHeart() == true
        else -> null
    }
    // L'instant de la pause est donné en temps écoulé depuis le démarrage : on le ramène à l'heure murale.
    val sinceUpdate = state?.lastPositionUpdateTime?.takeIf { it > 0 }?.let { SystemClock.elapsedRealtime() - it } ?: 0L
    val controls = transportControls
    return NowPlaying(
        title = title,
        artist = artist,
        art = art,
        isPlaying = playing,
        pausedAt = System.currentTimeMillis() - sinceUpdate,
        liked = liked,
        previous = controls::skipToPrevious,
        next = controls::skipToNext,
        playPause = { if (playing) controls.pause() else controls.play() },
        toggleLike = {
            when {
                likeAction != null -> controls.sendCustomAction(likeAction, null)
                heartRating -> controls.setRating(Rating.newHeartRating(liked != true))
            }
        },
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
