package com.example.slideshowclock.ui.slideshow

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.provider.Settings as AndroidSettings
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.slideshowclock.media.NowPlaying
import kotlin.math.roundToInt
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.slideshowclock.data.SlideshowSettings
import com.example.slideshowclock.data.TransitionType
import com.example.slideshowclock.ui.clock.ClockOverlay
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun SlideshowScreen(
    onOpenSettings: () -> Unit,
    viewModel: SlideshowViewModel = viewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val images by viewModel.images.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val nowPlaying by viewModel.nowPlaying.collectAsStateWithLifecycle()
    val mediaPermission by viewModel.mediaPermission.collectAsStateWithLifecycle()

    val context = LocalContext.current

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? -> if (uri != null) viewModel.setFolder(uri) }

    // Observe/control the foreground media app while this screen is shown. ON_RESUME
    // also picks up notification access the moment it's granted in system settings.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.startNowPlaying()
                Lifecycle.Event.ON_RESUME -> viewModel.refreshNowPlaying()
                Lifecycle.Event.ON_STOP -> viewModel.stopNowPlaying()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Keep the screen awake while the slideshow is showing, if requested.
    val view = LocalView.current
    LaunchedEffect(settings.keepScreenOn) { view.keepScreenOn = settings.keepScreenOn }

    // Optional window-level dimming for night / always-on use. Reset to the system
    // default when leaving the slideshow so Settings (same window) stays readable.
    DisposableEffect(settings.dimEnabled, settings.brightness) {
        val window = (view.context as? Activity)?.window
        window?.let {
            val lp = it.attributes
            lp.screenBrightness = if (settings.dimEnabled) {
                settings.brightness.coerceIn(SlideshowSettings.MIN_BRIGHTNESS, 1f)
            } else {
                WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            }
            it.attributes = lp
        }
        onDispose {
            (view.context as? Activity)?.window?.let {
                val lp = it.attributes
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                it.attributes = lp
            }
        }
    }

    // Immersive, full-bleed photo-frame experience.
    LaunchedEffect(Unit) {
        val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(window, view)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            settings.folderUri == null || (images.isEmpty() && !isLoading) ->
                EmptyState(onPickFolder = { folderPicker.launch(null) }, onOpenSettings = onOpenSettings)

            images.isEmpty() && isLoading ->
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center),
                )

            else ->
                SlideshowContent(
                    images = images,
                    settings = settings,
                    nowPlaying = nowPlaying,
                    mediaPermission = mediaPermission,
                    onMediaPrev = viewModel::mediaPrevious,
                    onMediaPlayPause = viewModel::mediaPlayPause,
                    onMediaNext = viewModel::mediaNext,
                    onRequestMediaPermission = {
                        runCatching {
                            context.startActivity(
                                Intent(AndroidSettings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    },
                    onOpenSettings = onOpenSettings,
                    onReload = viewModel::reload,
                )
        }
    }
}

@Composable
private fun SlideshowContent(
    images: List<Uri>,
    settings: com.example.slideshowclock.data.SlideshowSettings,
    nowPlaying: NowPlaying?,
    mediaPermission: Boolean,
    onMediaPrev: () -> Unit,
    onMediaPlayPause: () -> Unit,
    onMediaNext: () -> Unit,
    onRequestMediaPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onReload: () -> Unit,
) {
    var index by rememberSaveable(images) { mutableStateOf(0) }
    var paused by rememberSaveable { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val audio = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var volume by remember { mutableIntStateOf(audio.getStreamVolume(AudioManager.STREAM_MUSIC)) }
    val setVolumeFraction: (Float) -> Unit = { fraction ->
        val v = (fraction * maxVolume).roundToInt().coerceIn(0, maxVolume)
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, v, 0)
        volume = v
    }

    // Auto-advance.
    LaunchedEffect(images, settings.intervalSeconds, paused) {
        if (images.size <= 1 || paused) return@LaunchedEffect
        while (true) {
            delay(settings.intervalSeconds * 1000L)
            index = (index + 1) % images.size
        }
    }

    // When the controls appear: re-read volume (it may have changed via hardware keys)
    // then auto-hide them a few seconds later.
    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            volume = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
            delay(5000)
            controlsVisible = false
        }
    }

    val current = images[index.coerceIn(0, images.lastIndex)]

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { controlsVisible = !controlsVisible })
            }
    ) {
        SlideStage(
            image = current,
            transition = settings.transition,
            intervalSeconds = settings.intervalSeconds,
        )

        ClockOverlay(settings = settings)

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            TopControls(
                index = index,
                count = images.size,
                paused = paused,
                onTogglePause = { paused = !paused },
                onReload = onReload,
                onOpenSettings = onOpenSettings,
            )
        }

        // Vertical volume control on the right — slides in with the controls.
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn() + slideInHorizontally { it },
            exit = fadeOut() + slideOutHorizontally { it },
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            VolumeColumn(
                fraction = volume / maxVolume.toFloat(),
                onSet = setVolumeFraction,
                accent = settings.clockColor.color,
                modifier = Modifier.padding(end = 16.dp),
            )
        }

        // Now-playing + transport at bottom-left. Visible whenever music is playing,
        // and also surfaced (with the permission prompt) whenever the controls show.
        val nowPlayingVisible = (mediaPermission && nowPlaying != null) || controlsVisible
        AnimatedVisibility(
            visible = nowPlayingVisible,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomStart),
        ) {
            NowPlayingBar(
                state = nowPlaying,
                hasPermission = mediaPermission,
                onPrev = onMediaPrev,
                onPlayPause = onMediaPlayPause,
                onNext = onMediaNext,
                onRequestPermission = onRequestMediaPermission,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(start = 16.dp, bottom = 20.dp),
            )
        }
    }
}

@Composable
private fun VolumeColumn(
    fraction: Float,
    onSet: (Float) -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RoundIcon(Icons.Filled.VolumeUp, "音量を上げる") {
            onSet((fraction + 0.08f).coerceAtMost(1f))
        }
        BoxWithConstraints(
            Modifier
                .size(width = 44.dp, height = 170.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color.Black.copy(alpha = 0.4f))
                .pointerInput(Unit) {
                    detectVerticalDragGestures { change, _ ->
                        onSet((1f - change.position.y / size.height).coerceIn(0f, 1f))
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        onSet((1f - offset.y / size.height).coerceIn(0f, 1f))
                    }
                },
        ) {
            // Fill tinted to match the clock colour, kept soft so it isn't garish.
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(fraction.coerceIn(0f, 1f))
                    .background(
                        Brush.verticalGradient(
                            listOf(accent.copy(alpha = 0.85f), accent.copy(alpha = 0.45f))
                        )
                    ),
            )
        }
        RoundIcon(Icons.Filled.VolumeDown, "音量を下げる") {
            onSet((fraction - 0.08f).coerceAtLeast(0f))
        }
        Text(
            text = "${(fraction * 100).roundToInt()}%",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun NowPlayingBar(
    state: NowPlaying?,
    hasPermission: Boolean,
    onPrev: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .widthIn(max = 280.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.4f))
            .padding(14.dp),
    ) {
        if (!hasPermission) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onRequestPermission),
            ) {
                Icon(Icons.Filled.MusicNote, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "音楽コントロール",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        "タップして通知アクセスを許可",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                    )
                }
            }
        } else {
            Text(
                text = state?.title ?: "再生中の音楽なし",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            state?.artist?.let { artist ->
                Text(
                    text = artist,
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(10.dp))
            val enabled = state != null
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RoundIcon(Icons.Filled.SkipPrevious, "前の曲", enabled, onPrev)
                RoundIcon(
                    if (state?.isPlaying == true) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    "再生 / 一時停止",
                    enabled,
                    onPlayPause,
                )
                RoundIcon(Icons.Filled.SkipNext, "次の曲", enabled, onNext)
            }
        }
    }
}

@Composable
private fun RoundIcon(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = if (enabled) 0.4f else 0.2f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White.copy(alpha = if (enabled) 1f else 0.4f),
        )
    }
}

@Composable
private fun SlideStage(
    image: Uri,
    transition: TransitionType,
    intervalSeconds: Int,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = image,
        transitionSpec = {
            when (transition) {
                TransitionType.FADE, TransitionType.KEN_BURNS ->
                    fadeIn(tween(1100)) togetherWith fadeOut(tween(1100))

                TransitionType.SLIDE ->
                    (slideInHorizontally(tween(700)) { it } + fadeIn(tween(700))) togetherWith
                        (slideOutHorizontally(tween(700)) { -it } + fadeOut(tween(700)))

                TransitionType.NONE ->
                    fadeIn(snap()) togetherWith fadeOut(snap())
            }
        },
        label = "slide",
        modifier = modifier.fillMaxSize(),
    ) { img ->
        if (transition == TransitionType.KEN_BURNS) {
            KenBurnsImage(img, intervalSeconds)
        } else {
            FillImage(img)
        }
    }
}

@Composable
private fun FillImage(uri: Uri, modifier: Modifier = Modifier) {
    AsyncImage(
        model = uri,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.fillMaxSize().background(Color.Black),
    )
}

@Composable
private fun KenBurnsImage(uri: Uri, intervalSeconds: Int) {
    val scale = remember(uri) { Animatable(1.10f) }
    val progress = remember(uri) { Animatable(0f) }
    val direction = remember(uri) { Random.nextInt(4) }

    LaunchedEffect(uri) {
        val durationMs = intervalSeconds * 1000 + 1400
        // scale and pan run together over slightly longer than the slide interval.
        launch { scale.animateTo(1.22f, tween(durationMs, easing = LinearEasing)) }
        progress.animateTo(1f, tween(durationMs, easing = LinearEasing))
    }

    AsyncImage(
        model = uri,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                val pan = 16.dp.toPx() * progress.value
                when (direction) {
                    0 -> translationX = pan
                    1 -> translationX = -pan
                    2 -> translationY = pan
                    else -> translationY = -pan
                }
            },
    )
}

@Composable
private fun TopControls(
    index: Int,
    count: Int,
    paused: Boolean,
    onTogglePause: () -> Unit,
    onReload: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.35f))
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onTogglePause) {
            Icon(
                imageVector = if (paused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                contentDescription = null,
                tint = Color.White,
            )
        }
        Text(
            text = "${index + 1} / $count",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
        Row {
            IconButton(onClick = onReload) {
                Icon(Icons.Filled.Refresh, contentDescription = null, tint = Color.White)
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Settings, contentDescription = null, tint = Color.White)
            }
        }
    }
}

@Composable
private fun EmptyState(
    onPickFolder: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "写真フォルダを選択してください",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "端末内のフォルダを選ぶと、その中の画像を\nスライドショーで表示します。",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(28.dp))
            Button(onClick = onPickFolder) { Text("フォルダを選択") }
            Spacer(Modifier.height(12.dp))
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Settings, contentDescription = null, tint = Color.White)
            }
        }
    }
}
