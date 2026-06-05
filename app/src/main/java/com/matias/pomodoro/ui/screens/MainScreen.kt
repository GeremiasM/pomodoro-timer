package com.matias.pomodoro.ui.screens

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.view.accessibility.AccessibilityManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.matias.pomodoro.R
import com.matias.pomodoro.data.PomodoroSession
import com.matias.pomodoro.data.preferences.PomodoroPreferences
import com.matias.pomodoro.data.preferences.PomodoroSettings
import com.matias.pomodoro.timer.PomodoroPhase
import com.matias.pomodoro.timer.PomodoroTimerState
import com.matias.pomodoro.timer.TimerStatus
import com.matias.pomodoro.ui.theme.LocalPomodoroColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private enum class StatsPeriod(val labelRes: Int) {
    Day(R.string.stats_period_day),
    Week(R.string.stats_period_week),
    Month(R.string.stats_period_month)
}

private data class ThemeOption(
    val key: String,
    val labelRes: Int,
    val previewColors: List<Color>
)

private data class SoundOption(
    val key: String,
    val labelRes: Int,
    val rawRes: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroScreen(
    timerState: PomodoroTimerState,
    settings: PomodoroSettings,
    todayStats: PomodoroSession?,
    dailyGoalProgress: Float,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onSkip: () -> Unit,
    onReset: () -> Unit,
    onOpenStats: () -> Unit,
    onUpdateSettings: (suspend PomodoroPreferences.() -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSettings by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = LocalPomodoroColors.current
    val isRunning = timerState.status == TimerStatus.RUNNING

    Surface(
        modifier = modifier.fillMaxSize(),
        color = colors.background
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            AmbientBackground(
                primary = colors.primary,
                maxWidth = maxWidth,
                maxHeight = maxHeight,
                modifier = Modifier.fillMaxSize()
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(WindowInsets.statusBars.asPaddingValues())
                    .padding(horizontal = 22.dp)
                    .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HeaderActions(
                    onOpenStats = onOpenStats,
                    onOpenSettings = { showSettings = true }
                )

                PhaseIndicator(
                    timerState = timerState,
                    sessionsBeforeLongBreak = settings.sessionsBeforeLongBreak,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.weight(0.28f))

                PhaseAnimatedTimer(
                    timerState = timerState,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(30.dp))

                TodayProgressRow(
                    completedPomodoros = todayStats?.completedPomodoros ?: timerState.completedPomodoros,
                    dailyGoal = settings.dailyGoalPomodoros,
                    progress = dailyGoalProgress,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(28.dp))

                TimerControls(
                    isRunning = isRunning,
                    onReset = onReset,
                    onStart = onStart,
                    onPause = onPause,
                    onSkip = onSkip
                )

                Spacer(Modifier.height(24.dp))

                BottomPhaseLabel(
                    phase = timerState.phase,
                    currentSessionNumber = timerState.currentSessionNumber,
                    sessionsBeforeLongBreak = settings.sessionsBeforeLongBreak
                )
            }
        }
    }

    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            sheetState = sheetState,
            containerColor = colors.surface,
            contentColor = colors.onBackground
        ) {
            SettingsScreen(
                settings = settings,
                onUpdateSettings = onUpdateSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp)
            )
        }
    }
}

@Composable
private fun HeaderActions(
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onOpenStats,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.BarChart,
                contentDescription = stringResource(R.string.stats_content_description),
                tint = LocalPomodoroColors.current.muted
            )
        }
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = stringResource(R.string.settings_content_description),
                tint = LocalPomodoroColors.current.muted
            )
        }
    }
}

@Composable
private fun PhaseIndicator(
    timerState: PomodoroTimerState,
    sessionsBeforeLongBreak: Int,
    modifier: Modifier = Modifier
) {
    val colors = LocalPomodoroColors.current
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "phase_indicator_scale"
    )
    val dotDescriptionFormat = R.string.session_dot_content_description_format
    val filledDots = when (timerState.phase) {
        PomodoroPhase.Work -> timerState.currentSessionNumber - 1
        PomodoroPhase.ShortBreak -> timerState.currentSessionNumber
        PomodoroPhase.LongBreak -> sessionsBeforeLongBreak
    }.coerceIn(0, sessionsBeforeLongBreak)

    Column(
        modifier = modifier.graphicsLayer(scaleX = scale, scaleY = scale),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = phaseLabel(timerState.phase),
            color = colors.primary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 3.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(sessionsBeforeLongBreak) { index ->
                val filled = index < filledDots
                val dotScale by animateFloatAsState(
                    targetValue = if (filled) 1.15f else 0.82f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "session_dot_scale_$index"
                )
                val dotAlpha by animateFloatAsState(
                    targetValue = if (filled) 1f else 0.28f,
                    animationSpec = tween(300),
                    label = "session_dot_alpha_$index"
                )
                val dotDescription = stringResource(dotDescriptionFormat, index + 1, sessionsBeforeLongBreak)
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .graphicsLayer(scaleX = dotScale, scaleY = dotScale, alpha = dotAlpha)
                        .clip(CircleShape)
                        .background(if (filled) colors.primary else colors.primary.copy(alpha = 0.30f))
                        .semantics {
                            contentDescription = dotDescription
                        }
                )
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun PhaseAnimatedTimer(
    timerState: PomodoroTimerState,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = timerState.phase,
        transitionSpec = {
            ContentTransform(
                targetContentEnter = fadeIn(tween(400, delayMillis = 120, easing = EaseInOutCubic)) + scaleIn(tween(400, delayMillis = 120, easing = EaseInOutCubic), initialScale = 0.85f),
                initialContentExit = fadeOut(tween(300, easing = EaseInOutCubic)) + scaleOut(tween(300, easing = EaseInOutCubic), targetScale = 0.85f),
                sizeTransform = SizeTransform(clip = false)
            )
        },
        label = "phase_timer_transition",
        modifier = modifier
    ) {
        CircularPomodoroTimer(
            timerState = timerState,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CircularPomodoroTimer(
    timerState: PomodoroTimerState,
    modifier: Modifier = Modifier
) {
    val colors = LocalPomodoroColors.current
    val animatedProgress by animateFloatAsState(
        targetValue = timerState.progressFraction.coerceIn(0f, 1f),
        animationSpec = tween(850, easing = LinearEasing),
        label = "timer_progress"
    )
    val infiniteTransition = rememberInfiniteTransition(label = "idle_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idle_pulse_scale"
    )
    val completionScale = remember { Animatable(1f) }

    LaunchedEffect(timerState.status, timerState.phase) {
        if (timerState.status == TimerStatus.COMPLETED) {
            completionScale.snapTo(1f)
            completionScale.animateTo(1.05f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            completionScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }

    val timerDescription = stringResource(
        R.string.timer_accessibility_format,
        phaseTitle(timerState.phase),
        formatTime(timerState.remainingSeconds)
    )
    val view = LocalView.current
    val accessibilityManager = LocalContext.current.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val phaseAnnouncement = stringResource(R.string.phase_changed_announcement_format, phaseTitle(timerState.phase))
    LaunchedEffect(timerState.phase) {
        if (accessibilityManager.isEnabled) {
            view.announceForAccessibility(phaseAnnouncement)
        }
    }

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val diameter = maxWidth * 0.72f
        Box(
            modifier = Modifier
                .size(diameter)
                .graphicsLayer(
                    scaleX = (if (timerState.status == TimerStatus.IDLE) pulseScale else 1f) * completionScale.value,
                    scaleY = (if (timerState.status == TimerStatus.IDLE) pulseScale else 1f) * completionScale.value
                )
                .semantics { contentDescription = timerDescription },
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 12.dp.toPx()
                val glowStroke = 24.dp.toPx()
                val arcSize = Size(size.width - glowStroke, size.height - glowStroke)
                val topLeft = Offset(glowStroke / 2f, glowStroke / 2f)
                drawArc(
                    color = colors.track,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                drawArc(
                    color = colors.primary.copy(alpha = 0.30f),
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = glowStroke, cap = StrokeCap.Round),
                    blendMode = BlendMode.Screen
                )
                drawArc(
                    color = colors.primary,
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }

            CompletedParticleBurst(
                trigger = timerState.status == TimerStatus.COMPLETED,
                color = colors.accent,
                modifier = Modifier.fillMaxSize()
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formatTime(timerState.remainingSeconds),
                    color = colors.onBackground,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-1.6).sp
                )
                Text(
                    text = phaseTitle(timerState.phase),
                    color = colors.primary.copy(alpha = 0.60f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}

@Composable
private fun CompletedParticleBurst(
    trigger: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(1f) }
    LaunchedEffect(trigger) {
        if (trigger) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(720, easing = EaseInOutCubic))
        }
    }
    Canvas(modifier) {
        if (progress.value >= 1f) return@Canvas
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxDistance = size.minDimension * 0.40f
        repeat(12) { index ->
            val angle = (Math.PI * 2.0 / 12.0 * index).toFloat()
            val distance = maxDistance * progress.value
            val particleCenter = Offset(
                center.x + cos(angle) * distance,
                center.y + sin(angle) * distance
            )
            drawCircle(
                color = color.copy(alpha = 1f - progress.value),
                radius = 4.dp.toPx() * (1f - progress.value * 0.45f),
                center = particleCenter
            )
        }
    }
}

@Composable
private fun TodayProgressRow(
    completedPomodoros: Int,
    dailyGoal: Int,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val colors = LocalPomodoroColors.current
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(600, easing = EaseInOutCubic),
        label = "daily_goal_progress"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(colors.surface.copy(alpha = 0.72f))
            .border(1.dp, colors.primary.copy(alpha = 0.14f), RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.today_pomodoros_format, completedPomodoros),
            color = colors.onBackground,
            fontSize = 13.sp,
            modifier = Modifier.weight(1.1f)
        )
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .height(8.dp)
                .weight(0.9f)
                .clip(RoundedCornerShape(99.dp)),
            color = colors.primary,
            trackColor = colors.track
        )
        Text(
            text = stringResource(R.string.goal_progress_format, completedPomodoros.coerceAtMost(dailyGoal), dailyGoal),
            color = colors.muted,
            fontSize = 13.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.8f)
        )
    }
}

@Composable
private fun TimerControls(
    isRunning: Boolean,
    onReset: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onSkip: () -> Unit
) {
    val colors = LocalPomodoroColors.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PressScaleIconButton(
            onClick = onReset,
            containerColor = colors.surfaceElevated,
            contentColor = colors.primary,
            size = 56.dp,
            contentDescription = stringResource(R.string.reset_content_description)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
        }
        PressScaleIconButton(
            onClick = if (isRunning) onPause else onStart,
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
            size = 76.dp,
            contentDescription = stringResource(if (isRunning) R.string.pause_content_description else R.string.play_content_description)
        ) {
            Crossfade(targetState = isRunning, label = "play_pause_icon") { running ->
                Icon(
                    imageVector = if (running) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(34.dp)
                )
            }
        }
        PressScaleIconButton(
            onClick = onSkip,
            containerColor = colors.surfaceElevated,
            contentColor = colors.primary,
            size = 56.dp,
            contentDescription = stringResource(R.string.skip_content_description)
        ) {
            Icon(Icons.Default.SkipNext, contentDescription = null)
        }
    }
}

@Composable
private fun PressScaleIconButton(
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    size: Dp,
    contentDescription: String,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "button_press_scale"
    )
    FilledIconButton(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier
            .size(size)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .semantics { this.contentDescription = contentDescription },
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        content()
    }
}

@Composable
private fun BottomPhaseLabel(
    phase: PomodoroPhase,
    currentSessionNumber: Int,
    sessionsBeforeLongBreak: Int
) {
    val colors = LocalPomodoroColors.current
    val nextPhase = when (phase) {
        PomodoroPhase.Work -> if (currentSessionNumber >= sessionsBeforeLongBreak) PomodoroPhase.LongBreak else PomodoroPhase.ShortBreak
        PomodoroPhase.ShortBreak,
        PomodoroPhase.LongBreak -> PomodoroPhase.Work
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = phaseTitle(phase),
            color = colors.onBackground,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.next_phase_format, phaseTitle(nextPhase)),
            color = colors.muted,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun AmbientBackground(
    primary: Color,
    maxWidth: Dp,
    maxHeight: Dp,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "ambient_blob")
    val offsetX by transition.animateFloat(
        initialValue = -0.08f,
        targetValue = 0.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_offset_x"
    )
    val offsetY by transition.animateFloat(
        initialValue = 0.08f,
        targetValue = -0.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_offset_y"
    )
    val density = LocalDensity.current
    Canvas(modifier) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primary.copy(alpha = 0.20f), Color.Transparent),
                center = Offset(widthPx * (0.5f + offsetX), heightPx * (0.42f + offsetY)),
                radius = widthPx * 0.45f
            ),
            radius = widthPx * 0.45f,
            center = Offset(widthPx * (0.5f + offsetX), heightPx * (0.42f + offsetY)),
            alpha = 0.40f
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SettingsScreen(
    settings: PomodoroSettings,
    onUpdateSettings: (suspend PomodoroPreferences.() -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalPomodoroColors.current
    val soundPreviewer = rememberSoundPreviewer()

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            color = colors.onBackground,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold
        )

        SettingsSection(title = stringResource(R.string.settings_timer_durations)) {
            DebouncedDurationSlider(
                label = stringResource(R.string.settings_work_duration),
                value = settings.workDurationMinutes,
                min = 5,
                max = 60,
                step = 5,
                onValueCommitted = { value -> onUpdateSettings { updateWorkDurationMinutes(value) } }
            )
            DebouncedDurationSlider(
                label = stringResource(R.string.settings_short_break),
                value = settings.shortBreakMinutes,
                min = 1,
                max = 15,
                step = 1,
                onValueCommitted = { value -> onUpdateSettings { updateShortBreakMinutes(value) } }
            )
            DebouncedDurationSlider(
                label = stringResource(R.string.settings_long_break),
                value = settings.longBreakMinutes,
                min = 10,
                max = 30,
                step = 5,
                onValueCommitted = { value -> onUpdateSettings { updateLongBreakMinutes(value) } }
            )
            Text(
                text = stringResource(R.string.settings_sessions_before_long_break),
                color = colors.muted,
                fontSize = 13.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                (2..6).forEach { count ->
                    SelectableChip(
                        label = stringResource(R.string.settings_sessions_count_format, count),
                        selected = settings.sessionsBeforeLongBreak == count,
                        onClick = { onUpdateSettings { updateSessionsBeforeLongBreak(count) } }
                    )
                }
            }
        }

        SettingsSection(title = stringResource(R.string.settings_automation)) {
            SettingsSwitchRow(
                text = stringResource(R.string.settings_auto_start_breaks),
                checked = settings.autoStartBreaks,
                onCheckedChange = { onUpdateSettings { updateAutoStartBreaks(it) } }
            )
            SettingsSwitchRow(
                text = stringResource(R.string.settings_auto_start_work),
                checked = settings.autoStartWork,
                onCheckedChange = { onUpdateSettings { updateAutoStartWork(it) } }
            )
        }

        SettingsSection(title = stringResource(R.string.settings_sound_vibration)) {
            SettingsSwitchRow(
                text = stringResource(R.string.settings_sound_enabled),
                checked = settings.soundEnabled,
                onCheckedChange = { onUpdateSettings { updateSoundEnabled(it) } }
            )
            if (settings.soundEnabled) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    soundOptions().forEach { option ->
                        SoundChip(
                            option = option,
                            selected = settings.notificationSound == option.key,
                            onSelect = { onUpdateSettings { updateNotificationSound(option.key) } },
                            onPreview = { soundPreviewer.play(option.key, option.rawRes) }
                        )
                    }
                }
            }
            SettingsSwitchRow(
                text = stringResource(R.string.settings_vibration_enabled),
                checked = settings.vibrationEnabled,
                onCheckedChange = { onUpdateSettings { updateVibrationEnabled(it) } }
            )
        }

        SettingsSection(title = stringResource(R.string.settings_theme)) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                themeOptions().forEach { option ->
                    ThemeCircle(
                        option = option,
                        selected = settings.selectedTheme == option.key,
                        onSelect = { onUpdateSettings { updateSelectedTheme(option.key) } }
                    )
                }
            }
        }

        SettingsSection(title = stringResource(R.string.settings_daily_goal)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { onUpdateSettings { updateDailyGoalPomodoros(settings.dailyGoalPomodoros - 1) } },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.settings_decrease_goal_content_description), tint = colors.primary)
                }
                Text(
                    text = stringResource(R.string.settings_daily_goal_count_format, settings.dailyGoalPomodoros),
                    color = colors.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(
                    onClick = { onUpdateSettings { updateDailyGoalPomodoros(settings.dailyGoalPomodoros + 1) } },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.settings_increase_goal_content_description), tint = colors.primary)
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalPomodoroColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(colors.surfaceElevated.copy(alpha = 0.72f))
            .border(1.dp, colors.primary.copy(alpha = 0.12f), RoundedCornerShape(28.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(title, color = colors.onBackground, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        content()
    }
}

@Composable
private fun DebouncedDurationSlider(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    step: Int,
    onValueCommitted: (Int) -> Unit
) {
    val colors = LocalPomodoroColors.current
    var localValue by remember(value) { mutableIntStateOf(value) }
    LaunchedEffect(localValue) {
        delay(300L)
        if (localValue != value) onValueCommitted(localValue)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = colors.muted, fontSize = 13.sp)
            Text(stringResource(R.string.settings_duration_minutes_format, localValue), color = colors.onBackground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Slider(
            value = localValue.toFloat(),
            onValueChange = { raw -> localValue = ((raw / step).roundToInt() * step).coerceIn(min, max) },
            valueRange = min.toFloat()..max.toFloat(),
            steps = ((max - min) / step - 1).coerceAtLeast(0),
            colors = SliderDefaults.colors(
                thumbColor = colors.primary,
                activeTrackColor = colors.primary,
                inactiveTrackColor = colors.track
            )
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = LocalPomodoroColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text, color = colors.onBackground, fontSize = 14.sp)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalPomodoroColors.current
    Surface(
        modifier = Modifier
            .height(42.dp)
            .clip(RoundedCornerShape(99.dp))
            .clickable(onClick = onClick),
        color = if (selected) colors.primary else colors.surface,
        contentColor = if (selected) colors.onPrimary else colors.onBackground
    ) {
        Box(Modifier.padding(horizontal = 17.dp), contentAlignment = Alignment.Center) {
            Text(label, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SoundChip(
    option: SoundOption,
    selected: Boolean,
    onSelect: () -> Unit,
    onPreview: () -> Unit
) {
    val label = stringResource(option.labelRes)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(if (selected) LocalPomodoroColors.current.primary else LocalPomodoroColors.current.surface)
            .clickable(onClick = onSelect)
            .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, color = if (selected) LocalPomodoroColors.current.onPrimary else LocalPomodoroColors.current.onBackground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        IconButton(onClick = onPreview, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.settings_preview_sound_content_description_format, label), tint = if (selected) LocalPomodoroColors.current.onPrimary else LocalPomodoroColors.current.primary)
        }
    }
}

@Composable
private fun ThemeCircle(
    option: ThemeOption,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val scale by animateFloatAsState(if (selected) 1.15f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "theme_scale")
    val label = stringResource(option.labelRes)
    val themeContentDescription = stringResource(R.string.settings_theme_content_description_format, label)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .graphicsLayer(scaleX = scale, scaleY = scale)
                .clip(CircleShape)
                .background(Brush.linearGradient(option.previewColors))
                .border(2.dp, if (selected) LocalPomodoroColors.current.accent else Color.Transparent, CircleShape)
                .clickable(onClick = onSelect)
                .semantics { contentDescription = themeContentDescription },
            contentAlignment = Alignment.Center
        ) {
            if (selected) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
        }
        Text(label, color = LocalPomodoroColors.current.muted, fontSize = 12.sp)
    }
}

@Composable
fun PomodoroStatsScreen(
    settings: PomodoroSettings,
    todayStats: PomodoroSession?,
    weekStats: List<PomodoroSession>,
    monthStats: List<PomodoroSession>,
    dailyGoalProgress: Float,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalPomodoroColors.current
    var selectedPeriod by remember { mutableStateOf(StatsPeriod.Day) }
    val sessions = when (selectedPeriod) {
        StatsPeriod.Day -> listOfNotNull(todayStats)
        StatsPeriod.Week -> weekStats
        StatsPeriod.Month -> monthStats
    }

    BackHandler(onBack = onBack)

    Surface(modifier.fillMaxSize(), color = colors.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.statusBars.asPaddingValues())
                .padding(horizontal = 22.dp)
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 18.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.stats_back_content_description), tint = colors.muted)
                }
                Text(stringResource(R.string.stats_title), color = colors.onBackground, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Spacer(Modifier.width(48.dp))
            }

            DailyGoalRing(progress = dailyGoalProgress, completed = todayStats?.completedPomodoros ?: 0, goal = settings.dailyGoalPomodoros)

            TabRow(selectedTabIndex = selectedPeriod.ordinal, containerColor = colors.surface, contentColor = colors.primary) {
                StatsPeriod.entries.forEach { period ->
                    Tab(
                        selected = selectedPeriod == period,
                        onClick = { selectedPeriod = period },
                        text = { Text(stringResource(period.labelRes)) }
                    )
                }
            }

            if (sessions.isEmpty()) {
                EmptyStatsState()
            } else {
                SummaryGrid(sessions = sessions)
                PomodoroBarChart(sessions = sessions, title = stringResource(R.string.stats_pomodoros_completed_chart))
                FocusLineChart(sessions = sessions, title = stringResource(R.string.stats_focus_minutes_chart))
            }
        }
    }
}

@Composable
private fun DailyGoalRing(progress: Float, completed: Int, goal: Int) {
    val colors = LocalPomodoroColors.current
    val animatedProgress by animateFloatAsState(progress.coerceIn(0f, 1f), tween(900, easing = EaseInOutCubic), label = "stats_goal_ring")
    Card(colors = CardDefaults.cardColors(containerColor = colors.surface.copy(alpha = 0.76f)), shape = RoundedCornerShape(30.dp)) {
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Canvas(Modifier.size(104.dp)) {
                val stroke = 14.dp.toPx()
                val sizeArc = Size(size.width - stroke, size.height - stroke)
                val topLeft = Offset(stroke / 2f, stroke / 2f)
                drawArc(colors.track, -90f, 360f, false, topLeft, sizeArc, style = Stroke(stroke, cap = StrokeCap.Round))
                drawArc(colors.primary, -90f, animatedProgress * 360f, false, topLeft, sizeArc, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(stringResource(R.string.stats_goal_ring_title), color = colors.muted, fontSize = 13.sp)
                Text(stringResource(R.string.goal_progress_format, completed.coerceAtMost(goal), goal), color = colors.onBackground, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SummaryGrid(sessions: List<PomodoroSession>) {
    val totalPomodoros = sessions.sumOf { it.completedPomodoros }
    val dailyAverage = if (sessions.isEmpty()) 0f else totalPomodoros.toFloat() / sessions.size.toFloat()
    val totalFocusSeconds = sessions.sumOf { it.totalFocusSeconds }
    val best = sessions.maxByOrNull { it.completedPomodoros }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard(stringResource(R.string.stats_total_pomodoros), stringResource(R.string.stats_count_format, totalPomodoros), Modifier.weight(1f))
            SummaryCard(stringResource(R.string.stats_daily_average), stringResource(R.string.stats_decimal_format, dailyAverage), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard(stringResource(R.string.stats_total_focus_time), formatDuration(totalFocusSeconds), Modifier.weight(1f))
            SummaryCard(stringResource(R.string.stats_best_day), best?.let { stringResource(R.string.stats_best_day_format, it.date, it.completedPomodoros) } ?: stringResource(R.string.stats_no_best_day), Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryCard(title: String, value: String, modifier: Modifier = Modifier) {
    val colors = LocalPomodoroColors.current
    Card(
        modifier = modifier.widthIn(min = 150.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface.copy(alpha = 0.76f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = colors.muted, fontSize = 12.sp)
            Text(value, color = colors.onBackground, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun PomodoroBarChart(sessions: List<PomodoroSession>, title: String) {
    ChartCard(title = title) {
        val primary = LocalPomodoroColors.current.primary
        AndroidView(
            modifier = Modifier.fillMaxWidth().height(220.dp),
            factory = { context -> BarChart(context).apply { styleBaseChart(primary) } },
            update = { chart ->
                val entries = sessions.mapIndexed { index, session -> BarEntry(index.toFloat(), session.completedPomodoros.toFloat()) }
                val dataSet = BarDataSet(entries, title).apply {
                    color = primary.toArgbCompat()
                    valueTextColor = Color.Transparent.toArgbCompat()
                    setDrawValues(false)
                }
                chart.data = BarData(dataSet).apply { barWidth = 0.48f }
                chart.invalidate()
            }
        )
    }
}

@Composable
private fun FocusLineChart(sessions: List<PomodoroSession>, title: String) {
    ChartCard(title = title) {
        val accent = LocalPomodoroColors.current.accent
        AndroidView(
            modifier = Modifier.fillMaxWidth().height(220.dp),
            factory = { context -> LineChart(context).apply { styleBaseChart(accent) } },
            update = { chart ->
                val entries = sessions.mapIndexed { index, session -> Entry(index.toFloat(), session.totalFocusSeconds / 60f) }
                val dataSet = LineDataSet(entries, title).apply {
                    color = accent.toArgbCompat()
                    setCircleColor(accent.toArgbCompat())
                    lineWidth = 3f
                    circleRadius = 4f
                    mode = LineDataSet.Mode.CUBIC_BEZIER
                    valueTextColor = Color.Transparent.toArgbCompat()
                    setDrawValues(false)
                }
                chart.data = LineData(dataSet)
                chart.invalidate()
            }
        )
    }
}

@Composable
private fun ChartCard(title: String, content: @Composable () -> Unit) {
    val colors = LocalPomodoroColors.current
    Card(colors = CardDefaults.cardColors(containerColor = colors.surface.copy(alpha = 0.76f)), shape = RoundedCornerShape(28.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, color = colors.onBackground, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun EmptyStatsState() {
    val colors = LocalPomodoroColors.current
    Box(
        modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(28.dp)).background(colors.surface),
        contentAlignment = Alignment.Center
    ) {
        Text(stringResource(R.string.stats_empty_message), color = colors.muted, textAlign = TextAlign.Center, modifier = Modifier.padding(24.dp))
    }
}

private fun BarChart.styleBaseChart(color: Color) {
    description.isEnabled = false
    legend.isEnabled = false
    setDrawGridBackground(false)
    setDrawBorders(false)
    axisRight.isEnabled = false
    axisLeft.setDrawGridLines(false)
    axisLeft.textColor = color.copy(alpha = 0.70f).toArgbCompat()
    xAxis.position = XAxis.XAxisPosition.BOTTOM
    xAxis.setDrawGridLines(false)
    xAxis.textColor = color.copy(alpha = 0.70f).toArgbCompat()
    setTouchEnabled(false)
    setBackgroundColor(Color.Transparent.toArgbCompat())
}

private fun LineChart.styleBaseChart(color: Color) {
    description.isEnabled = false
    legend.isEnabled = false
    setDrawGridBackground(false)
    setDrawBorders(false)
    axisRight.isEnabled = false
    axisLeft.setDrawGridLines(false)
    axisLeft.textColor = color.copy(alpha = 0.70f).toArgbCompat()
    xAxis.position = XAxis.XAxisPosition.BOTTOM
    xAxis.setDrawGridLines(false)
    xAxis.textColor = color.copy(alpha = 0.70f).toArgbCompat()
    setTouchEnabled(false)
    setBackgroundColor(Color.Transparent.toArgbCompat())
}

private fun themeOptions(): List<ThemeOption> = listOf(
    ThemeOption("emerald", R.string.theme_emerald, listOf(Color(0xFF00C896), Color(0xFF00FFB2))),
    ThemeOption("sunset", R.string.theme_sunset, listOf(Color(0xFFE05C3A), Color(0xFFFF9966))),
    ThemeOption("ocean", R.string.theme_ocean, listOf(Color(0xFF3A7BD5), Color(0xFF80B4FF))),
    ThemeOption("forest", R.string.theme_forest, listOf(Color(0xFF5AAE3A), Color(0xFFA0FF70))),
    ThemeOption("lavender", R.string.theme_lavender, listOf(Color(0xFF8A5AE0), Color(0xFFC4A0FF)))
)

private fun soundOptions(): List<SoundOption> = listOf(
    SoundOption("bell", R.string.settings_sound_bell, R.raw.bell),
    SoundOption("digital", R.string.settings_sound_digital, R.raw.digital),
    SoundOption("soft", R.string.settings_sound_soft, R.raw.soft)
)

@Composable
private fun rememberSoundPreviewer(): SoundPreviewer {
    val context = LocalContext.current
    val previewer = remember { SoundPreviewer(context.applicationContext) }
    DisposableEffect(Unit) { onDispose { previewer.release() } }
    return previewer
}

private class SoundPreviewer(context: Context) {
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(1)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()
    private val ids = mutableMapOf<String, Int>()
    private val appContext = context.applicationContext

    fun play(key: String, rawRes: Int) {
        val id = ids.getOrPut(key) { soundPool.load(appContext, rawRes, 1) }
        soundPool.play(id, 1f, 1f, 1, 0, 1f)
    }

    fun release() {
        soundPool.release()
    }
}

@Composable
private fun phaseLabel(phase: PomodoroPhase): String = when (phase) {
    PomodoroPhase.Work -> stringResource(R.string.phase_focus)
    PomodoroPhase.ShortBreak -> stringResource(R.string.phase_short_break)
    PomodoroPhase.LongBreak -> stringResource(R.string.phase_long_break)
}

@Composable
private fun phaseTitle(phase: PomodoroPhase): String = when (phase) {
    PomodoroPhase.Work -> stringResource(R.string.phase_focus_title)
    PomodoroPhase.ShortBreak -> stringResource(R.string.phase_short_break_title)
    PomodoroPhase.LongBreak -> stringResource(R.string.phase_long_break_title)
}

@Composable
private fun formatDuration(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return if (hours > 0) stringResource(R.string.stats_hours_minutes_format, hours, minutes) else stringResource(R.string.stats_minutes_format, minutes)
}

@Composable
private fun formatTime(totalSeconds: Int): String {
    val safeSeconds = totalSeconds.coerceAtLeast(0)
    val minutes = safeSeconds / 60
    val seconds = safeSeconds % 60
    return stringResource(R.string.time_minutes_seconds_format, minutes, seconds)
}

private fun Color.toArgbCompat(): Int = android.graphics.Color.argb(
    (alpha * 255).roundToInt().coerceIn(0, 255),
    (red * 255).roundToInt().coerceIn(0, 255),
    (green * 255).roundToInt().coerceIn(0, 255),
    (blue * 255).roundToInt().coerceIn(0, 255)
)
