with open("app/src/main/java/com/example/ui/theme/ClevTheme.kt", "w") as f:
    f.write("""package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class AppThemeKey(val titleAr: String, val icon: String, val previewColorHex: String) {
    ELEGANT_DARK("داكن أنيق", "✨", "#121212"),
    LIGHT("فاتح ناصع", "☀️", "#FAFAFA"),
    GOLD("ذهبي ملكي", "👑", "#D4AF37"),
    ISLAMIC_GREEN("أخضر إسلامي", "🕌", "#046A38"),
    NIGHT_BLUE("أزرق ليلي", "🌌", "#0A192F"),
    DESERT("صحراوي", "🐪", "#C19A6B"),
    ROSE("وردي هادئ", "🌸", "#E5B8B7"),
    MONOCHROME("أحادي", "⚫", "#000000")
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
            appBg = Color(0xFF121212),
            headerBg = Color(0xFF1A1A1A),
            headerFg = Color(0xFFFFFFFF),
            surface = Color(0xFF1E1E1E),
            surface2 = Color(0xFF2C2C2C),
            border = Color(0xFF333333),
            text = Color(0xFFF0F0F0),
            textMuted = Color(0xFFA0A0A0),
            accent = Color(0xFFD4AF37), // Elegant Gold
            accentSecondary = Color(0xFFE5C05B),
            isDark = true
        )
        AppThemeKey.LIGHT -> CustomThemeColors(
            appBg = Color(0xFFF9F9F9),
            headerBg = Color(0xFFFFFFFF),
            headerFg = Color(0xFF121212),
            surface = Color(0xFFFFFFFF),
            surface2 = Color(0xFFF0F0F0),
            border = Color(0xFFE0E0E0),
            text = Color(0xFF121212),
            textMuted = Color(0xFF707070),
            accent = Color(0xFF005C97),
            accentSecondary = Color(0xFF363795),
            isDark = false
        )
        AppThemeKey.GOLD -> CustomThemeColors(
            appBg = Color(0xFFFAF8F5),
            headerBg = Color(0xFF1A1A1A),
            headerFg = Color(0xFFD4AF37),
            surface = Color(0xFFFFFFFF),
            surface2 = Color(0xFFF3E5AB),
            border = Color(0xFFE5C05B),
            text = Color(0xFF2C2C2C),
            textMuted = Color(0xFF8B7355),
            accent = Color(0xFFD4AF37),
            accentSecondary = Color(0xFFAA771C),
            isDark = false
        )
        AppThemeKey.ISLAMIC_GREEN -> CustomThemeColors(
            appBg = Color(0xFFF3F7F3),
            headerBg = Color(0xFF046A38),
            headerFg = Color(0xFFFFFFFF),
            surface = Color(0xFFFFFFFF),
            surface2 = Color(0xFFE8F1E8),
            border = Color(0xFFC8DEC8),
            text = Color(0xFF113311),
            textMuted = Color(0xFF4A7A4A),
            accent = Color(0xFF046A38),
            accentSecondary = Color(0xFF098246),
            isDark = false
        )
        AppThemeKey.NIGHT_BLUE -> CustomThemeColors(
            appBg = Color(0xFF0A192F),
            headerBg = Color(0xFF020C1B),
            headerFg = Color(0xFF64FFDA),
            surface = Color(0xFF112240),
            surface2 = Color(0xFF233554),
            border = Color(0xFF233554),
            text = Color(0xFFCCD6F6),
            textMuted = Color(0xFF8892B0),
            accent = Color(0xFF64FFDA),
            accentSecondary = Color(0xFF4CD2B4),
            isDark = true
        )
        AppThemeKey.DESERT -> CustomThemeColors(
            appBg = Color(0xFFFDF8F5),
            headerBg = Color(0xFF8B5A2B),
            headerFg = Color(0xFFFFFFFF),
            surface = Color(0xFFFFFFFF),
            surface2 = Color(0xFFF5E6D3),
            border = Color(0xFFE6C298),
            text = Color(0xFF4A3018),
            textMuted = Color(0xFF8B7355),
            accent = Color(0xFFC19A6B),
            accentSecondary = Color(0xFFDEB887),
            isDark = false
        )
        AppThemeKey.ROSE -> CustomThemeColors(
            appBg = Color(0xFFFFF7F8),
            headerBg = Color(0xFFFFFFFF),
            headerFg = Color(0xFF333333),
            surface = Color(0xFFFFFFFF),
            surface2 = Color(0xFFFDECEE),
            border = Color(0xFFFAD7DD),
            text = Color(0xFF4A2B2D),
            textMuted = Color(0xFF8B6C6E),
            accent = Color(0xFFE5B8B7),
            accentSecondary = Color(0xFFD49C9B),
            isDark = false
        )
        AppThemeKey.MONOCHROME -> CustomThemeColors(
            appBg = Color(0xFFFFFFFF),
            headerBg = Color(0xFF000000),
            headerFg = Color(0xFFFFFFFF),
            surface = Color(0xFFF5F5F5),
            surface2 = Color(0xFFE0E0E0),
            border = Color(0xFFCCCCCC),
            text = Color(0xFF000000),
            textMuted = Color(0xFF666666),
            accent = Color(0xFF000000),
            accentSecondary = Color(0xFF333333),
            isDark = false
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
""")
