@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.slideshowclock.ui.settings

import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.slideshowclock.data.AnalogStyle
import com.example.slideshowclock.data.ClockColor
import com.example.slideshowclock.data.ClockFont
import com.example.slideshowclock.data.ClockPosition
import com.example.slideshowclock.data.ClockType
import com.example.slideshowclock.data.MediaPosition
import com.example.slideshowclock.data.SlideshowSettings
import com.example.slideshowclock.data.TransitionType
import com.example.slideshowclock.ui.clock.ClockOverlay
import com.example.slideshowclock.ui.theme.Accent
import com.example.slideshowclock.ui.theme.Ink
import com.example.slideshowclock.ui.theme.Outline
import com.example.slideshowclock.ui.theme.Surface1
import com.example.slideshowclock.ui.theme.Surface2
import com.example.slideshowclock.ui.theme.TextHigh
import com.example.slideshowclock.ui.theme.TextMid
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val imageCount by viewModel.imageCount.collectAsStateWithLifecycle()

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? -> if (uri != null) viewModel.setFolder(uri) }

    // Hoisted so the size slider updates the live preview while dragging.
    var scaleLocal by remember(settings.clockScale) { mutableFloatStateOf(settings.clockScale) }
    var intervalLocal by remember(settings.intervalSeconds) {
        mutableFloatStateOf(settings.intervalSeconds.toFloat())
    }
    var brightnessLocal by remember(settings.brightness) {
        mutableFloatStateOf(settings.brightness)
    }
    val previewSettings = settings.copy(clockScale = scaleLocal)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("設定") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Ink,
                    titleContentColor = TextHigh,
                    navigationIconContentColor = TextHigh,
                ),
            )
        },
        containerColor = Ink,
    ) { padding ->
        val isLandscape =
            LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
        Column(Modifier.padding(padding)) {
            // Pinned so the live preview stays visible while the options below scroll.
            // Landscape is short on height, so there we shrink it to a font/colour swatch
            // (full layout/position is best judged on the real slideshow screen).
            if (isLandscape) {
                CompactPreview(
                    previewSettings,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
                )
            } else {
                PreviewCard(
                    previewSettings,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
                )
            }
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {

            // ----- Image source -----
            SectionCard("画像ソース") {
                val uri = settings.folderUri
                val name = uri?.let { viewModel.folderDisplayName(it) }
                Text(
                    text = "現在のフォルダ",
                    color = TextMid,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = name ?: "未選択",
                    color = TextHigh,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
                if (uri != null) {
                    Text("$imageCount 枚の画像", color = Accent, fontSize = 13.sp)
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { folderPicker.launch(null) }) {
                        Icon(Icons.Filled.FolderOpen, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("フォルダを選択")
                    }
                    OutlinedButton(onClick = { viewModel.reloadCount() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("再読み込み")
                    }
                }
                Spacer(Modifier.height(4.dp))
                SwitchRow(
                    label = "サブフォルダも含める",
                    checked = settings.recursive,
                    onCheckedChange = viewModel::setRecursive,
                )
                Text(
                    "選んだフォルダ内の下位フォルダの画像もすべて読み込みます。",
                    color = TextMid,
                    fontSize = 12.sp,
                )
            }

            // ----- Slideshow -----
            SectionCard("スライドショー") {
                LabeledSlider(
                    label = "表示間隔",
                    valueText = "${intervalLocal.roundToInt()} 秒",
                    value = intervalLocal,
                    range = SlideshowSettings.MIN_INTERVAL.toFloat()..SlideshowSettings.MAX_INTERVAL.toFloat(),
                    onValueChange = { intervalLocal = it },
                    onCommit = { viewModel.setInterval(intervalLocal.roundToInt()) },
                )
                Spacer(Modifier.height(8.dp))
                SwitchRow(
                    label = "シャッフル再生",
                    checked = settings.shuffle,
                    onCheckedChange = viewModel::setShuffle,
                )
                Spacer(Modifier.height(12.dp))
                Text("切り替え効果", color = TextMid, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                ChoiceChips(
                    options = TransitionType.entries,
                    selected = settings.transition,
                    label = ::transitionLabel,
                    onSelect = viewModel::setTransition,
                )
            }

            // ----- Clock -----
            SectionCard("時計") {
                Text("時計の種類", color = TextMid, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                ChoiceChips(
                    options = ClockType.entries,
                    selected = settings.clockType,
                    label = ::clockTypeLabel,
                    onSelect = viewModel::setClockType,
                )
                if (settings.clockType == ClockType.ANALOG) {
                    Spacer(Modifier.height(16.dp))
                    Text("アナログのデザイン", color = TextMid, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    ChoiceChips(
                        options = AnalogStyle.entries,
                        selected = settings.analogStyle,
                        label = ::analogStyleLabel,
                        onSelect = viewModel::setAnalogStyle,
                    )
                }
                if (settings.clockType != ClockType.NONE || settings.showDate) {
                    Spacer(Modifier.height(16.dp))
                    Text("フォント", color = TextMid, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    ChoiceChips(
                        options = ClockFont.entries,
                        selected = settings.clockFont,
                        label = ::clockFontLabel,
                        onSelect = viewModel::setClockFont,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("カラー", color = TextMid, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    ColorChips(
                        selected = settings.clockColor,
                        onSelect = viewModel::setClockColor,
                    )
                }
                Spacer(Modifier.height(16.dp))
                LabeledSlider(
                    label = "時計のサイズ",
                    valueText = "${(scaleLocal * 100).roundToInt()} %",
                    value = scaleLocal,
                    range = SlideshowSettings.MIN_SCALE..SlideshowSettings.MAX_SCALE,
                    onValueChange = { scaleLocal = it },
                    onCommit = { viewModel.setClockScale(scaleLocal) },
                )
                Spacer(Modifier.height(16.dp))
                Text("時計の位置", color = TextMid, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                PositionGrid(
                    selected = settings.clockPosition,
                    onSelect = viewModel::setClockPosition,
                )
                Spacer(Modifier.height(12.dp))
                SwitchRow(
                    label = "24時間表示",
                    checked = settings.use24Hour,
                    onCheckedChange = viewModel::setUse24Hour,
                )
                SwitchRow(
                    label = "秒を表示",
                    checked = settings.showSeconds,
                    onCheckedChange = viewModel::setShowSeconds,
                )
                SwitchRow(
                    label = "日付・曜日を表示",
                    checked = settings.showDate,
                    onCheckedChange = viewModel::setShowDate,
                )
            }

            // ----- Always-on / display -----
            SectionCard("常時表示") {
                SwitchRow(
                    label = "画面を常時点灯",
                    checked = settings.keepScreenOn,
                    onCheckedChange = viewModel::setKeepScreenOn,
                )
                Text(
                    "スライドショー中に画面を消灯しません。",
                    color = TextMid,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(12.dp))
                SwitchRow(
                    label = "明るさを下げる",
                    checked = settings.dimEnabled,
                    onCheckedChange = viewModel::setDimEnabled,
                )
                Text(
                    "夜間や常時表示向けに画面を暗くします。",
                    color = TextMid,
                    fontSize = 12.sp,
                )
                if (settings.dimEnabled) {
                    Spacer(Modifier.height(8.dp))
                    LabeledSlider(
                        label = "明るさ",
                        valueText = "${(brightnessLocal * 100).roundToInt()} %",
                        value = brightnessLocal,
                        range = SlideshowSettings.MIN_BRIGHTNESS..1.0f,
                        onValueChange = { brightnessLocal = it },
                        onCommit = { viewModel.setBrightness(brightnessLocal) },
                    )
                }
                Spacer(Modifier.height(12.dp))
                SwitchRow(
                    label = "焼き付き防止",
                    checked = settings.burnInProtection,
                    onCheckedChange = viewModel::setBurnInProtection,
                )
                Text(
                    "時計の位置を少しずつ動かし、有機ELの焼き付きを防ぎます。",
                    color = TextMid,
                    fontSize = 12.sp,
                )
            }

            // ----- Music -----
            SectionCard("音楽") {
                Text("音楽コントロールの位置", color = TextMid, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                ChoiceChips(
                    options = MediaPosition.entries,
                    selected = settings.mediaPosition,
                    label = ::mediaPositionLabel,
                    onSelect = viewModel::setMediaPosition,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "再生中の曲と操作ボタンを表示する位置です。時計と重なるときに変更してください。",
                    color = TextMid,
                    fontSize = 12.sp,
                )
            }

            Spacer(Modifier.height(24.dp))
            }
        }
    }
}

/** Short landscape preview: a centred, small clock just to confirm font + colour. */
@Composable
private fun CompactPreview(settings: SlideshowSettings, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF1B2A3A), Color(0xFF2C3E50), Color(0xFF20303D))
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        ClockOverlay(
            settings = settings.copy(clockScale = 0.4f, clockPosition = ClockPosition.CENTER),
            edgePadding = 8.dp,
        )
    }
}

@Composable
private fun PreviewCard(settings: SlideshowSettings, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text("プレビュー", color = TextMid, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF1B2A3A), Color(0xFF2C3E50), Color(0xFF20303D))
                    )
                ),
        ) {
            ClockOverlay(settings = settings, edgePadding = 16.dp)
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(bottom = 16.dp)) {
        Text(
            text = title,
            color = Accent,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Surface1)
                .padding(16.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onCommit: () -> Unit,
) {
    Column {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, color = TextHigh, fontSize = 16.sp)
            Text(valueText, color = Accent, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
        androidx.compose.material3.Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            onValueChangeFinished = onCommit,
        )
    }
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = TextHigh, fontSize = 16.sp)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun <T> ChoiceChips(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(label(option)) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Surface2,
                    labelColor = TextMid,
                    selectedContainerColor = Accent,
                    selectedLabelColor = Ink,
                ),
            )
        }
    }
}

@Composable
private fun ColorChips(
    selected: ClockColor,
    onSelect: (ClockColor) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ClockColor.entries.forEach { c ->
            val isSelected = c == selected
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Surface2)
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) Accent else Outline,
                        shape = CircleShape,
                    )
                    .clickable { onSelect(c) },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(c.color),
                )
            }
        }
    }
}

@Composable
private fun PositionGrid(
    selected: ClockPosition,
    onSelect: (ClockPosition) -> Unit,
) {
    val rows = listOf(
        listOf(ClockPosition.TOP_START, ClockPosition.TOP_CENTER, ClockPosition.TOP_END),
        listOf(ClockPosition.CENTER_START, ClockPosition.CENTER, ClockPosition.CENTER_END),
        listOf(ClockPosition.BOTTOM_START, ClockPosition.BOTTOM_CENTER, ClockPosition.BOTTOM_END),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { pos ->
                    val isSelected = pos == selected
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Accent else Surface2)
                            .clickable { onSelect(pos) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Ink else Outline),
                        )
                    }
                }
            }
        }
    }
}

private fun transitionLabel(t: TransitionType): String = when (t) {
    TransitionType.FADE -> "フェード"
    TransitionType.SLIDE -> "スライド"
    TransitionType.KEN_BURNS -> "Ken Burns"
    TransitionType.NONE -> "なし"
}

private fun clockTypeLabel(t: ClockType): String = when (t) {
    ClockType.DIGITAL -> "デジタル"
    ClockType.ANALOG -> "アナログ"
    ClockType.NONE -> "なし"
}

private fun analogStyleLabel(s: AnalogStyle): String = when (s) {
    AnalogStyle.MINIMAL -> "ミニマル"
    AnalogStyle.CLASSIC -> "クラシック"
    AnalogStyle.MODERN -> "モダン"
}

private fun clockFontLabel(f: ClockFont): String = when (f) {
    ClockFont.MINIMAL -> "標準"
    ClockFont.BOLD -> "ボールド"
    ClockFont.SERIF -> "明朝"
    ClockFont.MONO -> "等幅"
}

private fun mediaPositionLabel(p: MediaPosition): String = when (p) {
    MediaPosition.BOTTOM_START -> "左下"
    MediaPosition.BOTTOM_END -> "右下"
}
