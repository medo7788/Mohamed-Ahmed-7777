package com.example.ui.components

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CustomThemeColors
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

@Composable
fun ToolScreenScaffold(
    colors: CustomThemeColors,
    icon: ImageVector,
    title: String,
    subtitle: String = "",
    onBackClick: (() -> Unit)? = null,
    onPrimaryActionClick: (() -> Unit)? = null,
    primaryActionText: String? = null,
    showResult: Boolean = false,
    resultMainText: String? = null,
    resultSubText: String? = null,
    isScrollable: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val hazeState = remember { HazeState() }
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    // 5. Visuals: Deep dark cyberpunk background (#0B1120 to #0F172A)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0B1120),
                        Color(0xFF0F172A),
                        Color(0xFF070A0F)
                    )
                )
            )
            .haze(hazeState)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (isScrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
        ) {
            // 1. Top App Bar (Compact & Space-Efficient):
            // Minimalist header with back arrow, 24px neon icon, centered title. Zero vertical waste!
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = {
                        if (onBackClick != null) {
                            onBackClick()
                        } else {
                            backDispatcher?.onBackPressed()
                        }
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "رجوع",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF00FFCC).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFF00FFCC).copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color(0xFF00FFCC),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.size(40.dp))
            }

            // 2. Main Workspace (Inputs & Interactive Controls):
            // Begins immediately below top bar in modern Glassmorphism card (#1E293B, neon cyan border)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF1E293B).copy(alpha = 0.88f),
                border = BorderStroke(
                    1.2.dp,
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF00FFCC).copy(alpha = 0.45f),
                            Color(0xFF1E293B).copy(alpha = 0.2f),
                            Color(0xFF00FFCC).copy(alpha = 0.3f)
                        )
                    )
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(14.dp)
                        .fillMaxWidth()
                ) {
                    CompositionLocalProvider(LocalHazeState provides hazeState) {
                        content()
                    }

                    // 3. Output & Results Card:
                    // Distinct glowing card displaying calculations/results in bold golden digital typography (#FFB703)
                    if (showResult || resultMainText != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            color = Color(0xFF0F172A).copy(alpha = 0.95f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.5.dp, Color(0xFFFFB703).copy(alpha = 0.6f)),
                            shadowElevation = 6.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (resultMainText != null) {
                                    Text(
                                        text = resultMainText,
                                        color = Color(0xFFFFB703),
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                if (resultSubText != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = resultSubText,
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // Primary Action Button (Gold Cyberpunk Button)
                    if (primaryActionText != null && onPrimaryActionClick != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onPrimaryActionClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFB703),
                                contentColor = Color(0xFF0B1120)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = primaryActionText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(90.dp)) // Floating Bottom Nav clearance
        }
    }
}

