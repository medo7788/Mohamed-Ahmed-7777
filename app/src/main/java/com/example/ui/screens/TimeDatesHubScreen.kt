package com.example.ui.screens

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CalcKey
import com.example.ui.theme.AppIcons
import com.example.ui.theme.CustomThemeColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

// ==========================================
// 1. TIME & DATES DATA MODELS & PALETTE
// ==========================================

data class TimeDateToolItem(
    val calcKey: CalcKey,
    val titleAr: String,
    val descriptionAr: String,
    val badgeType: String? = null // "LIVE", "TICKING", "SYNC"
)

// The 4 Core Time & Dates Tools Definition
val TIME_DATES_TOOLS_LIST = listOf(
    TimeDateToolItem(
        calcKey = CalcKey.WORLD_TIME,
        titleAr = "التوقيت العالمي",
        descriptionAr = "متابعة ساعات المدن العالمية، المناطق الزمنية وتحويل التوقيت الدولي",
        badgeType = "LIVE"
    ),
    TimeDateToolItem(
        calcKey = CalcKey.DATE,
        titleAr = "حاسبة التاريخ",
        descriptionAr = "حساب الفرق بين تاريخين باليوم والشهر والسنة وإضافة أو طرح أيام",
        badgeType = "SYNC"
    ),
    TimeDateToolItem(
        calcKey = CalcKey.AGE,
        titleAr = "حاسبة العمر",
        descriptionAr = "حساب دقيق للعمر بالتفصيل الهجري والميلادي واليوم المتبقي لميلادك المقبل"
    ),
    TimeDateToolItem(
        calcKey = CalcKey.COUNTDOWN,
        titleAr = "العد التنازلي",
        descriptionAr = "مؤقت تنازلي حي لتتبع المناسبات والأعياد والأحداث بالثواني والدقائق",
        badgeType = "TICKING"
    )
)

// Palette: Dark Obsidian & Neon Violet Edition
private val ElectricViolet = Color(0xFF9D7CFF)
private val NeonIndigo = Color(0xFF6366F1)
private val MintGlow = Color(0xFF63F4DD)
private val DarkObsidianStart = Color(0xFF07080D)
private val DarkObsidianEnd = Color(0xFF11131F)
private val GlassSurfaceDark = Color(0xFF131522)
private val CardHighlightDark = Color(0xFF1A1D2E)
private val PinActiveGold = Color(0xFFFFB800)
private val MutedSlateText = Color(0xFF94A3B8)

// ==========================================
// 2. MAIN TIME & DATES HUB SCREEN COMPOSABLE
// ==========================================

@Composable
fun TimeDatesHubScreen(
    colors: CustomThemeColors,
    favoriteTools: Set<String>,
    onToggleFavorite: (CalcKey) -> Unit,
    onToolClick: (CalcKey) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Real-time ticking system clock state (updates every second)
    var currentTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            delay(1000)
        }
    }

    // 5 Core Screen States
    var isLoading by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isOffline by remember { mutableStateOf(false) }
    var isSyncingTime by remember { mutableStateOf(false) }

    // Search and View Controls
    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var isCompactGrid by remember { mutableStateOf(false) } // false = 2-column grid, true = 1-column list

    // Network check & high-end initial loading flow
    fun checkAndLoad() {
        coroutineScope.launch {
            isLoading = true
            isError = false
            errorMessage = null

            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNetwork = connectivityManager?.activeNetwork
            val capabilities = connectivityManager?.getNetworkCapabilities(activeNetwork)
            isOffline = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) != true

            delay(350) // Smooth entrance
            isLoading = false
        }
    }

    fun syncNtpTime() {
        coroutineScope.launch {
            isSyncingTime = true
            delay(600)
            currentTimeMillis = System.currentTimeMillis()
            isSyncingTime = false
        }
    }

    LaunchedEffect(Unit) {
        checkAndLoad()
    }

    // Process tools list based on search query and pinned status
    val processedTools by remember(searchQuery, favoriteTools) {
        derivedStateOf {
            TIME_DATES_TOOLS_LIST.filter { item ->
                searchQuery.isBlank() ||
                        item.titleAr.contains(searchQuery, ignoreCase = true) ||
                        item.descriptionAr.contains(searchQuery, ignoreCase = true) ||
                        item.calcKey.keywords.any { it.contains(searchQuery, ignoreCase = true) }
            }.sortedByDescending { favoriteTools.contains(it.calcKey.name) } // Pinned tools on top
        }
    }

    // Responsive Adaptive Columns Count
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val columnsCount = remember(screenWidthDp, isCompactGrid) {
        if (isCompactGrid) 1
        else if (screenWidthDp >= 800) 4
        else if (screenWidthDp >= 600) 3
        else 2
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DarkObsidianStart, DarkObsidianEnd)
                )
            )
    ) {
        // --- A. Procedural Canvas Cosmic Time Rings Backdrop ---
        ProceduralCosmicTimeCanvas()

        // --- B. Main Scrollable Layout ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 1. Top Bar Navigation Header
            TimeDatesHubTopBar(
                colors = colors,
                title = "الوقت والتواريخ",
                isSearchExpanded = isSearchExpanded,
                isCompactGrid = isCompactGrid,
                isSyncing = isSyncingTime,
                onBackClick = onBackClick,
                onToggleSearch = { isSearchExpanded = !isSearchExpanded },
                onToggleViewMode = { isCompactGrid = !isCompactGrid },
                onSyncClick = { syncNtpTime() }
            )

            // 2. Expandable Glass Search Bar
            AnimatedVisibility(
                visible = isSearchExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                TimeDatesGlassSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onClearSearch = { searchQuery = "" },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            // 3. Non-Intrusive Offline Notice Banner
            AnimatedVisibility(
                visible = isOffline,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                TimeDatesOfflineNoticeBanner()
            }

            // 4. Category Hero Banner with Live Ticking System Clock
            TimeDatesHeroBanner(
                currentTimeMillis = currentTimeMillis,
                pinnedCount = favoriteTools.count { id -> TIME_DATES_TOOLS_LIST.any { it.calcKey.name == id } }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Section Header & Dynamic Counter Bar
            TimeDatesSectionHeaderCounter(
                title = "أدوات التصنيف المتاحة (${processedTools.size})"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 6. Content Views based on 5 Core UI States
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when {
                    // Loading State: Shimmer Skeleton Grid
                    isLoading -> {
                        TimeDatesSkeletonLoadingGrid(columnsCount = columnsCount)
                    }

                    // Error State: Graceful Error UI
                    isError -> {
                        TimeDatesErrorStateCard(
                            errorMessage = errorMessage ?: "حدث خطأ أثناء تحميل أدوات التوقيت والتقويم",
                            onRetry = { checkAndLoad() }
                        )
                    }

                    // Empty Search Results State
                    processedTools.isEmpty() -> {
                        TimeDatesEmptyStateCard(
                            searchQuery = searchQuery,
                            onClearSearch = { searchQuery = "" }
                        )
                    }

                    // Success State: Interactive 2-Column Grid
                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columnsCount),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp, top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(processedTools, key = { it.calcKey.id }) { toolItem ->
                                val isPinned = favoriteTools.contains(toolItem.calcKey.name)

                                TimeDatesGlassToolCard(
                                    toolItem = toolItem,
                                    isPinned = isPinned,
                                    isCompactList = isCompactGrid,
                                    onTogglePin = { onToggleFavorite(toolItem.calcKey) },
                                    onClick = { onToolClick(toolItem.calcKey) },
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. PROCEDURAL COSMIC TIME CANVAS
// ==========================================

@Composable
fun ProceduralCosmicTimeCanvas() {
    val infiniteTransition = rememberInfiniteTransition(label = "rotationTransition")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gearRotation"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                // Optimized hardware layer rendering for canvas graphics
                alpha = 0.99f
            }
    ) {
        val width = size.width
        val height = size.height

        // Top-Right Electric Violet Radial Glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(ElectricViolet.copy(alpha = 0.08f), Color.Transparent),
                center = Offset(width * 0.85f, height * 0.15f),
                radius = width * 0.65f
            )
        )

        // Bottom-Left Neon Indigo Radial Glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(NeonIndigo.copy(alpha = 0.06f), Color.Transparent),
                center = Offset(width * 0.15f, height * 0.85f),
                radius = width * 0.7f
            )
        )

        // Procedural Celestial Clock Rings & Ticks Center Top
        val centerX = width * 0.85f
        val centerY = height * 0.18f
        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 10f), 0f)

        // Outer dashed temporal orbit ring
        drawCircle(
            color = ElectricViolet.copy(alpha = 0.06f),
            center = Offset(centerX, centerY),
            radius = 160.dp.toPx(),
            style = Stroke(width = 1.5f, pathEffect = pathEffect)
        )

        // Inner dashed temporal orbit ring
        drawCircle(
            color = NeonIndigo.copy(alpha = 0.08f),
            center = Offset(centerX, centerY),
            radius = 100.dp.toPx(),
            style = Stroke(width = 1f)
        )

        // Rotating clock tick marks around celestial gear center
        val numTicks = 12
        val tickRadius = 130.dp.toPx()
        val tickLength = 10.dp.toPx()

        for (i in 0 until numTicks) {
            val angleRad = Math.toRadians((i * (360.0 / numTicks) + rotationAngle)).toFloat()
            val startX = centerX + cos(angleRad) * tickRadius
            val startY = centerY + sin(angleRad) * tickRadius
            val endX = centerX + cos(angleRad) * (tickRadius - tickLength)
            val endY = centerY + sin(angleRad) * (tickRadius - tickLength)

            drawLine(
                color = ElectricViolet.copy(alpha = 0.07f),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 2f
            )
        }
    }
}

// ==========================================
// 4. TOP BAR & SEARCH BAR COMPONENTS
// ==========================================

@Composable
fun TimeDatesHubTopBar(
    colors: CustomThemeColors,
    title: String,
    isSearchExpanded: Boolean,
    isCompactGrid: Boolean,
    isSyncing: Boolean,
    onBackClick: () -> Unit,
    onToggleSearch: () -> Unit,
    onToggleViewMode: () -> Unit,
    onSyncClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Right side (RTL): Back button + Title & Subtitle
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(GlassSurfaceDark.copy(alpha = 0.7f))
                    .border(1.dp, ElectricViolet.copy(alpha = 0.3f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "رجوع",
                    tint = ElectricViolet,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = "المركز الزمني والحاسبات الفلكية",
                    fontSize = 10.sp,
                    color = MutedSlateText,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Left side action buttons
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // NTP Sync / Refresh button
            IconButton(
                onClick = onSyncClick,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(GlassSurfaceDark.copy(alpha = 0.7f))
                    .border(1.dp, ElectricViolet.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "مزامنة الوقت",
                    tint = ElectricViolet,
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer {
                            if (isSyncing) rotationZ += 180f
                        }
                )
            }

            // View mode toggle button
            IconButton(
                onClick = onToggleViewMode,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(GlassSurfaceDark.copy(alpha = 0.7f))
                    .border(1.dp, ElectricViolet.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isCompactGrid) Icons.Default.GridView else Icons.Default.ViewList,
                    contentDescription = "تغيير العرض",
                    tint = ElectricViolet,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Search toggle button
            IconButton(
                onClick = onToggleSearch,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isSearchExpanded) ElectricViolet.copy(alpha = 0.2f) else GlassSurfaceDark.copy(alpha = 0.7f))
                    .border(1.dp, ElectricViolet.copy(alpha = if (isSearchExpanded) 0.6f else 0.2f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = "بحث",
                    tint = ElectricViolet,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun TimeDatesGlassSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = GlassSurfaceDark.copy(alpha = 0.85f),
        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = ElectricViolet,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = {
                    Text(
                        "ابحث عن التوقيت، العمر، أو مؤقت تنازلي...",
                        fontSize = 12.sp,
                        color = MutedSlateText
                    )
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.weight(1f)
            )

            if (query.isNotEmpty()) {
                IconButton(onClick = onClearSearch) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "مسح",
                        tint = MutedSlateText,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TimeDatesOfflineNoticeBanner() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = NeonIndigo.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, NeonIndigo.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.WifiOff,
                contentDescription = null,
                tint = MintGlow,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "وضع عدم الاتصال: حاسبة العمر وفروق التواريخ والمؤقت تعمل محلياً بكفاءة 100%.",
                fontSize = 11.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ==========================================
// 5. CATEGORY HERO BANNER WITH LIVE CLOCK
// ==========================================

@Composable
fun TimeDatesHeroBanner(
    currentTimeMillis: Long,
    pinnedCount: Int
) {
    val formattedTime = remember(currentTimeMillis) {
        val sdf = SimpleDateFormat("hh:mm:ss a", Locale("ar"))
        sdf.format(Date(currentTimeMillis))
    }

    val formattedDate = remember(currentTimeMillis) {
        val sdf = SimpleDateFormat("EEEE، d MMMM yyyy", Locale("ar"))
        sdf.format(Date(currentTimeMillis))
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(24.dp),
        color = GlassSurfaceDark.copy(alpha = 0.8f),
        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.35f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            ElectricViolet.copy(alpha = 0.15f),
                            GlassSurfaceDark.copy(alpha = 0.8f),
                            CardHighlightDark.copy(alpha = 0.5f)
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Calendar/Clock Icon with radial glow halo
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(ElectricViolet.copy(alpha = 0.15f))
                        .border(1.dp, ElectricViolet.copy(alpha = 0.4f), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = ElectricViolet,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "مركز الوقت والتواريخ",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        if (pinnedCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = PinActiveGold.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, PinActiveGold.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PushPin,
                                        contentDescription = null,
                                        tint = PinActiveGold,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "$pinnedCount مثبت",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PinActiveGold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "التوقيت العالمي، حاسبة العمر، فروق التواريخ، ومؤقت العد التنازلي",
                        fontSize = 11.sp,
                        color = MutedSlateText,
                        lineHeight = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Live Ticking Clock Display Strip
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.Black.copy(alpha = 0.35f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MintGlow.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(MintGlow)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = formattedTime,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MintGlow
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                text = "•  $formattedDate",
                                fontSize = 10.sp,
                                color = MutedSlateText
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 6. SECTION HEADER & COUNTER
// ==========================================

@Composable
fun TimeDatesSectionHeaderCounter(
    title: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(ElectricViolet)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

// ==========================================
// 7. INTERACTIVE TIME TOOL CARD (GRID & LIST)
// ==========================================

@Composable
fun TimeDatesGlassToolCard(
    toolItem: TimeDateToolItem,
    isPinned: Boolean,
    isCompactList: Boolean,
    onTogglePin: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Tactile Scale Press Feedback
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "pressScale"
    )

    // Micro-badge infinite pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val badgeGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "badgePulse"
    )

    Surface(
        modifier = modifier
            .scale(scale)
            .fillMaxWidth()
            .then(if (isCompactList) Modifier.height(96.dp) else Modifier.height(140.dp))
            .clip(RoundedCornerShape(22.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            },
        shape = RoundedCornerShape(22.dp),
        color = GlassSurfaceDark.copy(alpha = 0.8f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isPinned) PinActiveGold.copy(alpha = 0.6f) else ElectricViolet.copy(alpha = 0.25f)
        ),
        shadowElevation = if (isPinned) 4.dp else 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            CardHighlightDark.copy(alpha = 0.4f),
                            GlassSurfaceDark.copy(alpha = 0.8f)
                        )
                    )
                )
        ) {
            // Top-Left Pin Action Button
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(DarkObsidianStart.copy(alpha = 0.6f))
                    .clickable { onTogglePin() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                    contentDescription = "تثبيت الأداة",
                    tint = if (isPinned) PinActiveGold else MutedSlateText,
                    modifier = Modifier.size(14.dp)
                )
            }

            // Dynamic Micro-Badges ("LIVE", "TICKING", "SYNC")
            if (toolItem.badgeType != null) {
                val badgeColor = when (toolItem.badgeType) {
                    "LIVE" -> MintGlow
                    "TICKING" -> ElectricViolet
                    else -> NeonIndigo
                }

                Surface(
                    color = badgeColor.copy(alpha = badgeGlowAlpha * 0.25f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = badgeGlowAlpha)),
                    shape = RoundedCornerShape(bottomStart = 10.dp, topEnd = 22.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(badgeColor)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = toolItem.badgeType,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = badgeColor
                        )
                    }
                }
            }

            // Card Inner Content
            if (isCompactList) {
                // Horizontal List Item Layout
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 48dp Rounded Square Icon Container
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(ElectricViolet.copy(alpha = 0.12f))
                            .border(1.dp, ElectricViolet.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = AppIcons.forCalc(toolItem.calcKey),
                            contentDescription = null,
                            tint = ElectricViolet,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = toolItem.titleAr,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = toolItem.descriptionAr,
                            fontSize = 11.sp,
                            color = MutedSlateText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = null,
                        tint = ElectricViolet.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                // Vertical 2-Column Grid Card Layout
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // 48dp Rounded Square Icon Container
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(ElectricViolet.copy(alpha = 0.12f))
                            .border(1.dp, ElectricViolet.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = AppIcons.forCalc(toolItem.calcKey),
                            contentDescription = null,
                            tint = ElectricViolet,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = toolItem.titleAr,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = toolItem.descriptionAr,
                            fontSize = 10.sp,
                            color = MutedSlateText,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 8. STATE IMPLEMENTATIONS (LOADING, EMPTY, ERROR)
// ==========================================

@Composable
fun TimeDatesSkeletonLoadingGrid(columnsCount: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(columnsCount),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(4) {
            Surface(
                modifier = Modifier
                    .height(140.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = GlassSurfaceDark.copy(alpha = shimmerAlpha),
                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                    )
                }
            }
        }
    }
}

@Composable
fun TimeDatesEmptyStateCard(
    searchQuery: String,
    onClearSearch: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = GlassSurfaceDark.copy(alpha = 0.8f),
            border = androidx.compose.foundation.BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.SearchOff,
                    contentDescription = null,
                    tint = ElectricViolet,
                    modifier = Modifier.size(52.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "لم نجد أداة مطابقة لـ \"$searchQuery\"",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "تأكد من كتابة اسم الأداة مثل: العمر، التوقيت، التاريخ أو المؤقت",
                    fontSize = 11.sp,
                    color = MutedSlateText,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onClearSearch,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("إعادة ضبط البحث", color = DarkObsidianStart, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TimeDatesErrorStateCard(
    errorMessage: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = GlassSurfaceDark.copy(alpha = 0.8f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF4D4D).copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFF4D4D),
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "تعذر تحميل أدوات التوقيت والتواريخ",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = errorMessage,
                    fontSize = 11.sp,
                    color = MutedSlateText,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4D4D)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إعادة المحاولة", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
