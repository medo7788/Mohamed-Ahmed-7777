package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.CalcKey
import com.example.model.CategoryKey
import com.example.ui.components.LocationCardState
import com.example.ui.components.LocationStatusCard
import com.example.ui.theme.AppIcons
import com.example.ui.theme.CustomThemeColors

@Composable
fun HomeScreen(
    colors: CustomThemeColors,
    onSelectCalc: (CalcKey) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<CategoryKey?>(null) }
    val keyboardController = LocalSoftwareKeyboardController.current

    val allTools = remember { CalcKey.values().filter { it != CalcKey.HOME } }

    val filteredTools = remember(searchQuery, selectedCategory) {
        allTools.filter { tool ->
            val matchesCategory = selectedCategory == null || tool.category == selectedCategory
            val matchesSearch = searchQuery.isBlank() || 
                tool.title.contains(searchQuery, ignoreCase = true) || 
                tool.keywords.any { it.contains(searchQuery, ignoreCase = true) }
            matchesCategory && matchesSearch
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (colors.isDark) {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F1717),
                            Color(0xFF1A2E2E),
                            Color(0xFF0F1415)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFAF8F5),
                            Color(0xFFF3EFEA),
                            Color(0xFFFAF8F5)
                        )
                    )
                }
            )
    ) {
        // Geometric Pattern Background Overlay
        Image(
            painter = painterResource(id = R.drawable.ic_islamic_pattern),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (colors.isDark) 0.04f else 0.02f),
            contentScale = ContentScale.Inside,
            colorFilter = ColorFilter.tint(colors.accent)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 100.dp)
        ) {
            // --- 1. Top Royal Header ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = CircleShape,
                        color = colors.accent.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, colors.accent)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "ClevCalc Pro",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.accent
                        )
                        Text(
                            "31 أداة وحاسبة ملكية",
                            fontSize = 12.sp,
                            color = colors.textMuted
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = colors.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onSelectCalc(CalcKey.AI) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI",
                                tint = Color(0xFFD4AF37),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "المساعد الذكي",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.accent
                            )
                        }
                    }
                }
            }

            // --- 2. Search Field ---
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("ابحث في 31 أداة وحاسبة...", color = colors.textMuted, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = colors.accent) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, null, tint = colors.textMuted)
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface,
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.border,
                    focusedTextColor = colors.text,
                    unfocusedTextColor = colors.text
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            )

            // --- 3. Live Banner Card (Royal Accent) ---
            if (searchQuery.isBlank()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { onSelectCalc(CalcKey.LIVE_PRICES) },
                    color = colors.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.accent.copy(alpha = 0.3f)),
                    shadowElevation = 6.dp
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF27AE60))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "مباشر • الأسعار والأسواق",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF27AE60)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "ذهب، عملات، نفط ومؤشرات اقتصادية",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.text
                                )
                                Text(
                                    "تحديث فوري مع تحليلات ذكية لأحدث التغيرات",
                                    fontSize = 11.sp,
                                    color = colors.textMuted
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFFD4AF37), Color(0xFFB8860B))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }

            // --- 4. Category Filter Chips ---
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    CategoryChip(
                        label = "الكل (${allTools.size})",
                        icon = "✨",
                        isSelected = selectedCategory == null,
                        colors = colors,
                        onClick = { selectedCategory = null }
                    )
                }
                items(CategoryKey.values().toList()) { cat ->
                    val count = allTools.count { it.category == cat }
                    CategoryChip(
                        label = "${cat.label} ($count)",
                        icon = cat.icon,
                        isSelected = selectedCategory == cat,
                        colors = colors,
                        onClick = { selectedCategory = cat }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- 5. All Tools Grid Section ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedCategory != null) selectedCategory!!.label else "جميع الأدوات والخدمات",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.text
                )
                Text(
                    text = "${filteredTools.size} أداة",
                    fontSize = 12.sp,
                    color = colors.accent,
                    fontWeight = FontWeight.Bold
                )
            }

            if (filteredTools.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = colors.textMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "لم يتم العثور على أداة مطابقة",
                            fontSize = 14.sp,
                            color = colors.textMuted
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    filteredTools.chunked(2).forEach { rowTools ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowTools.forEach { tool ->
                                RoyalToolCard(
                                    tool = tool,
                                    colors = colors,
                                    onClick = { onSelectCalc(tool) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowTools.size < 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryChip(
    label: String,
    icon: String,
    isSelected: Boolean,
    colors: CustomThemeColors,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) colors.accent else colors.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) colors.accent else colors.border
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else colors.text
            )
        }
    }
}

@Composable
fun RoyalToolCard(
    tool: CalcKey,
    colors: CustomThemeColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryGradient = when (tool.category) {
        CategoryKey.ISLAMIC -> Brush.linearGradient(listOf(Color(0xFF0D9488), Color(0xFF0F766E)))
        CategoryKey.FINANCE -> Brush.linearGradient(listOf(Color(0xFFD4AF37), Color(0xFFB8860B)))
        CategoryKey.CALC -> Brush.linearGradient(listOf(Color(0xFF0EA5E9), Color(0xFF0284C7)))
        CategoryKey.HEALTH -> Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF047857)))
        CategoryKey.VEHICLE -> Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF4338CA)))
        CategoryKey.DATES -> Brush.linearGradient(listOf(Color(0xFFEC4899), Color(0xFFBE185D)))
        CategoryKey.UTILITY -> Brush.linearGradient(listOf(Color(0xFF64748B), Color(0xFF475569)))
        else -> Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9)))
    }

    Surface(
        modifier = modifier
            .height(118.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = colors.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
        shadowElevation = 3.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Badge tag if present
            if (tool.badge != null) {
                Surface(
                    color = colors.accent,
                    shape = RoundedCornerShape(bottomStart = 10.dp, topEnd = 20.dp),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = tool.badge,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(categoryGradient),
                    contentAlignment = Alignment.Center
                ) {
                    when (val icon = AppIcons.forCalc(tool)) {
                        is ImageVector -> Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        else -> Text(tool.icon, fontSize = 22.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = tool.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.text,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = tool.category.label,
                    fontSize = 10.sp,
                    color = colors.textMuted,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}
