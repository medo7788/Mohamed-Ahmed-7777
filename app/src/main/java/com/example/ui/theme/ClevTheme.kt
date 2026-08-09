package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class AppThemeKey(val titleAr: String, val icon: String, val previewColorHex: String) {
    ELEGANT_DARK("داكن ملكي", "🌙", "#121417"),
    LIGHT("فاتح ملكي", "☀️", "#FAF8F5")
}

data class CustomThemeColors(
    val appBg: Color,
    val headerBg: Color,
    val headerFg: Color,
    val surface: Color,
    val surface2: Color,
    val border: Color,
    val text: Color,
    val textMuted: Color,
    val accent: Color,
    val accentSecondary: Color,
    val isDark: Boolean,

    // Semantic purpose-specific colors as requested in Phase 2.1
    val positive: Color = Color(0xFF10B981),
    val negative: Color = Color(0xFFEF4444),
    val warning: Color = Color(0xFFF59E0B)
)

fun getThemeColors(key: AppThemeKey): CustomThemeColors {
    return when (key) {
        AppThemeKey.ELEGANT_DARK -> CustomThemeColors(
            appBg = Color(0xFF080A0F),            // Obsidian BG
            headerBg = Color(0xFF0C1018),          // Deep Obsidian / Surface 2
            headerFg = Color(0xFFD4AF37),          // Champagne Gold
            surface = Color(0xFF121620),           // Surface
            surface2 = Color(0xFF0C1018),          // Deep Obsidian
            border = Color(0x33D4AF37),            // Subtle Champagne Gold
            text = Color(0xFFF8FAFC),              // Text Primary
            textMuted = Color(0xFF94A3B8),         // Muted
            accent = Color(0xFFD4AF37),            // Champagne Gold
            accentSecondary = Color(0xFF00F2FE),   // Ice Cyan
            isDark = true,
            positive = Color(0xFF10B981),          // Emerald
            negative = Color(0xFFEF4444),          // Crimson
            warning = Color(0xFFF59E0B)            // Amber
        )
        AppThemeKey.LIGHT -> CustomThemeColors(
            appBg = Color(0xFFFAF9F6),            // Warm White / Pearl
            headerBg = Color(0xFFFAF9F6),          // Pearl
            headerFg = Color(0xFFB8972E),          // Champagne Gold Controlled
            surface = Color(0xFFFFFFFF),           // Soft White
            surface2 = Color(0xFFF1EDE6),          // Soft Surface 2
            border = Color(0xFFE2D8C7),            // Pearl border
            text = Color(0xFF1E293B),              // Soft Graphite
            textMuted = Color(0xFF64748B),         // Muted light
            accent = Color(0xFFB8972E),            // Champagne Gold Controlled
            accentSecondary = Color(0xFF00B4D8),   // Controlled Cyan
            isDark = false,
            positive = Color(0xFF0D9488),          // Controlled Emerald
            negative = Color(0xFFEF4444),          // Crimson
            warning = Color(0xFFF59E0B)            // Amber
        )
    }
}

@Composable
fun ClevCalcTheme(
    themeKey: AppThemeKey = AppThemeKey.ELEGANT_DARK,
    content: @Composable () -> Unit
) {
    val themeColors = getThemeColors(themeKey)
    val colorScheme: ColorScheme = if (themeColors.isDark) {
        darkColorScheme(
            primary = themeColors.accent,
            secondary = themeColors.accentSecondary,
            background = themeColors.appBg,
            surface = themeColors.surface,
            onPrimary = Color.White,
            onBackground = themeColors.text,
            onSurface = themeColors.text
        )
    } else {
        lightColorScheme(
            primary = themeColors.accent,
            secondary = themeColors.accentSecondary,
            background = themeColors.appBg,
            surface = themeColors.surface,
            onPrimary = Color.White,
            onBackground = themeColors.text,
            onSurface = themeColors.text
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
