package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CustomThemeColors
import com.example.ui.theme.Spacing

@Composable
fun ToolScreenScaffold(
    colors: CustomThemeColors,
    icon: ImageVector,
    title: String,
    subtitle: String,
    onPrimaryActionClick: (() -> Unit)? = null,
    primaryActionText: String? = null,
    showResult: Boolean = false,
    resultMainText: String? = null,
    resultSubText: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    // Dynamic background with proper colors.appBg
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.appBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Hero Card Header (Dynamic background color)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(colors.headerBg, colors.appBg)
                        )
                    )
                    .padding(horizontal = Spacing.Medium, vertical = Spacing.Large),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(Spacing.Small))
                    Text(
                        text = title,
                        color = colors.text, // Dynamic text color
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = subtitle,
                        color = colors.textMuted, // Dynamic muted color
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }

            // 2. Content Surface (Removed weight to prevent scroll measurement crash!)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.Medium),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = colors.surface.copy(alpha = 0.75f) // Frosted style
            ) {
                Column(
                    modifier = Modifier
                        .padding(Spacing.Medium)
                        .fillMaxWidth()
                ) {
                    content()

                    // Result Area
                    if (showResult) {
                        Spacer(modifier = Modifier.height(Spacing.Large))
                        Surface(
                            color = colors.accent.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(Spacing.Medium),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (resultMainText != null) {
                                    Text(
                                        text = resultMainText,
                                        color = colors.accent,
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                if (resultSubText != null) {
                                    Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
                                    Text(
                                        text = resultSubText,
                                        color = colors.textMuted,
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // Primary Action Button placed directly inside the Column to prevent overlapping
                    if (primaryActionText != null && onPrimaryActionClick != null) {
                        Spacer(modifier = Modifier.height(Spacing.Large))
                        Button(
                            onClick = onPrimaryActionClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                            shape = RoundedCornerShape(16.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Text(
                                text = primaryActionText,
                                color = colors.appBg, // high contrast text matching spec
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.ExtraLarge))
                }
            }
        }
    }
}
