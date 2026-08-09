package com.example.ui.screens

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    FINANCIAL("financial", "حاسبات مالية", Icons.Default.AccountBalance),
    LIVE_MARKET("live", "أسعار مباشرة", Icons.Default.ShowChart),
    CURRENCY("currency", "عملات", Icons.Default.CurrencyExchange)
}

data class FinanceToolItem(
    val calcKey: CalcKey,
    val titleAr: String,
    val descriptionAr: String,
    val subgroup: FinanceToolSubgroup,
    val badgeType: String? = null // "LIVE", "AI", "جديد"
)

// The 14 Financial Tools Definition
val FINANCE_TOOLS_LIST = listOf(
    FinanceToolItem(
        calcKey = CalcKey.GOLD,
        titleAr = "حاسبة الذهب",
        descriptionAr = "حساب العيارات والمصنعية للبيع والشراء الفوري",
        subgroup = FinanceToolSubgroup.LIVE_MARKET,
        badgeType = "LIVE"
    ),
    FinanceToolItem(
        calcKey = CalcKey.CURRENCY,
        titleAr = "تحويل العملات",
        descriptionAr = "تحويل العملات العربية والعالمية بأسعار حية",
        subgroup = FinanceToolSubgroup.CURRENCY,
        badgeType = "LIVE"
    ),
    FinanceToolItem(
        calcKey = CalcKey.LOAN,
        titleAr = "حاسبة القروض",
        descriptionAr = "حساب أقساط القروض والفوائد البنكية والمرابحة",
        subgroup = FinanceToolSubgroup.FINANCIAL,
        badgeType = "AI"
    ),
    FinanceToolItem(
        calcKey = CalcKey.SALES_TAX,
        titleAr = "حاسبة الضرائب",
        descriptionAr = "تقدير ضريبة المبيعات وضريبة القيمة المضافة VAT",
        subgroup = FinanceToolSubgroup.FINANCIAL
    ),
    FinanceToolItem(
        calcKey = CalcKey.LIVE_PRICES,
        titleAr = "الأسعار الحية",
        descriptionAr = "تتبع أسعار الذهب والفضة والعملات والنفط لحظة بلحظة",
        subgroup = FinanceToolSubgroup.LIVE_MARKET,
        badgeType = "LIVE"
    ),
    FinanceToolItem(
        calcKey = CalcKey.ECONOMIC_INDICATORS,
        titleAr = "مؤشرات الاقتصاد",
        descriptionAr = "متابعة التضخم والفائدة والنمو بتحليل الخبير الذكي",
        subgroup = FinanceToolSubgroup.LIVE_MARKET,
        badgeType = "AI"
    ),
    FinanceToolItem(
        calcKey = CalcKey.LEDGER,
        titleAr = "دفتر المصروفات",
        descriptionAr = "تسجيل الإيرادات والواردات ومتابعة ميزانيتك الشهرية",
        subgroup = FinanceToolSubgroup.FINANCIAL,
        badgeType = "جديد"
    ),
    FinanceToolItem(
        calcKey = CalcKey.SAVINGS,
        titleAr = "حاسبة الادخار",
        descriptionAr = "تخطيط التوفير المستقبلي ونمو حسابات الودائع",
        subgroup = FinanceToolSubgroup.FINANCIAL
    ),
    FinanceToolItem(
        calcKey = CalcKey.BASIC,
        titleAr = "الآلة الحاسبة",
        descriptionAr = "حسابات عامة وعلمية بدقة فائقة وسرعة عالية",
        subgroup = FinanceToolSubgroup.FINANCIAL
    ),
    FinanceToolItem(
        calcKey = CalcKey.DISCOUNT,
        titleAr = "الخصم والتخفيض",
        descriptionAr = "معرفة السعر النهائي وقيمة التوفير أثناء العروض والخصومات",
        subgroup = FinanceToolSubgroup.FINANCIAL
    ),
    FinanceToolItem(
        calcKey = CalcKey.UNIT,
        titleAr = "محول الوحدات",
        descriptionAr = "تحويل أطوال، أوزان، مساحات وحجوم بدقة متناهية",
        subgroup = FinanceToolSubgroup.FINANCIAL
    ),
    FinanceToolItem(
        calcKey = CalcKey.TIP,
        titleAr = "حاسبة البقشيش",
        descriptionAr = "تقسيم الفاتورة والإكراميات بين عدة أشخاص بالتساوي",
        subgroup = FinanceToolSubgroup.FINANCIAL
    ),
    FinanceToolItem(
        calcKey = CalcKey.PERCENT,
        titleAr = "النسبة المئوية",
        descriptionAr = "حساب النسب المئوية والزيادة والنقصان بسهولة",
        subgroup = FinanceToolSubgroup.FINANCIAL
    ),
    FinanceToolItem(
        calcKey = CalcKey.UNIT_PRICE,
        titleAr = "سعر الوحدة",
        descriptionAr = "مقارنة أسعار المنتجات والكميات لاختيار الأوفر دائماً",
        subgroup = FinanceToolSubgroup.FINANCIAL
    )
)

// Brand Premium Colors
private val ColorBg = Color(0xFF121212)
private val ColorCard = Color(0xFF1E1E1E)
private val ColorGold = Color(0xFFD8B56A)
private val ColorWarmGold = Color(0xFFC99A45)
private val ColorNightBlue = Color(0xFF102A43)
private val ColorTeal = Color(0xFF0E6F73)
private val ColorTurquoise = Color(0xFF19A7A8)
private val ColorTextPrimary = Color(0xFFFFFFFF)
private val ColorTextSecondary = Color(0xFFA0A0A0)
private val ColorBorder = Color(0xFFFFFFFF).copy(alpha = 0.08f)

private val ColorPurpleAI = Color(0xFF8B5CF6)
private val ColorRedLive = Color(0xFFEF4444)

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

    // Screen States
    var isLoading by remember { mutableStateOf(true) }
    var isOffline by remember { mutableStateOf(false) }

    // Search and Filters
    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var selectedSubgroup by remember { mutableStateOf(FinanceToolSubgroup.ALL) }
    var showAllTools by remember { mutableStateOf(false) }

    // Simulation of high-end loading
    LaunchedEffect(Unit) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = connectivityManager?.activeNetwork
        val capabilities = connectivityManager?.getNetworkCapabilities(activeNetwork)
        isOffline = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) != true

        delay(400)
        isLoading = false
    }

    // Filter tools based on search and subgroup
    val filteredTools by remember(searchQuery, selectedSubgroup) {
        derivedStateOf {
            FINANCE_TOOLS_LIST.filter { item ->
                val matchesSubgroup = selectedSubgroup == FinanceToolSubgroup.ALL || item.subgroup == selectedSubgroup
                val matchesSearch = searchQuery.isBlank() ||
                        item.titleAr.contains(searchQuery, ignoreCase = true) ||
                        item.descriptionAr.contains(searchQuery, ignoreCase = true) ||
                        item.calcKey.keywords.any { it.contains(searchQuery, ignoreCase = true) }
                matchesSubgroup && matchesSearch
            }
        }
    }

    // Limit displayed tools based on selected filter and "showAllTools" status
    val displayedTools = remember(filteredTools, selectedSubgroup, showAllTools) {
        if (selectedSubgroup != FinanceToolSubgroup.ALL || showAllTools) {
            filteredTools
        } else {
            filteredTools.take(8)
        }
    }

    // Responsive column counts
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val columnsCount = remember(screenWidthDp) {
        if (screenWidthDp >= 1100) 4
        else if (screenWidthDp >= 700) 3
        else 2
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ColorBg)
    ) {
        // Subtle procedural vector-grid lines
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 56.dp.toPx()
            val gridPaint = Color.White.copy(alpha = 0.02f)
            for (x in 0..size.width.toInt() step step.toInt()) {
                drawLine(gridPaint, Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height), strokeWidth = 1f)
            }
            for (y in 0..size.height.toInt() step step.toInt()) {
                drawLine(gridPaint, Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), strokeWidth = 1f)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 1. TOP APP BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Back arrow button (44x44dp hit area)
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.06f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "رجوع",
                        tint = ColorTextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Center screen title
                Text(
                    text = "الخدمات المالية",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ColorTextPrimary,
                    textAlign = TextAlign.Center
                )

                // Search & Menu buttons in 44x44 round container
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { isSearchExpanded = !isSearchExpanded },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.06f))
                    ) {
                        Icon(
                            imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "بحث",
                            tint = ColorTextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = { /* menu action or drawer */ },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.06f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "القائمة",
                            tint = ColorTextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Expandable search bar view
            AnimatedVisibility(
                visible = isSearchExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = ColorCard,
                    border = BorderStroke(1.dp, ColorBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, null, tint = ColorGold, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("ابحث عن حاسبة، ذهب، قروض أو عملات...", fontSize = 12.sp, color = ColorTextSecondary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = ColorTextPrimary,
                                unfocusedTextColor = ColorTextPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 2. HERO BANNER
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .height(180.dp),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, ColorBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(ColorGold, ColorTeal),
                                start = Offset(0f, 0f),
                                end = Offset.Infinite
                            )
                        )
                ) {
                    // Soft transparent dark layer to guarantee text readability
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.25f))
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(22.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left Column (RTL: text on right, icon on left)
                        Column(
                            modifier = Modifier.weight(1.3f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "منصتك المالية الذكية",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ColorTextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "كل أدواتك المالية، تحويل العملات، الذهب، والحسابات في مكان واحد.",
                                fontSize = 13.sp,
                                color = ColorTextPrimary.copy(alpha = 0.85f),
                                lineHeight = 19.sp
                            )
                        }

                        // Right large visual icon (RTL: left side)
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = ColorTextPrimary.copy(alpha = 0.15f),
                            modifier = Modifier
                                .size(72.dp)
                                .weight(0.7f)
                        )
                    }
                }
            }

            // 3. CATEGORY CHIPS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FinanceToolSubgroup.values().forEach { subgroup ->
                    val isSelected = selectedSubgroup == subgroup
                    Surface(
                        onClick = { selectedSubgroup = subgroup },
                        shape = RoundedCornerShape(50.dp),
                        color = if (isSelected) ColorGold else ColorCard,
                        border = BorderStroke(1.dp, if (isSelected) ColorGold else ColorBorder),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = subgroup.titleAr,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) ColorBg else ColorTextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4. TOOLS GRID (Max 8 in home state, or expanded)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = ColorGold
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columnsCount),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(displayedTools, key = { it.calcKey.id }) { tool ->
                                PremiumToolCardItem(
                                    item = tool,
                                    onClick = { onToolClick(tool.calcKey) }
                                )
                            }

                            // View All tools item/button inside grid or as footer
                            if (selectedSubgroup == FinanceToolSubgroup.ALL && !showAllTools && filteredTools.size > 8) {
                                item {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(138.dp)
                                            .clip(RoundedCornerShape(18.dp))
                                            .clickable { showAllTools = true },
                                        shape = RoundedCornerShape(18.dp),
                                        color = ColorCard,
                                        border = BorderStroke(1.dp, ColorGold.copy(alpha = 0.4f))
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.GridView,
                                                contentDescription = null,
                                                tint = ColorGold,
                                                modifier = Modifier.size(32.dp)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "عرض جميع الأدوات (14) ➔",
                                                color = ColorGold,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. PREMIUM TOOL CARD COMPOSABLE
// ==========================================

@Composable
fun PremiumToolCardItem(
    item: FinanceToolItem,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "scale"
    )

    Surface(
        modifier = Modifier
            .scale(scale)
            .fillMaxWidth()
            .height(138.dp)
            .clip(RoundedCornerShape(18.dp))
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
        shape = RoundedCornerShape(18.dp),
        color = ColorCard,
        border = BorderStroke(1.dp, ColorBorder)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // State Badge top-start
            if (item.badgeType != null) {
                val badgeColor = when (item.badgeType) {
                    "AI" -> ColorPurpleAI
                    "LIVE" -> ColorRedLive
                    else -> ColorGold
                }
                Surface(
                    color = badgeColor,
                    shape = RoundedCornerShape(bottomStart = 8.dp, topEnd = 18.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = item.badgeType,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorTextPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Line Style central icon with no container
                Icon(
                    imageVector = AppIcons.forCalc(item.calcKey),
                    contentDescription = null,
                    tint = ColorGold,
                    modifier = Modifier.size(42.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = item.titleAr,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = item.descriptionAr,
                    fontSize = 11.sp,
                    color = ColorTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
