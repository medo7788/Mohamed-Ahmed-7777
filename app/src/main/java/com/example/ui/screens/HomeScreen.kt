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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.ui.theme.DesignTokens
import com.example.ui.theme.Spacing
import com.example.viewmodel.MainViewModel
import com.example.ui.components.FrostedGlassCard
import com.example.ui.components.FrostedGlassCardVariant
import com.example.ui.components.SectionHeader
import com.example.ui.components.HubCategoryCard
import com.example.util.AppLocationProvider
import com.example.data.WeatherRepository
import com.example.data.IslamicData
import kotlinx.coroutines.launch
import com.example.ui.components.GlassSearchBar
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    colors: CustomThemeColors,
    viewModel: MainViewModel,
    onSelectCalc: (CalcKey) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    // إصلاح: كانت activeHubCategory بـremember عادي - يعني لو المستخدم فتح باب
    // (مثلاً "المال والأسعار") واختار أداة منه، وبعدين رجع بزرار الرجوع، كان الباب
    // بيتقفل تلقائيًا وترجع الشاشة الرئيسية لوضعها الافتراضي، فيضطر يفتح الباب تاني
    // من الصفر. rememberSaveable بتحافظ على القيمة دي حتى لو الشاشة اتبنيت من جديد.
    var activeHubCategory by rememberSaveable(
        stateSaver = androidx.compose.runtime.saveable.Saver<CategoryKey?, String>(
            save = { it?.name ?: "" },
            restore = { name -> if (name.isBlank()) null else CategoryKey.valueOf(name) }
        )
    ) { mutableStateOf<CategoryKey?>(null) }
    val keyboardController = LocalSoftwareKeyboardController.current

    val favoriteTools by viewModel.favoriteTools.collectAsState()
    val recentTools by viewModel.recentTools.collectAsState()

    // إصلاح: كانت مدينة القاهرة/الطقس/العد التنازلي للصلاة نصوص ثابتة (Placeholder)
    // غير مرتبطة بموقع أو مواقيت المستخدم الفعلية، رغم إن نفس منطق الحساب الحقيقي
    // مستخدم بالفعل وبيشتغل صح في شاشتي الصلاة والقبلة. هنا بنجيب نفس البيانات
    // الحقيقية ونعرضها في الـHero بدل النص الثابت.
    var cityLabel by remember { mutableStateOf("جارِ تحديد الموقع...") }
    var weatherTempC by remember { mutableStateOf<Double?>(null) }
    var weatherConditionAr by remember { mutableStateOf("") }
    var nextPrayerText by remember { mutableStateOf("جارِ حساب مواقيت الصلاة...") }

    LaunchedEffect(Unit) {
        val locResult = AppLocationProvider.getLastKnownLocation(context)
        val (lat, lng) = when (locResult) {
            is AppLocationProvider.Result.Success -> locResult.latitude to locResult.longitude
            else -> {
                val cached = AppLocationProvider.getCachedLocation(context)
                if (cached != null) {
                    cityLabel = cached.placeName ?: cityLabel
                    cached.lat to cached.lng
                } else {
                    // لا يوجد إذن/موقع محفوظ - نسيب النص الافتراضي ونوقف هنا
                    nextPrayerText = "فعّل الموقع لعرض مواقيت الصلاة"
                    return@LaunchedEffect
                }
            }
        }

        // الطقس الحقيقي لنفس الإحداثيات
        launch {
            try {
                val weather = WeatherRepository.fetchRealWeather(context, lat, lng)
                weatherTempC = weather.tempC
                weatherConditionAr = weather.conditionAr
            } catch (_: Exception) { /* يفضل النص الافتراضي لو فشل الطلب */ }
        }

        // العد التنازلي الحقيقي للصلاة القادمة بنفس منطق شاشة الصلاة
        try {
            val tzOffset = IslamicData.getCorrectTimezoneOffset(lat, lng)
            val times = IslamicData.calculatePrayerTimes(lat, lng, tzOffset)
            val now = java.util.Calendar.getInstance()
            val nowMinutes = now.get(java.util.Calendar.HOUR_OF_DAY) * 60 + now.get(java.util.Calendar.MINUTE)

            fun toMinutes(hhmm: String): Int {
                val parts = hhmm.split(":")
                return parts[0].toInt() * 60 + parts[1].toInt()
            }

            val prayers = listOf(
                "الفجر" to toMinutes(times.fajr),
                "الظهر" to toMinutes(times.dhuhr),
                "العصر" to toMinutes(times.asr),
                "المغرب" to toMinutes(times.maghrib),
                "العشاء" to toMinutes(times.isha)
            )
            // أول صلاة قادمة لسه ماجاش وقتها، أو الفجر بكرة لو الكل فات
            val next = prayers.firstOrNull { it.second > nowMinutes } ?: prayers.first()
            val minutesUntil = if (next.second > nowMinutes) next.second - nowMinutes else (1440 - nowMinutes + next.second)
            nextPrayerText = if (minutesUntil < 60) {
                "صلاة ${next.first} بعد $minutesUntil دقيقة"
            } else {
                "صلاة ${next.first} بعد ${minutesUntil / 60} س ${minutesUntil % 60} د"
            }
        } catch (_: Exception) { }
    }

    val allTools = remember { CalcKey.values().filter { it != CalcKey.HOME && it != CalcKey.SETTINGS } }

    val filteredTools = remember(searchQuery) {
        allTools.filter { tool ->
            searchQuery.isBlank() ||
                tool.title.contains(searchQuery, ignoreCase = true) || 
                tool.keywords.any { it.contains(searchQuery, ignoreCase = true) }
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
        val hc = GregorianCalendar()
        val hYear = hc.get(Calendar.YEAR) - 579
        val hMonths = listOf("محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة")
        "15 ${hMonths[((hc.get(Calendar.MONTH) + 5) % 12)]} ${hYear}هـ"
    }

    // If an active category hub is selected, overlay the HubScreen seamlessly!
    if (activeHubCategory != null) {
        HubScreen(
            category = activeHubCategory!!,
            colors = colors,
            favoriteTools = favoriteTools,
            onToggleFavorite = { viewModel.toggleFavorite(context, it.name) },
            onToolClick = { onSelectCalc(it) },
            onBackClick = { activeHubCategory = null }
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.appBg)
        ) {
            // Subtle Islamic pattern in background at exactly 3% opacity
            Image(
                painter = painterResource(id = R.drawable.ic_islamic_pattern),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.03f),
                contentScale = ContentScale.Inside,
                colorFilter = ColorFilter.tint(colors.accent.copy(alpha = 0.5f))
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 100.dp)
            ) {
                // --- 1. Header (Premium Soft Frosted Glass Header) ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    colors.headerBg.copy(alpha = 0.9f),
                                    colors.appBg
                                )
                            )
                        )
                ) {
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
                                    color = colors.surface.copy(alpha = 0.75f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        colors.accent.copy(alpha = 0.3f)
                                    ),
                                    shadowElevation = 2.dp
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = colors.accent,
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
                                        color = colors.text
                                    )
                                    Text(
                                        text = "المنصة الذكية المتكاملة",
                                        fontSize = 11.sp,
                                        color = colors.textMuted,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Weather Miniature & Date (Cairo, Egypt)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = cityLabel,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.text
                                    )
                                    Text(
                                        text = if (weatherTempC != null) "${weatherTempC!!.toInt()}° م • $weatherConditionAr" else "جارِ التحميل...",
                                        fontSize = 10.sp,
                                        color = colors.textMuted
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.WbSunny,
                                    contentDescription = null,
                                    tint = colors.accent,
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
                                    color = colors.text
                                )
                                Text(
                                    text = hijriDateStr,
                                    fontSize = 11.sp,
                                    color = colors.textMuted
                                )
                            }

                            Surface(
                                shape = CircleShape,
                                color = colors.surface.copy(alpha = 0.75f),
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
                                        tint = Color(0xFFC084FC),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "المستشار الذكي AI",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.text
                                    )
                                }
                            }
                        }
                    }
                }

                // --- 2. Smart Hero Card ---
                FrostedGlassCard(
                    colors = colors,
                    variant = FrostedGlassCardVariant.Hero,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .offset(y = (-16).dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = dynamicGreetingText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.text
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (weatherTempC != null)
                                    "$nextPrayerText • الطقس $weatherConditionAr ${weatherTempC!!.toInt()}°م"
                                else nextPrayerText,
                                fontSize = 11.sp,
                                color = colors.accent,
                                fontWeight = FontWeight.Bold
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
                GlassSearchBar(
                    colors = colors,
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "ابحث عن أي أداة أو حاسبة...",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(Spacing.Medium))

                // If search query is NOT empty, display filtered search results overlay directly! (Progressive Disclosure)
                if (searchQuery.isNotBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        SectionHeader(
                            colors = colors,
                            title = "نتائج البحث لـ \"$searchQuery\" (${filteredTools.size})"
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (filteredTools.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
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
                                        fontSize = 13.sp,
                                        color = colors.textMuted
                                    )
                                }
                            }
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
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
                } else {
                    // Standard structured Premium Home Feed!

                    // --- 4. Smart AI Assistant Quick Action Banner ---
                    FrostedGlassCard(
                        colors = colors,
                        variant = FrostedGlassCardVariant.Compact,
                        onClick = { onSelectCalc(CalcKey.AI) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFC084FC).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color(0xFFC084FC),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "اسأل المساعد الذكي AI",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.text
                                    )
                                    Text(
                                        text = "مساعد فتاوى الزكاة، تحليلات ومحاكاة القروض والذهب",
                                        fontSize = 10.sp,
                                        color = colors.textMuted
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // --- 5. Favorites / Starred Section ---
                    if (favoriteTools.isNotEmpty()) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SectionHeader(
                                colors = colors,
                                title = "أدواتك المثبتة (المفضلة)",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
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
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // --- 6. Structured Category Hubs (The 5 Doors) ---
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SectionHeader(
                            colors = colors,
                            title = "أبواب المنصة والأقسام الشاملة"
                        )

                        // Gateway Hub 1: Islamic & Worship Hub (Direct Route to Prayer Times & Worship Screen)
                        HubCategoryCard(
                            colors = colors,
                            title = "مواقيت الصلاة والعبادات",
                            icon = AppIcons.forCategory(CategoryKey.ISLAMIC),
                            toolCount = allTools.count { it.category == CategoryKey.ISLAMIC },
                            gradient = Brush.linearGradient(listOf(Color(0xFF042F2C), Color(0xFF10B981))),
                            onClick = { onSelectCalc(CalcKey.PRAYER) }
                        )

                        // Gateway Hub 2: Finance
                        HubCategoryCard(
                            colors = colors,
                            title = CategoryKey.FINANCE.label,
                            icon = AppIcons.forCategory(CategoryKey.FINANCE),
                            toolCount = allTools.count { it.category == CategoryKey.FINANCE },
                            gradient = Brush.linearGradient(listOf(Color(0xFF292524), Color(0xFFF59E0B))),
                            onClick = { activeHubCategory = CategoryKey.FINANCE }
                        )

                        // Gateway Hub 3: Date & Time
                        HubCategoryCard(
                            colors = colors,
                            title = CategoryKey.DATE_TIME.label,
                            icon = AppIcons.forCategory(CategoryKey.DATE_TIME),
                            toolCount = allTools.count { it.category == CategoryKey.DATE_TIME },
                            gradient = Brush.linearGradient(listOf(Color(0xFF221E38), Color(0xFFC084FC))),
                            onClick = { activeHubCategory = CategoryKey.DATE_TIME }
                        )

                        // Gateway Hub 4: Health
                        HubCategoryCard(
                            colors = colors,
                            title = CategoryKey.HEALTH.label,
                            icon = AppIcons.forCategory(CategoryKey.HEALTH),
                            toolCount = allTools.count { it.category == CategoryKey.HEALTH },
                            gradient = Brush.linearGradient(listOf(Color(0xFF4C0519), Color(0xFFEF4444))),
                            onClick = { activeHubCategory = CategoryKey.HEALTH }
                        )

                        // Gateway Hub 5: Utilities
                        HubCategoryCard(
                            colors = colors,
                            title = CategoryKey.UTILITIES.label,
                            icon = AppIcons.forCategory(CategoryKey.UTILITIES),
                            toolCount = allTools.count { it.category == CategoryKey.UTILITIES },
                            gradient = Brush.linearGradient(listOf(Color(0xFF1F2937), Color(0xFF64748B))),
                            onClick = { activeHubCategory = CategoryKey.UTILITIES }
                        )
                    }
                }
            }
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
        CategoryKey.ISLAMIC -> Color(0xFF10B981)
        CategoryKey.FINANCE -> Color(0xFFF59E0B)
        CategoryKey.DATE_TIME -> Color(0xFFC084FC)
        CategoryKey.HEALTH -> Color(0xFFEF4444)
        CategoryKey.UTILITIES -> Color(0xFF64748B)
    }

    Surface(
        modifier = modifier
            .width(150.dp)
            .height(100.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = colors.surface.copy(alpha = 0.75f),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.accent.copy(alpha = 0.3f)),
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
                    tint = if (isFavorite) colors.accent else colors.textMuted,
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
        CategoryKey.ISLAMIC -> Color(0xFF10B981)
        CategoryKey.FINANCE -> Color(0xFFF59E0B)
        CategoryKey.DATE_TIME -> Color(0xFFC084FC)
        CategoryKey.HEALTH -> Color(0xFFEF4444)
        CategoryKey.UTILITIES -> Color(0xFF64748B)
    }

    Surface(
        modifier = modifier
            .height(130.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        color = colors.surface.copy(alpha = 0.75f),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.accent.copy(alpha = 0.3f)),
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
                    tint = if (isFavorite) colors.accent else colors.textMuted,
                    modifier = Modifier.size(15.dp)
                )
            }

            // Optional Badge tags
            if (tool.badge != null) {
                Surface(
                    color = when (tool.badge) {
                        "NEW" -> Color(0xFF22B573)
                        "HOT" -> Color(0xFFD4AF37)
                        "LIVE" -> Color(0xFFE45B5B)
                        "AI" -> Color(0xFFC084FC)
                        "PRO" -> Color(0xFF38BDF8)
                        else -> colors.accent
                    },
                    shape = RoundedCornerShape(bottomStart = 10.dp, topEnd = 24.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = tool.badge,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.appBg,
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
