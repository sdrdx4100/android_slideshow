package com.example.slideshowclock.media

import android.service.notification.NotificationListenerService

/**
 * Intentionally empty. We never read notifications here — the only reason this
 * service exists is that, once the user *enables* it under "Notification access",
 * the system lets us call [android.media.session.MediaSessionManager.getActiveSessions]
 * to observe and control whatever app is currently playing audio.
 */
class MediaNotificationListenerService : NotificationListenerService()
