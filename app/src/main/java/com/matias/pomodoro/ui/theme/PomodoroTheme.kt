package com.matias.pomodoro.ui.theme

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.matias.pomodoro.graphics.ColorScheme as VisualColorScheme

// ─────────────────────────────────────────────────────────────────────────────
// PALETA DE COLORES – DISEÑADA PARA SALUD OCULAR
// ─────────────────────────────────────────────────────────────────────────────
//
// Principios:
// 1. Bajo en luz azul: evitar blues brillantes y blancos puros.
// 2. Contraste suave: no máximo (blanco puro sobre negro puro fatiga).
// 3. Tonos cálidos: ámbar, verde musgo, terracota, como luz de tarde.
// 4. Modo oscuro auténtico: fondos muy oscuros, no grises medios.
//

object PomodoroColors {
    // ── Ámbar / Oro ──────────────────────────────────────────────────────────
    val Amber400    = Color(0xFFD4A96A)  // Ámbar cálido principal
    val Amber300    = Color(0xFFE0BB8A)  // Ámbar claro
    val Amber600    = Color(0xFFB8883E)  // Ámbar oscuro

    // ── Verde musgo ──────────────────────────────────────────────────────────
    val Moss500     = Color(0xFF6B8E6B)  // Verde musgo principal
    val Moss400     = Color(0xFF85A885)  // Verde musgo claro
    val Moss700     = Color(0xFF4A6B4A)  // Verde musgo oscuro

    // ── Fondos oscuros (bajo en azul) ────────────────────────────────────────
    val Surface0    = Color(0xFF111614)  // Fondo principal (~negro verdoso)
    val Surface1    = Color(0xFF1A2019)  // Tarjetas / superficies elevadas
    val Surface2    = Color(0xFF222B21)  // Elementos secundarios

    // ── Fondos claros ────────────────────────────────────────────────────────
    val SurfaceLight0 = Color(0xFFF5F0E8)  // Fondo principal claro (crema)
    val SurfaceLight1 = Color(0xFFEDE6D8)  // Tarjetas claras
    val SurfaceLight2 = Color(0xFFE2D9C8)  // Bordes claros

    // ── Texto ────────────────────────────────────────────────────────────────
    val TextPrimary     = Color(0xFFE8DFC8)  // Texto principal dark mode (crema)
    val TextSecondary   = Color(0xFF9EA898)  // Texto secundario dark mode
    val TextPrimaryLight = Color(0xFF2C3428)  // Texto principal light mode
    val TextSecondaryLight = Color(0xFF5A6456)  // Texto secundario light mode

    // ── Acento ───────────────────────────────────────────────────────────────
    val Accent      = Color(0xFFD4A96A)  // = Amber400
    val AccentLight = Color(0xFFE0BB8A)
    val Error       = Color(0xFFB85C3C)  // Rojo terracota suave

    // ── Overlay ──────────────────────────────────────────────────────────────
    val Scrim       = Color(0xCC0D110E)  // Overlay semi-transparente oscuro
}

// ─────────────────────────────────────────────────────────────────────────────
// COLOR SCHEMES MATERIAL 3
// ─────────────────────────────────────────────────────────────────────────────

private val DarkColorScheme = darkColorScheme(
    primary            = PomodoroColors.Amber400,
    onPrimary          = PomodoroColors.Surface0,
    primaryContainer   = PomodoroColors.Amber600,
    onPrimaryContainer = PomodoroColors.Amber300,

    secondary          = PomodoroColors.Moss500,
    onSecondary        = PomodoroColors.Surface0,
    secondaryContainer = PomodoroColors.Moss700,
    onSecondaryContainer = PomodoroColors.Moss400,

    background         = PomodoroColors.Surface0,
    onBackground       = PomodoroColors.TextPrimary,

    surface            = PomodoroColors.Surface1,
    onSurface          = PomodoroColors.TextPrimary,
    surfaceVariant     = PomodoroColors.Surface2,
    onSurfaceVariant   = PomodoroColors.TextSecondary,

    error              = PomodoroColors.Error,
    outline            = PomodoroColors.Moss700
)

private val LightColorScheme = lightColorScheme(
    primary            = PomodoroColors.Amber600,
    onPrimary          = PomodoroColors.SurfaceLight0,
    primaryContainer   = PomodoroColors.Amber300,
    onPrimaryContainer = Color(0xFF4A3010),

    secondary          = PomodoroColors.Moss700,
    onSecondary        = PomodoroColors.SurfaceLight0,
    secondaryContainer = PomodoroColors.Moss400,
    onSecondaryContainer = Color(0xFF1A3018),

    background         = PomodoroColors.SurfaceLight0,
    onBackground       = PomodoroColors.TextPrimaryLight,

    surface            = PomodoroColors.SurfaceLight1,
    onSurface          = PomodoroColors.TextPrimaryLight,
    surfaceVariant     = PomodoroColors.SurfaceLight2,
    onSurfaceVariant   = PomodoroColors.TextSecondaryLight,

    error              = PomodoroColors.Error,
    outline            = PomodoroColors.Moss500
)

// ─────────────────────────────────────────────────────────────────────────────
// TIPOGRAFÍA
// ─────────────────────────────────────────────────────────────────────────────
//
// Nota: Para el proyecto real, agregar estas fuentes al directorio res/font/
// o usar GoogleFonts. Aquí usamos las familias por nombre para referencia.

val PomodoroTypography = Typography(
    // Título grande: números del temporizador, pantalla de descanso
    displayLarge = TextStyle(
        fontFamily   = FontFamily.Default,
        fontWeight   = FontWeight.Light,
        fontSize     = 57.sp,
        lineHeight   = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    // Nombre de la app, títulos principales
    headlineLarge = TextStyle(
        fontFamily   = FontFamily.Default,
        fontWeight   = FontWeight.Normal,
        fontSize     = 32.sp,
        lineHeight   = 40.sp,
        letterSpacing = 0.sp
    ),
    // Subtítulos de sección
    titleMedium = TextStyle(
        fontFamily   = FontFamily.Default,
        fontWeight   = FontWeight.Medium,
        fontSize     = 16.sp,
        lineHeight   = 24.sp,
        letterSpacing = 0.15.sp
    ),
    // Texto de cuerpo principal
    bodyLarge = TextStyle(
        fontFamily   = FontFamily.Default,
        fontWeight   = FontWeight.Normal,
        fontSize     = 16.sp,
        lineHeight   = 24.sp,
        letterSpacing = 0.5.sp
    ),
    // Etiquetas de botones y chips
    labelLarge = TextStyle(
        fontFamily   = FontFamily.Default,
        fontWeight   = FontWeight.Medium,
        fontSize     = 14.sp,
        lineHeight   = 20.sp,
        letterSpacing = 0.1.sp
    )
)

// ─────────────────────────────────────────────────────────────────────────────
// TEMA PRINCIPAL
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PomodoroTheme(
    visualScheme: VisualColorScheme = VisualColorScheme.AMBER_MOSS,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    Crossfade(
        targetState = visualScheme,
        animationSpec = tween(durationMillis = 550),
        label = "pomodoro_visual_palette_crossfade"
    ) { scheme ->
        val materialScheme = if (darkTheme) scheme.toMaterialDark() else LightColorScheme

        MaterialTheme(
            colorScheme = materialScheme,
            typography  = PomodoroTypography,
            content     = content
        )
    }
}

private fun VisualColorScheme.toMaterialDark(): androidx.compose.material3.ColorScheme {
    // Tema oscuro de bajo deslumbramiento. Mantiene contraste suave y evita blancos puros.
    return when (this) {
        VisualColorScheme.AMBER_MOSS -> DarkColorScheme

        VisualColorScheme.TERRACOTTA_SAGE -> darkColorScheme(
            primary = Color(0xFFC77A5B),
            onPrimary = Color(0xFF120D0A),
            secondary = Color(0xFF84A07E),
            onSecondary = Color(0xFF120D0A),
            background = Color(0xFF171514),
            onBackground = PomodoroColors.TextPrimary,
            surface = Color(0xFF1A1715),
            onSurface = PomodoroColors.TextPrimary,
            surfaceVariant = Color(0xFF221E1B),
            onSurfaceVariant = PomodoroColors.TextSecondary,
            error = PomodoroColors.Error,
            outline = Color(0xFF5B4E45)
        )

        VisualColorScheme.DUSK -> darkColorScheme(
            primary = Color(0xFF967599),
            onPrimary = Color(0xFF0D0F14),
            secondary = Color(0xFF5B778C),
            onSecondary = Color(0xFF0D0F14),
            background = Color(0xFF10121A),
            onBackground = PomodoroColors.TextPrimary,
            surface = Color(0xFF121522),
            onSurface = PomodoroColors.TextPrimary,
            surfaceVariant = Color(0xFF1A1E2B),
            onSurfaceVariant = PomodoroColors.TextSecondary,
            error = PomodoroColors.Error,
            outline = Color(0xFF3A4152)
        )

        VisualColorScheme.FOREST -> darkColorScheme(
            primary = Color(0xFF4C804C),
            onPrimary = Color(0xFF0A130A),
            secondary = Color(0xFFBD984C),
            onSecondary = Color(0xFF0A130A),
            background = Color(0xFF0C150C),
            onBackground = PomodoroColors.TextPrimary,
            surface = Color(0xFF0F1A0F),
            onSurface = PomodoroColors.TextPrimary,
            surfaceVariant = Color(0xFF142214),
            onSurfaceVariant = PomodoroColors.TextSecondary,
            error = PomodoroColors.Error,
            outline = Color(0xFF2B3A2B)
        )

        VisualColorScheme.CANDLELIGHT -> darkColorScheme(
            primary = Color(0xFFE67E22),
            onPrimary = Color(0xFF120D0A),
            secondary = Color(0xFFB03A2E), // rojo ladrillo (acento)
            onSecondary = Color(0xFF120D0A),
            background = Color(0xFF120D0A),
            onBackground = Color(0xFFE8DFC8),
            surface = Color(0xFF17100C),
            onSurface = Color(0xFFE8DFC8),
            surfaceVariant = Color(0xFF20150F),
            onSurfaceVariant = Color(0xFFB7A99A),
            error = PomodoroColors.Error,
            outline = Color(0xFF3A2A22)
        )

        VisualColorScheme.SEPIA_PAPER -> darkColorScheme(
            primary = Color(0xFFA67C52),
            onPrimary = Color(0xFF1C1B17),
            secondary = Color(0xFF4A2F1A), // marrón oscuro (acento)
            onSecondary = Color(0xFF1C1B17),
            background = Color(0xFF1C1B17),
            onBackground = Color(0xFFE8DFC8),
            surface = Color(0xFF201F1A),
            onSurface = Color(0xFFE8DFC8),
            surfaceVariant = Color(0xFF2A2822),
            onSurfaceVariant = Color(0xFFB5AD9B),
            error = PomodoroColors.Error,
            outline = Color(0xFF3B3328)
        )

        VisualColorScheme.MIDNIGHT_EMERALD -> darkColorScheme(
            primary = Color(0xFF2D5A27),
            onPrimary = Color(0xFF081008),
            secondary = Color(0xFFB89B4A), // mostaza suave
            onSecondary = Color(0xFF081008),
            background = Color(0xFF081008),
            onBackground = Color(0xFFE8DFC8),
            surface = Color(0xFF0B160B),
            onSurface = Color(0xFFE8DFC8),
            surfaceVariant = Color(0xFF102010),
            onSurfaceVariant = Color(0xFF9EA898),
            error = PomodoroColors.Error,
            outline = Color(0xFF243324)
        )
    }
}
