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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CalcKey
import com.example.model.CategoryKey
import com.example.ui.theme.AppIcons
import com.example.ui.theme.CustomThemeColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ==========================================
// 1. FINANCIAL HUB DATA MODELS & SUBGROUPS
// ==========================================

enum class FinanceToolSubgroup(val id: String, val titleAr: String, val icon: ImageVector) {
    ALL("all", "الكل", Icons.Default.Apps),
    LIVE_MARKET("live", "أسعار مباشرة", Icons.Default.ShowChart),
    FINANCIAL("financial", "حاسبات مالية", Icons.Default.AccountBalance),
    DISCOUNTS("discounts", "خصومات ونسب", Icons.Default.LocalOffer),
    PRACTICAL("practical", "حاسبات رياضية", Icons.Default.Calculate)
}

data class FinanceToolItem(
    val calcKey: CalcKey,
    val titleAr: String,
    val descriptionAr: String,
    val subgroup: FinanceToolSubgroup,
    val badgeType: String? = null // "LIVE", "AI", etc.
)

// The 13 Financial Tools Definition
val FINANCE_TOOLS_LIST = listOf(
    FinanceToolItem(
        calcKey = CalcKey.BASIC,
        titleAr = "الآلة الحاسبة",
        descriptionAr = "حسابات عامة وعلمية وسريعة بدقة عالية",
        subgroup = FinanceToolSubgroup.PRACTICAL
    ),
    FinanceToolItem(
        calcKey = CalcKey.CURRENCY,
        titleAr = "محول العملات",
        descriptionAr = "أسعار الصرف والتحويل بين العملات العربية والعالمية",
        subgroup = FinanceToolSubgroup.FINANCIAL
    ),
    FinanceToolItem(
        calcKey = CalcKey.UNIT,
        titleAr = "محول الوحدات",
        descriptionAr = "تحويل أطوال، أوزان، مساحات وحجم بدقة",
        subgroup = FinanceToolSubgroup.PRACTICAL
    ),
    FinanceToolItem(
        calcKey = CalcKey.GOLD,
        titleAr = "حاسبة الذهب",
        descriptionAr = "حساب قيمة عيارات الذهب والسبائك والمصنعية",
        subgroup = FinanceToolSubgroup.FINANCIAL
    ),
    FinanceToolItem(
        calcKey = CalcKey.LIVE_PRICES,
        titleAr = "الأسعار الحية",
        descriptionAr = "تتبع أسعار الذهب والفضة والعملات لحظة بلحظة",
        subgroup = FinanceToolSubgroup.LIVE_MARKET,
        badgeType = "LIVE"
    ),
    FinanceToolItem(
        calcKey = CalcKey.ECONOMIC_INDICATORS,
        titleAr = "مؤشرات الاقتصاد",
        descriptionAr = "تحليلات التضخم، الفائدة والتضخم مدعوم بالذكاء الاصطناعي",
        subgroup = FinanceToolSubgroup.LIVE_MARKET,
        badgeType = "AI"
    ),
    FinanceToolItem(
        calcKey = CalcKey.DISCOUNT,
        titleAr = "الخصم والتخفيض",
        descriptionAr = "احسب السعر النهائي ومقدار التوفير في التنزيلات",
        subgroup = FinanceToolSubgroup.DISCOUNTS
    ),
    FinanceToolItem(
        calcKey = CalcKey.LOAN,
        titleAr = "حاسبة القروض",
        descriptionAr = "حساب الأقساط الشهرية والفوائد والمرابحة",
        subgroup = FinanceToolSubgroup.FINANCIAL
    ),
    FinanceToolItem(
        calcKey = CalcKey.SAVINGS,
        titleAr = "التوفير والفوائد",
        descriptionAr = "محاكاة النمو المالي والادخار التراكمي المستقبلي",
        subgroup = FinanceToolSubgroup.FINANCIAL
    ),
    FinanceToolItem(
        calcKey = CalcKey.SALES_TAX,
        titleAr = "ضريبة المبيعات",
        descriptionAr = "حساب قيمة الضريبة المضافة VAT والإجمالي",
        subgroup = FinanceToolSubgroup.FINANCIAL
    ),
    FinanceToolItem(
        calcKey = CalcKey.TIP,
        titleAr = "حاسبة البقشيش",
        descriptionAr = "تقسيم الفواتير والإكراميات بين الأشخاص بالتساوي",
        subgroup = FinanceToolSubgroup.DISCOUNTS
    ),
    FinanceToolItem(
        calcKey = CalcKey.PERCENT,
        titleAr = "النسبة المئوية",
        descriptionAr = "حساب النسب والزيادة والنقصان المئوي بسهولة",
        subgroup = FinanceToolSubgroup.DISCOUNTS
    ),
    FinanceToolItem(
        calcKey = CalcKey.LEDGER,
        titleAr = "دفتر الحسابات والمالية",
        descriptionAr = "تسجيل الدخل والمصاريف ومتابعة الميزانية الشخصية",
        subgroup = FinanceToolSubgroup.FINANCIAL,
        badgeType = "جديد"
    ),
    FinanceToolItem(
        calcKey = CalcKey.UNIT_PRICE,
        titleAr = "سعر الوحدة",
        descriptionAr = "مقارنة أسعار المنتجات والكميات لاختيار الأفضل",
        subgroup = FinanceToolSubgroup.DISCOUNTS
    )
)

// Colors Palette: Royal Gold & Dark Glass Edition
private val RoyalGold = Color(0xFFD8B56A)
private val RoyalGoldGlow = Color(0xFFF3E5AB)
private val DarkObsidianStart = Color(0xFF080A0F)
private val DarkObsidianEnd = Color(0xFF12161F)
private val GlassSurfaceDark = Color(0xFF141923)
private val CardHighlightDark = Color(0xFF1E2538)
private val LiveRedAccent = Color(0xFFFF4D4D)
private val AIVioletAccent = Color(0xFFA855F7)
private val PinActiveGold = Color(0xFFFFB800)
private val MutedSlateText = Color(0xFF94A3B8)

// ==========================================
// 2. MAIN FINANCE HUB SCREEN COMPOSABLE
// ==========================================

@Composable
fun FinanceHubScreen(
    colors: CustomThemeColors,
    favoriteTools: Set<String>,
    onToggleFavorite: (CalcKey) -> Unit,
    onToolClick: (CalcKey) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 5 Core Screen States
    var isLoading by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isOffline by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    // Search and Filters
    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var selectedSubgroup by remember { mutableStateOf(FinanceToolSubgroup.ALL) }
    var isCompactGrid by remember { mutableStateOf(false) } // false = 2-column, true = 1-column detailed list

    // Check internet connectivity & simulate initial high-end loading
    fun checkAndLoad() {
        coroutineScope.launch {
            isLoading = true
            isError = false
            errorMessage = null

            // Network check
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNetwork = connectivityManager?.activeNetwork
            val capabilities = connectivityManager?.getNetworkCapabilities(activeNetwork)
            isOffline = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) != true

            delay(400) // Smooth entrance effect
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        checkAndLoad()
    }

    // Filter tools list based on subgroup, search query, and sort pinned items to top
    val processedTools by remember(searchQuery, selectedSubgroup, favoriteTools) {
        derivedStateOf {
            FINANCE_TOOLS_LIST.filter { item ->
                // Subgroup filter
                val matchesSubgroup = selectedSubgroup == FinanceToolSubgroup.ALL || item.subgroup == selectedSubgroup
                
                // Search query filter
                val matchesSearch = searchQuery.isBlank() ||
                        item.titleAr.contains(searchQuery, ignoreCase = true) ||
                        item.descriptionAr.contains(searchQuery, ignoreCase = true) ||
                        item.calcKey.keywords.any { it.contains(searchQuery, ignoreCase = true) }

                matchesSubgroup && matchesSearch
            }.sortedByDescending { favoriteTools.contains(it.calcKey.name) } // Pinned items rise to top!
        }
    }

    // Responsive Column Count (2 on mobile, 3-4 on tablet)
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
        // --- A. Procedural Obsidian Night Financial Grid Backdrop ---
        ProceduralFinancialGridCanvas()

        // --- B. Main Scrollable View Layout ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 1. Top Bar Navigation Header
            FinanceHubTopBar(
                colors = colors,
                title = "المال والأسعار والحاسبات",
                isSearchExpanded = isSearchExpanded,
                isCompactGrid = isCompactGrid,
                onBackClick = onBackClick,
                onToggleSearch = { isSearchExpanded = !isSearchExpanded },
                onToggleViewMode = { isCompactGrid = !isCompactGrid }
            )

            // 2. Expandable Glass Search Bar
            AnimatedVisibility(
                visible = isSearchExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                FinanceGlassSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onClearSearch = { searchQuery = "" },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            // 3. Non-Intrusive Offline Banner
            AnimatedVisibility(
                visible = isOffline,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                FinanceOfflineNoticeBanner(colors = colors)
            }

            // 4. Category Hero Banner
            FinanceHeroBanner(
                colors = colors,
                totalToolsCount = FINANCE_TOOLS_LIST.size,
                pinnedCount = favoriteTools.count { id -> FINANCE_TOOLS_LIST.any { it.calcKey.name == id } }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Section Header & Dynamic Counter Bar
            FinanceSectionHeaderCounter(
                colors = colors,
                title = "أدوات التصنيف المتاحة (${processedTools.size})",
                selectedSubgroup = selectedSubgroup,
                onSubgroupSelected = { selectedSubgroup = it }
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
                        FinanceSkeletonLoadingGrid(columnsCount = columnsCount)
                    }

                    // Error State: Retry Banner
                    isError -> {
                        FinanceErrorStateCard(
                            errorMessage = errorMessage ?: "حدث خطأ غير متوقع أثناء تحميل الأدوات المالية",
                            onRetry = { checkAndLoad() }
                        )
                    }

                    // Empty Search State
                    processedTools.isEmpty() -> {
                        FinanceEmptyStateCard(
                            searchQuery = searchQuery,
                            onClearSearch = {
                                searchQuery = ""
                                selectedSubgroup = FinanceToolSubgroup.ALL
                            }
                        )
                    }

                    // Success State: Interactive Grid
                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columnsCount),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp, top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(processedTools, key = { it.calcKey.id }) { toolItem ->
                                val isPinned = favoriteTools.contains(toolItem.calcKey.name)

                                FinanceGlassToolCard(
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
// 3. PROCEDURAL PROCEDURAL BACKDROP CANVAS
// ==========================================

@Composable
fun ProceduralFinancialGridCanvas() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Radial Golden Glow top-right
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(RoyalGold.copy(alpha = 0.08f), Color.Transparent),
                center = Offset(width * 0.85f, height * 0.15f),
                radius = width * 0.6f
            )
        )

        // Radial Violet Glow bottom-left
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(AIVioletAccent.copy(alpha = 0.05f), Color.Transparent),
                center = Offset(width * 0.15f, height * 0.85f),
                radius = width * 0.7f
            )
        )

        // Low-opacity vector grid line pattern
        val gridSize = 48.dp.toPx()
        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 8f), 0f)

        var x = 0f
        while (x < width) {
            drawLine(
                color = RoyalGold.copy(alpha = 0.03f),
                start = Offset(x, 0f),
                end = Offset(x, height),
                strokeWidth = 1f,
                pathEffect = pathEffect
            )
            x += gridSize
        }

        var y = 0f
        while (y < height) {
            drawLine(
                color = RoyalGold.copy(alpha = 0.03f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f,
                pathEffect = pathEffect
            )
            y += gridSize
        }
    }
}

// ==========================================
// 4. TOP BAR & SEARCH BAR COMPONENTS
// ==========================================

@Composable
fun FinanceHubTopBar(
    colors: CustomThemeColors,
    title: String,
    isSearchExpanded: Boolean,
    isCompactGrid: Boolean,
    onBackClick: () -> Unit,
    onToggleSearch: () -> Unit,
    onToggleViewMode: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Right side (RTL): Back navigation button with Royal Gold ring
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(GlassSurfaceDark.copy(alpha = 0.7f))
                    .border(1.dp, RoyalGold.copy(alpha = 0.3f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "رجوع",
                    tint = RoyalGold,
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
                    text = "المركز المالي والحاسبات الذكية",
                    fontSize = 10.sp,
                    color = MutedSlateText,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Left side action buttons
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // View mode toggle button
            IconButton(
                onClick = onToggleViewMode,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(GlassSurfaceDark.copy(alpha = 0.7f))
                    .border(1.dp, RoyalGold.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isCompactGrid) Icons.Default.GridView else Icons.Default.ViewList,
                    contentDescription = "تغيير العرض",
                    tint = RoyalGold,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Search toggle button
            IconButton(
                onClick = onToggleSearch,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isSearchExpanded) RoyalGold.copy(alpha = 0.2f) else GlassSurfaceDark.copy(alpha = 0.7f))
                    .border(1.dp, RoyalGold.copy(alpha = if (isSearchExpanded) 0.6f else 0.2f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = "بحث",
                    tint = RoyalGold,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun FinanceGlassSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = GlassSurfaceDark.copy(alpha = 0.85f),
        border = androidx.compose.foundation.BorderStroke(1.dp, RoyalGold.copy(alpha = 0.3f))
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
                tint = RoyalGold,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = {
                    Text(
                        "ابحث عن حاسبة، ذهب، قروض أو عملات...",
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
fun FinanceOfflineNoticeBanner(colors: CustomThemeColors) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF59E0B).copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.WifiOff,
                contentDescription = null,
                tint = Color(0xFFF59E0B),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "وضع عدم الاتصال: يمكنك استخدام الحاسبات والتحويلات المحلية بشكل طبيعي.",
                fontSize = 11.sp,
                color = Color(0xFFFDE68A),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ==========================================
// 5. CATEGORY HERO BANNER
// ==========================================

@Composable
fun FinanceHeroBanner(
    colors: CustomThemeColors,
    totalToolsCount: Int,
    pinnedCount: Int
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(24.dp),
        color = GlassSurfaceDark.copy(alpha = 0.8f),
        border = androidx.compose.foundation.BorderStroke(1.dp, RoyalGold.copy(alpha = 0.35f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            RoyalGold.copy(alpha = 0.12f),
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
                // Bank/Finance Icon with radial glow halo
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(RoyalGold.copy(alpha = 0.15f))
                        .border(1.dp, RoyalGold.copy(alpha = 0.4f), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = RoyalGold,
                        modifier = Modifier.size(30.dp)
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
                            text = "المنصة المالية والأسعار",
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
                        text = "الآلة الحاسبة، محول العملات والذهب، الأسعار الفورية، ومؤشرات الاقتصاد والخصومات والقروض",
                        fontSize = 11.sp,
                        color = MutedSlateText,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ==========================================
// 6. SECTION HEADER & SUBGROUP CHIPS BAR
// ==========================================

@Composable
fun FinanceSectionHeaderCounter(
    colors: CustomThemeColors,
    title: String,
    selectedSubgroup: FinanceToolSubgroup,
    onSubgroupSelected: (FinanceToolSubgroup) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Section Title with Vertical Royal Gold Accent Bar
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
                    .background(RoyalGold)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Horizontal Scrollable Filter Chips Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FinanceToolSubgroup.values().forEach { subgroup ->
                val isSelected = selectedSubgroup == subgroup

                Surface(
                    onClick = { onSubgroupSelected(subgroup) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) RoyalGold else GlassSurfaceDark.copy(alpha = 0.7f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) RoyalGold else RoyalGold.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = subgroup.icon,
                            contentDescription = null,
                            tint = if (isSelected) DarkObsidianStart else RoyalGold,
                            modifier = Modifier.size(14.dp)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = subgroup.titleAr,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isSelected) DarkObsidianStart else Color.White
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 7. INTERACTIVE TOOL CARD (GRID & LIST)
// ==========================================

@Composable
fun FinanceGlassToolCard(
    toolItem: FinanceToolItem,
    isPinned: Boolean,
    isCompactList: Boolean,
    onTogglePin: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Tactile Scale Press Effect
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "pressScale"
    )

    // Micro-badge infinite pulse animations
    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    
    // Live Red Badge Pulse
    val livePulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "livePulse"
    )

    // AI Violet Badge Shimmer
    val aiGlowScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aiGlow"
    )

    Surface(
        modifier = modifier
            .scale(scale)
            .fillMaxWidth()
            .then(if (isCompactList) Modifier.height(96.dp) else Modifier.height(138.dp))
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
            if (isPinned) PinActiveGold.copy(alpha = 0.6f) else RoyalGold.copy(alpha = 0.25f)
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

            // Dynamic Micro-Badges ("LIVE", "AI")
            if (toolItem.badgeType != null) {
                val badgeBg = when (toolItem.badgeType) {
                    "LIVE" -> LiveRedAccent
                    "AI" -> AIVioletAccent
                    else -> RoyalGold
                }

                Surface(
                    color = badgeBg.copy(alpha = if (toolItem.badgeType == "LIVE") livePulseAlpha else 1.0f),
                    shape = RoundedCornerShape(bottomStart = 10.dp, topEnd = 22.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .scale(if (toolItem.badgeType == "AI") aiGlowScale else 1.0f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (toolItem.badgeType == "LIVE") {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = toolItem.badgeType,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }

            // Card Inner Layout
            if (isCompactList) {
                // Horizontal 1-Column Row Layout
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
                            .background(RoyalGold.copy(alpha = 0.12f))
                            .border(1.dp, RoyalGold.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = AppIcons.forCalc(toolItem.calcKey),
                            contentDescription = null,
                            tint = RoyalGold,
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
                        tint = RoyalGold.copy(alpha = 0.6f),
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
                            .background(RoyalGold.copy(alpha = 0.12f))
                            .border(1.dp, RoyalGold.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = AppIcons.forCalc(toolItem.calcKey),
                            contentDescription = null,
                            tint = RoyalGold,
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
fun FinanceSkeletonLoadingGrid(columnsCount: Int) {
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
        items(8) {
            Surface(
                modifier = Modifier
                    .height(138.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = GlassSurfaceDark.copy(alpha = shimmerAlpha),
                border = androidx.compose.foundation.BorderStroke(1.dp, RoyalGold.copy(alpha = 0.1f))
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
fun FinanceEmptyStateCard(
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
            border = androidx.compose.foundation.BorderStroke(1.dp, RoyalGold.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.SearchOff,
                    contentDescription = null,
                    tint = RoyalGold,
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
                    text = "تأكد من كتابة اسم الحاسبة أو جرب العودة لجميع الأدوات المالية",
                    fontSize = 11.sp,
                    color = MutedSlateText,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onClearSearch,
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalGold),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("عرض جميع الأدوات", color = DarkObsidianStart, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun FinanceErrorStateCard(
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
            border = androidx.compose.foundation.BorderStroke(1.dp, LiveRedAccent.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = LiveRedAccent,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "تعذر تحميل أدوات المركز المالي",
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
                    colors = ButtonDefaults.buttonColors(containerColor = LiveRedAccent),
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
