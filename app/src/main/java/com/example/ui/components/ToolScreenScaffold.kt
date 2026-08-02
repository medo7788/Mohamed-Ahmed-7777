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
import com.example.ui.theme.DesignTokens
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
    isScrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(colors.headerBg, colors.appBg)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (isScrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .statusBarsPadding()
        ) {
            // 1. Hero Card Header (Respects active theme colors)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.Medium, vertical = Spacing.Large),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(colors.accent.copy(alpha = 0.12f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(Spacing.Small))
                    Text(
                        text = title,
                        color = colors.text,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        color = colors.textMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }

            // 2. Content Surface (Clean, consistent z-axis spacing, no illegal weights)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.Medium),
                shape = RoundedCornerShape(topStart = DesignTokens.Radius.Medium, topEnd = DesignTokens.Radius.Medium),
                color = colors.surface.copy(alpha = 0.85f),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier
                        .padding(Spacing.Medium)
                        .fillMaxWidth()
                ) {
                    content()

                    // Result Area (Glassmorphic results container)
                    if (showResult) {
                        Spacer(modifier = Modifier.height(Spacing.Large))
                        Surface(
                            color = colors.accent.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(DesignTokens.Radius.Small),
                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.accent.copy(alpha = 0.25f)),
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
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Black,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                if (resultSubText != null) {
                                    Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
                                    Text(
                                        text = resultSubText,
                                        color = colors.text,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // 3. Prominent primary action button placed directly inline after content
                    if (primaryActionText != null && onPrimaryActionClick != null) {
                        Spacer(modifier = Modifier.height(Spacing.Large))
                        GoldPrimaryButton(
                            colors = colors,
                            text = primaryActionText,
                            onClick = onPrimaryActionClick,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.ExtraLarge))
                }
            }

            Spacer(modifier = Modifier.height(90.dp)) // Extra bottom spacing to avoid bottom nav clipping
        }
    }
}
