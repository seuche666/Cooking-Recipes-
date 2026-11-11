package de.rezeptbuch.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Design „Klar & Hell“: heller, leicht bläulich-grauer Hintergrund, weiße Karten, blauer Akzent.
private val LightColors = lightColorScheme(
    primary = Color(0xFF2F5BD3),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE8FF),
    onPrimaryContainer = Color(0xFF0F2A6B),
    secondary = Color(0xFF4B5873),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE6EAF1),
    onSecondaryContainer = Color(0xFF1F2937),
    tertiary = Color(0xFF15803D),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD9F5E3),
    onTertiaryContainer = Color(0xFF0B3B1E),
    background = Color(0xFFF3F5F9),
    onBackground = Color(0xFF111827),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFE6EAF1),
    onSurfaceVariant = Color(0xFF5B6475),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF7F8FB),
    surfaceContainerHighest = Color(0xFFEEF1F6),
    outline = Color(0xFFB8C0CD),
    outlineVariant = Color(0xFFE3E7EE),
    error = Color(0xFFC62828),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFDE0E0),
    onErrorContainer = Color(0xFF5F1111),
    inverseSurface = Color(0xFF1F2430),
    inverseOnSurface = Color(0xFFF1F3F8),
    inversePrimary = Color(0xFF9DB8FF),
)

// Dunkel: gleicher Aufbau, dunkler Hintergrund, dunkelgraue Karten, helle Schrift.
private val DarkColors = darkColorScheme(
    primary = Color(0xFF8DB0FF),
    onPrimary = Color(0xFF0B1B44),
    primaryContainer = Color(0xFF1E2E55),
    onPrimaryContainer = Color(0xFFDCE8FF),
    secondary = Color(0xFFB4BFD3),
    onSecondary = Color(0xFF1C2433),
    secondaryContainer = Color(0xFF2A3140),
    onSecondaryContainer = Color(0xFFE2E7F0),
    tertiary = Color(0xFF86EFAC),
    onTertiary = Color(0xFF0B3B1E),
    tertiaryContainer = Color(0xFF163B26),
    onTertiaryContainer = Color(0xFFD9F5E3),
    background = Color(0xFF0E1116),
    onBackground = Color(0xFFE8ECF4),
    surface = Color(0xFF181C24),
    onSurface = Color(0xFFE8ECF4),
    surfaceVariant = Color(0xFF232937),
    onSurfaceVariant = Color(0xFFA9B2C3),
    surfaceContainerLowest = Color(0xFF12161D),
    surfaceContainerLow = Color(0xFF181C24),
    surfaceContainer = Color(0xFF1C212B),
    surfaceContainerHigh = Color(0xFF232937),
    surfaceContainerHighest = Color(0xFF2A3140),
    outline = Color(0xFF4A5366),
    outlineVariant = Color(0xFF2E3544),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF4A0D0D),
    errorContainer = Color(0xFF4A1C1C),
    onErrorContainer = Color(0xFFFDE0E0),
    inverseSurface = Color(0xFFE8ECF4),
    inverseOnSurface = Color(0xFF1F2430),
    inversePrimary = Color(0xFF2F5BD3),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** `true`, wenn gerade das dunkle Design aktiv ist (für Schatten und Kategorie-Farben). */
val LocalDarkTheme = staticCompositionLocalOf { false }

@Composable
fun RezeptbuchTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
