package com.example.slideshowclock.ui.clock

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.example.slideshowclock.data.AnalogStyle
import com.example.slideshowclock.data.ClockFont
import com.example.slideshowclock.data.ClockPosition
import com.example.slideshowclock.data.ClockType
import com.example.slideshowclock.data.SlideshowSettings
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** A soft drop shadow that keeps light text legible over bright photos. */
private val LegibilityShadow = Shadow(
    color = Color.Black.copy(alpha = 0.6f),
    offset = Offset(0f, 2f),
    blurRadius = 18f,
)

/**
 * Emits the current time, re-aligning each update to the next second (or minute)
 * boundary so the displayed value never lags.
 */
@Composable
fun rememberCurrentTime(tickSeconds: Boolean): State<LocalDateTime> =
    produceState(initialValue = LocalDateTime.now(), tickSeconds) {
        while (true) {
            value = LocalDateTime.now()
            val nowMs = System.currentTimeMillis()
            val delayMs = if (tickSeconds) 1000L - (nowMs % 1000L) else 60_000L - (nowMs % 60_000L)
            delay(delayMs.coerceAtLeast(1L))
        }
    }

/**
 * Draws the configured clock + date, scaled and aligned per [settings], filling its
 * parent. Reused both full-screen in the slideshow and inside the Settings preview.
 */
@Composable
fun ClockOverlay(
    settings: SlideshowSettings,
    modifier: Modifier = Modifier,
    edgePadding: Dp = 32.dp,
) {
    val showClock = settings.clockType != ClockType.NONE
    if (!showClock && !settings.showDate) return

    val tickSeconds = settings.clockType == ClockType.ANALOG ||
        (settings.clockType == ClockType.DIGITAL && settings.showSeconds)
    val now by rememberCurrentTime(tickSeconds)

    val color = settings.clockColor.color
    val fontFamily = settings.clockFont.toFontFamily()
    val fontWeight = settings.clockFont.toWeight()
    // Slowly drift the overlay (once a minute) so a static clock can't burn into an OLED.
    val (shiftX, shiftY) = if (settings.burnInProtection) burnInShift(now) else (0.dp to 0.dp)

    Box(modifier.fillMaxSize().padding(edgePadding)) {
        val pos = settings.clockPosition
        Column(
            modifier = Modifier
                .align(pos.alignment)
                .offset(x = shiftX, y = shiftY),
            horizontalAlignment = pos.horizontalAlignment(),
        ) {
            when (settings.clockType) {
                ClockType.DIGITAL -> DigitalClock(
                    time = now,
                    use24Hour = settings.use24Hour,
                    showSeconds = settings.showSeconds,
                    scale = settings.clockScale,
                    fontFamily = fontFamily,
                    fontWeight = fontWeight,
                    color = color,
                )
                ClockType.ANALOG -> AnalogClock(
                    time = now.toLocalTime(),
                    scale = settings.clockScale,
                    showSeconds = settings.showSeconds,
                    style = settings.analogStyle,
                    accent = color,
                )
                ClockType.NONE -> Unit
            }
            if (settings.showDate) {
                if (showClock) Spacer(Modifier.height((12 * settings.clockScale).dp))
                DateLabel(
                    date = now.toLocalDate(),
                    scale = settings.clockScale,
                    fontFamily = fontFamily,
                    color = color,
                )
            }
        }
    }
}

@Composable
fun DigitalClock(
    time: LocalDateTime,
    use24Hour: Boolean,
    showSeconds: Boolean,
    scale: Float,
    fontFamily: FontFamily,
    fontWeight: FontWeight,
    color: Color,
) {
    val pattern = buildString {
        append(if (use24Hour) "HH:mm" else "h:mm")
        if (showSeconds) append(":ss")
    }
    val timeText = time.format(DateTimeFormatter.ofPattern(pattern))
    val amPm = if (!use24Hour) time.format(DateTimeFormatter.ofPattern("a", Locale.US)) else null

    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = timeText,
            style = TextStyle(
                color = color,
                fontSize = (88 * scale).sp,
                fontFamily = fontFamily,
                fontWeight = fontWeight,
                letterSpacing = (-1).sp,
                shadow = LegibilityShadow,
            ),
        )
        if (amPm != null) {
            Spacer(Modifier.width((8 * scale).dp))
            Text(
                text = amPm,
                style = TextStyle(
                    color = color.copy(alpha = 0.85f),
                    fontSize = (24 * scale).sp,
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Medium,
                    shadow = LegibilityShadow,
                ),
                modifier = Modifier.padding(bottom = (14 * scale).dp),
            )
        }
    }
}

@Composable
fun DateLabel(
    date: LocalDate,
    scale: Float,
    fontFamily: FontFamily,
    color: Color,
) {
    val text = date.format(DateTimeFormatter.ofPattern("M月d日 (E)", Locale.JAPANESE))
    Text(
        text = text,
        textAlign = TextAlign.Center,
        style = TextStyle(
            color = color.copy(alpha = 0.88f),
            fontSize = (22 * scale).sp,
            fontFamily = fontFamily,
            fontWeight = FontWeight.Light,
            letterSpacing = 1.sp,
            shadow = LegibilityShadow,
        ),
    )
}

@Composable
fun AnalogClock(
    time: LocalTime,
    scale: Float,
    showSeconds: Boolean,
    style: AnalogStyle,
    accent: Color,
) {
    val diameter = (220 * scale).dp

    Canvas(modifier = Modifier.size(diameter)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = min(size.width, size.height) / 2f

        // Subtle dark disc so hands stay visible over bright photos.
        drawCircle(color = Color.Black.copy(alpha = 0.22f), radius = radius, center = center)
        if (style != AnalogStyle.MODERN) {
            drawCircle(
                color = Color.White.copy(alpha = 0.55f),
                radius = radius * 0.97f,
                center = center,
                style = Stroke(width = radius * 0.012f),
            )
        }

        when (style) {
            AnalogStyle.MINIMAL -> drawMinuteTicks(center, radius)
            AnalogStyle.CLASSIC -> drawHourNumerals(center, radius)
            AnalogStyle.MODERN -> drawCardinalDots(center, radius, accent)
        }

        val hour = time.hour % 12
        val minute = time.minute
        val second = time.second

        fun hand(angleDeg: Double, length: Float, stroke: Float, color: Color) {
            val rad = Math.toRadians(angleDeg)
            val end = center + Offset(sin(rad).toFloat() * length, -cos(rad).toFloat() * length)
            drawLine(
                color = color,
                start = center,
                end = end,
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }

        // MODERN draws the main hands in the accent color; others keep them white
        // and reserve the accent for the second hand and hub.
        val handColor = if (style == AnalogStyle.MODERN) accent else Color.White
        val secondColor = if (style == AnalogStyle.MODERN) Color.White else accent

        hand((hour + minute / 60.0) * 30.0, radius * 0.50f, radius * 0.055f, handColor)
        hand((minute + second / 60.0) * 6.0, radius * 0.74f, radius * 0.040f, handColor)
        if (showSeconds) {
            hand(second * 6.0, radius * 0.82f, radius * 0.018f, secondColor)
        }

        drawCircle(color = accent, radius = radius * 0.045f, center = center)
        drawCircle(color = Color.White, radius = radius * 0.018f, center = center)
    }
}

/** 60 ticks, every 5th emphasised — the clean default face. */
private fun DrawScope.drawMinuteTicks(center: Offset, radius: Float) {
    for (i in 0 until 60) {
        val major = i % 5 == 0
        val angle = Math.toRadians(i * 6.0)
        val outer = radius * 0.95f
        val inner = outer - if (major) radius * 0.10f else radius * 0.05f
        val dx = sin(angle).toFloat()
        val dy = -cos(angle).toFloat()
        drawLine(
            color = Color.White.copy(alpha = if (major) 0.9f else 0.32f),
            start = center + Offset(dx * inner, dy * inner),
            end = center + Offset(dx * outer, dy * outer),
            strokeWidth = if (major) radius * 0.018f else radius * 0.009f,
            cap = StrokeCap.Round,
        )
    }
}

/** Painted 1–12 numerals for a traditional face. */
private fun DrawScope.drawHourNumerals(center: Offset, radius: Float) {
    val paint = Paint().apply {
        isAntiAlias = true
        color = Color.White.toArgb()
        textAlign = Paint.Align.CENTER
        textSize = radius * 0.20f
        typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
    }
    val ring = radius * 0.78f
    val baseline = (paint.descent() + paint.ascent()) / 2f // vertical centring offset
    drawContext.canvas.nativeCanvas.let { canvas ->
        for (n in 1..12) {
            val angle = Math.toRadians(n * 30.0)
            val x = center.x + sin(angle).toFloat() * ring
            val y = center.y - cos(angle).toFloat() * ring
            canvas.drawText(n.toString(), x, y - baseline, paint)
        }
    }
}

/** Four accent dots at 12/3/6/9 for a minimalist modern face. */
private fun DrawScope.drawCardinalDots(center: Offset, radius: Float, accent: Color) {
    for (i in 0 until 4) {
        val angle = Math.toRadians(i * 90.0)
        val r = radius * 0.84f
        val p = center + Offset(sin(angle).toFloat() * r, -cos(angle).toFloat() * r)
        drawCircle(color = accent, radius = radius * 0.03f, center = p)
    }
}

/** Built-in typeface for the digital clock + date. */
private fun ClockFont.toFontFamily(): FontFamily = when (this) {
    ClockFont.MINIMAL -> FontFamily.SansSerif
    ClockFont.BOLD -> FontFamily.SansSerif
    ClockFont.SERIF -> FontFamily.Serif
    ClockFont.MONO -> FontFamily.Monospace
}

private fun ClockFont.toWeight(): FontWeight = when (this) {
    ClockFont.MINIMAL -> FontWeight.Light
    ClockFont.BOLD -> FontWeight.Bold
    ClockFont.SERIF -> FontWeight.Normal
    ClockFont.MONO -> FontWeight.Medium
}

/**
 * A tiny offset that walks slowly around a small circle, advancing one step per
 * minute, so a long-running clock never sits on the exact same pixels.
 */
private fun burnInShift(now: LocalDateTime): Pair<Dp, Dp> {
    val steps = 12
    val idx = (now.hour * 60 + now.minute) % steps
    val angle = Math.toRadians(idx * (360.0 / steps))
    val r = 10f
    return (sin(angle).toFloat() * r).dp to (-cos(angle).toFloat() * r).dp
}

/** Horizontal alignment of the stacked clock/date that matches the grid position. */
private fun ClockPosition.horizontalAlignment(): Alignment.Horizontal = when (this) {
    ClockPosition.TOP_START, ClockPosition.CENTER_START, ClockPosition.BOTTOM_START ->
        Alignment.Start
    ClockPosition.TOP_END, ClockPosition.CENTER_END, ClockPosition.BOTTOM_END ->
        Alignment.End
    else -> Alignment.CenterHorizontally
}
