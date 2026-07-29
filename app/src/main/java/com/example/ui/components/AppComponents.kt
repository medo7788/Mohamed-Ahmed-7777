package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.model.CalcKey
import com.example.model.CategoryKey
import com.example.ui.theme.AppThemeKey
import com.example.ui.theme.CustomThemeColors
import com.example.ui.theme.GradientTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppHeader(
    currentCalc: CalcKey,
    colors: CustomThemeColors,
    onGoHome: () -> Unit,
    onOpenThemes: () -> Unit,
    onOpenAbout: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }

    Surface(
        color = colors.headerBg,
        contentColor = colors.headerFg,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (currentCalc != CalcKey.HOME) {
                IconButton(onClick = onGoHome) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "الرجوع للرئيسية",
                        tint = colors.headerFg
                    )
                }
            } else {
                IconButton(onClick = {}, enabled = false) {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = "ClevCalc Pro",
                        tint = colors.headerFg
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = currentCalc.icon, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = currentCalc.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.headerFg,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onOpenThemes) {
                        Icon(Icons.Default.Palette, contentDescription = "المظهر", tint = colors.headerFg)
                    }
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "خيارات", tint = colors.headerFg)
                    }
                }

                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                    modifier = Modifier.background(colors.surface)
                ) {
                    DropdownMenuItem(
                        text = { Text("حول التطبيق ℹ️", color = colors.text) },
                        onClick = {
                            menuOpen = false
                            onOpenAbout()
                        }
                    )
                }
            }
        }
    }
}



@Composable
fun ThemeSelectorModal(
    currentTheme: AppThemeKey,
    colors: CustomThemeColors,
    onSelectTheme: (AppThemeKey) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "🎨 اختر مظهر التطبيق",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colors.text,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                AppThemeKey.values().forEach { key ->
                    val isSelected = key == currentTheme
                    val tColors = com.example.ui.theme.getThemeColors(key)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) colors.surface2 else Color.Transparent)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) colors.accent else colors.border,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectTheme(key) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(tColors.accent)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "${key.icon} ${key.titleAr}",
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = colors.text
                            )
                        }

                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = colors.accent)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق", color = colors.accent)
            }
        },
        containerColor = colors.surface,
        shape = RoundedCornerShape(18.dp)
    )
}

@Composable
fun AboutModal(
    colors: CustomThemeColors,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🧮", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("ClevCalc Pro", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.text)
                Text("الإصدار 2.0.0", fontSize = 12.sp, color = colors.textMuted)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Surface(
                    color = colors.surface2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("✨ الميزات الاحترافية", fontWeight = FontWeight.Bold, color = colors.accent, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("• 30 حاسبة وأداة إسلامية ومالية شاملة", fontSize = 12.sp, color = colors.text)
                        Text("• أسعار حية للعملات والذهب والفضة والنفط", fontSize = 12.sp, color = colors.text)
                        Text("• مواقيت الصلاة والقبلة والقرآن الكريم الأذكار", fontSize = 12.sp, color = colors.text)
                        Text("• 8 ثيمات جذابة قابلة للتخصيص", fontSize = 12.sp, color = colors.text)
                        Text("• مساعد ذكي متقدم بالذكاء الاصطناعي (Gemini)", fontSize = 12.sp, color = colors.text)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    color = colors.surface2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("🌐 مصادر البيانات المباشرة", fontWeight = FontWeight.Bold, color = Color(0xFF10B981), fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("• 💱 العملات (166 عملة): exchangerate.fun", fontSize = 11.sp, color = colors.textMuted)
                        Text("• 🥇 المعادن والذهب: gold-api.com", fontSize = 11.sp, color = colors.textMuted)
                        Text("• 🕌 مواقيت الصلاة والقبلة: aladhan.com", fontSize = 11.sp, color = colors.textMuted)
                        Text("• 📖 القرآن الكريم: alquran.cloud", fontSize = 11.sp, color = colors.textMuted)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
            ) {
                Text("حسناً", color = Color.White)
            }
        },
        containerColor = colors.surface,
        shape = RoundedCornerShape(18.dp)
    )
}
