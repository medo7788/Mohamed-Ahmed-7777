package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CustomThemeColors
import com.example.ui.theme.DesignTokens
import com.example.ui.theme.Spacing

enum class FrostedGlassCardVariant {
    Hero, Standard, Compact
}

@Composable
fun FrostedGlassCard(
    colors: CustomThemeColors,
    modifier: Modifier = Modifier,
    variant: FrostedGlassCardVariant = FrostedGlassCardVariant.Standard,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val alpha = when (variant) {
        FrostedGlassCardVariant.Hero -> DesignTokens.GlassAlpha.Obsidian
        FrostedGlassCardVariant.Standard -> DesignTokens.GlassAlpha.Standard
        FrostedGlassCardVariant.Compact -> DesignTokens.GlassAlpha.Subtle
    }

    val radius = when (variant) {
        FrostedGlassCardVariant.Hero -> DesignTokens.Radius.Large
        FrostedGlassCardVariant.Standard -> DesignTokens.Radius.Medium
        FrostedGlassCardVariant.Compact -> DesignTokens.Radius.Small
    }

    val elevation = when (variant) {
        FrostedGlassCardVariant.Hero -> DesignTokens.Elevation.Hero
        else -> DesignTokens.Elevation.Card
    }

    val cardBgColor = if (colors.isDark) {
        Color(0xFF1E262C).copy(alpha = alpha)
    } else {
        colors.surface.copy(alpha = alpha)
    }

    val borderBrush = Brush.linearGradient(
        colors = listOf(
            colors.accent.copy(alpha = 0.35f),
            colors.border.copy(alpha = 0.15f)
        )
    )

    Surface(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = RoundedCornerShape(radius),
                clip = false
            )
            .clip(RoundedCornerShape(radius))
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        color = cardBgColor,
        border = BorderStroke(1.2.dp, borderBrush)
    ) {
        Column(
            modifier = Modifier.padding(
                when (variant) {
                    FrostedGlassCardVariant.Hero -> 20.dp
                    FrostedGlassCardVariant.Standard -> 16.dp
                    FrostedGlassCardVariant.Compact -> 12.dp
                }
            )
        ) {
            content()
        }
    }
}

@Composable
fun HeroSummaryCard(
    colors: CustomThemeColors,
    title: String,
    subtitle: String,
    primaryValue: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable (ColumnScope.() -> Unit)? = null
) {
    FrostedGlassCard(
        colors = colors,
        variant = FrostedGlassCardVariant.Hero,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.accent
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = primaryValue,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.text
                )
            }

            Box(
                modifier = Modifier
                    .size(DesignTokens.IconSize.Hero)
                    .clip(CircleShape)
                    .background(colors.accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(DesignTokens.IconSize.Large)
                )
            }
        }

        if (content != null) {
            Spacer(modifier = Modifier.height(Spacing.Medium))
            content()
        }
    }
}

@Composable
fun ExpandableCategoryCard(
    colors: CustomThemeColors,
    title: String,
    icon: ImageVector,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) -90f else 0f,
        animationSpec = tween(DesignTokens.Motion.Normal)
    )

    FrostedGlassCard(
        colors = colors,
        variant = FrostedGlassCardVariant.Standard,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(DesignTokens.IconSize.Medium)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.text
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier
                    .rotate(arrowRotation)
                    .size(20.dp)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(animationSpec = tween(DesignTokens.Motion.Normal)) + fadeIn(),
            exit = shrinkVertically(animationSpec = tween(DesignTokens.Motion.Normal)) + fadeOut()
        ) {
            Column {
                Spacer(modifier = Modifier.height(Spacing.Medium))
                content()
            }
        }
    }
}

@Composable
fun GoldPrimaryButton(
    colors: CustomThemeColors,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(52.dp)
            .shadow(DesignTokens.Elevation.Button, RoundedCornerShape(DesignTokens.Radius.Small)),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.accent,
            contentColor = colors.appBg
        ),
        shape = RoundedCornerShape(DesignTokens.Radius.Small)
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun GlassSecondaryButton(
    colors: CustomThemeColors,
    text: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        border = BorderStroke(1.2.dp, colors.accent.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(DesignTokens.Radius.Small),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = colors.text
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun GlassChip(
    colors: CustomThemeColors,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chipBg = if (selected) {
        colors.accent
    } else {
        colors.surface2.copy(alpha = 0.5f)
    }

    val textColor = if (selected) {
        colors.appBg
    } else {
        colors.text
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(chipBg)
            .border(
                1.dp,
                if (selected) colors.accent else colors.border.copy(alpha = 0.2f),
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
fun SectionHeader(
    colors: CustomThemeColors,
    title: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.accent)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = colors.text
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(colors.accent.copy(alpha = 0.2f), Color.Transparent)
                    )
                )
        )
    }
}

@Composable
fun EmptyStateBlock(
    colors: CustomThemeColors,
    message: String,
    buttonText: String? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    FrostedGlassCard(
        colors = colors,
        variant = FrostedGlassCardVariant.Compact,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = message,
                fontSize = 12.sp,
                color = colors.textMuted,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            if (buttonText != null && onClick != null) {
                Spacer(modifier = Modifier.height(16.dp))
                GoldPrimaryButton(
                    colors = colors,
                    text = buttonText,
                    onClick = onClick,
                    modifier = Modifier.wrapContentWidth()
                )
            }
        }
    }
}

@Composable
fun HubCategoryCard(
    colors: CustomThemeColors,
    title: String,
    icon: ImageVector,
    toolCount: Int,
    gradient: Brush,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FrostedGlassCard(
        colors = colors,
        variant = FrostedGlassCardVariant.Standard,
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(gradient),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(DesignTokens.IconSize.Medium)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.text
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$toolCount أدوات عملية ومفيدة",
                        fontSize = 11.sp,
                        color = colors.textMuted
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun GlassSearchBar(
    colors: CustomThemeColors,
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                text = placeholder,
                fontSize = 13.sp,
                color = colors.textMuted
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(20.dp)
            )
        },
        singleLine = true,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(DesignTokens.Elevation.Card, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = if (colors.isDark) Color(0xFF1E262C).copy(alpha = 0.85f) else colors.surface.copy(alpha = 0.85f),
            unfocusedContainerColor = if (colors.isDark) Color(0xFF1E262C).copy(alpha = 0.75f) else colors.surface.copy(alpha = 0.75f),
            focusedBorderColor = colors.accent,
            unfocusedBorderColor = colors.border.copy(alpha = 0.3f),
            focusedTextColor = colors.text,
            unfocusedTextColor = colors.text
        )
    )
}

@Composable
fun GlassListItem(
    colors: CustomThemeColors,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface.copy(alpha = 0.4f))
            .border(1.dp, colors.border.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
fun PremiumInfoRow(
    colors: CustomThemeColors,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = colors.textMuted
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = colors.text
        )
    }
}

@Composable
fun MetricRow(
    colors: CustomThemeColors,
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = colors.textMuted
            )
        }
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = colors.text
        )
    }
}

@Composable
fun SettingRow(
    colors: CustomThemeColors,
    title: String,
    subtitle: String,
    action: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colors.text
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = colors.textMuted
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        action()
    }
}
