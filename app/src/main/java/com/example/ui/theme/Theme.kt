package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PremiumDarkColorScheme = darkColorScheme(
    primary = VoltGreen,
    secondary = CyberBlue,
    tertiary = ThermalOrange,
    background = DarkBackdrop,
    surface = DarkSurface,
    onPrimary = Color(0xFF00381B),
    onSecondary = Color(0xFF00363D),
    onTertiary = Color(0xFF4C1D00),
    onBackground = PureWhite,
    onSurface = PureWhite,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = MutedSlate
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme by default (AMOLED power saving)
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PremiumDarkColorScheme,
        typography = Typography,
        content = content
    )
}
