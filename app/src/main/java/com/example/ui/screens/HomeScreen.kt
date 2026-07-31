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
import androidx.compose.ui.platform.LocalContext
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
import com.example.ui.theme.AppIcons
import com.example.ui.theme.CustomThemeColors
import com.example.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HomeScreen(
    colors: CustomThemeColors,
    viewModel: MainViewModel,
    onSelectCalc: (CalcKey) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<CategoryKey?>(null) }
    val keyboardController = LocalSoftwareKeyboardController.current

    val favoriteTools by viewModel.favoriteTools.collectAsState()
    val recentTools by viewModel.recentTools.collectAsState()

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

    // Dynamic greeting based on time of day
    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val dynamicGreetingText = when {
        currentHour in 5..11 -> "صباح الخير والبركة 👋"
        currentHour in 12..16 -> "طاب يومك بكل خير 👋"
        currentHour in 17..21 -> "مساء النور والسرور 👋"
        else -> "أسعد الله مساؤك بالخير 👋"
    }

    // Current Date Formatter
    val arabicLocale = Locale("ar")
    val dayName = SimpleDateFormat("EEEE", arabicLocale).format(Date())
    val dayOfMonth = SimpleDateFormat("d MMMM yyyy", arabicLocale).format(Date())
    val hijriDateStr = remember {
        // Simple mock hijri date calculation
        val hc = GregorianCalendar()
        val hYear = hc.get(Calendar.YEAR) - 579
        val hMonths = listOf("محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة")
        // Just mock a nice day
        "15 ${hMonths[((hc.get(Calendar.MONTH) + 5) % 12)]} ${hYear}هـ"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (colors.isDark) {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF121212),
                            Color(0xFF1E1E1E),
                            Color(0xFF121212)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF5F8F9),
                            Color(0xFFE7ECEF),
                            Color(0xFFF5F8F9)
                        )
                    )
                }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 100.dp)
        ) {
            // --- 1. Header (Dynamic Premium Gradient & Islamic Pattern) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = if (colors.isDark) {
                                listOf(Color(0xFF1B8A6B), Color(0xFF121212))
                            } else {
                                listOf(Color(0xFFD8EEE7), Color(0xFFBFE5DA))
                            }
                        )
                    )
            ) {
                // Transparent Islamic Pattern overlay at 5% opacity
                Image(
                    painter = painterResource(id = R.drawable.ic_islamic_pattern),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.05f),
                    contentScale = ContentScale.Inside,
                    colorFilter = ColorFilter.tint(if (colors.isDark) Color.White else Color(0xFF1B8A6B))
                )

                // Layout inside Header
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                color = if (colors.isDark) Color(0xFF242424) else Color.White,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (colors.isDark) colors.border else Color(0xFF1B8A6B).copy(alpha = 0.2f)
                                ),
                                shadowElevation = 2.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color(0xFF1B8A6B),
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "ClevCalc Pro",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (colors.isDark) Color.White else Color(0xFF1B8A6B)
                                )
                                Text(
                                    text = "المنصة الذكية المتكاملة",
                                    fontSize = 11.sp,
                                    color = if (colors.isDark) colors.textMuted else Color(0xFF1B8A6B).copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Weather Miniature & Date
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "القاهرة، مصر",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (colors.isDark) Color.White else Color(0xFF1B8A6B)
                                )
                                Text(
                                    text = "28° م • مشمس",
                                    fontSize = 10.sp,
                                    color = if (colors.isDark) colors.textMuted else Color(0xFF1B8A6B).copy(alpha = 0.7f)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.WbSunny,
                                contentDescription = null,
                                tint = if (colors.isDark) Color(0xFFF59E0B) else Color(0xFFE28A13),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "$dayName، $dayOfMonth",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (colors.isDark) Color.White else Color(0xFF1B8A6B)
                            )
                            Text(
                                text = hijriDateStr,
                                fontSize = 11.sp,
                                color = if (colors.isDark) colors.textMuted else Color(0xFF1B8A6B).copy(alpha = 0.7f)
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = colors.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onSelectCalc(CalcKey.AI) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "AI",
                                    tint = Color(0xFF7E57C2),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "المستشار الذكي AI",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.accent
                                )
                            }
                        }
                    }
                }
            }

            // --- 2. Smart Greeting Card ---
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .offset(y = (-16).dp),
                shape = RoundedCornerShape(24.dp),
                color = colors.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border.copy(alpha = 0.4f)),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = dynamicGreetingText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.text
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "جاهز لتسهيل حساباتك وإنجاز يومك بذكاء.",
                            fontSize = 11.sp,
                            color = colors.textMuted
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(colors.accent.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WbCloudy,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // --- 3. Floating Modern Search Bar ---
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        "ابحث عن أي أداة أو حاسبة...",
                        color = colors.textMuted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = colors.accent) },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, null, tint = colors.textMuted)
                            }
                        }
                        IconButton(onClick = { /* Voice Search Action */ }) {
                            Icon(Icons.Default.Mic, "صوت", tint = colors.accent)
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                shape = RoundedCornerShape(28.dp),
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
                    .height(58.dp)
                    .padding(horizontal = 16.dp)
            )

            // --- 4. Quick Actions (Horizontal Chips) ---
            Spacer(modifier = Modifier.height(16.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "الوصول السريع",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.text,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val quickActions = listOf(
                        Triple("الآلة الحاسبة", CalcKey.BASIC, Color(0xFF0077CC)),
                        Triple("المساعد الذكي", CalcKey.AI, Color(0xFF7E57C2)),
                        Triple("مواقيت الصلاة", CalcKey.PRAYER, Color(0xFF2D8C73)),
                        Triple("حاسبة الذهب", CalcKey.GOLD, Color(0xFFFFD700)),
                        Triple("محول العملات", CalcKey.CURRENCY, Color(0xFF2E7D32)),
                        Triple("تحليل الطقس", CalcKey.WEATHER, Color(0xFF0288D1))
                    )
                    items(quickActions) { (label, key, actionColor) ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = colors.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, actionColor.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onSelectCalc(key) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(actionColor.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = AppIcons.forCalc(key),
                                        contentDescription = null,
                                        tint = actionColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.text
                                )
                            }
                        }
                    }
                }
            }

            // --- 5. Favorites (Stateful Pinned / Starred Tools) ---
            if (favoriteTools.isNotEmpty() && searchQuery.isBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "أدواتك المثبتة (المفضلة)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.text
                            )
                        }
                    }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(favoriteTools.toList()) { toolId ->
                            val tool = allTools.find { it.name == toolId }
                            if (tool != null) {
                                PremiumToolCardMini(
                                    tool = tool,
                                    colors = colors,
                                    isFavorite = true,
                                    onToggleFavorite = { viewModel.toggleFavorite(context, tool.name) },
                                    onClick = { onSelectCalc(tool) }
                                )
                            }
                        }
                    }
                }
            }

            // --- 6. Continue Using (Recent Tools) ---
            if (recentTools.isNotEmpty() && searchQuery.isBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "متابعة الاستخدام (الأخيرة)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.text
                            )
                        }
                    }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(recentTools) { toolId ->
                            val tool = allTools.find { it.name == toolId }
                            if (tool != null) {
                                PremiumToolCardMini(
                                    tool = tool,
                                    colors = colors,
                                    isFavorite = favoriteTools.contains(tool.name),
                                    onToggleFavorite = { viewModel.toggleFavorite(context, tool.name) },
                                    onClick = { onSelectCalc(tool) }
                                )
                            }
                        }
                    }
                }
            }

            // --- 7. Suggested Tools Section (Based on Time of Day / Context) ---
            if (searchQuery.isBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Text(
                        text = "مقترح لك الآن",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.text,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    )

                    // Suggested Tool Card Based on Time
                    val suggestedTool = when {
                        currentHour in 5..9 -> CalcKey.ADHKAR // Morning Azhkar
                        currentHour in 11..14 -> CalcKey.PRAYER // Prayer Times
                        currentHour in 17..20 -> CalcKey.TASBIH // Electronic tasbih
                        else -> CalcKey.QURAN // Night Quran Reading
                    }

                    val suggestedTitle = when (suggestedTool) {
                        CalcKey.ADHKAR -> "أذكار الصباح والمساء"
                        CalcKey.PRAYER -> "مواقيت الصلاة والأذان"
                        CalcKey.TASBIH -> "المسبحة الرقمية والأذكار"
                        else -> "تلاوة القرآن الكريم"
                    }

                    val suggestedDesc = when (suggestedTool) {
                        CalcKey.ADHKAR -> "ابدأ يومك بذكر الله وحصن نفسك."
                        CalcKey.PRAYER -> "تابع مواقيت الصلاة القادمة وتنبيهات الأذان."
                        CalcKey.TASBIH -> "الاستغفار والتسبيح اليومي بلمسة ذكية."
                        else -> "وردك القرآني اليومي بأجمل تصميم وتفسير."
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .clickable { onSelectCalc(suggestedTool) },
                        color = colors.surface,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF7E57C2).copy(alpha = 0.3f)),
                        shadowElevation = 3.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF7E57C2), Color(0xFF673AB7))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = AppIcons.forCalc(suggestedTool),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = Color(0xFF7E57C2).copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "اقتراح ذكي",
                                            fontSize = 9.sp,
                                            color = Color(0xFF7E57C2),
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = suggestedTitle,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.text
                                )
                                Text(
                                    text = suggestedDesc,
                                    fontSize = 11.sp,
                                    color = colors.textMuted
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = null,
                                tint = colors.textMuted,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            // --- 8. Categories Filter Section ---
            Spacer(modifier = Modifier.height(20.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "الأقسام والخدمات",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.text,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
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
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- 9. All Tools (Grid Layout 24dp Corner Cards) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedCategory != null) selectedCategory!!.label else "جميع الأدوات والخدمات",
                    fontSize = 17.sp,
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
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    filteredTools.chunked(2).forEach { rowTools ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowTools.forEach { tool ->
                                PremiumToolCard(
                                    tool = tool,
                                    colors = colors,
                                    isFavorite = favoriteTools.contains(tool.name),
                                    onToggleFavorite = { viewModel.toggleFavorite(context, tool.name) },
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
fun PremiumToolCardMini(
    tool: CalcKey,
    colors: CustomThemeColors,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryColor = when (tool.category) {
        CategoryKey.ISLAMIC -> Color(0xFF2D8C73)
        CategoryKey.FINANCE -> Color(0xFF2E7D32)
        CategoryKey.CALC -> Color(0xFF0077CC)
        CategoryKey.HEALTH -> Color(0xFFD81B60)
        CategoryKey.VEHICLE -> Color(0xFFEF6C00)
        CategoryKey.DATES -> Color(0xFF1565C0)
        CategoryKey.UTILITY -> Color(0xFF546E7A)
        else -> Color(0xFF7E57C2)
    }

    Surface(
        modifier = modifier
            .width(150.dp)
            .height(100.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = colors.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border.copy(alpha = 0.5f)),
        shadowElevation = 2.dp
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            // Unpin button
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable { onToggleFavorite() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.PushPin else Icons.Outlined.PushPin,
                    contentDescription = null,
                    tint = if (isFavorite) Color(0xFFF59E0B) else colors.textMuted,
                    modifier = Modifier.size(14.dp)
                )
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(categoryColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.forCalc(tool),
                        contentDescription = null,
                        tint = categoryColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = tool.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = tool.category.label,
                        fontSize = 9.sp,
                        color = colors.textMuted,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun PremiumToolCard(
    tool: CalcKey,
    colors: CustomThemeColors,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryColor = when (tool.category) {
        CategoryKey.ISLAMIC -> Color(0xFF2D8C73)
        CategoryKey.FINANCE -> Color(0xFF2E7D32)
        CategoryKey.CALC -> Color(0xFF0077CC)
        CategoryKey.HEALTH -> Color(0xFFD81B60)
        CategoryKey.VEHICLE -> Color(0xFFEF6C00)
        CategoryKey.DATES -> Color(0xFF1565C0)
        CategoryKey.UTILITY -> Color(0xFF546E7A)
        else -> Color(0xFF7E57C2)
    }

    Surface(
        modifier = modifier
            .height(130.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        color = colors.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border.copy(alpha = 0.6f)),
        shadowElevation = 3.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Pinned/Favorite toggle button
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(colors.appBg.copy(alpha = 0.5f))
                    .clickable { onToggleFavorite() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.PushPin else Icons.Outlined.PushPin,
                    contentDescription = "تثبيت الأداة",
                    tint = if (isFavorite) Color(0xFFF59E0B) else colors.textMuted,
                    modifier = Modifier.size(15.dp)
                )
            }

            // Optional Badge tags
            if (tool.badge != null) {
                Surface(
                    color = when (tool.badge) {
                        "NEW" -> Color(0xFF43A047)
                        "HOT" -> Color(0xFFEF6C00)
                        "LIVE" -> Color(0xFFD32F2F)
                        "AI" -> Color(0xFF7E57C2)
                        "PRO" -> Color(0xFF0077CC)
                        else -> colors.accent
                    },
                    shape = RoundedCornerShape(bottomStart = 10.dp, topEnd = 24.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = tool.badge,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Large Icon in beautiful circular/rounded container (Radius 18dp)
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(categoryColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.forCalc(tool),
                        contentDescription = null,
                        tint = categoryColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        text = tool.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = tool.category.label,
                        fontSize = 10.sp,
                        color = colors.textMuted,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
