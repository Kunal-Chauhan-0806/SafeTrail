package com.kunalchauhan.safetrail.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = Teal80,
    onPrimary = Color(0xFF00363B),
    primaryContainer = TealContainerDark,
    onPrimaryContainer = Color(0xFFCFF3F6),
    secondary = Indigo80,
    onSecondary = Color(0xFF1C1870),
    secondaryContainer = IndigoContainerDark,
    onSecondaryContainer = Color(0xFFE2E0FF),
    tertiary = Amber80,
    onTertiary = Color(0xFF4A2200),
    tertiaryContainer = AmberContainerDark,
    onTertiaryContainer = Color(0xFFFFE8CC),
    background = BackgroundDark,
    onBackground = Color(0xFFE0EAEB),
    surface = SurfaceDark,
    onSurface = Color(0xFFE0EAEB),
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = Color(0xFFBCCBCD),
    outline = OutlineDark,
    error = SosRedDark,
    onError = Color(0xFF4A0A06),
    errorContainer = RedContainerDark,
    onErrorContainer = Color(0xFFFFE1DE)
)

private val LightColorScheme = lightColorScheme(
    primary = Teal40,
    onPrimary = Color.White,
    primaryContainer = TealContainerLight,
    onPrimaryContainer = Color(0xFF00363B),
    secondary = Indigo40,
    onSecondary = Color.White,
    secondaryContainer = IndigoContainerLight,
    onSecondaryContainer = Color(0xFF1C1870),
    tertiary = Amber40,
    onTertiary = Color.White,
    tertiaryContainer = AmberContainerLight,
    onTertiaryContainer = Color(0xFF4A2200),
    background = BackgroundLight,
    onBackground = Color(0xFF111A1B),
    surface = SurfaceLight,
    onSurface = Color(0xFF111A1B),
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = Color(0xFF3F4D4F),
    outline = OutlineLight,
    error = SosRed,
    onError = Color.White,
    errorContainer = RedContainerLight,
    onErrorContainer = Color(0xFF7A1410)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun SafeTrailTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Dynamic (wallpaper) color is off so the app always shows its own palette
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
