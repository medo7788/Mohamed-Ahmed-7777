package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CalcKey
import com.example.model.CategoryKey
import com.example.ui.theme.CustomThemeColors
import com.example.ui.theme.AppIcons
import com.example.ui.components.FrostedGlassCard
import com.example.ui.components.FrostedGlassCardVariant
import com.example.ui.components.SectionHeader

@Composable
fun HubScreen(
    category: CategoryKey,
    colors: CustomThemeColors,
    favoriteTools: Set<String>,
    onToggleFavorite: (CalcKey) -> Unit,
    onToolClick: (CalcKey) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryColor = when (category) {
        CategoryKey.ISLAMIC -> Color(0xFF10B981)
        CategoryKey.FINANCE -> Color(0xFFF59E0B)
        CategoryKey.DATE_TIME -> Color(0xFFC084FC)
        CategoryKey.HEALTH -> Color(0xFFEF4444)
        CategoryKey.UTILITIES -> Color(0xFF64748B)
    }

    // Get all tools belonging to this specific hub category
    val categoryTools = remember(category) {
        CalcKey.values().filter { it.category == category && it != CalcKey.HOME && it != CalcKey.SETTINGS }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(colors.appBg, colors.surface2.copy(alpha = 0.5f))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(colors.surface.copy(alpha = 0.4f))
                        .border(1.dp, colors.border.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "العودة",
                        tint = colors.text
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = category.label,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.text
                )
            }

            // Description Hero Banner (Translucent Frosted Card)
            FrostedGlassCard(
                colors = colors,
                variant = FrostedGlassCardVariant.Standard,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(categoryColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = AppIcons.forCategory(category),
                            contentDescription = null,
                            tint = categoryColor,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = category.label,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.text
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = category.description,
                            fontSize = 11.sp,
                            color = colors.textMuted,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Subtitle list header
            SectionHeader(
                colors = colors,
                title = "أدوات التصنيف المتاحة (${categoryTools.size})",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 2-Column Grid of tools in this Hub
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 90.dp, top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(categoryTools, key = { it.id }) { tool ->
                    val isFavorite = favoriteTools.contains(tool.name)
                    PremiumToolCard(
                        tool = tool,
                        colors = colors,
                        isFavorite = isFavorite,
                        onToggleFavorite = { onToggleFavorite(tool) },
                        onClick = { onToolClick(tool) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}
