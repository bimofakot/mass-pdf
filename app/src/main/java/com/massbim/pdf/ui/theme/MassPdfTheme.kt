// Tema visual Mass PDF: terang Kirmizi, gelap netral, font Plus Jakarta Sans, skala teks UI.
// Ubah warna di LightColors/DarkColors; ukuran teks hanya memengaruhi antarmuka, bukan isi PDF.

package com.massbim.pdf.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import com.massbim.pdf.R

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class TextSizeMode {
    SYSTEM,
    SMALL,
    STANDARD,
    LARGE,
}

private val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
)

private val AppTypography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = PlusJakartaSans),
        displayMedium = base.displayMedium.copy(fontFamily = PlusJakartaSans),
        displaySmall = base.displaySmall.copy(fontFamily = PlusJakartaSans),
        headlineLarge = base.headlineLarge.copy(fontFamily = PlusJakartaSans),
        headlineMedium = base.headlineMedium.copy(fontFamily = PlusJakartaSans),
        headlineSmall = base.headlineSmall.copy(fontFamily = PlusJakartaSans),
        titleLarge = base.titleLarge.copy(fontFamily = PlusJakartaSans),
        titleMedium = base.titleMedium.copy(fontFamily = PlusJakartaSans),
        titleSmall = base.titleSmall.copy(fontFamily = PlusJakartaSans),
        bodyLarge = base.bodyLarge.copy(fontFamily = PlusJakartaSans),
        bodyMedium = base.bodyMedium.copy(fontFamily = PlusJakartaSans),
        bodySmall = base.bodySmall.copy(fontFamily = PlusJakartaSans),
        labelLarge = base.labelLarge.copy(fontFamily = PlusJakartaSans),
        labelMedium = base.labelMedium.copy(fontFamily = PlusJakartaSans),
        labelSmall = base.labelSmall.copy(fontFamily = PlusJakartaSans),
    )
}

private val LightColors = lightColorScheme(
    primary = Color(0xFFB91C1C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF9DEDC),
    onPrimaryContainer = Color(0xFF410002),
    secondary = Color(0xFF7A5A55),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF2E3DF),
    onSecondaryContainer = Color(0xFF2E1512),
    tertiary = Color(0xFFB45309),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFBE7C9),
    onTertiaryContainer = Color(0xFF3B2100),
    background = Color(0xFFFBF7F4),
    onBackground = Color(0xFF221A18),
    surface = Color(0xFFFBF7F4),
    onSurface = Color(0xFF221A18),
    surfaceVariant = Color(0xFFF0E7E2),
    onSurfaceVariant = Color(0xFF6B5B56),
    outline = Color(0xFFDCCFC9),
    outlineVariant = Color(0xFFE9DFDA),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
)

private val DarkColors = darkColorScheme(
    // Tema gelap netral: latar hitam-abu tanpa rona pink, aksen merah #FF5A4E.
    // Nilai dipilih agar kontras teks minimal 4,5:1 di atas latar dan kartu.
    primary = Color(0xFFFF5A4E),
    onPrimary = Color(0xFF2A0A07),
    primaryContainer = Color(0xFF4A1512),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFFCFCFCF),
    onSecondary = Color(0xFF1A1A1A),
    secondaryContainer = Color(0xFF3A3A3A),
    onSecondaryContainer = Color(0xFFECECEC),
    tertiary = Color(0xFFF5B971),
    onTertiary = Color(0xFF46280A),
    tertiaryContainer = Color(0xFF4A3411),
    onTertiaryContainer = Color(0xFFFFDDB6),
    background = Color(0xFF121212),
    onBackground = Color(0xFFECECEC),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFECECEC),
    surfaceVariant = Color(0xFF333333),
    onSurfaceVariant = Color(0xFFC7C7C7),
    outline = Color(0xFF7A7A7A),
    outlineVariant = Color(0xFF3D3D3D),
    error = Color(0xFFFF897D),
    onError = Color(0xFF4A0B06),
    errorContainer = Color(0xFF5A1A14),
    onErrorContainer = Color(0xFFFFDAD6),
    surfaceContainerLowest = Color(0xFF0E0E0E),
    surfaceContainerLow = Color(0xFF171717),
    surfaceContainer = Color(0xFF1C1C1C),
    surfaceContainerHigh = Color(0xFF262626),
    surfaceContainerHighest = Color(0xFF303030),
)

@Composable
fun MassPdfTheme(
    mode: ThemeMode,
    textSize: TextSizeMode = TextSizeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val useDarkColors = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val density = LocalDensity.current
    val fontScale = when (textSize) {
        TextSizeMode.SYSTEM -> density.fontScale
        TextSizeMode.SMALL -> 0.9f
        TextSizeMode.STANDARD -> 1f
        TextSizeMode.LARGE -> 1.15f
    }
    val colorScheme = if (useDarkColors) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !useDarkColors
                isAppearanceLightNavigationBars = !useDarkColors
            }
        }
    }

    CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content,
        )
    }
}
