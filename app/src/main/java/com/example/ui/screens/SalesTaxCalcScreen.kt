package com.example.ui.screens

import android.content.Context
import android.content.Intent
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
private val ColorSlateMuted = Color(0xFF94A3B8)

enum class VatCalculationMode(
    val titleAr: String,
    val subtitleAr: String,
    val inputLabelAr: String,
    val outputLabelAr: String
) {
    EXCLUSIVE_TO_INCLUSIVE(
        titleAr = "إضافة الضريبة (+)",
        subtitleAr = "حساب الإجمالي الشامل للضريبة من المبلغ الخالي منها",
        inputLabelAr = "المبلغ غير شامل الضريبة (الصافي)",
        outputLabelAr = "الإجمالي شامل الضريبة"
    ),
    INCLUSIVE_TO_EXCLUSIVE(
        titleAr = "استخراج الضريبة (-)",
        subtitleAr = "حساب المبلغ الصافي وقيمة الضريبة المستقطعة من الإجمالي",
        inputLabelAr = "المبلغ الإجمالي (شامل الضريبة)",
        outputLabelAr = "المبلغ الصافي قبل الضريبة"
    )
}

data class VatBasketItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val priceStr: String = "100",
    val quantityStr: String = "1"
)

data class VatCalcResult(
    val baseAmount: BigDecimal,
    val discountAmount: BigDecimal,
    val taxableBase: BigDecimal,
    val taxValue: BigDecimal,
    val totalWithTax: BigDecimal,
    val effectiveTaxRate: BigDecimal,
    val isForwardMode: Boolean,
    val isValid: Boolean,
    val errorMessage: String? = null,
    val basketTotalNet: BigDecimal = BigDecimal.ZERO,
    val basketTotalTax: BigDecimal = BigDecimal.ZERO,
    val basketGrandTotal: BigDecimal = BigDecimal.ZERO
)

data class VatHistoryItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: String,
    val modeName: String,
    val modeTitle: String,
    val baseAmountStr: String,
    val taxRateStr: String,
    val discountRateStr: String,
    val taxValueStr: String,
    val totalAmountStr: String,
    val currency: String
)

// ==========================================
// MAIN COMPOSABLE SCREEN
// ==========================================

@Composable
fun SalesTaxCalcScreen(colors: CustomThemeColors) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current

    // State Retention across Configuration Changes
    var activeMode by rememberSaveable { mutableStateOf(VatCalculationMode.EXCLUSIVE_TO_INCLUSIVE) }
    var amountStr by rememberSaveable { mutableStateOf("500") }
    var taxRateStr by rememberSaveable { mutableStateOf("14") }
    var discountRateStr by rememberSaveable { mutableStateOf("0") }
    var isDiscountEnabled by rememberSaveable { mutableStateOf(false) }
    var isBasketModeEnabled by rememberSaveable { mutableStateOf(false) }
    var selectedCurrency by rememberSaveable { mutableStateOf("EGP") }

    // Multi-Item Basket List
    var basketItems by remember {
        mutableStateOf(
            listOf(
                VatBasketItem(name = "المنتج الأوّل", priceStr = "200", quantityStr = "1"),
                VatBasketItem(name = "المنتج الثاني", priceStr = "300", quantityStr = "1")
            )
        )
    }

    // Expandable Sections
    var isBreakdownExpanded by rememberSaveable { mutableStateOf(true) }
    var isFavoritesExpanded by rememberSaveable { mutableStateOf(false) }
    var isHistoryExpanded by rememberSaveable { mutableStateOf(false) }

    // Toast Feedback Banner Message
    var toastMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2500)
            toastMessage = null
        }
    }

    // Local Persistence Data
    var historyList by remember { mutableStateOf(loadVatHistoryFromPrefs(context)) }
    var favoritesList by remember { mutableStateOf(loadVatFavoritesFromPrefs(context)) }

    // Derived High-Precision Calculation Engine
    val calculationResult by remember(
        activeMode, amountStr, taxRateStr, discountRateStr, isDiscountEnabled,
        isBasketModeEnabled, basketItems
    ) {
        derivedStateOf {
            calculateVatEngine(
                mode = activeMode,
                amountInput = amountStr,
                taxRateInput = taxRateStr,
                discountRateInput = if (isDiscountEnabled) discountRateStr else "0",
                isBasketEnabled = isBasketModeEnabled,
                basketList = basketItems
            )
        }
    }

    // Shaking Animation offset for error state
    val shakeOffset by animateFloatAsState(
        targetValue = if (!calculationResult.isValid) 10f else 0f,
        animationSpec = repeatable(
            iterations = 4,
            animation = tween(durationMillis = 50, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shakeOffset"
    )

    ToolScreenScaffold(
        colors = colors,
        icon = AppIcons.forCalc(CalcKey.SALES_TAX),
        title = "حاسبة ضريبة المبيعات (VAT)",
        subtitle = "حساب ضريبة القيمة المضافة، الخصومات والمبلغ الشامل وغير الشامل للضريبة فوراً",
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
            // Background Canvas Grid Pattern with Tax Receipt / Vault Graphics
            Canvas(modifier = Modifier.fillMaxSize()) {
                val step = 70.dp.toPx()
                val linePaint = ColorGoldBorder.copy(alpha = 0.025f)
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

                // SECTION A1: Header Card
                item(key = "header_section") {
                    HeaderCard(
                        selectedCurrency = selectedCurrency,
                        onCurrencySelected = {
                            selectedCurrency = it
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    )
                }

                // SECTION A2: Calculation Mode Segmented Switcher
                item(key = "mode_segmented_control") {
                    ModeSegmentedControl(
                        activeMode = activeMode,
                        onModeSelected = { mode ->
                            activeMode = mode
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    )
                }

                // SECTION B1: Interactive Inputs Panel (Glassmorphic Container)
                item(key = "inputs_panel") {
                    Box(modifier = Modifier.offset(x = shakeOffset.dp)) {
                        InputsPanel(
                            activeMode = activeMode,
                            amountStr = amountStr,
                            onAmountChange = { amountStr = it },
                            taxRateStr = taxRateStr,
                            onTaxRateChange = { taxRateStr = it },
                            discountRateStr = discountRateStr,
                            onDiscountRateChange = { discountRateStr = it },
                            isDiscountEnabled = isDiscountEnabled,
                            onToggleDiscount = { isDiscountEnabled = !isDiscountEnabled },
                            isBasketEnabled = isBasketModeEnabled,
                            onToggleBasket = { isBasketModeEnabled = !isBasketModeEnabled },
                            basketItems = basketItems,
                            onBasketItemsChange = { basketItems = it },
                            currency = selectedCurrency
                        )
                    }
                }

                // SECTION B2: Live Result Display Panel
                item(key = "result_display_panel") {
                    ResultDisplayPanel(
                        result = calculationResult,
                        activeMode = activeMode,
                        currency = selectedCurrency,
                        onSaveHistory = {
                            if (!calculationResult.isValid) return@ResultDisplayPanel
                            val timeStr = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm - yyyy/MM/dd"))
                            val newItem = VatHistoryItem(
                                timestamp = timeStr,
                                modeName = activeMode.name,
                                modeTitle = activeMode.titleAr,
                                baseAmountStr = calculationResult.baseAmount.toPlainString(),
                                taxRateStr = taxRateStr,
                                discountRateStr = if (isDiscountEnabled) discountRateStr else "0",
                                taxValueStr = calculationResult.taxValue.toPlainString(),
                                totalAmountStr = calculationResult.totalWithTax.toPlainString(),
                                currency = selectedCurrency
                            )
                            historyList = listOf(newItem) + historyList.take(19)
                            saveVatHistoryToPrefs(context, historyList)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            toastMessage = "تم حفظ العملية في السجل"
                        },
                        onSaveFavorite = {
                            if (!calculationResult.isValid) return@ResultDisplayPanel
                            val timeStr = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm - yyyy/MM/dd"))
                            val newItem = VatHistoryItem(
                                timestamp = timeStr,
                                modeName = activeMode.name,
                                modeTitle = activeMode.titleAr,
                                baseAmountStr = calculationResult.baseAmount.toPlainString(),
                                taxRateStr = taxRateStr,
                                discountRateStr = if (isDiscountEnabled) discountRateStr else "0",
                                taxValueStr = calculationResult.taxValue.toPlainString(),
                                totalAmountStr = calculationResult.totalWithTax.toPlainString(),
                                currency = selectedCurrency
                            )
                            favoritesList = listOf(newItem) + favoritesList
                            saveVatFavoritesToPrefs(context, favoritesList)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            toastMessage = "تمت إضافة العملية إلى المفضلة ⭐"
                        },
                        onCopyReport = {
                            val shareText = formatVatReport(calculationResult, activeMode, selectedCurrency, taxRateStr)
                            clipboardManager.setText(AnnotatedString(shareText))
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            toastMessage = "تم نسخ تقرير الضريبة للحافظة 📋"
                        },
                        onShare = {
                            val shareText = formatVatReport(calculationResult, activeMode, selectedCurrency, taxRateStr)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(intent, "مشاركة تقرير الضريبة"))
                        },
                        onReset = {
                            amountStr = "500"
                            taxRateStr = "14"
                            discountRateStr = "0"
                            isDiscountEnabled = false
                            isBasketModeEnabled = false
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            toastMessage = "تم إعادة ضبط المدخلات"
                        }
                    )
                }

                // SECTION C: Detailed Tax Breakdown Card
                item(key = "tax_breakdown_section") {
                    ExpandableCard(
                        title = "تفاصيل الحسبة الضريبية والخصم",
                        icon = Icons.Outlined.ReceiptLong,
                        isExpanded = isBreakdownExpanded,
                        onToggle = { isBreakdownExpanded = !isBreakdownExpanded }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("• المبلغ الأصلي المدخل:", fontSize = 12.sp, color = ColorSlateMuted)
                                Text("${calculationResult.baseAmount.toPlainString()} $selectedCurrency", fontSize = 12.sp, color = Color.White)
                            }

                            if (calculationResult.discountAmount > BigDecimal.ZERO) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("• قيمة الخصم المطبق:", fontSize = 12.sp, color = ColorSlateMuted)
                                    Text("-${calculationResult.discountAmount.toPlainString()} $selectedCurrency", fontSize = 12.sp, color = ColorEmeraldGreen, fontWeight = FontWeight.Bold)
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("• المبلغ الخاضع للضريبة بعد الخصم:", fontSize = 12.sp, color = ColorSlateMuted)
                                    Text("${calculationResult.taxableBase.toPlainString()} $selectedCurrency", fontSize = 12.sp, color = ColorAmberGlow)
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("• قيمة ضريبة القيمة المضافة ($taxRateStr%):", fontSize = 12.sp, color = ColorSlateMuted)
                                Text("${calculationResult.taxValue.toPlainString()} $selectedCurrency", fontSize = 12.sp, color = ColorIceCyan, fontWeight = FontWeight.Bold)
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("• الإجمالي الشامل للضريبة والخصم:", fontSize = 13.sp, color = ColorAmberGlow, fontWeight = FontWeight.Bold)
                                Text("${calculationResult.totalWithTax.toPlainString()} $selectedCurrency", fontSize = 13.sp, color = ColorAmberGlow, fontWeight = FontWeight.Bold)
                            }

                            if (isBasketModeEnabled && calculationResult.basketGrandTotal > BigDecimal.ZERO) {
                                Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

                                Text("تفاصيل سلة المنتجات المتعددة:", fontSize = 12.sp, color = ColorAmberGlow, fontWeight = FontWeight.Bold)

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("• إجمالي صافي المنتجات:", fontSize = 12.sp, color = ColorSlateMuted)
                                    Text("${calculationResult.basketTotalNet.toPlainString()} $selectedCurrency", fontSize = 12.sp, color = Color.White)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("• إجمالي الضريبة على السلة:", fontSize = 12.sp, color = ColorSlateMuted)
                                    Text("${calculationResult.basketTotalTax.toPlainString()} $selectedCurrency", fontSize = 12.sp, color = ColorIceCyan, fontWeight = FontWeight.Bold)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("• الإجمالي النهائي للسلة:", fontSize = 12.sp, color = ColorEmeraldGreen, fontWeight = FontWeight.Bold)
                                    Text("${calculationResult.basketGrandTotal.toPlainString()} $selectedCurrency", fontSize = 12.sp, color = ColorEmeraldGreen, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // SECTION D: Favorites List
                if (favoritesList.isNotEmpty()) {
                    item(key = "favorites_section") {
                        ExpandableCard(
                            title = "العمليات الضريبية المفضلة (${favoritesList.size})",
                            icon = Icons.Outlined.Star,
                            isExpanded = isFavoritesExpanded,
                            onToggle = { isFavoritesExpanded = !isFavoritesExpanded }
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                favoritesList.forEach { fav ->
                                    HistoryItemRow(
                                        item = fav,
                                        onReuse = {
                                            amountStr = fav.baseAmountStr
                                            taxRateStr = fav.taxRateStr
                                            discountRateStr = fav.discountRateStr
                                            selectedCurrency = fav.currency
                                            activeMode = VatCalculationMode.values().firstOrNull { it.name == fav.modeName } ?: activeMode
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            toastMessage = "تم تطبيق بيانات المفضلة"
                                        },
                                        onDelete = {
                                            favoritesList = favoritesList.filter { it.id != fav.id }
                                            saveVatFavoritesToPrefs(context, favoritesList)
                                            toastMessage = "تم حذف العنصر من المفضلة"
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // SECTION E: History Log
                if (historyList.isNotEmpty()) {
                    item(key = "history_section") {
                        ExpandableCard(
                            title = "سجل العمليات الضريبية السابقة (${historyList.size})",
                            icon = Icons.Outlined.History,
                            isExpanded = isHistoryExpanded,
                            onToggle = { isHistoryExpanded = !isHistoryExpanded }
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                historyList.forEach { hist ->
                                    HistoryItemRow(
                                        item = hist,
                                        onReuse = {
                                            amountStr = hist.baseAmountStr
                                            taxRateStr = hist.taxRateStr
                                            discountRateStr = hist.discountRateStr
                                            selectedCurrency = hist.currency
                                            activeMode = VatCalculationMode.values().firstOrNull { it.name == hist.modeName } ?: activeMode
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            toastMessage = "تم استرجاع العملية السابقة"
                                        },
                                        onDelete = {
                                            historyList = historyList.filter { it.id != hist.id }
                                            saveVatHistoryToPrefs(context, historyList)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Offline Notice Footer
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
                        Text("جميع الحسابات تعمل دون اتصال بمحرك BigDecimal بدقة متناهية", fontSize = 11.sp, color = ColorSlateMuted)
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
            // Procedural Canvas VAT / Tax Vault Icon with Pulse Glow
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

                    // Tax Tag / Shield outline
                    val shieldPath = Path().apply {
                        moveTo(w * 0.5f, h * 0.1f)
                        lineTo(w * 0.85f, h * 0.25f)
                        lineTo(w * 0.85f, h * 0.6f)
                        quadraticTo(w * 0.5f, h * 0.9f, w * 0.5f, h * 0.9f)
                        quadraticTo(w * 0.5f, h * 0.9f, w * 0.15f, h * 0.6f)
                        lineTo(w * 0.15f, h * 0.25f)
                        close()
                    }
                    drawPath(shieldPath, color = ColorGoldBorder, style = Stroke(width = 3f))

                    // Inner % slash
                    drawLine(ColorIceCyan, Offset(w * 0.35f, h * 0.65f), Offset(w * 0.65f, h * 0.35f), strokeWidth = 2.5f)
                    drawCircle(ColorIceCyan, radius = 3.5f, center = Offset(w * 0.4f, h * 0.4f))
                    drawCircle(ColorIceCyan, radius = 3.5f, center = Offset(w * 0.6f, h * 0.6f))
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "حاسبة ضريبة المبيعات",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "حساب القيمة المضافة، الخصم والإجمالي",
                    fontSize = 11.sp,
                    color = ColorSlateMuted
                )
            }

            // Currency Selector Chips
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("EGP", "SAR", "USD", "EUR").forEach { curr ->
                    val isSelected = curr == selectedCurrency
                    Surface(
                        color = if (isSelected) ColorAmberGlow.copy(alpha = 0.25f) else Color(0xFF1E2638),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (isSelected) ColorAmberGlow else Color.White.copy(alpha = 0.15f)),
                        modifier = Modifier.clickable { onCurrencySelected(curr) }
                    ) {
                        Text(
                            curr,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) ColorAmberGlow else ColorSlateMuted,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeSegmentedControl(
    activeMode: VatCalculationMode,
    onModeSelected: (VatCalculationMode) -> Unit
) {
    Surface(
        color = ColorGlassCard,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, ColorGoldBorder.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            VatCalculationMode.values().forEach { mode ->
                val isSelected = mode == activeMode
                Surface(
                    color = if (isSelected) ColorAmberGlow.copy(alpha = 0.25f) else Color.Transparent,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, if (isSelected) ColorAmberGlow else Color.Transparent),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onModeSelected(mode) }
                ) {
                    Text(
                        mode.titleAr,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else ColorSlateMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun InputsPanel(
    activeMode: VatCalculationMode,
    amountStr: String,
    onAmountChange: (String) -> Unit,
    taxRateStr: String,
    onTaxRateChange: (String) -> Unit,
    discountRateStr: String,
    onDiscountRateChange: (String) -> Unit,
    isDiscountEnabled: Boolean,
    onToggleDiscount: () -> Unit,
    isBasketEnabled: Boolean,
    onToggleBasket: () -> Unit,
    basketItems: List<VatBasketItem>,
    onBasketItemsChange: (List<VatBasketItem>) -> Unit,
    currency: String
) {
    Surface(
        color = ColorGlassCard,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, ColorGoldBorder.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                activeMode.subtitleAr,
                fontSize = 12.sp,
                color = ColorAmberGlow,
                fontWeight = FontWeight.SemiBold
            )

            // Amount Field
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(activeMode.inputLabelAr + " ($currency)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = onAmountChange,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        if (amountStr.isNotEmpty()) {
                            IconButton(onClick = { onAmountChange("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "مسح", tint = ColorSlateMuted, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ColorAmberGlow,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }

            // Tax Rate Selector + Slider + Quick Chips
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("نسبة ضريبة القيمة المضافة (%)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    OutlinedTextField(
                        value = taxRateStr,
                        onValueChange = onTaxRateChange,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.width(100.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ColorIceCyan,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                // Quick Tax Rate Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "5" to "خليجي مخفض",
                        "10" to "مخفض",
                        "14" to "مصر 🇪🇬",
                        "15" to "السعودية 🇸🇦",
                        "20" to "أوروبا 🇪🇺"
                    ).forEach { (rate, label) ->
                        val isSelected = taxRateStr == rate
                        Surface(
                            color = if (isSelected) ColorIceCyan.copy(alpha = 0.25f) else Color(0xFF1E2638),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (isSelected) ColorIceCyan else Color.White.copy(alpha = 0.15f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onTaxRateChange(rate) }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "$rate%",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) ColorIceCyan else Color.White
                                )
                                Text(
                                    label,
                                    fontSize = 8.sp,
                                    color = ColorSlateMuted
                                )
                            }
                        }
                    }
                }
            }

            // Optional Pre-Tax Discount Toggle & Field
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = isDiscountEnabled,
                        onCheckedChange = { onToggleDiscount() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ColorEmeraldGreen,
                            checkedTrackColor = ColorEmeraldGreen.copy(alpha = 0.3f)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تطبيق خصم قبل الضريبة (%)", color = Color.White, fontSize = 12.sp)
                }

                if (isDiscountEnabled) {
                    OutlinedTextField(
                        value = discountRateStr,
                        onValueChange = onDiscountRateChange,
                        label = { Text("الخصم %", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.width(90.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ColorEmeraldGreen,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            }

            // Multi-Item Basket Toggle & List
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = isBasketEnabled,
                        onCheckedChange = { onToggleBasket() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ColorAmberGlow,
                            checkedTrackColor = ColorAmberGlow.copy(alpha = 0.3f)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("سلة منتجات متعددة (تجميع الفواتير)", color = Color.White, fontSize = 12.sp)
                }
            }

            if (isBasketEnabled) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("عناصر السلة:", fontSize = 12.sp, color = ColorAmberGlow, fontWeight = FontWeight.Bold)

                        IconButton(onClick = {
                            val nextNum = basketItems.size + 1
                            onBasketItemsChange(basketItems + VatBasketItem(name = "منتج $nextNum", priceStr = "100", quantityStr = "1"))
                        }) {
                            Icon(Icons.Filled.AddCircle, contentDescription = "إضافة عنصر", tint = ColorEmeraldGreen)
                        }
                    }

                    basketItems.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = item.name,
                                onValueChange = { newName ->
                                    val list = basketItems.toMutableList()
                                    list[index] = item.copy(name = newName)
                                    onBasketItemsChange(list)
                                },
                                label = { Text("الاسم", fontSize = 10.sp) },
                                modifier = Modifier.weight(1.2f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ColorGoldBorder,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            OutlinedTextField(
                                value = item.priceStr,
                                onValueChange = { newPrice ->
                                    val list = basketItems.toMutableList()
                                    list[index] = item.copy(priceStr = newPrice)
                                    onBasketItemsChange(list)
                                },
                                label = { Text("السعر", fontSize = 10.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ColorGoldBorder,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            OutlinedTextField(
                                value = item.quantityStr,
                                onValueChange = { newQty ->
                                    val list = basketItems.toMutableList()
                                    list[index] = item.copy(quantityStr = newQty)
                                    onBasketItemsChange(list)
                                },
                                label = { Text("الكمية", fontSize = 10.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.width(65.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ColorGoldBorder,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            if (basketItems.size > 1) {
                                IconButton(onClick = {
                                    onBasketItemsChange(basketItems.filterIndexed { i, _ -> i != index })
                                }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "حذف", tint = ColorCrimsonRed, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultDisplayPanel(
    result: VatCalcResult,
    activeMode: VatCalculationMode,
    currency: String,
    onSaveHistory: () -> Unit,
    onSaveFavorite: () -> Unit,
    onCopyReport: () -> Unit,
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
        border = BorderStroke(1.5.dp, ColorGoldBorder.copy(alpha = glowAlpha)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!result.isValid) {
                // Error State Box
                Surface(
                    color = ColorCrimsonRed.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, ColorCrimsonRed)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = ColorCrimsonRed)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            result.errorMessage ?: "خطأ في أرقام المدخلات",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                // Main Highlight Output Metric
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        activeMode.outputLabelAr,
                        fontSize = 12.sp,
                        color = ColorSlateMuted
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${result.totalWithTax.toPlainString()} $currency",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                        color = ColorAmberGlow
                    )
                }

                // VAT Amount Badge & Net Base
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("مقدار الضريبة (VAT)", fontSize = 11.sp, color = ColorSlateMuted)
                        Surface(
                            color = ColorIceCyan.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, ColorIceCyan)
                        ) {
                            Text(
                                "+${result.taxValue.toPlainString()} $currency",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorIceCyan,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("المبلغ الصافي قبل الضريبة", fontSize = 11.sp, color = ColorSlateMuted)
                        Text(
                            "${result.baseAmount.toPlainString()} $currency",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Visual Tax Ratio Bar
                VisualTaxBar(
                    netBase = result.taxableBase,
                    taxVal = result.taxValue
                )

                // Quick Action Toolbar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onCopyReport,
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF1E2638), RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "نسخ التقرير", tint = ColorIceCyan)
                    }

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
}

@Composable
private fun VisualTaxBar(
    netBase: BigDecimal,
    taxVal: BigDecimal
) {
    val total = netBase + taxVal
    val netPercent = if (total > BigDecimal.ZERO) {
        netBase.multiply(BigDecimal("100")).divide(total, 1, RoundingMode.HALF_UP).toFloat()
    } else 86f

    val taxPercent = 100f - netPercent

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("النسبة من الإجمالي: الصافي (${netPercent.toInt()}%)", fontSize = 10.sp, color = ColorSlateMuted)
            Text("الضريبة (${taxPercent.toInt()}%)", fontSize = 10.sp, color = ColorIceCyan)
        }

        ClipProgressGraphic(netRatio = netPercent / 100f)
    }
}

@Composable
private fun ClipProgressGraphic(netRatio: Float) {
    val animatedRatio by animateFloatAsState(
        targetValue = netRatio.coerceIn(0.1f, 0.95f),
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "netRatioAnim"
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
    ) {
        val w = size.width
        val h = size.height
        val netWidth = w * animatedRatio

        // Base Net portion
        drawRect(
            color = ColorGoldBorder,
            size = Size(netWidth, h)
        )

        // Tax portion
        drawRect(
            color = ColorIceCyan,
            topLeft = Offset(netWidth, 0f),
            size = Size(w - netWidth, h)
        )
    }
}

@Composable
private fun ExpandableCard(
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
                    if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = ColorSlateMuted
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun HistoryItemRow(
    item: VatHistoryItem,
    onReuse: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = Color.Black.copy(alpha = 0.3f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.modeTitle, color = ColorAmberGlow, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(
                    "الصافي: ${item.baseAmountStr} | الضريبة (${item.taxRateStr}%): ${item.taxValueStr} ← الإجمالي: ${item.totalAmountStr} ${item.currency}",
                    color = Color.White,
                    fontSize = 10.sp
                )
                Text(item.timestamp, color = ColorSlateMuted, fontSize = 9.sp)
            }

            Row {
                IconButton(onClick = onReuse) {
                    Icon(Icons.Filled.Input, contentDescription = "تطبيق", tint = ColorIceCyan, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "حذف", tint = ColorCrimsonRed, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// ==========================================
// MATHEMATICAL ENGINE (BIGDECIMAL)
// ==========================================

private fun calculateVatEngine(
    mode: VatCalculationMode,
    amountInput: String,
    taxRateInput: String,
    discountRateInput: String,
    isBasketEnabled: Boolean,
    basketList: List<VatBasketItem>
): VatCalcResult {
    val amount = amountInput.toBigDecimalOrNull()
    val rate = taxRateInput.toBigDecimalOrNull()
    val discountRate = discountRateInput.toBigDecimalOrNull() ?: BigDecimal.ZERO

    if (amount == null || rate == null || amount < BigDecimal.ZERO || rate < BigDecimal.ZERO) {
        return VatCalcResult(
            baseAmount = BigDecimal.ZERO,
            discountAmount = BigDecimal.ZERO,
            taxableBase = BigDecimal.ZERO,
            taxValue = BigDecimal.ZERO,
            totalWithTax = BigDecimal.ZERO,
            effectiveTaxRate = BigDecimal.ZERO,
            isForwardMode = mode == VatCalculationMode.EXCLUSIVE_TO_INCLUSIVE,
            isValid = false,
            errorMessage = "يرجى إدخال مبلغ ونسبة ضريبة صحيحة"
        )
    }

    var basketNet = BigDecimal.ZERO
    var basketTax = BigDecimal.ZERO
    var basketGrand = BigDecimal.ZERO

    if (isBasketEnabled && basketList.isNotEmpty()) {
        basketList.forEach { b ->
            val p = b.priceStr.toBigDecimalOrNull() ?: BigDecimal.ZERO
            val q = b.quantityStr.toBigDecimalOrNull() ?: BigDecimal.ONE
            val itemNet = p.multiply(q)
            val itemTax = itemNet.multiply(rate).divide(BigDecimal("100"), 2, RoundingMode.HALF_UP)
            basketNet = basketNet.add(itemNet)
            basketTax = basketTax.add(itemTax)
        }
        basketGrand = basketNet.add(basketTax)
    }

    val hundred = BigDecimal("100")

    return when (mode) {
        VatCalculationMode.EXCLUSIVE_TO_INCLUSIVE -> {
            // Base -> Total
            val discountVal = amount.multiply(discountRate).divide(hundred, 2, RoundingMode.HALF_UP)
            val taxable = amount.subtract(discountVal)
            val taxVal = taxable.multiply(rate).divide(hundred, 2, RoundingMode.HALF_UP)
            val grand = taxable.add(taxVal)

            VatCalcResult(
                baseAmount = amount.setScale(2, RoundingMode.HALF_UP),
                discountAmount = discountVal,
                taxableBase = taxable,
                taxValue = taxVal,
                totalWithTax = grand,
                effectiveTaxRate = rate,
                isForwardMode = true,
                isValid = true,
                basketTotalNet = basketNet,
                basketTotalTax = basketTax,
                basketGrandTotal = basketGrand
            )
        }

        VatCalculationMode.INCLUSIVE_TO_EXCLUSIVE -> {
            // Total -> Base
            val divisor = BigDecimal.ONE.add(rate.divide(hundred, 6, RoundingMode.HALF_UP))
            val baseBeforeTax = amount.divide(divisor, 2, RoundingMode.HALF_UP)
            val taxVal = amount.subtract(baseBeforeTax)
            val discountVal = baseBeforeTax.multiply(discountRate).divide(hundred, 2, RoundingMode.HALF_UP)
            val netBase = baseBeforeTax.subtract(discountVal)

            VatCalcResult(
                baseAmount = baseBeforeTax,
                discountAmount = discountVal,
                taxableBase = netBase,
                taxValue = taxVal,
                totalWithTax = amount.setScale(2, RoundingMode.HALF_UP),
                effectiveTaxRate = rate,
                isForwardMode = false,
                isValid = true,
                basketTotalNet = basketNet,
                basketTotalTax = basketTax,
                basketGrandTotal = basketGrand
            )
        }
    }
}

private fun formatVatReport(
    result: VatCalcResult,
    mode: VatCalculationMode,
    currency: String,
    taxRateStr: String
): String {
    return "🧾 تقرير حاسبة ضريبة المبيعات (${mode.titleAr}):\n" +
            "• المبلغ الصافي الأصلي: ${result.baseAmount.toPlainString()} $currency\n" +
            (if (result.discountAmount > BigDecimal.ZERO) "• الخصم المطبق: -${result.discountAmount.toPlainString()} $currency\n" else "") +
            "• نسبة الضريبة: $taxRateStr%\n" +
            "• قيمة الضريبة (VAT): ${result.taxValue.toPlainString()} $currency\n" +
            "• الإجمالي النهائي: ${result.totalWithTax.toPlainString()} $currency\n" +
            "محسوب بواسطة حاسبة ضريبة المبيعات الذكية"
}

// ==========================================
// PREFERENCES PERSISTENCE HELPERS
// ==========================================

private const val VAT_PREFS_NAME = "vat_calc_prefs"

private fun saveVatHistoryToPrefs(context: Context, list: List<VatHistoryItem>) {
    try {
        val prefs = context.getSharedPreferences(VAT_PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        list.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("timestamp", item.timestamp)
                put("modeName", item.modeName)
                put("modeTitle", item.modeTitle)
                put("baseAmountStr", item.baseAmountStr)
                put("taxRateStr", item.taxRateStr)
                put("discountRateStr", item.discountRateStr)
                put("taxValueStr", item.taxValueStr)
                put("totalAmountStr", item.totalAmountStr)
                put("currency", item.currency)
            }
            arr.put(obj)
        }
        prefs.edit().putString("history_json", arr.toString()).apply()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun loadVatHistoryFromPrefs(context: Context): List<VatHistoryItem> {
    val list = mutableListOf<VatHistoryItem>()
    try {
        val prefs = context.getSharedPreferences(VAT_PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("history_json", null) ?: return emptyList()
        val arr = JSONArray(jsonStr)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                VatHistoryItem(
                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                    timestamp = obj.optString("timestamp", ""),
                    modeName = obj.optString("modeName", ""),
                    modeTitle = obj.optString("modeTitle", ""),
                    baseAmountStr = obj.optString("baseAmountStr", ""),
                    taxRateStr = obj.optString("taxRateStr", ""),
                    discountRateStr = obj.optString("discountRateStr", ""),
                    taxValueStr = obj.optString("taxValueStr", ""),
                    totalAmountStr = obj.optString("totalAmountStr", ""),
                    currency = obj.optString("currency", "EGP")
                )
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}

private fun saveVatFavoritesToPrefs(context: Context, list: List<VatHistoryItem>) {
    try {
        val prefs = context.getSharedPreferences(VAT_PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        list.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("timestamp", item.timestamp)
                put("modeName", item.modeName)
                put("modeTitle", item.modeTitle)
                put("baseAmountStr", item.baseAmountStr)
                put("taxRateStr", item.taxRateStr)
                put("discountRateStr", item.discountRateStr)
                put("taxValueStr", item.taxValueStr)
                put("totalAmountStr", item.totalAmountStr)
                put("currency", item.currency)
            }
            arr.put(obj)
        }
        prefs.edit().putString("favorites_json", arr.toString()).apply()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun loadVatFavoritesFromPrefs(context: Context): List<VatHistoryItem> {
    val list = mutableListOf<VatHistoryItem>()
    try {
        val prefs = context.getSharedPreferences(VAT_PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("favorites_json", null) ?: return emptyList()
        val arr = JSONArray(jsonStr)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                VatHistoryItem(
                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                    timestamp = obj.optString("timestamp", ""),
                    modeName = obj.optString("modeName", ""),
                    modeTitle = obj.optString("modeTitle", ""),
                    baseAmountStr = obj.optString("baseAmountStr", ""),
                    taxRateStr = obj.optString("taxRateStr", ""),
                    discountRateStr = obj.optString("discountRateStr", ""),
                    taxValueStr = obj.optString("taxValueStr", ""),
                    totalAmountStr = obj.optString("totalAmountStr", ""),
                    currency = obj.optString("currency", "EGP")
                )
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}
