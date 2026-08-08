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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
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
private val ColorPurpleGlow = Color(0xFF8B5CF6)
private val ColorEmeraldGreen = Color(0xFF10B981)
private val ColorCrimsonRed = Color(0xFFEF4444)
private val ColorSlateMuted = Color(0xFF94A3B8)

enum class SplitMode(val titleAr: String, val descAr: String) {
    EQUAL("تقسيم متساوي", "توزيع الفاتورة والبقشيش بالتساوي بين جميع المشاركين"),
    CUSTOM("تقسيم مخصص", "إدخال قيمة استهلاك كل شخص بشكل منفصل مع توزيع نسبي للبقشيش والخدمة")
}

enum class TipBasis(val titleAr: String) {
    PRE_SERVICE("قبل الخدمة والضريبة"),
    POST_SERVICE("بعد الخدمة والضريبة")
}

enum class CashRoundingMode(val titleAr: String, val roundStep: Int) {
    EXACT("بدون تقريب (دقيق)", 0),
    ROUND_1("أقرب 1 صحيح", 1),
    ROUND_5("أقرب 5 وحدات", 5),
    ROUND_10("أقرب 10 وحدات", 10)
}

data class CustomPersonItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    var name: String = "",
    var amountStr: String = "0"
)

data class TipSplitResult(
    val baseBill: BigDecimal,
    val serviceFeeAmount: BigDecimal,
    val totalTipAmount: BigDecimal,
    val grandTotalBill: BigDecimal,
    val perPersonExact: BigDecimal,
    val perPersonRounded: BigDecimal,
    val roundingVarianceTotal: BigDecimal,
    val peopleCount: Int,
    val itemizedBreakdown: List<PersonBreakdownShare>,
    val isValid: Boolean,
    val errorMessage: String? = null
)

data class PersonBreakdownShare(
    val name: String,
    val netConsumption: BigDecimal,
    val serviceShare: BigDecimal,
    val tipShare: BigDecimal,
    val totalShare: BigDecimal
)

data class TipHistoryItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: String,
    val billAmount: String,
    val tipPercent: String,
    val servicePercent: String,
    val peopleCount: Int,
    val splitMode: String,
    val perPersonShare: String,
    val grandTotal: String,
    val currency: String
)

// ==========================================
// MAIN COMPOSABLE SCREEN
// ==========================================

@Composable
fun TipCalcScreen(colors: CustomThemeColors) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current

    // State Retention across Configuration Changes
    var billStr by rememberSaveable { mutableStateOf("250") }
    var tipPercentStr by rememberSaveable { mutableStateOf("12") }
    var servicePercentStr by rememberSaveable { mutableStateOf("10") }
    var isServiceFeeEnabled by rememberSaveable { mutableStateOf(true) }

    var peopleCount by rememberSaveable { mutableIntStateOf(2) }
    var selectedSplitMode by rememberSaveable { mutableStateOf(SplitMode.EQUAL) }
    var selectedTipBasis by rememberSaveable { mutableStateOf(TipBasis.POST_SERVICE) }
    var selectedRoundingMode by rememberSaveable { mutableStateOf(CashRoundingMode.EXACT) }
    var selectedCurrency by rememberSaveable { mutableStateOf("EGP") }

    // Custom Persons List
    val initialCustomPersons = remember {
        listOf(
            CustomPersonItem(name = "أحمد", amountStr = "150"),
            CustomPersonItem(name = "محمد", amountStr = "100")
        )
    }
    var customPersonsList by remember { mutableStateOf(initialCustomPersons) }

    // Expandable Drawers
    var isBreakdownExpanded by rememberSaveable { mutableStateOf(true) }
    var isFavoritesExpanded by rememberSaveable { mutableStateOf(false) }
    var isHistoryExpanded by rememberSaveable { mutableStateOf(false) }

    // Toast Feedback Message
    var toastMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2500)
            toastMessage = null
        }
    }

    // Saved History & Favorites
    var historyList by remember { mutableStateOf(loadTipHistoryFromPrefs(context)) }
    var favoritesList by remember { mutableStateOf(loadTipFavoritesFromPrefs(context)) }

    // Derived Calculation Engine
    val calculationResult by remember(
        billStr, tipPercentStr, servicePercentStr, isServiceFeeEnabled,
        peopleCount, selectedSplitMode, selectedTipBasis, selectedRoundingMode,
        customPersonsList
    ) {
        derivedStateOf {
            calculateTipAndSplitEngine(
                billStr = billStr,
                tipPercentStr = tipPercentStr,
                servicePercentStr = if (isServiceFeeEnabled) servicePercentStr else "0",
                peopleCount = if (selectedSplitMode == SplitMode.CUSTOM) customPersonsList.size else peopleCount,
                splitMode = selectedSplitMode,
                tipBasis = selectedTipBasis,
                roundingMode = selectedRoundingMode,
                customPersons = customPersonsList
            )
        }
    }

    ToolScreenScaffold(
        colors = colors,
        icon = AppIcons.forCalc(CalcKey.TIP),
        title = "حاسبة البقشيش وتقسيم الفواتير",
        subtitle = "حساب البقشيش ورسوم الخدمة وتقسيم الفاتورة بالتساوي أو حسب الاستهلاك",
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
            // Background Canvas Grid Pattern with Wallet / Receipt Vectors
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

                // SECTION A2: Split Mode Segmented Control (Equal vs Custom)
                item(key = "split_mode_selector") {
                    SplitModeSegmentedControl(
                        selectedMode = selectedSplitMode,
                        onModeSelected = { mode ->
                            selectedSplitMode = mode
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    )
                }

                // SECTION B1: Interactive Inputs Panel
                item(key = "inputs_panel") {
                    InputsPanel(
                        billStr = billStr,
                        onBillChange = { billStr = it },
                        tipPercentStr = tipPercentStr,
                        onTipPercentChange = { tipPercentStr = it },
                        servicePercentStr = servicePercentStr,
                        onServicePercentChange = { servicePercentStr = it },
                        isServiceFeeEnabled = isServiceFeeEnabled,
                        onToggleServiceFee = { isServiceFeeEnabled = !isServiceFeeEnabled },
                        peopleCount = peopleCount,
                        onPeopleCountChange = { newCount ->
                            peopleCount = newCount.coerceIn(1, 50)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        selectedSplitMode = selectedSplitMode,
                        customPersons = customPersonsList,
                        onCustomPersonsChange = { customPersonsList = it },
                        selectedTipBasis = selectedTipBasis,
                        onTipBasisChange = {
                            selectedTipBasis = it
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        selectedRoundingMode = selectedRoundingMode,
                        onRoundingModeChange = {
                            selectedRoundingMode = it
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        currency = selectedCurrency
                    )
                }

                // SECTION B2: Live Result Display Card
                item(key = "result_display_card") {
                    ResultDisplayCard(
                        result = calculationResult,
                        currency = selectedCurrency,
                        splitMode = selectedSplitMode,
                        onSaveHistory = {
                            if (!calculationResult.isValid) return@ResultDisplayCard
                            val timeStr = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm - yyyy/MM/dd"))
                            val newItem = TipHistoryItem(
                                timestamp = timeStr,
                                billAmount = billStr,
                                tipPercent = tipPercentStr,
                                servicePercent = if (isServiceFeeEnabled) servicePercentStr else "0",
                                peopleCount = if (selectedSplitMode == SplitMode.CUSTOM) customPersonsList.size else peopleCount,
                                splitMode = selectedSplitMode.titleAr,
                                perPersonShare = calculationResult.perPersonRounded.toPlainString(),
                                grandTotal = calculationResult.grandTotalBill.toPlainString(),
                                currency = selectedCurrency
                            )
                            historyList = listOf(newItem) + historyList.take(19)
                            saveTipHistoryToPrefs(context, historyList)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            toastMessage = "تم حفظ الفاتورة في السجل"
                        },
                        onSaveFavorite = {
                            if (!calculationResult.isValid) return@ResultDisplayCard
                            val timeStr = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm - yyyy/MM/dd"))
                            val newItem = TipHistoryItem(
                                timestamp = timeStr,
                                billAmount = billStr,
                                tipPercent = tipPercentStr,
                                servicePercent = if (isServiceFeeEnabled) servicePercentStr else "0",
                                peopleCount = if (selectedSplitMode == SplitMode.CUSTOM) customPersonsList.size else peopleCount,
                                splitMode = selectedSplitMode.titleAr,
                                perPersonShare = calculationResult.perPersonRounded.toPlainString(),
                                grandTotal = calculationResult.grandTotalBill.toPlainString(),
                                currency = selectedCurrency
                            )
                            favoritesList = listOf(newItem) + favoritesList
                            saveTipFavoritesToPrefs(context, favoritesList)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            toastMessage = "تمت إضافة العملية للمفضلة ⭐"
                        },
                        onWhatsAppShare = {
                            val msg = formatWhatsAppMessage(calculationResult, selectedCurrency, selectedSplitMode)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, msg)
                                setPackage("com.whatsapp")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Fallback general share
                                val chooser = Intent.createChooser(
                                    Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, msg)
                                    }, "مشاركة تفاصيل الفاتورة"
                                )
                                context.startActivity(chooser)
                            }
                        },
                        onCopy = {
                            val shareText = formatWhatsAppMessage(calculationResult, selectedCurrency, selectedSplitMode)
                            clipboardManager.setText(AnnotatedString(shareText))
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            toastMessage = "تم نسخ التقرير للحافظة 📋"
                        },
                        onReset = {
                            billStr = "250"
                            tipPercentStr = "12"
                            servicePercentStr = "10"
                            peopleCount = 2
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            toastMessage = "تم إعادة ضبط الفاتورة"
                        }
                    )
                }

                // SECTION C: Detailed Itemized Breakdown Card
                item(key = "itemized_breakdown") {
                    ExpandableCard(
                        title = "تفاصيل الحسبة وتوزيع الأنصبة",
                        icon = Icons.Outlined.ReceiptLong,
                        isExpanded = isBreakdownExpanded,
                        onToggle = { isBreakdownExpanded = !isBreakdownExpanded }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("• قيمة الفاتورة الأساسية:", fontSize = 12.sp, color = ColorSlateMuted)
                                Text("${calculationResult.baseBill.toPlainString()} $selectedCurrency", fontSize = 12.sp, color = Color.White)
                            }

                            if (calculationResult.serviceFeeAmount > BigDecimal.ZERO) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("• رسوم الخدمة والضريبة:", fontSize = 12.sp, color = ColorSlateMuted)
                                    Text("${calculationResult.serviceFeeAmount.toPlainString()} $selectedCurrency", fontSize = 12.sp, color = ColorPurpleGlow, fontWeight = FontWeight.Bold)
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("• إجمالي البقشيش:", fontSize = 12.sp, color = ColorSlateMuted)
                                Text("${calculationResult.totalTipAmount.toPlainString()} $selectedCurrency", fontSize = 12.sp, color = ColorIceCyan, fontWeight = FontWeight.Bold)
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("• الإجمالي الكلي النهائي:", fontSize = 13.sp, color = ColorAmberGlow, fontWeight = FontWeight.Bold)
                                Text("${calculationResult.grandTotalBill.toPlainString()} $selectedCurrency", fontSize = 13.sp, color = ColorAmberGlow, fontWeight = FontWeight.Bold)
                            }

                            Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

                            Text("توزيع الأنصبة لكل مشارك:", fontSize = 12.sp, color = ColorAmberGlow, fontWeight = FontWeight.Bold)

                            calculationResult.itemizedBreakdown.forEachIndexed { index, person ->
                                Surface(
                                    color = Color.Black.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("${person.name} (${index + 1}#)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text(
                                                "صافي: ${person.netConsumption.toPlainString()} + بقشيش: ${person.tipShare.toPlainString()} $selectedCurrency",
                                                color = ColorSlateMuted,
                                                fontSize = 10.sp
                                            )
                                        }

                                        Text(
                                            "${person.totalShare.toPlainString()} $selectedCurrency",
                                            color = ColorEmeraldGreen,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // SECTION D: Favorites List
                if (favoritesList.isNotEmpty()) {
                    item(key = "favorites_section") {
                        ExpandableCard(
                            title = "الفواتير المفضلة (${favoritesList.size})",
                            icon = Icons.Outlined.Star,
                            isExpanded = isFavoritesExpanded,
                            onToggle = { isFavoritesExpanded = !isFavoritesExpanded }
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                favoritesList.forEach { fav ->
                                    HistoryItemRow(
                                        item = fav,
                                        onReuse = {
                                            billStr = fav.billAmount
                                            tipPercentStr = fav.tipPercent
                                            servicePercentStr = fav.servicePercent
                                            peopleCount = fav.peopleCount
                                            selectedCurrency = fav.currency
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            toastMessage = "تم تطبيق الفاتورة المفضلة"
                                        },
                                        onDelete = {
                                            favoritesList = favoritesList.filter { it.id != fav.id }
                                            saveTipFavoritesToPrefs(context, favoritesList)
                                            toastMessage = "تم حذف الفاتورة من المفضلة"
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
                            title = "سجل الفواتير السابقة (${historyList.size})",
                            icon = Icons.Outlined.History,
                            isExpanded = isHistoryExpanded,
                            onToggle = { isHistoryExpanded = !isHistoryExpanded }
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                historyList.forEach { hist ->
                                    HistoryItemRow(
                                        item = hist,
                                        onReuse = {
                                            billStr = hist.billAmount
                                            tipPercentStr = hist.tipPercent
                                            servicePercentStr = hist.servicePercent
                                            peopleCount = hist.peopleCount
                                            selectedCurrency = hist.currency
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            toastMessage = "تم استرجاع الفاتورة السابقة"
                                        },
                                        onDelete = {
                                            historyList = historyList.filter { it.id != hist.id }
                                            saveTipHistoryToPrefs(context, historyList)
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
            // Procedural Canvas Receipt / Bill Icon with Glow Animation
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

                    // Receipt body
                    val receiptPath = Path().apply {
                        moveTo(w * 0.2f, h * 0.15f)
                        lineTo(w * 0.8f, h * 0.15f)
                        lineTo(w * 0.8f, h * 0.85f)
                        lineTo(w * 0.7f, h * 0.8f)
                        lineTo(w * 0.6f, h * 0.85f)
                        lineTo(w * 0.5f, h * 0.8f)
                        lineTo(w * 0.4f, h * 0.85f)
                        lineTo(w * 0.3f, h * 0.8f)
                        lineTo(w * 0.2f, h * 0.85f)
                        close()
                    }
                    drawPath(receiptPath, color = ColorGoldBorder, style = Stroke(width = 3f))

                    // Receipt lines
                    drawLine(ColorIceCyan, Offset(w * 0.35f, h * 0.35f), Offset(w * 0.65f, h * 0.35f), strokeWidth = 2.5f)
                    drawLine(ColorIceCyan, Offset(w * 0.35f, h * 0.5f), Offset(w * 0.65f, h * 0.5f), strokeWidth = 2.5f)
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "حاسبة البقشيش والتقسيم",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "حساب البقشيش ورسوم الخدمة وتقسيم الفواتير",
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
private fun SplitModeSegmentedControl(
    selectedMode: SplitMode,
    onModeSelected: (SplitMode) -> Unit
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
            SplitMode.values().forEach { mode ->
                val isSelected = mode == selectedMode
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
    billStr: String,
    onBillChange: (String) -> Unit,
    tipPercentStr: String,
    onTipPercentChange: (String) -> Unit,
    servicePercentStr: String,
    onServicePercentChange: (String) -> Unit,
    isServiceFeeEnabled: Boolean,
    onToggleServiceFee: () -> Unit,
    peopleCount: Int,
    onPeopleCountChange: (Int) -> Unit,
    selectedSplitMode: SplitMode,
    customPersons: List<CustomPersonItem>,
    onCustomPersonsChange: (List<CustomPersonItem>) -> Unit,
    selectedTipBasis: TipBasis,
    onTipBasisChange: (TipBasis) -> Unit,
    selectedRoundingMode: CashRoundingMode,
    onRoundingModeChange: (CashRoundingMode) -> Unit,
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
            // Bill Amount Input
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("قيمة الفاتورة الإجمالية ($currency)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)

                OutlinedTextField(
                    value = billStr,
                    onValueChange = onBillChange,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        if (billStr.isNotEmpty()) {
                            IconButton(onClick = { onBillChange("") }) {
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

            // Service Fee & Tax Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = isServiceFeeEnabled,
                        onCheckedChange = { onToggleServiceFee() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ColorPurpleGlow,
                            checkedTrackColor = ColorPurpleGlow.copy(alpha = 0.3f)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إضافة رسوم خدمة / ضريبة مضافة", color = Color.White, fontSize = 12.sp)
                }

                if (isServiceFeeEnabled) {
                    OutlinedTextField(
                        value = servicePercentStr,
                        onValueChange = onServicePercentChange,
                        label = { Text("الخدمة %", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.width(90.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ColorPurpleGlow,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            }

            // Tip Percentage & Quick Chips
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("نسبة البقشيش (%)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    OutlinedTextField(
                        value = tipPercentStr,
                        onValueChange = onTipPercentChange,
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

                // Quick Tip Preset Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("0", "5", "10", "12", "15", "20").forEach { preset ->
                        val isSelected = tipPercentStr == preset
                        Surface(
                            color = if (isSelected) ColorIceCyan.copy(alpha = 0.25f) else Color(0xFF1E2638),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (isSelected) ColorIceCyan else Color.White.copy(alpha = 0.15f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onTipPercentChange(preset) }
                        ) {
                            Text(
                                "$preset%",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) ColorIceCyan else ColorSlateMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Tip Calculation Basis Toggle (Pre vs Post Service)
            if (isServiceFeeEnabled) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("أساس حساب البقشيش:", fontSize = 11.sp, color = ColorSlateMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TipBasis.values().forEach { basis ->
                            val isSel = basis == selectedTipBasis
                            Surface(
                                color = if (isSel) ColorGoldBorder.copy(alpha = 0.2f) else Color(0xFF1E2638),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (isSel) ColorGoldBorder else Color.White.copy(alpha = 0.1f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onTipBasisChange(basis) }
                            ) {
                                Text(
                                    basis.titleAr,
                                    fontSize = 10.sp,
                                    color = if (isSel) ColorAmberGlow else ColorSlateMuted,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Equal Split: People Count Stepper
            if (selectedSplitMode == SplitMode.EQUAL) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("عدد الأشخاص المشاركين في الفاتورة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Stepper -
                        IconButton(
                            onClick = { onPeopleCountChange(peopleCount - 1) },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF1E2638), CircleShape)
                        ) {
                            Icon(Icons.Filled.Remove, contentDescription = "إنقاص", tint = Color.White)
                        }

                        Text("$peopleCount أشخاص", fontSize = 18.sp, fontWeight = FontWeight.Black, color = ColorAmberGlow)

                        // Stepper +
                        IconButton(
                            onClick = { onPeopleCountChange(peopleCount + 1) },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF1E2638), CircleShape)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "زيادة", tint = Color.White)
                        }
                    }

                    // Quick People Preset Chips
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(2, 3, 4, 5, 6, 8, 10).forEach { p ->
                            val isSel = peopleCount == p
                            Surface(
                                color = if (isSel) ColorAmberGlow.copy(alpha = 0.25f) else Color(0xFF1E2638),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (isSel) ColorAmberGlow else Color.White.copy(alpha = 0.1f)),
                                modifier = Modifier.clickable { onPeopleCountChange(p) }
                            ) {
                                Text(
                                    "$p",
                                    fontSize = 11.sp,
                                    color = if (isSel) ColorAmberGlow else ColorSlateMuted,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // Custom Split: Persons List Manager
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("قائمة استهلاك الأشخاص المنفردة:", fontSize = 12.sp, color = ColorAmberGlow, fontWeight = FontWeight.Bold)

                        IconButton(onClick = {
                            val nextIndex = customPersons.size + 1
                            onCustomPersonsChange(customPersons + CustomPersonItem(name = "شخص $nextIndex", amountStr = "50"))
                        }) {
                            Icon(Icons.Filled.AddCircle, contentDescription = "إضافة شخص", tint = ColorEmeraldGreen)
                        }
                    }

                    customPersons.forEachIndexed { index, person ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = person.name,
                                onValueChange = { newName ->
                                    val list = customPersons.toMutableList()
                                    list[index] = person.copy(name = newName)
                                    onCustomPersonsChange(list)
                                },
                                label = { Text("الاسم", fontSize = 10.sp) },
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
                                value = person.amountStr,
                                onValueChange = { newAmt ->
                                    val list = customPersons.toMutableList()
                                    list[index] = person.copy(amountStr = newAmt)
                                    onCustomPersonsChange(list)
                                },
                                label = { Text("الاستهلاك", fontSize = 10.sp) },
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

                            if (customPersons.size > 1) {
                                IconButton(onClick = {
                                    onCustomPersonsChange(customPersons.filterIndexed { i, _ -> i != index })
                                }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "حذف", tint = ColorCrimsonRed)
                                }
                            }
                        }
                    }
                }
            }

            // Cash Rounding Mode Chips
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("خيارات تقريب المبلغ النادي:", fontSize = 11.sp, color = ColorSlateMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CashRoundingMode.values().forEach { mode ->
                        val isSel = mode == selectedRoundingMode
                        Surface(
                            color = if (isSel) ColorEmeraldGreen.copy(alpha = 0.25f) else Color(0xFF1E2638),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (isSel) ColorEmeraldGreen else Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onRoundingModeChange(mode) }
                        ) {
                            Text(
                                mode.titleAr,
                                fontSize = 9.sp,
                                color = if (isSel) ColorEmeraldGreen else ColorSlateMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultDisplayCard(
    result: TipSplitResult,
    currency: String,
    splitMode: SplitMode,
    onSaveHistory: () -> Unit,
    onSaveFavorite: () -> Unit,
    onWhatsAppShare: () -> Unit,
    onCopy: () -> Unit,
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
                Surface(
                    color = ColorCrimsonRed.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, ColorCrimsonRed)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = ColorCrimsonRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(result.errorMessage ?: "خطأ في المدخلات", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Primary Per Person Metric
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (splitMode == SplitMode.EQUAL) "المستحق للشخص الواحد" else "متوسط نصيب الفرد",
                        fontSize = 12.sp,
                        color = ColorSlateMuted
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${result.perPersonRounded.toPlainString()} $currency",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = ColorAmberGlow
                    )
                }

                // Total Tip & Fees Badge & Grand Total
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("إجمالي البقشيش والخدمة", fontSize = 11.sp, color = ColorSlateMuted)
                        Text(
                            "${(result.totalTipAmount + result.serviceFeeAmount).toPlainString()} $currency",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorIceCyan
                        )
                    }

                    Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color.White.copy(alpha = 0.15f)))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("الإجمالي الكلي للفاتورة", fontSize = 11.sp, color = ColorSlateMuted)
                        Text(
                            "${result.grandTotalBill.toPlainString()} $currency",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorEmeraldGreen
                        )
                    }
                }

                if (result.roundingVarianceTotal != BigDecimal.ZERO) {
                    Text(
                        "فارق التقريب النادي الكلي: ${result.roundingVarianceTotal.toPlainString()} $currency",
                        fontSize = 10.sp,
                        color = ColorPurpleGlow
                    )
                }

                // Quick Action Toolbar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onWhatsAppShare,
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF1E2638), RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = "واتساب", tint = ColorEmeraldGreen)
                    }

                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF1E2638), RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "نسخ", tint = ColorIceCyan)
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
                        Icon(Icons.Filled.Save, contentDescription = "حفظ السجل", tint = ColorPurpleGlow)
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
    item: TipHistoryItem,
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
                Text("${item.splitMode} (${item.peopleCount} أشخاص)", color = ColorAmberGlow, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("الفاتورة: ${item.billAmount} | حصة الفرد: ${item.perPersonShare} ${item.currency}", color = Color.White, fontSize = 11.sp)
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

private fun calculateTipAndSplitEngine(
    billStr: String,
    tipPercentStr: String,
    servicePercentStr: String,
    peopleCount: Int,
    splitMode: SplitMode,
    tipBasis: TipBasis,
    roundingMode: CashRoundingMode,
    customPersons: List<CustomPersonItem>
): TipSplitResult {
    val billVal = billStr.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val tipPctVal = tipPercentStr.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val servicePctVal = servicePercentStr.toBigDecimalOrNull() ?: BigDecimal.ZERO

    val safePeopleCount = maxOf(peopleCount, 1)

    if (billVal <= BigDecimal.ZERO && splitMode == SplitMode.EQUAL) {
        return TipSplitResult(
            baseBill = BigDecimal.ZERO,
            serviceFeeAmount = BigDecimal.ZERO,
            totalTipAmount = BigDecimal.ZERO,
            grandTotalBill = BigDecimal.ZERO,
            perPersonExact = BigDecimal.ZERO,
            perPersonRounded = BigDecimal.ZERO,
            roundingVarianceTotal = BigDecimal.ZERO,
            peopleCount = safePeopleCount,
            itemizedBreakdown = emptyList(),
            isValid = false,
            errorMessage = "يرجى إدخال قيمة فاتورة صالحة أكبر من صفر"
        )
    }

    val hundred = BigDecimal("100")

    // Calculate base service fee
    val serviceFee = billVal.multiply(servicePctVal).divide(hundred, 2, RoundingMode.HALF_UP)

    // Calculate tip based on pre or post service
    val tipBaseAmount = if (tipBasis == TipBasis.POST_SERVICE) billVal.add(serviceFee) else billVal
    val totalTip = tipBaseAmount.multiply(tipPctVal).divide(hundred, 2, RoundingMode.HALF_UP)

    val grandTotal = billVal.add(serviceFee).add(totalTip)

    var perPersonExact = BigDecimal.ZERO
    var perPersonRounded = BigDecimal.ZERO
    var roundingVarianceTotal = BigDecimal.ZERO

    val itemizedList = mutableListOf<PersonBreakdownShare>()

    if (splitMode == SplitMode.EQUAL) {
        val countBd = BigDecimal(safePeopleCount)
        perPersonExact = grandTotal.divide(countBd, 2, RoundingMode.HALF_UP)

        // Apply Cash Rounding
        perPersonRounded = applyCashRounding(perPersonExact, roundingMode)
        roundingVarianceTotal = perPersonRounded.multiply(countBd).subtract(grandTotal)

        val personNet = billVal.divide(countBd, 2, RoundingMode.HALF_UP)
        val personService = serviceFee.divide(countBd, 2, RoundingMode.HALF_UP)
        val personTip = totalTip.divide(countBd, 2, RoundingMode.HALF_UP)

        for (i in 1..safePeopleCount) {
            itemizedList.add(
                PersonBreakdownShare(
                    name = "شخص $i",
                    netConsumption = personNet,
                    serviceShare = personService,
                    tipShare = personTip,
                    totalShare = perPersonRounded
                )
            )
        }
    } else {
        // Custom Split
        var sumCustomBill = BigDecimal.ZERO
        customPersons.forEach { p ->
            val amt = p.amountStr.toBigDecimalOrNull() ?: BigDecimal.ZERO
            sumCustomBill = sumCustomBill.add(amt)
        }

        val effectiveBill = if (sumCustomBill > BigDecimal.ZERO) sumCustomBill else billVal

        customPersons.forEach { p ->
            val pAmt = p.amountStr.toBigDecimalOrNull() ?: BigDecimal.ZERO
            val ratio = if (effectiveBill > BigDecimal.ZERO) pAmt.divide(effectiveBill, 4, RoundingMode.HALF_UP) else BigDecimal.ZERO

            val pService = serviceFee.multiply(ratio).setScale(2, RoundingMode.HALF_UP)
            val pTip = totalTip.multiply(ratio).setScale(2, RoundingMode.HALF_UP)
            val pTotalExact = pAmt.add(pService).add(pTip)
            val pTotalRounded = applyCashRounding(pTotalExact, roundingMode)

            itemizedList.add(
                PersonBreakdownShare(
                    name = if (p.name.isBlank()) "شخص" else p.name,
                    netConsumption = pAmt,
                    serviceShare = pService,
                    tipShare = pTip,
                    totalShare = pTotalRounded
                )
            )
        }

        val sumRoundedTotal = itemizedList.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.totalShare) }
        roundingVarianceTotal = sumRoundedTotal.subtract(grandTotal)
        perPersonRounded = if (customPersons.isNotEmpty()) sumRoundedTotal.divide(BigDecimal(customPersons.size), 2, RoundingMode.HALF_UP) else BigDecimal.ZERO
    }

    return TipSplitResult(
        baseBill = billVal,
        serviceFeeAmount = serviceFee,
        totalTipAmount = totalTip,
        grandTotalBill = grandTotal,
        perPersonExact = perPersonExact,
        perPersonRounded = perPersonRounded,
        roundingVarianceTotal = roundingVarianceTotal,
        peopleCount = if (splitMode == SplitMode.CUSTOM) customPersons.size else safePeopleCount,
        itemizedBreakdown = itemizedList,
        isValid = true
    )
}

private fun applyCashRounding(amount: BigDecimal, mode: CashRoundingMode): BigDecimal {
    if (mode.roundStep <= 0) return amount
    val doubleVal = amount.toDouble()
    val step = mode.roundStep.toDouble()
    val roundedVal = kotlin.math.ceil(doubleVal / step) * step
    return BigDecimal(roundedVal).setScale(2, RoundingMode.HALF_UP)
}

private fun formatWhatsAppMessage(
    result: TipSplitResult,
    currency: String,
    splitMode: SplitMode
): String {
    val sb = java.lang.StringBuilder()
    sb.append("🍽️ *تقرير تقسيم الفاتورة والبقشيش*\n")
    sb.append("-----------------------------\n")
    sb.append("• قيمة الفاتورة: ${result.baseBill.toPlainString()} $currency\n")
    if (result.serviceFeeAmount > BigDecimal.ZERO) {
        sb.append("• رسوم الخدمة: ${result.serviceFeeAmount.toPlainString()} $currency\n")
    }
    sb.append("• البقشيش: ${result.totalTipAmount.toPlainString()} $currency\n")
    sb.append("• الإجمالي الكلي: ${result.grandTotalBill.toPlainString()} $currency\n")
    sb.append("-----------------------------\n")
    sb.append("👥 *طريقة التقسيم*: ${splitMode.titleAr}\n")
    sb.append("• المستحق للشخص الواحدة: *${result.perPersonRounded.toPlainString()} $currency*\n\n")

    sb.append("تفاصيل حصة كل شخص:\n")
    result.itemizedBreakdown.forEach { p ->
        sb.append("• ${p.name}: ${p.totalShare.toPlainString()} $currency\n")
    }
    sb.append("\nتم الحساب بواسطة حاسبة البقشيش وتقسيم الفواتير الذكية ⚡")
    return sb.toString()
}

// ==========================================
// PREFERENCES PERSISTENCE HELPERS
// ==========================================

private fun saveTipHistoryToPrefs(context: Context, list: List<TipHistoryItem>) {
    try {
        val prefs = context.getSharedPreferences("tip_calc_prefs", Context.MODE_PRIVATE)
        val array = JSONArray()
        list.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("timestamp", item.timestamp)
                put("billAmount", item.billAmount)
                put("tipPercent", item.tipPercent)
                put("servicePercent", item.servicePercent)
                put("peopleCount", item.peopleCount)
                put("splitMode", item.splitMode)
                put("perPersonShare", item.perPersonShare)
                put("grandTotal", item.grandTotal)
                put("currency", item.currency)
            }
            array.put(obj)
        }
        prefs.edit().putString("tip_history", array.toString()).apply()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun loadTipHistoryFromPrefs(context: Context): List<TipHistoryItem> {
    val result = mutableListOf<TipHistoryItem>()
    try {
        val prefs = context.getSharedPreferences("tip_calc_prefs", Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("tip_history", null) ?: return emptyList()
        val array = JSONArray(jsonStr)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            result.add(
                TipHistoryItem(
                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                    timestamp = obj.optString("timestamp", ""),
                    billAmount = obj.optString("billAmount", ""),
                    tipPercent = obj.optString("tipPercent", ""),
                    servicePercent = obj.optString("servicePercent", ""),
                    peopleCount = obj.optInt("peopleCount", 1),
                    splitMode = obj.optString("splitMode", ""),
                    perPersonShare = obj.optString("perPersonShare", ""),
                    grandTotal = obj.optString("grandTotal", ""),
                    currency = obj.optString("currency", "EGP")
                )
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return result
}

private fun saveTipFavoritesToPrefs(context: Context, list: List<TipHistoryItem>) {
    try {
        val prefs = context.getSharedPreferences("tip_calc_prefs", Context.MODE_PRIVATE)
        val array = JSONArray()
        list.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("timestamp", item.timestamp)
                put("billAmount", item.billAmount)
                put("tipPercent", item.tipPercent)
                put("servicePercent", item.servicePercent)
                put("peopleCount", item.peopleCount)
                put("splitMode", item.splitMode)
                put("perPersonShare", item.perPersonShare)
                put("grandTotal", item.grandTotal)
                put("currency", item.currency)
            }
            array.put(obj)
        }
        prefs.edit().putString("tip_favorites", array.toString()).apply()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun loadTipFavoritesFromPrefs(context: Context): List<TipHistoryItem> {
    val result = mutableListOf<TipHistoryItem>()
    try {
        val prefs = context.getSharedPreferences("tip_calc_prefs", Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("tip_favorites", null) ?: return emptyList()
        val array = JSONArray(jsonStr)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            result.add(
                TipHistoryItem(
                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                    timestamp = obj.optString("timestamp", ""),
                    billAmount = obj.optString("billAmount", ""),
                    tipPercent = obj.optString("tipPercent", ""),
                    servicePercent = obj.optString("servicePercent", ""),
                    peopleCount = obj.optInt("peopleCount", 1),
                    splitMode = obj.optString("splitMode", ""),
                    perPersonShare = obj.optString("perPersonShare", ""),
                    grandTotal = obj.optString("grandTotal", ""),
                    currency = obj.optString("currency", "EGP")
                )
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return result
}
