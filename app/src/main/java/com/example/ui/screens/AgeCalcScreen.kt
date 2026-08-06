package com.example.ui.screens

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.provider.CalendarContract
import androidx.core.content.FileProvider
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ToolScreenScaffold
import com.example.ui.theme.AppIcons
import com.example.ui.theme.CustomThemeColors
import com.example.model.CalcKey
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.time.*
import java.time.chrono.HijrahChronology
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import java.util.Locale

// ==========================================
// DATA MODELS & COLOR PALETTE
// ==========================================

data class SavedProfile(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val birthYear: Int,
    val birthMonth: Int,
    val birthDay: Int,
    val emoji: String,
    val colorHex: String
)

data class CalculationRecord(
    val id: String = java.util.UUID.randomUUID().toString(),
    val birthDateStr: String,
    val ageSummary: String,
    val nextBirthdayStr: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class NotificationSettings(
    val remind7Days: Boolean = true,
    val remind3Days: Boolean = true,
    val remind1Day: Boolean = true,
    val remindTime: String = "10:00"
)

data class ZodiacInfo(
    val nameAr: String,
    val symbol: String,
    val element: String
)

data class AgeCategory(
    val label: String,
    val emoji: String,
    val gradientColors: List<Color>
)

// Luxury Obsidian Theme Colors
private val ColorObsidianBgStart = Color(0xFF080A0F)
private val ColorObsidianBgEnd = Color(0xFF121620)
private val ColorGlassCard = Color(0xFF141926).copy(alpha = 0.85f)
private val ColorGoldBorder = Color(0xFFD4AF37)
private val ColorAmberGlow = Color(0xFFF59E0B)
private val ColorIceCyan = Color(0xFF00F2FE)
private val ColorEmeraldGreen = Color(0xFF10B981)
private val ColorLavender = Color(0xFFA78BFA)
private val ColorSlateMuted = Color(0xFF94A3B8)

// ==========================================
// MAIN COMPOSABLE SCREEN
// ==========================================

@Composable
fun AgeCalcScreen(colors: CustomThemeColors) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current

    // ------------------------------------------
    // State Persistence & Initialization
    // ------------------------------------------
    var selectedYear by rememberSaveable { mutableStateOf(1995) }
    var selectedMonth by rememberSaveable { mutableStateOf(5) }
    var selectedDay by rememberSaveable { mutableStateOf(15) }
    var activeProfileName by rememberSaveable { mutableStateOf("عمرك الشخصي") }

    // Live Clock Ticker State
    var nowDateTime by remember { mutableStateOf(LocalDateTime.now()) }
    val currentDate = remember(nowDateTime) { nowDateTime.toLocalDate() }

    LaunchedEffect(Unit) {
        while (true) {
            nowDateTime = LocalDateTime.now()
            delay(1000L) // Real-time tick every second
        }
    }

    // Profiles & History
    var profiles by remember { mutableStateOf(loadProfilesFromPrefs(context)) }
    var historyRecords by remember { mutableStateOf(loadHistoryFromPrefs(context)) }
    var notificationSettings by remember { mutableStateOf(loadNotificationSettings(context)) }

    // Expandable Sections
    var isBreakdownExpanded by rememberSaveable { mutableStateOf(false) }
    var isHistoryExpanded by rememberSaveable { mutableStateOf(false) }
    var isSettingsExpanded by rememberSaveable { mutableStateOf(false) }

    // Modals
    var showAddProfileDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmProfile by remember { mutableStateOf<SavedProfile?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Toast / Feedback
    var toastMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2500)
            toastMessage = null
        }
    }

    // Date Validation
    val birthDate = try {
        LocalDate.of(selectedYear, selectedMonth, selectedDay)
    } catch (e: Exception) {
        LocalDate.of(1995, 5, 15)
    }

    val isFutureDate = birthDate.isAfter(currentDate)

    // Age Calculations
    val period = remember(birthDate, currentDate) {
        if (isFutureDate) Period.ZERO else Period.between(birthDate, currentDate)
    }
    val ageYears = period.years
    val ageMonths = period.months
    val ageDays = period.days

    // Next Birthday
    val nextBirthdayDate = remember(birthDate, currentDate) {
        var next = birthDate.withYear(currentDate.year)
        if (next.isBefore(currentDate) || next.isEqual(currentDate)) {
            next = next.plusYears(1)
        }
        next
    }

    val nextBirthdayDateTime = remember(nextBirthdayDate) { nextBirthdayDate.atStartOfDay() }
    val durationToNextBirthday = remember(nowDateTime, nextBirthdayDateTime) {
        if (isFutureDate) Duration.ZERO
        else if (nowDateTime.isAfter(nextBirthdayDateTime)) Duration.ZERO
        else Duration.between(nowDateTime, nextBirthdayDateTime)
    }

    val daysToNextBirthday = durationToNextBirthday.toDays()
    val hoursToNextBirthday = (durationToNextBirthday.toHours() % 24)
    val minutesToNextBirthday = (durationToNextBirthday.toMinutes() % 60)
    val secondsToNextBirthday = (durationToNextBirthday.seconds % 60)
    val isBirthdayToday = (birthDate.month == currentDate.month && birthDate.dayOfMonth == currentDate.dayOfMonth)

    // Detailed Micro Breakdown Metrics
    val totalDaysLived = remember(birthDate, currentDate) {
        if (isFutureDate) 0L else ChronoUnit.DAYS.between(birthDate, currentDate)
    }
    val totalWeeksLived = totalDaysLived / 7
    val totalHoursLived = totalDaysLived * 24
    val totalMinutesLived = totalHoursLived * 60
    val totalSecondsLived = remember(totalMinutesLived, nowDateTime) {
        (totalMinutesLived * 60) + (nowDateTime.hour * 3600) + (nowDateTime.minute * 60) + nowDateTime.second
    }

    // Hijri & Zodiac
    val hijriStr = remember(birthDate) { formatHijriDate(birthDate) }
    val westernZodiac = remember(birthDate) { getWesternZodiac(birthDate.monthValue, birthDate.dayOfMonth) }
    val chineseZodiac = remember(birthDate) { getChineseZodiac(birthDate.year) }
    val ageCategory = remember(ageYears) { getAgeCategory(ageYears) }
    val birthDayName = remember(birthDate) { getArabicDayOfWeek(birthDate) }

    // Date Picker Launcher
    if (showDatePicker) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                selectedYear = year
                selectedMonth = month + 1
                selectedDay = dayOfMonth
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                showDatePicker = false
            },
            selectedYear,
            selectedMonth - 1,
            selectedDay
        ).apply {
            datePicker.maxDate = System.currentTimeMillis()
            setOnDismissListener { showDatePicker = false }
            show()
        }
    }

    // Main Layout Scaffold Wrapper
    ToolScreenScaffold(
        colors = colors,
        icon = AppIcons.forCalc(CalcKey.AGE),
        title = "حاسبة العمر الذكية",
        subtitle = "حاسبة العمر الدقيقة ومحرك ذكاء أعياد الميلاد والتحويل الهجري",
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
            // Background Procedural Grid Graphic
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gridStep = 60.dp.toPx()
                val gridPaint = ColorGoldBorder.copy(alpha = 0.03f)
                for (x in 0..size.width.toInt() step gridStep.toInt()) {
                    drawLine(gridPaint, Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height), strokeWidth = 1f)
                }
                for (y in 0..size.height.toInt() step gridStep.toInt()) {
                    drawLine(gridPaint, Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), strokeWidth = 1f)
                }
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

                // SECTION A: Header & Saved Profiles
                item(key = "section_header") {
                    HeaderCard(
                        notificationSettings = notificationSettings,
                        onToggleNotifications = { enabled ->
                            notificationSettings = notificationSettings.copy(remind7Days = enabled, remind3Days = enabled, remind1Day = enabled)
                            saveNotificationSettings(context, notificationSettings)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    )
                }

                item(key = "section_profiles_bar") {
                    ProfilesBar(
                        profiles = profiles,
                        activeProfileName = activeProfileName,
                        onSelectProfile = { profile ->
                            selectedYear = profile.birthYear
                            selectedMonth = profile.birthMonth
                            selectedDay = profile.birthDay
                            activeProfileName = profile.name
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onAddClick = { showAddProfileDialog = true },
                        onLongClickProfile = { profile ->
                            showDeleteConfirmProfile = profile
                        }
                    )
                }

                // SECTION B: Birth Date Input & Live Display
                item(key = "section_date_input") {
                    DateInputPanel(
                        selectedYear = selectedYear,
                        selectedMonth = selectedMonth,
                        selectedDay = selectedDay,
                        birthDayName = birthDayName,
                        hijriStr = hijriStr,
                        westernZodiac = westernZodiac,
                        chineseZodiac = chineseZodiac,
                        ageCategory = ageCategory,
                        isFutureDate = isFutureDate,
                        onOpenDatePicker = { showDatePicker = true },
                        onResetToToday = {
                            selectedYear = currentDate.year
                            selectedMonth = currentDate.monthValue
                            selectedDay = currentDate.dayOfMonth
                            activeProfileName = "عمرك الشخصي"
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            toastMessage = "تم إعادة الضبط لتاريخ اليوم"
                        }
                    )
                }

                if (isFutureDate) {
                    item(key = "error_future_date") {
                        ErrorFutureDateCard(
                            onReset = {
                                selectedYear = 1995
                                selectedMonth = 5
                                selectedDay = 15
                                activeProfileName = "عمرك الشخصي"
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        )
                    }
                } else {
                    item(key = "section_age_display") {
                        LiveAgeDisplayPanel(
                            years = ageYears,
                            months = ageMonths,
                            days = ageDays,
                            daysToNext = daysToNextBirthday,
                            hoursToNext = hoursToNextBirthday,
                            minutesToNext = minutesToNextBirthday,
                            secondsToNext = secondsToNextBirthday,
                            nextBirthdayDateStr = "${nextBirthdayDate.year}/${nextBirthdayDate.monthValue}/${nextBirthdayDate.dayOfMonth} (${getArabicDayOfWeek(nextBirthdayDate)})",
                            isBirthdayToday = isBirthdayToday,
                            birthDayName = birthDayName
                        )
                    }

                    item(key = "section_micro_breakdown") {
                        MicroBreakdownMatrix(
                            totalDays = totalDaysLived,
                            totalWeeks = totalWeeksLived,
                            totalHours = totalHoursLived,
                            totalMinutes = totalMinutesLived,
                            totalSeconds = totalSecondsLived
                        )
                    }

                    // SECTION C: Action Buttons Bar
                    item(key = "section_actions_bar") {
                        ActionButtonsBar(
                            onCalendarReminder = {
                                addBirthdayToSystemCalendar(context, activeProfileName, selectedMonth, selectedDay)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                toastMessage = "تم فتح التقويم لإضافة التذكير"
                            },
                            onCopyText = {
                                val summary = "عمر $activeProfileName الحالي: $ageYears سنة، $ageMonths شهر، و $ageDays يوم.\nتاريخ الميلاد: $selectedYear/$selectedMonth/$selectedDay ($hijriStr).\nالبرج: ${westernZodiac.nameAr} ${westernZodiac.symbol} | الصيني: ${chineseZodiac.nameAr} ${chineseZodiac.symbol}.\nعيد الميلاد القادم بعد: $daysToNextBirthday يوم."
                                clipboardManager.setText(AnnotatedString(summary))
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                toastMessage = "تم نسخ التفاصيل إلى الحافظة بنجاح"
                            },
                            onShareImage = {
                                generateAndShareCardImage(
                                    context = context,
                                    profileName = activeProfileName,
                                    birthDateStr = "$selectedYear/$selectedMonth/$selectedDay",
                                    ageYears = ageYears,
                                    ageMonths = ageMonths,
                                    ageDays = ageDays,
                                    hijriDateStr = hijriStr,
                                    westernZodiac = westernZodiac,
                                    chineseZodiac = chineseZodiac,
                                    totalDays = totalDaysLived
                                )
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                toastMessage = "جاري تحضير بطاقة المشاركة..."
                            },
                            onSaveHistory = {
                                val newRecord = CalculationRecord(
                                    birthDateStr = "$selectedYear/$selectedMonth/$selectedDay",
                                    ageSummary = "$ageYears سنة و $ageMonths شهر و $ageDays يوم",
                                    nextBirthdayStr = "$daysToNextBirthday يوم متبقي"
                                )
                                historyRecords = (listOf(newRecord) + historyRecords).take(20)
                                saveHistoryToPrefs(context, historyRecords)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                toastMessage = "تم حفظ العمليات إلى السجل"
                            },
                            onReset = {
                                selectedYear = 1995
                                selectedMonth = 5
                                selectedDay = 15
                                activeProfileName = "عمرك الشخصي"
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                toastMessage = "تمت إعادة الضبط"
                            }
                        )
                    }

                    // SECTION D: Expandable Calculation Math Breakdown
                    item(key = "section_math_breakdown") {
                        ExpandableSectionCard(
                            title = "طريقة الحساب الدقيقة (Period Mathematics)",
                            icon = Icons.Outlined.Calculate,
                            isExpanded = isBreakdownExpanded,
                            onToggle = { isBreakdownExpanded = !isBreakdownExpanded }
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "تعتمد الحسابات على معيار java.time.Period و java.time.Duration الرسمي لضمان الدقة وتفادي أخطاء السنوات الكبيسة والأشهر ذات الـ 28 أو 30 أو 31 يوماً:",
                                    fontSize = 12.sp,
                                    color = ColorSlateMuted
                                )
                                MetricRow(label = "تاريخ الميلاد المحسوب", value = "$selectedYear / $selectedMonth / $selectedDay")
                                MetricRow(label = "تاريخ اليوم الجاري", value = "${currentDate.year} / ${currentDate.monthValue} / ${currentDate.dayOfMonth}")
                                MetricRow(label = "الفارق الدقيق للسنوات", value = "$ageYears سنة")
                                MetricRow(label = "الفارق الدقيق للشهور", value = "$ageMonths شهر")
                                MetricRow(label = "الفارق الدقيق للأيام", value = "$ageDays يوم")
                                MetricRow(label = "سنة كبيسة بـ 366 يوماً", value = if (Year.of(selectedYear).isLeap) "نعم" else "لا")
                            }
                        }
                    }

                    // SECTION E: History Drawer
                    item(key = "section_history_drawer") {
                        ExpandableSectionCard(
                            title = "سجل الحسابات المحفوظة (${historyRecords.size})",
                            icon = Icons.Outlined.History,
                            isExpanded = isHistoryExpanded,
                            onToggle = { isHistoryExpanded = !isHistoryExpanded }
                        ) {
                            if (historyRecords.isEmpty()) {
                                Text("لا يوجد عمليات محفوظة بالسجل حالياً", fontSize = 12.sp, color = ColorSlateMuted, modifier = Modifier.padding(8.dp))
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("السجل الإجمالي", fontSize = 12.sp, color = ColorAmberGlow, fontWeight = FontWeight.Bold)
                                        TextButton(onClick = {
                                            historyRecords = emptyList()
                                            saveHistoryToPrefs(context, emptyList())
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }) {
                                            Text("مسح السجل", color = Color.Red.copy(alpha = 0.8f), fontSize = 11.sp)
                                        }
                                    }
                                    historyRecords.forEach { record ->
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
                                                    Text("تاريخ الميلاد: ${record.birthDateStr}", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                    Text(record.ageSummary, fontSize = 11.sp, color = ColorIceCyan)
                                                }
                                                IconButton(onClick = {
                                                    val parts = record.birthDateStr.split("/")
                                                    if (parts.size == 3) {
                                                        selectedYear = parts[0].toIntOrNull() ?: 1995
                                                        selectedMonth = parts[1].toIntOrNull() ?: 5
                                                        selectedDay = parts[2].toIntOrNull() ?: 15
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        toastMessage = "تم تحميل التاريخ من السجل"
                                                    }
                                                }) {
                                                    Icon(Icons.Outlined.Replay, contentDescription = "استعادة", tint = ColorAmberGlow)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // SECTION F: Notification Settings
                    item(key = "section_notification_settings") {
                        ExpandableSectionCard(
                            title = "إعدادات وتنبيهات أعياد الميلاد",
                            icon = Icons.Outlined.NotificationsActive,
                            isExpanded = isSettingsExpanded,
                            onToggle = { isSettingsExpanded = !isSettingsExpanded }
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                SettingSwitchRow(
                                    label = "تنبيه قبل 7 أيام من المناسبة",
                                    checked = notificationSettings.remind7Days,
                                    onCheckedChange = {
                                        notificationSettings = notificationSettings.copy(remind7Days = it)
                                        saveNotificationSettings(context, notificationSettings)
                                    }
                                )
                                SettingSwitchRow(
                                    label = "تنبيه قبل 3 أيام من المناسبة",
                                    checked = notificationSettings.remind3Days,
                                    onCheckedChange = {
                                        notificationSettings = notificationSettings.copy(remind3Days = it)
                                        saveNotificationSettings(context, notificationSettings)
                                    }
                                )
                                SettingSwitchRow(
                                    label = "تنبيه قبل يوم واحد من المناسبة",
                                    checked = notificationSettings.remind1Day,
                                    onCheckedChange = {
                                        notificationSettings = notificationSettings.copy(remind1Day = it)
                                        saveNotificationSettings(context, notificationSettings)
                                    }
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("توقيت التذكير اليومي", fontSize = 12.sp, color = Color.White)
                                    Text(notificationSettings.remindTime, fontSize = 13.sp, color = ColorGoldBorder, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Offline Notice Footer
                item(key = "offline_notice") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Verified, contentDescription = null, tint = ColorEmeraldGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("جميع الحسابات تعمل أوفلاين 100% بدون إنترنت", fontSize = 11.sp, color = ColorSlateMuted)
                    }
                }
            }
        }
    }

    // Modal Dialog: Add New Profile
    if (showAddProfileDialog) {
        AddProfileDialog(
            onDismiss = { showAddProfileDialog = false },
            onConfirm = { name, year, month, day, emoji, colorHex ->
                val newP = SavedProfile(
                    name = name,
                    birthYear = year,
                    birthMonth = month,
                    birthDay = day,
                    emoji = emoji,
                    colorHex = colorHex
                )
                profiles = (profiles + newP).take(10)
                saveProfilesToPrefs(context, profiles)
                selectedYear = year
                selectedMonth = month
                selectedDay = day
                activeProfileName = name
                showAddProfileDialog = false
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                toastMessage = "تم إضافة الملف ($name) بنجاح"
            }
        )
    }

    // Modal Dialog: Confirm Delete Profile
    if (showDeleteConfirmProfile != null) {
        val targetProfile = showDeleteConfirmProfile!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirmProfile = null },
            title = { Text("حذف الملف", color = Color.White) },
            text = { Text("هل أنت تأكد من حذف ملف '${targetProfile.name}'؟", color = ColorSlateMuted) },
            confirmButton = {
                Button(
                    onClick = {
                        profiles = profiles.filter { it.id != targetProfile.id }
                        saveProfilesToPrefs(context, profiles)
                        showDeleteConfirmProfile = null
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        toastMessage = "تم حذف الملف بنجاح"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("حذف", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmProfile = null }) {
                    Text("إلغاء", color = Color.White)
                }
            },
            containerColor = Color(0xFF181E2C)
        )
    }
}

// ==========================================
// COMPOSABLE SUB-COMPONENTS
// ==========================================

@Composable
private fun HeaderCard(
    notificationSettings: NotificationSettings,
    onToggleNotifications: (Boolean) -> Unit
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
            // Procedural Canvas Cake Icon
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(ColorAmberGlow.copy(alpha = 0.3f), Color.Transparent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(36.dp)) {
                    val width = size.width
                    val height = size.height
                    val goldColor = ColorGoldBorder

                    // Cake Base
                    drawRoundRect(
                        color = goldColor,
                        topLeft = Offset(width * 0.15f, height * 0.5f),
                        size = androidx.compose.ui.geometry.Size(width * 0.7f, height * 0.4f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
                    )
                    // Candle
                    drawLine(
                        color = ColorIceCyan,
                        start = Offset(width * 0.5f, height * 0.25f),
                        end = Offset(width * 0.5f, height * 0.5f),
                        strokeWidth = 6f
                    )
                    // Flame
                    drawCircle(
                        color = ColorAmberGlow,
                        center = Offset(width * 0.5f, height * 0.18f),
                        radius = 8f
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "حاسبة العمر الذكية",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "احسب عمرك بالتفصيل ودقائق الميلاد والمناسبات القادمة",
                    fontSize = 11.sp,
                    color = ColorSlateMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Quick Notifications Toggle Switch
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Switch(
                    checked = notificationSettings.remind7Days,
                    onCheckedChange = onToggleNotifications,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ColorGoldBorder,
                        checkedTrackColor = ColorAmberGlow.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.scale(0.8f)
                )
                Text("التنبيهات", fontSize = 9.sp, color = ColorSlateMuted)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProfilesBar(
    profiles: List<SavedProfile>,
    activeProfileName: String,
    onSelectProfile: (SavedProfile) -> Unit,
    onAddClick: () -> Unit,
    onLongClickProfile: (SavedProfile) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("الملفات المحفوظة (${profiles.size}/10)", fontSize = 12.sp, color = ColorGoldBorder, fontWeight = FontWeight.Bold)
            Text("اضغط مطولاً للحذف", fontSize = 10.sp, color = ColorSlateMuted)
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Add Profile Button
            item {
                Surface(
                    color = ColorAmberGlow.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, ColorAmberGlow.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .height(42.dp)
                        .clickable { onAddClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = ColorAmberGlow, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إضافة ملف", fontSize = 12.sp, color = ColorAmberGlow, fontWeight = FontWeight.Bold)
                    }
                }
            }

            items(profiles, key = { it.id }) { profile ->
                val isSelected = profile.name == activeProfileName
                val chipBg = if (isSelected) ColorAmberGlow.copy(alpha = 0.3f) else ColorGlassCard
                val borderCol = if (isSelected) ColorGoldBorder else Color.White.copy(alpha = 0.1f)

                Surface(
                    color = chipBg,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, borderCol),
                    modifier = Modifier
                        .height(42.dp)
                        .combinedClickable(
                            onClick = { onSelectProfile(profile) },
                            onLongClick = { onLongClickProfile(profile) }
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(profile.emoji, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            profile.name,
                            fontSize = 12.sp,
                            color = if (isSelected) Color.White else ColorSlateMuted,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DateInputPanel(
    selectedYear: Int,
    selectedMonth: Int,
    selectedDay: Int,
    birthDayName: String,
    hijriStr: String,
    westernZodiac: ZodiacInfo,
    chineseZodiac: ZodiacInfo,
    ageCategory: AgeCategory,
    isFutureDate: Boolean,
    onOpenDatePicker: () -> Unit,
    onResetToToday: () -> Unit
) {
    Surface(
        color = ColorGlassCard,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, ColorGoldBorder.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("تاريخ الميلاد المختار", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                TextButton(onClick = onResetToToday) {
                    Text("اليوم 📅", fontSize = 12.sp, color = ColorIceCyan)
                }
            }

            // Clickable Date Picker Display Card
            Surface(
                color = Color.Black.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, ColorIceCyan.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenDatePicker() }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "$selectedYear / ${String.format(Locale.US, "%02d", selectedMonth)} / ${String.format(Locale.US, "%02d", selectedDay)}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = ColorAmberGlow
                        )
                        Text("يوم الميلاد: $birthDayName", fontSize = 12.sp, color = ColorSlateMuted)
                    }
                    Icon(Icons.Filled.CalendarMonth, contentDescription = "اختر التاريخ", tint = ColorGoldBorder, modifier = Modifier.size(28.dp))
                }
            }

            // Dual Calendar & Badges Row
            if (!isFutureDate) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Hijri Badge
                    Surface(
                        color = Color(0xFF1E2638),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("الموافق الهجري", fontSize = 10.sp, color = ColorSlateMuted)
                            Text(hijriStr, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ColorIceCyan, textAlign = TextAlign.Center)
                        }
                    }

                    // Category Badge
                    Surface(
                        color = ageCategory.gradientColors.first().copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ageCategory.gradientColors.first().copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("الفئة العمرية", fontSize = 10.sp, color = ColorSlateMuted)
                            Text("${ageCategory.emoji} ${ageCategory.label}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                // Zodiac Signs Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = ColorLavender.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(westernZodiac.symbol, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("البرج: ${westernZodiac.nameAr}", fontSize = 11.sp, color = ColorLavender, fontWeight = FontWeight.Bold)
                        }
                    }

                    Surface(
                        color = ColorEmeraldGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(chineseZodiac.symbol, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("الصيني: ${chineseZodiac.nameAr}", fontSize = 11.sp, color = ColorEmeraldGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveAgeDisplayPanel(
    years: Int,
    months: Int,
    days: Int,
    daysToNext: Long,
    hoursToNext: Long,
    minutesToNext: Long,
    secondsToNext: Long,
    nextBirthdayDateStr: String,
    isBirthdayToday: Boolean,
    birthDayName: String
) {
    Surface(
        color = ColorGlassCard,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, ColorAmberGlow.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("العمر الحالي بالكامل", fontSize = 13.sp, color = ColorSlateMuted, fontWeight = FontWeight.Bold)

            // Primary Animated Age Display Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AgeUnitCard(value = years, label = "سنوات", color = ColorAmberGlow)
                AgeUnitCard(value = months, label = "شهور", color = ColorIceCyan)
                AgeUnitCard(value = days, label = "أيام", color = ColorEmeraldGreen)
            }

            Divider(color = Color.White.copy(alpha = 0.1f))

            // Birthday Countdown Box
            if (isBirthdayToday) {
                Surface(
                    color = ColorEmeraldGreen.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, ColorEmeraldGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🎉 اليوم هو عيد ميلادك! 🎉", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Text("نتمنى لك عاماً سعيداً مليئاً بالصحة والنجاح والخير!", fontSize = 12.sp, color = ColorIceCyan, textAlign = TextAlign.Center)
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("موعد عيد الميلاد القادم: $nextBirthdayDateStr", fontSize = 12.sp, color = ColorGoldBorder)

                    // Countdown Live Ticker Units
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CountdownUnitChip(value = daysToNext, label = "يوم")
                        Text(":", color = ColorSlateMuted, fontWeight = FontWeight.Bold)
                        CountdownUnitChip(value = hoursToNext, label = "ساعة")
                        Text(":", color = ColorSlateMuted, fontWeight = FontWeight.Bold)
                        CountdownUnitChip(value = minutesToNext, label = "دقيقة")
                        Text(":", color = ColorSlateMuted, fontWeight = FontWeight.Bold)
                        CountdownUnitChip(value = secondsToNext, label = "ثانية")
                    }
                }
            }
        }
    }
}

@Composable
private fun AgeUnitCard(value: Int, label: String, color: Color) {
    Surface(
        color = Color.Black.copy(alpha = 0.35f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = Modifier.width(90.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "$value",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
            Text(label, fontSize = 11.sp, color = ColorSlateMuted)
        }
    }
}

@Composable
private fun CountdownUnitChip(value: Long, label: String) {
    Surface(
        color = Color(0xFF1B2232),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.width(55.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                String.format(Locale.US, "%02d", value),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(label, fontSize = 9.sp, color = ColorSlateMuted)
        }
    }
}

@Composable
private fun MicroBreakdownMatrix(
    totalDays: Long,
    totalWeeks: Long,
    totalHours: Long,
    totalMinutes: Long,
    totalSeconds: Long
) {
    Surface(
        color = ColorGlassCard,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("مصفوفة المكونات الزمانية التفصيلية", fontSize = 13.sp, color = ColorGoldBorder, fontWeight = FontWeight.Bold)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BreakdownItemCard(modifier = Modifier.weight(1f), label = "إجمالي الأيام", value = String.format(Locale.US, "%,d يوم", totalDays))
                BreakdownItemCard(modifier = Modifier.weight(1f), label = "إجمالي الأسابيع", value = String.format(Locale.US, "%,d أسبوع", totalWeeks))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BreakdownItemCard(modifier = Modifier.weight(1f), label = "إجمالي الساعات", value = String.format(Locale.US, "%,d ساعة", totalHours))
                BreakdownItemCard(modifier = Modifier.weight(1f), label = "إجمالي الدقائق", value = String.format(Locale.US, "%,d دقيقة", totalMinutes))
            }
            BreakdownItemCard(
                modifier = Modifier.fillMaxWidth(),
                label = "إجمالي الثواني التي عشتها حياً (مباشر)",
                value = String.format(Locale.US, "%,d ثانية", totalSeconds),
                valueColor = ColorAmberGlow
            )
        }
    }
}

@Composable
private fun BreakdownItemCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    valueColor: Color = ColorIceCyan
) {
    Surface(
        color = Color.Black.copy(alpha = 0.3f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, fontSize = 10.sp, color = ColorSlateMuted)
            Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = valueColor)
        }
    }
}

@Composable
private fun ActionButtonsBar(
    onCalendarReminder: () -> Unit,
    onCopyText: () -> Unit,
    onShareImage: () -> Unit,
    onSaveHistory: () -> Unit,
    onReset: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onCalendarReminder,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Event, contentDescription = null, tint = ColorIceCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("تذكير التقويم 📅", fontSize = 11.sp, color = Color.White)
            }

            Button(
                onClick = onShareImage,
                colors = ButtonDefaults.buttonColors(containerColor = ColorAmberGlow),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Share, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("مشاركة كصورة 🖼️", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onCopyText,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, ColorSlateMuted.copy(alpha = 0.5f)),
                modifier = Modifier.weight(1f)
            ) {
                Text("نسخ التفاصيل 📋", fontSize = 11.sp, color = Color.White)
            }

            OutlinedButton(
                onClick = onSaveHistory,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, ColorEmeraldGreen.copy(alpha = 0.5f)),
                modifier = Modifier.weight(1f)
            ) {
                Text("حفظ بالسجل 💾", fontSize = 11.sp, color = ColorEmeraldGreen)
            }

            IconButton(
                onClick = onReset,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
                    .size(40.dp)
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = "إعادة ضبط", tint = Color.White)
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
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
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
                    Text(title, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
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
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Divider(color = Color.White.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(12.dp))
                    content()
                }
            }
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 11.sp, color = ColorSlateMuted)
        Text(value, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SettingSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = Color.White)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = ColorGoldBorder)
        )
    }
}

@Composable
private fun ErrorFutureDateCard(onReset: () -> Unit) {
    Surface(
        color = Color.Red.copy(alpha = 0.15f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = Color.Red, modifier = Modifier.size(36.dp))
            Text("تاريخ الميلاد في المستقبل!", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(
                "يرجى اختيار تاريخ ميلاد سابق أو يساوي اليوم للتمكن من حساب العمر الدقيق والمكونات الزمانية.",
                fontSize = 12.sp,
                color = ColorSlateMuted,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onReset,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("إعادة الضبط", color = Color.White)
            }
        }
    }
}

@Composable
private fun AddProfileDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Int, Int, Int, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var yearStr by remember { mutableStateOf("1998") }
    var monthStr by remember { mutableStateOf("7") }
    var dayStr by remember { mutableStateOf("10") }
    var selectedEmoji by remember { mutableStateOf("👨") }

    val emojis = listOf("👨", "👩", "👶", "👦", "👧", "🧓", "❤️", "👑", "🌟", "🎉")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة ملف جديد", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الشخص (مثال: أمي)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ColorGoldBorder,
                        unfocusedBorderColor = ColorSlateMuted,
                        focusedLabelColor = ColorGoldBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = dayStr,
                        onValueChange = { dayStr = it },
                        label = { Text("يوم") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = monthStr,
                        onValueChange = { monthStr = it },
                        label = { Text("شهر") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = yearStr,
                        onValueChange = { yearStr = it },
                        label = { Text("سنة") },
                        modifier = Modifier.weight(1.5f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }

                Text("اختر رمزاً:", fontSize = 12.sp, color = ColorSlateMuted)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(emojis) { em ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (em == selectedEmoji) ColorGoldBorder.copy(alpha = 0.4f) else Color.Transparent)
                                .clickable { selectedEmoji = em },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(em, fontSize = 18.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val y = yearStr.toIntOrNull() ?: 1998
                        val m = monthStr.toIntOrNull() ?: 7
                        val d = dayStr.toIntOrNull() ?: 10
                        onConfirm(name.trim(), y, m, d, selectedEmoji, "#D4AF37")
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ColorGoldBorder)
            ) {
                Text("حفظ الملف", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = Color.White)
            }
        },
        containerColor = Color(0xFF181E2C)
    )
}

// ==========================================
// UTILITY FUNCTIONS & CALCULATIONS
// ==========================================

private fun getArabicDayOfWeek(date: LocalDate): String {
    return when (date.dayOfWeek) {
        DayOfWeek.MONDAY -> "الإثنين"
        DayOfWeek.TUESDAY -> "الثلاثاء"
        DayOfWeek.WEDNESDAY -> "الأربعاء"
        DayOfWeek.THURSDAY -> "الخميس"
        DayOfWeek.FRIDAY -> "الجمعة"
        DayOfWeek.SATURDAY -> "السبت"
        DayOfWeek.SUNDAY -> "الأحد"
    }
}

private fun formatHijriDate(date: LocalDate): String {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val hijrahDate = HijrahDate.from(date)
            val day = hijrahDate.get(ChronoField.DAY_OF_MONTH)
            val month = hijrahDate.get(ChronoField.MONTH_OF_YEAR)
            val year = hijrahDate.get(ChronoField.YEAR)

            val monthNames = listOf("محرم", "صفر", "ربيع الأول", "ربيع الثاني", "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة")
            val monthName = monthNames.getOrElse(month - 1) { "" }
            "$day $monthName $year هـ"
        } else {
            val hijriYear = ((date.year - 622) * 1.0307).toInt()
            val monthNames = listOf("محرم", "صفر", "ربيع الأول", "ربيع الثاني", "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة")
            val monthName = monthNames.getOrElse((date.monthValue - 1) % 12) { "" }
            "${date.dayOfMonth} $monthName $hijriYear هـ"
        }
    } catch (e: Exception) {
        "1410 هـ"
    }
}

private fun getWesternZodiac(month: Int, day: Int): ZodiacInfo {
    return when (month) {
        1 -> if (day <= 19) ZodiacInfo("الجدي", "♑", "ترابي") else ZodiacInfo("الدلو", "♒", "هوائي")
        2 -> if (day <= 18) ZodiacInfo("الدلو", "♒", "هوائي") else ZodiacInfo("الحوت", "<ctrl42>", "مائي")
        3 -> if (day <= 20) ZodiacInfo("الحوت", "♓", "مائي") else ZodiacInfo("الحمل", "♈", "ناري")
        4 -> if (day <= 19) ZodiacInfo("الحمل", "♈", "ناري") else ZodiacInfo("الثور", "♉", "ترابي")
        5 -> if (day <= 20) ZodiacInfo("الثور", "♉", "ترابي") else ZodiacInfo("الجوزاء", "♊", "هوائي")
        6 -> if (day <= 20) ZodiacInfo("الجوزاء", "♊", "هوائي") else ZodiacInfo("السرطان", "♋", "مائي")
        7 -> if (day <= 22) ZodiacInfo("السرطان", "♋", "مائي") else ZodiacInfo("الأسد", "♌", "ناري")
        8 -> if (day <= 22) ZodiacInfo("الأسد", "♌", "ناري") else ZodiacInfo("العذراء", "♍", "ترابي")
        9 -> if (day <= 22) ZodiacInfo("العذراء", "♍", "ترابي") else ZodiacInfo("الميزان", "♎", "هوائي")
        10 -> if (day <= 22) ZodiacInfo("الميزان", "♎", "هوائي") else ZodiacInfo("العقرب", "♏", "مائي")
        11 -> if (day <= 21) ZodiacInfo("العقرب", "♏", "مائي") else ZodiacInfo("القوس", "♐", "ناري")
        12 -> if (day <= 21) ZodiacInfo("القوس", "♐", "ناري") else ZodiacInfo("الجدي", "♑", "ترابي")
        else -> ZodiacInfo("الحمل", "♈", "ناري")
    }
}

private fun getChineseZodiac(year: Int): ZodiacInfo {
    val animals = listOf(
        ZodiacInfo("القرد", "🐒", "معدن"),
        ZodiacInfo("الديك", "🐓", "معدن"),
        ZodiacInfo("الكلب", "🐕", "الأرض"),
        ZodiacInfo("الخنزير", "🐖", "ماء"),
        ZodiacInfo("الفأر", "🐀", "ماء"),
        ZodiacInfo("الثور", "🐂", "الأرض"),
        ZodiacInfo("النمر", "🐅", "خشب"),
        ZodiacInfo("الأرنب", "🐇", "خشب"),
        ZodiacInfo("التنين", "🐉", "الأرض"),
        ZodiacInfo("الأفعى", "🐍", "نار"),
        ZodiacInfo("الحصان", "🐎", "نار"),
        ZodiacInfo("الخروف", "🐐", "الأرض")
    )
    return animals[Math.abs(year % 12)]
}

private fun getAgeCategory(years: Int): AgeCategory {
    return when {
        years <= 12 -> AgeCategory("طفل", "👶", listOf(Color(0xFF60A5FA), Color(0xFF3B82F6)))
        years <= 30 -> AgeCategory("شاب", "👦", listOf(Color(0xFF34D399), Color(0xFF10B981)))
        years <= 55 -> AgeCategory("كهول", "👨", listOf(Color(0xFFF59E0B), Color(0xFFD4AF37)))
        else -> AgeCategory("شيوخ", "🧓", listOf(Color(0xFFA78BFA), Color(0xFF8B5CF6)))
    }
}

// ==========================================
// PREFERENCES PERSISTENCE HELPERS
// ==========================================

private const val PREFS_NAME = "age_calc_prefs"
private const val KEY_PROFILES = "saved_profiles_json"
private const val KEY_HISTORY = "history_records_json"
private const val KEY_NOTIFS = "notif_settings_json"

private fun loadProfilesFromPrefs(context: Context): List<SavedProfile> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val jsonStr = prefs.getString(KEY_PROFILES, null) ?: return listOf(
        SavedProfile(name = "عمرك الشخصي", birthYear = 1995, birthMonth = 5, birthDay = 15, emoji = "👤", colorHex = "#D4AF37")
    )
    return try {
        val list = mutableListOf<SavedProfile>()
        val arr = JSONArray(jsonStr)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                SavedProfile(
                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                    name = obj.getString("name"),
                    birthYear = obj.getInt("birthYear"),
                    birthMonth = obj.getInt("birthMonth"),
                    birthDay = obj.getInt("birthDay"),
                    emoji = obj.optString("emoji", "👤"),
                    colorHex = obj.optString("colorHex", "#D4AF37")
                )
            )
        }
        if (list.isEmpty()) listOf(SavedProfile(name = "عمرك الشخصي", birthYear = 1995, birthMonth = 5, birthDay = 15, emoji = "👤", colorHex = "#D4AF37")) else list
    } catch (e: Exception) {
        listOf(SavedProfile(name = "عمرك الشخصي", birthYear = 1995, birthMonth = 5, birthDay = 15, emoji = "👤", colorHex = "#D4AF37"))
    }
}

private fun saveProfilesToPrefs(context: Context, profiles: List<SavedProfile>) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val arr = JSONArray()
    profiles.forEach { p ->
        val obj = JSONObject()
        obj.put("id", p.id)
        obj.put("name", p.name)
        obj.put("birthYear", p.birthYear)
        obj.put("birthMonth", p.birthMonth)
        obj.put("birthDay", p.birthDay)
        obj.put("emoji", p.emoji)
        obj.put("colorHex", p.colorHex)
        arr.put(obj)
    }
    prefs.edit().putString(KEY_PROFILES, arr.toString()).apply()
}

private fun loadHistoryFromPrefs(context: Context): List<CalculationRecord> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val jsonStr = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
    return try {
        val list = mutableListOf<CalculationRecord>()
        val arr = JSONArray(jsonStr)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                CalculationRecord(
                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                    birthDateStr = obj.getString("birthDateStr"),
                    ageSummary = obj.getString("ageSummary"),
                    nextBirthdayStr = obj.getString("nextBirthdayStr"),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                )
            )
        }
        list
    } catch (e: Exception) {
        emptyList()
    }
}

private fun saveHistoryToPrefs(context: Context, records: List<CalculationRecord>) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val arr = JSONArray()
    records.forEach { r ->
        val obj = JSONObject()
        obj.put("id", r.id)
        obj.put("birthDateStr", r.birthDateStr)
        obj.put("ageSummary", r.ageSummary)
        obj.put("nextBirthdayStr", r.nextBirthdayStr)
        obj.put("timestamp", r.timestamp)
        arr.put(obj)
    }
    prefs.edit().putString(KEY_HISTORY, arr.toString()).apply()
}

private fun loadNotificationSettings(context: Context): NotificationSettings {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val jsonStr = prefs.getString(KEY_NOTIFS, null) ?: return NotificationSettings()
    return try {
        val obj = JSONObject(jsonStr)
        NotificationSettings(
            remind7Days = obj.optBoolean("remind7Days", true),
            remind3Days = obj.optBoolean("remind3Days", true),
            remind1Day = obj.optBoolean("remind1Day", true),
            remindTime = obj.optString("remindTime", "10:00")
        )
    } catch (e: Exception) {
        NotificationSettings()
    }
}

private fun saveNotificationSettings(context: Context, settings: NotificationSettings) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val obj = JSONObject()
    obj.put("remind7Days", settings.remind7Days)
    obj.put("remind3Days", settings.remind3Days)
    obj.put("remind1Day", settings.remind1Day)
    obj.put("remindTime", settings.remindTime)
    prefs.edit().putString(KEY_NOTIFS, obj.toString()).apply()
}

// ==========================================
// SYSTEM INTENTS & CANVAS SHARE GENERATOR
// ==========================================

private fun addBirthdayToSystemCalendar(context: Context, name: String, month: Int, day: Int) {
    try {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, "🎂 عيد ميلاد $name")
            putExtra(CalendarContract.Events.DESCRIPTION, "تذكير سنوياً بموعد عيد ميلاد $name")
            putExtra(CalendarContract.Events.RRULE, "FREQ=YEARLY")
            val cal = java.util.Calendar.getInstance()
            cal.set(java.util.Calendar.MONTH, month - 1)
            cal.set(java.util.Calendar.DAY_OF_MONTH, day)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, cal.timeInMillis)
            putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun generateAndShareCardImage(
    context: Context,
    profileName: String,
    birthDateStr: String,
    ageYears: Int,
    ageMonths: Int,
    ageDays: Int,
    hijriDateStr: String,
    westernZodiac: ZodiacInfo,
    chineseZodiac: ZodiacInfo,
    totalDays: Long
) {
    try {
        val width = 1080
        val height = 1350
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background Gradient
        val bgPaint = Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, height.toFloat(), 0xFF080A0F.toInt(), 0xFF121620.toInt(), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Card Container
        val cardPaint = Paint().apply {
            color = 0xFF141926.toInt()
            style = Paint.Style.FILL
        }
        val cardRect = RectF(60f, 80f, width - 60f, height - 80f)
        canvas.drawRoundRect(cardRect, 40f, 40f, cardPaint)

        // Gold Border
        val borderPaint = Paint().apply {
            color = 0xFFD4AF37.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRoundRect(cardRect, 40f, 40f, borderPaint)

        // Text Paints
        val titlePaint = Paint().apply {
            color = 0xFFD4AF37.toInt()
            textSize = 54f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val namePaint = Paint().apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 42f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val agePaint = Paint().apply {
            color = 0xFFF59E0B.toInt()
            textSize = 72f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val detailPaint = Paint().apply {
            color = 0xFF00F2FE.toInt()
            textSize = 34f
            textAlign = Paint.Align.CENTER
        }
        val footerPaint = Paint().apply {
            color = 0xFF94A3B8.toInt()
            textSize = 28f
            textAlign = Paint.Align.CENTER
        }

        // Draw Content onto Canvas
        canvas.drawText("🎂 حاسبة العمر الذكية 🎂", width / 2f, 180f, titlePaint)
        canvas.drawText("بطاقة عمر: $profileName", width / 2f, 270f, namePaint)

        canvas.drawText("$ageYears سنة و $ageMonths شهر و $ageDays يوم", width / 2f, 430f, agePaint)

        canvas.drawText("تاريخ الميلاد: $birthDateStr", width / 2f, 560f, detailPaint)
        canvas.drawText("الموافق الهجري: $hijriDateStr", width / 2f, 640f, detailPaint)

        canvas.drawText("البرج الغربي: ${westernZodiac.nameAr} ${westernZodiac.symbol}", width / 2f, 740f, namePaint)
        canvas.drawText("البرج الصيني: ${chineseZodiac.nameAr} ${chineseZodiac.symbol}", width / 2f, 820f, namePaint)

        canvas.drawText("إجمالي الأيام التي عشتها: $totalDays يوم", width / 2f, 940f, titlePaint)

        canvas.drawText("تم استخراج البطاقة عبر تطبيق حاسبة العمر الذكية", width / 2f, 1180f, footerPaint)

        // Save Bitmap to Cache
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, "age_card_share.png")
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()

        // Get Uri & Share Intent
        val contentUri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "مشاركة بطاقة العمر"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
