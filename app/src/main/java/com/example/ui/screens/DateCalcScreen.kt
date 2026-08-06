package com.example.ui.screens

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
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
import java.time.*
import java.time.chrono.HijrahChronology
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale

// ==========================================
// COLOR PALETTE & DATA MODELS
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

enum class DateCalcMode(val title: String, val icon: String) {
    ADD_SUBTRACT("إضافة / طرح أيام", "➕➖"),
    DATE_DIFF("الفرق بين تاريخين", "📅"),
    BUSINESS_DAYS("أيام العمل الرسمية", "💼"),
    HIJRI_CONVERT("تحويل هجري / ميلادي", "🌙"),
    AGE_CALC("حاسبة العمر الشاملة", "🎂")
}

enum class DurationUnit(val labelAr: String) {
    DAYS("أيام"),
    WEEKS("أسابيع"),
    MONTHS("أشهر"),
    YEARS("سنوات")
}

data class BusinessDateResult(
    val targetDate: LocalDate,
    val totalCalendarDays: Int,
    val weekendDaysCount: Int,
    val holidayDaysCount: Int
)

data class DateHistoryRecord(
    val id: String = java.util.UUID.randomUUID().toString(),
    val modeTitle: String,
    val inputSummary: String,
    val resultSummary: String,
    val timestamp: Long = System.currentTimeMillis(),
    var isFavorite: Boolean = false
)

private val hijriMonthNamesAr = listOf(
    "محرم", "صفر", "ربيع الأول", "ربيع الثاني",
    "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان",
    "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
)

private val gregorianMonthNamesAr = listOf(
    "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
    "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
)

private val arabicDayNames = mapOf(
    DayOfWeek.MONDAY to "الإثنين",
    DayOfWeek.TUESDAY to "الثلاثاء",
    DayOfWeek.WEDNESDAY to "الأربعاء",
    DayOfWeek.THURSDAY to "الخميس",
    DayOfWeek.FRIDAY to "الجمعة",
    DayOfWeek.SATURDAY to "السبت",
    DayOfWeek.SUNDAY to "الأحد"
)

// ==========================================
// MAIN COMPOSABLE SCREEN
// ==========================================

@Composable
fun DateCalcScreen(colors: CustomThemeColors) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current

    // ------------------------------------------
    // Core State Variables
    // ------------------------------------------
    var currentMode by rememberSaveable { mutableStateOf(DateCalcMode.ADD_SUBTRACT) }
    var selectedDateFormat by rememberSaveable { mutableStateOf("YYYY/MM/DD") }

    // Date A (Start Date)
    var startYear by rememberSaveable { mutableStateOf(LocalDate.now().year) }
    var startMonth by rememberSaveable { mutableStateOf(LocalDate.now().monthValue) }
    var startDay by rememberSaveable { mutableStateOf(LocalDate.now().dayOfMonth) }

    val startDate = remember(startYear, startMonth, startDay) {
        try { LocalDate.of(startYear, startMonth, startDay) } catch (e: Exception) { LocalDate.now() }
    }

    // Date B (End Date / Secondary Date)
    var endYear by rememberSaveable { mutableStateOf(LocalDate.now().plusDays(30).year) }
    var endMonth by rememberSaveable { mutableStateOf(LocalDate.now().plusDays(30).monthValue) }
    var endDay by rememberSaveable { mutableStateOf(LocalDate.now().plusDays(30).dayOfMonth) }

    val endDate = remember(endYear, endMonth, endDay) {
        try { LocalDate.of(endYear, endMonth, endDay) } catch (e: Exception) { LocalDate.now().plusDays(30) }
    }

    // Mode 1: Add / Subtract Settings
    var isAdditionMode by rememberSaveable { mutableStateOf(true) }
    var durationValueInput by rememberSaveable { mutableStateOf("30") }
    var selectedDurationUnit by rememberSaveable { mutableStateOf(DurationUnit.DAYS) }

    // Mode 3: Business Days Settings
    var businessDaysInput by rememberSaveable { mutableStateOf("10") }
    var weekendType by rememberSaveable { mutableStateOf("FRI_SAT") } // FRI_SAT or SAT_SUN
    var holidayDates by remember { mutableStateOf<List<LocalDate>>(emptyList()) }

    // Mode 4: Hijri Convert Inputs
    var hijriYearInput by rememberSaveable { mutableStateOf("1448") }
    var hijriMonthInput by rememberSaveable { mutableStateOf(3) } // 1..12
    var hijriDayInput by rememberSaveable { mutableStateOf(20) }

    // Expandable Drawers & Sections
    var isBreakdownExpanded by rememberSaveable { mutableStateOf(true) }
    var isExplanationExpanded by rememberSaveable { mutableStateOf(false) }
    var isRecurringExpanded by rememberSaveable { mutableStateOf(false) }
    var isHistoryExpanded by rememberSaveable { mutableStateOf(false) }

    // Recurring Events State
    var repeatFrequency by rememberSaveable { mutableStateOf("MONTHLY") } // DAILY, WEEKLY, MONTHLY, YEARLY
    var repeatCount by rememberSaveable { mutableStateOf(5) }

    // History Log
    var historyRecords by remember { mutableStateOf(loadDateHistoryFromPrefs(context)) }

    // Pickers Dialogs
    var activeDatePickerTarget by remember { mutableStateOf<String?>(null) } // "START", "END", "HOLIDAY"
    var toastMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2500)
            toastMessage = null
        }
    }

    // ------------------------------------------
    // Date Calculations Engine Logic
    // ------------------------------------------
    val durationVal = durationValueInput.toIntOrNull() ?: 0
    val targetCalculatedDate = remember(startDate, isAdditionMode, durationVal, selectedDurationUnit, currentMode) {
        if (currentMode != DateCalcMode.ADD_SUBTRACT) startDate
        else {
            if (isAdditionMode) {
                when (selectedDurationUnit) {
                    DurationUnit.DAYS -> startDate.plusDays(durationVal.toLong())
                    DurationUnit.WEEKS -> startDate.plusWeeks(durationVal.toLong())
                    DurationUnit.MONTHS -> startDate.plusMonths(durationVal.toLong())
                    DurationUnit.YEARS -> startDate.plusYears(durationVal.toLong())
                }
            } else {
                when (selectedDurationUnit) {
                    DurationUnit.DAYS -> startDate.minusDays(durationVal.toLong())
                    DurationUnit.WEEKS -> startDate.minusWeeks(durationVal.toLong())
                    DurationUnit.MONTHS -> startDate.minusMonths(durationVal.toLong())
                    DurationUnit.YEARS -> startDate.minusYears(durationVal.toLong())
                }
            }
        }
    }

    // Business Days Engine Result
    val businessDaysCount = businessDaysInput.toIntOrNull() ?: 0
    val businessDateResult = remember(startDate, businessDaysCount, isAdditionMode, weekendType, holidayDates, currentMode) {
        if (currentMode == DateCalcMode.BUSINESS_DAYS) {
            calculateBusinessDate(
                startDate = startDate,
                daysCount = businessDaysCount,
                isAdd = isAdditionMode,
                weekendType = weekendType,
                holidays = holidayDates
            )
        } else null
    }

    // Mode 4 Hijri to Gregorian / Gregorian to Hijri Result
    val convertedGregorianFromHijri = remember(hijriYearInput, hijriMonthInput, hijriDayInput) {
        val y = hijriYearInput.toIntOrNull() ?: 1448
        val m = hijriMonthInput.coerceIn(1, 12)
        val d = hijriDayInput.coerceIn(1, 30)
        hijriToGregorian(y, m, d)
    }

    // Determine Final Primary Target Date for Display
    val primaryDisplayDate: LocalDate = when (currentMode) {
        DateCalcMode.ADD_SUBTRACT -> targetCalculatedDate
        DateCalcMode.DATE_DIFF -> endDate
        DateCalcMode.BUSINESS_DAYS -> businessDateResult?.targetDate ?: startDate
        DateCalcMode.HIJRI_CONVERT -> convertedGregorianFromHijri
        DateCalcMode.AGE_CALC -> LocalDate.now()
    }

    // Relative Days Difference
    val relativeDays = remember(startDate, primaryDisplayDate) {
        ChronoUnit.DAYS.between(startDate, primaryDisplayDate)
    }

    // Date Picker Launcher Handler
    if (activeDatePickerTarget != null) {
        val initY = if (activeDatePickerTarget == "END") endYear else startYear
        val initM = if (activeDatePickerTarget == "END") endMonth else startMonth
        val initD = if (activeDatePickerTarget == "END") endDay else startDay

        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                if (activeDatePickerTarget == "START") {
                    startYear = year
                    startMonth = month + 1
                    startDay = dayOfMonth
                } else if (activeDatePickerTarget == "END") {
                    endYear = year
                    endMonth = month + 1
                    endDay = dayOfMonth
                } else if (activeDatePickerTarget == "HOLIDAY") {
                    val newHoliday = LocalDate.of(year, month + 1, dayOfMonth)
                    if (!holidayDates.contains(newHoliday)) {
                        holidayDates = holidayDates + newHoliday
                    }
                }
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                activeDatePickerTarget = null
            },
            initY,
            initM - 1,
            initD
        ).apply {
            setOnDismissListener { activeDatePickerTarget = null }
            show()
        }
    }

    // Main Tool Scaffold with inner LazyColumn
    ToolScreenScaffold(
        colors = colors,
        icon = AppIcons.forCalc(CalcKey.DATE),
        title = "حاسبة التاريخ الذكية",
        subtitle = "حساب الفروق الزمانية، التواريخ المستقبلية، وإضافة أيام العمل بصلابة مع تحويل هجري/ميلادي دقيق",
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
            // Procedural Background Grid Drawing
            Canvas(modifier = Modifier.fillMaxSize()) {
                val step = 64.dp.toPx()
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
                // Toast Feedback
                if (toastMessage != null) {
                    item(key = "toast_message") {
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

                // SECTION A: Header & Mode Selector
                item(key = "section_header") {
                    HeaderAndFormatCard(
                        selectedFormat = selectedDateFormat,
                        onFormatChange = {
                            selectedDateFormat = it
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    )
                }

                item(key = "section_mode_chips") {
                    ModeSelectorChips(
                        currentMode = currentMode,
                        onModeSelected = {
                            currentMode = it
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    )
                }

                // SECTION B: Dynamic Input Panel based on active mode
                item(key = "section_input_panel") {
                    DynamicInputPanel(
                        currentMode = currentMode,
                        startDate = startDate,
                        endDate = endDate,
                        isAdditionMode = isAdditionMode,
                        durationValueInput = durationValueInput,
                        selectedDurationUnit = selectedDurationUnit,
                        businessDaysInput = businessDaysInput,
                        weekendType = weekendType,
                        holidayDates = holidayDates,
                        hijriYearInput = hijriYearInput,
                        hijriMonthInput = hijriMonthInput,
                        hijriDayInput = hijriDayInput,
                        selectedDateFormat = selectedDateFormat,
                        onOpenDatePicker = { target -> activeDatePickerTarget = target },
                        onResetStartDateToToday = {
                            startYear = LocalDate.now().year
                            startMonth = LocalDate.now().monthValue
                            startDay = LocalDate.now().dayOfMonth
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            toastMessage = "تم ضبط تاريخ البدء إلى اليوم"
                        },
                        onToggleAdditionMode = {
                            isAdditionMode = !isAdditionMode
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDurationValueChange = { durationValueInput = it },
                        onDurationUnitChange = { selectedDurationUnit = it },
                        onBusinessDaysInputChange = { businessDaysInput = it },
                        onWeekendTypeChange = { weekendType = it },
                        onAddHolidayClick = { activeDatePickerTarget = "HOLIDAY" },
                        onRemoveHolidayClick = { date ->
                            holidayDates = holidayDates.filter { it != date }
                        },
                        onHijriYearChange = { hijriYearInput = it },
                        onHijriMonthChange = { hijriMonthInput = it },
                        onHijriDayChange = { hijriDayInput = it },
                        onSwapDatesClick = {
                            val tempY = startYear; val tempM = startMonth; val tempD = startDay
                            startYear = endYear; startMonth = endMonth; startDay = endDay
                            endYear = tempY; endMonth = tempM; endDay = tempD
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            toastMessage = "تم التبادل بين التاريخين"
                        }
                    )
                }

                // SECTION C: Live Target Result Display
                item(key = "section_result_display") {
                    PrimaryResultCard(
                        currentMode = currentMode,
                        startDate = startDate,
                        displayDate = primaryDisplayDate,
                        endDate = endDate,
                        relativeDays = relativeDays,
                        isAdditionMode = isAdditionMode,
                        selectedDateFormat = selectedDateFormat,
                        businessDaysResult = businessDateResult,
                        hijriYearInput = hijriYearInput,
                        hijriMonthInput = hijriMonthInput,
                        hijriDayInput = hijriDayInput
                    )
                }

                // SECTION C.2: Time Breakdown Matrix
                item(key = "section_breakdown_matrix") {
                    ExpandableCard(
                        title = "المصفوفة الزمنية التفصيلية (Multi-Unit Breakdown)",
                        icon = Icons.Outlined.Analytics,
                        isExpanded = isBreakdownExpanded,
                        onToggle = { isBreakdownExpanded = !isBreakdownExpanded }
                    ) {
                        TimeBreakdownGrid(
                            startDate = startDate,
                            targetDate = primaryDisplayDate
                        )
                    }
                }

                // SECTION C.3: Action Buttons Bar
                item(key = "section_action_buttons") {
                    ActionButtonsRow(
                        onExportCalendar = {
                            val title = "موعد: ${currentMode.title}"
                            val desc = "محسوب بواسطة حاسبة التاريخ الذكية. التاريخ: ${formatDateWithPattern(primaryDisplayDate, selectedDateFormat)}"
                            exportToSystemCalendar(context, title, primaryDisplayDate, desc)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            toastMessage = "تم فتح التقويم لإضافة الموعد"
                        },
                        onCopyText = {
                            val summary = buildSummaryReportText(
                                mode = currentMode,
                                startDate = startDate,
                                targetDate = primaryDisplayDate,
                                dateFormat = selectedDateFormat,
                                isAdd = isAdditionMode,
                                durationVal = durationVal,
                                durationUnit = selectedDurationUnit,
                                businessResult = businessDateResult
                            )
                            clipboardManager.setText(AnnotatedString(summary))
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            toastMessage = "تم نسخ التقرير بالتفصيل للحافظة"
                        },
                        onShareReport = {
                            val summary = buildSummaryReportText(
                                mode = currentMode,
                                startDate = startDate,
                                targetDate = primaryDisplayDate,
                                dateFormat = selectedDateFormat,
                                isAdd = isAdditionMode,
                                durationVal = durationVal,
                                durationUnit = selectedDurationUnit,
                                businessResult = businessDateResult
                            )
                            shareDateReport(context, summary)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onSaveHistory = {
                            val inputStr = "${formatDateWithPattern(startDate, selectedDateFormat)} (${currentMode.title})"
                            val resultStr = formatDateWithPattern(primaryDisplayDate, selectedDateFormat)
                            val record = DateHistoryRecord(
                                modeTitle = currentMode.title,
                                inputSummary = inputStr,
                                resultSummary = resultStr
                            )
                            historyRecords = (listOf(record) + historyRecords).take(20)
                            saveDateHistoryToPrefs(context, historyRecords)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            toastMessage = "تم حفظ العملية في السجل"
                        },
                        onResetAll = {
                            startYear = LocalDate.now().year
                            startMonth = LocalDate.now().monthValue
                            startDay = LocalDate.now().dayOfMonth
                            endYear = LocalDate.now().plusDays(30).year
                            endMonth = LocalDate.now().plusDays(30).monthValue
                            endDay = LocalDate.now().plusDays(30).dayOfMonth
                            durationValueInput = "30"
                            businessDaysInput = "10"
                            holidayDates = emptyList()
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            toastMessage = "تمت إعادة الضبط للقيم الافتراضية"
                        }
                    )
                }

                // SECTION D: Step-by-Step Mathematical Explanation
                item(key = "section_step_explanation") {
                    ExpandableCard(
                        title = "الشرح والخطوات الحسابية (Step-by-Step Math)",
                        icon = Icons.Outlined.Functions,
                        isExpanded = isExplanationExpanded,
                        onToggle = { isExplanationExpanded = !isExplanationExpanded }
                    ) {
                        StepByStepExplanationContent(
                            mode = currentMode,
                            startDate = startDate,
                            targetDate = primaryDisplayDate,
                            endDate = endDate,
                            isAdd = isAdditionMode,
                            durationVal = durationVal,
                            durationUnit = selectedDurationUnit,
                            businessResult = businessDateResult
                        )
                    }
                }

                // SECTION E: Recurring Events Calculator
                item(key = "section_recurring_events") {
                    ExpandableCard(
                        title = "حاسبة التكرار والمناسبات الدورية (Recurring Generator)",
                        icon = Icons.Outlined.Repeat,
                        isExpanded = isRecurringExpanded,
                        onToggle = { isRecurringExpanded = !isRecurringExpanded }
                    ) {
                        RecurringEventsContent(
                            baseDate = primaryDisplayDate,
                            frequency = repeatFrequency,
                            repeatCount = repeatCount,
                            dateFormat = selectedDateFormat,
                            onFrequencyChange = { repeatFrequency = it },
                            onCountChange = { repeatCount = it }
                        )
                    }
                }

                // SECTION F: Calculation History Drawer
                item(key = "section_history_drawer") {
                    ExpandableCard(
                        title = "سجل العمليات المحفوظة (${historyRecords.size})",
                        icon = Icons.Outlined.History,
                        isExpanded = isHistoryExpanded,
                        onToggle = { isHistoryExpanded = !isHistoryExpanded }
                    ) {
                        HistoryDrawerContent(
                            historyList = historyRecords,
                            onClearHistory = {
                                historyRecords = emptyList()
                                saveDateHistoryToPrefs(context, emptyList())
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                toastMessage = "تم مسح السجل بالكامل"
                            },
                            onToggleFavorite = { recordId ->
                                historyRecords = historyRecords.map {
                                    if (it.id == recordId) it.copy(isFavorite = !it.isFavorite) else it
                                }
                                saveDateHistoryToPrefs(context, historyRecords)
                            }
                        )
                    }
                }

                // Offline & Guarantee Footer
                item(key = "section_footer") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = ColorEmeraldGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("جميع الحسابات تعمل أوفلاين 100% بدقة زمنية متناهية", fontSize = 11.sp, color = ColorSlateMuted)
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
private fun HeaderAndFormatCard(
    selectedFormat: String,
    onFormatChange: (String) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
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
            // Animated Canvas Calendar & Clock Icon
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
                    // Calendar Body
                    drawRoundRect(
                        color = ColorGoldBorder,
                        topLeft = Offset(w * 0.15f, h * 0.25f),
                        size = androidx.compose.ui.geometry.Size(w * 0.7f, h * 0.65f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f),
                        style = Stroke(width = 4f)
                    )
                    // Calendar Top Header
                    drawLine(
                        color = ColorAmberGlow,
                        start = Offset(w * 0.15f, h * 0.45f),
                        end = Offset(w * 0.85f, h * 0.45f),
                        strokeWidth = 4f
                    )
                    // Rings
                    drawCircle(ColorIceCyan, radius = 3f, center = Offset(w * 0.35f, h * 0.2f))
                    drawCircle(ColorIceCyan, radius = 3f, center = Offset(w * 0.65f, h * 0.2f))
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "محرك الذكاء الزمني $20 Pro",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "حساب التواريخ والأيام والتقويم الهجري وأيام العمل",
                    fontSize = 11.sp,
                    color = ColorSlateMuted
                )
            }

            // Date Format Switcher Menu
            var formatExpanded by remember { mutableStateOf(false) }
            Box {
                Surface(
                    color = Color(0xFF1E2638),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ColorIceCyan.copy(alpha = 0.4f)),
                    modifier = Modifier.clickable { formatExpanded = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(selectedFormat, fontSize = 11.sp, color = ColorIceCyan, fontWeight = FontWeight.Bold)
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = ColorIceCyan, modifier = Modifier.size(16.dp))
                    }
                }

                DropdownMenu(
                    expanded = formatExpanded,
                    onDismissRequest = { formatExpanded = false },
                    modifier = Modifier.background(Color(0xFF181E2C))
                ) {
                    listOf("YYYY/MM/DD", "DD/MM/YYYY", "MM/DD/YYYY", "FULL_TEXT").forEach { fmt ->
                        DropdownMenuItem(
                            text = { Text(fmt, color = Color.White, fontSize = 12.sp) },
                            onClick = {
                                onFormatChange(fmt)
                                formatExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeSelectorChips(
    currentMode: DateCalcMode,
    onModeSelected: (DateCalcMode) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(DateCalcMode.values(), key = { it.name }) { mode ->
            val isSelected = mode == currentMode
            val bg = if (isSelected) ColorAmberGlow.copy(alpha = 0.25f) else ColorGlassCard
            val borderCol = if (isSelected) ColorGoldBorder else Color.White.copy(alpha = 0.1f)

            Surface(
                color = bg,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, borderCol),
                modifier = Modifier.clickable { onModeSelected(mode) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(mode.icon, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        mode.title,
                        fontSize = 12.sp,
                        color = if (isSelected) Color.White else ColorSlateMuted,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun DynamicInputPanel(
    currentMode: DateCalcMode,
    startDate: LocalDate,
    endDate: LocalDate,
    isAdditionMode: Boolean,
    durationValueInput: String,
    selectedDurationUnit: DurationUnit,
    businessDaysInput: String,
    weekendType: String,
    holidayDates: List<LocalDate>,
    hijriYearInput: String,
    hijriMonthInput: Int,
    hijriDayInput: Int,
    selectedDateFormat: String,
    onOpenDatePicker: (String) -> Unit,
    onResetStartDateToToday: () -> Unit,
    onToggleAdditionMode: () -> Unit,
    onDurationValueChange: (String) -> Unit,
    onDurationUnitChange: (DurationUnit) -> Unit,
    onBusinessDaysInputChange: (String) -> Unit,
    onWeekendTypeChange: (String) -> Unit,
    onAddHolidayClick: () -> Unit,
    onRemoveHolidayClick: (LocalDate) -> Unit,
    onHijriYearChange: (String) -> Unit,
    onHijriMonthChange: (Int) -> Unit,
    onHijriDayChange: (Int) -> Unit,
    onSwapDatesClick: () -> Unit
) {
    Surface(
        color = ColorGlassCard,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, ColorGoldBorder.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // Start Date Selector (Modes 1, 2, 3, 5)
            if (currentMode != DateCalcMode.HIJRI_CONVERT) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (currentMode == DateCalcMode.AGE_CALC) "تاريخ الميلاد" else "تاريخ البدء الأساسي",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        TextButton(onClick = onResetStartDateToToday) {
                            Text("اليوم 📅", fontSize = 12.sp, color = ColorIceCyan)
                        }
                    }

                    Surface(
                        color = Color.Black.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, ColorIceCyan.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenDatePicker("START") }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    formatDateWithPattern(startDate, selectedDateFormat),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = ColorAmberGlow
                                )
                                Text("اليوم: ${arabicDayNames[startDate.dayOfWeek] ?: ""}", fontSize = 12.sp, color = ColorSlateMuted)
                            }
                            Icon(Icons.Filled.EditCalendar, contentDescription = "تغيير", tint = ColorGoldBorder)
                        }
                    }
                }
            }

            // Mode Specific Custom Inputs:
            when (currentMode) {
                DateCalcMode.ADD_SUBTRACT -> {
                    // Addition / Subtraction Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("نوع العملية الزمانية", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)

                        Surface(
                            color = if (isAdditionMode) ColorEmeraldGreen.copy(alpha = 0.25f) else ColorCrimsonRed.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, if (isAdditionMode) ColorEmeraldGreen else ColorCrimsonRed),
                            modifier = Modifier.clickable { onToggleAdditionMode() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (isAdditionMode) Icons.Filled.AddCircle else Icons.Filled.RemoveCircle,
                                    contentDescription = null,
                                    tint = if (isAdditionMode) ColorEmeraldGreen else ColorCrimsonRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (isAdditionMode) "إضافة زمني (➕)" else "طرح زمني (➖)",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Duration Value & Unit
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = durationValueInput,
                            onValueChange = onDurationValueChange,
                            label = { Text("المقدار العددي", color = ColorSlateMuted) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ColorGoldBorder,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        // Duration Unit Segmented Selector
                        Column(modifier = Modifier.weight(1.5f)) {
                            Text("الوحدة الزمانية", fontSize = 11.sp, color = ColorSlateMuted)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                DurationUnit.values().forEach { unit ->
                                    val isSelected = unit == selectedDurationUnit
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) ColorAmberGlow else Color.Transparent)
                                            .clickable { onDurationUnitChange(unit) }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            unit.labelAr,
                                            fontSize = 11.sp,
                                            color = if (isSelected) Color.Black else Color.White,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Quick Preset Chips
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("اختصارات سريعة للمدة", fontSize = 11.sp, color = ColorSlateMuted)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(listOf("7" to DurationUnit.DAYS, "30" to DurationUnit.DAYS, "90" to DurationUnit.DAYS, "1" to DurationUnit.YEARS, "2" to DurationUnit.YEARS, "5" to DurationUnit.YEARS)) { (valStr, unit) ->
                                Surface(
                                    color = Color(0xFF1E2638),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, ColorIceCyan.copy(alpha = 0.3f)),
                                    modifier = Modifier.clickable {
                                        onDurationValueChange(valStr)
                                        onDurationUnitChange(unit)
                                    }
                                ) {
                                    Text(
                                        "+$valStr ${unit.labelAr}",
                                        fontSize = 11.sp,
                                        color = ColorIceCyan,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                DateCalcMode.DATE_DIFF -> {
                    // Second Date (Date B) Picker
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("تاريخ النهاية / الثاني", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            IconButton(onClick = onSwapDatesClick) {
                                Icon(Icons.Outlined.SwapVert, contentDescription = "تبديل", tint = ColorAmberGlow)
                            }
                        }

                        Surface(
                            color = Color.Black.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, ColorIceCyan.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenDatePicker("END") }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        formatDateWithPattern(endDate, selectedDateFormat),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black,
                                        color = ColorAmberGlow
                                    )
                                    Text("اليوم: ${arabicDayNames[endDate.dayOfWeek] ?: ""}", fontSize = 12.sp, color = ColorSlateMuted)
                                }
                                Icon(Icons.Filled.EditCalendar, contentDescription = "تغيير", tint = ColorGoldBorder)
                            }
                        }
                    }
                }

                DateCalcMode.BUSINESS_DAYS -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = businessDaysInput,
                            onValueChange = onBusinessDaysInputChange,
                            label = { Text("عدد أيام العمل المطلوبة", color = ColorSlateMuted) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ColorGoldBorder,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }

                    // Weekend Days Config
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("تحديد العطلة الأسبوعية", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = weekendType == "FRI_SAT",
                                onClick = { onWeekendTypeChange("FRI_SAT") },
                                label = { Text("الجمعة والسبت (عربي)") },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ColorAmberGlow, selectedLabelColor = Color.Black)
                            )
                            FilterChip(
                                selected = weekendType == "SAT_SUN",
                                onClick = { onWeekendTypeChange("SAT_SUN") },
                                label = { Text("السبت والأحد (غربي)") },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ColorAmberGlow, selectedLabelColor = Color.Black)
                            )
                        }
                    }

                    // Custom Holiday Exceptions Picker
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("الإجازات الرسمية المستثناة (${holidayDates.size})", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            TextButton(onClick = onAddHolidayClick) {
                                Text("+ إضافة إجازة", fontSize = 11.sp, color = ColorAmberGlow)
                            }
                        }

                        if (holidayDates.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(holidayDates) { hDate ->
                                    Surface(
                                        color = ColorCrimsonRed.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, ColorCrimsonRed.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(formatDateWithPattern(hDate, "YYYY/MM/DD"), fontSize = 11.sp, color = Color.White)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                Icons.Filled.Close,
                                                contentDescription = "حذف",
                                                tint = Color.Red,
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clickable { onRemoveHolidayClick(hDate) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                DateCalcMode.HIJRI_CONVERT -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("إدخال التاريخ الهجري (أم القرى)", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = hijriDayInput.toString(),
                                onValueChange = { onHijriDayChange(it.toIntOrNull() ?: 1) },
                                label = { Text("اليوم (1-30)", fontSize = 10.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = hijriMonthInput.toString(),
                                onValueChange = { onHijriMonthChange(it.toIntOrNull() ?: 1) },
                                label = { Text("الشهر (1-12)", fontSize = 10.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = hijriYearInput,
                                onValueChange = onHijriYearChange,
                                label = { Text("السنة (هـ)", fontSize = 10.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1.2f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        val hMonthName = hijriMonthNamesAr.getOrElse(hijriMonthInput - 1) { "" }
                        Text("الشهر الهجري المحدد: $hMonthName", fontSize = 11.sp, color = ColorIceCyan)
                    }
                }

                DateCalcMode.AGE_CALC -> {
                    Text(
                        "سيتم حساب العمر الدقيق، الأيام، الشهور، والساعات المنقضية من الميلاد وحتى اليوم الجاري.",
                        fontSize = 11.sp,
                        color = ColorSlateMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun PrimaryResultCard(
    currentMode: DateCalcMode,
    startDate: LocalDate,
    displayDate: LocalDate,
    endDate: LocalDate,
    relativeDays: Long,
    isAdditionMode: Boolean,
    selectedDateFormat: String,
    businessDaysResult: BusinessDateResult?,
    hijriYearInput: String,
    hijriMonthInput: Int,
    hijriDayInput: Int
) {
    val animatedAlpha by animateFloatAsState(targetValue = 1f, animationSpec = tween(500))

    Surface(
        color = ColorGlassCard,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, ColorAmberGlow.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .scale(animatedAlpha),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                when (currentMode) {
                    DateCalcMode.ADD_SUBTRACT -> "التاريخ المستهدف المحسوب"
                    DateCalcMode.DATE_DIFF -> "الفارق الإجمالي بين التاريخين"
                    DateCalcMode.BUSINESS_DAYS -> "تاريخ انتهاء أيام العمل"
                    DateCalcMode.HIJRI_CONVERT -> "المقابل الميلادي الدقيق"
                    DateCalcMode.AGE_CALC -> "عمرك الحالي بالكامل"
                },
                fontSize = 13.sp,
                color = ColorSlateMuted,
                fontWeight = FontWeight.Bold
            )

            if (currentMode == DateCalcMode.DATE_DIFF) {
                val period = Period.between(startDate, endDate)
                val totalDays = ChronoUnit.DAYS.between(startDate, endDate)

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${Math.abs(period.years)} سنة و ${Math.abs(period.months)} شهر و ${Math.abs(period.days)} يوم",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = ColorAmberGlow,
                        textAlign = TextAlign.Center
                    )
                    Text("إجمالي الأيام: $totalDays يوم بين التاريخين", fontSize = 12.sp, color = ColorIceCyan, modifier = Modifier.padding(top = 4.dp))
                }
            } else if (currentMode == DateCalcMode.AGE_CALC) {
                val today = LocalDate.now()
                val period = Period.between(startDate, today)
                val totalDays = ChronoUnit.DAYS.between(startDate, today)

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${period.years} سنة ، ${period.months} شهر ، ${period.days} يوم",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = ColorAmberGlow,
                        textAlign = TextAlign.Center
                    )
                    Text("إجمالي الأيام المعاشة: $totalDays يوم", fontSize = 12.sp, color = ColorEmeraldGreen, modifier = Modifier.padding(top = 4.dp))
                }
            } else {
                // Display Target Date
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        formatDateWithPattern(displayDate, selectedDateFormat),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = ColorAmberGlow,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "يوم: ${arabicDayNames[displayDate.dayOfWeek] ?: ""}",
                        fontSize = 13.sp,
                        color = ColorIceCyan,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Divider(color = Color.White.copy(alpha = 0.1f))

            // Badges Row (Hijri Equivalent + Humanized Relative String)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hijri Equivalent Badge
                val hijriStr = formatHijriDate(displayDate)
                Surface(
                    color = Color(0xFF1E2638),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🌙 $hijriStr", fontSize = 11.sp, color = ColorIceCyan)
                    }
                }

                // Relative String Badge
                val relText = when {
                    relativeDays > 0 -> "بعد $relativeDays يوماً من البدء"
                    relativeDays < 0 -> "قبل ${Math.abs(relativeDays)} يوماً من البدء"
                    else -> "نفس تاريخ البدء"
                }
                Surface(
                    color = if (relativeDays >= 0) ColorEmeraldGreen.copy(alpha = 0.2f) else ColorCrimsonRed.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(relText, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Business Days Extra Metrics
            if (currentMode == DateCalcMode.BUSINESS_DAYS && businessDaysResult != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("إجمالي الأيام التقويمية: ${businessDaysResult.totalCalendarDays}", fontSize = 11.sp, color = ColorSlateMuted)
                    Text("العطلات المقتطعة: ${businessDaysResult.weekendDaysCount + businessDaysResult.holidayDaysCount}", fontSize = 11.sp, color = ColorAmberGlow)
                }
            }
        }
    }
}

@Composable
private fun TimeBreakdownGrid(
    startDate: LocalDate,
    targetDate: LocalDate
) {
    val totalDays = Math.abs(ChronoUnit.DAYS.between(startDate, targetDate))
    val totalWeeks = totalDays / 7
    val remDays = totalDays % 7
    val totalHours = totalDays * 24
    val totalMinutes = totalHours * 60
    val totalSeconds = totalMinutes * 60

    val dayOfYear = targetDate.dayOfYear
    val yearLength = if (targetDate.isLeapYear) 366 else 365
    val remYearDays = yearLength - dayOfYear
    val yearProgressPct = ((dayOfYear.toFloat() / yearLength) * 100).toInt()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricBox(label = "إجمالي الأسابيع", value = "$totalWeeks أسبوع و $remDays يوم", modifier = Modifier.weight(1f))
            MetricBox(label = "إجمالي الساعات", value = "$totalHours ساعة", modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricBox(label = "إجمالي الدقائق", value = "%,d دقيقة".format(totalMinutes), modifier = Modifier.weight(1f))
            MetricBox(label = "إجمالي الثواني", value = "%,d ثانية".format(totalSeconds), modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricBox(label = "ترتيب اليوم في السنة", value = "اليوم $dayOfYear من $yearLength", modifier = Modifier.weight(1f))
            MetricBox(label = "المتبقي لنهاية السنة", value = "$remYearDays يوم ($yearProgressPct% انقضى)", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun MetricBox(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = Color.Black.copy(alpha = 0.3f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, fontSize = 10.sp, color = ColorSlateMuted)
            Text(value, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ActionButtonsRow(
    onExportCalendar: () -> Unit,
    onCopyText: () -> Unit,
    onShareReport: () -> Unit,
    onSaveHistory: () -> Unit,
    onResetAll: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onExportCalendar) {
            Icon(Icons.Outlined.EventAvailable, contentDescription = "تصدير للتقويم", tint = ColorAmberGlow)
        }
        IconButton(onClick = onCopyText) {
            Icon(Icons.Outlined.ContentCopy, contentDescription = "نسخ", tint = ColorIceCyan)
        }
        IconButton(onClick = onShareReport) {
            Icon(Icons.Outlined.Share, contentDescription = "مشاركة", tint = ColorLavender)
        }
        IconButton(onClick = onSaveHistory) {
            Icon(Icons.Outlined.BookmarkAdd, contentDescription = "حفظ", tint = ColorEmeraldGreen)
        }
        IconButton(onClick = onResetAll) {
            Icon(Icons.Outlined.RestartAlt, contentDescription = "إعادة ضبط", tint = ColorCrimsonRed)
        }
    }
}

@Composable
private fun StepByStepExplanationContent(
    mode: DateCalcMode,
    startDate: LocalDate,
    targetDate: LocalDate,
    endDate: LocalDate,
    isAdd: Boolean,
    durationVal: Int,
    durationUnit: DurationUnit,
    businessResult: BusinessDateResult?
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("تفاصيل الحساب الرياضي الدقيق:", fontSize = 12.sp, color = ColorAmberGlow, fontWeight = FontWeight.Bold)

        when (mode) {
            DateCalcMode.ADD_SUBTRACT -> {
                Text("1. البدء من تاريخ: ${startDate.year}/${startDate.monthValue}/${startDate.dayOfMonth}", fontSize = 11.sp, color = Color.White)
                Text("2. إجراء عملية ${if (isAdd) "إضافة" else "طرح"} بمقدار $durationVal ${durationUnit.labelAr}", fontSize = 11.sp, color = Color.White)
                Text("3. مراعاة أطوال الأشهر وقواعد السنوات الكبيسة تلقائياً حسب معيار java.time.", fontSize = 11.sp, color = ColorSlateMuted)
                Text("4. النتيجة النهائية: ${targetDate.year}/${targetDate.monthValue}/${targetDate.dayOfMonth}", fontSize = 11.sp, color = ColorIceCyan, fontWeight = FontWeight.Bold)
            }
            DateCalcMode.DATE_DIFF -> {
                val period = Period.between(startDate, endDate)
                val days = ChronoUnit.DAYS.between(startDate, endDate)
                Text("1. تاريخ البدء: $startDate | تاريخ النهاية: $endDate", fontSize = 11.sp, color = Color.White)
                Text("2. الفارق التقويمي: ${period.years} سنة و ${period.months} شهر و ${period.days} يوم", fontSize = 11.sp, color = Color.White)
                Text("3. إجمالي الأيام المطلقة المستغرقة: $days يوم", fontSize = 11.sp, color = ColorIceCyan, fontWeight = FontWeight.Bold)
            }
            DateCalcMode.BUSINESS_DAYS -> {
                if (businessResult != null) {
                    Text("1. إجمالي أيام العمل المطلوبة: ${businessResult.totalCalendarDays - businessResult.weekendDaysCount - businessResult.holidayDaysCount} يوم عمل", fontSize = 11.sp, color = Color.White)
                    Text("2. عدد أيام العطلات الأسبوعية المستثناة: ${businessResult.weekendDaysCount} يوم", fontSize = 11.sp, color = Color.White)
                    Text("3. عدد الإجازات الرسمية المقتطعة: ${businessResult.holidayDaysCount} يوم", fontSize = 11.sp, color = Color.White)
                    Text("4. إجمالي الأيام التقومية المستغرقة: ${businessResult.totalCalendarDays} يوم", fontSize = 11.sp, color = ColorIceCyan, fontWeight = FontWeight.Bold)
                }
            }
            DateCalcMode.HIJRI_CONVERT -> {
                Text("تم التحويل المباشر اعتماداً على تقويم أم القرى الرسمي (HijrahChronology).", fontSize = 11.sp, color = ColorSlateMuted)
            }
            DateCalcMode.AGE_CALC -> {
                Text("حساب الفارق الدقيق بين تاريخ الميلاد واللحظة الحالية بكسور الشهور والأسابيع.", fontSize = 11.sp, color = ColorSlateMuted)
            }
        }
    }
}

@Composable
private fun RecurringEventsContent(
    baseDate: LocalDate,
    frequency: String,
    repeatCount: Int,
    dateFormat: String,
    onFrequencyChange: (String) -> Unit,
    onCountChange: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("نوع التكرار", fontSize = 12.sp, color = Color.White)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("DAILY" to "يومي", "WEEKLY" to "أسبوعي", "MONTHLY" to "شهري", "YEARLY" to "سنوي").forEach { (freqKey, label) ->
                    FilterChip(
                        selected = frequency == freqKey,
                        onClick = { onFrequencyChange(freqKey) },
                        label = { Text(label, fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ColorAmberGlow)
                    )
                }
            }
        }

        // Upcoming 5 Recurring Dates
        Text("المواعيد القادمة المتكررة:", fontSize = 11.sp, color = ColorIceCyan, fontWeight = FontWeight.Bold)

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            (1..repeatCount).forEach { i ->
                val nextD = when (frequency) {
                    "DAILY" -> baseDate.plusDays(i.toLong())
                    "WEEKLY" -> baseDate.plusWeeks(i.toLong())
                    "MONTHLY" -> baseDate.plusMonths(i.toLong())
                    "YEARLY" -> baseDate.plusYears(i.toLong())
                    else -> baseDate.plusDays(i.toLong())
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("التكرار #$i", fontSize = 11.sp, color = ColorSlateMuted)
                    Text("${formatDateWithPattern(nextD, dateFormat)} (${arabicDayNames[nextD.dayOfWeek]})", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun HistoryDrawerContent(
    historyList: List<DateHistoryRecord>,
    onClearHistory: () -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    if (historyList.isEmpty()) {
        Text("لا يوجد سجل حسابات محفوظ حالياً", fontSize = 12.sp, color = ColorSlateMuted)
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("السجل الإجمالي", fontSize = 12.sp, color = ColorAmberGlow, fontWeight = FontWeight.Bold)
                TextButton(onClick = onClearHistory) {
                    Text("مسح السجل", color = ColorCrimsonRed, fontSize = 11.sp)
                }
            }

            historyList.forEach { record ->
                Surface(
                    color = Color.Black.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(record.modeTitle, fontSize = 11.sp, color = ColorAmberGlow, fontWeight = FontWeight.Bold)
                            Text(record.inputSummary, fontSize = 11.sp, color = ColorSlateMuted)
                            Text("النتيجة: ${record.resultSummary}", fontSize = 12.sp, color = ColorIceCyan, fontWeight = FontWeight.Bold)
                        }

                        IconButton(onClick = { onToggleFavorite(record.id) }) {
                            Icon(
                                if (record.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = "المفضلة",
                                tint = if (record.isFavorite) ColorAmberGlow else ColorSlateMuted
                            )
                        }
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
        border = BorderStroke(1.dp, ColorGoldBorder.copy(alpha = 0.2f)),
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
                    Icon(icon, contentDescription = null, tint = ColorGoldBorder, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Icon(
                    if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = ColorSlateMuted
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    content()
                }
            }
        }
    }
}

// ==========================================
// UTILITY FUNCTIONS & CALCULATIONS
// ==========================================

private fun calculateBusinessDate(
    startDate: LocalDate,
    daysCount: Int,
    isAdd: Boolean,
    weekendType: String,
    holidays: List<LocalDate>
): BusinessDateResult {
    var currentDate = startDate
    var remainingDays = daysCount
    var weekendDaysCount = 0
    var holidayDaysCount = 0

    while (remainingDays > 0) {
        currentDate = if (isAdd) currentDate.plusDays(1) else currentDate.minusDays(1)
        val dayOfWeek = currentDate.dayOfWeek

        val isWeekend = if (weekendType == "FRI_SAT") {
            dayOfWeek == DayOfWeek.FRIDAY || dayOfWeek == DayOfWeek.SATURDAY
        } else {
            dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY
        }

        val isHoliday = holidays.contains(currentDate)

        if (isWeekend) {
            weekendDaysCount++
        } else if (isHoliday) {
            holidayDaysCount++
        } else {
            remainingDays--
        }
    }

    val totalCalendarDays = ChronoUnit.DAYS.between(
        if (isAdd) startDate else currentDate,
        if (isAdd) currentDate else startDate
    ).toInt()

    return BusinessDateResult(
        targetDate = currentDate,
        totalCalendarDays = totalCalendarDays,
        weekendDaysCount = weekendDaysCount,
        holidayDaysCount = holidayDaysCount
    )
}

private fun formatDateWithPattern(date: LocalDate, formatPattern: String): String {
    val d = String.format(Locale.US, "%02d", date.dayOfMonth)
    val m = String.format(Locale.US, "%02d", date.monthValue)
    val y = date.year.toString()
    val monthName = gregorianMonthNamesAr.getOrElse(date.monthValue - 1) { "" }
    return when (formatPattern) {
        "YYYY/MM/DD" -> "$y/$m/$d"
        "DD/MM/YYYY" -> "$d/$m/$y"
        "MM/DD/YYYY" -> "$m/$d/$y"
        "FULL_TEXT" -> "$d $monthName $y"
        else -> "$y/$m/$d"
    }
}

private fun formatHijriDate(date: LocalDate): String {
    return try {
        val hijrahDate = HijrahChronology.INSTANCE.date(date)
        val hYear = hijrahDate.get(ChronoField.YEAR)
        val hMonth = hijrahDate.get(ChronoField.MONTH_OF_YEAR)
        val hDay = hijrahDate.get(ChronoField.DAY_OF_MONTH)
        val monthName = hijriMonthNamesAr.getOrElse(hMonth - 1) { "" }
        "$hDay $monthName $hYear هـ"
    } catch (e: Exception) {
        "1448 هـ"
    }
}

private fun hijriToGregorian(hYear: Int, hMonth: Int, hDay: Int): LocalDate {
    return try {
        val hijrahDate = HijrahChronology.INSTANCE.date(hYear, hMonth, hDay)
        LocalDate.from(hijrahDate)
    } catch (e: Exception) {
        LocalDate.now()
    }
}

private fun exportToSystemCalendar(context: Context, title: String, date: LocalDate, description: String) {
    try {
        val startMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, startMillis + 3600000)
            putExtra(CalendarContract.Events.TITLE, title)
            putExtra(CalendarContract.Events.DESCRIPTION, description)
            putExtra(CalendarContract.Events.ALL_DAY, true)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback
    }
}

private fun shareDateReport(context: Context, reportText: String) {
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "تقرير حاسبة التاريخ الذكية")
            putExtra(Intent.EXTRA_TEXT, reportText)
        }
        context.startActivity(Intent.createChooser(intent, "مشاركة تقرير التاريخ"))
    } catch (e: Exception) {
        // Fallback
    }
}

private fun buildSummaryReportText(
    mode: DateCalcMode,
    startDate: LocalDate,
    targetDate: LocalDate,
    dateFormat: String,
    isAdd: Boolean,
    durationVal: Int,
    durationUnit: DurationUnit,
    businessResult: BusinessDateResult?
): String {
    val sb = StringBuilder()
    sb.append("📅 *** تقرير حاسبة التاريخ الذكية Pro ***\n")
    sb.append("----------------------------------\n")
    sb.append("• الوضع: ${mode.title}\n")
    sb.append("• تاريخ البدء: ${formatDateWithPattern(startDate, dateFormat)}\n")
    sb.append("• النتيجة النهائية: ${formatDateWithPattern(targetDate, dateFormat)} (${arabicDayNames[targetDate.dayOfWeek]})\n")
    sb.append("• الموافق الهجري: ${formatHijriDate(targetDate)}\n")
    if (mode == DateCalcMode.BUSINESS_DAYS && businessResult != null) {
        sb.append("• أيام التقويم المستغرقة: ${businessResult.totalCalendarDays} يوم\n")
        sb.append("• الإجازات المستثناة: ${businessResult.weekendDaysCount + businessResult.holidayDaysCount} يوم\n")
    }
    sb.append("----------------------------------\n")
    sb.append("محسوب عبر تطبيق الذكاء الزمني.")
    return sb.toString()
}

// History SharedPreferences Logic
private fun saveDateHistoryToPrefs(context: Context, history: List<DateHistoryRecord>) {
    try {
        val prefs = context.getSharedPreferences("date_calc_history_prefs", Context.MODE_PRIVATE)
        val jsonArr = JSONArray()
        history.forEach { record ->
            val obj = JSONObject().apply {
                put("id", record.id)
                put("modeTitle", record.modeTitle)
                put("inputSummary", record.inputSummary)
                put("resultSummary", record.resultSummary)
                put("timestamp", record.timestamp)
                put("isFavorite", record.isFavorite)
            }
            jsonArr.put(obj)
        }
        prefs.edit().putString("history_data", jsonArr.toString()).apply()
    } catch (e: Exception) {
        // Ignore
    }
}

private fun loadDateHistoryFromPrefs(context: Context): List<DateHistoryRecord> {
    val list = mutableListOf<DateHistoryRecord>()
    try {
        val prefs = context.getSharedPreferences("date_calc_history_prefs", Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("history_data", null) ?: return emptyList()
        val jsonArr = JSONArray(jsonStr)
        for (i in 0 until jsonArr.length()) {
            val obj = jsonArr.getJSONObject(i)
            list.add(
                DateHistoryRecord(
                    id = obj.optString("id"),
                    modeTitle = obj.optString("modeTitle"),
                    inputSummary = obj.optString("inputSummary"),
                    resultSummary = obj.optString("resultSummary"),
                    timestamp = obj.optLong("timestamp"),
                    isFavorite = obj.optBoolean("isFavorite")
                )
            )
        }
    } catch (e: Exception) {
        // Ignore
    }
    return list
}
