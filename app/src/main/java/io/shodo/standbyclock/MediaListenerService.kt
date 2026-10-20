package io.shodo.standbyclock

import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import androidx.core.app.NotificationManagerCompat

/**
 * Écouteur de notifications vide : Android ne donne accès aux sessions média des autres apps
 * (Spotify...) qu'aux apps dont un écouteur a reçu l'« accès aux notifications ».
 */
class MediaListenerService : NotificationListenerService() {
    companion object {
        fun component(context: Context) = ComponentName(context, MediaListenerService::class.java)

        fun isGranted(context: Context) =
            context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)
    }
}
