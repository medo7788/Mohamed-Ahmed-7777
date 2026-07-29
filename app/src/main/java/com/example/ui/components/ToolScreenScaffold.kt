package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
    description: String,
    gradient: Brush,
    inputContent: @Composable ColumnScope.() -> Unit,
    primaryActionText: String? = null,
    onPrimaryActionClick: (() -> Unit)? = null,
    showResult: Boolean = false,
    resultMainText: String? = null,
    resultSubText: String? = null,
    extraContent: @Composable (ColumnScope.() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.appBg)
            .padding(Spacing.Medium),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Hero Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.Medium),
            shadowElevation = 2.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(gradient)
                    .padding(Spacing.Medium),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.Small))
                    Column {
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
                        Text(
                            text = description,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // 2. Input Area
        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.Medium)
        ) {
            Column(
                modifier = Modifier.padding(Spacing.Medium),
                verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
            ) {
                inputContent()
            }
        }

        // 3. Primary Action Button (Optional for live tools)
        if (primaryActionText != null && onPrimaryActionClick != null) {
            Button(
                onClick = onPrimaryActionClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(bottom = Spacing.Medium),
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = primaryActionText,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 4. Result Card
        AnimatedVisibility(
            visible = showResult,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Surface(
                color = colors.surface2,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.Medium)
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.Medium),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (resultMainText != null) {
                        Text(
                            text = resultMainText,
                            color = colors.accent,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )
                    }
                    if (resultSubText != null) {
                        Spacer(modifier = Modifier.height(Spacing.Small))
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
        
        extraContent?.invoke(this)
    }
}
