package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(),
    onNavigateToTool: (String) -> Unit = {},
    onNavigateToAssistant: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070A0F))
    ) {
        val maxAvailableHeight = maxHeight
        val heroHeight = (maxAvailableHeight * 0.70f).coerceIn(240.dp, 360.dp)

        when (val state = uiState) {
            is DashboardUiState.Loading -> DashboardShimmerSkeleton(heroHeight)
            is DashboardUiState.Success -> DashboardContent(
                state = state,
                heroHeight = heroHeight,
                onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                onNavigateToTool = onNavigateToTool,
                onNavigateToSettings = onNavigateToSettings
            )
            is DashboardUiState.Error -> MainDashboardErrorState(
                message = state.message,
                onRetry = { viewModel.retryFetchData() }
            )
            is DashboardUiState.Empty -> DashboardEmptyState()
        }
    }
}

@Composable
private fun DashboardContent(
    state: DashboardUiState.Success,
    heroHeight: Dp,
    onSearchQueryChanged: (String) -> Unit,
    onNavigateToTool: (String) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // A. Top Bar Header
        item {
            GlassmorphicTopHeader(
                dateHijri = state.dateHijri,
                dateMiladi = state.dateMiladi,
                hasNotificationBadge = state.hasNotificationBadge,
                onSettingsClick = onNavigateToSettings,
                onNotificationClick = { }
            )
        }

        // Offline Banner if active
        if (state.isOffline) {
            item { OfflineStatusBanner() }
        }

        // B. Hero Mosque Card
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                HeroMosqueCanvasCard(
                    dateMiladi = state.dateMiladi,
                    dateHijri = state.dateHijri,
                    prayerName = state.nextPrayerName,
                    prayerTime12h = state.nextPrayerTime12h,
                    cityName = state.nextPrayerCity,
                    remainingMinutes = state.remainingMinutesToPrayer,
                    totalIntervalMinutes = state.totalPrayerIntervalMinutes,
                    height = heroHeight
                )
            }
        }

        // C. Smart Search Bar with Side Pill Button
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                SmartFlexibleSearchBar(
                    query = state.searchQuery,
                    onQueryChange = onSearchQueryChanged,
                    onSearchSubmit = { query ->
                        val normalized = query.normalizeArabic()
                        onNavigateToTool("search_$normalized")
                    },
                    onSidePillClick = { onNavigateToTool("LOAN") }
                )
            }
        }

        // D. Feature Grid (Main 5 Tools)
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                FeatureToolsGrid(
                    tools = state.featureTools,
                    onToolClick = { toolId ->
                        when (toolId) {
                            "TIME_DATE" -> onNavigateToTool("PRAYER")
                            "CURRENCY_TOOLS" -> onNavigateToTool("CURRENCY")
                            "FINANCE_TOOLS" -> onNavigateToTool("ZAKAT")
                            "HEALTH_FITNESS" -> onNavigateToTool("HEALTH")
                            "AI_ASSISTANT" -> onNavigateToTool("AI")
                            else -> onNavigateToTool(toolId)
                        }
                    }
                )
            }
        }

        // E. Recent Activity Carousel
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                RecentToolsRow(
                    tools = state.recentTools,
                    onToolClick = { toolId -> onNavigateToTool(toolId) }
                )
            }
        }

        item { ProFooterBadge() }
    }
}

// A. Glassmorphic Top Header
@Composable
fun GlassmorphicTopHeader(
    dateHijri: String,
    dateMiladi: String,
    hasNotificationBadge: Boolean,
    onSettingsClick: () -> Unit,
    onNotificationClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = Color(0xFF141926).copy(alpha = 0.85f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Right side (RTL): Settings gear
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF1E293B), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "الإعدادات",
                    tint = Color(0xFF94A3B8)
                )
            }

            // Center: Dual Dates
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = dateHijri,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color(0xFFF5B041),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                )
                Text(
                    text = dateMiladi,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                )
            }

            // Left side (RTL): Notification bell with red badge
            Box {
                IconButton(
                    onClick = onNotificationClick,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF1E293B), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "التنبيهات",
                        tint = Color(0xFF00D4CC)
                    )
                }
                if (hasNotificationBadge) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(0xFFEF4444), CircleShape)
                            .align(Alignment.TopEnd)
                    )
                }
            }
        }
    }
}

// C. Smart Search Bar with Gradient Side Pill Button
@Composable
fun SmartFlexibleSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchSubmit: (String) -> Unit,
    onSidePillClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .height(54.dp),
            placeholder = {
                Text(
                    "ابحث عن أي أداة (حاسبة، زكاة، طقس...)",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF64748B), fontSize = 12.sp)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "بحث",
                    tint = Color(0xFF00D4CC)
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF141926),
                unfocusedContainerColor = Color(0xFF141926),
                focusedBorderColor = Color(0xFF00D4CC),
                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearchSubmit(query) })
        )

        // Side Pill Button: "حاسبة الودائع"
        Surface(
            modifier = Modifier
                .height(54.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable { onSidePillClick() },
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFFF5B041), Color(0xFFF97316))
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "حاسبة الودائع",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}

// D. Feature Grid (Main 5 Tools)
@Composable
fun FeatureToolsGrid(
    tools: List<FeatureToolModel>,
    onToolClick: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Grid 2 columns for first 4 tools
        val regularTools = tools.filter { !it.isFullWidth }
        val fullWidthTools = tools.filter { it.isFullWidth }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            regularTools.chunked(2).forEach { rowTools ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowTools.forEach { tool ->
                        Box(modifier = Modifier.weight(1f)) {
                            FeatureToolCard(tool = tool, onClick = { onToolClick(tool.id) })
                        }
                    }
                    if (rowTools.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            fullWidthTools.forEach { tool ->
                FeatureToolCard(tool = tool, onClick = { onToolClick(tool.id) })
            }
        }
    }
}

@Composable
fun FeatureToolCard(
    tool: FeatureToolModel,
    onClick: () -> Unit
) {
    val icon: ImageVector = when (tool.id) {
        "TIME_DATE" -> Icons.Default.Schedule
        "CURRENCY_TOOLS" -> Icons.Default.MonetizationOn
        "FINANCE_TOOLS" -> Icons.Default.AccountBalance
        "HEALTH_FITNESS" -> Icons.Default.Favorite
        "AI_ASSISTANT" -> Icons.Default.AutoAwesome
        else -> Icons.Default.Calculate
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .border(
                1.dp,
                Color(tool.accentColorHex).copy(alpha = 0.35f),
                RoundedCornerShape(20.dp)
            ),
        color = Color(0xFF141926),
        shape = RoundedCornerShape(20.dp)
    ) {
        if (tool.isFullWidth) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFF5B041).copy(alpha = 0.3f), Color(0xFF8B5CF6).copy(alpha = 0.3f))
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = tool.title,
                            tint = Color(0xFFF5B041),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = tool.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        )
                        tool.subtitle?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }

                Surface(
                    color = Color(0xFFF5B041).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = tool.badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFFF5B041),
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(tool.accentColorHex).copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = tool.title,
                            tint = Color(tool.accentColorHex),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (tool.isSquareBmiBadge) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFF10B981), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tool.badgeText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    } else {
                        Surface(
                            color = Color(tool.accentColorHex).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = tool.badgeText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(tool.accentColorHex),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Text(
                    text = tool.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                )
            }
        }
    }
}

// E. Recent Tools Row
@Composable
fun RecentToolsRow(tools: List<RecentToolDomainModel>, onToolClick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "الأدوات الأخيرة والأنشطة",
            style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp),
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(tools) { tool ->
                Surface(
                    onClick = { onToolClick(tool.id) },
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF141926),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = Color(0xFF00D4CC),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            tool.title,
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontSize = 12.sp)
                        )
                    }
                }
            }
        }
    }
}

// F. Premium Floating Bottom Bar (RTL Order: [Home, Calculator, Currency, Health, AI])
@Composable
fun DashboardBottomBar(
    currentRoute: String,
    onNavigateToTool: (String) -> Unit = {},
    onNavigateToAssistant: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    NavigationBar(
        containerColor = Color(0xFF141926).copy(alpha = 0.95f),
        contentColor = Color.White,
        tonalElevation = 12.dp,
        modifier = Modifier
            .shadow(16.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(Color(0xFFF5B041).copy(alpha = 0.4f), Color.Transparent)
                ),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            )
    ) {
        // In Compose with RTL system/layout direction, items are laid out right-to-left:
        // Item 1 (Rightmost): Home (الرئيسية)
        NavigationBarItem(
            selected = currentRoute == "home",
            onClick = { },
            icon = { Icon(Icons.Filled.Home, contentDescription = "الرئيسية") },
            label = { Text("الرئيسية") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFFF5B041),
                selectedTextColor = Color(0xFFF5B041),
                unselectedIconColor = Color(0xFF64748B),
                unselectedTextColor = Color(0xFF64748B),
                indicatorColor = Color(0xFFF5B041).copy(alpha = 0.15f)
            )
        )
        // Item 2: Calculator (الحاسبة)
        NavigationBarItem(
            selected = false,
            onClick = { onNavigateToTool("BASIC") },
            icon = { Icon(Icons.Outlined.Calculate, contentDescription = "الحاسبة") },
            label = { Text("الحاسبة") },
            colors = NavigationBarItemDefaults.colors(
                unselectedIconColor = Color(0xFF64748B),
                unselectedTextColor = Color(0xFF64748B)
            )
        )
        // Item 3: Currency (العملات)
        NavigationBarItem(
            selected = false,
            onClick = { onNavigateToTool("CURRENCY") },
            icon = { Icon(Icons.Outlined.MonetizationOn, contentDescription = "العملات") },
            label = { Text("العملات") },
            colors = NavigationBarItemDefaults.colors(
                unselectedIconColor = Color(0xFF64748B),
                unselectedTextColor = Color(0xFF64748B)
            )
        )
        // Item 4: Health (الصحة - with green BMI box)
        NavigationBarItem(
            selected = false,
            onClick = { onNavigateToTool("HEALTH") },
            icon = {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color(0xFF10B981), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "BMI",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            },
            label = { Text("الصحة") },
            colors = NavigationBarItemDefaults.colors(
                unselectedIconColor = Color(0xFF64748B),
                unselectedTextColor = Color(0xFF64748B)
            )
        )
        // Item 5 (Leftmost in RTL): AI Assistant (المساعد)
        NavigationBarItem(
            selected = false,
            onClick = onNavigateToAssistant,
            icon = { Icon(Icons.Outlined.SmartToy, contentDescription = "المساعد الذكي") },
            label = { Text("المساعد") },
            colors = NavigationBarItemDefaults.colors(
                unselectedIconColor = Color(0xFF64748B),
                unselectedTextColor = Color(0xFF64748B)
            )
        )
    }
}

// 1. Shimmer Skeleton (Loading State)
@Composable
fun DashboardShimmerSkeleton(heroHeight: Dp) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(Color(0xFF141926), RoundedCornerShape(16.dp))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(heroHeight)
                .background(Color(0xFF141926), RoundedCornerShape(28.dp))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .background(Color(0xFF141926), RoundedCornerShape(16.dp))
        )
    }
}

// 2. Error State
@Composable
private fun MainDashboardErrorState(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "خطأ",
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge.copy(color = Color.White)
            )
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF5B041))
            ) {
                Text("إعادة المحاولة", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// 3. Empty State
@Composable
fun DashboardEmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("لا توجد بيانات للعرض", style = MaterialTheme.typography.bodyLarge.copy(color = Color.Gray))
    }
}

// 4. Offline Banner
@Composable
fun OfflineStatusBanner() {
    Surface(
        color = Color(0xFFF59E0B).copy(alpha = 0.15f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "معلومات: أنت تعمل دون اتصال بالإنترنت. يتم عرض البيانات المخزنة محليًا.",
            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFF59E0B)),
            modifier = Modifier.padding(12.dp)
        )
    }
}

// B. Hero Mosque Canvas Card (The Masterpiece)
@Composable
fun HeroMosqueCanvasCard(
    dateMiladi: String,
    dateHijri: String,
    prayerName: String,
    prayerTime12h: String,
    cityName: String,
    remainingMinutes: Int,
    totalIntervalMinutes: Int,
    height: Dp
) {
    val progressFraction = (remainingMinutes.toFloat() / totalIntervalMinutes.toFloat()).coerceIn(0f, 1f)

    // Breathing Neon Glow
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    // 1. Calculate static star positions only once via remember
    val starPositions = remember {
        List(35) {
            Pair((0..100).random() / 100f, (0..60).random() / 100f)
        }
    }

    // 2. Draw Mosque Silhouette Path once using normalized percentage coordinates (0.0f to 1.0f)
    val mosquePath = remember {
        Path().apply {
            moveTo(0.05f, 1f)
            lineTo(0.05f, 0.35f); lineTo(0.08f, 0.35f)
            lineTo(0.08f, 0.20f); lineTo(0.10f, 0.15f) // Crescent Tip
            lineTo(0.12f, 0.20f); lineTo(0.12f, 0.35f); lineTo(0.15f, 0.35f)

            cubicTo(0.18f, 0.60f, 0.22f, 0.60f, 0.25f, 1f) // Right Arch
            cubicTo(0.28f, 0.50f, 0.32f, 0.50f, 0.35f, 1f) // Small Dome
            cubicTo(0.40f, 0.20f, 0.60f, 0.20f, 0.65f, 1f) // Central Main Grand Dome

            cubicTo(0.68f, 0.50f, 0.72f, 0.50f, 0.75f, 1f) // Left Arch
            lineTo(0.85f, 0.35f); lineTo(0.88f, 0.35f)
            lineTo(0.88f, 0.20f); lineTo(0.90f, 0.15f)
            lineTo(0.92f, 0.20f); lineTo(0.92f, 0.35f); lineTo(0.95f, 0.35f)
            lineTo(0.95f, 1f)
            close()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF00D4CC).copy(alpha = 0.5f * glowAlpha),
                        Color(0xFF1E293B)
                    )
                ),
                RoundedCornerShape(28.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            
            // Canvas: Zero-Allocation High Performance Drawing
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Ambient Glow Background
                drawRect(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFF00D4CC).copy(alpha = 0.15f * glowAlpha), Color.Transparent),
                        center = Offset(w * 0.8f, h * 0.4f),
                        radius = w * 0.75f
                    )
                )

                // Static particles (Stars)
                starPositions.forEach { (percentX, percentY) ->
                    drawCircle(
                        color = Color.White.copy(alpha = 0.35f),
                        radius = 1.5f,
                        center = Offset(w * percentX, h * percentY)
                    )
                }

                // Scale path dynamically without creating new objects
                scale(scaleX = w, scaleY = h, pivot = Offset.Zero) {
                    drawPath(
                        path = mosquePath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFF5B041).copy(alpha = 0.4f),
                                Color(0xFF0F172A).copy(alpha = 0.9f)
                            ),
                            startY = 0f,
                            endY = 1f
                        )
                    )
                    drawPath(
                        path = mosquePath,
                        color = Color(0xFF00D4CC).copy(alpha = 0.35f * glowAlpha),
                        style = Stroke(width = 2.dp.toPx() / w)
                    )
                }
            }

            // Overlay Layout Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(22.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top: City name badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(0xFF1E293B).copy(alpha = 0.85f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFF00D4CC).copy(alpha = 0.4f))
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFF00D4CC),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                cityName,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = Color(0xFF00D4CC),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }

                // Middle: Prayer Info
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "الصلاة القادمة",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = Color(0xFF00D4CC),
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            prayerName,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 26.sp
                            )
                        )
                        Text(
                            prayerTime12h,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                color = Color(0xFFF5B041),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 26.sp
                            )
                        )
                    }
                }

                // Bottom: Phosphor Progress Bar
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "متبقي على الأذان",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        )
                        Text(
                            "$remainingMinutes دقيقة",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFF00D4CC),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        )
                    }
                    PhosphorProgressBar(progress = progressFraction, glowAlpha = glowAlpha)
                }
            }
        }
    }
}

@Composable
fun PhosphorProgressBar(progress: Float, glowAlpha: Float) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
    ) {
        val w = size.width
        drawLine(
            color = Color.White.copy(alpha = 0.15f),
            start = Offset(0f, 4.dp.toPx()),
            end = Offset(w, 4.dp.toPx()),
            strokeWidth = 6.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            brush = Brush.horizontalGradient(
                listOf(Color(0xFF00D4CC).copy(alpha = 0.4f * glowAlpha), Color(0xFF00D4CC))
            ),
            start = Offset(0f, 4.dp.toPx()),
            end = Offset(w * progress, 4.dp.toPx()),
            strokeWidth = 6.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFF00D4CC).copy(alpha = 0.25f * glowAlpha),
            start = Offset(0f, 4.dp.toPx()),
            end = Offset(w * progress, 4.dp.toPx()),
            strokeWidth = 18.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun ProFooterBadge() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "ClevCalc Pro • النسخة الاحترافية © 2026",
            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B), fontSize = 11.sp)
        )
    }
}
