package com.example.slideshowclock.data

import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color

/** Which clock face (if any) is drawn over the slideshow. */
enum class ClockType { DIGITAL, ANALOG, NONE }

/** How one image gives way to the next. */
enum class TransitionType { FADE, SLIDE, KEN_BURNS, NONE }

/** Typeface family for the digital clock + date. Uses built-in families (no bundled fonts). */
enum class ClockFont { MINIMAL, BOLD, SERIF, MONO }

/** Face design for the analog clock. */
enum class AnalogStyle { MINIMAL, CLASSIC, MODERN }

/** Tint applied to the clock (digital text / analog hands & accents). */
enum class ClockColor(val color: Color) {
    WHITE(Color(0xFFFFFFFF)),
    BLUE(Color(0xFF7AA2F7)),
    AMBER(Color(0xFFFFC766)),
    MINT(Color(0xFF7EE0B8)),
    ROSE(Color(0xFFFF8FA3)),
}

/** Nine-point grid position for the clock overlay. */
enum class ClockPosition(val alignment: Alignment) {
    TOP_START(Alignment.TopStart),
    TOP_CENTER(Alignment.TopCenter),
    TOP_END(Alignment.TopEnd),
    CENTER_START(Alignment.CenterStart),
    CENTER(Alignment.Center),
    CENTER_END(Alignment.CenterEnd),
    BOTTOM_START(Alignment.BottomStart),
    BOTTOM_CENTER(Alignment.BottomCenter),
    BOTTOM_END(Alignment.BottomEnd),
}

/**
 * The full, persisted configuration of the app. Backed by DataStore
 * (see [SettingsRepository]). Defaults are chosen so a freshly installed app
 * behaves like a pleasant photo-frame clock the moment a folder is picked.
 */
data class SlideshowSettings(
    val folderUri: String? = null,
    val recursive: Boolean = false,
    val intervalSeconds: Int = 8,
    val shuffle: Boolean = true,
    val transition: TransitionType = TransitionType.KEN_BURNS,
    val clockType: ClockType = ClockType.DIGITAL,
    val clockScale: Float = 1.0f,
    val clockPosition: ClockPosition = ClockPosition.BOTTOM_CENTER,
    val clockFont: ClockFont = ClockFont.MINIMAL,
    val analogStyle: AnalogStyle = AnalogStyle.MINIMAL,
    val clockColor: ClockColor = ClockColor.WHITE,
    val use24Hour: Boolean = true,
    val showSeconds: Boolean = false,
    val showDate: Boolean = true,
    val keepScreenOn: Boolean = true,
    // Always-on / photo-frame tuning.
    val dimEnabled: Boolean = false,
    val brightness: Float = 1.0f,
    val burnInProtection: Boolean = false,
) {
    companion object {
        const val MIN_INTERVAL = 3
        const val MAX_INTERVAL = 60
        const val MIN_SCALE = 0.5f
        const val MAX_SCALE = 2.5f
        const val MIN_BRIGHTNESS = 0.05f
    }
}
