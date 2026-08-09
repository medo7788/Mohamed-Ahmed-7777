package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
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

// ==========================================
// 1. BRAND PREMIUM STYLING COLOR PATHS
// ==========================================
private val ColorBg = Color(0xFF121212)
private val ColorCard = Color(0xFF1E1E1E)
private val ColorGold = Color(0xFFD8B56A)
private val ColorWarmGold = Color(0xFFC29C57)
private val ColorNightBlue = Color(0xFF0C1E33)
private val ColorTurquoise = Color(0xFF1FD0C5)
private val ColorPrimaryText = Color(0xFFFFFFFF)
private val ColorSecondaryText = Color(0xFFA0A0A0)
private val ColorBorder = Color.White.copy(alpha = 0.08f)

data class PracticalToolItem(
    val titleAr: String,
    val descriptionAr: String,
    val calcKey: CalcKey,
    val isProductivity: Boolean = false
)

// The Exact 7 Tools List as requested
val PRACTICAL_TOOLS_LIST = listOf(
    // 1. حاسبات يومية
    PracticalToolItem(
        titleAr = "تفقيط الأرقام",
        descriptionAr = "حوّل الأرقام والعملات إلى كلمات عربية فصحى بدقة",
        calcKey = CalcKey.NUM_WORDS
    ),
    PracticalToolItem(
        titleAr = "حاسبة الوقود",
        descriptionAr = "احسب استهلاك البنزين وتكلفة الرحلة التقديرية لسيارتك",
        calcKey = CalcKey.FUEL_COST
    ),
    PracticalToolItem(
        titleAr = "حاسبة الدهان",
        descriptionAr = "احسب مساحة الجدران وكمية الطلاء والعلب المطلوبة بدقة",
        calcKey = CalcKey.PAINT
    ),
    PracticalToolItem(
        titleAr = "تحويل المقاسات",
        descriptionAr = "حوّل بين وحدات ومقاسات القياس المختلفة بسهولة تامة",
        calcKey = CalcKey.UNIT
    ),
    // 2. مساحة إنتاجية
    PracticalToolItem(
        titleAr = "دفتر المصروفات",
        descriptionAr = "تابع إيراداتك ومصروفاتك اليومية واعرف أين تذهب أموالك",
        calcKey = CalcKey.LEDGER,
        isProductivity = true
    ),
    PracticalToolItem(
        titleAr = "المفكرة",
        descriptionAr = "دوّن أفكارك وملاحظاتك ومهامك اليومية بسرعة وأمان",
        calcKey = CalcKey.NOTES,
        isProductivity = true
    ),
    PracticalToolItem(
        titleAr = "المتصفح",
        descriptionAr = "تصفح أهم المواقع الإخبارية والويب مباشرة من داخل التطبيق",
        calcKey = CalcKey.NEWS_BROWSER,
        isProductivity = true
    )
)

// ==========================================
// 2. MAIN HUBSCREEN COMPOSABLE
// ==========================================
@Composable
fun PracticalToolsHubScreen(
    colors: CustomThemeColors,
    favoriteTools: Set<String>,
    onToggleFavorite: (CalcKey) -> Unit,
    onToolClick: (CalcKey) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }

    // Filter tools list based on search query
    val filteredTools = remember(searchQuery) {
        PRACTICAL_TOOLS_LIST.filter { tool ->
            searchQuery.isBlank() ||
                    tool.titleAr.contains(searchQuery, ignoreCase = true) ||
                    tool.descriptionAr.contains(searchQuery, ignoreCase = true)
        }
    }

    // Split tools into two sections
    val lifeCalculators = filteredTools.filter { !it.isProductivity }
    val productivityHub = filteredTools.filter { it.isProductivity }

    // Responsive columns (2 on phone, 4 on tablet)
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val columnsCount = if (screenWidthDp >= 600) 4 else 2

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ColorBg)
    ) {
        // Grid pattern canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 56.dp.toPx()
            val gridPaint = Color.White.copy(alpha = 0.015f)
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
                        tint = ColorPrimaryText,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = "أدوات عملية",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.W800,
                    color = ColorPrimaryText,
                    textAlign = TextAlign.Center
                )

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
                            tint = ColorPrimaryText,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Expandable Search view
            AnimatedVisibility(
                visible = isSearchExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
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
                            placeholder = { Text("ابحث عن أي أداة عملية...", fontSize = 12.sp, color = ColorSecondaryText) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = ColorPrimaryText,
                                unfocusedTextColor = ColorPrimaryText
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Scrollable Category Columns
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 90.dp)
                ) {
                    // --- SECTION 1: حاسبات يومية ---
                    if (lifeCalculators.isNotEmpty()) {
                        SectionHeaderSeparator("حاسبات يومية")

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columnsCount),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp) // bounded height for grid layout scroll nesting
                        ) {
                            items(lifeCalculators, key = { it.titleAr }) { tool ->
                                PracticalToolCardItem(
                                    item = tool,
                                    isPinned = favoriteTools.contains(tool.calcKey.name),
                                    onTogglePin = { onToggleFavorite(tool.calcKey) },
                                    onClick = { onToolClick(tool.calcKey) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- SECTION 2: مساحة إنتاجية ---
                    if (productivityHub.isNotEmpty()) {
                        SectionHeaderSeparator("مساحة إنتاجية")

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columnsCount),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp)
                        ) {
                            items(productivityHub, key = { it.titleAr }) { tool ->
                                PracticalToolCardItem(
                                    item = tool,
                                    isPinned = favoriteTools.contains(tool.calcKey.name),
                                    onTogglePin = { onToggleFavorite(tool.calcKey) },
                                    onClick = { onToolClick(tool.calcKey) }
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
// 3. CORE SUBCOMPONENTS (HEADERS & SEPARATORS)
// ==========================================

@Composable
fun SectionHeaderSeparator(title: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = ColorGold
        )
        Spacer(modifier = Modifier.height(4.dp))
        // Simple linear golden light bar
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(2.dp)
                .background(ColorGold)
        )
    }
}

@Composable
fun PracticalToolCardItem(
    item: PracticalToolItem,
    isPinned: Boolean,
    onTogglePin: () -> Unit,
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
            .height(142.dp)
            .clip(RoundedCornerShape(20.dp))
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
        shape = RoundedCornerShape(20.dp),
        color = ColorCard,
        border = BorderStroke(1.dp, if (isPinned) ColorGold.copy(alpha = 0.5f) else ColorBorder)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Pin Action Button
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable { onTogglePin() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                    contentDescription = "تثبيت الأداة",
                    tint = if (isPinned) ColorGold else ColorSecondaryText,
                    modifier = Modifier.size(14.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Procedural Drawing Area instead of material icons
                ChamberToolIconDrawing(
                    calcKey = item.calcKey,
                    modifier = Modifier.size(42.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = item.titleAr,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorPrimaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = item.descriptionAr,
                    fontSize = 11.sp,
                    color = ColorSecondaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

// ==========================================
// 4. PROCEDURAL CANVA DRAWING ADAPTER
// ==========================================
@Composable
fun ChamberToolIconDrawing(
    calcKey: CalcKey,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f

        when (calcKey) {
            // 1. تفقيط الأرقام: Overlapping gold coins with elegant Arabic letter vectors
            CalcKey.NUM_WORDS -> {
                // Coin 1
                drawCircle(
                    color = ColorGold,
                    radius = 12.dp.toPx(),
                    center = Offset(centerX - 4.dp.toPx(), centerY + 4.dp.toPx()),
                    style = Stroke(width = 1.5.dp.toPx())
                )
                // Coin 2
                drawCircle(
                    color = ColorGold,
                    radius = 12.dp.toPx(),
                    center = Offset(centerX + 4.dp.toPx(), centerY - 4.dp.toPx()),
                    style = Stroke(width = 1.5.dp.toPx())
                )
                // Letter 'د' simple path vector on coin
                val path = Path().apply {
                    moveTo(centerX + 1.dp.toPx(), centerY - 7.dp.toPx())
                    lineTo(centerX + 5.dp.toPx(), centerY - 3.dp.toPx())
                    lineTo(centerX + 1.dp.toPx(), centerY + 1.dp.toPx())
                }
                drawPath(path, ColorPrimaryText, style = Stroke(width = 2.dp.toPx()))
            }

            // 2. حاسبة الوقود: Modern fuel pump silhouette with digital Turquoise screen
            CalcKey.FUEL_COST -> {
                // Pump outline
                drawRect(
                    color = ColorGold,
                    topLeft = Offset(centerX - 8.dp.toPx(), centerY - 14.dp.toPx()),
                    size = Size(16.dp.toPx(), 28.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx())
                )
                // Turquoise display
                drawRect(
                    color = ColorTurquoise,
                    topLeft = Offset(centerX - 5.dp.toPx(), centerY - 10.dp.toPx()),
                    size = Size(10.dp.toPx(), 7.dp.toPx())
                )
                // Hose line
                drawLine(
                    color = ColorGold,
                    start = Offset(centerX + 8.dp.toPx(), centerY),
                    end = Offset(centerX + 14.dp.toPx(), centerY + 10.dp.toPx()),
                    strokeWidth = 2.dp.toPx()
                )
            }

            // 3. حاسبة الدهان: Paint cylindrical can & mitered brush leaving sweeping gold trace
            CalcKey.PAINT -> {
                // Paint bucket oval
                drawRect(
                    color = ColorGold,
                    topLeft = Offset(centerX - 8.dp.toPx(), centerY - 6.dp.toPx()),
                    size = Size(16.dp.toPx(), 18.dp.toPx()),
                    style = Stroke(width = 1.5.dp.toPx())
                )
                // Sweeping gold path trace
                val path = Path().apply {
                    moveTo(centerX - 14.dp.toPx(), centerY - 10.dp.toPx())
                    quadraticTo(
                        centerX - 6.dp.toPx(), centerY - 14.dp.toPx(),
                        centerX + 12.dp.toPx(), centerY - 8.dp.toPx()
                    )
                }
                drawPath(path, ColorWarmGold, style = Stroke(width = 2.dp.toPx()))
            }

            // 4. تحويل المقاسات: Broken golden ruler halves with connecting Turquoise arrow
            CalcKey.UNIT -> {
                // Ruler 1
                drawLine(
                    color = ColorGold,
                    start = Offset(centerX - 14.dp.toPx(), centerY - 4.dp.toPx()),
                    end = Offset(centerX - 2.dp.toPx(), centerY - 4.dp.toPx()),
                    strokeWidth = 3.dp.toPx()
                )
                // Ruler 2
                drawLine(
                    color = ColorGold,
                    start = Offset(centerX + 2.dp.toPx(), centerY + 4.dp.toPx()),
                    end = Offset(centerX + 14.dp.toPx(), centerY + 4.dp.toPx()),
                    strokeWidth = 3.dp.toPx()
                )
                // Connecting turquoise arrow arc
                val path = Path().apply {
                    moveTo(centerX - 6.dp.toPx(), centerY + 2.dp.toPx())
                    quadraticTo(
                        centerX, centerY,
                        centerX + 6.dp.toPx(), centerY - 2.dp.toPx()
                    )
                }
                drawPath(path, ColorTurquoise, style = Stroke(width = 1.5.dp.toPx()))
            }

            // 5. دفتر الإيرادات والمصروفات: Minimalist wallet + ascending Turquoise trend path
            CalcKey.LEDGER -> {
                // Wallet body
                drawRect(
                    color = ColorGold,
                    topLeft = Offset(centerX - 12.dp.toPx(), centerY - 6.dp.toPx()),
                    size = Size(24.dp.toPx(), 14.dp.toPx()),
                    style = Stroke(width = 1.5.dp.toPx())
                )
                // Ascending trendline path
                val path = Path().apply {
                    moveTo(centerX - 10.dp.toPx(), centerY + 4.dp.toPx())
                    lineTo(centerX - 2.dp.toPx(), centerY - 2.dp.toPx())
                    lineTo(centerX + 6.dp.toPx(), centerY - 10.dp.toPx())
                }
                drawPath(path, ColorTurquoise, style = Stroke(width = 2.dp.toPx()))
            }

            // 6. المفكرة: Paper sheet vector with clean gold pen overlay
            CalcKey.NOTES -> {
                // Paper outline
                drawRect(
                    color = ColorPrimaryText.copy(alpha = 0.7f),
                    topLeft = Offset(centerX - 10.dp.toPx(), centerY - 12.dp.toPx()),
                    size = Size(20.dp.toPx(), 24.dp.toPx()),
                    style = Stroke(width = 1.5.dp.toPx())
                )
                // Gold pen miter line
                drawLine(
                    color = ColorGold,
                    start = Offset(centerX - 4.dp.toPx(), centerY + 6.dp.toPx()),
                    end = Offset(centerX + 10.dp.toPx(), centerY - 8.dp.toPx()),
                    strokeWidth = 2.5.dp.toPx()
                )
            }

            // 7. المتصفح: Globe outline with orbiting light-trail
            CalcKey.NEWS_BROWSER -> {
                // Central circle
                drawCircle(
                    color = ColorPrimaryText.copy(alpha = 0.5f),
                    radius = 12.dp.toPx(),
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 1.dp.toPx())
                )
                // Orbiting path ellipse ring
                drawCircle(
                    color = ColorTurquoise,
                    radius = 15.dp.toPx(),
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 1.5.dp.toPx())
                )
                // Glowing orbit dot focus point
                drawCircle(
                    color = ColorGold,
                    radius = 3.dp.toPx(),
                    center = Offset(centerX + 11.dp.toPx(), centerY - 10.dp.toPx())
                )
            }

            else -> {
                // Default fallback drawing (Geometric square)
                drawRect(
                    color = ColorGold,
                    topLeft = Offset(centerX - 10.dp.toPx(), centerY - 10.dp.toPx()),
                    size = Size(20.dp.toPx(), 20.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}
