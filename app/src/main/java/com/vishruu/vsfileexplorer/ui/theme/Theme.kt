package com.vishruu.vsfileexplorer.ui.theme


import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.core.view.WindowCompat

data class FontOption(val name: String, val family: FontFamily)

val FontOptions = listOf(
    FontOption("Default", FontFamily.Default),
    FontOption("Sans Serif", FontFamily.SansSerif),
    FontOption("Serif", FontFamily.Serif),
    FontOption("Monospace", FontFamily.Monospace),
    FontOption("Cursive", FontFamily.Cursive)
)

val DefaultFont: FontFamily = FontFamily.Default

private fun Color.darkenForLightMode(): Color {
    // If accent is very light (like white), darken it for text visibility
    return if (this.luminance() > 0.5f) {
        Color(
            red = this.red * 0.45f,
            green = this.green * 0.45f,
            blue = this.blue * 0.45f,
            alpha = 1f
        )
    } else this
}

@Composable
fun VSFileExplorerTheme(
    accentColor: Color = DefaultAccent,
    fontFamily: FontFamily = DefaultFont,
    isDark: Boolean = true,
    content: @Composable () -> Unit
) {
    val effectiveAccent = if (isDark) {
        accentColor
    } else {
        // If accent is too light (like white), use fallback purple in light mode
        if (accentColor.luminance() > 0.5f) Color(0xFF7B1FA2) else accentColor
    }

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = accentColor,
            onPrimary = AmoledBlack,
            primaryContainer = accentColor.copy(alpha = 0.25f),
            onPrimaryContainer = TextPrimary,
            secondary = accentColor,
            onSecondary = AmoledBlack,
            background = AmoledBlack,
            onBackground = TextPrimary,
            surface = SurfaceDark,
            onSurface = TextPrimary,
            surfaceVariant = SurfaceVariantDark,
            onSurfaceVariant = TextSecondary,
            outline = BorderDark,
            outlineVariant = BorderDark,
            error = Color_ErrorRed,
            onError = TextPrimary
        )
    } else {
        lightColorScheme(
            primary = effectiveAccent,
            onPrimary = Color.White,
            primaryContainer = effectiveAccent.copy(alpha = 0.15f),
            onPrimaryContainer = Color(0xFF0D0D0F),
            secondary = effectiveAccent,
            onSecondary = Color.White,
            background = Color(0xFFEDEDF0),
            onBackground = Color(0xFF0D0D0F),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF0D0D0F),
            surfaceVariant = Color(0xFFDCDCE0),
            onSurfaceVariant = Color(0xFF2A2A2E),
            outline = Color(0xFF9E9EA3),
            outlineVariant = Color(0xFFC4C4C8),
            error = Color(0xFFC62828),
            onError = Color.White
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            try {
                val window = (view.context as Activity).window
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !isDark
                controller.isAppearanceLightNavigationBars = !isDark
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = buildTypography(fontFamily),
        content = content
    )
}