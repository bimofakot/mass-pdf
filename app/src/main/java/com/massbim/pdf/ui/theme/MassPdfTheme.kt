package com.massbim.pdf.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

private val LightColors = lightColorScheme(
    primary = Color(0xFF176B67),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4F2ED),
    onPrimaryContainer = Color(0xFF00201D),
    secondary = Color(0xFF4F6260),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD2E8E4),
    onSecondaryContainer = Color(0xFF0B1F1D),
    background = Color(0xFFF6F8F7),
    surface = Color(0xFFF6F8F7),
    surfaceVariant = Color(0xFFE5EAE8),
    onSurface = Color(0xFF191D1C),
    onSurfaceVariant = Color(0xFF404947),
    error = Color(0xFFBA1A1A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8DD5CB),
    onPrimary = Color(0xFF003733),
    primaryContainer = Color(0xFF00504B),
    onPrimaryContainer = Color(0xFFA8F1E6),
    secondary = Color(0xFFB6CCC8),
    onSecondary = Color(0xFF203331),
    secondaryContainer = Color(0xFF364A47),
    onSecondaryContainer = Color(0xFFD2E8E4),
    background = Color(0xFF101413),
    surface = Color(0xFF101413),
    surfaceVariant = Color(0xFF404947),
    onSurface = Color(0xFFE0E4E2),
    onSurfaceVariant = Color(0xFFC0C9C6),
    error = Color(0xFFFFB4AB),
)

@Composable
fun MassPdfTheme(
    mode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val useDarkColors = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    MaterialTheme(
        colorScheme = if (useDarkColors) DarkColors else LightColors,
        content = content,
    )
}
