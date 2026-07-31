package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class AppThemeKey(val titleAr: String, val icon: String, val previewColorHex: String) {
    ELEGANT_DARK("داكن ملكي", "🌙", "#121417"),
    LIGHT("فاتح ملكي", "☀️", "#FAF8F5"),
    EMERALD_ISLAMIC("زمردي إسلامي", "🕌", "#042F2C"),
    MIDNIGHT_PURPLE("ليل أرجواني", "🔮", "#1E1B2E"),
    OCEAN_BLUE("أزرق ملكي", "🌊", "#0B192C"),
    GOLDEN_LUXURY("ذهبي كلاسيك", "✨", "#1C1917")
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
        AppThemeKey.EMERALD_ISLAMIC -> CustomThemeColors(
            appBg = Color(0xFF042F2C),
            headerBg = Color(0xFF0A403C),
            headerFg = Color(0xFFFFD700),
            surface = Color(0xFF0C4A45),
            surface2 = Color(0xFF125C56),
            border = Color(0x40FFD700),
            text = Color(0xFFE6F4F1),
            textMuted = Color(0xFFA3CFCB),
            accent = Color(0xFFFFD700),
            accentSecondary = Color(0xFF2DD4BF),
            isDark = true
        )
        AppThemeKey.MIDNIGHT_PURPLE -> CustomThemeColors(
            appBg = Color(0xFF13111C),
            headerBg = Color(0xFF231E38),
            headerFg = Color(0xFFF472B6),
            surface = Color(0xFF1E1B2E),
            surface2 = Color(0xFF2D2845),
            border = Color(0x33C084FC),
            text = Color(0xFFF3E8FF),
            textMuted = Color(0xFFA78BFA),
            accent = Color(0xFFC084FC),
            accentSecondary = Color(0xFFF472B6),
            isDark = true
        )
        AppThemeKey.OCEAN_BLUE -> CustomThemeColors(
            appBg = Color(0xFF0B192C),
            headerBg = Color(0xFF1E3E62),
            headerFg = Color(0xFF38BDF8),
            surface = Color(0xFF1E293B),
            surface2 = Color(0xFF334155),
            border = Color(0x3338BDF8),
            text = Color(0xFFF1F5F9),
            textMuted = Color(0xFF94A3B8),
            accent = Color(0xFF38BDF8),
            accentSecondary = Color(0xFF0EA5E9),
            isDark = true
        )
        AppThemeKey.GOLDEN_LUXURY -> CustomThemeColors(
            appBg = Color(0xFF1C1917),
            headerBg = Color(0xFF292524),
            headerFg = Color(0xFFF59E0B),
            surface = Color(0xFF292524),
            surface2 = Color(0xFF3B3533),
            border = Color(0x40F59E0B),
            text = Color(0xFFFAFAF9),
            textMuted = Color(0xFFA8A29E),
            accent = Color(0xFFF59E0B),
            accentSecondary = Color(0xFFFBBF24),
            isDark = true
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
