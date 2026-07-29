package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CalcKey
import com.example.model.CategoryKey
import com.example.ui.theme.AppIcons
import com.example.ui.theme.CustomThemeColors
import com.example.ui.theme.GradientTokens
import com.example.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    colors: CustomThemeColors,
    onSelectCalc: (CalcKey) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<CategoryKey?>(null) }

    val filteredCalcs = CalcKey.values().filter { item ->
        item != CalcKey.HOME &&
        (selectedCategory == null || item.category == selectedCategory) &&
        (searchQuery.isBlank() || item.title.contains(searchQuery, ignoreCase = true) || item.keywords.any { it.contains(searchQuery, ignoreCase = true) })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.appBg)
            .padding(horizontal = Spacing.Medium)
    ) {
        Spacer(modifier = Modifier.height(Spacing.Small))

        // Welcome header
        Card(
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp)
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(GradientTokens.AI)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Column {
                    Text(
                        "أهلاً بك في ClevCalc Pro",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "كل أدواتك الذكية في مكان واحد",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            placeholder = { Text("ابحث عن آلة حاسبة أو أداة...", color = colors.textMuted, fontSize = 13.sp) },
            leadingIcon = { Icon(AppIcons.Search, contentDescription = null, tint = colors.accent) },
            trailingIcon = {
                IconButton(onClick = {}) {
                    Icon(AppIcons.Mic, contentDescription = "بحث صوتي", tint = colors.textMuted)
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = colors.text,
                unfocusedTextColor = colors.text,
                focusedBorderColor = colors.accent,
                unfocusedBorderColor = colors.border,
                focusedContainerColor = colors.surface,
                unfocusedContainerColor = colors.surface
            )
        )

        // Category chips
        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val allSelected = selectedCategory == null
            Surface(
                onClick = { selectedCategory = null },
                shape = RoundedCornerShape(20.dp),
                color = if (allSelected) colors.accent else colors.surface,
                contentColor = if (allSelected) Color.White else colors.text,
                border = if (allSelected) null else BorderStroke(1.dp, colors.border),
                modifier = Modifier.height(36.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text("الكل", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            CategoryKey.values().forEach { cat ->
                val isSelected = selectedCategory == cat
                Surface(
                    onClick = { selectedCategory = if (isSelected) null else cat },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) colors.accent else colors.surface,
                    contentColor = if (isSelected) Color.White else colors.text,
                    border = if (isSelected) null else BorderStroke(1.dp, colors.border),
                    modifier = Modifier.height(36.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Icon(
                            AppIcons.forCategory(cat),
                            contentDescription = null,
                            modifier = Modifier
                                .size(14.dp)
                                .padding(end = 4.dp),
                            tint = if (isSelected) Color.White else colors.textMuted
                        )
                        Text(cat.label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Grid of tool cards
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredCalcs) { calc ->
                val categoryColor = Color(android.graphics.Color.parseColor(calc.category.colorHex))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(115.dp)
                        .clickable { onSelectCalc(calc) },
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(categoryColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    AppIcons.forCalc(calc),
                                    contentDescription = null,
                                    tint = categoryColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            if (calc.badge != null) {
                                val bBg = when (calc.badge) {
                                    "LIVE" -> Color(0xFFEF4444)
                                    "AI" -> Color(0xFF8B5CF6)
                                    "HOT" -> Color(0xFFF59E0B)
                                    else -> Color(0xFF10B981)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(bBg)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(calc.badge, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Column {
                            Text(
                                calc.title,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.text,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                calc.category.label,
                                color = colors.textMuted,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
