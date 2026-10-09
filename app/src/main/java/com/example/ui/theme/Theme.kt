package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BoxingRed,
    onPrimary = Color.White,
    primaryContainer = BoxingRedDark,
    onPrimaryContainer = BoxingWhite,
    secondary = ChampionshipGold,
    onSecondary = Color.Black,
    secondaryContainer = ChampionshipGoldDark,
    onSecondaryContainer = Color.White,
    tertiary = BoxingGreen,
    background = ArenaBackground,
    onBackground = BoxingWhite,
    surface = ArenaSurface,
    onSurface = BoxingWhite,
    surfaceVariant = ArenaSurfaceVariant,
    onSurfaceVariant = BoxingMuted
)

@Composable
fun KnockoutAlarmTheme(
    content: @Composable () -> Unit
) {
    // Boxing arena stays intentionally dark and high-contrast
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
