package com.example.slideshowclock.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.slideshowclock.data.AnalogStyle
import com.example.slideshowclock.data.ClockColor
import com.example.slideshowclock.data.ClockFont
import com.example.slideshowclock.data.ClockPosition
import com.example.slideshowclock.data.ClockType
import com.example.slideshowclock.data.SettingsRepository
import com.example.slideshowclock.data.SlideshowSettings
import com.example.slideshowclock.data.TransitionType
import com.example.slideshowclock.media.ImageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = SettingsRepository(app)
    private val imageRepo = ImageRepository(app)

    val settings: StateFlow<SlideshowSettings> =
        repo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, SlideshowSettings())

    private val _imageCount = MutableStateFlow(0)
    val imageCount: StateFlow<Int> = _imageCount.asStateFlow()

    init {
        viewModelScope.launch {
            repo.settings
                .map { it.folderUri to it.recursive }
                .distinctUntilChanged()
                .collectLatest { (uri, recursive) -> _imageCount.value = countImages(uri, recursive) }
        }
    }

    private suspend fun countImages(uriString: String?, recursive: Boolean): Int =
        if (uriString == null) 0
        else runCatching { imageRepo.listImages(Uri.parse(uriString), recursive).size }.getOrDefault(0)

    /** Pure helper used by the UI to show a friendly folder name. */
    fun folderDisplayName(uriString: String): String? =
        imageRepo.folderDisplayName(Uri.parse(uriString))

    fun setFolder(uri: Uri) {
        imageRepo.persistPermission(uri)
        viewModelScope.launch { repo.setFolderUri(uri.toString()) }
    }

    fun reloadCount() {
        viewModelScope.launch {
            _imageCount.value = countImages(settings.value.folderUri, settings.value.recursive)
        }
    }

    fun setRecursive(value: Boolean) = viewModelScope.launch { repo.setRecursive(value) }
    fun setInterval(seconds: Int) = viewModelScope.launch { repo.setIntervalSeconds(seconds) }
    fun setShuffle(value: Boolean) = viewModelScope.launch { repo.setShuffle(value) }
    fun setTransition(value: TransitionType) = viewModelScope.launch { repo.setTransition(value) }
    fun setClockType(value: ClockType) = viewModelScope.launch { repo.setClockType(value) }
    fun setClockScale(value: Float) = viewModelScope.launch { repo.setClockScale(value) }
    fun setClockPosition(value: ClockPosition) = viewModelScope.launch { repo.setClockPosition(value) }
    fun setClockFont(value: ClockFont) = viewModelScope.launch { repo.setClockFont(value) }
    fun setAnalogStyle(value: AnalogStyle) = viewModelScope.launch { repo.setAnalogStyle(value) }
    fun setClockColor(value: ClockColor) = viewModelScope.launch { repo.setClockColor(value) }
    fun setUse24Hour(value: Boolean) = viewModelScope.launch { repo.setUse24Hour(value) }
    fun setShowSeconds(value: Boolean) = viewModelScope.launch { repo.setShowSeconds(value) }
    fun setShowDate(value: Boolean) = viewModelScope.launch { repo.setShowDate(value) }
    fun setKeepScreenOn(value: Boolean) = viewModelScope.launch { repo.setKeepScreenOn(value) }
    fun setDimEnabled(value: Boolean) = viewModelScope.launch { repo.setDimEnabled(value) }
    fun setBrightness(value: Float) = viewModelScope.launch { repo.setBrightness(value) }
    fun setBurnInProtection(value: Boolean) = viewModelScope.launch { repo.setBurnInProtection(value) }
}
