package com.matias.pomodoro.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matias.pomodoro.R
import com.matias.pomodoro.graphics.LavaLampView
import com.matias.pomodoro.graphics.LavaMode
import com.matias.pomodoro.ui.theme.PomodoroColors
import com.matias.pomodoro.viewmodel.PomodoroIntent
import com.matias.pomodoro.viewmodel.PomodoroUiState

// ─────────────────────────────────────────────────────────────────────────────
// PANTALLA DE DESCANSO ACTIVO
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Overlay de pantalla completa que se muestra durante los 20 segundos de
 * descanso visual.
 *
 * Características:
 * - La lámpara de lava ocupa toda la pantalla en modo Focus Point.
 * - Cuenta regresiva de 20 segundos con barra de progreso suave.
 * - Mensaje de guía para el usuario (instrucciones de higiene ocular).
 * - Botón para omitir el descanso.
 *
 * Diseño intencionalmente oscuro y de baja luminosidad para no fatigar
 * aún más los ojos del usuario.
 */
@Composable
fun RestScreen(
    uiState: PomodoroUiState,
    onIntent: (PomodoroIntent) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PomodoroColors.Surface0)
    ) {
        // ── Animación de fondo (pantalla completa) ───────────────────────────
        LavaLampView(
            mode        = uiState.lavaMode.takeIf {
                it == LavaMode.FOCUS_POINT_INFINITY || it == LavaMode.FOCUS_POINT_CIRCLE
            } ?: LavaMode.FOCUS_POINT_INFINITY,
            colorScheme = uiState.colorScheme,
            modifier    = Modifier.fillMaxSize()
        )

        // ── Gradiente superior (oscuro) para legibilidad del texto ───────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.25f)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            PomodoroColors.Surface0.copy(alpha = 0.85f),
                            Color.Transparent
                        )
                    )
                )
        )

        // ── Gradiente inferior (oscuro) para legibilidad del texto ───────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.35f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            PomodoroColors.Surface0.copy(alpha = 0.9f)
                        )
                    )
                )
        )

        // ── Contenido superpuesto ─────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ── Encabezado ───────────────────────────────────────────────────
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text  = stringResource(R.string.rest_screen_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color         = PomodoroColors.Amber400,
                        letterSpacing = 4.sp,
                        fontWeight    = FontWeight.Light
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text  = stringResource(R.string.rest_screen_subtitle),
                    style = MaterialTheme.typography.labelLarge.copy(
                        color         = PomodoroColors.TextSecondary,
                        letterSpacing = 2.sp
                    )
                )
            }

            // ── Instrucción central ──────────────────────────────────────────
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp)),
                color = PomodoroColors.Surface0.copy(alpha = 0.75f)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text      = stringResource(uiState.restMessageResId),
                        style     = MaterialTheme.typography.bodyLarge.copy(
                            color     = PomodoroColors.TextPrimary,
                            textAlign = TextAlign.Center,
                            lineHeight = 26.sp
                        )
                    )
                }
            }

            // ── Cuenta regresiva + controles ─────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Número de cuenta regresiva con animación de latido
                val pulse by rememberInfiniteTransition(label = "pulse")
                    .animateFloat(
                        initialValue   = 0.95f,
                        targetValue    = 1.05f,
                        animationSpec  = infiniteRepeatable(
                            animation = tween(800, easing = EaseInOutSine),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse_scale"
                    )

                Text(
                    text  = "${uiState.restCountdownSeconds}",
                    style = MaterialTheme.typography.displayLarge.copy(
                        color      = PomodoroColors.Amber300,
                        fontWeight = FontWeight.Light,
                        fontSize   = (56 * pulse).sp
                    )
                )

                // Barra de progreso del descanso
                val restProgress = 1f - (uiState.restCountdownSeconds / 20f)
                val animProgress by animateFloatAsState(
                    targetValue   = restProgress,
                    animationSpec = tween(900, easing = LinearEasing),
                    label         = "rest_progress"
                )

                LinearProgressIndicator(
                    progress    = { animProgress },
                    modifier    = Modifier
                        .fillMaxWidth(0.7f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color       = PomodoroColors.Amber400,
                    trackColor  = PomodoroColors.Surface2
                )

                // Botón de omitir
                TextButton(
                    onClick = { onIntent(PomodoroIntent.DismissRestScreen) },
                    colors  = ButtonDefaults.textButtonColors(
                        contentColor = PomodoroColors.TextSecondary
                    )
                ) {
                    Text(
                        text  = stringResource(R.string.rest_screen_skip_button),
                        style = MaterialTheme.typography.labelLarge.copy(
                            letterSpacing = 1.sp
                        )
                    )
                }
            }
        }
    }
}
