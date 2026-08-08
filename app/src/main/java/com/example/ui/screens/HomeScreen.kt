package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.IslamicData
import com.example.data.WeatherRepository
import com.example.model.CalcKey
import com.example.model.CategoryKey
import com.example.ui.components.GlassSearchBar
import com.example.ui.theme.AppIcons
import com.example.ui.theme.AppThemeKey
import com.example.ui.theme.CustomThemeColors
import com.example.util.AppLocationProvider
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ==========================================
// LUXURY CYBER OBSIDIAN PALETTE
// ==========================================
private val ColorObsidianBgStart = Color(0xFF080A0F)
private val ColorObsidianBgEnd = Color(0xFF121620)
private val ColorGlassCard = Color(0xFF141926).copy(alpha = 0.85f)
private val ColorGoldBorder = Color(0xFFD4AF37)
private val ColorAmberGlow = Color(0xFFF59E0B)
private val ColorSapphireBlue = Color(0xFF3B82F6)
private val ColorIceCyan = Color(0xFF00F2FE)
private val ColorEmeraldMint = Color(0xFF10B981)
private val ColorCrimsonRed = Color(0xFFEF4444)
private val ColorSlateMuted = Color(0xFF94A3B8)
private val ColorHeadlineText = Color(0xFFF8FAFC)
private val ColorPurpleAI = Color(0xFFC084FC)

// ==========================================
// HOME UI STATE (IMMUTABLE STATE PATTERN)
// ==========================================
data class HomeUiState(
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val searchQuery: String = "",
    val selectedCategory: CategoryKey? = null,
    val nextPrayerName: String = "العصر",
    val nextPrayerCountdown: String = "01:24:10",
    val cityName: String = "القاهرة، مصر",
    val weatherTemp: Int? = 32,
    val weatherCondition: String = "مشمس",
    val userName: String = "أحمد",
    val isDarkTheme: Boolean = true,
    val batteryLevel: Int = 85,
    val storageAvailableGbs: String = "12.4GB",
    val favoriteTools: Set<String> = emptySet(),
    val recentHistory: List<String> = emptyList(),
    val errorMessage: String? = null
)

// ==========================================
// MAIN HOME DASHBOARD COMPOSABLE
// ==========================================
@Composable
fun HomeScreen(
    colors: CustomThemeColors,
    viewModel: MainViewModel,
    onSelectCalc: (CalcKey) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val configuration = LocalConfiguration.current

    // Reactive State Collections from ViewModel
    val favoriteTools by viewModel.favoriteTools.collectAsState()
    val recentTools by viewModel.recentTools.collectAsState()
    val userNameSaved by viewModel.userName.collectAsState()
    val currentThemeKey by viewModel.currentThemeKey.collectAsState()

    // Screen State
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var activeHubCategory by rememberSaveable(
        stateSaver = androidx.compose.runtime.saveable.Saver<CategoryKey?, String>(
            save = { it?.name ?: "" },
            restore = { name -> if (name.isBlank()) null else CategoryKey.valueOf(name) }
        )
    ) { mutableStateOf<CategoryKey?>(null) }

    // Dialog & UI State
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var isLoadingState by remember { mutableStateOf(true) }
    var errorMessageState by remember { mutableStateOf<String?>(null) }

    // Live Device Sensors State
    var batteryLevel by remember { mutableStateOf(85) }
    var availableStorage by remember { mutableStateOf("12.4GB") }
    var isOffline by remember { mutableStateOf(false) }

    // Live Prayer & Weather State
    var cityLabel by remember { mutableStateOf("القاهرة، مصر") }
    var weatherTempC by remember { mutableStateOf<Int?>(32) }
    var weatherConditionAr by remember { mutableStateOf("مشمس") }
    var nextPrayerName by remember { mutableStateOf("العصر") }
    var nextPrayerText by remember { mutableStateOf("01:24:10") }

    // Infinite breathing glow animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlowAlpha"
    )

    // Initial Load & Device Sensor Monitor Side Effect
    LaunchedEffect(Unit) {
        // Battery check
        try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val bat = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 85
            if (bat in 1..100) batteryLevel = bat
        } catch (_: Exception) {}

        // Storage check
        try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val bytesAvailable = stat.availableBlocksLong * stat.blockSizeLong
            val gbs = bytesAvailable / (1024.0 * 1024.0 * 1024.0)
            availableStorage = String.format(Locale.US, "%.1fGB", gbs)
        } catch (_: Exception) {}

        // Network check
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = cm?.activeNetwork
            val caps = cm?.getNetworkCapabilities(network)
            isOffline = caps == null || (!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) && !caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
        } catch (_: Exception) {}

        // Fast shimmer loading transition
        delay(600)
        isLoadingState = false

        // Location & Prayer calculation
        val locResult = AppLocationProvider.getLastKnownLocation(context)
        val (lat, lng) = when (locResult) {
            is AppLocationProvider.Result.Success -> locResult.latitude to locResult.longitude
            else -> {
                val cached = AppLocationProvider.getCachedLocation(context)
                if (cached != null) {
                    cityLabel = cached.placeName ?: cityLabel
                    cached.lat to cached.lng
                } else {
                    30.0444 to 31.2357 // Default Cairo
                }
            }
        }

        // Live Weather
        launch {
            try {
                val weather = WeatherRepository.fetchRealWeather(context, lat, lng)
                weatherTempC = weather.tempC.toInt()
                weatherConditionAr = weather.conditionAr
            } catch (_: Exception) {}
        }

        // Live Prayer Countdown
        try {
            val tzOffset = IslamicData.getCorrectTimezoneOffset(lat, lng)
            val times = IslamicData.calculatePrayerTimes(lat, lng, tzOffset)
            val now = Calendar.getInstance()
            val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

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
            val next = prayers.firstOrNull { it.second > nowMinutes } ?: prayers.first()
            val minutesUntil = if (next.second > nowMinutes) next.second - nowMinutes else (1440 - nowMinutes + next.second)

            nextPrayerName = next.first
            nextPrayerText = if (minutesUntil < 60) {
                "$minutesUntil دقيقة"
            } else {
                "${minutesUntil / 60} س ${minutesUntil % 60} د"
            }
        } catch (_: Exception) {}
    }

    // All available tools filter logic
    val allTools = remember { CalcKey.values().filter { it != CalcKey.HOME && it != CalcKey.SETTINGS } }
    val filteredTools = remember(searchQuery) {
        allTools.filter { tool ->
            searchQuery.isBlank() ||
                    tool.title.contains(searchQuery, ignoreCase = true) ||
                    tool.keywords.any { it.contains(searchQuery, ignoreCase = true) }
        }
    }

    // Dynamic Time-of-Day Greeting
    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val dynamicGreetingText = when {
        currentHour in 5..11 -> "صباح الخير والبركة ☀️"
        currentHour in 12..16 -> "طاب يومك بكل خير 🌤️"
        currentHour in 17..21 -> "مساء النور والسرور 🌙"
        else -> "أسعد الله مساؤك بالخير ✨"
    }

    // Date formatting
    val arabicLocale = remember { Locale("ar") }
    val dateCalendar = remember { Date() }
    val dayName = remember { SimpleDateFormat("EEEE", arabicLocale).format(dateCalendar) }
    val dayOfMonth = remember { SimpleDateFormat("d MMMM yyyy", arabicLocale).format(dateCalendar) }
    val hijriDateStr = remember {
        val hc = GregorianCalendar()
        val hYear = hc.get(Calendar.YEAR) - 579
        val hMonths = listOf("محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة")
        "15 ${hMonths[((hc.get(Calendar.MONTH) + 5) % 12)]} $hYear هـ"
    }

    // Category Hub Overlay handle
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
                .background(
                    Brush.verticalGradient(
                        colors = listOf(ColorObsidianBgStart, ColorObsidianBgEnd)
                    )
                )
        ) {
            // Background Canvas Micro-Tech Grid Pattern
            Canvas(modifier = Modifier.fillMaxSize()) {
                val step = 64.dp.toPx()
                val gridPaint = ColorGoldBorder.copy(alpha = 0.03f)
                for (x in 0..size.width.toInt() step step.toInt()) {
                    drawLine(gridPaint, Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height), strokeWidth = 1f)
                }
                for (y in 0..size.height.toInt() step step.toInt()) {
                    drawLine(gridPaint, Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), strokeWidth = 1f)
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. OFFLINE BANNER (IF APPLICABLE)
                if (isOffline) {
                    item(key = "offline_banner") {
                        Surface(
                            color = ColorAmberGlow.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, ColorAmberGlow)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.WifiOff, contentDescription = null, tint = ColorAmberGlow, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "يعمل دون اتصال - البيانات مخزنة محلياً بأمان",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Text("محفوظ", fontSize = 10.sp, color = ColorSlateMuted)
                            }
                        }
                    }
                }

                // 2. ERROR STATE BANNER (IF ANY)
                if (errorMessageState != null) {
                    item(key = "error_banner") {
                        Surface(
                            color = ColorCrimsonRed.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, ColorCrimsonRed)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = ColorCrimsonRed)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(errorMessageState!!, fontSize = 12.sp, color = Color.White)
                                }
                                Button(
                                    onClick = { errorMessageState = null },
                                    colors = ButtonDefaults.buttonColors(containerColor = ColorCrimsonRed),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("إعادة المحاولة", fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }

                // 3. SECTION A: Top Glass Header & Personalized Status Bar
                item(key = "section_a_header") {
                    HeaderGlassPanel(
                        greeting = dynamicGreetingText,
                        userName = userNameSaved,
                        hijriDate = hijriDateStr,
                        gregorianDate = "$dayName، $dayOfMonth",
                        batteryLevel = batteryLevel,
                        availableStorage = availableStorage,
                        isDarkTheme = currentThemeKey == AppThemeKey.ELEGANT_DARK,
                        onToggleTheme = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.toggleTheme(context)
                        },
                        onEditNameClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showEditNameDialog = true
                        },
                        onNotificationClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showNotificationsDialog = true
                        }
                    )
                }

                // 4. SKELETON SHIMMER LOADING OR CONTENT
                if (isLoadingState) {
                    item(key = "skeleton_loading") {
                        SkeletonShimmerDashboardCard(pulseGlowAlpha = pulseGlowAlpha)
                    }
                } else {
                    // 5. SECTION B: Quick Action Buttons (Horizontal LazyRow)
                    item(key = "section_b_quick_actions") {
                        QuickActionRow(
                            onSelectAction = { calcKey ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSelectCalc(calcKey)
                            },
                            onSelectCategory = { catKey ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                activeHubCategory = catKey
                            }
                        )
                    }

                    // 6. UNIFIED HERO CARD: MERGED NEXT PRAYER & LIVE WEATHER
                    item(key = "section_unified_hero") {
                        UnifiedHeroPrayerWeatherCard(
                            prayerName = nextPrayerName,
                            countdownText = nextPrayerText,
                            tempC = weatherTempC ?: 32,
                            weatherCondition = weatherConditionAr,
                            cityName = cityLabel,
                            pulseAlpha = pulseGlowAlpha,
                            onNavigateToPrayer = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSelectCalc(CalcKey.PRAYER)
                            },
                            onNavigateToWeather = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSelectCalc(CalcKey.WEATHER)
                            }
                        )
                    }

                    // 7. UNIVERSAL SEARCH BAR & 6-SECTION CATEGORY SLIDER
                    item(key = "section_search_and_categories") {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            // Sapphire AI Glass Search Bar
                            AISearchBar(
                                query = searchQuery,
                                onQueryChange = { searchQuery = it },
                                pulseAlpha = pulseGlowAlpha,
                                onAIClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSelectCalc(CalcKey.AI)
                                }
                            )

                            // 6 Sections Horizontal Slider (Replaces individual tool chips & removes big bottom block)
                            PlatformCategorySliderRow(
                                onSelectCategory = { catKey ->
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    activeHubCategory = catKey
                                },
                                onSelectAi = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSelectCalc(CalcKey.AI)
                                }
                            )
                        }
                    }

                    // 8. SEARCH RESULTS OVERLAY OR STANDARD CATEGORY GRID
                    if (searchQuery.isNotBlank()) {
                        item(key = "search_results_section") {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "نتائج البحث لـ \"$searchQuery\" (${filteredTools.size})",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorAmberGlow
                                    )
                                    TextButton(onClick = { searchQuery = "" }) {
                                        Text("إلغاء البحث", color = ColorSlateMuted, fontSize = 11.sp)
                                    }
                                }

                                if (filteredTools.isEmpty()) {
                                    // EMPTY STATE
                                    EmptySearchResultsCard(onResetSearch = { searchQuery = "" })
                                } else {
                                    // GRID OF MATCHED TOOLS
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        filteredTools.chunked(2).forEach { rowTools ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                rowTools.forEach { tool ->
                                                    CyberToolGridCard(
                                                        tool = tool,
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
                    } else {
                        // 9. SECTION G: Quick Insights & Recent Activity Bar (If Any Recents)
                        if (recentTools.isNotEmpty()) {
                            item(key = "section_g_recents") {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        "آخر الأدوات المستخدمة ⏱️",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )

                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(recentTools, key = { it }) { toolId ->
                                            val tool = allTools.find { it.name == toolId }
                                            if (tool != null) {
                                                RecentToolChip(
                                                    tool = tool,
                                                    onClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        onSelectCalc(tool)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }



                        // 11. PINNED / FAVORITES SECTION (IF ANY)
                        if (favoriteTools.isNotEmpty()) {
                            item(key = "section_favorites") {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        "أدواتك المثبتة (المفضلة) ⭐",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )

                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(favoriteTools.toList(), key = { it }) { toolName ->
                                            val tool = allTools.find { it.name == toolName }
                                            if (tool != null) {
                                                FavoriteToolMiniCard(
                                                    tool = tool,
                                                    onUnpin = { viewModel.toggleFavorite(context, tool.name) },
                                                    onClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        onSelectCalc(tool)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 12. FOOTER & VERIFIED STAMP
                        item(key = "footer_stamp") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Verified, contentDescription = null, tint = ColorEmeraldMint, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("المنصة الذكية المتكاملة • إصدار برو المحترف 2026", fontSize = 11.sp, color = ColorSlateMuted)
                            }
                        }
                    }
                }
            }

            // MODAL DIALOG: EDIT USER NAME
            if (showEditNameDialog) {
                EditNameDialog(
                    currentName = userNameSaved,
                    onDismiss = { showEditNameDialog = false },
                    onSaveName = { newName ->
                        viewModel.setUserName(context, newName)
                        showEditNameDialog = false
                    }
                )
            }

            // MODAL DIALOG: NOTIFICATIONS
            if (showNotificationsDialog) {
                NotificationsDialog(onDismiss = { showNotificationsDialog = false })
            }
        }
    }
}

// ==========================================
// SHARED PREMIUM TOOL CARD FOR HUBSCREEN COMPATIBILITY
// ==========================================
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
        border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.3f)),
        shadowElevation = 3.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
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

// ==========================================
// SUB-COMPONENTS
// ==========================================

@Composable
private fun HeaderGlassPanel(
    greeting: String,
    userName: String,
    hijriDate: String,
    gregorianDate: String,
    batteryLevel: Int,
    availableStorage: String,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onEditNameClick: () -> Unit,
    onNotificationClick: () -> Unit
) {
    Surface(
        color = ColorGlassCard,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, ColorGoldBorder.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Row 1: Greeting + Name + Quick Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.clickable { onEditNameClick() }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "$greeting $userName",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ColorAmberGlow
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Outlined.Edit, contentDescription = "تعديل الاسم", tint = ColorSlateMuted, modifier = Modifier.size(14.dp))
                    }
                    Text("المنصة الذكية المتكاملة", fontSize = 11.sp, color = ColorSlateMuted)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Theme Toggle Button (Moon/Sun)
                    Surface(
                        color = Color(0xFF1E2638),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, ColorGoldBorder.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .size(38.dp)
                            .clickable { onToggleTheme() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                if (isDarkTheme) "🌙" else "☀️",
                                fontSize = 16.sp
                            )
                        }
                    }

                    // Notification Icon with Gold Dot Badge
                    Surface(
                        color = Color(0xFF1E2638),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                        modifier = Modifier
                            .size(38.dp)
                            .clickable { onNotificationClick() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Notifications, contentDescription = "التنبيهات", tint = Color.White, modifier = Modifier.size(18.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(ColorAmberGlow)
                                    .align(Alignment.TopEnd)
                            )
                        }
                    }
                }
            }

            Divider(color = Color.White.copy(alpha = 0.08f))

            // Row 2: Live Date Banner + Device Status Indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(gregorianDate, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ColorHeadlineText)
                    Text(hijriDate, fontSize = 11.sp, color = ColorGoldBorder)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Battery indicator
                    Surface(
                        color = Color(0xFF1E2638),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🔋 $batteryLevel%", fontSize = 10.sp, color = ColorEmeraldMint, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Storage indicator
                    Surface(
                        color = Color(0xFF1E2638),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("💾 $availableStorage", fontSize = 10.sp, color = ColorIceCyan, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionRow(
    onSelectAction: (CalcKey) -> Unit,
    onSelectCategory: (CategoryKey) -> Unit
) {
    val actions = listOf(
        Triple("مواقيت الصلاة", "🕌", CalcKey.PRAYER),
        Triple("اتجاه القبلة", "🧭", CalcKey.QIBLA),
        Triple("حاسبة متطورة", "🧮", CalcKey.BASIC),
        Triple("الأذكار", "📿", CalcKey.ADHKAR)
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(actions, key = { it.third }) { (label, emoji, key) ->
            var isPressed by remember { mutableStateOf(false) }
            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.94f else 1f,
                animationSpec = spring(stiffness = Spring.StiffnessHigh),
                label = "pressScale"
            )

            Surface(
                color = ColorGlassCard,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, ColorGoldBorder.copy(alpha = 0.3f)),
                modifier = Modifier
                    .scale(scale)
                    .clickable {
                        isPressed = true
                        onSelectAction(key)
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(emoji, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // Quick Category Action: Utilities Hub
        item {
            Surface(
                color = ColorGlassCard,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, ColorGoldBorder.copy(alpha = 0.3f)),
                modifier = Modifier.clickable { onSelectCategory(CategoryKey.UTILITIES) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⚙️", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("أدوات سريعة", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun UnifiedHeroPrayerWeatherCard(
    prayerName: String,
    countdownText: String,
    tempC: Int,
    weatherCondition: String,
    cityName: String,
    pulseAlpha: Float,
    onNavigateToPrayer: () -> Unit,
    onNavigateToWeather: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = ColorGlassCard,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(
            1.5.dp,
            Brush.horizontalGradient(
                colors = listOf(
                    ColorEmeraldMint.copy(alpha = pulseAlpha),
                    ColorGoldBorder.copy(alpha = 0.6f),
                    ColorIceCyan.copy(alpha = 0.5f)
                )
            )
        ),
        shadowElevation = 8.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Bar: Location & Real-Time Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E2638))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = ColorIceCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = cityName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Surface(
                    color = ColorEmeraldMint.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ColorEmeraldMint.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(ColorEmeraldMint)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "مباشر ودقيق 📍",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorEmeraldMint
                        )
                    }
                }
            }

            // Split Interactive Content Section (Prayer | Weather)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // PRAYER TIME COLUMN (Clickable)
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF182030).copy(alpha = 0.6f))
                        .border(1.dp, ColorEmeraldMint.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                        .clickable { onNavigateToPrayer() }
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🕌", fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("الصلاة القادمة", fontSize = 11.sp, color = ColorSlateMuted, fontWeight = FontWeight.Medium)
                        }
                        Text("➔", fontSize = 11.sp, color = ColorEmeraldMint, fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = "صلاة $prayerName",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("خلال: ", fontSize = 11.sp, color = ColorSlateMuted)
                        Text(
                            text = countdownText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ColorAmberGlow
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // VERTICAL DIVIDER
                Box(
                    modifier = Modifier
                        .height(68.dp)
                        .width(1.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, ColorGoldBorder.copy(alpha = 0.5f), Color.Transparent)
                            )
                        )
                )

                Spacer(modifier = Modifier.width(10.dp))

                // WEATHER COLUMN (Clickable)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF182030).copy(alpha = 0.6f))
                        .border(1.dp, ColorIceCyan.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                        .clickable { onNavigateToWeather() }
                        .padding(12.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("☀️", fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("الطقس", fontSize = 11.sp, color = ColorSlateMuted, fontWeight = FontWeight.Medium)
                        }
                        Text("➔", fontSize = 11.sp, color = ColorIceCyan, fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = "$tempC°م",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = ColorIceCyan
                    )

                    Text(
                        text = weatherCondition,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun AISearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    pulseAlpha: Float,
    onAIClick: () -> Unit
) {
    Surface(
        color = ColorGlassCard,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, ColorSapphireBlue.copy(alpha = pulseAlpha)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Search, contentDescription = "بحث", tint = ColorIceCyan, modifier = Modifier.size(20.dp))

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("ابحث عن أي أداة، حاسبة، أو ميزة...", fontSize = 12.sp, color = ColorSlateMuted) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = "مسح", tint = ColorSlateMuted, modifier = Modifier.size(16.dp))
                }
            } else {
                Surface(
                    color = ColorPurpleAI.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ColorPurpleAI),
                    modifier = Modifier.clickable { onAIClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI", tint = ColorPurpleAI, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("AI", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ColorPurpleAI)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlatformCategorySliderRow(
    onSelectCategory: (CategoryKey) -> Unit,
    onSelectAi: () -> Unit
) {
    val categoryItems = listOf(
        CategorySliderItem("العبادات والقرآن", "7 أدوات معتمدة", AppIcons.forCategory(CategoryKey.ISLAMIC), ColorEmeraldMint, "🕌", CategoryKey.ISLAMIC),
        CategorySliderItem("المال والأسعار", "14 حاسبة ومؤشر", AppIcons.forCategory(CategoryKey.FINANCE), ColorGoldBorder, "💰", CategoryKey.FINANCE),
        CategorySliderItem("الوقت والتواريخ", "4 أدوات تحويل", AppIcons.forCategory(CategoryKey.DATE_TIME), ColorPurpleAI, "⏱️", CategoryKey.DATE_TIME),
        CategorySliderItem("الصحة واللياقة", "مؤشرات وتتبع", AppIcons.forCategory(CategoryKey.HEALTH), ColorCrimsonRed, "🩺", CategoryKey.HEALTH),
        CategorySliderItem("أدوات عملية", "10 أدوات مساعدة", AppIcons.forCategory(CategoryKey.UTILITIES), ColorSapphireBlue, "🛠️", CategoryKey.UTILITIES),
        CategorySliderItem("المستشار الذكي AI", "تحليلات واستشارات", Icons.Default.AutoAwesome, ColorIceCyan, "✨", null)
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "أقسام المنصة والشاشات الرئيسية",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = ColorHeadlineText
            )
            Text(
                text = "6 أقسام ↔️",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = ColorGoldBorder
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categoryItems, key = { it.title }) { item ->
                Surface(
                    color = ColorGlassCard,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, item.color.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .width(155.dp)
                        .clickable {
                            if (item.categoryKey != null) {
                                onSelectCategory(item.categoryKey)
                            } else {
                                onSelectAi()
                            }
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(item.color.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    tint = item.color,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(text = item.badge, fontSize = 14.sp)
                        }

                        Column {
                            Text(
                                text = item.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.subtitle,
                                fontSize = 10.sp,
                                color = ColorSlateMuted,
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

private data class CategorySliderItem(
    val title: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color,
    val badge: String,
    val categoryKey: CategoryKey?
)

@Composable
private fun CyberToolGridCard(
    tool: CalcKey,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryColor = when (tool.category) {
        CategoryKey.ISLAMIC -> ColorEmeraldMint
        CategoryKey.FINANCE -> ColorGoldBorder
        CategoryKey.DATE_TIME -> ColorPurpleAI
        CategoryKey.HEALTH -> ColorCrimsonRed
        CategoryKey.UTILITIES -> ColorSapphireBlue
    }

    Surface(
        color = ColorGlassCard,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, categoryColor.copy(alpha = 0.3f)),
        modifier = modifier.clickable { onClick() }
    ) {
        Box(modifier = Modifier.padding(12.dp)) {
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.TopEnd)
            ) {
                Icon(
                    if (isFavorite) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                    contentDescription = "تثبيت",
                    tint = if (isFavorite) ColorGoldBorder else ColorSlateMuted,
                    modifier = Modifier.size(14.dp)
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(categoryColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(AppIcons.forCalc(tool), contentDescription = null, tint = categoryColor, modifier = Modifier.size(18.dp))
                }

                Column {
                    Text(
                        tool.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        tool.category.label,
                        fontSize = 9.sp,
                        color = ColorSlateMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentToolChip(
    tool: CalcKey,
    onClick: () -> Unit
) {
    Surface(
        color = ColorGlassCard,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(AppIcons.forCalc(tool), contentDescription = null, tint = ColorIceCyan, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(tool.title, fontSize = 11.sp, color = Color.White)
        }
    }
}

@Composable
private fun FavoriteToolMiniCard(
    tool: CalcKey,
    onUnpin: () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        color = ColorGlassCard,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, ColorGoldBorder.copy(alpha = 0.4f)),
        modifier = Modifier
            .width(130.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(AppIcons.forCalc(tool), contentDescription = null, tint = ColorAmberGlow, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(tool.title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onUnpin, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "إلغاء التثبيت", tint = ColorSlateMuted, modifier = Modifier.size(12.dp))
            }
        }
    }
}

@Composable
private fun SkeletonShimmerDashboardCard(pulseGlowAlpha: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(ColorGlassCard.copy(alpha = pulseGlowAlpha))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(ColorGlassCard.copy(alpha = pulseGlowAlpha))
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(ColorGlassCard.copy(alpha = pulseGlowAlpha))
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(ColorGlassCard.copy(alpha = pulseGlowAlpha))
            )
        }
    }
}

@Composable
private fun EmptySearchResultsCard(onResetSearch: () -> Unit) {
    Surface(
        color = ColorGlassCard,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Filled.SearchOff, contentDescription = null, tint = ColorSlateMuted, modifier = Modifier.size(48.dp))
            Text("لم نجد أي أداة تطابق كلمات بحثك", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
            Text("تأكد من كتابة الكلمات بشكل صحيح أو جرب البحث بقسم آخر", fontSize = 11.sp, color = ColorSlateMuted, textAlign = TextAlign.Center)
            Button(
                onClick = onResetSearch,
                colors = ButtonDefaults.buttonColors(containerColor = ColorAmberGlow),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("إعادة ضبط البحث", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun EditNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSaveName: (String) -> Unit
) {
    var nameText by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تحديث الاسم الشخصي", color = ColorAmberGlow, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("أدخل اسمك ليظهر في التحية اليومية للوحة التحكم:", fontSize = 12.sp, color = Color.White)
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ColorAmberGlow,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameText.isNotBlank()) {
                        onSaveName(nameText.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ColorAmberGlow),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("حفظ", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = ColorSlateMuted)
            }
        },
        containerColor = Color(0xFF141926),
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun NotificationsDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Notifications, contentDescription = null, tint = ColorAmberGlow)
                Spacer(modifier = Modifier.width(8.dp))
                Text("التنبيهات والإشعارات", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    color = Color(0xFF1E2638),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("🕌", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("تنبيه صلاة العصر", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("متبقي 01:24:10 على رفع أذان العصر", fontSize = 10.sp, color = ColorSlateMuted)
                        }
                    }
                }

                Surface(
                    color = Color(0xFF1E2638),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("💰", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("تحديث أسعار الذهب اليوم", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("عيار 21 يسجل 4,550 ج.م بزيادة طفيفة", fontSize = 10.sp, color = ColorSlateMuted)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = ColorAmberGlow),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("إغلاق", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color(0xFF141926),
        shape = RoundedCornerShape(20.dp)
    )
}
