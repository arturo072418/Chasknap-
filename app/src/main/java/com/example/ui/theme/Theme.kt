package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    secondary = ElectricPink,
    tertiary = WarningYellow,
    background = MidnightOnyx,
    surface = CyberSlate,
    onPrimary = MidnightOnyx,
    onSecondary = Color.White,
    onTertiary = MidnightOnyx,
    onBackground = TextWhite,
    onSurface = TextWhite,
    error = Color(0xFFFF5252)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force urban dark theme for maximum street youthful glow
    dynamicColor: Boolean = false, // Disable dynamic colors to keep brand colors intact
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
