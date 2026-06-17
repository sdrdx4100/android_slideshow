package com.example.slideshowclock.media

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A snapshot of whatever audio app is currently in focus. */
data class NowPlaying(
    val title: String,
    val artist: String?,
    val isPlaying: Boolean,
)

/**
 * Observes and controls the foreground media session of *other* apps (Apple Music,
 * Spotify, …) via [MediaSessionManager]. Requires our [MediaNotificationListenerService]
 * to be enabled under Notification access; until then [hasPermission] stays false and
 * [state] is null.
 *
 * Lifecycle is explicit: call [start] when the screen is shown, [stop] when hidden, and
 * [refresh] on resume (to pick up a freshly granted permission or a changed session).
 */
class NowPlayingController(private val context: Context) {

    private val component = ComponentName(context, MediaNotificationListenerService::class.java)
    private val sessionManager =
        context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager

    private val _state = MutableStateFlow<NowPlaying?>(null)
    val state: StateFlow<NowPlaying?> = _state.asStateFlow()

    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    private var controller: MediaController? = null
    private var started = false

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
        override fun onSessionDestroyed() {
            detachController()
            refresh()
        }
    }

    private val sessionsListener =
        MediaSessionManager.OnActiveSessionsChangedListener { refresh() }

    fun start() {
        if (started) return
        started = true
        refresh()
        if (_hasPermission.value) {
            runCatching {
                sessionManager.addOnActiveSessionsChangedListener(sessionsListener, component)
            }
        }
    }

    fun stop() {
        started = false
        runCatching { sessionManager.removeOnActiveSessionsChangedListener(sessionsListener) }
        detachController()
    }

    /** Re-evaluate permission and re-pick the active session. Safe to call repeatedly. */
    fun refresh() {
        val granted = isNotificationAccessGranted()
        val wasGranted = _hasPermission.value
        _hasPermission.value = granted
        if (!granted) {
            detachController()
            _state.value = null
            return
        }
        // If permission was just granted while running, begin listening for changes.
        if (started && !wasGranted) {
            runCatching {
                sessionManager.addOnActiveSessionsChangedListener(sessionsListener, component)
            }
        }

        val controllers = runCatching {
            sessionManager.getActiveSessions(component)
        }.getOrDefault(emptyList())

        val chosen = controllers.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        } ?: controllers.firstOrNull()

        attachController(chosen)
        publish()
    }

    fun playPause() {
        val c = controller ?: return
        if (c.playbackState?.state == PlaybackState.STATE_PLAYING) {
            c.transportControls.pause()
        } else {
            c.transportControls.play()
        }
    }

    fun next() {
        controller?.transportControls?.skipToNext()
    }

    fun previous() {
        controller?.transportControls?.skipToPrevious()
    }

    private fun attachController(next: MediaController?) {
        if (next?.sessionToken == controller?.sessionToken) return
        detachController()
        controller = next
        next?.registerCallback(controllerCallback)
    }

    private fun detachController() {
        controller?.unregisterCallback(controllerCallback)
        controller = null
    }

    private fun publish() {
        val c = controller
        val title = c?.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
        if (c == null || title.isNullOrBlank()) {
            _state.value = null
            return
        }
        val artist = (c.metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: c.metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST))
            ?.takeIf { it.isNotBlank() }
        val playing = c.playbackState?.state == PlaybackState.STATE_PLAYING
        _state.value = NowPlaying(title = title, artist = artist, isPlaying = playing)
    }

    private fun isNotificationAccessGranted(): Boolean {
        val flat = Settings.Secure.getString(
            context.contentResolver, "enabled_notification_listeners"
        ) ?: return false
        return flat.split(':').any {
            ComponentName.unflattenFromString(it)?.packageName == context.packageName
        }
    }
}
