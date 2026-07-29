package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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
import com.example.model.CategoryKey
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
        Spacer(modifier = Modifier.height(Spacing.Medium))

        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.Small),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GradientTokens.AI)
                    .padding(vertical = Spacing.Large, horizontal = Spacing.Medium)
            ) {
                Column {
                    Text(
                        "مرحباً بك في ClevCalc Pro ✨",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
                    Text(
                        "تطبيق الحاسبة والمساعد الذكي والأدوات المتكاملة في مكان واحد.",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp
                    )
                }
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.Small),
            placeholder = { Text("ابحث عن آلة حاسبة أو أداة...", color = colors.textMuted) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = colors.accent) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = colors.text,
                unfocusedTextColor = colors.text,
                focusedBorderColor = colors.accent,
                unfocusedBorderColor = colors.border,
                focusedContainerColor = colors.surface,
                unfocusedContainerColor = colors.surface
            )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.Small),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
        ) {
            val allSelected = selectedCategory == null
            Surface(
                onClick = { selectedCategory = null },
                shape = RoundedCornerShape(12.dp),
                color = if (allSelected) colors.accent else colors.surface,
                contentColor = if (allSelected) Color.White else colors.text,
                border = if (allSelected) null else androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                modifier = Modifier.height(36.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 14.dp)
                ) {
                    Text("الكل", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            val majorCategories = listOf(CategoryKey.ISLAMIC, CategoryKey.CALC, CategoryKey.FINANCE)
            majorCategories.forEach { cat ->
                val isSelected = selectedCategory == cat
                Surface(
                    onClick = { selectedCategory = if (isSelected) null else cat },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) colors.accent else colors.surface,
                    contentColor = if (isSelected) Color.White else colors.text,
                    border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                    modifier = Modifier.height(36.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    ) {
                        Text(cat.icon, modifier = Modifier.padding(end = 4.dp))
                        Text(cat.label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.Small))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = Spacing.Small),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
        ) {
            items(filteredCalcs) { calc ->
                val categoryColor = Color(android.graphics.Color.parseColor(calc.category.colorHex))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clickable { onSelectCalc(calc) },
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Spacing.Medium),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(categoryColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(calc.icon, fontSize = 20.sp)
                            }

                            if (calc.badge != null) {
                                Badge(containerColor = colors.accent) {
                                    Text(calc.badge, color = Color.White, fontSize = 9.sp, modifier = Modifier.padding(2.dp))
                                }
                            }
                        }

                        Column {
                            Text(
                                calc.title,
                                fontWeight = FontWeight.Bold,
                                color = colors.text,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                calc.category.label,
                                color = colors.textMuted,
                                fontSize = 11.sp,
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
