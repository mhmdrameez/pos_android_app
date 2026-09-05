package com.example.quickbillposs.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PosSteelBlue,
    onPrimary = PosTextWhite,
    primaryContainer = PosSteelBlueDark,
    onPrimaryContainer = PosTextWhite,
    secondary = AccentGreen,
    onSecondary = PosTextWhite,
    background = NavyBackground,
    onBackground = TextPrimary,
    surface = NavySurface,
    onSurface = TextPrimary,
    surfaceVariant = NavyCard,
    onSurfaceVariant = TextSecondary,
    outline = NavyBorder,
    error = AccentRed,
    onError = PosTextWhite
)

private val LightColorScheme = lightColorScheme(
    primary = PosSteelBlue,
    onPrimary = PosTextWhite,
    primaryContainer = Color(0xFFEEF2FF),
    onPrimaryContainer = PosSteelBlue,
    secondary = AccentGreen,
    onSecondary = PosTextWhite,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = AccentGreen,
    background = PosBgMain,
    onBackground = PosTextDark,
    surface = PosBgMain,
    onSurface = PosTextDark,
    surfaceVariant = PosBgKeypadKey,
    onSurfaceVariant = PosTextMuted,
    outline = PosBorder,
    error = AccentRed,
    onError = PosTextWhite
)

@Composable
fun QuickBillTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
