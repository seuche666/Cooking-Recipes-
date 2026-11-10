package de.rezeptbuch.app.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Color(0xFF9A4521),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDBCE),
    onPrimaryContainer = Color(0xFF370E00),
    secondary = Color(0xFF6B5D2F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF5E1A7),
    onSecondaryContainer = Color(0xFF231B00),
    tertiary = Color(0xFF4A6545),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCCEBC2),
    onTertiaryContainer = Color(0xFF072107),
    background = Color(0xFFFFF8F5),
    onBackground = Color(0xFF231A16),
    surface = Color(0xFFFFF8F5),
    onSurface = Color(0xFF231A16),
    surfaceVariant = Color(0xFFF5DED5),
    onSurfaceVariant = Color(0xFF53433D),
    outline = Color(0xFF85736C),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB598),
    onPrimary = Color(0xFF591D00),
    primaryContainer = Color(0xFF7B2E0B),
    onPrimaryContainer = Color(0xFFFFDBCE),
    secondary = Color(0xFFD8C58D),
    onSecondary = Color(0xFF3A2F05),
    secondaryContainer = Color(0xFF52461A),
    onSecondaryContainer = Color(0xFFF5E1A7),
    tertiary = Color(0xFFB0CFA8),
    onTertiary = Color(0xFF1D361A),
    tertiaryContainer = Color(0xFF334D2F),
    onTertiaryContainer = Color(0xFFCCEBC2),
    background = Color(0xFF1A120F),
    onBackground = Color(0xFFF0DFD8),
    surface = Color(0xFF1A120F),
    onSurface = Color(0xFFF0DFD8),
    surfaceVariant = Color(0xFF53433D),
    onSurfaceVariant = Color(0xFFD8C2B9),
    outline = Color(0xFFA08D85),
)

@Composable
fun RezeptbuchTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, typography = Typography(), content = content)
}
