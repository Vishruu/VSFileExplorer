package com.vishruu.vsfileexplorer.ui.theme

import androidx.compose.ui.graphics.Color

// Backgrounds
val AmoledBlack = Color(0xFF000000)
val SurfaceDark = Color(0xFF0F0F12)
val SurfaceVariantDark = Color(0xFF1A1A1F)
val CardDark = Color(0xFF16161C)

// Text
val TextPrimary = Color(0xFFF5F5F7)
val TextSecondary = Color(0xFFB0B0B8)
val TextMuted = Color(0xFF6E6E76)

// Borders
val BorderDark = Color(0xFF2A2A32)

// Error
val Color_ErrorRed = Color(0xFFEF4444)

// Accent options
data class AccentOption(val name: String, val color: Color)

val AccentOptions = listOf(
    AccentOption("White", Color(0xFFFFFFFF)),
    AccentOption("Purple", Color(0xFF8B5CF6)),
    AccentOption("Blue", Color(0xFF3B82F6)),
    AccentOption("Green", Color(0xFF10B981)),
    AccentOption("Orange", Color(0xFFF97316)),
    AccentOption("Pink", Color(0xFFEC4899)),
    AccentOption("Red", Color(0xFFEF4444)),
    AccentOption("Yellow", Color(0xFFFACC15)),
    AccentOption("Cyan", Color(0xFF06B6D4))
)

val DefaultAccent = AccentOptions[0].color