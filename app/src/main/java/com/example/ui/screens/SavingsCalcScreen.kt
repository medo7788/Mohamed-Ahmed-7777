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
import kotlin.math.*
import kotlin.random.Random

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

data class SavingsGoal(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val targetAmount: Double,
    val currentSaved: Double,
    val category: String, // e.g. "سيارة", "منزل", "سفر", "تعليم", "طوارئ", "استثمار", "هدية"
    val targetDays: Int = 180,
    val createdAt: Long = System.currentTimeMillis()
)

data class DepositYieldResult(
    val principal: BigDecimal,
    val grossReturn: BigDecimal,
    val netReturn: BigDecimal,
    val netProfit: BigDecimal,
    val roiPercentage: BigDecimal,
    val realValueInflationAdjusted: BigDecimal,
    val monthlyPayout: BigDecimal,
    val taxAmount: BigDecimal,
    val feeAmount: BigDecimal,
    val isValid: Boolean,
    val errorMessage: String? = null
)

data class SavingsHistoryItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: String,
    val type: String, // "وديعة" or "هدف"
    val title: String,
    val amount: String,
    val note: String,
    val currency: String
)

// ==========================================
// MAIN COMPOSABLE SCREEN
// ==========================================

@Composable
fun SavingsCalcScreen(colors: CustomThemeColors) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current

    // State Persistence
    var depositPrincipalStr by rememberSaveable { mutableStateOf("100000") }
    var interestRateStr by rememberSaveable { mutableStateOf("18.5") }
    var tenureValStr by rememberSaveable { mutableStateOf("3") }
    var isTenureInYears by rememberSaveable { mutableStateOf(true) } // true: Years, false: Months
    var payoutFrequency by rememberSaveable { mutableStateOf("شهري") } // "شهري", "ربع سنوي", "سنوي"
    var taxPercentStr by rememberSaveable { mutableStateOf("0") }
    var isTaxSectionExpanded by rememberSaveable { mutableStateOf(false) }
    var selectedCurrency by rememberSaveable { mutableStateOf("EGP") }

    // Goals Data
    var savingsGoals by remember { mutableStateOf(loadSavingsGoalsFromPrefs(context)) }
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    var quickDepositGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    var quickDepositAmountStr by remember { mutableStateOf("1000") }

    // History Log Data
    var historyList by remember { mutableStateOf(loadSavingsHistoryFromPrefs(context)) }
    var isHistoryExpanded by rememberSaveable { mutableStateOf(false) }

    // Toast Message
    var toastMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2500)
            toastMessage = null
        }
    }

    // Confetti Effect Trigger State
    var triggerConfetti by remember { mutableStateOf(false) }

    // Reactive Yield Engine Calculation
    val yieldResult by remember(
        depositPrincipalStr, interestRateStr, tenureValStr, isTenureInYears,
        payoutFrequency, taxPercentStr
    ) {
        derivedStateOf {
            calculateDepositYieldEngine(
                principalInput = depositPrincipalStr,
                rateInput = interestRateStr,
                tenureInput = tenureValStr,
                isYears = isTenureInYears,
                frequency = payoutFrequency,
                taxInput = taxPercentStr
            )
        }
    }

    // Aggregate Goals Stats
    val totalTargetSavings by remember(savingsGoals) {
        derivedStateOf { savingsGoals.sumOf { it.targetAmount } }
    }
    val totalCurrentSavings by remember(savingsGoals) {
        derivedStateOf { savingsGoals.sumOf { it.currentSaved } }
    }
    val overallSavingsProgress by remember(totalTargetSavings, totalCurrentSavings) {
        derivedStateOf {
            if (totalTargetSavings > 0) (totalCurrentSavings / totalTargetSavings).coerceIn(0.0, 1.0) else 0.0
        }
    }

    ToolScreenScaffold(
        colors = colors,
        icon = AppIcons.forCalc(CalcKey.SAVINGS),
        title = "حاسبة الادخار وأهداف الاستثمار",
        subtitle = "توقع أرباح الودائع، خطط لأهدافك المالية وتابع نمو ثروتك بدقة",
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
            // Background Canvas Vault / Wealth Grid Pattern
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

            // Confetti Burst Layer
            if (triggerConfetti) {
                ConfettiBurstCanvas(
                    modifier = Modifier.fillMaxSize(),
                    onFinished = { triggerConfetti = false }
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Toast Banner
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

                // SECTION A1: Currency & Title Header
                item(key = "header_section") {
                    SavingsHeaderCard(
                        selectedCurrency = selectedCurrency,
                        onCurrencySelected = {
                            selectedCurrency = it
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    )
                }

                // SECTION A2: Deposit Yield Inputs Panel
                item(key = "deposit_inputs_panel") {
                    DepositInputsPanel(
                        principalStr = depositPrincipalStr,
                        onPrincipalChange = { depositPrincipalStr = it },
                        interestRateStr = interestRateStr,
                        onInterestRateChange = { interestRateStr = it },
                        tenureValStr = tenureValStr,
                        onTenureValChange = { tenureValStr = it },
                        isTenureInYears = isTenureInYears,
                        onToggleTenureUnit = {
                            isTenureInYears = !isTenureInYears
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        payoutFrequency = payoutFrequency,
                        onPayoutFrequencyChange = {
                            payoutFrequency = it
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        taxPercentStr = taxPercentStr,
                        onTaxPercentChange = { taxPercentStr = it },
                        isTaxExpanded = isTaxSectionExpanded,
                        onToggleTax = { isTaxSectionExpanded = !isTaxSectionExpanded },
                        currency = selectedCurrency
                    )
                }

                // SECTION A3: Deposit Live Yield Results
                item(key = "yield_results_panel") {
                    YieldResultsPanel(
                        result = yieldResult,
                        currency = selectedCurrency,
                        tenureText = "$tenureValStr ${if (isTenureInYears) "سنوات" else "أشهر"}",
                        payoutFrequency = payoutFrequency,
                        onSaveDepositToHistory = {
                            if (!yieldResult.isValid) return@YieldResultsPanel
                            val timeStr = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm - yyyy/MM/dd"))
                            val newItem = SavingsHistoryItem(
                                timestamp = timeStr,
                                type = "استثمار وديعة",
                                title = "مبلغ ${yieldResult.principal.toPlainString()} $selectedCurrency بفائدة $interestRateStr%",
                                amount = "+${yieldResult.netProfit.toPlainString()} $selectedCurrency (أرباح)",
                                note = "عائد $payoutFrequency على مدار $tenureValStr ${if (isTenureInYears) "سنوات" else "أشهر"}",
                                currency = selectedCurrency
                            )
                            historyList = listOf(newItem) + historyList.take(19)
                            saveSavingsHistoryToPrefs(context, historyList)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            toastMessage = "تم حفظ تقدير الوديعة في السجل"
                        },
                        onCopyReport = {
                            val shareText = formatYieldReport(yieldResult, interestRateStr, tenureValStr, isTenureInYears, payoutFrequency, selectedCurrency)
                            clipboardManager.setText(AnnotatedString(shareText))
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            toastMessage = "تم نسخ تقرير الأرباح للحافظة 📋"
                        },
                        onShare = {
                            val shareText = formatYieldReport(yieldResult, interestRateStr, tenureValStr, isTenureInYears, payoutFrequency, selectedCurrency)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(intent, "مشاركة تقرير الاستثمار"))
                        }
                    )
                }

                // SECTION B1: Savings Goals Overview Header & Canvas Wealth Gauge
                item(key = "goals_header_section") {
                    GoalsOverviewHeaderCard(
                        totalSaved = totalCurrentSavings,
                        totalTarget = totalTargetSavings,
                        progressRatio = overallSavingsProgress,
                        goalsCount = savingsGoals.size,
                        currency = selectedCurrency,
                        onAddNewGoalClick = {
                            editingGoal = null
                            showAddGoalDialog = true
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    )
                }

                // SECTION B2: Savings Goals List
                if (savingsGoals.isEmpty()) {
                    item(key = "empty_goals_state") {
                        EmptyGoalsCard(onAddGoal = {
                            editingGoal = null
                            showAddGoalDialog = true
                        })
                    }
                } else {
                    items(savingsGoals, key = { it.id }) { goal ->
                        SavingsGoalItemCard(
                            goal = goal,
                            currency = selectedCurrency,
                            onQuickDepositClick = {
                                quickDepositGoal = goal
                                quickDepositAmountStr = "1000"
                            },
                            onEditClick = {
                                editingGoal = goal
                                showAddGoalDialog = true
                            },
                            onDeleteClick = {
                                savingsGoals = savingsGoals.filter { it.id != goal.id }
                                saveSavingsGoalsToPrefs(context, savingsGoals)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                toastMessage = "تم حذف الهدف"
                            }
                        )
                    }
                }

                // SECTION C: History Log
                if (historyList.isNotEmpty()) {
                    item(key = "history_section") {
                        ExpandableCard(
                            title = "سجل النشاط والعمليات (${historyList.size})",
                            icon = Icons.Outlined.History,
                            isExpanded = isHistoryExpanded,
                            onToggle = { isHistoryExpanded = !isHistoryExpanded }
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                historyList.forEach { hist ->
                                    HistoryLogItemRow(
                                        item = hist,
                                        onDelete = {
                                            historyList = historyList.filter { it.id != hist.id }
                                            saveSavingsHistoryToPrefs(context, historyList)
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
                        Text("جميع أهدافك وحساباتك تحفظ محلياً على جهازك بأمان تام", fontSize = 11.sp, color = ColorSlateMuted)
                    }
                }
            }

            // Add/Edit Goal Modal Sheet / Dialog
            if (showAddGoalDialog) {
                AddEditGoalDialog(
                    initialGoal = editingGoal,
                    onDismiss = { showAddGoalDialog = false },
                    onSave = { newGoal ->
                        val existingIndex = savingsGoals.indexOfFirst { it.id == newGoal.id }
                        val wasCompletedBefore = existingIndex >= 0 && savingsGoals[existingIndex].currentSaved >= savingsGoals[existingIndex].targetAmount
                        val isNowCompleted = newGoal.currentSaved >= newGoal.targetAmount

                        if (!wasCompletedBefore && isNowCompleted) {
                            triggerConfetti = true
                        }

                        if (existingIndex >= 0) {
                            val mutable = savingsGoals.toMutableList()
                            mutable[existingIndex] = newGoal
                            savingsGoals = mutable
                        } else {
                            savingsGoals = listOf(newGoal) + savingsGoals
                        }
                        saveSavingsGoalsToPrefs(context, savingsGoals)
                        showAddGoalDialog = false
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        toastMessage = if (editingGoal == null) "تم إضافة هدف جديد بنجاح 🎉" else "تم تحديث بيانات الهدف"
                    }
                )
            }

            // Quick Deposit Dialog
            if (quickDepositGoal != null) {
                val goal = quickDepositGoal!!
                QuickDepositDialog(
                    goal = goal,
                    amountStr = quickDepositAmountStr,
                    onAmountChange = { quickDepositAmountStr = it },
                    currency = selectedCurrency,
                    onDismiss = { quickDepositGoal = null },
                    onConfirmDeposit = { addAmount ->
                        val updatedGoal = goal.copy(currentSaved = goal.currentSaved + addAmount)
                        val existingIndex = savingsGoals.indexOfFirst { it.id == goal.id }
                        if (existingIndex >= 0) {
                            val mutable = savingsGoals.toMutableList()
                            mutable[existingIndex] = updatedGoal
                            savingsGoals = mutable
                            saveSavingsGoalsToPrefs(context, savingsGoals)
                        }

                        // Save history entry
                        val timeStr = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm - yyyy/MM/dd"))
                        val newItem = SavingsHistoryItem(
                            timestamp = timeStr,
                            type = "إيداع للهدف",
                            title = "إيداع في ${goal.title}",
                            amount = "+${addAmount.toInt()} $selectedCurrency",
                            note = "إجمالي المدخر بالهدف: ${updatedGoal.currentSaved.toInt()} / ${goal.targetAmount.toInt()} $selectedCurrency",
                            currency = selectedCurrency
                        )
                        historyList = listOf(newItem) + historyList.take(19)
                        saveSavingsHistoryToPrefs(context, historyList)

                        if (updatedGoal.currentSaved >= goal.targetAmount) {
                            triggerConfetti = true
                            toastMessage = "مبروك! اكتمل الهدف بنجاح 🎉🏆"
                        } else {
                            toastMessage = "تم إضافة المبلغ لرصيد الهدف 💰"
                        }

                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        quickDepositGoal = null
                    }
                )
            }
        }
    }
}

// ==========================================
// SUB-COMPONENTS
// ==========================================

@Composable
private fun SavingsHeaderCard(
    selectedCurrency: String,
    onCurrencySelected: (String) -> Unit
) {
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
            Box(
                modifier = Modifier
                    .size(52.dp)
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

                    // Vault Piggy Bank contour
                    val path = Path().apply {
                        moveTo(w * 0.2f, h * 0.5f)
                        cubicTo(w * 0.2f, h * 0.25f, w * 0.8f, h * 0.25f, w * 0.8f, h * 0.5f)
                        cubicTo(w * 0.85f, h * 0.65f, w * 0.75f, h * 0.85f, w * 0.5f, h * 0.85f)
                        cubicTo(w * 0.25f, h * 0.85f, w * 0.15f, h * 0.65f, w * 0.2f, h * 0.5f)
                    }
                    drawPath(path, color = ColorGoldBorder, style = Stroke(width = 3f))

                    // Coin slot line
                    drawLine(ColorIceCyan, Offset(w * 0.4f, h * 0.3f), Offset(w * 0.6f, h * 0.3f), strokeWidth = 3f)
                    // Coin falling
                    drawCircle(ColorAmberGlow, radius = 4f, center = Offset(w * 0.5f, h * 0.2f))
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "لوحة الادخار والاستثمار الذكي",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "حساب الفوائد المركبة وتتبع الأهداف",
                    fontSize = 11.sp,
                    color = ColorSlateMuted
                )
            }

            // Currency Switcher Chips
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
private fun DepositInputsPanel(
    principalStr: String,
    onPrincipalChange: (String) -> Unit,
    interestRateStr: String,
    onInterestRateChange: (String) -> Unit,
    tenureValStr: String,
    onTenureValChange: (String) -> Unit,
    isTenureInYears: Boolean,
    onToggleTenureUnit: () -> Unit,
    payoutFrequency: String,
    onPayoutFrequencyChange: (String) -> Unit,
    taxPercentStr: String,
    onTaxPercentChange: (String) -> Unit,
    isTaxExpanded: Boolean,
    onToggleTax: () -> Unit,
    currency: String
) {
    Surface(
        color = ColorGlassCard,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, ColorGoldBorder.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("حاسبة أرباح الودائع والشهادات", fontSize = 14.sp, color = ColorAmberGlow, fontWeight = FontWeight.Bold)

            // Principal Field + Quick Increment Chips
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("مبلغ الوديعة ($currency)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)

                OutlinedTextField(
                    value = principalStr,
                    onValueChange = onPrincipalChange,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        if (principalStr.isNotEmpty()) {
                            IconButton(onClick = { onPrincipalChange("") }) {
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

                // Quick Increment Chips
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(10000, 50000, 100000, 500000).forEach { inc ->
                        Surface(
                            color = Color(0xFF1E2638),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                            modifier = Modifier.clickable {
                                val current = principalStr.toDoubleOrNull() ?: 0.0
                                onPrincipalChange((current + inc).toLong().toString())
                            }
                        ) {
                            Text(
                                "+${inc / 1000}k",
                                fontSize = 10.sp,
                                color = ColorIceCyan,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Interest Rate & Tenure Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Interest Rate Field
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("الفائدة السنوية (%)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                    OutlinedTextField(
                        value = interestRateStr,
                        onValueChange = onInterestRateChange,
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

                // Tenure Field + Unit Toggle
                Column(modifier = Modifier.weight(1.2f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("المدة الزمنية", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                        // Toggle Unit Button
                        Surface(
                            color = ColorAmberGlow.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, ColorAmberGlow),
                            modifier = Modifier.clickable { onToggleTenureUnit() }
                        ) {
                            Text(
                                if (isTenureInYears) "سنوات 🔄" else "أشهر 🔄",
                                fontSize = 10.sp,
                                color = ColorAmberGlow,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = tenureValStr,
                        onValueChange = onTenureValChange,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ColorAmberGlow,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            }

            // Payout Frequency Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("دورية صرف العائد", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("شهري", "ربع سنوي", "سنوي", "في نهاية المدة").forEach { freq ->
                        val isSelected = freq == payoutFrequency
                        Surface(
                            color = if (isSelected) ColorEmeraldGreen.copy(alpha = 0.25f) else Color(0xFF1E2638),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (isSelected) ColorEmeraldGreen else Color.White.copy(alpha = 0.15f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onPayoutFrequencyChange(freq) }
                        ) {
                            Text(
                                freq,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) ColorEmeraldGreen else Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // Expandable Tax & Fees Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleTax() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Percent, contentDescription = null, tint = ColorSlateMuted, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إعدادات الضرائب والاستقطاعات (اختياري)", fontSize = 12.sp, color = ColorSlateMuted)
                }
                Icon(
                    if (isTaxExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = ColorSlateMuted
                )
            }

            if (isTaxExpanded) {
                OutlinedTextField(
                    value = taxPercentStr,
                    onValueChange = onTaxPercentChange,
                    label = { Text("نسبة الضريبة على الفوائد (%)", fontSize = 10.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ColorCrimsonRed,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        }
    }
}

@Composable
private fun YieldResultsPanel(
    result: DepositYieldResult,
    currency: String,
    tenureText: String,
    payoutFrequency: String,
    onSaveDepositToHistory: () -> Unit,
    onCopyReport: () -> Unit,
    onShare: () -> Unit
) {
    Surface(
        color = ColorGlassCard,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.5.dp, ColorGoldBorder.copy(alpha = 0.4f)),
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
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ColorCrimsonRed)
                ) {
                    Text(
                        result.errorMessage ?: "تأكد من إدخال قيم صالحة للمبلغ والمدة والفائدة",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                // Main Highlight Total Return
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("إجمالي المستحق في نهاية المدة ($tenureText)", color = ColorSlateMuted, fontSize = 12.sp)
                    Text(
                        "${result.netReturn.toPlainString()} $currency",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = ColorAmberGlow
                    )
                }

                Divider(color = Color.White.copy(alpha = 0.1f))

                // Stats Grid Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("صافي الربح المحقق", fontSize = 11.sp, color = ColorSlateMuted)
                        Text(
                            "+${result.netProfit.toPlainString()} $currency",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorIceCyan
                        )
                        Surface(
                            color = ColorIceCyan.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                "+${result.roiPercentage.toPlainString()}% ROI",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorIceCyan,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("الدخل ($payoutFrequency)", fontSize = 11.sp, color = ColorSlateMuted)
                        Text(
                            "${result.monthlyPayout.toPlainString()} $currency",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorEmeraldGreen
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("القيمة بعد التضخم (3%)", fontSize = 11.sp, color = ColorSlateMuted)
                        Text(
                            "~${result.realValueInflationAdjusted.toPlainString()} $currency",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                Divider(color = Color.White.copy(alpha = 0.1f))

                // Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onSaveDepositToHistory,
                        colors = ButtonDefaults.buttonColors(containerColor = ColorAmberGlow),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Icon(Icons.Filled.BookmarkAdd, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("حفظ بالسجل", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }

                    OutlinedButton(
                        onClick = onCopyReport,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ColorGoldBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = ColorGoldBorder, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("نسخ 📋", fontSize = 11.sp, color = ColorGoldBorder)
                    }

                    OutlinedButton(
                        onClick = onShare,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ColorIceCyan),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, tint = ColorIceCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مشاركة", fontSize = 11.sp, color = ColorIceCyan)
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalsOverviewHeaderCard(
    totalSaved: Double,
    totalTarget: Double,
    progressRatio: Double,
    goalsCount: Int,
    currency: String,
    onAddNewGoalClick: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progressRatio.toFloat(),
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "animatedProgress"
    )

    Surface(
        color = ColorGlassCard,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, ColorGoldBorder.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("أهداف الادخار المخصصة", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("$goalsCount أهداف ادخار نشطة", fontSize = 11.sp, color = ColorSlateMuted)
                }

                Button(
                    onClick = onAddNewGoalClick,
                    colors = ButtonDefaults.buttonColors(containerColor = ColorAmberGlow),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("هدف جديد +", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Procedural Canvas Circular Wealth Gauge
                Box(
                    modifier = Modifier.size(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 12.dp.toPx()
                        val diameter = size.minDimension - stroke
                        val topPx = stroke / 2
                        val leftPx = stroke / 2

                        // Track Arc
                        drawArc(
                            color = Color(0xFF1E2638),
                            startAngle = 135f,
                            sweepAngle = 270f,
                            useCenter = false,
                            topLeft = Offset(leftPx, topPx),
                            size = Size(diameter, diameter),
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )

                        // Progress Arc
                        drawArc(
                            brush = Brush.horizontalGradient(listOf(ColorIceCyan, ColorEmeraldGreen, ColorAmberGlow)),
                            startAngle = 135f,
                            sweepAngle = 270f * animatedProgress,
                            useCenter = false,
                            topLeft = Offset(leftPx, topPx),
                            size = Size(diameter, diameter),
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${(animatedProgress * 100).toInt()}%",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = ColorAmberGlow
                        )
                        Text("الإنجاز", fontSize = 9.sp, color = ColorSlateMuted)
                    }
                }

                // Summary Numbers
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("إجمالي المدخر:", fontSize = 12.sp, color = ColorSlateMuted)
                        Text("${formatCompactNumber(totalSaved)} $currency", fontSize = 12.sp, color = ColorEmeraldGreen, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("إجمالي المستهدف:", fontSize = 12.sp, color = ColorSlateMuted)
                        Text("${formatCompactNumber(totalTarget)} $currency", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("المبلغ المتبقي:", fontSize = 12.sp, color = ColorSlateMuted)
                        Text("${formatCompactNumber(max(0.0, totalTarget - totalSaved))} $currency", fontSize = 12.sp, color = ColorIceCyan, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SavingsGoalItemCard(
    goal: SavingsGoal,
    currency: String,
    onQuickDepositClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val progress = if (goal.targetAmount > 0) (goal.currentSaved / goal.targetAmount).coerceIn(0.0, 1.0) else 0.0
    val isCompleted = progress >= 1.0
    val remainingAmount = max(0.0, goal.targetAmount - goal.currentSaved)
    val dailyRequired = if (goal.targetDays > 0) remainingAmount / goal.targetDays else 0.0

    val animatedProgress by animateFloatAsState(
        targetValue = progress.toFloat(),
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "goalProgress"
    )

    Surface(
        color = ColorGlassCard,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, if (isCompleted) ColorAmberGlow else ColorGoldBorder.copy(alpha = 0.25f)),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryIconCanvas(category = goal.category)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(goal.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(goal.category, fontSize = 10.sp, color = ColorSlateMuted)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Filled.Edit, contentDescription = "تعديل", tint = ColorSlateMuted, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(Icons.Filled.Delete, contentDescription = "حذف", tint = ColorCrimsonRed, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "تم جمع ${formatCompactNumber(goal.currentSaved)} من ${formatCompactNumber(goal.targetAmount)} $currency",
                        fontSize = 11.sp,
                        color = Color.White
                    )
                    Text("${(progress * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isCompleted) ColorAmberGlow else ColorIceCyan)
                }

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (isCompleted) ColorAmberGlow else ColorEmeraldGreen,
                    trackColor = Color(0xFF1E2638)
                )
            }

            if (isCompleted) {
                Surface(
                    color = ColorAmberGlow.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ColorAmberGlow),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("🎉 اكتمل الهدف بنجاح! مبروك إنجازك المالي 🏆", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ColorAmberGlow)
                    }
                }
            } else {
                // Smart Recommendation Pill
                Surface(
                    color = ColorIceCyan.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(0.5.dp, ColorIceCyan.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "💡 أضف ~${dailyRequired.roundToInt()} $currency/يوم للوصول بموعدك",
                            fontSize = 10.sp,
                            color = ColorIceCyan
                        )

                        Button(
                            onClick = onQuickDepositClick,
                            colors = ButtonDefaults.buttonColors(containerColor = ColorEmeraldGreen),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("إيداع +", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryIconCanvas(category: String) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color(0xFF1E2638)),
        contentAlignment = Alignment.Center
    ) {
        val icon = when (category) {
            "سيارة" -> Icons.Filled.DirectionsCar
            "منزل" -> Icons.Filled.Home
            "سفر" -> Icons.Filled.Flight
            "تعليم" -> Icons.Filled.School
            "طوارئ" -> Icons.Filled.HealthAndSafety
            "استثمار" -> Icons.Filled.TrendingUp
            "هدية" -> Icons.Filled.CardGiftcard
            else -> Icons.Filled.Savings
        }
        Icon(icon, contentDescription = category, tint = ColorGoldBorder, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun EmptyGoalsCard(onAddGoal: () -> Unit) {
    Surface(
        color = ColorGlassCard,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, ColorGoldBorder.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Canvas(modifier = Modifier.size(60.dp)) {
                drawCircle(ColorGoldBorder.copy(alpha = 0.2f), radius = size.width / 2)
                drawCircle(ColorAmberGlow, radius = size.width / 4, center = center)
            }

            Text("ابدأ رحلة ادخارك الآن", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(
                "لم تقم بإضافة أي هدف ادخار بعد. اضف هدفك الأول لتتبع المدخرات يومياً!",
                fontSize = 11.sp,
                color = ColorSlateMuted,
                textAlign = TextAlign.Center
            )

            Button(
                onClick = onAddGoal,
                colors = ButtonDefaults.buttonColors(containerColor = ColorAmberGlow),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("إضافة هدف ادخار جديد +", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }
    }
}

@Composable
private fun AddEditGoalDialog(
    initialGoal: SavingsGoal?,
    onDismiss: () -> Unit,
    onSave: (SavingsGoal) -> Unit
) {
    var title by remember { mutableStateOf(initialGoal?.title ?: "") }
    var targetStr by remember { mutableStateOf(initialGoal?.targetAmount?.toLong()?.toString() ?: "50000") }
    var savedStr by remember { mutableStateOf(initialGoal?.currentSaved?.toLong()?.toString() ?: "0") }
    var selectedCategory by remember { mutableStateOf(initialGoal?.category ?: "طوارئ") }
    var daysStr by remember { mutableStateOf(initialGoal?.targetDays?.toString() ?: "180") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF121620),
        title = {
            Text(
                if (initialGoal == null) "إضافة هدف ادخار جديد" else "تعديل هدف الادخار",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("اسم الهدف (مثال: شراء سيارة)", fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ColorGoldBorder,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = targetStr,
                    onValueChange = { targetStr = it },
                    label = { Text("المبلغ المستهدف الإجمالي", fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ColorGoldBorder,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = savedStr,
                    onValueChange = { savedStr = it },
                    label = { Text("المبلغ المدخر حالياً", fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ColorGoldBorder,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = daysStr,
                    onValueChange = { daysStr = it },
                    label = { Text("المدة المستهدفة (بالأيام)", fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ColorGoldBorder,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Text("فئة الهدف:", fontSize = 11.sp, color = ColorSlateMuted)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val categories = listOf("طوارئ", "سيارة", "منزل", "سفر", "تعليم", "استثمار", "هدية", "أخرى")
                    items(categories, key = { it }) { cat ->
                        val isSel = cat == selectedCategory
                        Surface(
                            color = if (isSel) ColorAmberGlow.copy(alpha = 0.25f) else Color(0xFF1E2638),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isSel) ColorAmberGlow else Color.White.copy(alpha = 0.15f)),
                            modifier = Modifier.clickable { selectedCategory = cat }
                        ) {
                            Text(
                                cat,
                                fontSize = 10.sp,
                                color = if (isSel) ColorAmberGlow else Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) return@Button
                    val target = targetStr.toDoubleOrNull() ?: 1000.0
                    val saved = savedStr.toDoubleOrNull() ?: 0.0
                    val days = daysStr.toIntOrNull() ?: 30

                    val newGoal = SavingsGoal(
                        id = initialGoal?.id ?: java.util.UUID.randomUUID().toString(),
                        title = title,
                        targetAmount = target,
                        currentSaved = saved,
                        category = selectedCategory,
                        targetDays = days
                    )
                    onSave(newGoal)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ColorAmberGlow)
            ) {
                Text("حفظ الهدف", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = ColorSlateMuted)
            }
        }
    )
}

@Composable
private fun QuickDepositDialog(
    goal: SavingsGoal,
    amountStr: String,
    onAmountChange: (String) -> Unit,
    currency: String,
    onDismiss: () -> Unit,
    onConfirmDeposit: (Double) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF121620),
        title = {
            Text("إيداع مبلغ في: ${goal.title}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "الرصيد الحالي: ${formatCompactNumber(goal.currentSaved)} / ${formatCompactNumber(goal.targetAmount)} $currency",
                    fontSize = 11.sp,
                    color = ColorSlateMuted
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = onAmountChange,
                    label = { Text("مبلغ الإيداع الجديد ($currency)", fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ColorEmeraldGreen,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                // Presets
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(100, 500, 1000, 5000).forEach { preset ->
                        Surface(
                            color = Color(0xFF1E2638),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                            modifier = Modifier.clickable { onAmountChange(preset.toString()) }
                        ) {
                            Text(
                                "+$preset",
                                fontSize = 10.sp,
                                color = ColorEmeraldGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val addVal = amountStr.toDoubleOrNull() ?: 0.0
                    if (addVal > 0) {
                        onConfirmDeposit(addVal)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ColorEmeraldGreen)
            ) {
                Text("تأكيد الإيداع 💰", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = ColorSlateMuted)
            }
        }
    )
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
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, ColorGoldBorder.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = ColorGoldBorder, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Icon(
                    if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = ColorSlateMuted
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                content()
            }
        }
    }
}

@Composable
private fun HistoryLogItemRow(
    item: SavingsHistoryItem,
    onDelete: () -> Unit
) {
    Surface(
        color = Color(0xFF1E2638),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = ColorAmberGlow.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(item.type, fontSize = 9.sp, color = ColorAmberGlow, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(item.timestamp, fontSize = 9.sp, color = ColorSlateMuted)
                }
                Text(item.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(item.amount, fontSize = 11.sp, color = ColorIceCyan, fontWeight = FontWeight.Bold)
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Close, contentDescription = "حذف", tint = ColorSlateMuted, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun ConfettiBurstCanvas(
    modifier: Modifier = Modifier,
    onFinished: () -> Unit
) {
    var animProgress by remember { mutableStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 2000, easing = LinearOutSlowInEasing),
        finishedListener = { onFinished() },
        label = "confettiAnim"
    )
    LaunchedEffect(Unit) { animProgress = 1f }

    val particles = remember {
        List(60) {
            val angle = Random.nextFloat() * 2 * PI.toFloat()
            val speed = Random.nextFloat() * 400f + 100f
            val color = listOf(ColorAmberGlow, ColorIceCyan, ColorEmeraldGreen, ColorGoldBorder, Color(0xFFFF4081)).random()
            val radius = Random.nextFloat() * 6f + 3f
            Triple(Offset(cos(angle) * speed, sin(angle) * speed), color, radius)
        }
    }

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 3)
        particles.forEach { (dir, color, radius) ->
            val currentPos = center + Offset(dir.x * animatedProgress, dir.y * animatedProgress + (animatedProgress * 200f))
            drawCircle(
                color = color.copy(alpha = (1f - animatedProgress).coerceIn(0f, 1f)),
                radius = radius,
                center = currentPos
            )
        }
    }
}

// ==========================================
// MATHEMATICAL ENGINE & PREFS HELPERS
// ==========================================

private fun calculateDepositYieldEngine(
    principalInput: String,
    rateInput: String,
    tenureInput: String,
    isYears: Boolean,
    frequency: String,
    taxInput: String
): DepositYieldResult {
    val p = principalInput.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val r = rateInput.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val t = tenureInput.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val taxRate = taxInput.toBigDecimalOrNull() ?: BigDecimal.ZERO

    if (p <= BigDecimal.ZERO || r < BigDecimal.ZERO || t <= BigDecimal.ZERO) {
        return DepositYieldResult(
            principal = p, grossReturn = BigDecimal.ZERO, netReturn = BigDecimal.ZERO,
            netProfit = BigDecimal.ZERO, roiPercentage = BigDecimal.ZERO,
            realValueInflationAdjusted = BigDecimal.ZERO, monthlyPayout = BigDecimal.ZERO,
            taxAmount = BigDecimal.ZERO, feeAmount = BigDecimal.ZERO,
            isValid = false, errorMessage = "رجاءً أدخل مبلغ ومدة ونسبة فائدة صالحة"
        )
    }

    val totalYears = if (isYears) t else t.divide(BigDecimal(12), 4, RoundingMode.HALF_UP)
    val totalMonths = if (isYears) t.multiply(BigDecimal(12)) else t

    // Simple / Compound interest formula
    val grossProfit = p.multiply(r).multiply(totalYears).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
    val taxVal = grossProfit.multiply(taxRate).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
    val netProfit = grossProfit.subtract(taxVal)
    val netReturn = p.add(netProfit)

    val roi = if (p > BigDecimal.ZERO) netProfit.multiply(BigDecimal(100)).divide(p, 2, RoundingMode.HALF_UP) else BigDecimal.ZERO

    val payoutIntervalMonths = when (frequency) {
        "شهري" -> BigDecimal(1)
        "ربع سنوي" -> BigDecimal(3)
        "سنوي" -> BigDecimal(12)
        else -> totalMonths
    }

    val payoutsCount = if (payoutIntervalMonths > BigDecimal.ZERO) totalMonths.divide(payoutIntervalMonths, 2, RoundingMode.HALF_UP) else BigDecimal.ONE
    val monthlyPayout = if (payoutsCount > BigDecimal.ZERO) netProfit.divide(payoutsCount, 2, RoundingMode.HALF_UP) else netProfit

    // Inflation adjustment factor (~3% per year)
    val inflationDiscountFactor = (1.0 / (1.0 + 0.03).pow(totalYears.toDouble()))
    val realValue = netReturn.multiply(BigDecimal(inflationDiscountFactor)).setScale(2, RoundingMode.HALF_UP)

    return DepositYieldResult(
        principal = p.setScale(2, RoundingMode.HALF_UP),
        grossReturn = p.add(grossProfit).setScale(2, RoundingMode.HALF_UP),
        netReturn = netReturn.setScale(2, RoundingMode.HALF_UP),
        netProfit = netProfit.setScale(2, RoundingMode.HALF_UP),
        roiPercentage = roi,
        realValueInflationAdjusted = realValue,
        monthlyPayout = monthlyPayout.setScale(2, RoundingMode.HALF_UP),
        taxAmount = taxVal.setScale(2, RoundingMode.HALF_UP),
        feeAmount = BigDecimal.ZERO,
        isValid = true
    )
}

private fun formatYieldReport(
    result: DepositYieldResult,
    interestRateStr: String,
    tenureValStr: String,
    isYears: Boolean,
    frequency: String,
    currency: String
): String {
    return """
        📊 تقرير دراسة أرباح الوديعة / الشهادة الاستثمارية
        ------------------------------------------
        • رأس المال المستثمر: ${result.principal.toPlainString()} $currency
        • الفائدة السنوية: $interestRateStr%
        • مدة الوديعة: $tenureValStr ${if (isYears) "سنوات" else "أشهر"}
        • دورية صرف العائد: $frequency
        ------------------------------------------
        💰 إجمالي العائد والأرباح:
        • صافي الأرباح المحققة: +${result.netProfit.toPlainString()} $currency (+${result.roiPercentage.toPlainString()}% ROI)
        • دورية الصرف ($frequency): ${result.monthlyPayout.toPlainString()} $currency
        • إجمالي المبلغ النهائي: ${result.netReturn.toPlainString()} $currency
        • القيمة التقديرية الحقيقية بعد التضخم: ~${result.realValueInflationAdjusted.toPlainString()} $currency
        ------------------------------------------
        تطبيق الحاسبة المالية - ClevCalc Intelligence
    """.trimIndent()
}

private fun formatCompactNumber(number: Double): String {
    return when {
        number >= 1_000_000 -> String.format("%.1fM", number / 1_000_000)
        number >= 1_000 -> String.format("%.1fk", number / 1_000)
        else -> number.toInt().toString()
    }
}

private const val GOALS_PREFS_NAME = "savings_dashboard_goals"
private const val HISTORY_PREFS_NAME = "savings_dashboard_history"

private fun saveSavingsGoalsToPrefs(context: Context, list: List<SavingsGoal>) {
    val prefs = context.getSharedPreferences(GOALS_PREFS_NAME, Context.MODE_PRIVATE)
    val array = JSONArray()
    list.forEach { item ->
        val obj = JSONObject().apply {
            put("id", item.id)
            put("title", item.title)
            put("targetAmount", item.targetAmount)
            put("currentSaved", item.currentSaved)
            put("category", item.category)
            put("targetDays", item.targetDays)
            put("createdAt", item.createdAt)
        }
        array.put(obj)
    }
    prefs.edit().putString("goals_json", array.toString()).apply()
}

private fun loadSavingsGoalsFromPrefs(context: Context): List<SavingsGoal> {
    val prefs = context.getSharedPreferences(GOALS_PREFS_NAME, Context.MODE_PRIVATE)
    val json = prefs.getString("goals_json", null) ?: return listOf(
        SavingsGoal(title = "صندوق الطوارئ", targetAmount = 50000.0, currentSaved = 35000.0, category = "طوارئ", targetDays = 120),
        SavingsGoal(title = "شراء سيارة جديدة", targetAmount = 250000.0, currentSaved = 110000.0, category = "سيارة", targetDays = 365)
    )
    return try {
        val array = JSONArray(json)
        val list = mutableListOf<SavingsGoal>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                SavingsGoal(
                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                    title = obj.getString("title"),
                    targetAmount = obj.getDouble("targetAmount"),
                    currentSaved = obj.getDouble("currentSaved"),
                    category = obj.optString("category", "طوارئ"),
                    targetDays = obj.optInt("targetDays", 180),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }
        list
    } catch (e: Exception) {
        emptyList()
    }
}

private fun saveSavingsHistoryToPrefs(context: Context, list: List<SavingsHistoryItem>) {
    val prefs = context.getSharedPreferences(HISTORY_PREFS_NAME, Context.MODE_PRIVATE)
    val array = JSONArray()
    list.forEach { item ->
        val obj = JSONObject().apply {
            put("id", item.id)
            put("timestamp", item.timestamp)
            put("type", item.type)
            put("title", item.title)
            put("amount", item.amount)
            put("note", item.note)
            put("currency", item.currency)
        }
        array.put(obj)
    }
    prefs.edit().putString("history_json", array.toString()).apply()
}

private fun loadSavingsHistoryFromPrefs(context: Context): List<SavingsHistoryItem> {
    val prefs = context.getSharedPreferences(HISTORY_PREFS_NAME, Context.MODE_PRIVATE)
    val json = prefs.getString("history_json", null) ?: return emptyList()
    return try {
        val array = JSONArray(json)
        val list = mutableListOf<SavingsHistoryItem>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                SavingsHistoryItem(
                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                    timestamp = obj.getString("timestamp"),
                    type = obj.getString("type"),
                    title = obj.getString("title"),
                    amount = obj.getString("amount"),
                    note = obj.optString("note", ""),
                    currency = obj.optString("currency", "EGP")
                )
            )
        }
        list
    } catch (e: Exception) {
        emptyList()
    }
}
