package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FrostedGlassCard
import com.example.ui.components.FrostedGlassCardVariant
import com.example.ui.components.SectionHeader
import com.example.ui.components.ToolScreenScaffold
import com.example.ui.theme.CustomThemeColors

/**
 * شاشة الإعدادات العامة الحقيقية للتطبيق - كانت "المزيد" في الشريط السفلي بتفتح
 * إعدادات الأذان مباشرة بدل شاشة إعدادات شاملة، دي الشاشة اللي كانت ناقصة.
 */
@Composable
fun SettingsScreen(
    colors: CustomThemeColors,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onOpenAdhanSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenPrivacy: () -> Unit
) {
    ToolScreenScaffold(
        colors = colors,
        icon = Icons.Default.Settings,
        title = "إعدادات التطبيق",
        subtitle = "المظهر والتنبيهات والمعلومات العامة"
    ) {
        SectionHeader(colors = colors, title = "المظهر")
        FrostedGlassCard(colors = colors, variant = FrostedGlassCardVariant.Standard) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("الوضع الداكن / الفاتح", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.text)
                    Text(
                        if (isDarkTheme) "الوضع الداكن مفعّل حاليًا" else "الوضع الفاتح مفعّل حاليًا",
                        fontSize = 12.sp, color = colors.textMuted
                    )
                }
                Switch(checked = isDarkTheme, onCheckedChange = { onToggleTheme() })
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        SectionHeader(colors = colors, title = "الصلاة والأذان")
        FrostedGlassCard(colors = colors, variant = FrostedGlassCardVariant.Standard, onClick = onOpenAdhanSettings) {
            SettingsRow(colors, Icons.Default.NotificationsActive, "إعدادات الأذان والمؤذن", "صوت التنبيه، الاهتزاز، وأوقات التذكير")
        }

        Spacer(modifier = Modifier.height(16.dp))
        SectionHeader(colors = colors, title = "عن التطبيق")
        FrostedGlassCard(colors = colors, variant = FrostedGlassCardVariant.Standard, onClick = onOpenAbout) {
            SettingsRow(colors, Icons.Default.Info, "عن ClevCalc Pro", "الإصدار الحالي وبيانات التطبيق")
        }
        Spacer(modifier = Modifier.height(10.dp))
        FrostedGlassCard(colors = colors, variant = FrostedGlassCardVariant.Standard, onClick = onOpenPrivacy) {
            SettingsRow(colors, Icons.Default.PrivacyTip, "سياسة الخصوصية", "كيف بنستخدم بيانات موقعك")
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsRow(colors: CustomThemeColors, icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = colors.accent, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.text)
                Text(subtitle, fontSize = 12.sp, color = colors.textMuted)
            }
        }
        Icon(Icons.Default.ChevronLeft, null, tint = colors.textMuted, modifier = Modifier.size(20.dp))
    }
}
