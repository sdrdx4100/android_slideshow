package com.example.slideshowclock.ui.slideshow

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.slideshowclock.data.SettingsRepository
import com.example.slideshowclock.data.SlideshowSettings
import com.example.slideshowclock.media.ImageRepository
import com.example.slideshowclock.media.NowPlayingController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SlideshowViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = SettingsRepository(app)
    private val imageRepo = ImageRepository(app)
    private val nowPlayingController = NowPlayingController(app)

    val settings: StateFlow<SlideshowSettings> =
        repo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, SlideshowSettings())

    /** Currently playing track in another app (null if none / no permission). */
    val nowPlaying = nowPlayingController.state
    val mediaPermission = nowPlayingController.hasPermission

    private val _images = MutableStateFlow<List<Uri>>(emptyList())
    val images: StateFlow<List<Uri>> = _images.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        // Reload images whenever the folder, recursion, or shuffle flag changes.
        viewModelScope.launch {
            repo.settings
                .map { Triple(it.folderUri, it.shuffle, it.recursive) }
                .distinctUntilChanged()
                .collectLatest { (uri, shuffle, recursive) -> loadImages(uri, shuffle, recursive) }
        }
    }

    private suspend fun loadImages(uriString: String?, shuffle: Boolean, recursive: Boolean) {
        if (uriString == null) {
            _images.value = emptyList()
            return
        }
        _isLoading.value = true
        val list = runCatching {
            imageRepo.listImages(Uri.parse(uriString), recursive)
        }.getOrDefault(emptyList())
        _images.value = if (shuffle) list.shuffled() else list
        _isLoading.value = false
    }

    /** Persist access to the picked folder and remember it. */
    fun setFolder(uri: Uri) {
        imageRepo.persistPermission(uri)
        viewModelScope.launch { repo.setFolderUri(uri.toString()) }
    }

    /** Re-scan the current folder (e.g. after adding photos to it). */
    fun reload() {
        val current = settings.value
        viewModelScope.launch { loadImages(current.folderUri, current.shuffle, current.recursive) }
    }

    // ----- Media control of the currently playing audio app -----
    fun startNowPlaying() = nowPlayingController.start()
    fun stopNowPlaying() = nowPlayingController.stop()
    fun refreshNowPlaying() = nowPlayingController.refresh()
    fun mediaPlayPause() = nowPlayingController.playPause()
    fun mediaNext() = nowPlayingController.next()
    fun mediaPrevious() = nowPlayingController.previous()

    override fun onCleared() {
        nowPlayingController.stop()
    }
}
