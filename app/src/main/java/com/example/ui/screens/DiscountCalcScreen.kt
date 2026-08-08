package com.example.ui.screens

import android.content.Context
import android.content.Intent
import java.util.Locale
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CalcKey
import com.example.ui.components.ToolScreenScaffold
import com.example.ui.theme.AppIcons
import com.example.ui.theme.CustomThemeColors
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.roundToInt

// --- DATA MODELS & ENUMS ---

enum class DiscountMode(val title: String, val subtitle: String) {
    SIMPLE("خصم بسيط", "حساب السعر بعد الخصم المباشر والتوفير"),
    DOUBLE("خصم إضافي / كوبون", "تطبيق خصم أساسي مضاف إليه خصم الكوبون أو القسيمة"),
    VAT("مع ضريبة القيمة المضافة", "حساب الخصم مضاف إليه نسبة ضريبة المبيعات أو VAT"),
    REVERSE("حساب عكسي", "معرفة السعر الأصلي أو نسبة الخصم من السعر النهائي"),
    OFFERS("عروض (اشتر X واحصل على Y)", "حساب قيمة العروض الترويجية والهدايا المجانية")
}

data class DiscountHistoryItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val mode: DiscountMode,
    val originalPrice: Double,
    val finalPrice: Double,
    val savedAmount: Double,
    val discountPercent: Double,
    val currency: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)

data class DiscountCalculationResult(
    val originalPrice: Double,
    val primaryDiscountPercent: Double,
    val primaryDiscountAmount: Double,
    val extraDiscountAmount: Double,
    val taxAmount: Double,
    val finalPrice: Double,
    val totalSaved: Double,
    val effectiveDiscountPercent: Double
)

// --- MAIN COMPOSABLE SCREEN ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscountCalcScreen(colors: CustomThemeColors) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // SharedPreferences for History Persistence
    val sharedPrefs = remember {
        context.getSharedPreferences("discount_calc_prefs", Context.MODE_PRIVATE)
    }

    // States
    var activeMode by rememberSaveable { mutableStateOf(DiscountMode.SIMPLE) }
    var selectedCurrency by rememberSaveable { mutableStateOf("EGP") }

    // Input States
    var originalPriceText by rememberSaveable { mutableStateOf("1000") }
    var discountPercentText by rememberSaveable { mutableStateOf("20") }
    var extraDiscountText by rememberSaveable { mutableStateOf("5") } // for double/coupon
    var vatPercentText by rememberSaveable { mutableStateOf("14") } // for VAT mode
    var finalPaidPriceText by rememberSaveable { mutableStateOf("800") } // for reverse mode

    // Buy X Get Y States
    var buyQuantityText by rememberSaveable { mutableStateOf("2") }
    var getQuantityText by rememberSaveable { mutableStateOf("1") }
    var itemUnitPriceText by rememberSaveable { mutableStateOf("150") }

    // Expandable Sections
    var showHistoryDrawer by rememberSaveable { mutableStateOf(false) }

    // History JSON
    val initialHistoryJson = remember(sharedPrefs) { sharedPrefs.getString("discount_history_json", "[]") ?: "[]" }
    var historyListJson by remember { mutableStateOf(initialHistoryJson) }

    val historyItems = remember(historyListJson) {
        parseDiscountHistory(historyListJson)
    }

    fun saveHistory(newList: List<DiscountHistoryItem>) {
        val json = serializeDiscountHistory(newList)
        sharedPrefs.edit().putString("discount_history_json", json).apply()
        historyListJson = json
    }

    // Error Shake Animation Trigger
    var triggerShake by remember { mutableStateOf(false) }
    val shakeOffset by animateFloatAsState(
        targetValue = if (triggerShake) 12f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium),
        finishedListener = { triggerShake = false },
        label = "shake"
    )

    // Calculations using derivedStateOf for instant 60fps responsiveness
    val calculationResult = remember(
        activeMode,
        originalPriceText,
        discountPercentText,
        extraDiscountText,
        vatPercentText,
        finalPaidPriceText,
        buyQuantityText,
        getQuantityText,
        itemUnitPriceText
    ) {
        derivedStateOf {
            val price = originalPriceText.toDoubleOrNull() ?: 0.0
            val pDisc = discountPercentText.toDoubleOrNull() ?: 0.0

            if (activeMode == DiscountMode.REVERSE) {
                val finalP = finalPaidPriceText.toDoubleOrNull() ?: 0.0
                if (finalP <= 0 || price <= 0) {
                    // if price is 0, try to estimate from finalP if discount is given
                    val estOrig = if (pDisc in 0.0..99.9) finalP / (1.0 - (pDisc / 100.0)) else finalP
                    val saved = max(0.0, estOrig - finalP)
                    return@derivedStateOf DiscountCalculationResult(
                        originalPrice = estOrig,
                        primaryDiscountPercent = pDisc,
                        primaryDiscountAmount = saved,
                        extraDiscountAmount = 0.0,
                        taxAmount = 0.0,
                        finalPrice = finalP,
                        totalSaved = saved,
                        effectiveDiscountPercent = if (estOrig > 0) (saved / estOrig) * 100.0 else 0.0
                    )
                }
                val saved = max(0.0, price - finalP)
                val effPct = if (price > 0) (saved / price) * 100.0 else 0.0
                return@derivedStateOf DiscountCalculationResult(
                    originalPrice = price,
                    primaryDiscountPercent = effPct,
                    primaryDiscountAmount = saved,
                    extraDiscountAmount = 0.0,
                    taxAmount = 0.0,
                    finalPrice = finalP,
                    totalSaved = saved,
                    effectiveDiscountPercent = effPct
                )
            }

            if (activeMode == DiscountMode.OFFERS) {
                val bQty = buyQuantityText.toIntOrNull() ?: 1
                val gQty = getQuantityText.toIntOrNull() ?: 0
                val unitP = itemUnitPriceText.toDoubleOrNull() ?: 0.0
                val totalItems = bQty + gQty
                val totalGrossPrice = totalItems * unitP
                val finalP = bQty * unitP
                val saved = gQty * unitP
                val effPct = if (totalGrossPrice > 0) (saved / totalGrossPrice) * 100.0 else 0.0

                return@derivedStateOf DiscountCalculationResult(
                    originalPrice = totalGrossPrice,
                    primaryDiscountPercent = effPct,
                    primaryDiscountAmount = saved,
                    extraDiscountAmount = 0.0,
                    taxAmount = 0.0,
                    finalPrice = finalP,
                    totalSaved = saved,
                    effectiveDiscountPercent = effPct
                )
            }

            // Standard, Double, VAT modes
            if (price <= 0) return@derivedStateOf null

            val primaryDiscAmount = price * (pDisc / 100.0)
            val priceAfterPrimary = max(0.0, price - primaryDiscAmount)

            var extraDiscAmount = 0.0
            if (activeMode == DiscountMode.DOUBLE) {
                val extraPct = extraDiscountText.toDoubleOrNull() ?: 0.0
                extraDiscAmount = priceAfterPrimary * (extraPct / 100.0)
            }

            val subtotalAfterDiscounts = max(0.0, priceAfterPrimary - extraDiscAmount)

            var taxAmount = 0.0
            if (activeMode == DiscountMode.VAT) {
                val vatPct = vatPercentText.toDoubleOrNull() ?: 0.0
                taxAmount = subtotalAfterDiscounts * (vatPct / 100.0)
            }

            val finalP = subtotalAfterDiscounts + taxAmount
            val totalSaved = max(0.0, price - finalP + taxAmount) // or just savings before tax
            val netSaved = max(0.0, price - subtotalAfterDiscounts)
            val effPct = if (price > 0) (netSaved / price) * 100.0 else 0.0

            DiscountCalculationResult(
                originalPrice = price,
                primaryDiscountPercent = pDisc,
                primaryDiscountAmount = primaryDiscAmount,
                extraDiscountAmount = extraDiscAmount,
                taxAmount = taxAmount,
                finalPrice = finalP,
                totalSaved = netSaved,
                effectiveDiscountPercent = effPct
            )
        }
    }.value

    ToolScreenScaffold(
        colors = colors,
        icon = AppIcons.forCalc(CalcKey.DISCOUNT),
        title = "حاسبة الخصم والتخفيض وسلة التوفير",
        subtitle = "حساب الأسعار النهائية، التخفيضات المركبة، والعروض الترويجية بدقة",
        isScrollable = false
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. TOP HEADER & CURRENCY SWITCHER
                item {
                    DiscountHeaderSection(
                        colors = colors,
                        selectedCurrency = selectedCurrency,
                        onCurrencyChange = { curr ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedCurrency = curr
                        }
                    )
                }

                // 2. SEGMENTED MODE SELECTOR BAR (5 Modes)
                item {
                    ModeSelectorRow(
                        activeMode = activeMode,
                        onModeSelected = { mode ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            activeMode = mode
                        },
                        colors = colors
                    )
                }

                // 3. HERO RESULT CARD & SAVINGS VISUALIZER
                item {
                    if (calculationResult != null) {
                        HeroDiscountResultCard(
                            result = calculationResult,
                            currency = selectedCurrency,
                            activeMode = activeMode,
                            colors = colors
                        )
                    } else {
                        EmptyDiscountCard(
                            originalPriceText = originalPriceText,
                            shakeOffset = shakeOffset,
                            colors = colors,
                            onPresetPrice = { price ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                originalPriceText = price.toString()
                            }
                        )
                    }
                }

                // 4. INPUT CONTROLS & PRESET CHIPS (Varying by Active Mode)
                item {
                    DiscountInputSection(
                        activeMode = activeMode,
                        originalPriceText = originalPriceText,
                        onOriginalPriceChange = { originalPriceText = it },
                        discountPercentText = discountPercentText,
                        onDiscountPercentChange = { discountPercentText = it },
                        extraDiscountText = extraDiscountText,
                        onExtraDiscountChange = { extraDiscountText = it },
                        vatPercentText = vatPercentText,
                        onVatPercentChange = { vatPercentText = it },
                        finalPaidPriceText = finalPaidPriceText,
                        onFinalPaidPriceChange = { finalPaidPriceText = it },
                        buyQuantityText = buyQuantityText,
                        onBuyQuantityChange = { buyQuantityText = it },
                        getQuantityText = getQuantityText,
                        onGetQuantityChange = { getQuantityText = it },
                        itemUnitPriceText = itemUnitPriceText,
                        onItemUnitPriceChange = { itemUnitPriceText = it },
                        currency = selectedCurrency,
                        shakeOffset = shakeOffset,
                        colors = colors,
                        haptic = haptic
                    )
                }

                // 5. ACTION BUTTONS ROW (Copy, Share, Save History, Reset)
                item {
                    DiscountActionButtonsRow(
                        result = calculationResult,
                        currency = selectedCurrency,
                        colors = colors,
                        onCopy = {
                            if (calculationResult != null) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val summary = """
                                    🏷️ ملخص حاسبة الخصم والتخفيض ($selectedCurrency):
                                    • السعر الأصلي: ${formatCurrency(calculationResult.originalPrice, selectedCurrency)}
                                    • نسبة الخصم: ${String.format(Locale.US, "%.1f", calculationResult.effectiveDiscountPercent)}%
                                    • إجمالي التوفير: ${formatCurrency(calculationResult.totalSaved, selectedCurrency)}
                                    • السعر النهائي: ${formatCurrency(calculationResult.finalPrice, selectedCurrency)}
                                """.trimIndent()
                                clipboardManager.setText(AnnotatedString(summary))
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("تم نسخ ملخص الخصم إلى الحافظة بنجاح 📋")
                                }
                            }
                        },
                        onShare = {
                            if (calculationResult != null) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "عرض تسوق مميز! السعر النهائي ${formatCurrency(calculationResult.finalPrice, selectedCurrency)} " +
                                                "(وفرت ${formatCurrency(calculationResult.totalSaved, selectedCurrency)} بنسبة ${String.format(Locale.US, "%.1f", calculationResult.effectiveDiscountPercent)}%)"
                                    )
                                }
                                context.startActivity(Intent.createChooser(intent, "مشاركة تفاصيل التوفير"))
                            }
                        },
                        onSaveHistory = {
                            if (calculationResult != null) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val newItem = DiscountHistoryItem(
                                    mode = activeMode,
                                    originalPrice = calculationResult.originalPrice,
                                    finalPrice = calculationResult.finalPrice,
                                    savedAmount = calculationResult.totalSaved,
                                    discountPercent = calculationResult.effectiveDiscountPercent,
                                    currency = selectedCurrency
                                )
                                val updated = listOf(newItem) + historyItems.take(19)
                                saveHistory(updated)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("تم حفظ الحسبة في سجل التوفير 💾")
                                }
                            }
                        },
                        onReset = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            originalPriceText = "1000"
                            discountPercentText = "20"
                            extraDiscountText = "5"
                            vatPercentText = "14"
                            finalPaidPriceText = "800"
                            buyQuantityText = "2"
                            getQuantityText = "1"
                            itemUnitPriceText = "150"
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("تم إعادة ضبط الحقول بنجاح")
                            }
                        }
                    )
                }

                // 6. EXPANDABLE SECTION: HISTORY LOG
                item {
                    Column {
                        ExpandableDiscountHistoryHeader(
                            title = "سجل التوفير والحسابات السابقة (${historyItems.size})",
                            subtitle = "استعرض عمليات الخصم المحفوظة وأعد استخدامها بضغطة واحدة",
                            isExpanded = showHistoryDrawer,
                            onToggle = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showHistoryDrawer = !showHistoryDrawer
                            },
                            colors = colors
                        )

                        AnimatedVisibility(
                            visible = showHistoryDrawer,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            DiscountHistorySection(
                                historyItems = historyItems,
                                colors = colors,
                                onSelectItem = { item ->
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    originalPriceText = item.originalPrice.toLong().toString()
                                    discountPercentText = item.discountPercent.toString()
                                    activeMode = item.mode
                                    selectedCurrency = item.currency
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("تم تحميل بيانات الخصم من السجل ⚡")
                                    }
                                },
                                onToggleFavorite = { id ->
                                    val updated = historyItems.map {
                                        if (it.id == id) it.copy(isFavorite = !it.isFavorite) else it
                                    }
                                    saveHistory(updated)
                                },
                                onClearHistory = {
                                    saveHistory(emptyList())
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("تم مسح سجل التوفير بالكامل")
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Snackbar Host
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }
    }
}

// --- SUB-COMPOSABLES & ORGANISMS ---

@Composable
private fun DiscountHeaderSection(
    colors: CustomThemeColors,
    selectedCurrency: String,
    onCurrencyChange: (String) -> Unit
) {
    Surface(
        color = Color(0xFF141926).copy(alpha = 0.85f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Offline badge
                Surface(
                    color = Color(0xFF10B981).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Text(
                            text = "حاسبة التوفير تعمل أوفلاين",
                            fontSize = 11.sp,
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Currency selector
                val currencies = listOf("EGP", "SAR", "USD", "EUR", "AED", "KWD")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(currencies, key = { it }) { curr ->
                        val isSelected = curr == selectedCurrency
                        Surface(
                            color = if (isSelected) Color(0xFFD4AF37) else Color(0xFF1E2638),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFFF59E0B) else Color(0xFF334155)
                            ),
                            modifier = Modifier.clickable { onCurrencyChange(curr) }
                        ) {
                            Text(
                                text = curr,
                                color = if (isSelected) Color(0xFF0F1422) else Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // Title & Procedural Price Tag Icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFD4AF37), Color(0xFFF59E0B))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalOffer,
                        contentDescription = null,
                        tint = Color(0xFF0F1422),
                        modifier = Modifier.size(26.dp)
                    )
                }

                Column {
                    Text(
                        text = "حاسبة الخصم والتخفيض",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "وفر أموالك واحسب الصفقات والعروض التجارية بدقة",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeSelectorRow(
    activeMode: DiscountMode,
    onModeSelected: (DiscountMode) -> Unit,
    colors: CustomThemeColors
) {
    val modes = DiscountMode.values()
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        items(modes, key = { it.title }) { mode ->
            val isSelected = mode == activeMode
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) Color(0xFFD4AF37) else Color(0xFF141926).copy(alpha = 0.85f),
                border = BorderStroke(
                    1.dp,
                    if (isSelected) Color(0xFFF59E0B) else Color(0xFF334155)
                ),
                modifier = Modifier.clickable { onModeSelected(mode) }
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = mode.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color(0xFF0F1422) else Color.White
                    )
                    Text(
                        text = mode.subtitle,
                        fontSize = 9.sp,
                        color = if (isSelected) Color(0xFF0F1422).copy(alpha = 0.7f) else Color(0xFF94A3B8),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroDiscountResultCard(
    result: DiscountCalculationResult,
    currency: String,
    activeMode: DiscountMode,
    colors: CustomThemeColors
) {
    val animatedFinalPrice by animateFloatAsState(
        targetValue = result.finalPrice.toFloat(),
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "finalPrice"
    )

    Surface(
        color = Color(0xFF141926).copy(alpha = 0.90f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.5.dp, Color(0xFFD4AF37).copy(alpha = 0.5f)),
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "السعر النهائي بعد الخصم والتخفيض",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8)
            )

            // Final Price Display
            Text(
                text = formatCurrency(animatedFinalPrice.toDouble(), currency),
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFF59E0B)
            )

            // Original Price Strike-through & Savings Badge
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatCurrency(result.originalPrice, currency),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFEF4444),
                    textDecoration = TextDecoration.LineThrough
                )

                Surface(
                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "وفرت ${formatCurrency(result.totalSaved, currency)} (${String.format(Locale.US, "%.1f", result.effectiveDiscountPercent)}%)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Divider(color = Color(0xFF334155).copy(alpha = 0.5f))

            // Procedural Canvas Savings Progress Bar
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("نسبة التوفير من السعر الأصلي", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    Text("${String.format(Locale.US, "%.1f", result.effectiveDiscountPercent)}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                }

                val progressRatio = (result.totalSaved / max(1.0, result.originalPrice)).toFloat().coerceIn(0f, 1f)
                val animatedProgress by animateFloatAsState(
                    targetValue = progressRatio,
                    animationSpec = tween(700, easing = FastOutSlowInEasing),
                    label = "progress"
                )

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                ) {
                    // Background track
                    drawRect(color = Color(0xFF1E2638))
                    // Saved portion (Green)
                    drawRect(
                        color = Color(0xFF10B981),
                        size = Size(size.width * animatedProgress, size.height)
                    )
                }
            }

            // Detailed Breakdown Rows
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BreakdownRow("السعر الأصلي للمنتج", formatCurrency(result.originalPrice, currency), Color.White)
                if (result.primaryDiscountAmount > 0) {
                    BreakdownRow("قيمة الخصم الأساسي (${String.format(Locale.US, "%.1f", result.primaryDiscountPercent)}%)", "- " + formatCurrency(result.primaryDiscountAmount, currency), Color(0xFFEF4444))
                }
                if (result.extraDiscountAmount > 0) {
                    BreakdownRow("قيمة الخصم الإضافي / الكوبون", "- " + formatCurrency(result.extraDiscountAmount, currency), Color(0xFFEF4444))
                }
                if (result.taxAmount > 0) {
                    BreakdownRow("قيمة ضريبة القيمة المضافة (VAT)", "+ " + formatCurrency(result.taxAmount, currency), Color(0xFF3B82F6))
                }
                Divider(color = Color(0xFF334155).copy(alpha = 0.3f))
                BreakdownRow("إجمالي ما تم توفيره", formatCurrency(result.totalSaved, currency), Color(0xFF10B981), isBold = true)
            }
        }
    }
}

@Composable
private fun BreakdownRow(label: String, value: String, valueColor: Color, isBold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = Color(0xFF94A3B8))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = if (isBold) FontWeight.Black else FontWeight.Bold,
            color = valueColor
        )
    }
}

@Composable
private fun EmptyDiscountCard(
    originalPriceText: String,
    shakeOffset: Float,
    colors: CustomThemeColors,
    onPresetPrice: (Long) -> Unit
) {
    Surface(
        color = Color(0xFF141926).copy(alpha = 0.85f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .offset(x = shakeOffset.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = null,
                tint = Color(0xFFD4AF37),
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "أدخل السعر الأصلي للبدء في الحساب",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                text = "اختر من الأسعار الشائعة أدناه أو اكتب السعر يدوياً:",
                fontSize = 12.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )

            val presets = listOf(100L, 250L, 500L, 1000L, 2500L, 5000L)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(presets, key = { it }) { preset ->
                    Surface(
                        color = Color(0xFF1E2638),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier.clickable { onPresetPrice(preset) }
                    ) {
                        Text(
                            text = "$preset",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD4AF37),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscountInputSection(
    activeMode: DiscountMode,
    originalPriceText: String,
    onOriginalPriceChange: (String) -> Unit,
    discountPercentText: String,
    onDiscountPercentChange: (String) -> Unit,
    extraDiscountText: String,
    onExtraDiscountChange: (String) -> Unit,
    vatPercentText: String,
    onVatPercentChange: (String) -> Unit,
    finalPaidPriceText: String,
    onFinalPaidPriceChange: (String) -> Unit,
    buyQuantityText: String,
    onBuyQuantityChange: (String) -> Unit,
    getQuantityText: String,
    onGetQuantityChange: (String) -> Unit,
    itemUnitPriceText: String,
    onItemUnitPriceChange: (String) -> Unit,
    currency: String,
    shakeOffset: Float,
    colors: CustomThemeColors,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
    Surface(
        color = Color(0xFF141926).copy(alpha = 0.85f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .offset(x = shakeOffset.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "مدخلات التخفيض والأسعار",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            // 1. OFFERS MODE INPUTS
            if (activeMode == DiscountMode.OFFERS) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = buyQuantityText,
                        onValueChange = onBuyQuantityChange,
                        label = { Text("اشتر (X)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = discountTextFieldColors()
                    )
                    OutlinedTextField(
                        value = getQuantityText,
                        onValueChange = onGetQuantityChange,
                        label = { Text("احصل على (Y)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = discountTextFieldColors()
                    )
                }

                OutlinedTextField(
                    value = itemUnitPriceText,
                    onValueChange = onItemUnitPriceChange,
                    label = { Text("سعر الوحدة الواحدة ($currency)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = discountTextFieldColors()
                )
            } else if (activeMode == DiscountMode.REVERSE) {
                // REVERSE MODE INPUTS
                OutlinedTextField(
                    value = originalPriceText,
                    onValueChange = onOriginalPriceChange,
                    label = { Text("السعر الأصلي المقدر ($currency)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = discountTextFieldColors()
                )

                OutlinedTextField(
                    value = finalPaidPriceText,
                    onValueChange = onFinalPaidPriceChange,
                    label = { Text("السعر النهائي المدفوع ($currency)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = discountTextFieldColors()
                )
            } else {
                // STANDARD, DOUBLE, VAT MODE INPUTS
                OutlinedTextField(
                    value = originalPriceText,
                    onValueChange = onOriginalPriceChange,
                    label = { Text("السعر الأصلي ($currency)") },
                    trailingIcon = {
                        if (originalPriceText.isNotEmpty()) {
                            IconButton(onClick = { onOriginalPriceChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "مسح", tint = Color(0xFF94A3B8))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = discountTextFieldColors()
                )

                // Quick Preset Discount Chips
                val discountPresets = listOf(10, 15, 20, 25, 30, 50, 70)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(discountPresets, key = { it }) { disc ->
                        val isSelected = discountPercentText == disc.toString()
                        Surface(
                            color = if (isSelected) Color(0xFFD4AF37) else Color(0xFF1E2638),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFFF59E0B) else Color(0xFF334155)),
                            modifier = Modifier.clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDiscountPercentChange(disc.toString())
                            }
                        ) {
                            Text(
                                text = "$disc%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color(0xFF0F1422) else Color(0xFFD4AF37),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = discountPercentText,
                    onValueChange = onDiscountPercentChange,
                    label = { Text("نسبة الخصم الأساسية (%)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = discountTextFieldColors()
                )

                // Slider for discount percent
                val discNum = discountPercentText.toDoubleOrNull() ?: 0.0
                Slider(
                    value = discNum.toFloat().coerceIn(0f, 100f),
                    onValueChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDiscountPercentChange(it.roundToInt().toString())
                    },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFD4AF37),
                        activeTrackColor = Color(0xFFD4AF37),
                        inactiveTrackColor = Color(0xFF334155)
                    )
                )

                // Conditional Extra Coupon Input for Double Mode
                AnimatedVisibility(visible = activeMode == DiscountMode.DOUBLE) {
                    OutlinedTextField(
                        value = extraDiscountText,
                        onValueChange = onExtraDiscountChange,
                        label = { Text("نسبة الخصم الإضافي / الكوبون (%)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = discountTextFieldColors()
                    )
                }

                // Conditional VAT Input for VAT Mode
                AnimatedVisibility(visible = activeMode == DiscountMode.VAT) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val vatPresets = listOf(5, 14, 15, 20)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(vatPresets, key = { it }) { vat ->
                                Surface(
                                    color = Color(0xFF1E2638),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(0xFF334155)),
                                    modifier = Modifier.clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onVatPercentChange(vat.toString())
                                    }
                                ) {
                                    Text(
                                        text = "$vat% ضريبة",
                                        fontSize = 11.sp,
                                        color = Color(0xFF3B82F6),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = vatPercentText,
                            onValueChange = onVatPercentChange,
                            label = { Text("نسبة ضريبة القيمة المضافة (%)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = discountTextFieldColors()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun discountTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color(0xFFD4AF37),
    unfocusedBorderColor = Color(0xFF334155),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedLabelColor = Color(0xFFD4AF37),
    unfocusedLabelColor = Color(0xFF94A3B8)
)

@Composable
private fun DiscountActionButtonsRow(
    result: DiscountCalculationResult?,
    currency: String,
    colors: CustomThemeColors,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onSaveHistory: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onCopy,
            enabled = result != null,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2638), contentColor = Color(0xFFD4AF37))
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("نسخ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        Button(
            onClick = onShare,
            enabled = result != null,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2638), contentColor = Color(0xFF3B82F6))
        ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("مشاركة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        Button(
            onClick = onSaveHistory,
            enabled = result != null,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37), contentColor = Color(0xFF0F1422))
        ) {
            Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("حفظ", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        }

        Button(
            onClick = onReset,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2638), contentColor = Color(0xFFEF4444))
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("إعادة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ExpandableDiscountHistoryHeader(
    title: String,
    subtitle: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    colors: CustomThemeColors
) {
    Surface(
        color = Color(0xFF141926).copy(alpha = 0.85f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E2638)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = Color(0xFFD4AF37),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(text = subtitle, fontSize = 11.sp, color = Color(0xFF94A3B8))
                }
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
private fun DiscountHistorySection(
    historyItems: List<DiscountHistoryItem>,
    colors: CustomThemeColors,
    onSelectItem: (DiscountHistoryItem) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onClearHistory: () -> Unit
) {
    Surface(
        color = Color(0xFF141926).copy(alpha = 0.90f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "العمليات المحفوظة (${historyItems.size})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (historyItems.isNotEmpty()) {
                    TextButton(onClick = onClearHistory) {
                        Text("مسح الكل", fontSize = 11.sp, color = Color(0xFFEF4444))
                    }
                }
            }

            if (historyItems.isEmpty()) {
                Text(
                    text = "لا توجد عمليات محفوظة حتى الآن",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(vertical = 16.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    historyItems.forEach { item ->
                        Surface(
                            color = Color(0xFF1E2638),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectItem(item) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = item.mode.title,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFD4AF37)
                                        )
                                        Text(
                                            text = "• ${String.format(Locale.US, "%.1f", item.discountPercent)}% خصم",
                                            fontSize = 11.sp,
                                            color = Color(0xFF10B981)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "السعر الأصلي: ${formatCurrency(item.originalPrice, item.currency)} ⬅️ النهائي: ${formatCurrency(item.finalPrice, item.currency)}",
                                        fontSize = 11.sp,
                                        color = Color.White
                                    )
                                }

                                IconButton(onClick = { onToggleFavorite(item.id) }) {
                                    Icon(
                                        imageVector = if (item.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = "مفضل",
                                        tint = if (item.isFavorite) Color(0xFFF59E0B) else Color(0xFF94A3B8)
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

// --- UTILITY FUNCTIONS ---

private fun formatCurrency(amount: Double, currency: String): String {
    return try {
        val formatter = java.text.NumberFormat.getInstance(Locale("ar", "EG")).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 0
        }
        "${formatter.format(amount)} $currency"
    } catch (e: Exception) {
        "${amount.roundToInt()} $currency"
    }
}

private fun parseDiscountHistory(jsonStr: String): List<DiscountHistoryItem> {
    val list = mutableListOf<DiscountHistoryItem>()
    try {
        val jsonArray = JSONArray(jsonStr)
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val modeName = obj.optString("mode", DiscountMode.SIMPLE.name)
            val mode = try { DiscountMode.valueOf(modeName) } catch (e: Exception) { DiscountMode.SIMPLE }
            list.add(
                DiscountHistoryItem(
                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                    mode = mode,
                    originalPrice = obj.optDouble("originalPrice", 0.0),
                    finalPrice = obj.optDouble("finalPrice", 0.0),
                    savedAmount = obj.optDouble("savedAmount", 0.0),
                    discountPercent = obj.optDouble("discountPercent", 0.0),
                    currency = obj.optString("currency", "EGP"),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                    isFavorite = obj.optBoolean("isFavorite", false)
                )
            )
        }
    } catch (e: Exception) {
        // fallback
    }
    return list
}

private fun serializeDiscountHistory(items: List<DiscountHistoryItem>): String {
    val jsonArray = JSONArray()
    try {
        for (item in items) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("mode", item.mode.name)
                put("originalPrice", item.originalPrice)
                put("finalPrice", item.finalPrice)
                put("savedAmount", item.savedAmount)
                put("discountPercent", item.discountPercent)
                put("currency", item.currency)
                put("timestamp", item.timestamp)
                put("isFavorite", item.isFavorite)
            }
            jsonArray.put(obj)
        }
    } catch (e: Exception) {
        // fallback
    }
    return jsonArray.toString()
}
