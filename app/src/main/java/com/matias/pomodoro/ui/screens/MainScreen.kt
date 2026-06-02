package com.matias.pomodoro.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalUriHandler
import com.matias.pomodoro.R
import com.matias.pomodoro.graphics.ColorScheme
import com.matias.pomodoro.graphics.LavaLampView
import com.matias.pomodoro.graphics.LavaMode
import com.matias.pomodoro.ui.theme.PomodoroColors
import com.matias.pomodoro.viewmodel.PomodoroIntent
import com.matias.pomodoro.viewmodel.PomodoroUiState
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────────────────────────────────────
// PANTALLA PRINCIPAL
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MainScreen(
    uiState: PomodoroUiState,
    onIntent: (PomodoroIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val uriHandler = LocalUriHandler.current
    var showPalette by remember { mutableStateOf(false) }
    var showFocusModes by remember { mutableStateOf(false) }

    var showEyeWellnessInfo by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Header ──────────────────────────────────────────────────────
            PomodoroHeader()

            // ── Preview de la lámpara de lava ────────────────────────────────
            LavaPreviewSection(
                uiState  = uiState,
                onIntent = onIntent,
                onTogglePalette = { showPalette = !showPalette },
                onToggleFocusModes = { showFocusModes = !showFocusModes }
            )

            Spacer(modifier = Modifier.height(20.dp))
            
            // ── Configuración ────────────────────────────────────────────────
            SettingsSection(
                uiState  = uiState,
                onIntent = onIntent,
                showPalette = showPalette,
                onColorSelected = { showPalette = false },
                showFocusModes = showFocusModes,
                onFocusModeSelected = { showFocusModes = false }
            )
            Spacer(modifier = Modifier.height(24.dp))

            // ── Estadísticas de sesión ────────────────────────────────────────
            if (uiState.isTimerRunning || uiState.completedBreaks > 0) {
                SessionStatsRow(completedBreaks = uiState.completedBreaks)
                Spacer(modifier = Modifier.height(24.dp))
            }


            Spacer(modifier = Modifier.height(24.dp))

            EyeWellnessCard(
                onClick = { showEyeWellnessInfo = true }
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            PomodoroProCard(
                onClick = {
                    uriHandler.openUri("https://play.google.com/store/apps/details?id=com.matias.pomodoro")
                }
            )

            Spacer(modifier = Modifier.height(48.dp))

        }

        if (showEyeWellnessInfo) {
            EyeWellnessDialog(
                onDismiss = { showEyeWellnessInfo = false }
            )
        }
        // ── Pantalla completa de la lámpara (overlay) ────────────────────────
        AnimatedVisibility(
            visible = uiState.isFullscreen,
            enter   = fadeIn() + expandIn(expandFrom = Alignment.Center),
            exit    = fadeOut() + shrinkOut(shrinkTowards = Alignment.Center)
        ) {
            // Estado local para mostrar/ocultar controles en pantalla completa
            var showControls by remember { mutableStateOf(true) }

            // Temporizador para ocultar controles tras 3 segundos de inactividad
            LaunchedEffect(showControls, uiState.isFullscreen) {
                if (showControls && uiState.isFullscreen) {
                    delay(3000L)
                    showControls = false
                }
            }

            // Manejar botón atrás para salir de pantalla completa
            BackHandler(enabled = uiState.isFullscreen) {
                onIntent(PomodoroIntent.ToggleFullscreen)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        showControls = true
                    }
            ) {
                LavaLampView(
                    mode        = uiState.lavaMode,
                    colorScheme = uiState.colorScheme,
                    modifier    = Modifier.fillMaxSize()
                )

                // Botón para salir de pantalla completa (con su propia animación)
                AnimatedVisibility(
                    visible = showControls,
                    enter   = fadeIn() + slideInVertically { it / 2 },
                    exit    = fadeOut() + slideOutVertically { it / 2 },
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    IconButton(
                        onClick = { onIntent(PomodoroIntent.ToggleFullscreen) },
                        modifier = Modifier
                            .padding(24.dp)
                            .size(56.dp)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(
                            imageVector        = Icons.Default.FullscreenExit,
                            contentDescription = stringResource(R.string.main_exit_fullscreen_content_description),
                            tint               = Color.White,
                            modifier           = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }

        // ── Pantalla de descanso activo (overlay) ────────────────────────────
        AnimatedVisibility(
            visible = uiState.isRestScreenVisible,
            enter   = fadeIn() + scaleIn(initialScale = 0.95f),
            exit    = fadeOut() + scaleOut(targetScale = 0.95f)
        ) {
            RestScreen(
                uiState  = uiState,
                onIntent = onIntent
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SECCIONES
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun lavaModeLabel(mode: LavaMode): String = when (mode) {
    LavaMode.LAVA                 -> stringResource(R.string.focus_mode_lava)
    LavaMode.FOCUS_POINT_INFINITY -> stringResource(R.string.focus_mode_infinity)
    LavaMode.FOCUS_POINT_CIRCLE   -> stringResource(R.string.focus_mode_circular)
}

@Composable
private fun colorSchemeLabel(scheme: ColorScheme): String = when (scheme) {
    ColorScheme.AMBER_MOSS        -> stringResource(R.string.color_amber)
    ColorScheme.TERRACOTTA_SAGE   -> stringResource(R.string.color_terra)
    ColorScheme.DUSK              -> stringResource(R.string.color_dusk)
    ColorScheme.FOREST            -> stringResource(R.string.color_forest)
    ColorScheme.CANDLELIGHT       -> stringResource(R.string.color_candlelight)
    ColorScheme.SEPIA_PAPER       -> stringResource(R.string.color_sepia)
    ColorScheme.MIDNIGHT_EMERALD  -> stringResource(R.string.color_emerald)
}

@Composable
private fun PomodoroHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Column {
            Text(
                text  = stringResource(R.string.app_name).lowercase(),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight    = FontWeight.Light,
                    letterSpacing = 8.sp,
                    color         = MaterialTheme.colorScheme.primary
                )
            )
            Text(
                text  = stringResource(R.string.main_header_tagline),
                style = MaterialTheme.typography.labelLarge.copy(
                    color         = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 2.sp,
                    fontWeight    = FontWeight.Normal
                )
            )
        }
    }
}

@Composable
private fun LavaPreviewSection(
    uiState: PomodoroUiState,
    onIntent: (PomodoroIntent) -> Unit,
    onTogglePalette: () -> Unit,
    onToggleFocusModes: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(
                    width  = 1.dp,
                    color  = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    shape  = RoundedCornerShape(20.dp)
                )
        ) {
            LavaLampView(
                mode        = uiState.lavaMode,
                colorScheme = uiState.colorScheme,
                modifier    = Modifier.fillMaxSize()
            )

            // Botón Fullscreen (solo cuando está en reproducción)
            if (uiState.isTimerRunning) {
                IconButton(
                    onClick = { onIntent(PomodoroIntent.ToggleFullscreen) },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), CircleShape)
                ) {
                    Icon(
                        imageVector        = Icons.Default.Fullscreen,
                        contentDescription = stringResource(R.string.main_fullscreen_content_description),
                        tint               = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text  = stringResource(R.string.main_lava_preview_instruction),
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.main_customize_label),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.width(8.dp))

            // Selector de Modo Lava
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onToggleFocusModes() },
                color  = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape  = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Animation,
                        contentDescription = stringResource(R.string.main_change_mode_content_description),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text     = stringResource(
                            R.string.main_mode_label_format,
                            lavaModeLabel(uiState.lavaMode)
                        ),
                        style    = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Selector de Color
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onTogglePalette() },
                color  = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape  = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ColorLens,
                        contentDescription = stringResource(R.string.main_change_color_content_description),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text  = stringResource(
                            R.string.main_color_label_format,
                            colorSchemeLabel(uiState.colorScheme)
                        ),
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        }

    }
}

@Composable
private fun TimerSection(
    uiState: PomodoroUiState,
    onIntent: (PomodoroIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        // Temporizador circular animado
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(196.dp)
        ) {
            val animatedProgress by animateFloatAsState(
                targetValue    = uiState.timerProgress,
                animationSpec  = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                label          = "timer_progress"
            )

            CircularProgressIndicator(
                progress      = { animatedProgress },
                modifier      = Modifier.fillMaxSize(),
                color         = MaterialTheme.colorScheme.primary,
                trackColor    = MaterialTheme.colorScheme.surfaceVariant,
                strokeWidth   = 6.dp
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (uiState.isTimerRunning) {
                    Text(
                        text  = uiState.formattedTimeLeft,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize   = 36.sp,
                            fontWeight = FontWeight.Light,
                            color      = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    Text(
                        text  = stringResource(R.string.main_timer_until_break),
                        style = MaterialTheme.typography.labelLarge.copy(
                            color  = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                } else {
                    Text(
                        text  = "${uiState.intervalMinutes}",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize   = 48.sp,
                            fontWeight = FontWeight.Light,
                            color      = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                    )
                    Text(
                        text  = stringResource(R.string.main_timer_minutes_label),
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                FilledTonalButton(
                    onClick = {
                        if (uiState.isTimerRunning) onIntent(PomodoroIntent.StopTimer)
                        else onIntent(PomodoroIntent.StartTimer)
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (uiState.isTimerRunning)
                            MaterialTheme.colorScheme.secondaryContainer
                        else
                            MaterialTheme.colorScheme.primaryContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .height(36.dp)
                ) {
                    Icon(
                        imageVector = if (uiState.isTimerRunning) Icons.Default.Stop
                        else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (uiState.isTimerRunning) {
                            stringResource(R.string.main_timer_pause_button)
                        } else {
                            stringResource(R.string.main_timer_start_button)
                        },
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionStatsRow(completedBreaks: Int) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        color   = MaterialTheme.colorScheme.surfaceVariant,
        shape   = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatChip(value = "$completedBreaks", label = stringResource(R.string.main_stats_breaks_today))
            StatChip(value = "${completedBreaks * 20}", label = stringResource(R.string.main_stats_pause_seconds))
            StatChip(
                value = if (completedBreaks >= 3) {
                    stringResource(R.string.main_stats_healthy_done)
                } else {
                    stringResource(R.string.main_stats_healthy_pending)
                },
                label = stringResource(R.string.main_stats_healthy_session)
            )
        }
    }
}

@Composable
private fun StatChip(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text  = value,
            style = MaterialTheme.typography.titleMedium.copy(
                color      = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        )
        Text(
            text  = label,
            style = MaterialTheme.typography.labelLarge.copy(
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        )
    }
}

@Composable
private fun SettingsSection(
    uiState: PomodoroUiState,
    onIntent: (PomodoroIntent) -> Unit,
    showPalette: Boolean,
    onColorSelected: () -> Unit,
    showFocusModes: Boolean,
    onFocusModeSelected: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        AnimatedVisibility(
            visible = showPalette,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            SettingsCard(title = stringResource(R.string.main_palette_title)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(
                        ColorScheme.MIDNIGHT_EMERALD,
                        ColorScheme.DUSK,
                        ColorScheme.CANDLELIGHT
                    ).forEach { scheme ->
                        ColorChip(
                            scheme = scheme,
                            selected = scheme == uiState.colorScheme,
                            onClick = { 
                                onIntent(PomodoroIntent.SetColorScheme(scheme))
                                onColorSelected()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showFocusModes,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            SettingsCard(title = stringResource(R.string.main_focus_style_title)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FocusModeChip(
                        label     = stringResource(R.string.focus_mode_lava),
                        sublabel  = stringResource(R.string.focus_mode_lava_description),
                        selected  = uiState.lavaMode == LavaMode.LAVA,
                        onClick   = { 
                            onIntent(PomodoroIntent.SetLavaMode(LavaMode.LAVA))
                            onFocusModeSelected()
                        },
                        modifier  = Modifier.weight(1f)
                    )
                    FocusModeChip(
                        label     = stringResource(R.string.focus_mode_concentric_circles),
                        sublabel  = stringResource(R.string.focus_mode_circles_description),
                        selected  = uiState.lavaMode == LavaMode.FOCUS_POINT_CIRCLE,
                        onClick   = { 
                            onIntent(PomodoroIntent.SetLavaMode(LavaMode.FOCUS_POINT_CIRCLE))
                            onFocusModeSelected()
                        },
                        modifier  = Modifier.weight(1f)
                    )
                }
            }
        }

        SettingsCard(title = stringResource(R.string.main_interval_title)) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text  = stringResource(R.string.main_interval_description),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text  = stringResource(
                            R.string.main_interval_minutes_short_format,
                            uiState.intervalMinutes
                        ),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color      = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Text(
                        text  = stringResource(R.string.main_interval_who_recommendation),
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                Slider(
                    value         = uiState.intervalMinutes.toFloat(),
                    onValueChange = { onIntent(PomodoroIntent.SetInterval(it.toInt())) },
                    valueRange    = 5f..60f,
                    steps         = 10,
                    colors        = SliderDefaults.colors(
                        thumbColor       = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(
                        5 to R.string.main_interval_tick_5,
                        15 to R.string.main_interval_tick_15,
                        20 to R.string.main_interval_tick_20,
                        30 to R.string.main_interval_tick_30,
                        45 to R.string.main_interval_tick_45,
                        60 to R.string.main_interval_tick_60
                    ).forEach { (minutes, labelResId) ->
                        Text(
                            text  = stringResource(labelResId),
                            style = MaterialTheme.typography.labelLarge.copy(
                                color    = if (minutes == 20)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                fontSize = 10.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                TimerSection(
                    uiState = uiState,
                    onIntent = onIntent,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}


@Composable
private fun EyeWellnessCard(
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .heightIn(min = 96.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.96f),
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.86f)
                    )
                )
            )
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.72f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Spa,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.92f),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(R.string.eye_wellness_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = stringResource(R.string.eye_wellness_card_description),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.82f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = stringResource(R.string.eye_wellness_view_info_content_description),
                tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.9f),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun EyeWellnessDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_understood_button))
            }
        },
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = stringResource(R.string.eye_wellness_title),
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                        )

                        Text(
                            text = stringResource(R.string.eye_wellness_dialog_subtitle),
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.eye_wellness_dialog_intro),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                )

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.30f),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.eye_wellness_rule_title),
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                        )

                        Text(
                            text = stringResource(R.string.eye_wellness_rule_description),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        )
                    }
                }

                WellnessInfoItem(
                    title = stringResource(R.string.eye_wellness_less_digital_strain_title),
                    description = stringResource(R.string.eye_wellness_less_digital_strain_description)
                )

                WellnessInfoItem(
                    title = stringResource(R.string.eye_wellness_blinking_title),
                    description = stringResource(R.string.eye_wellness_blinking_description)
                )

                WellnessInfoItem(
                    title = stringResource(R.string.eye_wellness_prolonged_use_risk_title),
                    description = stringResource(R.string.eye_wellness_prolonged_use_risk_description)
                )

                WellnessInfoItem(
                    title = stringResource(R.string.eye_wellness_blue_light_sleep_title),
                    description = stringResource(R.string.eye_wellness_blue_light_sleep_description)
                )

                WellnessInfoItem(
                    title = stringResource(R.string.eye_wellness_attention_title),
                    description = stringResource(R.string.eye_wellness_attention_description)
                )

                WellnessInfoItem(
                    title = stringResource(R.string.eye_wellness_duration_matters_title),
                    description = stringResource(R.string.eye_wellness_duration_matters_description)
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.eye_wellness_healthy_tips_title),
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )

                        Text(
                            text = stringResource(R.string.eye_wellness_healthy_tips_body),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.50f),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.eye_wellness_references_title),
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                        )

                        Text(
                            text = stringResource(R.string.eye_wellness_references_body),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.82f),
                                fontSize = 10.5.sp,
                                lineHeight = 15.sp
                            )
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.eye_wellness_medical_disclaimer),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                )
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
private fun WellnessInfoItem(
    title: String,
    description: String
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.26f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 3.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                )
            }
        }
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// COMPONENTES REUTILIZABLES
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color  = MaterialTheme.colorScheme.surface,
        shape  = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text  = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun ColorChip(
    scheme: ColorScheme,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (bg, accent) = when (scheme) {
        ColorScheme.AMBER_MOSS      -> PomodoroColors.Surface0 to PomodoroColors.Amber400
        ColorScheme.TERRACOTTA_SAGE -> Color(0xFF170E0B) to Color(0xFFC77A5B)
        ColorScheme.DUSK            -> Color(0xFF0D0F14) to Color(0xFF967599)
        ColorScheme.FOREST          -> Color(0xFF0A130A) to Color(0xFF4C804C)
        ColorScheme.CANDLELIGHT     -> Color(0xFF120D0A) to Color(0xFFE67E22)
        ColorScheme.SEPIA_PAPER     -> Color(0xFF1C1B17) to Color(0xFFA67C52)
        ColorScheme.MIDNIGHT_EMERALD -> Color(0xFF081008) to Color(0xFF2D5A27)
    }

    val label = colorSchemeLabel(scheme)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .border(
                width  = if (selected) 2.dp else 1.dp,
                color  = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape  = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(bg)
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(accent)
                    .align(Alignment.Center)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text  = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface
            ),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FocusModeChip(
    label: String,
    sublabel: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .border(
                width  = if (selected) 2.dp else 1.dp,
                color  = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape  = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text  = sublabel,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            textAlign = TextAlign.Center,
            lineHeight = 12.sp
        )
    }
}

@Composable
private fun PomodoroProCard(
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .heightIn(min = 96.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF2E1A47).copy(alpha = 0.96f),
                        Color(0xFF1B0F2E).copy(alpha = 0.96f)
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFD700).copy(alpha = 0.7f),
                        Color(0xFFDA70D6).copy(alpha = 0.3f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(id = com.matias.pomodoro.R.drawable.ic_pomodoro_pro),
                    contentDescription = stringResource(R.string.pomodoro_pro_icon_content_description),
                    modifier = Modifier.size(46.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.pomodoro_pro_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color(0xFFFFD700),
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = Color(0xFFFFD700),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.pro_badge),
                            color = Color(0xFF1B0F2E),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 9.sp
                            ),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.pomodoro_pro_description),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = stringResource(R.string.pomodoro_pro_play_store_content_description),
                tint = Color(0xFFFFD700).copy(alpha = 0.9f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
