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
    val isDark: Boolean
)

fun getThemeColors(key: AppThemeKey): CustomThemeColors {
    return when (key) {
        AppThemeKey.ELEGANT_DARK -> CustomThemeColors(
            appBg = Color(0xFF121417),
            headerBg = Color(0xFF1A1E22),
            headerFg = Color(0xFFD4AF37),
            surface = Color(0xFF20262B),
            surface2 = Color(0xFF1A1E22),
            border = Color(0x26D4AF37), // subtle gold
            text = Color(0xFFFFFFFF),
            textMuted = Color(0xFFC7CDD4),
            accent = Color(0xFFD4AF37),
            accentSecondary = Color(0xFF22B573),
            isDark = true
        )
        AppThemeKey.LIGHT -> CustomThemeColors(
            appBg = Color(0xFFFAF8F5),
            headerBg = Color(0xFFFAF8F5),
            headerFg = Color(0xFFD4AF37),
            surface = Color(0xFFFFFFFF),
            surface2 = Color(0xFFF1EDE6),
            border = Color(0xFFE2D8C7),
            text = Color(0xFF121417),
            textMuted = Color(0xFF5A6B6B),
            accent = Color(0xFFD4AF37),
            accentSecondary = Color(0xFF22B573),
            isDark = false
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
