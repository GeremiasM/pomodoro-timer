package com.matias.pomodoro.ui.screens

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.view.accessibility.AccessibilityManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.filled.Close
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.matias.pomodoro.analytics.AnalyticsManager
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
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private enum class StatsPeriod(val labelRes: Int, val analyticsValue: String) {
    Day(R.string.stats_tab_day, "day"),
    Week(R.string.stats_tab_week, "week"),
    Month(R.string.stats_tab_month, "month")
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
    featureStatsEnabled: Boolean,
    featureDailyGoalEnabled: Boolean,
    motdText: String,
    motdEnabled: Boolean,
    dismissedMotdText: String,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onSkip: () -> Unit,
    onReset: () -> Unit,
    onOpenStats: () -> Unit,
    onDismissMotd: (String) -> Unit,
    onUpdateSettings: (String, String, suspend PomodoroPreferences.() -> Unit) -> Unit,
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
                    onOpenSettings = { showSettings = true },
                    featureStatsEnabled = featureStatsEnabled
                )

                MotdBanner(
                    text = motdText,
                    visible = motdEnabled && motdText.isNotBlank() && dismissedMotdText != motdText,
                    onDismiss = { onDismissMotd(motdText) },
                    modifier = Modifier.fillMaxWidth()
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

                if (featureDailyGoalEnabled) {
                    TodayProgressRow(
                        completedPomodoros = todayStats?.completedPomodoros ?: timerState.completedPomodoros,
                        dailyGoal = settings.dailyGoalPomodoros,
                        progress = dailyGoalProgress,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(28.dp))
                }

                TimerControls(
                    timerStatus = timerState.status,
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
                featureDailyGoalEnabled = featureDailyGoalEnabled,
                onBack = { showSettings = false },
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
    onOpenSettings: () -> Unit,
    featureStatsEnabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (featureStatsEnabled) {
            IconButton(
                onClick = onOpenStats,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = stringResource(R.string.pomodoro_cd_stats),
                    tint = LocalPomodoroColors.current.muted
                )
            }
        }
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = stringResource(R.string.pomodoro_cd_settings),
                tint = LocalPomodoroColors.current.muted
            )
        }
    }
}

@Composable
private fun MotdBanner(
    text: String,
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalPomodoroColors.current
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(tween(250)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .background(colors.primary.copy(alpha = 0.20f))
                .padding(start = 16.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = text,
                color = colors.onBackground,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.motd_dismiss_cd),
                    tint = colors.onBackground
                )
            }
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
    val dotDescriptionFormat = R.string.pomodoro_session_label
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
    ) { targetPhase ->
        CircularPomodoroTimer(
            timerState = timerState.copy(phase = targetPhase),
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

    val timerDescription = stringResource(R.string.pomodoro_cd_timer_circle, formatTime(timerState.remainingSeconds))
    val view = LocalView.current
    val accessibilityManager = LocalContext.current.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val phaseAnnouncement = phaseTitle(timerState.phase)
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
            text = stringResource(R.string.pomodoro_today_count, completedPomodoros),
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
            text = stringResource(R.string.pomodoro_goal_label, completedPomodoros.coerceAtMost(dailyGoal), dailyGoal),
            color = colors.muted,
            fontSize = 13.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.8f)
        )
    }
}

@Composable
private fun TimerControls(
    timerStatus: TimerStatus,
    onReset: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onSkip: () -> Unit
) {
    val colors = LocalPomodoroColors.current
    val isRunning = timerStatus == TimerStatus.RUNNING
    val primaryActionDescription = when (timerStatus) {
        TimerStatus.RUNNING -> stringResource(R.string.pomodoro_btn_pause)
        TimerStatus.PAUSED -> stringResource(R.string.pomodoro_btn_resume)
        TimerStatus.IDLE,
        TimerStatus.COMPLETED -> stringResource(R.string.pomodoro_btn_start)
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PressScaleIconButton(
            onClick = onReset,
            containerColor = colors.surfaceElevated,
            contentColor = colors.primary,
            size = 56.dp,
            contentDescription = stringResource(R.string.pomodoro_btn_reset)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
        }
        PressScaleIconButton(
            onClick = if (isRunning) onPause else onStart,
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
            size = 76.dp,
            contentDescription = primaryActionDescription
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
            contentDescription = stringResource(R.string.pomodoro_btn_skip)
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
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = phaseTitle(phase),
            color = colors.onBackground,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.pomodoro_session_label, currentSessionNumber, sessionsBeforeLongBreak),
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
    featureDailyGoalEnabled: Boolean,
    onBack: () -> Unit,
    onUpdateSettings: (String, String, suspend PomodoroPreferences.() -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalPomodoroColors.current
    val soundPreviewer = rememberSoundPreviewer()

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.stats_back_cd),
                    tint = colors.muted
                )
            }
            Text(
                text = stringResource(R.string.settings_title),
                color = colors.onBackground,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.width(48.dp))
        }

        SettingsSection(title = stringResource(R.string.settings_section_timer)) {
            DebouncedDurationSlider(
                label = stringResource(R.string.settings_work_duration),
                value = settings.workDurationMinutes,
                min = 5,
                max = 60,
                step = 5,
                onValueCommitted = { value ->
                    onUpdateSettings("work_duration_minutes", value.toString()) { updateWorkDurationMinutes(value) }
                }
            )
            DebouncedDurationSlider(
                label = stringResource(R.string.settings_short_break),
                value = settings.shortBreakMinutes,
                min = 1,
                max = 15,
                step = 1,
                onValueCommitted = { value ->
                    onUpdateSettings("short_break_minutes", value.toString()) { updateShortBreakMinutes(value) }
                }
            )
            DebouncedDurationSlider(
                label = stringResource(R.string.settings_long_break),
                value = settings.longBreakMinutes,
                min = 10,
                max = 30,
                step = 5,
                onValueCommitted = { value ->
                    onUpdateSettings("long_break_minutes", value.toString()) { updateLongBreakMinutes(value) }
                }
            )
            Text(
                text = stringResource(R.string.settings_sessions_before_long),
                color = colors.muted,
                fontSize = 13.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                (2..6).forEach { count ->
                    SelectableChip(
                        label = count.toString(),
                        selected = settings.sessionsBeforeLongBreak == count,
                        onClick = {
                            onUpdateSettings("sessions_before_long_break", count.toString()) {
                                updateSessionsBeforeLongBreak(count)
                            }
                        }
                    )
                }
            }
        }

        SettingsSection(title = stringResource(R.string.settings_section_automation)) {
            SettingsSwitchRow(
                text = stringResource(R.string.settings_auto_start_breaks),
                checked = settings.autoStartBreaks,
                onCheckedChange = { value ->
                    onUpdateSettings("auto_start_breaks", value.toString()) { updateAutoStartBreaks(value) }
                }
            )
            SettingsSwitchRow(
                text = stringResource(R.string.settings_auto_start_work),
                checked = settings.autoStartWork,
                onCheckedChange = { value ->
                    onUpdateSettings("auto_start_work", value.toString()) { updateAutoStartWork(value) }
                }
            )
        }

        SettingsSection(title = stringResource(R.string.settings_section_sound)) {
            SettingsSwitchRow(
                text = stringResource(R.string.settings_sound_enabled),
                checked = settings.soundEnabled,
                onCheckedChange = { value ->
                    onUpdateSettings("sound_enabled", value.toString()) { updateSoundEnabled(value) }
                }
            )
            if (settings.soundEnabled) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    soundOptions().forEach { option ->
                        SoundChip(
                            option = option,
                            selected = settings.notificationSound == option.key,
                            onSelect = {
                                onUpdateSettings("notification_sound", option.key) {
                                    updateNotificationSound(option.key)
                                }
                            },
                            onPreview = { soundPreviewer.play(option.key, option.rawRes) }
                        )
                    }
                }
            }
            SettingsSwitchRow(
                text = stringResource(R.string.settings_vibration_enabled),
                checked = settings.vibrationEnabled,
                onCheckedChange = { value ->
                    onUpdateSettings("vibration_enabled", value.toString()) { updateVibrationEnabled(value) }
                }
            )
        }

        SettingsSection(title = stringResource(R.string.settings_section_theme)) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                themeOptions().forEach { option ->
                    ThemeCircle(
                        option = option,
                        selected = settings.selectedTheme == option.key,
                        onSelect = {
                            onUpdateSettings("selected_theme", option.key) {
                                updateSelectedTheme(option.key)
                            }
                        }
                    )
                }
            }
        }

        if (featureDailyGoalEnabled) {
            SettingsSection(title = stringResource(R.string.settings_section_goal)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = {
                            val value = settings.dailyGoalPomodoros - 1
                            onUpdateSettings("daily_goal_pomodoros", value.toString()) {
                                updateDailyGoalPomodoros(value)
                            }
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.settings_daily_goal), tint = colors.primary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.settings_daily_goal),
                            color = colors.muted,
                            fontSize = 12.sp
                        )
                        Text(
                            text = settings.dailyGoalPomodoros.toString(),
                            color = colors.onBackground,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(
                        onClick = {
                            val value = settings.dailyGoalPomodoros + 1
                            onUpdateSettings("daily_goal_pomodoros", value.toString()) {
                                updateDailyGoalPomodoros(value)
                            }
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.settings_daily_goal), tint = colors.primary)
                    }
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
            Text(stringResource(R.string.settings_minutes_suffix, localValue), color = colors.onBackground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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
            Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.settings_sound_preview), tint = if (selected) LocalPomodoroColors.current.onPrimary else LocalPomodoroColors.current.primary)
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
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .graphicsLayer(scaleX = scale, scaleY = scale)
                .clip(CircleShape)
                .background(Brush.linearGradient(option.previewColors))
                .border(2.dp, if (selected) LocalPomodoroColors.current.accent else Color.Transparent, CircleShape)
                .clickable(onClick = onSelect)
                .semantics { contentDescription = label },
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
    featureDailyGoalEnabled: Boolean,
    adBannerEnabled: Boolean,
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
    LaunchedEffect(selectedPeriod) {
        AnalyticsManager.logStatsViewed(selectedPeriod.analyticsValue)
    }

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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.stats_back_cd), tint = colors.muted)
                }
                Text(stringResource(R.string.stats_title), color = colors.onBackground, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Spacer(Modifier.width(48.dp))
            }

            if (featureDailyGoalEnabled) {
                DailyGoalRing(progress = dailyGoalProgress, completed = todayStats?.completedPomodoros ?: 0, goal = settings.dailyGoalPomodoros)
            }

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
                PomodoroBarChart(
                    sessions = sessions,
                    period = selectedPeriod,
                    title = stringResource(R.string.stats_chart_pomodoros_title),
                    label = stringResource(R.string.stats_chart_pomodoros_label)
                )
                FocusLineChart(
                    sessions = sessions,
                    period = selectedPeriod,
                    title = stringResource(R.string.stats_chart_focus_title),
                    label = stringResource(R.string.stats_chart_focus_label)
                )
            }

            if (adBannerEnabled) {
                BannerAd(modifier = Modifier.fillMaxWidth())
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
                Text(stringResource(R.string.stats_goal_ring_label), color = colors.muted, fontSize = 13.sp)
                Text(stringResource(R.string.pomodoro_goal_label, completed.coerceAtMost(goal), goal), color = colors.onBackground, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
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
            SummaryCard(stringResource(R.string.stats_total_pomodoros), totalPomodoros.toString(), Modifier.weight(1f))
            SummaryCard(
                stringResource(R.string.stats_daily_average),
                String.format(Locale.getDefault(), "%.1f", dailyAverage),
                Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard(stringResource(R.string.stats_total_focus_time), formatDuration(totalFocusSeconds), Modifier.weight(1f))
            SummaryCard(stringResource(R.string.stats_best_day), bestDayText(best), Modifier.weight(1f))
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
private fun PomodoroBarChart(
    sessions: List<PomodoroSession>,
    period: StatsPeriod,
    title: String,
    label: String
) {
    ChartCard(title = title) {
        val primary = LocalPomodoroColors.current.primary
        val labels = chartLabels(sessions, period)
        AndroidView(
            modifier = Modifier.fillMaxWidth().height(220.dp),
            factory = { context -> BarChart(context).apply { styleBaseChart(primary) } },
            update = { chart ->
                val entries = sessions.mapIndexed { index, session -> BarEntry(index.toFloat(), session.completedPomodoros.toFloat()) }
                val dataSet = BarDataSet(entries, label).apply {
                    color = primary.toArgbCompat()
                    valueTextColor = Color.Transparent.toArgbCompat()
                    setDrawValues(false)
                }
                chart.data = BarData(dataSet).apply { barWidth = 0.48f }
                chart.xAxis.valueFormatter = IndexedLabelFormatter(labels)
                chart.invalidate()
            }
        )
    }
}

@Composable
private fun FocusLineChart(
    sessions: List<PomodoroSession>,
    period: StatsPeriod,
    title: String,
    label: String
) {
    ChartCard(title = title) {
        val accent = LocalPomodoroColors.current.accent
        val labels = chartLabels(sessions, period)
        AndroidView(
            modifier = Modifier.fillMaxWidth().height(220.dp),
            factory = { context -> LineChart(context).apply { styleBaseChart(accent) } },
            update = { chart ->
                val entries = sessions.mapIndexed { index, session -> Entry(index.toFloat(), session.totalFocusSeconds / 60f) }
                val dataSet = LineDataSet(entries, label).apply {
                    color = accent.toArgbCompat()
                    setCircleColor(accent.toArgbCompat())
                    lineWidth = 3f
                    circleRadius = 4f
                    mode = LineDataSet.Mode.CUBIC_BEZIER
                    valueTextColor = Color.Transparent.toArgbCompat()
                    setDrawValues(false)
                }
                chart.data = LineData(dataSet)
                chart.xAxis.valueFormatter = IndexedLabelFormatter(labels)
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

@Composable
private fun BannerAd(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier) {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val adWidth = maxWidth.value.roundToInt().coerceAtLeast(1)
        val adSize = remember(context, adWidth) {
            AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)
        }
        val adView = remember(context, adWidth) {
            AdView(context).apply {
                adUnitId = BANNER_AD_UNIT_ID
                setAdSize(adSize)
            }
        }

        LaunchedEffect(adView) {
            adView.loadAd(AdRequest.Builder().build())
        }
        DisposableEffect(lifecycleOwner, adView) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> adView.resume()
                    Lifecycle.Event.ON_PAUSE -> adView.pause()
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                adView.destroy()
            }
        }

        AndroidView(
            modifier = Modifier.fillMaxWidth().height(adSize.height.dp),
            factory = { adView }
        )
    }
}

@Composable
private fun bestDayText(session: PomodoroSession?): String {
    if (session == null) return "0"
    val date = parseSessionDate(session.date) ?: return session.completedPomodoros.toString()
    return stringResource(
        R.string.stats_best_day_format,
        monthLabel(date.monthValue),
        date.dayOfMonth,
        session.completedPomodoros
    )
}

@Composable
private fun chartLabels(sessions: List<PomodoroSession>, period: StatsPeriod): List<String> {
    return sessions.map { session ->
        val date = parseSessionDate(session.date)
        when {
            date == null -> session.completedPomodoros.toString()
            period == StatsPeriod.Month -> monthLabel(date.monthValue)
            else -> dayLabel(date.dayOfWeek.value)
        }
    }
}

@Composable
private fun dayLabel(dayOfWeekValue: Int): String {
    val labelRes = when (dayOfWeekValue) {
        1 -> R.string.day_label_mon
        2 -> R.string.day_label_tue
        3 -> R.string.day_label_wed
        4 -> R.string.day_label_thu
        5 -> R.string.day_label_fri
        6 -> R.string.day_label_sat
        else -> R.string.day_label_sun
    }
    return stringResource(labelRes)
}

@Composable
private fun monthLabel(monthValue: Int): String {
    val labelRes = when (monthValue) {
        1 -> R.string.month_label_jan
        2 -> R.string.month_label_feb
        3 -> R.string.month_label_mar
        4 -> R.string.month_label_apr
        5 -> R.string.month_label_may
        6 -> R.string.month_label_jun
        7 -> R.string.month_label_jul
        8 -> R.string.month_label_aug
        9 -> R.string.month_label_sep
        10 -> R.string.month_label_oct
        11 -> R.string.month_label_nov
        else -> R.string.month_label_dec
    }
    return stringResource(labelRes)
}

private fun parseSessionDate(date: String): LocalDate? {
    return try {
        LocalDate.parse(date)
    } catch (_: DateTimeParseException) {
        null
    }
}

private class IndexedLabelFormatter(private val labels: List<String>) : ValueFormatter() {
    override fun getFormattedValue(value: Float): String {
        return labels.getOrNull(value.roundToInt()).orEmpty()
    }
}

private const val BANNER_AD_UNIT_ID = "ca-app-pub-4712794855635991/4313841604"

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
    ThemeOption("emerald", R.string.settings_theme_emerald, listOf(Color(0xFF00C896), Color(0xFF00FFB2))),
    ThemeOption("sunset", R.string.settings_theme_sunset, listOf(Color(0xFFE05C3A), Color(0xFFFF9966))),
    ThemeOption("ocean", R.string.settings_theme_ocean, listOf(Color(0xFF3A7BD5), Color(0xFF80B4FF))),
    ThemeOption("forest", R.string.settings_theme_forest, listOf(Color(0xFF5AAE3A), Color(0xFFA0FF70))),
    ThemeOption("lavender", R.string.settings_theme_lavender, listOf(Color(0xFF8A5AE0), Color(0xFFC4A0FF)))
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
    PomodoroPhase.Work -> stringResource(R.string.pomodoro_phase_work)
    PomodoroPhase.ShortBreak -> stringResource(R.string.pomodoro_phase_short_break)
    PomodoroPhase.LongBreak -> stringResource(R.string.pomodoro_phase_long_break)
}

@Composable
private fun phaseTitle(phase: PomodoroPhase): String = when (phase) {
    PomodoroPhase.Work -> stringResource(R.string.pomodoro_phase_work)
    PomodoroPhase.ShortBreak -> stringResource(R.string.pomodoro_phase_short_break)
    PomodoroPhase.LongBreak -> stringResource(R.string.pomodoro_phase_long_break)
}

@Composable
private fun formatDuration(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return stringResource(R.string.stats_focus_time_format, hours, minutes)
}

@Composable
private fun formatTime(totalSeconds: Int): String {
    val safeSeconds = totalSeconds.coerceAtLeast(0)
    val minutes = safeSeconds / 60
    val seconds = safeSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

private fun Color.toArgbCompat(): Int = android.graphics.Color.argb(
    (alpha * 255).roundToInt().coerceIn(0, 255),
    (red * 255).roundToInt().coerceIn(0, 255),
    (green * 255).roundToInt().coerceIn(0, 255),
    (blue * 255).roundToInt().coerceIn(0, 255)
)
