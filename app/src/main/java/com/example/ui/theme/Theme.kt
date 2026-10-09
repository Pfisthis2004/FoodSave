package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalDarkTheme = staticCompositionLocalOf { false }

private val LightColorScheme = lightColorScheme(
    primary = OliveGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = OliveGreenContainer,
    onPrimaryContainer = OnOliveGreenContainer,
    secondary = MutedOliveSecondary,
    onSecondary = Color.White,
    secondaryContainer = MutedOliveSecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    background = WarmWhiteBackground,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceMuted,
    onSurfaceVariant = TextSecondary,
    outline = SurfaceBorder,
    outlineVariant = Color(0xFFD6DBD2),
    error = ExpiredRed,
    onError = Color.White,
    errorContainer = ExpiredRedBg,
    onErrorContainer = ExpiredRed
)

private val DarkColorScheme = darkColorScheme(
    primary = OliveGreenPrimaryDark,
    onPrimary = Color(0xFF0C2B14),
    primaryContainer = OliveGreenContainerDark,
    onPrimaryContainer = OnOliveGreenContainerDark,
    secondary = Color(0xFFB5C5B0),
    onSecondary = Color(0xFF223220),
    secondaryContainer = Color(0xFF354533),
    onSecondaryContainer = Color(0xFFD4E6D0),
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceMuted,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    outlineVariant = Color(0xFF2E352E),
    error = ExpiredRedDark,
    onError = Color(0xFF560000),
    errorContainer = ExpiredRedBgDark,
    onErrorContainer = ExpiredRedDark
)

val MaterialTheme.statusFresh: Color
    @Composable get() = if (LocalDarkTheme.current) FreshGreenDark else FreshGreen

val MaterialTheme.statusFreshBg: Color
    @Composable get() = if (LocalDarkTheme.current) FreshGreenBgDark else FreshGreenBg

val MaterialTheme.statusExpiring: Color
    @Composable get() = if (LocalDarkTheme.current) ExpiringOrangeDark else ExpiringOrange

val MaterialTheme.statusExpiringBg: Color
    @Composable get() = if (LocalDarkTheme.current) ExpiringOrangeBgDark else ExpiringOrangeBg

val MaterialTheme.statusExpired: Color
    @Composable get() = if (LocalDarkTheme.current) ExpiredRedDark else ExpiredRed

val MaterialTheme.statusExpiredBg: Color
    @Composable get() = if (LocalDarkTheme.current) ExpiredRedBgDark else ExpiredRedBg

@Composable
fun FoodSaveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
