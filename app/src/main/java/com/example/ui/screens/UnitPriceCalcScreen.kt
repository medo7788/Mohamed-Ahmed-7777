package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.*
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CalcKey
import com.example.ui.components.ToolScreenScaffold
import com.example.ui.theme.AppIcons
import com.example.ui.theme.CustomThemeColors
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

// ==========================================
// LUXURY CYBER OBSIDIAN PALETTE & MODELS
// ==========================================

private val ColorObsidianBgStart = Color(0xFF080A0F)
private val ColorObsidianBgEnd = Color(0xFF121620)
private val ColorGlassCard = Color(0xFF141926).copy(alpha = 0.85f)
private val ColorGoldBorder = Color(0xFFD4AF37)
private val ColorAmberGlow = Color(0xFFF59E0B)
private val ColorIceCyan = Color(0xFF00F2FE)
private val ColorEmeraldGreen = Color(0xFF10B981)
private val ColorCrimsonRed = Color(0xFFEF4444)
private val ColorLavender = Color(0xFFA78BFA)
private val ColorSlateMuted = Color(0xFF94A3B8)

enum class UnitCategory(val labelAr: String, val unitA: String, val unitB: String, val factor: BigDecimal) {
    GRAM_ML("جرام / مل (g/ml)", "جرام (g)", "كيلو (kg)", BigDecimal("1000")),
    KG_LITER("كيلو / لتر (kg/L)", "كيلو (kg)", "لتر (L)", BigDecimal("1")),
    PCS_PACK("قطعة / عبوة (Pcs/Pack)", "قطعة", "عبوة", BigDecimal("1")),
    DOZEN_TON("دستة / طن (Dozen/Ton)", "دستة", "طن", BigDecimal("1000"))
}

data class ExtraProductItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    var name: String = "",
    var price: String = "",
    var quantity: String = ""
)

data class ComparisonHistoryItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: String,
    val priceA: String,
    val qtyA: String,
    val priceB: String,
    val qtyB: String,
    val unitPriceA: String,
    val unitPriceB: String,
    val winnerText: String,
    val savingsPercent: String,
    val currency: String
)

// ==========================================
// MAIN COMPOSABLE SCREEN
// ==========================================

@Composable
fun UnitPriceCalcScreen(colors: CustomThemeColors) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current

    // User Inputs with State Retention
    var priceA by rememberSaveable { mutableStateOf("100") }
    var qtyA by rememberSaveable { mutableStateOf("500") }

    var priceB by rememberSaveable { mutableStateOf("180") }
    var qtyB by rememberSaveable { mutableStateOf("1000") }

    var selectedCurrency by rememberSaveable { mutableStateOf("EGP") }
    var selectedUnitCat by rememberSaveable { mutableStateOf(UnitCategory.GRAM_ML) }

    // Swap Button Rotation State
    var swapRotationAngle by remember { mutableFloatStateOf(0f) }
    val animatedRotation by animateFloatAsState(
        targetValue = swapRotationAngle,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "swapRotation"
    )

    // Expandable Sections
    var isMultiProductExpanded by rememberSaveable { mutableStateOf(false) }
    var isExplanationExpanded by rememberSaveable { mutableStateOf(false) }
    var isHistoryExpanded by rememberSaveable { mutableStateOf(false) }
    var isFavoritesExpanded by rememberSaveable { mutableStateOf(false) }

    // Multi Product List
    var extraProducts by remember { mutableStateOf(listOf<ExtraProductItem>()) }

    // Toast Message
    var toastMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2500)
            toastMessage = null
        }
    }

    // Saved History & Favorites
    var historyList by remember { mutableStateOf(loadHistoryFromPrefs(context)) }
    var favoritesList by remember { mutableStateOf(loadFavoritesFromPrefs(context)) }

    // BigDecimal Mathematical Engine
    val calculationResult = remember(priceA, qtyA, priceB, qtyB, selectedCurrency) {
        calculateUnitPriceComparison(priceA, qtyA, priceB, qtyB, selectedCurrency)
    }

    ToolScreenScaffold(
        colors = colors,
        icon = AppIcons.forCalc(CalcKey.UNIT_PRICE),
        title = "سعر الوحدة والمقارنة الذكية",
        subtitle = "مقارنة السعر بالجرام والكيلو والقطعة لاكتشاف العرض الأوفر فوراً",
        isScrollable = false
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(ColorObsidianBgStart, ColorObsidianBgEnd)
                    )
                )
        ) {
            // Background Grid Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val step = 60.dp.toPx()
                val linePaint = ColorGoldBorder.copy(alpha = 0.03f)
                for (x in 0..size.width.toInt() step step.toInt()) {
                    drawLine(linePaint, Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height), strokeWidth = 1f)
                }
                for (y in 0..size.height.toInt() step step.toInt()) {
                    drawLine(linePaint, Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), strokeWidth = 1f)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Toast Feedback Banner
                if (toastMessage != null) {
                    item(key = "toast_banner") {
                        Surface(
                            color = ColorEmeraldGreen,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(8.dp, RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(toastMessage!!, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }

                // SECTION A: Header & Unit Category Selector
                item(key = "header_section") {
                    HeaderCard(
                        selectedCurrency = selectedCurrency,
                        onCurrencySelected = {
                            selectedCurrency = it
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    )
                }

                item(key = "unit_category_row") {
                    UnitCategorySelectorRow(
                        selectedCategory = selectedUnitCat,
                        onCategorySelected = {
                            selectedUnitCat = it
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    )
                }

                // SECTION B: Dual Product Input Panel & Winner Card
                item(key = "dual_product_input") {
                    DualProductInputPanel(
                        priceA = priceA,
                        onPriceAChange = { priceA = it },
                        qtyA = qtyA,
                        onQtyAChange = { qtyA = it },
                        priceB = priceB,
                        onPriceBChange = { priceB = it },
                        qtyB = qtyB,
                        onQtyBChange = { qtyB = it },
                        unitLabel = selectedUnitCat.unitA,
                        rotationAngle = animatedRotation,
                        onSwap = {
                            val tempP = priceA
                            val tempQ = qtyA
                            priceA = priceB
                            qtyA = qtyB
                            priceB = tempP
                            qtyB = tempQ
                            swapRotationAngle += 180f
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            toastMessage = "تم تبديل قيم المنتج أ و ب"
                        }
                    )
                }

                // Winner Result Card
                item(key = "winner_result_card") {
                    WinnerComparisonCard(
                        result = calculationResult,
                        unitLabel = selectedUnitCat.unitA,
                        currency = selectedCurrency,
                        onSaveHistory = {
                            val timeStr = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm - yyyy/MM/dd"))
                            val newItem = ComparisonHistoryItem(
                                timestamp = timeStr,
                                priceA = priceA,
                                qtyA = qtyA,
                                priceB = priceB,
                                qtyB = qtyB,
                                unitPriceA = calculationResult.unitPriceA.toPlainString(),
                                unitPriceB = calculationResult.unitPriceB.toPlainString(),
                                winnerText = calculationResult.winnerText,
                                savingsPercent = calculationResult.savingsPercent.toPlainString(),
                                currency = selectedCurrency
                            )
                            historyList = listOf(newItem) + historyList.take(19)
                            saveHistoryToPrefs(context, historyList)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            toastMessage = "تم حفظ المقارنة في السجل"
                        },
                        onSaveFavorite = {
                            val timeStr = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm - yyyy/MM/dd"))
                            val newItem = ComparisonHistoryItem(
                                timestamp = timeStr,
                                priceA = priceA,
                                qtyA = qtyA,
                                priceB = priceB,
                                qtyB = qtyB,
                                unitPriceA = calculationResult.unitPriceA.toPlainString(),
                                unitPriceB = calculationResult.unitPriceB.toPlainString(),
                                winnerText = calculationResult.winnerText,
                                savingsPercent = calculationResult.savingsPercent.toPlainString(),
                                currency = selectedCurrency
                            )
                            favoritesList = listOf(newItem) + favoritesList
                            saveFavoritesToPrefs(context, favoritesList)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            toastMessage = "تمت إضافة المقارنة للمفضلة ⭐"
                        },
                        onShare = {
                            val shareText = "📊 نتيجة مقارنة الأسعار (${selectedCurrency}):\n" +
                                    "• المنتج (أ): $priceA لـ $qtyA ${selectedUnitCat.unitA} (سعر الوحدة: ${calculationResult.unitPriceAStr})\n" +
                                    "• المنتج (ب): $priceB لـ $qtyB ${selectedUnitCat.unitA} (سعر الوحدة: ${calculationResult.unitPriceBStr})\n" +
                                    "🏆 النتيجة: ${calculationResult.winnerText}\n" +
                                    "💰 نسبة التوفير: ${calculationResult.savingsPercentStr}%\n" +
                                    "محسوب بـ حاسبة التواريخ وسعر الوحدة الذكية"
                            clipboardManager.setText(AnnotatedString(shareText))
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            toastMessage = "تم نسخ المقارنة للحافظة"
                        },
                        onReset = {
                            priceA = "100"
                            qtyA = "500"
                            priceB = "180"
                            qtyB = "1000"
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            toastMessage = "تم إعادة ضبط المدخلات"
                        }
                    )
                }

                // SECTION C: Multi-Product Expansion Mode
                item(key = "multi_product_drawer") {
                    ExpandableSectionCard(
                        title = "مقارنة متعددة (حتى 4 منتجات)",
                        icon = Icons.Outlined.FormatListNumberedRtl,
                        isExpanded = isMultiProductExpanded,
                        onToggle = { isMultiProductExpanded = !isMultiProductExpanded }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("أضف منتجات إضافية للمقارنة الجماعية:", fontSize = 12.sp, color = ColorSlateMuted)

                            extraProducts.forEachIndexed { index, item ->
                                Surface(
                                    color = Color.Black.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, ColorIceCyan.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("${index + 3}#", color = ColorAmberGlow, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                                        OutlinedTextField(
                                            value = item.price,
                                            onValueChange = { newP ->
                                                extraProducts = extraProducts.toMutableList().also { l -> l[index] = item.copy(price = newP) }
                                            },
                                            label = { Text("السعر", fontSize = 10.sp) },
                                            modifier = Modifier.weight(1f),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = ColorGoldBorder,
                                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White
                                            )
                                        )

                                        OutlinedTextField(
                                            value = item.quantity,
                                            onValueChange = { newQ ->
                                                extraProducts = extraProducts.toMutableList().also { l -> l[index] = item.copy(quantity = newQ) }
                                            },
                                            label = { Text("الكمية", fontSize = 10.sp) },
                                            modifier = Modifier.weight(1f),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = ColorGoldBorder,
                                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White
                                            )
                                        )

                                        IconButton(onClick = {
                                            extraProducts = extraProducts.filterIndexed { i, _ -> i != index }
                                        }) {
                                            Icon(Icons.Filled.Close, contentDescription = "حذف", tint = ColorCrimsonRed)
                                        }
                                    }
                                }
                            }

                            if (extraProducts.size < 2) {
                                Button(
                                    onClick = {
                                        extraProducts = extraProducts + ExtraProductItem()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ColorIceCyan.copy(alpha = 0.2f), contentColor = ColorIceCyan),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("إضافة منتج إضافي", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // SECTION D: Educational Breakdown Section
                item(key = "educational_drawer") {
                    ExpandableSectionCard(
                        title = "طريقة الحساب الرياضية وشرح المعادلة",
                        icon = Icons.Outlined.School,
                        isExpanded = isExplanationExpanded,
                        onToggle = { isExplanationExpanded = !isExplanationExpanded }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, ColorGoldBorder.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("📐 معادلة سعر الوحدة القياسية:", color = ColorAmberGlow, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("سعر الوحدة = الإجمالي (السعر) ÷ الكمية المشتراة", color = Color.White, fontSize = 12.sp)
                                }
                            }

                            Text("• سعر وحدة المنتج (أ) = $priceA ÷ $qtyA = ${calculationResult.unitPriceAStr} $selectedCurrency", color = ColorSlateMuted, fontSize = 12.sp)
                            Text("• سعر وحدة المنتج (ب) = $priceB ÷ $qtyB = ${calculationResult.unitPriceBStr} $selectedCurrency", color = ColorSlateMuted, fontSize = 12.sp)
                            Text("• الفرق المطلق = ${calculationResult.diffAmountStr} $selectedCurrency لكل وحدة قياسية", color = ColorEmeraldGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // SECTION E: Favorites Section
                if (favoritesList.isNotEmpty()) {
                    item(key = "favorites_section") {
                        ExpandableSectionCard(
                            title = "المقارنات المفضلة (${favoritesList.size})",
                            icon = Icons.Outlined.Star,
                            isExpanded = isFavoritesExpanded,
                            onToggle = { isFavoritesExpanded = !isFavoritesExpanded }
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                favoritesList.forEach { fav ->
                                    HistoryItemRow(
                                        item = fav,
                                        onReuse = {
                                            priceA = fav.priceA
                                            qtyA = fav.qtyA
                                            priceB = fav.priceB
                                            qtyB = fav.qtyB
                                            selectedCurrency = fav.currency
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            toastMessage = "تم إدراج بيانات المفضلة"
                                        },
                                        onDelete = {
                                            favoritesList = favoritesList.filter { it.id != fav.id }
                                            saveFavoritesToPrefs(context, favoritesList)
                                            toastMessage = "تم حذف العنصر من المفضلة"
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // SECTION F: History Section
                if (historyList.isNotEmpty()) {
                    item(key = "history_drawer") {
                        ExpandableSectionCard(
                            title = "سجل المقارنات السابق (${historyList.size})",
                            icon = Icons.Outlined.History,
                            isExpanded = isHistoryExpanded,
                            onToggle = { isHistoryExpanded = !isHistoryExpanded }
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                historyList.forEach { hist ->
                                    HistoryItemRow(
                                        item = hist,
                                        onReuse = {
                                            priceA = hist.priceA
                                            qtyA = hist.qtyA
                                            priceB = hist.priceB
                                            qtyB = hist.qtyB
                                            selectedCurrency = hist.currency
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            toastMessage = "تم إعادة استخدام المقارنة السابقة"
                                        },
                                        onDelete = {
                                            historyList = historyList.filter { it.id != hist.id }
                                            saveHistoryToPrefs(context, historyList)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Offline Notice
                item(key = "offline_footer") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = ColorEmeraldGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("جميع العمليات الحسابية تتم محلياً باستخدام BigDecimal بنسبة دقة 100%", fontSize = 11.sp, color = ColorSlateMuted)
                    }
                }
            }
        }
    }
}

// ==========================================
// SUB-COMPONENTS
// ==========================================

@Composable
private fun HeaderCard(
    selectedCurrency: String,
    onCurrencySelected: (String) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Surface(
        color = ColorGlassCard,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, ColorGoldBorder.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Procedural Canvas Shopping Cart Icon
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(ColorAmberGlow.copy(alpha = 0.35f), Color.Transparent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(36.dp)) {
                    val w = size.width
                    val h = size.height

                    // Shopping Cart body
                    val cartPath = Path().apply {
                        moveTo(w * 0.15f, h * 0.25f)
                        lineTo(w * 0.35f, h * 0.25f)
                        lineTo(w * 0.45f, h * 0.65f)
                        lineTo(w * 0.85f, h * 0.65f)
                        lineTo(w * 0.95f, h * 0.35f)
                        lineTo(w * 0.3f, h * 0.35f)
                    }
                    drawPath(cartPath, color = ColorGoldBorder, style = Stroke(width = 3.5f))

                    // Cart Wheels
                    drawCircle(ColorIceCyan, radius = 4f, center = Offset(w * 0.5f, h * 0.8f))
                    drawCircle(ColorIceCyan, radius = 4f, center = Offset(w * 0.8f, h * 0.8f))
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "مقارن الأسعار والوحدات",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "اكتشف الخيار الأوفر مالياً على الفور",
                    fontSize = 11.sp,
                    color = ColorSlateMuted
                )
            }

            // Currency Switcher Dropdown Chips
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("EGP", "SAR", "USD").forEach { curr ->
                    val isSelected = curr == selectedCurrency
                    Surface(
                        color = if (isSelected) ColorAmberGlow.copy(alpha = 0.25f) else Color(0xFF1E2638),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (isSelected) ColorAmberGlow else Color.White.copy(alpha = 0.15f)),
                        modifier = Modifier.clickable { onCurrencySelected(curr) }
                    ) {
                        Text(
                            curr,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) ColorAmberGlow else ColorSlateMuted,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UnitCategorySelectorRow(
    selectedCategory: UnitCategory,
    onCategorySelected: (UnitCategory) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(UnitCategory.values(), key = { it.name }) { cat ->
            val isSelected = cat == selectedCategory
            val bg = if (isSelected) ColorAmberGlow.copy(alpha = 0.25f) else ColorGlassCard
            val borderCol = if (isSelected) ColorAmberGlow else Color.White.copy(alpha = 0.1f)

            Surface(
                color = bg,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, borderCol),
                modifier = Modifier.clickable { onCategorySelected(cat) }
            ) {
                Text(
                    cat.labelAr,
                    fontSize = 12.sp,
                    color = if (isSelected) Color.White else ColorSlateMuted,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun DualProductInputPanel(
    priceA: String,
    onPriceAChange: (String) -> Unit,
    qtyA: String,
    onQtyAChange: (String) -> Unit,
    priceB: String,
    onPriceBChange: (String) -> Unit,
    qtyB: String,
    onQtyBChange: (String) -> Unit,
    unitLabel: String,
    rotationAngle: Float,
    onSwap: () -> Unit
) {
    Surface(
        color = ColorGlassCard,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, ColorGoldBorder.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Product A Column
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("المنتج (أ)", color = ColorAmberGlow, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    OutlinedTextField(
                        value = priceA,
                        onValueChange = onPriceAChange,
                        label = { Text("السعر", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ColorGoldBorder,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    OutlinedTextField(
                        value = qtyA,
                        onValueChange = onQtyAChange,
                        label = { Text("الكمية ($unitLabel)", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ColorGoldBorder,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                // Tactile Swap Button (Center)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .rotate(rotationAngle)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(ColorGoldBorder.copy(alpha = 0.3f), Color.Transparent)
                            )
                        )
                        .border(1.dp, ColorGoldBorder, CircleShape)
                        .clickable { onSwap() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.SwapHoriz, contentDescription = "تبديل", tint = ColorGoldBorder)
                }

                // Product B Column
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("المنتج (ب)", color = ColorIceCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    OutlinedTextField(
                        value = priceB,
                        onValueChange = onPriceBChange,
                        label = { Text("السعر", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ColorIceCyan,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    OutlinedTextField(
                        value = qtyB,
                        onValueChange = onQtyBChange,
                        label = { Text("الكمية ($unitLabel)", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ColorIceCyan,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Preset Quick Quantity Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("اختصارات سريعة للكمية:", fontSize = 11.sp, color = ColorSlateMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("250", "500", "1000").forEach { preset ->
                        Surface(
                            color = Color(0xFF1E2638),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                            modifier = Modifier.clickable {
                                onQtyAChange(preset)
                                onQtyBChange(preset)
                            }
                        ) {
                            Text(preset, fontSize = 10.sp, color = ColorIceCyan, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WinnerComparisonCard(
    result: CalculationResultData,
    unitLabel: String,
    currency: String,
    onSaveHistory: () -> Unit,
    onSaveFavorite: () -> Unit,
    onShare: () -> Unit,
    onReset: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Surface(
        color = ColorGlassCard,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.5.dp, ColorEmeraldGreen.copy(alpha = glowAlpha)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Dynamic Winner Header Badge
            Surface(
                color = ColorEmeraldGreen.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, ColorEmeraldGreen)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🏆", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        result.winnerText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = ColorEmeraldGreen
                    )
                }
            }

            // Percentage Savings Indicator
            if (result.savingsPercent > BigDecimal.ZERO) {
                Text(
                    "وفر حوالي ${result.savingsPercentStr}% عند اختيار هذا العرض!",
                    fontSize = 13.sp,
                    color = ColorAmberGlow,
                    fontWeight = FontWeight.Bold
                )
            }

            // Side-by-side Unit Price Breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("سعر وحدة (أ)", fontSize = 11.sp, color = ColorSlateMuted)
                    Text(
                        "${result.unitPriceAStr} $currency",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (result.isAWinner) ColorEmeraldGreen else Color.White
                    )
                    Text("لكل $unitLabel", fontSize = 10.sp, color = ColorSlateMuted)
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(Color.White.copy(alpha = 0.15f))
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("سعر وحدة (ب)", fontSize = 11.sp, color = ColorSlateMuted)
                    Text(
                        "${result.unitPriceBStr} $currency",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (!result.isAWinner) ColorEmeraldGreen else Color.White
                    )
                    Text("لكل $unitLabel", fontSize = 10.sp, color = ColorSlateMuted)
                }
            }

            // Procedural Canvas Ratio Comparison Bar
            CanvasBarComparisonGraph(
                unitPriceA = result.unitPriceA.toFloat(),
                unitPriceB = result.unitPriceB.toFloat(),
                isAWinner = result.isAWinner
            )

            // Quick Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onShare,
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFF1E2638), RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Filled.Share, contentDescription = "مشاركة", tint = ColorIceCyan)
                }

                IconButton(
                    onClick = onSaveFavorite,
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFF1E2638), RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Filled.Star, contentDescription = "المفضلة", tint = ColorAmberGlow)
                }

                IconButton(
                    onClick = onSaveHistory,
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFF1E2638), RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Filled.Save, contentDescription = "حفظ السجل", tint = ColorEmeraldGreen)
                }

                IconButton(
                    onClick = onReset,
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFF1E2638), RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = "إعادة ضبط", tint = ColorSlateMuted)
                }
            }
        }
    }
}

@Composable
private fun CanvasBarComparisonGraph(
    unitPriceA: Float,
    unitPriceB: Float,
    isAWinner: Boolean
) {
    val maxVal = maxOf(unitPriceA, unitPriceB, 0.001f)
    val ratioA = (unitPriceA / maxVal).coerceIn(0.1f, 1f)
    val ratioB = (unitPriceB / maxVal).coerceIn(0.1f, 1f)

    val animatedRatioA by animateFloatAsState(targetValue = ratioA, label = "ratioA")
    val animatedRatioB by animateFloatAsState(targetValue = ratioB, label = "ratioB")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("نسبة مقارنة سعر الوحدة:", fontSize = 11.sp, color = ColorSlateMuted)

        // Bar A
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("أ", fontSize = 12.sp, color = ColorAmberGlow, fontWeight = FontWeight.Bold, modifier = Modifier.width(20.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color(0xFF1E2638))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedRatioA)
                        .background(if (isAWinner) ColorEmeraldGreen else ColorAmberGlow)
                )
            }
        }

        // Bar B
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("ب", fontSize = 12.sp, color = ColorIceCyan, fontWeight = FontWeight.Bold, modifier = Modifier.width(20.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color(0xFF1E2638))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedRatioB)
                        .background(if (!isAWinner) ColorEmeraldGreen else ColorIceCyan)
                )
            }
        }
    }
}

@Composable
private fun ExpandableSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        color = ColorGlassCard,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, ColorGoldBorder.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = ColorAmberGlow, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Icon(
                    if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = ColorSlateMuted
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun HistoryItemRow(
    item: ComparisonHistoryItem,
    onReuse: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = Color.Black.copy(alpha = 0.35f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.winnerText, color = ColorEmeraldGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("أ: ${item.priceA}/${item.qtyA} | ب: ${item.priceB}/${item.qtyB} (${item.currency})", color = ColorSlateMuted, fontSize = 11.sp)
                Text("توفير ${item.savingsPercent}% • ${item.timestamp}", color = ColorIceCyan, fontSize = 10.sp)
            }

            Row {
                IconButton(onClick = onReuse) {
                    Icon(Icons.Filled.Redo, contentDescription = "تطبيق", tint = ColorGoldBorder)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "حذف", tint = ColorCrimsonRed)
                }
            }
        }
    }
}

// ==========================================
// CALCULATOR ENGINE & PERSISTENCE
// ==========================================

data class CalculationResultData(
    val unitPriceA: BigDecimal,
    val unitPriceB: BigDecimal,
    val unitPriceAStr: String,
    val unitPriceBStr: String,
    val isAWinner: Boolean,
    val winnerText: String,
    val diffAmountStr: String,
    val savingsPercent: BigDecimal,
    val savingsPercentStr: String
)

private fun calculateUnitPriceComparison(
    priceA: String,
    qtyA: String,
    priceB: String,
    qtyB: String,
    currency: String
): CalculationResultData {
    val pA = priceA.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val qA = qtyA.toBigDecimalOrNull() ?: BigDecimal.ONE

    val pB = priceB.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val qB = qtyB.toBigDecimalOrNull() ?: BigDecimal.ONE

    val unitA = if (qA > BigDecimal.ZERO) pA.divide(qA, 4, RoundingMode.HALF_UP) else BigDecimal.ZERO
    val unitB = if (qB > BigDecimal.ZERO) pB.divide(qB, 4, RoundingMode.HALF_UP) else BigDecimal.ZERO

    val isAWinner = unitA <= unitB

    val higher = maxOf(unitA, unitB)
    val lower = minOf(unitA, unitB)
    val diff = higher - lower

    val savings = if (higher > BigDecimal.ZERO) {
        diff.multiply(BigDecimal("100")).divide(higher, 2, RoundingMode.HALF_UP)
    } else BigDecimal.ZERO

    val winnerText = if (unitA == unitB) {
        "المنتجان متساويان في السعر 🤝"
    } else if (isAWinner) {
        "المنتج (أ) هو الأوفر ✅"
    } else {
        "المنتج (ب) هو الأوفر ✅"
    }

    return CalculationResultData(
        unitPriceA = unitA,
        unitPriceB = unitB,
        unitPriceAStr = String.format(Locale.US, "%.3f", unitA.toDouble()),
        unitPriceBStr = String.format(Locale.US, "%.3f", unitB.toDouble()),
        isAWinner = isAWinner,
        winnerText = winnerText,
        diffAmountStr = String.format(Locale.US, "%.3f", diff.toDouble()),
        savingsPercent = savings,
        savingsPercentStr = String.format(Locale.US, "%.1f", savings.toDouble())
    )
}

private fun saveHistoryToPrefs(context: Context, list: List<ComparisonHistoryItem>) {
    try {
        val prefs = context.getSharedPreferences("unit_price_prefs", Context.MODE_PRIVATE)
        val arr = JSONArray()
        list.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("timestamp", item.timestamp)
            obj.put("priceA", item.priceA)
            obj.put("qtyA", item.qtyA)
            obj.put("priceB", item.priceB)
            obj.put("qtyB", item.qtyB)
            obj.put("unitPriceA", item.unitPriceA)
            obj.put("unitPriceB", item.unitPriceB)
            obj.put("winnerText", item.winnerText)
            obj.put("savingsPercent", item.savingsPercent)
            obj.put("currency", item.currency)
            arr.put(obj)
        }
        prefs.edit().putString("history_json", arr.toString()).apply()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun loadHistoryFromPrefs(context: Context): List<ComparisonHistoryItem> {
    val list = mutableListOf<ComparisonHistoryItem>()
    try {
        val prefs = context.getSharedPreferences("unit_price_prefs", Context.MODE_PRIVATE)
        val str = prefs.getString("history_json", null) ?: return emptyList()
        val arr = JSONArray(str)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                ComparisonHistoryItem(
                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                    timestamp = obj.optString("timestamp", ""),
                    priceA = obj.optString("priceA", ""),
                    qtyA = obj.optString("qtyA", ""),
                    priceB = obj.optString("priceB", ""),
                    qtyB = obj.optString("qtyB", ""),
                    unitPriceA = obj.optString("unitPriceA", ""),
                    unitPriceB = obj.optString("unitPriceB", ""),
                    winnerText = obj.optString("winnerText", ""),
                    savingsPercent = obj.optString("savingsPercent", ""),
                    currency = obj.optString("currency", "EGP")
                )
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}

private fun saveFavoritesToPrefs(context: Context, list: List<ComparisonHistoryItem>) {
    try {
        val prefs = context.getSharedPreferences("unit_price_prefs", Context.MODE_PRIVATE)
        val arr = JSONArray()
        list.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("timestamp", item.timestamp)
            obj.put("priceA", item.priceA)
            obj.put("qtyA", item.qtyA)
            obj.put("priceB", item.priceB)
            obj.put("qtyB", item.qtyB)
            obj.put("unitPriceA", item.unitPriceA)
            obj.put("unitPriceB", item.unitPriceB)
            obj.put("winnerText", item.winnerText)
            obj.put("savingsPercent", item.savingsPercent)
            obj.put("currency", item.currency)
            arr.put(obj)
        }
        prefs.edit().putString("favorites_json", arr.toString()).apply()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun loadFavoritesFromPrefs(context: Context): List<ComparisonHistoryItem> {
    val list = mutableListOf<ComparisonHistoryItem>()
    try {
        val prefs = context.getSharedPreferences("unit_price_prefs", Context.MODE_PRIVATE)
        val str = prefs.getString("favorites_json", null) ?: return emptyList()
        val arr = JSONArray(str)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                ComparisonHistoryItem(
                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                    timestamp = obj.optString("timestamp", ""),
                    priceA = obj.optString("priceA", ""),
                    qtyA = obj.optString("qtyA", ""),
                    priceB = obj.optString("priceB", ""),
                    qtyB = obj.optString("qtyB", ""),
                    unitPriceA = obj.optString("unitPriceA", ""),
                    unitPriceB = obj.optString("unitPriceB", ""),
                    winnerText = obj.optString("winnerText", ""),
                    savingsPercent = obj.optString("savingsPercent", ""),
                    currency = obj.optString("currency", "EGP")
                )
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}
