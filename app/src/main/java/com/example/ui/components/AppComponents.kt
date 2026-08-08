package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CalcKey
import com.example.ui.theme.AppIcons
import com.example.ui.theme.AppThemeKey
import com.example.ui.theme.CustomThemeColors

/**
 * Top app bar. Icon-driven (no emoji), matches the current tool's identity via
 * AppIcons.forCalc, and keeps a consistent 56dp Material touch target for actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppHeader(
    currentCalc: CalcKey,
    colors: CustomThemeColors,
    onGoHome: () -> Unit,
    onToggleTheme: () -> Unit,
    onOpenThemes: () -> Unit,
    onOpenAbout: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }

    Surface(
        color = colors.headerBg,
        contentColor = colors.headerFg,
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (currentCalc != CalcKey.HOME) {
                IconButton(onClick = onGoHome) {
                    Icon(
                        imageVector = AppIcons.Back,
                        contentDescription = "الرجوع للرئيسية",
                        tint = colors.headerFg
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .size(32.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(colors.accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.forCalc(CalcKey.BASIC),
                        contentDescription = "ClevCalc Pro",
                        tint = colors.accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                if (currentCalc != CalcKey.HOME) {
                    Icon(
                        imageVector = AppIcons.forCalc(currentCalc),
                        contentDescription = null,
                        tint = colors.headerFg,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = currentCalc.title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.headerFg,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (colors.isDark) AppIcons.Sun else AppIcons.Moon,
                            contentDescription = if (colors.isDark) "الوضع النهاري" else "الوضع الليلي",
                            tint = colors.accent
                        )
                    }
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(AppIcons.More, contentDescription = "خيارات", tint = colors.headerFg)
                    }
                }

                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                    modifier = Modifier.background(colors.surface)
                ) {
                    DropdownMenuItem(
                        text = { Text("حول التطبيق", color = colors.text) },
                        leadingIcon = { Icon(AppIcons.Info, contentDescription = null, tint = colors.textMuted) },
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
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.forCalc(CalcKey.BASIC),
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
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
                        Text("الميزات الاحترافية", fontWeight = FontWeight.Bold, color = colors.accent, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("• 30 حاسبة وأداة إسلامية ومالية شاملة", fontSize = 12.sp, color = colors.text)
                        Text("• أسعار حية للعملات والذهب والفضة والنفط", fontSize = 12.sp, color = colors.text)
                        Text("• مواقيت الصلاة والقبلة والقرآن الكريم والأذكار", fontSize = 12.sp, color = colors.text)
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
                        Text("مصادر البيانات المباشرة", fontWeight = FontWeight.Bold, color = Color(0xFF10B981), fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("• العملات (166 عملة): exchangerate.fun", fontSize = 11.sp, color = colors.textMuted)
                        Text("• المعادن والذهب: gold-api.com", fontSize = 11.sp, color = colors.textMuted)
                        Text("• مواقيت الصلاة والقبلة: aladhan.com", fontSize = 11.sp, color = colors.textMuted)
                        Text("• القرآن الكريم: alquran.cloud", fontSize = 11.sp, color = colors.textMuted)
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

/**
 * Reusable Atomic AppButton component adhering to M3 rules,
 * 48dp minimum touch target, and flexible state hoisting.
 */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ),
    shape: androidx.compose.ui.graphics.Shape = MaterialTheme.shapes.medium,
    contentDescription: String? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
        colors = colors,
        shape = shape
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Reusable LocationDisabledStateView for location state handling.
 */
@Composable
fun LocationDisabledStateView(
    onEnableLocationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        AppButton(
            text = "تفعيل الموقع الجغرافي",
            onClick = onEnableLocationClick,
            modifier = Modifier.padding(16.dp)
        )
    }
}

