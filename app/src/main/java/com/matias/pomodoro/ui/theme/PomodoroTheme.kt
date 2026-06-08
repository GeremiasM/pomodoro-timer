package com.matias.pomodoro.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.matias.pomodoro.timer.PomodoroPhase

@Immutable
data class PomodoroColors(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val primary: Color,
    val accent: Color,
    val onBackground: Color,
    val onPrimary: Color,
    val muted: Color,
    val glow: Color,
    val track: Color
)

val LocalPomodoroColors = staticCompositionLocalOf {
    PomodoroColors(
        background = Color(0xFF0D1B14),
        surface = Color(0xFF12251D),
        surfaceElevated = Color(0xFF183228),
        primary = Color(0xFF00C896),
        accent = Color(0xFF00FFB2),
        onBackground = Color(0xFFEAF7F1),
        onPrimary = Color(0xFF06140F),
        muted = Color(0x99EAF7F1),
        glow = Color(0x3300C896),
        track = Color(0x2600C896)
    )
}

val PomodoroTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Light,
        fontSize = 64.sp,
        lineHeight = 72.sp,
        letterSpacing = (-1.2).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
        lineHeight = 34.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp
    )
)

@Composable
fun PomodoroTheme(
    phase: PomodoroPhase,
    selectedTheme: String,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val targetColors = paletteFor(selectedTheme, phase).let { palette ->
        if (darkTheme) palette else palette.lightVariant()
    }
    val animationSpec = tween<Color>(durationMillis = 800, easing = EaseInOutCubic)

    val background = animateColorAsState(targetColors.background, animationSpec, label = "pomodoro_background").value
    val surface = animateColorAsState(targetColors.surface, animationSpec, label = "pomodoro_surface").value
    val surfaceElevated = animateColorAsState(targetColors.surfaceElevated, animationSpec, label = "pomodoro_surface_elevated").value
    val primary = animateColorAsState(targetColors.primary, animationSpec, label = "pomodoro_primary").value
    val accent = animateColorAsState(targetColors.accent, animationSpec, label = "pomodoro_accent").value
    val onBackground = animateColorAsState(targetColors.onBackground, animationSpec, label = "pomodoro_on_background").value
    val onPrimary = animateColorAsState(targetColors.onPrimary, animationSpec, label = "pomodoro_on_primary").value
    val muted = animateColorAsState(targetColors.muted, animationSpec, label = "pomodoro_muted").value
    val glow = animateColorAsState(targetColors.glow, animationSpec, label = "pomodoro_glow").value
    val track = animateColorAsState(targetColors.track, animationSpec, label = "pomodoro_track").value

    val colors = PomodoroColors(
        background = background,
        surface = surface,
        surfaceElevated = surfaceElevated,
        primary = primary,
        accent = accent,
        onBackground = onBackground,
        onPrimary = onPrimary,
        muted = muted,
        glow = glow,
        track = track
    )

    CompositionLocalProvider(LocalPomodoroColors provides colors) {
        MaterialTheme(
            colorScheme = materialScheme(colors),
            typography = PomodoroTypography,
            content = content
        )
    }
}

private fun materialScheme(colors: PomodoroColors): ColorScheme = darkColorScheme(
    primary = colors.primary,
    onPrimary = colors.onPrimary,
    primaryContainer = colors.surfaceElevated,
    onPrimaryContainer = colors.onBackground,
    secondary = colors.accent,
    onSecondary = colors.onPrimary,
    background = colors.background,
    onBackground = colors.onBackground,
    surface = colors.surface,
    onSurface = colors.onBackground,
    surfaceVariant = colors.surfaceElevated,
    onSurfaceVariant = colors.muted,
    outline = colors.primary.copy(alpha = 0.36f)
)

private fun paletteFor(selectedTheme: String, phase: PomodoroPhase): PomodoroColors {
    val base = when (selectedTheme) {
        "sunset" -> when (phase) {
            PomodoroPhase.Work -> Triple(Color(0xFF1B0D0D), Color(0xFFE05C3A), Color(0xFFFF9966))
            PomodoroPhase.ShortBreak -> Triple(Color(0xFF1B140D), Color(0xFFE0A03A), Color(0xFFFFD080))
            PomodoroPhase.LongBreak -> Triple(Color(0xFF1B0D18), Color(0xFFC03A9E), Color(0xFFFF80E0))
        }
        "ocean" -> when (phase) {
            PomodoroPhase.Work -> Triple(Color(0xFF0D1320), Color(0xFF3A7BD5), Color(0xFF80B4FF))
            PomodoroPhase.ShortBreak -> Triple(Color(0xFF0D1B1A), Color(0xFF3AB5A0), Color(0xFF80EEE0))
            PomodoroPhase.LongBreak -> Triple(Color(0xFF14100D), Color(0xFF8B6914), Color(0xFFFFD966))
        }
        "forest" -> when (phase) {
            PomodoroPhase.Work -> Triple(Color(0xFF0F1A0D), Color(0xFF5AAE3A), Color(0xFFA0FF70))
            PomodoroPhase.ShortBreak -> Triple(Color(0xFF0D1A18), Color(0xFF3AAE8C), Color(0xFF70FFD4))
            PomodoroPhase.LongBreak -> Triple(Color(0xFF1A180D), Color(0xFFAE9A3A), Color(0xFFFFE870))
        }
        "lavender" -> when (phase) {
            PomodoroPhase.Work -> Triple(Color(0xFF130D1B), Color(0xFF8A5AE0), Color(0xFFC4A0FF))
            PomodoroPhase.ShortBreak -> Triple(Color(0xFF0D1320), Color(0xFF5A80E0), Color(0xFFA0C4FF))
            PomodoroPhase.LongBreak -> Triple(Color(0xFF0D1A18), Color(0xFF5AE0B4), Color(0xFFA0FFE0))
        }
        else -> when (phase) {
            PomodoroPhase.Work -> Triple(Color(0xFF0D1B14), Color(0xFF00C896), Color(0xFF00FFB2))
            PomodoroPhase.ShortBreak -> Triple(Color(0xFF0D1A1B), Color(0xFF00C8C8), Color(0xFF80FFFF))
            PomodoroPhase.LongBreak -> Triple(Color(0xFF0D1320), Color(0xFF4A90D9), Color(0xFF80C4FF))
        }
    }

    val background = base.first
    val primary = base.second
    val accent = base.third
    return PomodoroColors(
        background = background,
        surface = lerp(background, Color.White, 0.05f),
        surfaceElevated = lerp(background, Color.White, 0.10f),
        primary = primary,
        accent = accent,
        onBackground = Color(0xFFF1F7F4),
        onPrimary = Color(0xFF06100C),
        muted = Color(0x99F1F7F4),
        glow = primary.copy(alpha = 0.20f),
        track = primary.copy(alpha = 0.15f)
    )
}

private fun PomodoroColors.lightVariant(): PomodoroColors = copy(
    background = lerp(background, Color.White, 0.15f),
    surface = lerp(surface, Color.White, 0.15f),
    surfaceElevated = lerp(surfaceElevated, Color.White, 0.15f),
    onBackground = Color(0xFFF8FBFA),
    muted = Color(0xB3F8FBFA)
)
