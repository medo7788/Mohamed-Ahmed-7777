package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class AppThemeKey(val titleAr: String, val icon: String, val previewColorHex: String) {
    ELEGANT_DARK("داكن أنيق", "✨", "#1C1B1F"),
    LIGHT("فاتح ناصع", "☀️", "#FFFFFF"),
    DARK("داكن محيطي", "🌙", "#1E293B"),
    EMERALD("زمردي إسلامي", "🕌", "#059669"),
    VIOLET("بنفسجي ذكي", "✨", "#8B5CF6"),
    GOLD("ذهبي فاخر", "🥇", "#D97706"),
    OCEAN("أزرق سماوي", "🌊", "#0284C7"),
    SUNSET("غروب متألق", "🌅", "#E11D48"),
    MIDNIGHT("ليل عميق", "🌃", "#0F172A")
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
            appBg = Color(0xFF1C1B1F),
            headerBg = Color(0xFF1C1B1F),
            headerFg = Color(0xFFE6E1E5),
            surface = Color(0xFF2B2930),
            surface2 = Color(0xFF36343B),
            border = Color(0xFF49454F),
            text = Color(0xFFE6E1E5),
            textMuted = Color(0xFF938F99),
            accent = Color(0xFFD0BCFF),
            accentSecondary = Color(0xFF4F378B),
            isDark = true
        )
        AppThemeKey.LIGHT -> CustomThemeColors(
            appBg = Color(0xFFF8FAFC),
            headerBg = Color(0xFF1E293B),
            headerFg = Color(0xFFFFFFFF),
            surface = Color(0xFFFFFFFF),
            surface2 = Color(0xFFF1F5F9),
            border = Color(0xFFE2E8F0),
            text = Color(0xFF0F172A),
            textMuted = Color(0xFF64748B),
            accent = Color(0xFF2563EB),
            accentSecondary = Color(0xFF3B82F6),
            isDark = false
        )
        AppThemeKey.DARK -> CustomThemeColors(
            appBg = Color(0xFF0F172A),
            headerBg = Color(0xFF1E293B),
            headerFg = Color(0xFFF8FAFC),
            surface = Color(0xFF1E293B),
            surface2 = Color(0xFF334155),
            border = Color(0xFF334155),
            text = Color(0xFFF8FAFC),
            textMuted = Color(0xFF94A3B8),
            accent = Color(0xFF38BDF8),
            accentSecondary = Color(0xFF818CF8),
            isDark = true
        )
        AppThemeKey.EMERALD -> CustomThemeColors(
            appBg = Color(0xFFF0FDF4),
            headerBg = Color(0xFF065F46),
            headerFg = Color(0xFFFFFFFF),
            surface = Color(0xFFFFFFFF),
            surface2 = Color(0xFFDCFCE7),
            border = Color(0xFFBBF7D0),
            text = Color(0xFF064E3B),
            textMuted = Color(0xFF047857),
            accent = Color(0xFF059669),
            accentSecondary = Color(0xFF10B981),
            isDark = false
        )
        AppThemeKey.VIOLET -> CustomThemeColors(
            appBg = Color(0xFFF5F3FF),
            headerBg = Color(0xFF5B21B6),
            headerFg = Color(0xFFFFFFFF),
            surface = Color(0xFFFFFFFF),
            surface2 = Color(0xFFEDE9FE),
            border = Color(0xFFDDD6FE),
            text = Color(0xFF4C1D95),
            textMuted = Color(0xFF6D28D9),
            accent = Color(0xFF8B5CF6),
            accentSecondary = Color(0xFFA78BFA),
            isDark = false
        )
        AppThemeKey.GOLD -> CustomThemeColors(
            appBg = Color(0xFFFFFBEB),
            headerBg = Color(0xFF78350F),
            headerFg = Color(0xFFFFFFFF),
            surface = Color(0xFFFFFFFF),
            surface2 = Color(0xFFFEF3C7),
            border = Color(0xFFFDE68A),
            text = Color(0xFF451A03),
            textMuted = Color(0xFFB45309),
            accent = Color(0xFFD97706),
            accentSecondary = Color(0xFFF59E0B),
            isDark = false
        )
        AppThemeKey.OCEAN -> CustomThemeColors(
            appBg = Color(0xFFF0F9FF),
            headerBg = Color(0xFF075985),
            headerFg = Color(0xFFFFFFFF),
            surface = Color(0xFFFFFFFF),
            surface2 = Color(0xFFE0F2FE),
            border = Color(0xFFBAE6FD),
            text = Color(0xFF0C4A6E),
            textMuted = Color(0xFF0284C7),
            accent = Color(0xFF0284C7),
            accentSecondary = Color(0xFF38BDF8),
            isDark = false
        )
        AppThemeKey.SUNSET -> CustomThemeColors(
            appBg = Color(0xFFFFF1F2),
            headerBg = Color(0xFF881337),
            headerFg = Color(0xFFFFFFFF),
            surface = Color(0xFFFFFFFF),
            surface2 = Color(0xFFFFE4E6),
            border = Color(0xFFFECDD3),
            text = Color(0xFF4C0519),
            textMuted = Color(0xFFBE123C),
            accent = Color(0xFFE11D48),
            accentSecondary = Color(0xFFFB7185),
            isDark = false
        )
        AppThemeKey.MIDNIGHT -> CustomThemeColors(
            appBg = Color(0xFF0B0F19),
            headerBg = Color(0xFF111827),
            headerFg = Color(0xFFF9FAFB),
            surface = Color(0xFF1F2937),
            surface2 = Color(0xFF374151),
            border = Color(0xFF374151),
            text = Color(0xFFF9FAFB),
            textMuted = Color(0xFF9CA3AF),
            accent = Color(0xFF6366F1),
            accentSecondary = Color(0xFF818CF8),
            isDark = true
        )
    }
}

@Composable
fun ClevCalcTheme(
    themeKey: AppThemeKey = AppThemeKey.LIGHT,
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
