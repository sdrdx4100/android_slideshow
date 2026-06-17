package com.example.slideshowclock.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Single source of truth for [SlideshowSettings]. Reads expose a [Flow] that emits
 * on every change; writes are individual suspend setters so callers only touch the
 * field they mean to change.
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val folderUri = stringPreferencesKey("folder_uri")
        val recursive = booleanPreferencesKey("recursive")
        val intervalSeconds = intPreferencesKey("interval_seconds")
        val shuffle = booleanPreferencesKey("shuffle")
        val transition = stringPreferencesKey("transition")
        val clockType = stringPreferencesKey("clock_type")
        val clockScale = floatPreferencesKey("clock_scale")
        val clockPosition = stringPreferencesKey("clock_position")
        val clockFont = stringPreferencesKey("clock_font")
        val analogStyle = stringPreferencesKey("analog_style")
        val clockColor = stringPreferencesKey("clock_color")
        val use24Hour = booleanPreferencesKey("use_24hour")
        val showSeconds = booleanPreferencesKey("show_seconds")
        val showDate = booleanPreferencesKey("show_date")
        val keepScreenOn = booleanPreferencesKey("keep_screen_on")
        val dimEnabled = booleanPreferencesKey("dim_enabled")
        val brightness = floatPreferencesKey("brightness")
        val burnInProtection = booleanPreferencesKey("burn_in_protection")
    }

    val settings: Flow<SlideshowSettings> = context.dataStore.data.map { p ->
        val defaults = SlideshowSettings()
        SlideshowSettings(
            folderUri = p[Keys.folderUri],
            recursive = p[Keys.recursive] ?: defaults.recursive,
            intervalSeconds = p[Keys.intervalSeconds] ?: defaults.intervalSeconds,
            shuffle = p[Keys.shuffle] ?: defaults.shuffle,
            transition = p[Keys.transition].toEnum(defaults.transition),
            clockType = p[Keys.clockType].toEnum(defaults.clockType),
            clockScale = p[Keys.clockScale] ?: defaults.clockScale,
            clockPosition = p[Keys.clockPosition].toEnum(defaults.clockPosition),
            clockFont = p[Keys.clockFont].toEnum(defaults.clockFont),
            analogStyle = p[Keys.analogStyle].toEnum(defaults.analogStyle),
            clockColor = p[Keys.clockColor].toEnum(defaults.clockColor),
            use24Hour = p[Keys.use24Hour] ?: defaults.use24Hour,
            showSeconds = p[Keys.showSeconds] ?: defaults.showSeconds,
            showDate = p[Keys.showDate] ?: defaults.showDate,
            keepScreenOn = p[Keys.keepScreenOn] ?: defaults.keepScreenOn,
            dimEnabled = p[Keys.dimEnabled] ?: defaults.dimEnabled,
            brightness = p[Keys.brightness] ?: defaults.brightness,
            burnInProtection = p[Keys.burnInProtection] ?: defaults.burnInProtection,
        )
    }

    suspend fun setFolderUri(uri: String?) = context.dataStore.edit {
        if (uri == null) it.remove(Keys.folderUri) else it[Keys.folderUri] = uri
    }

    suspend fun setRecursive(value: Boolean) = context.dataStore.edit { it[Keys.recursive] = value }

    suspend fun setIntervalSeconds(value: Int) = context.dataStore.edit {
        it[Keys.intervalSeconds] = value.coerceIn(
            SlideshowSettings.MIN_INTERVAL, SlideshowSettings.MAX_INTERVAL
        )
    }

    suspend fun setShuffle(value: Boolean) = context.dataStore.edit { it[Keys.shuffle] = value }

    suspend fun setTransition(value: TransitionType) = context.dataStore.edit {
        it[Keys.transition] = value.name
    }

    suspend fun setClockType(value: ClockType) = context.dataStore.edit {
        it[Keys.clockType] = value.name
    }

    suspend fun setClockScale(value: Float) = context.dataStore.edit {
        it[Keys.clockScale] = value.coerceIn(
            SlideshowSettings.MIN_SCALE, SlideshowSettings.MAX_SCALE
        )
    }

    suspend fun setClockPosition(value: ClockPosition) = context.dataStore.edit {
        it[Keys.clockPosition] = value.name
    }

    suspend fun setClockFont(value: ClockFont) = context.dataStore.edit {
        it[Keys.clockFont] = value.name
    }

    suspend fun setAnalogStyle(value: AnalogStyle) = context.dataStore.edit {
        it[Keys.analogStyle] = value.name
    }

    suspend fun setClockColor(value: ClockColor) = context.dataStore.edit {
        it[Keys.clockColor] = value.name
    }

    suspend fun setUse24Hour(value: Boolean) = context.dataStore.edit { it[Keys.use24Hour] = value }

    suspend fun setShowSeconds(value: Boolean) = context.dataStore.edit { it[Keys.showSeconds] = value }

    suspend fun setShowDate(value: Boolean) = context.dataStore.edit { it[Keys.showDate] = value }

    suspend fun setKeepScreenOn(value: Boolean) = context.dataStore.edit { it[Keys.keepScreenOn] = value }

    suspend fun setDimEnabled(value: Boolean) = context.dataStore.edit { it[Keys.dimEnabled] = value }

    suspend fun setBrightness(value: Float) = context.dataStore.edit {
        it[Keys.brightness] = value.coerceIn(SlideshowSettings.MIN_BRIGHTNESS, 1.0f)
    }

    suspend fun setBurnInProtection(value: Boolean) = context.dataStore.edit {
        it[Keys.burnInProtection] = value
    }
}

/** Parse a stored enum name back to its constant, falling back to [default] on anything unexpected. */
private inline fun <reified T : Enum<T>> String?.toEnum(default: T): T =
    this?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default
