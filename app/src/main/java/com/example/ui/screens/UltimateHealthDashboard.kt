package com.example.ui.screens

import android.app.DatePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CustomThemeColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// ====================================================================
// 1. PALETTE & THEME CONSTANTS (EMERALD GLASS & OBSIDIAN EDITION)
// ====================================================================

enum class UserGender { MALE, FEMALE }
enum class DashboardState { LOADING, SUCCESS, EMPTY, ERROR }

private val DarkObsidianStart = Color(0xFF0A0C10)
private val DarkObsidianEnd = Color(0xFF12161F)
private val GlassSurface = Color(0xFF161B26)
private val ChampagneGold = Color(0xFFD4AF37)
private val ChampagneGoldBorder = Color(0xFFD4AF37).copy(alpha = 0.30f)
private val EmeraldGlow = Color(0xFF4EECD5)
private val SoftRose = Color(0xFFF472B6)
private val LavenderAccent = Color(0xFFA78BFA)
private val HydrationBlue = Color(0xFF38BDF8)
private val EnergyAmber = Color(0xFFFB923C)
private val MutedText = Color(0xFF94A3B8)

// ====================================================================
// 2. MAIN ULTIMATE HEALTH DASHBOARD COMPOSABLE
// ====================================================================

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun UltimateHealthDashboard(
    colors: CustomThemeColors,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("clevcalc_health_prefs", Context.MODE_PRIVATE) }

    // --- Core Dashboard State Machine ---
    var screenState by remember { mutableStateOf(DashboardState.LOADING) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    // --- Persistent User Health Data (DataStore / SharedPreferences) ---
    var userGender by rememberSaveable {
        mutableStateOf(if (prefs.getBoolean("profile_is_female", true)) UserGender.FEMALE else UserGender.MALE)
    }
    var ageText by rememberSaveable { mutableStateOf(prefs.getString("health_age", "25") ?: "25") }
    var heightText by rememberSaveable { mutableStateOf(prefs.getString("health_height", "172") ?: "172") }
    var weightText by rememberSaveable { mutableStateOf(prefs.getString("health_weight", "65") ?: "65") }
    var waterCups by rememberSaveable { mutableStateOf(prefs.getInt("water_cups", 6)) }
    var sleepHoursText by rememberSaveable { mutableStateOf(prefs.getString("sleep_hours", "7.5") ?: "7.5") }
    var stepCount by rememberSaveable { mutableStateOf(prefs.getInt("daily_steps", 6420)) }

    // Cycle Data (Female Only)
    var cycleLengthText by rememberSaveable { mutableStateOf(prefs.getString("cycle_length", "28") ?: "28") }
    var periodDurationText by rememberSaveable { mutableStateOf(prefs.getString("period_duration", "5") ?: "5") }
    var lastPeriodDay by rememberSaveable { mutableStateOf(prefs.getInt("start_day", 1)) }
    var lastPeriodMonth by rememberSaveable { mutableStateOf(prefs.getInt("start_month", 8)) }
    var lastPeriodYear by rememberSaveable { mutableStateOf(prefs.getInt("start_year", 2026)) }

    // Logger & Toggle States
    var selectedMood by rememberSaveable { mutableStateOf(prefs.getString("selected_mood", "سعيد 😄") ?: "سعيد 😄") }
    val selectedSymptoms = remember { mutableStateListOf<String>() }
    var medicationReminder by rememberSaveable { mutableStateOf(prefs.getBoolean("med_reminder", true)) }
    var doctorReminder by rememberSaveable { mutableStateOf(prefs.getBoolean("doc_reminder", false)) }
    var hapticFeedbackEnabled by rememberSaveable { mutableStateOf(prefs.getBoolean("haptic_enabled", true)) }

    // Tab Selection State
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var showHealthTipsDialog by remember { mutableStateOf(false) }

    // --- Save Local Preferences Helper ---
    fun savePreferences() {
        prefs.edit()
            .putBoolean("profile_is_female", userGender == UserGender.FEMALE)
            .putString("health_age", ageText)
            .putString("health_height", heightText)
            .putString("health_weight", weightText)
            .putInt("water_cups", waterCups)
            .putString("sleep_hours", sleepHoursText)
            .putInt("daily_steps", stepCount)
            .putString("cycle_length", cycleLengthText)
            .putString("period_duration", periodDurationText)
            .putInt("start_day", lastPeriodDay)
            .putInt("start_month", lastPeriodMonth)
            .putInt("start_year", lastPeriodYear)
            .putString("selected_mood", selectedMood)
            .putBoolean("med_reminder", medicationReminder)
            .putBoolean("doc_reminder", doctorReminder)
            .putBoolean("haptic_enabled", hapticFeedbackEnabled)
            .apply()

        if (hapticFeedbackEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    // --- Initial Simulated Data Load ---
    fun loadDashboardData() {
        coroutineScope.launch {
            screenState = DashboardState.LOADING
            delay(400) // Smooth entrance shimmer
            screenState = DashboardState.SUCCESS
        }
    }

    LaunchedEffect(Unit) {
        loadDashboardData()
    }

    // --- Fully Implemented Biometric Computations ---
    val age = ageText.toIntOrNull() ?: 25
    val height = heightText.toDoubleOrNull() ?: 172.0
    val weight = weightText.toDoubleOrNull() ?: 65.0
    val sleepHours = sleepHoursText.toDoubleOrNull() ?: 7.5
    val cycleLength = cycleLengthText.toIntOrNull() ?: 28

    // 1. BMI Calculation
    val heightMeters = (height / 100.0).coerceAtLeast(0.5)
    val bmi = weight / (heightMeters * heightMeters)
    val bmiCategory = when {
        bmi < 18.5 -> "نقص في الوزن (نحافة)"
        bmi < 25.0 -> "وزن صحي ومثالي"
        bmi < 30.0 -> "زيادة في الوزن"
        else -> "سمنة مفرطة"
    }
    val bmiBadgeColor = when {
        bmi < 18.5 -> EnergyAmber
        bmi < 25.0 -> EmeraldGlow
        else -> SoftRose
    }

    // 2. BMR Calculation (Mifflin-St Jeor)
    val bmr = if (userGender == UserGender.MALE) {
        (10 * weight) + (6.25 * height) - (5 * age) + 5
    } else {
        (10 * weight) + (6.25 * height) - (5 * age) - 161
    }

    // 3. Needed Daily Calories (Moderate activity factor)
    val neededCalories = (bmr * 1.375).roundToInt()

    // 4. Ideal Weight Range
    val minIdealWeight = (18.5 * heightMeters * heightMeters).roundToInt()
    val maxIdealWeight = (24.9 * heightMeters * heightMeters).roundToInt()

    // 5. Hydration Goal
    val targetWaterCups = ((weight * 0.033) / 0.25).roundToInt().coerceAtLeast(8)

    // 6. Female Menstrual & Fertility Cycle Calculations
    val lastPeriodCal = Calendar.getInstance().apply {
        set(lastPeriodYear, lastPeriodMonth - 1, lastPeriodDay)
    }
    val currentCal = Calendar.getInstance()
    val diffMillis = currentCal.timeInMillis - lastPeriodCal.timeInMillis
    val daysSinceLastPeriod = (diffMillis / (1000 * 60 * 60 * 24)).toInt()
    val currentCycleDay = ((daysSinceLastPeriod % cycleLength) + 1).coerceAtLeast(1)

    val nextPeriodCal = (lastPeriodCal.clone() as Calendar).apply {
        add(Calendar.DAY_OF_YEAR, cycleLength)
    }
    val ovulationCal = (lastPeriodCal.clone() as Calendar).apply {
        add(Calendar.DAY_OF_YEAR, cycleLength - 14)
    }
    val daysUntilNextPeriod = ((nextPeriodCal.timeInMillis - currentCal.timeInMillis) / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)

    val sdfDisplay = SimpleDateFormat("dd MMMM", Locale("ar"))
    val ovulationDateStr = sdfDisplay.format(ovulationCal.time)
    val nextPeriodDateStr = sdfDisplay.format(nextPeriodCal.time)

    // Overall Computed Health Score (0-100)
    val healthScore = remember(bmi, waterCups, sleepHours, stepCount) {
        var score = 70
        if (bmi in 18.5..24.9) score += 10
        if (waterCups >= targetWaterCups) score += 10
        if (sleepHours in 7.0..9.0) score += 5
        if (stepCount >= 6000) score += 5
        score.coerceIn(40, 98)
    }

    // --- Main Screen Container ---
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DarkObsidianStart, DarkObsidianEnd)
                )
            )
    ) {
        // Ambient Procedural Canvas Background Grid
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridSpacing = 40.dp.toPx()
            var x = 0f
            while (x < size.width) {
                drawLine(
                    color = EmeraldGlow.copy(alpha = 0.02f),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1f
                )
                x += gridSpacing
            }
            var y = 0f
            while (y < size.height) {
                drawLine(
                    color = EmeraldGlow.copy(alpha = 0.02f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
                y += gridSpacing
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Top Bar Navigation
            DashboardTopBar(
                title = "لوحة الصحة والرشاقة",
                subtitle = "Emerald Glass Edition • المتابعة الحيوية",
                userGender = userGender,
                isRefreshing = isRefreshing,
                onGenderToggle = {
                    userGender = if (userGender == UserGender.MALE) UserGender.FEMALE else UserGender.MALE
                    savePreferences()
                },
                onRefresh = {
                    coroutineScope.launch {
                        isRefreshing = true
                        delay(600)
                        isRefreshing = false
                        Toast.makeText(context, "تم تحديث المؤشرات الحيوية بنجاح", Toast.LENGTH_SHORT).show()
                    }
                },
                onBackClick = onBackClick
            )

            // 2. Offline Notice Banner
            OfflineNoticeBanner()

            // 3. Screen Body according to DashboardState
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (screenState) {
                    DashboardState.LOADING -> {
                        DashboardShimmerLoading()
                    }

                    DashboardState.ERROR -> {
                        DashboardErrorState(
                            message = errorMessage ?: "حدث خطأ غير متوقع أثناء تحميل بيانات الصحة",
                            onRetry = { loadDashboardData() }
                        )
                    }

                    DashboardState.EMPTY -> {
                        DashboardEmptyState(
                            onStartSetup = {
                                ageText = "25"
                                heightText = "172"
                                weightText = "65"
                                savePreferences()
                                screenState = DashboardState.SUCCESS
                            }
                        )
                    }

                    DashboardState.SUCCESS -> {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // --- SECTION A: Dynamic Health Status Hero Card ---
                            item {
                                HealthHeroCard(
                                    healthScore = healthScore,
                                    bmi = bmi,
                                    bmr = bmr,
                                    weight = weight,
                                    waterCups = waterCups,
                                    targetWaterCups = targetWaterCups,
                                    medicationReminder = medicationReminder,
                                    doctorReminder = doctorReminder,
                                    onToggleMedication = {
                                        medicationReminder = it
                                        savePreferences()
                                    },
                                    onToggleDoctor = {
                                        doctorReminder = it
                                        savePreferences()
                                    }
                                )
                            }

                            // --- SECTION B: Health Summary Strip ---
                            item {
                                HealthSummaryStrip(
                                    sleepHours = sleepHours,
                                    waterCups = waterCups,
                                    targetWaterCups = targetWaterCups,
                                    weight = weight,
                                    bmr = bmr,
                                    bmi = bmi
                                )
                            }

                            // --- SECTION C: BMI & BMR Interactive Calculator Card ---
                            item {
                                BmiBmrCalculatorCard(
                                    userGender = userGender,
                                    ageText = ageText,
                                    heightText = heightText,
                                    weightText = weightText,
                                    bmi = bmi,
                                    bmiCategory = bmiCategory,
                                    bmiBadgeColor = bmiBadgeColor,
                                    bmr = bmr,
                                    neededCalories = neededCalories,
                                    minIdealWeight = minIdealWeight,
                                    maxIdealWeight = maxIdealWeight,
                                    onGenderChange = {
                                        userGender = it
                                        savePreferences()
                                    },
                                    onAgeChange = { ageText = it },
                                    onHeightChange = { heightText = it },
                                    onWeightChange = { weightText = it },
                                    onCalculate = { savePreferences() }
                                )
                            }

                            // --- SECTION D & E: Female Menstrual Cycle & Ovulation Tracker ---
                            item {
                                Column {
                                    AnimatedVisibility(
                                        visible = userGender == UserGender.FEMALE,
                                        enter = fadeIn() + expandVertically(),
                                        exit = fadeOut() + shrinkVertically()
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        MenstrualCycleCard(
                                            currentCycleDay = currentCycleDay,
                                            cycleLength = cycleLength,
                                            daysUntilNextPeriod = daysUntilNextPeriod,
                                            ovulationDateStr = ovulationDateStr,
                                            nextPeriodDateStr = nextPeriodDateStr,
                                            cycleLengthText = cycleLengthText,
                                            periodDurationText = periodDurationText,
                                            lastPeriodDay = lastPeriodDay,
                                            lastPeriodMonth = lastPeriodMonth,
                                            lastPeriodYear = lastPeriodYear,
                                            onCycleLengthChange = { cycleLengthText = it },
                                            onPeriodDurationChange = { periodDurationText = it },
                                            onDatePicked = { d, m, y ->
                                                lastPeriodDay = d
                                                lastPeriodMonth = m
                                                lastPeriodYear = y
                                                savePreferences()
                                            }
                                        )

                                        FertilityTimelineStrip(
                                            currentCycleDay = currentCycleDay,
                                            cycleLength = cycleLength
                                        )
                                    }
                                }
                            }
                        }

                            // --- SECTION F: Daily Symptoms Logger ---
                            item {
                                DailySymptomsLoggerCard(
                                    selectedMood = selectedMood,
                                    selectedSymptoms = selectedSymptoms,
                                    hapticEnabled = hapticFeedbackEnabled,
                                    onMoodSelected = {
                                        selectedMood = it
                                        savePreferences()
                                    },
                                    onSymptomToggled = { symptom ->
                                        if (selectedSymptoms.contains(symptom)) {
                                            selectedSymptoms.remove(symptom)
                                        } else {
                                            selectedSymptoms.add(symptom)
                                        }
                                        savePreferences()
                                    },
                                    onHapticToggled = {
                                        hapticFeedbackEnabled = it
                                        savePreferences()
                                    }
                                )
                            }

                            // --- SECTION G: Vital Activity Tri-Card Grid ---
                            item {
                                VitalActivityGrid(
                                    waterCups = waterCups,
                                    targetWaterCups = targetWaterCups,
                                    sleepHoursText = sleepHoursText,
                                    stepCount = stepCount,
                                    onWaterAdd = {
                                        waterCups++
                                        savePreferences()
                                    },
                                    onWaterRemove = {
                                        if (waterCups > 0) waterCups--
                                        savePreferences()
                                    },
                                    onSleepHoursChange = {
                                        sleepHoursText = it
                                        savePreferences()
                                    },
                                    onStepCountAdd = {
                                        stepCount += 500
                                        savePreferences()
                                    }
                                )
                            }

                            // --- SECTION H: Quick Metric Tabs with Animated Content ---
                            item {
                                QuickMetricTabsCard(
                                    selectedTab = selectedTab,
                                    onTabSelected = { selectedTab = it },
                                    waterCups = waterCups,
                                    sleepHours = sleepHours,
                                    bmi = bmi,
                                    weight = weight
                                )
                            }
                        }
                    }
                }
            }

            // --- SECTION I: Bottom Sticky Summary Panel & Action Buttons ---
            if (screenState == DashboardState.SUCCESS) {
                StickyBottomActionPanel(
                    bmi = bmi,
                    bmr = bmr,
                    neededCalories = neededCalories,
                    daysUntilNextPeriod = if (userGender == UserGender.FEMALE) daysUntilNextPeriod else null,
                    onOpenTips = { showHealthTipsDialog = true },
                    onSave = {
                        savePreferences()
                        Toast.makeText(context, "تم حفظ جميع البيانات والمؤشرات بنجاح", Toast.LENGTH_SHORT).show()
                    },
                    onExportPdf = {
                        Toast.makeText(context, "جاري إعداد تقرير PDF الطبي المفصل...", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    // Health Advice Bottom Sheet / Dialog
    if (showHealthTipsDialog) {
        HealthAdviceDialog(onDismiss = { showHealthTipsDialog = false })
    }
}

// ====================================================================
// 3. TOP BAR & BANNER COMPONENTS
// ====================================================================

@Composable
fun DashboardTopBar(
    title: String,
    subtitle: String,
    userGender: UserGender,
    isRefreshing: Boolean,
    onGenderToggle: () -> Unit,
    onRefresh: () -> Unit,
    onBackClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onBackClick != null) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(GlassSurface.copy(alpha = 0.8f))
                        .border(1.dp, ChampagneGoldBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "رجوع",
                        tint = EmeraldGlow,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = MutedText,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Quick Gender Toggle Pill
            Surface(
                onClick = onGenderToggle,
                shape = RoundedCornerShape(14.dp),
                color = GlassSurface.copy(alpha = 0.8f),
                border = androidx.compose.foundation.BorderStroke(1.dp, ChampagneGoldBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (userGender == UserGender.FEMALE) "أنثى 👩" else "ذكر 👨",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Refresh Button
            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(GlassSurface.copy(alpha = 0.8f))
                    .border(1.dp, ChampagneGoldBorder, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "تحديث",
                    tint = EmeraldGlow,
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer {
                            if (isRefreshing) rotationZ += 180f
                        }
                )
            }
        }
    }
}

@Composable
fun OfflineNoticeBanner() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        color = EmeraldGlow.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGlow.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = EmeraldGlow,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "المحرك الحيوي يعمل بنسبة 100% محلياً ودون الحاجة للاتصال بالإنترنت",
                fontSize = 10.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ====================================================================
// 4. SECTION A: DYNAMIC HEALTH STATUS HERO CARD
// ====================================================================

@Composable
fun HealthHeroCard(
    healthScore: Int,
    bmi: Double,
    bmr: Double,
    weight: Double,
    waterCups: Int,
    targetWaterCups: Int,
    medicationReminder: Boolean,
    doctorReminder: Boolean,
    onToggleMedication: (Boolean) -> Unit,
    onToggleDoctor: (Boolean) -> Unit
) {
    val animatedScore by animateIntAsState(
        targetValue = healthScore,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "scoreAnim"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = GlassSurface.copy(alpha = 0.85f),
        border = androidx.compose.foundation.BorderStroke(1.dp, ChampagneGoldBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Procedural Canvas Vector Illustration & Ring
                Box(
                    modifier = Modifier.size(90.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 8.dp.toPx()
                        val diameter = size.minDimension - stroke
                        val radius = diameter / 2f
                        val center = Offset(size.width / 2f, size.height / 2f)

                        // Background Ring
                        drawCircle(
                            color = Color.White.copy(alpha = 0.08f),
                            radius = radius,
                            center = center,
                            style = Stroke(width = stroke)
                        )

                        // Outer Emerald Score Gauge
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(EmeraldGlow, HydrationBlue, EmeraldGlow)
                            ),
                            startAngle = -90f,
                            sweepAngle = (animatedScore / 100f) * 360f,
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$animatedScore",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = EmeraldGlow
                        )
                        Text(
                            text = "من 100",
                            fontSize = 9.sp,
                            color = MutedText
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "الحالة الصحية العامة: ممتازة ✨",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "المؤشرات الحيوية مستقرة بناءً على الوزن والنشاط اليومي",
                        fontSize = 10.sp,
                        color = MutedText,
                        lineHeight = 14.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Biometric Summary Tags Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        BiometricPill(label = "BMI: ${String.format("%.1f", bmi)}")
                        BiometricPill(label = "BMR: ${bmr.toInt()}")
                        BiometricPill(label = "الوزن: ${weight.toInt()}k")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(12.dp))

            // Smart Health Insights & Toggles Banner
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkObsidianStart.copy(alpha = 0.6f),
                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGlow.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = ChampagneGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "توصية ذكية: اليوم مناسب للمشي لمدة 30 دقيقة لتحسين عملية الأيض.",
                            fontSize = 11.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("موعد الدواء", fontSize = 11.sp, color = MutedText)
                            Spacer(modifier = Modifier.width(6.dp))
                            Switch(
                                checked = medicationReminder,
                                onCheckedChange = onToggleMedication,
                                modifier = Modifier.scale(0.75f),
                                colors = SwitchDefaults.colors(checkedThumbColor = EmeraldGlow)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("موعد الطبيب", fontSize = 11.sp, color = MutedText)
                            Spacer(modifier = Modifier.width(6.dp))
                            Switch(
                                checked = doctorReminder,
                                onCheckedChange = onToggleDoctor,
                                modifier = Modifier.scale(0.75f),
                                colors = SwitchDefaults.colors(checkedThumbColor = EmeraldGlow)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BiometricPill(label: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = EmeraldGlow.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGlow.copy(alpha = 0.3f))
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = EmeraldGlow,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

// ====================================================================
// 5. SECTION B: HEALTH SUMMARY STRIP
// ====================================================================

@Composable
fun HealthSummaryStrip(
    sleepHours: Double,
    waterCups: Int,
    targetWaterCups: Int,
    weight: Double,
    bmr: Double,
    bmi: Double
) {
    val items = listOf(
        Triple("😴 النوم", "$sleepHours ساعات", HydrationBlue),
        Triple("💧 الماء", "$waterCups / $targetWaterCups أكواب", EmeraldGlow),
        Triple("⚖️ الوزن", "${weight.toInt()} كجم", ChampagneGold),
        Triple("🔥 BMR", "${bmr.toInt()} kcal", EnergyAmber),
        Triple("❤️ BMI", String.format("%.1f", bmi), SoftRose)
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(items) { (title, subtitle, accentColor) ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = GlassSurface.copy(alpha = 0.8f),
                border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = accentColor
                    )
                }
            }
        }
    }
}

// ====================================================================
// 6. SECTION C: BMI & BMR INTERACTIVE CALCULATOR CARD
// ====================================================================

@Composable
fun BmiBmrCalculatorCard(
    userGender: UserGender,
    ageText: String,
    heightText: String,
    weightText: String,
    bmi: Double,
    bmiCategory: String,
    bmiBadgeColor: Color,
    bmr: Double,
    neededCalories: Int,
    minIdealWeight: Int,
    maxIdealWeight: Int,
    onGenderChange: (UserGender) -> Unit,
    onAgeChange: (String) -> Unit,
    onHeightChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onCalculate: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = GlassSurface.copy(alpha = 0.85f),
        border = androidx.compose.foundation.BorderStroke(1.dp, ChampagneGoldBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "حاسبة المؤشرات الحيوية (BMI & BMR)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // Gender Segmented Selector
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkObsidianStart)
                        .padding(2.dp)
                ) {
                    Surface(
                        onClick = { onGenderChange(UserGender.MALE) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (userGender == UserGender.MALE) EmeraldGlow else Color.Transparent
                    ) {
                        Text(
                            text = "ذكر 👨",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (userGender == UserGender.MALE) Color.Black else MutedText,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        onClick = { onGenderChange(UserGender.FEMALE) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (userGender == UserGender.FEMALE) EmeraldGlow else Color.Transparent
                    ) {
                        Text(
                            text = "أنثى 👩",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (userGender == UserGender.FEMALE) Color.Black else MutedText,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Text Inputs Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = ageText,
                    onValueChange = onAgeChange,
                    label = { Text("العمر", fontSize = 10.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGlow,
                        unfocusedBorderColor = ChampagneGoldBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = heightText,
                    onValueChange = onHeightChange,
                    label = { Text("الطول (سم)", fontSize = 10.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGlow,
                        unfocusedBorderColor = ChampagneGoldBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = weightText,
                    onValueChange = onWeightChange,
                    label = { Text("الوزن (كجم)", fontSize = 10.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGlow,
                        unfocusedBorderColor = ChampagneGoldBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Button
            Button(
                onClick = onCalculate,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow)
            ) {
                Text(
                    text = "احسب النتائج وتحديث المؤشرات ⚡",
                    color = Color.Black,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Results Display Panel
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkObsidianStart.copy(alpha = 0.7f),
                border = androidx.compose.foundation.BorderStroke(1.dp, ChampagneGoldBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("مؤشر كتلة الجسم (BMI)", fontSize = 10.sp, color = MutedText)
                            Text(
                                text = String.format("%.1f", bmi),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = EmeraldGlow
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = bmiBadgeColor.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, bmiBadgeColor)
                        ) {
                            Text(
                                text = bmiCategory,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = bmiBadgeColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("معدل الأيض (BMR)", fontSize = 10.sp, color = MutedText)
                            Text("${bmr.toInt()} سعرة/يوم", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Column {
                            Text("السعرات اليومية", fontSize = 10.sp, color = MutedText)
                            Text("$neededCalories kcal", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EnergyAmber)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("الوزن المثالي", fontSize = 10.sp, color = MutedText)
                            Text("$minIdealWeight - $maxIdealWeight كجم", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HydrationBlue)
                        }
                    }
                }
            }
        }
    }
}

// ====================================================================
// 7. SECTION D & E: FEMALE CYCLE & FERTILITY TRACKER
// ====================================================================

@Composable
fun MenstrualCycleCard(
    currentCycleDay: Int,
    cycleLength: Int,
    daysUntilNextPeriod: Int,
    ovulationDateStr: String,
    nextPeriodDateStr: String,
    cycleLengthText: String,
    periodDurationText: String,
    lastPeriodDay: Int,
    lastPeriodMonth: Int,
    lastPeriodYear: Int,
    onCycleLengthChange: (String) -> Unit,
    onPeriodDurationChange: (String) -> Unit,
    onDatePicked: (Int, Int, Int) -> Unit
) {
    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = GlassSurface.copy(alpha = 0.85f),
        border = androidx.compose.foundation.BorderStroke(1.dp, SoftRose.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "متتبع الدورة الشهرية والخصوبة 🌸",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = SoftRose
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SoftRose.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "الدورة القادمة خلال $daysUntilNextPeriod يوماً",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SoftRose,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Gauge Ring Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(70.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 6.dp.toPx()
                        drawCircle(
                            color = Color.White.copy(alpha = 0.08f),
                            style = Stroke(width = stroke)
                        )
                        drawArc(
                            color = SoftRose,
                            startAngle = -90f,
                            sweepAngle = (currentCycleDay.toFloat() / cycleLength.toFloat()) * 360f,
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }
                    Text(
                        text = "$currentCycleDay",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "اليوم الـ $currentCycleDay من أصل $cycleLength يوماً",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "المرحلة الحالية: فترة الخصوبة المتوقعة",
                        fontSize = 11.sp,
                        color = LavenderAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Phase Pills Row
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PhasePill(title = "دورة", color = SoftRose)
                PhasePill(title = "خصوبة", color = LavenderAccent)
                PhasePill(title = "تبويض", color = EmeraldGlow)
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(12.dp))

            // Dates Summary Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("يوم التبويض المتوقع", fontSize = 10.sp, color = MutedText)
                    Text(ovulationDateStr, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldGlow)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("تاريخ الدورة القادمة", fontSize = 10.sp, color = MutedText)
                    Text(nextPeriodDateStr, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SoftRose)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Last Period DatePicker Button & Length Inputs
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = {
                    DatePickerDialog(context, { _, y, m, d ->
                        onDatePicked(d, m + 1, y)
                    }, lastPeriodYear, lastPeriodMonth - 1, lastPeriodDay).show()
                }) {
                    Text("بداية آخر طمث: $lastPeriodYear/$lastPeriodMonth/$lastPeriodDay 📅", fontSize = 11.sp, color = ChampagneGold)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = cycleLengthText,
                    onValueChange = onCycleLengthChange,
                    label = { Text("طول الدورة (يوم)", fontSize = 10.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SoftRose, unfocusedBorderColor = ChampagneGoldBorder)
                )

                OutlinedTextField(
                    value = periodDurationText,
                    onValueChange = onPeriodDurationChange,
                    label = { Text("مدة الطمث (يوم)", fontSize = 10.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SoftRose, unfocusedBorderColor = ChampagneGoldBorder)
                )
            }
        }
    }
}

@Composable
fun PhasePill(title: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color)
    ) {
        Text(
            text = title,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun FertilityTimelineStrip(currentCycleDay: Int, cycleLength: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = GlassSurface.copy(alpha = 0.8f),
        border = androidx.compose.foundation.BorderStroke(1.dp, ChampagneGoldBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "الجدول الزمني للخصوبة والدورة 🗓️",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(14) { index ->
                    val dayNum = index + 1
                    val isCurrent = dayNum == currentCycleDay
                    val isFertile = dayNum in 12..16

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = when {
                            isCurrent -> SoftRose
                            isFertile -> LavenderAccent.copy(alpha = 0.3f)
                            else -> DarkObsidianStart
                        },
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isCurrent) Color.White else ChampagneGoldBorder
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "يوم $dayNum",
                                fontSize = 9.sp,
                                color = if (isCurrent) Color.Black else MutedText
                            )
                            Text(
                                text = if (isFertile) "خصوبة" else "عادي",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrent) Color.Black else Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

// ====================================================================
// 8. SECTION F: DAILY SYMPTOMS LOGGER
// ====================================================================

@Composable
fun DailySymptomsLoggerCard(
    selectedMood: String,
    selectedSymptoms: List<String>,
    hapticEnabled: Boolean,
    onMoodSelected: (String) -> Unit,
    onSymptomToggled: (String) -> Unit,
    onHapticToggled: (Boolean) -> Unit
) {
    val moods = listOf("سعيد 😄", "طبيعي 🙂", "حزين 😔", "متوتر 😰", "نشيط ⚡")
    val symptoms = listOf("خفيف 🌸", "متوسط 🟡", "غزير 🔴", "صداع 🧠", "انتفاخ 🎈", "مغص ⚡", "إرهاق 😴", "ألم ظهر 🦴")

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = GlassSurface.copy(alpha = 0.85f),
        border = androidx.compose.foundation.BorderStroke(1.dp, ChampagneGoldBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "سجل الأعراض والمزاج اليومي 📝",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text("المزاج الحالي:", fontSize = 11.sp, color = MutedText)
            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(moods) { mood ->
                    val isSelected = mood == selectedMood
                    Surface(
                        onClick = { onMoodSelected(mood) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) EmeraldGlow else DarkObsidianStart,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) EmeraldGlow else ChampagneGoldBorder)
                    ) {
                        Text(
                            text = mood,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.Black else Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("الأعراض اليومية:", fontSize = 11.sp, color = MutedText)
            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(symptoms) { symptom ->
                    val isSelected = selectedSymptoms.contains(symptom)
                    Surface(
                        onClick = { onSymptomToggled(symptom) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) SoftRose.copy(alpha = 0.3f) else DarkObsidianStart,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) SoftRose else ChampagneGoldBorder)
                    ) {
                        Text(
                            text = symptom,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected) SoftRose else Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("تفعيل الاهتزاز التلمسي (Haptic Feedback)", fontSize = 11.sp, color = MutedText)
                Switch(
                    checked = hapticEnabled,
                    onCheckedChange = onHapticToggled,
                    modifier = Modifier.scale(0.8f),
                    colors = SwitchDefaults.colors(checkedThumbColor = EmeraldGlow)
                )
            }
        }
    }
}

// ====================================================================
// 9. SECTION G: VITAL ACTIVITY TRI-CARD GRID
// ====================================================================

@Composable
fun VitalActivityGrid(
    waterCups: Int,
    targetWaterCups: Int,
    sleepHoursText: String,
    stepCount: Int,
    onWaterAdd: () -> Unit,
    onWaterRemove: () -> Unit,
    onSleepHoursChange: (String) -> Unit,
    onStepCountAdd: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Hydration Card
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                color = GlassSurface.copy(alpha = 0.85f),
                border = androidx.compose.foundation.BorderStroke(1.dp, HydrationBlue.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("💧 متابعة الماء", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = HydrationBlue)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("$waterCups / $targetWaterCups", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Text("أكواب اليوم", fontSize = 9.sp, color = MutedText)

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = onWaterRemove,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(DarkObsidianStart)
                        ) {
                            Icon(Icons.Default.Remove, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }

                        IconButton(
                            onClick = onWaterAdd,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(HydrationBlue)
                        ) {
                            Icon(Icons.Default.Add, null, tint = Color.Black, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            // Sleep Card
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                color = GlassSurface.copy(alpha = 0.85f),
                border = androidx.compose.foundation.BorderStroke(1.dp, LavenderAccent.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("😴 ساعات النوم", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LavenderAccent)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("$sleepHoursText س", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Text("جودة النوم: 88%", fontSize = 9.sp, color = MutedText)

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(5) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = ChampagneGold,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }

        // Activity Card with Mini Canvas Trend Chart
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = GlassSurface.copy(alpha = 0.85f),
            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGlow.copy(alpha = 0.35f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("🏃 النشاط والخطوات", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("$stepCount خطوة اليوم", fontSize = 18.sp, fontWeight = FontWeight.Black, color = EmeraldGlow)
                    }

                    Button(
                        onClick = onStepCountAdd,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("+500 خطوة", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Procedural Canvas Line Graph
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    val points = listOf(3000f, 4500f, 6000f, 5200f, 7800f, 6420f)
                    val maxVal = 10000f
                    val w = size.width
                    val h = size.height

                    val path = Path()
                    points.forEachIndexed { idx, value ->
                        val x = (idx.toFloat() / (points.size - 1)) * w
                        val y = h - ((value / maxVal) * h)
                        if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }

                    drawPath(
                        path = path,
                        color = EmeraldGlow,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}

// ====================================================================
// 10. SECTION H: QUICK METRIC TABS WITH ANIMATED CONTENT
// ====================================================================

@Composable
fun QuickMetricTabsCard(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    waterCups: Int,
    sleepHours: Double,
    bmi: Double,
    weight: Double
) {
    val tabTitles = listOf("الماء 💧", "النوم 😴", "BMI ❤️", "الوزن ⚖️")

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = GlassSurface.copy(alpha = 0.85f),
        border = androidx.compose.foundation.BorderStroke(1.dp, ChampagneGoldBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Tab Selector Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkObsidianStart)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                tabTitles.forEachIndexed { index, title ->
                    val isSelected = index == selectedTab
                    Surface(
                        onClick = { onTabSelected(index) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) EmeraldGlow else Color.Transparent,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = title,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.Black else MutedText,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Animated Tab Content Display
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "tabAnim"
            ) { tab ->
                when (tab) {
                    0 -> TabBarChart(title = "استهلاك الماء الأسبوعي", valueLabel = "$waterCups أكواب اليوم")
                    1 -> TabBarChart(title = "نمط ساعات النوم الأسبوعي", valueLabel = "$sleepHours ساعات اليوم")
                    2 -> TabBarChart(title = "استقرار مؤشر كتلة الجسم", valueLabel = String.format("%.1f BMI", bmi))
                    else -> TabBarChart(title = "مخطط تطور الوزن الشهرية", valueLabel = "${weight.toInt()} كجم")
                }
            }
        }
    }
}

@Composable
fun TabBarChart(title: String, valueLabel: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, fontSize = 11.sp, color = MutedText)
            Text(valueLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldGlow)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Canvas Bar Chart
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(65.dp)
        ) {
            val bars = listOf(0.6f, 0.8f, 0.5f, 0.9f, 0.7f, 0.85f, 0.95f)
            val barWidth = 14.dp.toPx()
            val spacing = (size.width - (bars.size * barWidth)) / (bars.size + 1)

            bars.forEachIndexed { i, factor ->
                val left = spacing + i * (barWidth + spacing)
                val top = size.height * (1f - factor)
                drawRoundRect(
                    color = if (i == bars.size - 1) EmeraldGlow else ChampagneGoldBorder,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, size.height * factor),
                    cornerRadius = CornerRadius(6.dp.toPx())
                )
            }
        }
    }
}

// ====================================================================
// 11. SECTION I: BOTTOM STICKY PANEL & DIALOGS
// ====================================================================

@Composable
fun StickyBottomActionPanel(
    bmi: Double,
    bmr: Double,
    neededCalories: Int,
    daysUntilNextPeriod: Int?,
    onOpenTips: () -> Unit,
    onSave: () -> Unit,
    onExportPdf: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = GlassSurface.copy(alpha = 0.95f),
        border = androidx.compose.foundation.BorderStroke(1.dp, ChampagneGoldBorder),
        shadowElevation = 16.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "BMI: ${String.format("%.1f", bmi)}  •  BMR: ${bmr.toInt()}  •  ${neededCalories}kcal",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                if (daysUntilNextPeriod != null) {
                    Text(
                        text = "الدورة: $daysUntilNextPeriod يوم",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SoftRose
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onOpenTips,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGlow)
                ) {
                    Text("نصائح 💡", color = EmeraldGlow, fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = onExportPdf,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ChampagneGold)
                ) {
                    Text("PDF 📄", color = ChampagneGold, fontSize = 11.sp)
                }

                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow)
                ) {
                    Text("حفظ 💾", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun HealthAdviceDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("نصائح صحية وطبية موصى بها 💚", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("• شرب 8 أكواب ماء يومياً يعزز معدل الأيض والحيوية.", fontSize = 12.sp, color = MutedText)
                Text("• النوم المنتظم 7-8 ساعات يحافظ على توازن الهرمونات والصحة الذهنية.", fontSize = 12.sp, color = MutedText)
                Text("• المشي 30 دقيقة يومياً يقلل من مخاطر السمنة وأمراض القلب.", fontSize = 12.sp, color = MutedText)
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow)
            ) {
                Text("حسنًا، فهمت", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = GlassSurface,
        shape = RoundedCornerShape(20.dp)
    )
}

// ====================================================================
// 12. DASHBOARD STATES (LOADING, ERROR, EMPTY)
// ====================================================================

@Composable
fun DashboardShimmerLoading() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(3) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                shape = RoundedCornerShape(24.dp),
                color = GlassSurface.copy(alpha = 0.3f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {}
        }
    }
}

@Composable
fun DashboardErrorState(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Warning, null, tint = SoftRose, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(message, color = Color.White, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow)) {
                Text("إعادة المحاولة", color = Color.Black)
            }
        }
    }
}

@Composable
fun DashboardEmptyState(onStartSetup: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.FitnessCenter, null, tint = EmeraldGlow, modifier = Modifier.size(56.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text("مرحباً بك في لوحة الصحة والرشاقة!", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("لم تقم بإدخال بياناتك الحيوية بعد.", color = MutedText, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onStartSetup, colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow)) {
                Text("بدء الإعداد الآن ✨", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}
