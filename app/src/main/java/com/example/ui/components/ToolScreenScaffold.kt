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
import com.example.ui.theme.GradientTokens
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GradientTokens.MainBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Hero Card Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
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
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = subtitle,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 2. Content Surface
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .padding(horizontal = Spacing.Medium),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = colors.surface
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

                    Spacer(modifier = Modifier.height(Spacing.ExtraLarge))
                }
            }
        }

        // 3. Main Action Button (Fixed at bottom if needed, but requested inside scroll or bottom)
        // User said: "Prominent primary button styled in Gold (#D4AF37) placed at the bottom."
        // I'll place it inside the column at the end of the content surface for better responsiveness in scroll.
        if (primaryActionText != null && onPrimaryActionClick != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(Spacing.Medium)
            ) {
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
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
