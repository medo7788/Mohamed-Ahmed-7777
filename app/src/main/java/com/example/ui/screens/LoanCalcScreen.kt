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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.data.LivePricesRepository
import com.example.model.CalcKey
import com.example.ui.components.ToolScreenScaffold
import com.example.ui.theme.AppIcons
import com.example.ui.theme.CustomThemeColors
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

// --- DATA MODELS ---

enum class LoanInterestMode(val title: String, val subtitle: String) {
    REDUCING("فائدة متناقصة", "تحتسب الفائدة على الرصيد المتبقي شهرياً (EMI)"),
    FLAT("فائدة ثابتة", "تحتسب الفائدة على أصل المبلغ طوال المدة")
}

enum class TenureUnit(val title: String) {
    MONTHS("شهور"),
    YEARS("سنوات")
}

enum class ProcessingFeeType(val title: String) {
    PERCENTAGE("نسبة %"),
    FIXED("مبلغ ثابت")
}

data class AmortizationRow(
    val monthNumber: Int,
    val payment: Double,
    val principalPaid: Double,
    val interestPaid: Double,
    val remainingBalance: Double
)

data class LoanHistoryItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val principal: Double,
    val annualRate: Double,
    val months: Int,
    val mode: LoanInterestMode,
    val currency: String,
    val emi: Double,
    val totalInterest: Double,
    val totalPayment: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)

data class LoanScenario(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val principal: Double,
    val annualRate: Double,
    val months: Int,
    val mode: LoanInterestMode,
    val currency: String
)

// --- MAIN COMPOSABLE SCREEN ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanCalcScreen(colors: CustomThemeColors) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Preferences & State Persistence
    val sharedPrefs = remember {
        context.getSharedPreferences("loan_calc_prefs", Context.MODE_PRIVATE)
    }

    // Input States
    var loanAmountText by rememberSaveable { mutableStateOf("250000") }
    var interestRateText by rememberSaveable { mutableStateOf("12.5") }
    var tenureText by rememberSaveable { mutableStateOf("36") }
    var tenureUnit by rememberSaveable { mutableStateOf(TenureUnit.MONTHS) }
    var interestMode by rememberSaveable { mutableStateOf(LoanInterestMode.REDUCING) }
    var selectedCurrency by rememberSaveable { mutableStateOf("EGP") }

    // Processing Fee Options
    var enableFee by rememberSaveable { mutableStateOf(false) }
    var feeTypeText by rememberSaveable { mutableStateOf("PERCENTAGE") }
    var feeValueText by rememberSaveable { mutableStateOf("1.0") }

    // Toggles & Modal Sheets
    var showAmortizationSheet by rememberSaveable { mutableStateOf(false) }
    var showEarlySettlement by rememberSaveable { mutableStateOf(false) }
    var showComparisonMode by rememberSaveable { mutableStateOf(false) }
    var showHistoryDrawer by rememberSaveable { mutableStateOf(false) }

    // Early Settlement Inputs
    var paidMonthsText by rememberSaveable { mutableStateOf("12") }
    var settlementFeePercentText by rememberSaveable { mutableStateOf("0.0") }

    // History State (Persisted in SharedPreferences)
    var historyListJson by remember {
        mutableStateOf(sharedPrefs.getString("history_json", "[]") ?: "[]")
    }

    val historyItems = remember(historyListJson) {
        parseHistoryJson(historyListJson)
    }

    fun saveHistoryList(newList: List<LoanHistoryItem>) {
        val json = serializeHistoryJson(newList)
        sharedPrefs.edit().putString("history_json", json).apply()
        historyListJson = json
    }

    // Validation & Calculation Parsing
    val principal = loanAmountText.toDoubleOrNull() ?: 0.0
    val annualRate = interestRateText.toDoubleOrNull() ?: 0.0
    val tenureValue = tenureText.toIntOrNull() ?: 0

    val totalMonths = remember(tenureValue, tenureUnit) {
        if (tenureUnit == TenureUnit.YEARS) tenureValue * 12 else tenureValue
    }

    val feeValue = feeValueText.toDoubleOrNull() ?: 0.0
    val processingFeeType = if (feeTypeText == "FIXED") ProcessingFeeType.FIXED else ProcessingFeeType.PERCENTAGE

    // Processing Fee Amount
    val processingFeeAmount = remember(principal, feeValue, processingFeeType, enableFee) {
        if (!enableFee) 0.0
        else if (processingFeeType == ProcessingFeeType.PERCENTAGE) principal * (feeValue / 100.0)
        else feeValue
    }

    // Derived Calculations using derivedStateOf for instant 60fps re-computation
    val calculations = remember(principal, annualRate, totalMonths, interestMode, processingFeeAmount) {
        derivedStateOf {
            if (principal <= 0 || totalMonths <= 0) {
                return@derivedStateOf null
            }

            val monthlyRate = (annualRate / 100.0) / 12.0

            val emi: Double
            val totalPayment: Double
            val totalInterest: Double

            if (interestMode == LoanInterestMode.REDUCING) {
                if (monthlyRate > 0) {
                    emi = (principal * monthlyRate * (1 + monthlyRate).pow(totalMonths.toDouble())) /
                            ((1 + monthlyRate).pow(totalMonths.toDouble()) - 1)
                } else {
                    emi = principal / totalMonths
                }
                totalPayment = emi * totalMonths
                totalInterest = max(0.0, totalPayment - principal)
            } else {
                // FLAT RATE
                totalInterest = principal * (annualRate / 100.0) * (totalMonths / 12.0)
                totalPayment = principal + totalInterest
                emi = totalPayment / totalMonths
            }

            val netDisbursed = max(0.0, principal - processingFeeAmount)

            // Generate Amortization Schedule
            val rows = mutableListOf<AmortizationRow>()
            var balance = principal

            if (interestMode == LoanInterestMode.REDUCING) {
                for (m in 1..totalMonths) {
                    val interestForMonth = balance * monthlyRate
                    var principalForMonth = emi - interestForMonth
                    if (m == totalMonths || balance < principalForMonth) {
                        principalForMonth = balance
                    }
                    balance = max(0.0, balance - principalForMonth)
                    rows.add(
                        AmortizationRow(
                            monthNumber = m,
                            payment = emi,
                            principalPaid = principalForMonth,
                            interestPaid = interestForMonth,
                            remainingBalance = balance
                        )
                    )
                }
            } else {
                // FLAT RATE
                val flatMonthlyInterest = totalInterest / totalMonths
                val flatMonthlyPrincipal = principal / totalMonths
                for (m in 1..totalMonths) {
                    balance = max(0.0, balance - flatMonthlyPrincipal)
                    rows.add(
                        AmortizationRow(
                            monthNumber = m,
                            payment = emi,
                            principalPaid = flatMonthlyPrincipal,
                            interestPaid = flatMonthlyInterest,
                            remainingBalance = balance
                        )
                    )
                }
            }

            LoanCalcResult(
                principal = principal,
                annualRate = annualRate,
                totalMonths = totalMonths,
                emi = emi,
                totalInterest = totalInterest,
                totalPayment = totalPayment,
                processingFee = processingFeeAmount,
                netDisbursed = netDisbursed,
                amortizationRows = rows
            )
        }
    }

    val result = calculations.value

    // Early Settlement Calculation
    val paidMonths = paidMonthsText.toIntOrNull() ?: 0
    val settlementFeePercent = settlementFeePercentText.toDoubleOrNull() ?: 0.0

    val earlySettlementResult = remember(result, paidMonths, settlementFeePercent) {
        if (result == null || paidMonths <= 0 || paidMonths >= result.totalMonths) null
        else {
            val paidRows = result.amortizationRows.take(paidMonths)
            val remainingRows = result.amortizationRows.drop(paidMonths)

            val remainingPrincipal = paidRows.lastOrNull()?.remainingBalance ?: result.principal
            val interestSaved = remainingRows.sumOf { it.interestPaid }
            val settlementFee = remainingPrincipal * (settlementFeePercent / 100.0)
            val netPayoffAmount = remainingPrincipal + settlementFee

            EarlySettlementData(
                paidMonths = paidMonths,
                remainingPrincipal = remainingPrincipal,
                interestSaved = interestSaved,
                settlementFee = settlementFee,
                netPayoffAmount = netPayoffAmount
            )
        }
    }

    // Comparison Scenarios State
    var scenario2PrincipalText by rememberSaveable { mutableStateOf("250000") }
    var scenario2RateText by rememberSaveable { mutableStateOf("11.5") }
    var scenario2MonthsText by rememberSaveable { mutableStateOf("36") }

    val scenario2Result = remember(scenario2PrincipalText, scenario2RateText, scenario2MonthsText) {
        val p = scenario2PrincipalText.toDoubleOrNull() ?: 0.0
        val r = scenario2RateText.toDoubleOrNull() ?: 0.0
        val m = scenario2MonthsText.toIntOrNull() ?: 0
        if (p <= 0 || m <= 0) null
        else calculateSimpleLoan(p, r, m, LoanInterestMode.REDUCING)
    }

    // Error shaking animation state
    var triggerErrorShake by remember { mutableStateOf(false) }
    val shakeOffset by animateFloatAsState(
        targetValue = if (triggerErrorShake) 12f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium),
        finishedListener = { triggerErrorShake = false },
        label = "shake"
    )

    ToolScreenScaffold(
        colors = colors,
        icon = AppIcons.forCalc(CalcKey.LOAN),
        title = "حاسبة القروض والتمويل",
        subtitle = "حساب الأقساط الشهرية، الفوائد المركبة، وجدول السداد التفصيلي"
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. TOP HEADER & CURRENCY SWITCHER & INTEREST MODE TOGGLE
                item {
                    HeroHeaderSection(
                        colors = colors,
                        selectedCurrency = selectedCurrency,
                        onCurrencySelect = { currency ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedCurrency = currency
                        },
                        interestMode = interestMode,
                        onModeSelect = { mode ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            interestMode = mode
                        }
                    )
                }

                // 2. HERO RESULT CARD & CANVAS DONUT CHART
                item {
                    if (result != null) {
                        HeroResultCard(
                            result = result,
                            currency = selectedCurrency,
                            colors = colors,
                            onShowAmortization = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showAmortizationSheet = true
                            }
                        )
                    } else {
                        EmptyOrErrorCard(
                            loanAmountText = loanAmountText,
                            tenureText = tenureText,
                            shakeOffset = shakeOffset,
                            colors = colors,
                            onQuickPreset = { amount ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                loanAmountText = amount.toString()
                            }
                        )
                    }
                }

                // 3. INPUT CONTROLS SECTION
                item {
                    InputControlsCard(
                        loanAmountText = loanAmountText,
                        onLoanAmountChange = { loanAmountText = it },
                        interestRateText = interestRateText,
                        onInterestRateChange = { interestRateText = it },
                        tenureText = tenureText,
                        onTenureChange = { tenureText = it },
                        tenureUnit = tenureUnit,
                        onTenureUnitChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            tenureUnit = it
                        },
                        enableFee = enableFee,
                        onEnableFeeChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            enableFee = it
                        },
                        feeTypeText = feeTypeText,
                        onFeeTypeChange = { feeTypeText = it },
                        feeValueText = feeValueText,
                        onFeeValueChange = { feeValueText = it },
                        currency = selectedCurrency,
                        shakeOffset = shakeOffset,
                        colors = colors,
                        haptic = haptic
                    )
                }

                // 4. ACTION BUTTONS & UTILITIES (Copy, Share, Save History, Reset)
                item {
                    ActionButtonsRow(
                        result = result,
                        currency = selectedCurrency,
                        colors = colors,
                        onCopy = {
                            if (result != null) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val summary = """
                                    📊 ملخص القرض والتمويل ($selectedCurrency):
                                    • مبلغ القرض: ${formatCurrency(result.principal, selectedCurrency)}
                                    • الفائدة السنوية: ${result.annualRate}% (${interestMode.title})
                                    • المدة: ${result.totalMonths} شهر
                                    • القسط الشهري: ${formatCurrency(result.emi, selectedCurrency)}
                                    • إجمالي الفائدة: ${formatCurrency(result.totalInterest, selectedCurrency)}
                                    • إجمالي السداد: ${formatCurrency(result.totalPayment, selectedCurrency)}
                                """.trimIndent()
                                clipboardManager.setText(AnnotatedString(summary))
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("تم نسخ ملخص القرض إلى الحافظة بنجاح 📋")
                                }
                            }
                        },
                        onShare = {
                            if (result != null) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "تقرير القسط الشهري (${result.annualRate}% - ${result.totalMonths} شهر): " +
                                                "${formatCurrency(result.emi, selectedCurrency)} شهرياً | إجمالي السداد: ${formatCurrency(result.totalPayment, selectedCurrency)}"
                                    )
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "مشاركة تقرير القرض"))
                            }
                        },
                        onSaveHistory = {
                            if (result != null) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val newItem = LoanHistoryItem(
                                    principal = result.principal,
                                    annualRate = result.annualRate,
                                    months = result.totalMonths,
                                    mode = interestMode,
                                    currency = selectedCurrency,
                                    emi = result.emi,
                                    totalInterest = result.totalInterest,
                                    totalPayment = result.totalPayment
                                )
                                val updatedList = listOf(newItem) + historyItems.take(19)
                                saveHistoryList(updatedList)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("تمت إضافة الحسبة إلى سجل القروض 💾")
                                }
                            }
                        },
                        onReset = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            loanAmountText = "250000"
                            interestRateText = "12.5"
                            tenureText = "36"
                            tenureUnit = TenureUnit.MONTHS
                            interestMode = LoanInterestMode.REDUCING
                            enableFee = false
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("تم إعادة ضبط جميع الحقول إلى القيم الافتراضية")
                            }
                        }
                    )
                }

                // 5. EXPANDABLE SECTION: EARLY SETTLEMENT CALCULATOR
                item {
                    Column {
                        ExpandableSectionHeader(
                            title = "حاسبة السداد المبكر (التسوية المبكرة)",
                            subtitle = "احسب المبلغ المتبقي والتوفير في الفوائد عند السداد قبل الموعد",
                            icon = Icons.Default.Savings,
                            isExpanded = showEarlySettlement,
                            onToggle = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showEarlySettlement = !showEarlySettlement
                            },
                            colors = colors
                        )

                        AnimatedVisibility(
                            visible = showEarlySettlement,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            EarlySettlementCard(
                                paidMonthsText = paidMonthsText,
                                onPaidMonthsChange = { paidMonthsText = it },
                                settlementFeePercentText = settlementFeePercentText,
                                onSettlementFeeChange = { settlementFeePercentText = it },
                                earlySettlementResult = earlySettlementResult,
                                totalMonths = result?.totalMonths ?: 36,
                                currency = selectedCurrency,
                                colors = colors
                            )
                        }
                    }
                }

                // 6. EXPANDABLE SECTION: LOAN COMPARISON MODE
                item {
                    Column {
                        ExpandableSectionHeader(
                            title = "مقارنة القروض والتمويل (عرض مقارب)",
                            subtitle = "قارن بين خطتين للتمويل لاختيار الخيار الأقل تكلفة والفائدة",
                            icon = Icons.Default.Compare,
                            isExpanded = showComparisonMode,
                            onToggle = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showComparisonMode = !showComparisonMode
                            },
                            colors = colors
                        )

                        AnimatedVisibility(
                            visible = showComparisonMode,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            LoanComparisonCard(
                                scenario1Result = result,
                                scenario2PrincipalText = scenario2PrincipalText,
                                onScenario2PrincipalChange = { scenario2PrincipalText = it },
                                scenario2RateText = scenario2RateText,
                                onScenario2RateChange = { scenario2RateText = it },
                                scenario2MonthsText = scenario2MonthsText,
                                onScenario2MonthsChange = { scenario2MonthsText = it },
                                scenario2Result = scenario2Result,
                                currency = selectedCurrency,
                                colors = colors
                            )
                        }
                    }
                }

                // 7. EXPANDABLE SECTION: HISTORY LOG
                item {
                    Column {
                        ExpandableSectionHeader(
                            title = "سجل الحسابات السابقة (${historyItems.size})",
                            subtitle = "استعرض عمليات الحساب المحفوظة وأعد استخدامها بضغطة واحدة",
                            icon = Icons.Default.History,
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
                            HistoryLogSection(
                                historyItems = historyItems,
                                colors = colors,
                                onSelectHistoryItem = { item ->
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    loanAmountText = item.principal.toLong().toString()
                                    interestRateText = item.annualRate.toString()
                                    tenureText = item.months.toString()
                                    tenureUnit = TenureUnit.MONTHS
                                    interestMode = item.mode
                                    selectedCurrency = item.currency
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("تم تحميل بيانات القرض من السجل ⚡")
                                    }
                                },
                                onToggleFavorite = { itemId ->
                                    val updated = historyItems.map {
                                        if (it.id == itemId) it.copy(isFavorite = !it.isFavorite) else it
                                    }
                                    saveHistoryList(updated)
                                },
                                onClearHistory = {
                                    saveHistoryList(emptyList())
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("تم مسح سجل الحسابات المحفوظة")
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Snackbar Host at bottom
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }

        // AMORTIZATION SCHEDULE MODAL BOTTOM SHEET
        if (showAmortizationSheet && result != null) {
            ModalBottomSheet(
                onDismissRequest = { showAmortizationSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
                containerColor = Color(0xFF0F1422),
                scrimColor = Color.Black.copy(alpha = 0.6f)
            ) {
                AmortizationScheduleSheetContent(
                    result = result,
                    currency = selectedCurrency,
                    colors = colors,
                    onClose = { showAmortizationSheet = false }
                )
            }
        }
    }
}

// --- SUB-COMPOSABLES & ORGANISMS ---

@Composable
private fun HeroHeaderSection(
    colors: CustomThemeColors,
    selectedCurrency: String,
    onCurrencySelect: (String) -> Unit,
    interestMode: LoanInterestMode,
    onModeSelect: (LoanInterestMode) -> Unit
) {
    Surface(
        color = Color(0xFF141926).copy(alpha = 0.85f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Bar: Offline Tag & Currency Selector Dropdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Offline status badge
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
                            text = "جميع الحسابات تعمل أوفلاين",
                            fontSize = 11.sp,
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Currency Selector Chips
                val currencies = listOf("EGP", "SAR", "USD", "EUR", "AED", "KWD")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(currencies) { curr ->
                        val isSelected = curr == selectedCurrency
                        Surface(
                            color = if (isSelected) Color(0xFFD4AF37) else Color(0xFF1E2638),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFFF59E0B) else Color(0xFF334155)
                            ),
                            modifier = Modifier.clickable { onCurrencySelect(curr) }
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

            // Pulsing Glowing Bank Icon Header Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Procedural Bank Icon with Glowing Animation
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseGlow by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 0.7f,
                    animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Reverse),
                    label = "glow"
                )

                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFD4AF37).copy(alpha = pulseGlow),
                                    Color(0xFF0F1422)
                                )
                            )
                        )
                        .border(1.dp, Color(0xFFD4AF37).copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(28.dp)) {
                        val w = size.width
                        val h = size.height
                        // Bank Vault/Columns Graphic
                        drawRect(
                            color = Color(0xFFD4AF37),
                            topLeft = Offset(0f, h * 0.85f),
                            size = Size(w, h * 0.15f)
                        )
                        drawRect(
                            color = Color(0xFFF59E0B),
                            topLeft = Offset(0f, 0f),
                            size = Size(w, h * 0.15f)
                        )
                        val colWidth = w * 0.18f
                        drawRect(Color(0xFFD4AF37), Offset(w * 0.05f, h * 0.2f), Size(colWidth, h * 0.6f))
                        drawRect(Color(0xFFD4AF37), Offset(w * 0.41f, h * 0.2f), Size(colWidth, h * 0.6f))
                        drawRect(Color(0xFFD4AF37), Offset(w * 0.77f, h * 0.2f), Size(colWidth, h * 0.6f))
                    }
                }

                Column {
                    Text(
                        text = "حاسبة القروض والتمويل",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "الأقساط الشهرية، الفوائد المركبة، وجدول السداد",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Divider(color = Color(0xFF334155).copy(alpha = 0.5f))

            // Interest Mode Segmented Switcher (Reducing vs Flat)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0F1422))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                LoanInterestMode.values().forEach { mode ->
                    val isSelected = mode == interestMode
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onModeSelect(mode) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFFD4AF37) else Color.Transparent,
                        border = if (isSelected) BorderStroke(1.dp, Color(0xFFF59E0B)) else null
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = mode.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color(0xFF0F1422) else Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroResultCard(
    result: LoanCalcResult,
    currency: String,
    colors: CustomThemeColors,
    onShowAmortization: () -> Unit
) {
    val animatedEmi by animateFloatAsState(
        targetValue = result.emi.toFloat(),
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "emi"
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
                text = "القسط الشهري المتوقع (EMI)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8)
            )

            // Animated EMI Display
            Text(
                text = formatCurrency(animatedEmi.toDouble(), currency),
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFF59E0B)
            )

            Divider(color = Color(0xFF334155).copy(alpha = 0.5f))

            // Procedural Canvas Donut Chart Visualizer
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Donut Chart Canvas
                val principalRatio = (result.principal / result.totalPayment).toFloat()
                val interestRatio = (result.totalInterest / result.totalPayment).toFloat()

                val animatedPrincipalSweep by animateFloatAsState(
                    targetValue = principalRatio * 360f,
                    animationSpec = tween(800, easing = FastOutSlowInEasing),
                    label = "principalSweep"
                )
                val animatedInterestSweep by animateFloatAsState(
                    targetValue = interestRatio * 360f,
                    animationSpec = tween(800, easing = FastOutSlowInEasing),
                    label = "interestSweep"
                )

                Box(
                    modifier = Modifier.size(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 18.dp.toPx()
                        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                        val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                        // Principal Arc (Green)
                        drawArc(
                            color = Color(0xFF10B981),
                            startAngle = -90f,
                            sweepAngle = animatedPrincipalSweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        // Interest Arc (Red)
                        drawArc(
                            color = Color(0xFFEF4444),
                            startAngle = -90f + animatedPrincipalSweep,
                            sweepAngle = animatedInterestSweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${(principalRatio * 100).roundToInt()}%",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "أصل القرض",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                // Financial Breakdown Metrics
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f).padding(start = 16.dp)
                ) {
                    MetricBadgeRow(
                        label = "أصل مبلغ القرض",
                        value = formatCurrency(result.principal, currency),
                        badgeColor = Color(0xFF10B981)
                    )
                    MetricBadgeRow(
                        label = "إجمالي الفائدة",
                        value = formatCurrency(result.totalInterest, currency),
                        badgeColor = Color(0xFFEF4444)
                    )
                    if (result.processingFee > 0) {
                        MetricBadgeRow(
                            label = "المصاريف الإدارية",
                            value = formatCurrency(result.processingFee, currency),
                            badgeColor = Color(0xFF3B82F6)
                        )
                    }
                    MetricBadgeRow(
                        label = "إجمالي المبلغ المسدد",
                        value = formatCurrency(result.totalPayment, currency),
                        badgeColor = Color(0xFFF59E0B)
                    )
                }
            }

            // Amortization Table Button
            Button(
                onClick = onShowAmortization,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD4AF37),
                    contentColor = Color(0xFF0F1422)
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text(
                        text = "عرض جدول السداد الشهري التفصيلي",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricBadgeRow(
    label: String,
    value: String,
    badgeColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(badgeColor)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )
        }
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun InputControlsCard(
    loanAmountText: String,
    onLoanAmountChange: (String) -> Unit,
    interestRateText: String,
    onInterestRateChange: (String) -> Unit,
    tenureText: String,
    onTenureChange: (String) -> Unit,
    tenureUnit: TenureUnit,
    onTenureUnitChange: (TenureUnit) -> Unit,
    enableFee: Boolean,
    onEnableFeeChange: (Boolean) -> Unit,
    feeTypeText: String,
    onFeeTypeChange: (String) -> Unit,
    feeValueText: String,
    onFeeValueChange: (String) -> Unit,
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
                text = "بيانات القرض والتمويل",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            // 1. LOAN AMOUNT INPUT & PRESET CHIPS
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = loanAmountText,
                    onValueChange = onLoanAmountChange,
                    label = { Text("مبلغ القرض الأساسي ($currency)") },
                    trailingIcon = {
                        if (loanAmountText.isNotEmpty()) {
                            IconButton(onClick = { onLoanAmountChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "مسح", tint = Color(0xFF94A3B8))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFD4AF37),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Color(0xFFD4AF37),
                        unfocusedLabelColor = Color(0xFF94A3B8)
                    )
                )

                // Quick Presets
                val amountPresets = listOf(50000L, 100000L, 250000L, 500000L, 1000000L, 2500000L)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(amountPresets) { preset ->
                        Surface(
                            color = Color(0xFF1E2638),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier.clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onLoanAmountChange(preset.toString())
                            }
                        ) {
                            Text(
                                text = formatShortNumber(preset) + " " + currency,
                                fontSize = 11.sp,
                                color = Color(0xFFD4AF37),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Slider for loan amount
                val currentAmount = loanAmountText.toDoubleOrNull() ?: 0.0
                Slider(
                    value = currentAmount.coerceIn(10000.0, 5000000.0).toFloat(),
                    onValueChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onLoanAmountChange(it.toLong().toString())
                    },
                    valueRange = 10000f..5000000f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFD4AF37),
                        activeTrackColor = Color(0xFFD4AF37),
                        inactiveTrackColor = Color(0xFF1E2638)
                    )
                )
            }

            Divider(color = Color(0xFF334155).copy(alpha = 0.5f))

            // 2. INTEREST RATE INPUT & +/- FINE-TUNING
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = interestRateText,
                        onValueChange = onInterestRateChange,
                        label = { Text("معدل الفائدة السنوية (%)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFD4AF37),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = Color(0xFFD4AF37),
                            unfocusedLabelColor = Color(0xFF94A3B8)
                        )
                    )

                    // Fine-tuning +/- Buttons
                    val currentRate = interestRateText.toDoubleOrNull() ?: 12.0
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val newRate = max(0.25, currentRate - 0.25)
                            onInterestRateChange(String.format(Locale.US, "%.2f", newRate))
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E2638))
                    ) {
                        Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val newRate = currentRate + 0.25
                            onInterestRateChange(String.format(Locale.US, "%.2f", newRate))
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E2638))
                    ) {
                        Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                // Rate Slider
                val currentRate = interestRateText.toDoubleOrNull() ?: 12.0
                Slider(
                    value = currentRate.coerceIn(0.5, 30.0).toFloat(),
                    onValueChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onInterestRateChange(String.format(Locale.US, "%.1f", it))
                    },
                    valueRange = 0.5f..30.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFF59E0B),
                        activeTrackColor = Color(0xFFF59E0B),
                        inactiveTrackColor = Color(0xFF1E2638)
                    )
                )
            }

            Divider(color = Color(0xFF334155).copy(alpha = 0.5f))

            // 3. TENURE INPUT & UNIT TOGGLE (Months vs Years)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = tenureText,
                        onValueChange = onTenureChange,
                        label = { Text("مدة السداد (${tenureUnit.title})") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFD4AF37),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = Color(0xFFD4AF37),
                            unfocusedLabelColor = Color(0xFF94A3B8)
                        )
                    )

                    // Unit Toggle Switcher
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F1422))
                            .padding(4.dp)
                    ) {
                        TenureUnit.values().forEach { unit ->
                            val isSelected = unit == tenureUnit
                            Surface(
                                modifier = Modifier.clickable { onTenureUnitChange(unit) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(0xFFD4AF37) else Color.Transparent
                            ) {
                                Text(
                                    text = unit.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color(0xFF0F1422) else Color(0xFF94A3B8),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                // Quick Tenure Presets
                val tenurePresets = if (tenureUnit == TenureUnit.MONTHS) {
                    listOf("12", "24", "36", "60", "120", "240")
                } else {
                    listOf("1", "2", "3", "5", "10", "20")
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(tenurePresets) { tPreset ->
                        Surface(
                            color = Color(0xFF1E2638),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier.clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTenureChange(tPreset)
                            }
                        ) {
                            Text(
                                text = "$tPreset ${tenureUnit.title}",
                                fontSize = 11.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            Divider(color = Color(0xFF334155).copy(alpha = 0.5f))

            // 4. OPTIONAL PROCESSING FEE SWITCH & CARD
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Receipt, contentDescription = null, tint = Color(0xFF3B82F6))
                    Column {
                        Text("إضافة المصاريف الإدارية والتأمين", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("خصم رسوم المعاملة من أصل التمويل", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    }
                }

                Switch(
                    checked = enableFee,
                    onCheckedChange = onEnableFeeChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF3B82F6),
                        checkedTrackColor = Color(0xFF3B82F6).copy(alpha = 0.3f),
                        uncheckedThumbColor = Color(0xFF94A3B8),
                        uncheckedTrackColor = Color(0xFF1E2638)
                    )
                )
            }

            AnimatedVisibility(visible = enableFee) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = feeValueText,
                        onValueChange = onFeeValueChange,
                        label = { Text("قيمة الرسوم الإدارية") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButtonsRow(
    result: LoanCalcResult?,
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
        // Copy Summary
        OutlinedButton(
            onClick = onCopy,
            enabled = result != null,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.5f))
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFD4AF37))
            Spacer(modifier = Modifier.width(4.dp))
            Text("نسخ", fontSize = 12.sp, color = Color(0xFFD4AF37))
        }

        // Share Report
        OutlinedButton(
            onClick = onShare,
            enabled = result != null,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.5f))
        ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF3B82F6))
            Spacer(modifier = Modifier.width(4.dp))
            Text("مشاركة", fontSize = 12.sp, color = Color(0xFF3B82F6))
        }

        // Save to History
        OutlinedButton(
            onClick = onSaveHistory,
            enabled = result != null,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f))
        ) {
            Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF10B981))
            Spacer(modifier = Modifier.width(4.dp))
            Text("حفظ", fontSize = 12.sp, color = Color(0xFF10B981))
        }

        // Reset All Inputs
        OutlinedButton(
            onClick = onReset,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f))
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFEF4444))
            Spacer(modifier = Modifier.width(4.dp))
            Text("ضبط", fontSize = 12.sp, color = Color(0xFFEF4444))
        }
    }
}

@Composable
private fun ExpandableSectionHeader(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    colors: CustomThemeColors
) {
    Surface(
        color = Color(0xFF141926).copy(alpha = 0.85f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.25f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(icon, contentDescription = null, tint = Color(0xFFD4AF37), modifier = Modifier.size(22.dp))
                Column {
                    Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(text = subtitle, fontSize = 11.sp, color = Color(0xFF94A3B8))
                }
            }

            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = Color(0xFFD4AF37)
            )
        }
    }
}

@Composable
private fun EarlySettlementCard(
    paidMonthsText: String,
    onPaidMonthsChange: (String) -> Unit,
    settlementFeePercentText: String,
    onSettlementFeeChange: (String) -> Unit,
    earlySettlementResult: EarlySettlementData?,
    totalMonths: Int,
    currency: String,
    colors: CustomThemeColors
) {
    Surface(
        color = Color(0xFF141926).copy(alpha = 0.85f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = paidMonthsText,
                    onValueChange = onPaidMonthsChange,
                    label = { Text("عدد الأشهر المدفوعة حتى الآن") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = settlementFeePercentText,
                    onValueChange = onSettlementFeeChange,
                    label = { Text("عمولة السداد المبكر (%)") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }

            if (earlySettlementResult != null) {
                Divider(color = Color(0xFF334155).copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("رصيد أصل الدين المتبقي", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        Text(formatCurrency(earlySettlementResult.remainingPrincipal, currency), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("وفر الفوائد المحقق 🌟", fontSize = 11.sp, color = Color(0xFF10B981))
                        Text(formatCurrency(earlySettlementResult.interestSaved, currency), fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFF10B981))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("عمولة السداد المبكر", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        Text(formatCurrency(earlySettlementResult.settlementFee, currency), fontSize = 13.sp, color = Color(0xFFEF4444))
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("إجمالي المبلغ المطلوب لتسوية القرض", fontSize = 11.sp, color = Color(0xFFF59E0B))
                        Text(formatCurrency(earlySettlementResult.netPayoffAmount, currency), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFF59E0B))
                    }
                }
            } else {
                Text(
                    text = "أدخل عدد الأشهر المدفوعة (أقل من $totalMonths شهر) لعرض حسابات التسوية والتوفير",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}

@Composable
private fun LoanComparisonCard(
    scenario1Result: LoanCalcResult?,
    scenario2PrincipalText: String,
    onScenario2PrincipalChange: (String) -> Unit,
    scenario2RateText: String,
    onScenario2RateChange: (String) -> Unit,
    scenario2MonthsText: String,
    onScenario2MonthsChange: (String) -> Unit,
    scenario2Result: SimpleLoanResult?,
    currency: String,
    colors: CustomThemeColors
) {
    Surface(
        color = Color(0xFF141926).copy(alpha = 0.85f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("مدخلات الخيار الثاني للمقارنة", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3B82F6))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = scenario2PrincipalText,
                    onValueChange = onScenario2PrincipalChange,
                    label = { Text("المبلغ") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = scenario2RateText,
                    onValueChange = onScenario2RateChange,
                    label = { Text("الفائدة %") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                OutlinedTextField(
                    value = scenario2MonthsText,
                    onValueChange = onScenario2MonthsChange,
                    label = { Text("الشهور") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            if (scenario1Result != null && scenario2Result != null) {
                Divider(color = Color(0xFF334155).copy(alpha = 0.5f))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    // Scenario 1 Column
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("الخيار الحالي (1)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD4AF37))
                        Text("قسط: ${formatCurrency(scenario1Result.emi, currency)}", fontSize = 11.sp, color = Color.White)
                        Text("فائدة: ${formatCurrency(scenario1Result.totalInterest, currency)}", fontSize = 11.sp, color = Color(0xFFEF4444))
                    }

                    // Divider
                    Box(modifier = Modifier.width(1.dp).height(50.dp).background(Color(0xFF334155)))

                    // Scenario 2 Column
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("الخيار البديل (2)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3B82F6))
                        Text("قسط: ${formatCurrency(scenario2Result.emi, currency)}", fontSize = 11.sp, color = Color.White)
                        Text("فائدة: ${formatCurrency(scenario2Result.totalInterest, currency)}", fontSize = 11.sp, color = Color(0xFFEF4444))
                    }
                }

                // Comparison Verdict
                val diffEmi = scenario1Result.emi - scenario2Result.emi
                val diffInterest = scenario1Result.totalInterest - scenario2Result.totalInterest

                Surface(
                    color = if (diffInterest > 0) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (diffInterest > 0) {
                            "💡 الخيار الثاني يوفر ${formatCurrency(diffInterest, currency)} في إجمالي الفوائد!"
                        } else {
                            "💡 الخيار الأول يوفر ${formatCurrency(-diffInterest, currency)} في إجمالي الفوائد!"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (diffInterest > 0) Color(0xFF10B981) else Color(0xFFF59E0B),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryLogSection(
    historyItems: List<LoanHistoryItem>,
    colors: CustomThemeColors,
    onSelectHistoryItem: (LoanHistoryItem) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onClearHistory: () -> Unit
) {
    Surface(
        color = Color(0xFF141926).copy(alpha = 0.85f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
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
                Text("سجل القروض المحفوظة", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                if (historyItems.isNotEmpty()) {
                    TextButton(onClick = onClearHistory) {
                        Text("مسح السجل", fontSize = 11.sp, color = Color(0xFFEF4444))
                    }
                }
            }

            if (historyItems.isEmpty()) {
                Text(
                    text = "لا توجد حسابات قروض محفوظة بالسجل حالياً. اضغط على زر 'حفظ' بأعلى للتخزين.",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                )
            } else {
                historyItems.forEach { item ->
                    Surface(
                        color = Color(0xFF1E2638),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectHistoryItem(item) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${formatCurrency(item.principal, item.currency)} • ${item.annualRate}% (${item.months} شهر)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "قسط: ${formatCurrency(item.emi, item.currency)} | فائدة: ${formatCurrency(item.totalInterest, item.currency)}",
                                    fontSize = 11.sp,
                                    color = Color(0xFFD4AF37)
                                )
                            }

                            IconButton(onClick = { onToggleFavorite(item.id) }) {
                                Icon(
                                    imageVector = if (item.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                                    contentDescription = "مفضلة",
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

@Composable
private fun EmptyOrErrorCard(
    loanAmountText: String,
    tenureText: String,
    shakeOffset: Float,
    colors: CustomThemeColors,
    onQuickPreset: (Long) -> Unit
) {
    Surface(
        color = Color(0xFF141926).copy(alpha = 0.85f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth().offset(x = shakeOffset.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalance,
                contentDescription = null,
                tint = Color(0xFFD4AF37),
                modifier = Modifier.size(48.dp)
            )

            Text(
                text = "أدخل مبلغ القرض والمدة للبدء بالحساب",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "اختر أحد المبالغ السريعة أدناه لتجربة الحساب الفوري:",
                fontSize = 12.sp,
                color = Color(0xFF94A3B8)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onQuickPreset(100000L) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2638))
                ) {
                    Text("100,000", color = Color(0xFFD4AF37))
                }

                Button(
                    onClick = { onQuickPreset(250000L) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2638))
                ) {
                    Text("250,000", color = Color(0xFFD4AF37))
                }

                Button(
                    onClick = { onQuickPreset(500000L) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2638))
                ) {
                    Text("500,000", color = Color(0xFFD4AF37))
                }
            }
        }
    }
}

@Composable
private fun AmortizationScheduleSheetContent(
    result: LoanCalcResult,
    currency: String,
    colors: CustomThemeColors,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "جدول السداد الشهري التفصيلي (${result.totalMonths} شهر)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
            }
        }

        // Table Sticky Header
        Surface(
            color = Color(0xFF1E2638),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("#", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD4AF37), modifier = Modifier.weight(0.6f))
                Text("القسط", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD4AF37), modifier = Modifier.weight(1.2f))
                Text("الأصل", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981), modifier = Modifier.weight(1.2f))
                Text("الفائدة", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444), modifier = Modifier.weight(1.2f))
                Text("الرصيد", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), modifier = Modifier.weight(1.4f))
            }
        }

        // LazyColumn with month by month breakdown
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(
                items = result.amortizationRows,
                key = { it.monthNumber }
            ) { row ->
                Surface(
                    color = if (row.monthNumber % 2 == 0) Color(0xFF141926) else Color(0xFF0F1422),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${row.monthNumber}", fontSize = 11.sp, color = Color.White, modifier = Modifier.weight(0.6f))
                        Text(formatCurrency(row.payment, currency), fontSize = 10.sp, color = Color.White, modifier = Modifier.weight(1.2f))
                        Text(formatCurrency(row.principalPaid, currency), fontSize = 10.sp, color = Color(0xFF10B981), modifier = Modifier.weight(1.2f))
                        Text(formatCurrency(row.interestPaid, currency), fontSize = 10.sp, color = Color(0xFFEF4444), modifier = Modifier.weight(1.2f))
                        Text(formatCurrency(row.remainingBalance, currency), fontSize = 10.sp, color = Color(0xFF94A3B8), modifier = Modifier.weight(1.4f))
                    }
                }
            }
        }

        // Footer Summary
        Surface(
            color = Color(0xFF1E2638),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "الإجمالي: ${formatCurrency(result.totalPayment, currency)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF59E0B)
                )

                Text(
                    text = "فوائد: ${formatCurrency(result.totalInterest, currency)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEF4444)
                )
            }
        }
    }
}

// --- CALCULATION LOGIC & HELPER FUNCTIONS ---

data class LoanCalcResult(
    val principal: Double,
    val annualRate: Double,
    val totalMonths: Int,
    val emi: Double,
    val totalInterest: Double,
    val totalPayment: Double,
    val processingFee: Double,
    val netDisbursed: Double,
    val amortizationRows: List<AmortizationRow>
)

data class EarlySettlementData(
    val paidMonths: Int,
    val remainingPrincipal: Double,
    val interestSaved: Double,
    val settlementFee: Double,
    val netPayoffAmount: Double
)

data class SimpleLoanResult(
    val emi: Double,
    val totalInterest: Double,
    val totalPayment: Double
)

private fun calculateSimpleLoan(
    principal: Double,
    annualRate: Double,
    months: Int,
    mode: LoanInterestMode
): SimpleLoanResult {
    val monthlyRate = (annualRate / 100.0) / 12.0
    val emi: Double
    val totalPayment: Double
    val totalInterest: Double

    if (mode == LoanInterestMode.REDUCING) {
        if (monthlyRate > 0) {
            emi = (principal * monthlyRate * (1 + monthlyRate).pow(months.toDouble())) /
                    ((1 + monthlyRate).pow(months.toDouble()) - 1)
        } else {
            emi = principal / months
        }
        totalPayment = emi * months
        totalInterest = max(0.0, totalPayment - principal)
    } else {
        totalInterest = principal * (annualRate / 100.0) * (months / 12.0)
        totalPayment = principal + totalInterest
        emi = totalPayment / months
    }

    return SimpleLoanResult(emi, totalInterest, totalPayment)
}

private fun formatCurrency(amount: Double, currency: String): String {
    return LivePricesRepository.formatNumber(amount) + " " + currency
}

private fun formatShortNumber(amount: Long): String {
    return if (amount >= 1000000) {
        "${amount / 1000000}M"
    } else if (amount >= 1000) {
        "${amount / 1000}k"
    } else {
        amount.toString()
    }
}

private fun parseHistoryJson(json: String): List<LoanHistoryItem> {
    if (json.isBlank() || json == "[]") return emptyList()
    return try {
        val list = mutableListOf<LoanHistoryItem>()
        val array = org.json.JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                LoanHistoryItem(
                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                    principal = obj.optDouble("principal", 0.0),
                    annualRate = obj.optDouble("annualRate", 0.0),
                    months = obj.optInt("months", 0),
                    mode = if (obj.optString("mode") == "FLAT") LoanInterestMode.FLAT else LoanInterestMode.REDUCING,
                    currency = obj.optString("currency", "EGP"),
                    emi = obj.optDouble("emi", 0.0),
                    totalInterest = obj.optDouble("totalInterest", 0.0),
                    totalPayment = obj.optDouble("totalPayment", 0.0),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                    isFavorite = obj.optBoolean("isFavorite", false)
                )
            )
        }
        list
    } catch (e: Exception) {
        emptyList()
    }
}

private fun serializeHistoryJson(items: List<LoanHistoryItem>): String {
    val array = org.json.JSONArray()
    items.forEach { item ->
        val obj = org.json.JSONObject()
        obj.put("id", item.id)
        obj.put("principal", item.principal)
        obj.put("annualRate", item.annualRate)
        obj.put("months", item.months)
        obj.put("mode", item.mode.name)
        obj.put("currency", item.currency)
        obj.put("emi", item.emi)
        obj.put("totalInterest", item.totalInterest)
        obj.put("totalPayment", item.totalPayment)
        obj.put("timestamp", item.timestamp)
        obj.put("isFavorite", item.isFavorite)
        array.put(obj)
    }
    return array.toString()
}
