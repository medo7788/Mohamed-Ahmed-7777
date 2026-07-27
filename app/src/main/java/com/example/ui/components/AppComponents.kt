package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CalcKey
import com.example.model.CategoryKey
import com.example.ui.theme.AppThemeKey
import com.example.ui.theme.CustomThemeColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppHeader(
    currentCalc: CalcKey,
    colors: CustomThemeColors,
    onOpenDrawer: () -> Unit,
    onOpenThemes: () -> Unit,
    onOpenAbout: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }

    Surface(
        color = colors.headerBg,
        contentColor = colors.headerFg,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onOpenDrawer) {
                Icon(Icons.Default.Menu, contentDescription = "القائمة", tint = colors.headerFg)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = currentCalc.icon, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = currentCalc.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.headerFg,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onOpenThemes) {
                        Icon(Icons.Default.Palette, contentDescription = "المظهر", tint = colors.headerFg)
                    }
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "خيارات", tint = colors.headerFg)
                    }
                }

                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                    modifier = Modifier.background(colors.surface)
                ) {
                    DropdownMenuItem(
                        text = { Text("الثيمات والمظهر 🎨", color = colors.text) },
                        onClick = {
                            menuOpen = false
                            onOpenThemes()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("حول التطبيق ℹ️", color = colors.text) },
                        onClick = {
                            menuOpen = false
                            onOpenAbout()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AppDrawerContent(
    currentKey: CalcKey,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    colors: CustomThemeColors,
    onSelectCalc: (CalcKey) -> Unit,
    onOpenThemes: () -> Unit
) {
    val allCalcs = remember { CalcKey.values().toList() }
    val categories = remember { CategoryKey.values().toList() }

    val filteredCalcs = remember(searchQuery) {
        if (searchQuery.isBlank()) allCalcs
        else {
            val q = searchQuery.trim().lowercase()
            allCalcs.filter { c ->
                c.title.lowercase().contains(q) || c.keywords.any { it.lowercase().contains(q) }
            }
        }
    }

    val groupedCalcs = remember(filteredCalcs) {
        filteredCalcs.groupBy { it.category }
    }

    val collapsedState = remember { mutableStateMapOf<CategoryKey, Boolean>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Drawer Header Gradient Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(colors.accent, colors.accentSecondary)
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🧮", fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ClevCalc Pro",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "الحاسبة الاحترافية الشاملة",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Text(
                    text = "${allCalcs.size} حاسبة • أسعار حية • ذكاء اصطناعي",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("ابحث عن حاسبة...", fontSize = 14.sp, color = colors.textMuted) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = colors.textMuted) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.accent,
                unfocusedBorderColor = colors.border,
                focusedContainerColor = colors.surface2,
                unfocusedContainerColor = colors.surface2,
                focusedTextColor = colors.text,
                unfocusedTextColor = colors.text
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        )

        // List of Calculators
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Featured shortcuts grid if no search query
            if (searchQuery.isBlank() && groupedCalcs[CategoryKey.FEATURED] != null) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        groupedCalcs[CategoryKey.FEATURED]?.forEach { item ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (item == CalcKey.AI) Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF6366F1)))
                                        else Brush.linearGradient(listOf(Color(0xFFEF4444), Color(0xFFF59E0B)))
                                    )
                                    .clickable { onSelectCalc(item) }
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(item.icon, fontSize = 28.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = item.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Categories
            categories.filter { it != CategoryKey.FEATURED || searchQuery.isNotBlank() }.forEach { cat ->
                val itemsInCat = groupedCalcs[cat]
                if (!itemsInCat.isNullOrEmpty()) {
                    val isCollapsed = collapsedState[cat] ?: false

                    item(key = cat.id) {
                        Surface(
                            color = colors.surface,
                            onClick = { collapsedState[cat] = !isCollapsed },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(cat.icon, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = cat.label,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.text
                                        )
                                        Text(
                                            text = cat.description,
                                            fontSize = 11.sp,
                                            color = colors.textMuted
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Badge(
                                        containerColor = colors.accent,
                                        contentColor = Color.White
                                    ) {
                                        Text(itemsInCat.size.toString(), fontSize = 11.sp)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = if (isCollapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                        contentDescription = null,
                                        tint = colors.textMuted
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = colors.border.copy(alpha = 0.5f))
                    }

                    if (!isCollapsed) {
                        items(itemsInCat) { calc ->
                            val isSelected = calc == currentKey
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isSelected) colors.surface2 else Color.Transparent)
                                    .clickable { onSelectCalc(calc) }
                                    .padding(horizontal = 24.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(calc.icon, fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = calc.title,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) colors.accent else colors.text
                                    )
                                }

                                calc.badge?.let { bText ->
                                    val bBg = when (bText) {
                                        "LIVE" -> Color(0xFFEF4444)
                                        "AI" -> Color(0xFF8B5CF6)
                                        "HOT" -> Color(0xFFF59E0B)
                                        else -> Color(0xFF10B981)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(bBg)
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = bText,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Drawer Footer
        HorizontalDivider(color = colors.border)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenThemes() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text("🎨 تغيير الثيم والمظهر", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.accent)
        }
    }
}

@Composable
fun ThemeSelectorModal(
    currentTheme: AppThemeKey,
    colors: CustomThemeColors,
    onSelectTheme: (AppThemeKey) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "🎨 اختر مظهر التطبيق",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colors.text,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                AppThemeKey.values().forEach { key ->
                    val isSelected = key == currentTheme
                    val tColors = com.example.ui.theme.getThemeColors(key)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) colors.surface2 else Color.Transparent)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) colors.accent else colors.border,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectTheme(key) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(tColors.accent)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "${key.icon} ${key.titleAr}",
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = colors.text
                            )
                        }

                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = colors.accent)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق", color = colors.accent)
            }
        },
        containerColor = colors.surface,
        shape = RoundedCornerShape(18.dp)
    )
}

@Composable
fun AboutModal(
    colors: CustomThemeColors,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🧮", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("ClevCalc Pro", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.text)
                Text("الإصدار 2.0.0", fontSize = 12.sp, color = colors.textMuted)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Surface(
                    color = colors.surface2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("✨ الميزات الاحترافية", fontWeight = FontWeight.Bold, color = colors.accent, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("• 30 حاسبة وأداة إسلامية ومالية شاملة", fontSize = 12.sp, color = colors.text)
                        Text("• أسعار حية للعملات والذهب والفضة والنفط", fontSize = 12.sp, color = colors.text)
                        Text("• مواقيت الصلاة والقبلة والقرآن الكريم الأذكار", fontSize = 12.sp, color = colors.text)
                        Text("• 8 ثيمات جذابة قابلة للتخصيص", fontSize = 12.sp, color = colors.text)
                        Text("• مساعد ذكي متقدم بالذكاء الاصطناعي (Gemini)", fontSize = 12.sp, color = colors.text)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    color = colors.surface2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("🌐 مصادر البيانات المباشرة", fontWeight = FontWeight.Bold, color = Color(0xFF10B981), fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("• 💱 العملات (166 عملة): exchangerate.fun", fontSize = 11.sp, color = colors.textMuted)
                        Text("• 🥇 المعادن والذهب: gold-api.com", fontSize = 11.sp, color = colors.textMuted)
                        Text("• 🕌 مواقيت الصلاة والقبلة: aladhan.com", fontSize = 11.sp, color = colors.textMuted)
                        Text("• 📖 القرآن الكريم: alquran.cloud", fontSize = 11.sp, color = colors.textMuted)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
            ) {
                Text("حسناً", color = Color.White)
            }
        },
        containerColor = colors.surface,
        shape = RoundedCornerShape(18.dp)
    )
}
