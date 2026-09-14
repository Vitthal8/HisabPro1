package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DeepNavy,
    primaryContainer = NavyDark,
    onPrimaryContainer = Color.White,
    secondary = DarkSecondary,
    onSecondary = Color.Black,
    secondaryContainer = SaffronDark,
    onSecondaryContainer = Color.White,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceCard,
    onBackground = Color(0xFFE2E2E6),
    onSurface = Color(0xFFE2E2E6),
    outline = Color(0xFF44474E)
)

private val LightColorScheme = lightColorScheme(
    primary = DeepNavy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8EAF6),
    onPrimaryContainer = DeepNavy,
    secondary = Saffron,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE0B2),
    onSecondaryContainer = SaffronDark,
    tertiary = CreditGreen,
    onTertiary = Color.White,
    tertiaryContainer = CreditGreenLight,
    onTertiaryContainer = CreditGreen,
    background = OffWhite,
    surface = SurfaceCard,
    surfaceVariant = Color(0xFFF0F4F8),
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = BorderStroke,
    error = DebitRed,
    onError = Color.White,
    errorContainer = DebitRedLight,
    onErrorContainer = DebitRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
