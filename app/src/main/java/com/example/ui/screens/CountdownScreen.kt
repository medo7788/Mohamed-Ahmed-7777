package com.example.ui.screens

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
import java.time.*
import java.time.chrono.HijrahChronology
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.ChronoField
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
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
private val ColorLavender = Color(0xFFA78BFA)
private val ColorSlateMuted = Color(0xFF94A3B8)

enum class EventCategory(val labelAr: String, val icon: String, val accentColor: Color) {
    ALL("الكل", "🌐", ColorAmberGlow),
    RELIGIOUS("دينية", "🌙", ColorEmeraldGreen),
    NATIONAL("وطنية", "🇸🇦", ColorIceCyan),
    CUSTOM("مخصصة", "🎯", ColorLavender),
    COMPLETED("منتهية", "⏳", ColorSlateMuted)
}

data class CountdownEvent(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val emoji: String,
    val category: EventCategory,
    val targetYear: Int,
    val targetMonth: Int,
    val targetDay: Int,
    val targetHour: Int = 0,
    val targetMinute: Int = 0,
    val isRecurring: Boolean = true,
    val isCustom: Boolean = false,
    val notes: String = ""
) {
    fun getTargetLocalDateTime(): LocalDateTime {
        return try {
            LocalDateTime.of(targetYear, targetMonth, targetDay, targetHour, targetMinute)
        } catch (e: Exception) {
            LocalDateTime.now().plusDays(1)
        }
    }

    fun getAdjustedTargetForNow(now: LocalDateTime): LocalDateTime {
        var target = getTargetLocalDateTime()
        if (isRecurring && target.isBefore(now)) {
            while (target.isBefore(now)) {
                target = target.plusYears(1)
            }
        }
        return target
    }
}

// Default standard events
private fun getDefaultEvents(): List<CountdownEvent> {
    val currentYear = LocalDate.now().year
    return listOf(
        CountdownEvent(
            id = "ramadan_event",
            title = "شهر رمضان المبارك",
            emoji = "🌙",
            category = EventCategory.RELIGIOUS,
            targetYear = if (LocalDate.now().isAfter(LocalDate.of(currentYear, 3, 1))) currentYear + 1 else currentYear,
            targetMonth = 3,
            targetDay = 1,
            targetHour = 0,
            targetMinute = 0,
            isRecurring = true
        ),
        CountdownEvent(
            id = "eid_fitr_event",
            title = "عيد الفطر السعيد",
            emoji = "✨",
            category = EventCategory.RELIGIOUS,
            targetYear = if (LocalDate.now().isAfter(LocalDate.of(currentYear, 3, 31))) currentYear + 1 else currentYear,
            targetMonth = 3,
            targetDay = 31,
            targetHour = 0,
            targetMinute = 0,
            isRecurring = true
        ),
        CountdownEvent(
            id = "eid_adha_event",
            title = "عيد الأضحى المبارك",
            emoji = "🐑",
            category = EventCategory.RELIGIOUS,
            targetYear = if (LocalDate.now().isAfter(LocalDate.of(currentYear, 6, 7))) currentYear + 1 else currentYear,
            targetMonth = 6,
            targetDay = 7,
            targetHour = 0,
            targetMinute = 0,
            isRecurring = true
        ),
        CountdownEvent(
            id = "saudi_national_day",
            title = "اليوم الوطني السعودي",
            emoji = "🇸🇦",
            category = EventCategory.NATIONAL,
            targetYear = if (LocalDate.now().isAfter(LocalDate.of(currentYear, 9, 23))) currentYear + 1 else currentYear,
            targetMonth = 9,
            targetDay = 23,
            isRecurring = true
        ),
        CountdownEvent(
            id = "new_year_event",
            title = "رأس السنة الميلادية الجديدة",
            emoji = "🎉",
            category = EventCategory.NATIONAL,
            targetYear = currentYear + 1,
            targetMonth = 1,
            targetDay = 1,
            isRecurring = true
        )
    )
}

// ==========================================
// MAIN COMPOSABLE SCREEN
// ==========================================

@Composable
fun CountdownScreen(colors: CustomThemeColors) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current

    // Real-time ticker clock updating every second
    var currentTime by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = LocalDateTime.now()
        }
    }

    // Load custom user events and combine with default
    var customEventsList by remember { mutableStateOf(loadCustomEventsFromPrefs(context)) }
    val allEvents = remember(customEventsList) {
        getDefaultEvents() + customEventsList
    }

    // Category Filter State
    var selectedCategory by rememberSaveable { mutableStateOf(EventCategory.ALL) }
    var notificationsEnabled by rememberSaveable { mutableStateOf(true) }

    // Dialog & Add Modal State
    var showAddEventModal by remember { mutableStateOf(false) }
    var isArchiveExpanded by rememberSaveable { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2500)
            toastMessage = null
        }
    }

    // Filtered Events
    val filteredEvents = remember(allEvents, selectedCategory, currentTime) {
        val filtered = if (selectedCategory == EventCategory.ALL) {
            allEvents
        } else {
            allEvents.filter { it.category == selectedCategory }
        }

        // Sort by closest target date
        filtered.sortedBy { event ->
            val target = event.getAdjustedTargetForNow(currentTime)
            Duration.between(currentTime, target).seconds
        }
    }

    // Closest Event (Hero Spotlight)
    val heroEvent = remember(allEvents, currentTime) {
        allEvents.map { event ->
            val target = event.getAdjustedTargetForNow(currentTime)
            val duration = Duration.between(currentTime, target)
            Pair(event, duration)
        }.filter { it.second.seconds >= 0 }
            .minByOrNull { it.second.seconds }?.first ?: allEvents.firstOrNull()
    }

    // Completed Events List
    val completedEvents = remember(allEvents, currentTime) {
        allEvents.filter { event ->
            !event.isRecurring && event.getTargetLocalDateTime().isBefore(currentTime)
        }
    }

    ToolScreenScaffold(
        colors = colors,
        icon = AppIcons.forCalc(CalcKey.COUNTDOWN),
        title = "المؤقتات والأحداث",
        subtitle = "تابع العد التنازلي الحقيقي للمناسبات القادمة مع إشعارات ذكية وتذكيرات تقويم",
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
            // Background Grid Canvas Drawing
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
                // Toast Feedback
                if (toastMessage != null) {
                    item(key = "toast_msg") {
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

                // SECTION A: Header & Category Selector
                item(key = "header_card") {
                    HeaderCard(
                        notificationsEnabled = notificationsEnabled,
                        onToggleNotifications = {
                            notificationsEnabled = !notificationsEnabled
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            toastMessage = if (notificationsEnabled) "تم تفعيل التنبيهات للأحداث" else "تم إيقاف التنبيهات"
                        }
                    )
                }

                item(key = "category_bar") {
                    CategoryFilterRow(
                        selectedCategory = selectedCategory,
                        onCategorySelected = {
                            selectedCategory = it
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    )
                }

                // SECTION B: Hero Featured Event Card (Spotlight)
                if (heroEvent != null && selectedCategory == EventCategory.ALL) {
                    item(key = "hero_spotlight") {
                        HeroSpotlightCard(
                            event = heroEvent,
                            currentTime = currentTime,
                            onExportCalendar = {
                                val target = heroEvent.getAdjustedTargetForNow(currentTime)
                                exportEventToCalendar(context, heroEvent.title, target, heroEvent.notes)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                toastMessage = "تم فتح التقويم لإضافة الحدث"
                            },
                            onShareText = {
                                val target = heroEvent.getAdjustedTargetForNow(currentTime)
                                val duration = Duration.between(currentTime, target)
                                val days = duration.toDays()
                                val hours = duration.toHours() % 24
                                val mins = duration.toMinutes() % 60
                                val text = "⏳ متبقي على ${heroEvent.emoji} ${heroEvent.title}:\n$days يوم و $hours ساعة و $mins دقيقة.\nالموعد: ${target.toLocalDate()}\nمحسوب بـ حاسبة التواريخ الذكية"
                                clipboardManager.setText(AnnotatedString(text))
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                toastMessage = "تم نسخ بطاقة العد التنازلي للحافظة"
                            }
                        )
                    }
                }

                // SECTION C: All Event Item Cards
                items(filteredEvents, key = { it.id }) { event ->
                    EventCardItem(
                        event = event,
                        currentTime = currentTime,
                        onExportCalendar = {
                            val target = event.getAdjustedTargetForNow(currentTime)
                            exportEventToCalendar(context, event.title, target, event.notes)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            toastMessage = "تم فتح التقويم لإضافة التذكير"
                        },
                        onDeleteCustom = {
                            if (event.isCustom) {
                                customEventsList = customEventsList.filter { it.id != event.id }
                                saveCustomEventsToPrefs(context, customEventsList)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                toastMessage = "تم حذف الحدث المخصص"
                            }
                        }
                    )
                }

                // SECTION D: Completed Events Archive Drawer
                if (completedEvents.isNotEmpty()) {
                    item(key = "archive_drawer") {
                        ExpandableSectionCard(
                            title = "الأحداث المكتملة والسابقة (${completedEvents.size})",
                            icon = Icons.Outlined.History,
                            isExpanded = isArchiveExpanded,
                            onToggle = { isArchiveExpanded = !isArchiveExpanded }
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                completedEvents.forEach { comp ->
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, ColorSlateMuted.copy(alpha = 0.3f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(comp.emoji, fontSize = 18.sp)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(comp.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                    Text("اكتمال: ${comp.targetYear}/${comp.targetMonth}/${comp.targetDay}", color = ColorSlateMuted, fontSize = 11.sp)
                                                }
                                            }
                                            Text("🎉 مكتمل", fontSize = 11.sp, color = ColorEmeraldGreen, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Offline & Precision Footer
                item(key = "footer_info") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = ColorEmeraldGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("عد تنازلي بنظام java.time ثانية بثانية 100% أوفلاين", fontSize = 11.sp, color = ColorSlateMuted)
                    }
                }
            }

            // Floating Action Button to Add New Custom Event
            FloatingActionButton(
                onClick = {
                    showAddEventModal = true
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                containerColor = ColorAmberGlow,
                contentColor = Color.Black,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 90.dp)
                    .shadow(12.dp, CircleShape)
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = "إضافة حدث")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("حدث جديد", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            // Modal Sheet to Add Custom Event
            if (showAddEventModal) {
                AddEventModalDialog(
                    onDismiss = { showAddEventModal = false },
                    onSave = { newEvt ->
                        customEventsList = customEventsList + newEvt
                        saveCustomEventsToPrefs(context, customEventsList)
                        showAddEventModal = false
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        toastMessage = "تمت إضافة الحدث المخصص بنجاح"
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
private fun HeaderCard(
    notificationsEnabled: Boolean,
    onToggleNotifications: () -> Unit
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
            // Animated Stopwatch / Hourglass Graphic
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

                    // Hourglass Top & Bottom Glass
                    val path = Path().apply {
                        moveTo(w * 0.2f, h * 0.15f)
                        lineTo(w * 0.8f, h * 0.15f)
                        lineTo(w * 0.5f, h * 0.5f)
                        lineTo(w * 0.8f, h * 0.85f)
                        lineTo(w * 0.2f, h * 0.85f)
                        lineTo(w * 0.5f, h * 0.5f)
                        close()
                    }
                    drawPath(path, color = ColorGoldBorder, style = Stroke(width = 3f))

                    // Sand inside
                    drawCircle(ColorIceCyan, radius = 4f, center = Offset(w * 0.5f, h * 0.65f))
                    drawCircle(ColorAmberGlow, radius = 3f, center = Offset(w * 0.5f, h * 0.75f))
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "عداد الأحداث الذكي Pro",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "متابعة دقيقة ثانية بثانية مع تذكيرات التقويم",
                    fontSize = 11.sp,
                    color = ColorSlateMuted
                )
            }

            // Notification Switch Toggle Button
            Surface(
                color = if (notificationsEnabled) ColorEmeraldGreen.copy(alpha = 0.2f) else Color(0xFF1E2638),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (notificationsEnabled) ColorEmeraldGreen else Color.White.copy(alpha = 0.15f)),
                modifier = Modifier.clickable { onToggleNotifications() }
            ) {
                Icon(
                    if (notificationsEnabled) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsOff,
                    contentDescription = "التنبيهات",
                    tint = if (notificationsEnabled) ColorEmeraldGreen else ColorSlateMuted,
                    modifier = Modifier.padding(10.dp).size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun CategoryFilterRow(
    selectedCategory: EventCategory,
    onCategorySelected: (EventCategory) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(EventCategory.values(), key = { it.name }) { cat ->
            val isSelected = cat == selectedCategory
            val bg = if (isSelected) cat.accentColor.copy(alpha = 0.25f) else ColorGlassCard
            val borderCol = if (isSelected) cat.accentColor else Color.White.copy(alpha = 0.1f)

            Surface(
                color = bg,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, borderCol),
                modifier = Modifier.clickable { onCategorySelected(cat) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(cat.icon, fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        cat.labelAr,
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
private fun HeroSpotlightCard(
    event: CountdownEvent,
    currentTime: LocalDateTime,
    onExportCalendar: () -> Unit,
    onShareText: () -> Unit
) {
    val target = event.getAdjustedTargetForNow(currentTime)
    val duration = Duration.between(currentTime, target)

    val totalSeconds = duration.seconds.coerceAtLeast(0)
    val days = totalSeconds / (24 * 3600)
    val hours = (totalSeconds % (24 * 3600)) / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    val isCompleted = totalSeconds <= 0

    // Hijri Date conversion
    val hijriStr = remember(target) {
        try {
            val hDate = HijrahChronology.INSTANCE.date(target.toLocalDate())
            "${hDate.get(ChronoField.DAY_OF_MONTH)} ${getHijriMonthNameAr(hDate.get(ChronoField.MONTH_OF_YEAR))} ${hDate.get(ChronoField.YEAR)} هـ"
        } catch (e: Exception) {
            ""
        }
    }

    Surface(
        color = ColorGlassCard,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(ColorAmberGlow, ColorIceCyan))),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // Spotlight Header Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = ColorAmberGlow.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ColorAmberGlow)
                ) {
                    Text(
                        "⭐ أقرب مناسبة قادمة",
                        fontSize = 11.sp,
                        color = ColorAmberGlow,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    color = event.category.accentColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "${event.category.icon} ${event.category.labelAr}",
                        fontSize = 11.sp,
                        color = event.category.accentColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Title & Date Display
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(event.emoji, fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        event.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "الموعد: ${target.year}/${target.monthValue}/${target.dayOfMonth} م | $hijriStr",
                    fontSize = 12.sp,
                    color = ColorIceCyan
                )
            }

            // Massive Live Ticker Displays
            if (isCompleted) {
                // Confetti / Celebration Header
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
                        Text("🎉 🎉 🎉", fontSize = 28.sp)
                        Text("حان الموعد الآن المبارك!", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Text("نتمنى لكم أوقاتاً خيرة وسعيدة", fontSize = 12.sp, color = ColorEmeraldGreen)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TickerUnitBox("أيام", days.toString(), ColorAmberGlow, Modifier.weight(1f))
                    TickerUnitBox("ساعات", String.format(Locale.US, "%02d", hours), ColorIceCyan, Modifier.weight(1f))
                    TickerUnitBox("دقائق", String.format(Locale.US, "%02d", minutes), ColorEmeraldGreen, Modifier.weight(1f))
                    TickerUnitBox("ثواني", String.format(Locale.US, "%02d", seconds), ColorLavender, Modifier.weight(1f))
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onExportCalendar,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, ColorGoldBorder.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = ColorGoldBorder, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إضافة للتقويم", fontSize = 12.sp, color = ColorGoldBorder)
                }

                OutlinedButton(
                    onClick = onShareText,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, ColorIceCyan.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, tint = ColorIceCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("مشاركة الكارت", fontSize = 12.sp, color = ColorIceCyan)
                }
            }
        }
    }
}

@Composable
private fun TickerUnitBox(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Black.copy(alpha = 0.45f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = accentColor
            )
            Text(
                label,
                fontSize = 11.sp,
                color = ColorSlateMuted
            )
        }
    }
}

@Composable
private fun EventCardItem(
    event: CountdownEvent,
    currentTime: LocalDateTime,
    onExportCalendar: () -> Unit,
    onDeleteCustom: () -> Unit
) {
    val target = event.getAdjustedTargetForNow(currentTime)
    val duration = Duration.between(currentTime, target)
    val totalSeconds = duration.seconds.coerceAtLeast(0)

    val days = totalSeconds / (24 * 3600)
    val hours = (totalSeconds % (24 * 3600)) / 3600
    val minutes = (totalSeconds % 3600) / 60

    val isCompleted = totalSeconds <= 0

    Surface(
        color = ColorGlassCard,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, event.category.accentColor.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Left Color Stripe Indicator
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(56.dp)
                    .clip(CircleShape)
                    .background(event.category.accentColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Emoji & Main Title Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(event.emoji, fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        event.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    "الموعد: ${target.year}/${target.monthValue}/${target.dayOfMonth}",
                    fontSize = 11.sp,
                    color = ColorSlateMuted
                )
            }

            // Compact Live Counter
            Column(horizontalAlignment = Alignment.End) {
                if (isCompleted) {
                    Text("🎉 مكتمل", fontSize = 13.sp, color = ColorEmeraldGreen, fontWeight = FontWeight.Bold)
                } else {
                    Text(
                        "$days يوم",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = ColorAmberGlow
                    )
                    Text(
                        "و $hours س : $minutes د",
                        fontSize = 11.sp,
                        color = ColorIceCyan
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onExportCalendar,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = "تقويم", tint = ColorGoldBorder, modifier = Modifier.size(16.dp))
                    }

                    if (event.isCustom) {
                        IconButton(
                            onClick = onDeleteCustom,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Outlined.Delete, contentDescription = "حذف", tint = ColorCrimsonRed, modifier = Modifier.size(16.dp))
                        }
                    }
                }
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
private fun AddEventModalDialog(
    onDismiss: () -> Unit,
    onSave: (CountdownEvent) -> Unit
) {
    var titleInput by remember { mutableStateOf("") }
    var emojiInput by remember { mutableStateOf("🎯") }
    var categoryInput by remember { mutableStateOf(EventCategory.CUSTOM) }

    var targetYearInput by remember { mutableStateOf(LocalDate.now().plusDays(30).year) }
    var targetMonthInput by remember { mutableStateOf(LocalDate.now().plusDays(30).monthValue) }
    var targetDayInput by remember { mutableStateOf(LocalDate.now().plusDays(30).dayOfMonth) }

    var notesInput by remember { mutableStateOf("") }

    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141926),
        title = {
            Text("إضافة حدث مخصص جديد", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Title
                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text("اسم المناسبة / الحدث", color = ColorSlateMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ColorGoldBorder,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                // Emoji Selector Quick Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("الرمز التعبييري:", fontSize = 12.sp, color = ColorSlateMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("🎯", "🎂", "✈️", "🎓", "💍", "⚽", "🚗").forEach { em ->
                            Surface(
                                color = if (emojiInput == em) ColorAmberGlow.copy(alpha = 0.3f) else Color.Transparent,
                                shape = CircleShape,
                                modifier = Modifier.clickable { emojiInput = em }
                            ) {
                                Text(em, fontSize = 20.sp, modifier = Modifier.padding(4.dp))
                            }
                        }
                    }
                }

                // Date Picker Button
                Surface(
                    color = Color.Black.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ColorIceCyan.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            android.app.DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    targetYearInput = y
                                    targetMonthInput = m + 1
                                    targetDayInput = d
                                },
                                targetYearInput,
                                targetMonthInput - 1,
                                targetDayInput
                            ).show()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("تاريخ الحدث: $targetYearInput/$targetMonthInput/$targetDayInput", color = ColorAmberGlow, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Icon(Icons.Filled.EditCalendar, contentDescription = null, tint = ColorGoldBorder)
                    }
                }

                // Notes Input
                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    label = { Text("ملاحظات / أفكار للتذكير", color = ColorSlateMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ColorGoldBorder,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (titleInput.isNotBlank()) {
                        val newEvt = CountdownEvent(
                            title = titleInput,
                            emoji = emojiInput,
                            category = EventCategory.CUSTOM,
                            targetYear = targetYearInput,
                            targetMonth = targetMonthInput,
                            targetDay = targetDayInput,
                            isCustom = true,
                            notes = notesInput
                        )
                        onSave(newEvt)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ColorAmberGlow, contentColor = Color.Black)
            ) {
                Text("حفظ الحدث", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = ColorSlateMuted)
            }
        }
    )
}

// ==========================================
// HELPER UTILITIES & PERSISTENCE
// ==========================================

private fun getHijriMonthNameAr(m: Int): String {
    val hijriMonths = listOf(
        "محرم", "صفر", "ربيع الأول", "ربيع الثاني",
        "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان",
        "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
    )
    return hijriMonths.getOrElse(m - 1) { "" }
}

private fun exportEventToCalendar(context: Context, title: String, target: LocalDateTime, notes: String) {
    try {
        val millis = target.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, title)
            putExtra(CalendarContract.Events.DESCRIPTION, notes)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, millis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, millis + 3600000)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun saveCustomEventsToPrefs(context: Context, events: List<CountdownEvent>) {
    try {
        val prefs = context.getSharedPreferences("countdown_app_prefs", Context.MODE_PRIVATE)
        val array = JSONArray()
        events.forEach { ev ->
            val obj = JSONObject()
            obj.put("id", ev.id)
            obj.put("title", ev.title)
            obj.put("emoji", ev.emoji)
            obj.put("category", ev.category.name)
            obj.put("targetYear", ev.targetYear)
            obj.put("targetMonth", ev.targetMonth)
            obj.put("targetDay", ev.targetDay)
            obj.put("targetHour", ev.targetHour)
            obj.put("targetMinute", ev.targetMinute)
            obj.put("isRecurring", ev.isRecurring)
            obj.put("isCustom", ev.isCustom)
            obj.put("notes", ev.notes)
            array.put(obj)
        }
        prefs.edit().putString("custom_events_json", array.toString()).apply()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun loadCustomEventsFromPrefs(context: Context): List<CountdownEvent> {
    val list = mutableListOf<CountdownEvent>()
    try {
        val prefs = context.getSharedPreferences("countdown_app_prefs", Context.MODE_PRIVATE)
        val str = prefs.getString("custom_events_json", null) ?: return emptyList()
        val array = JSONArray(str)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                CountdownEvent(
                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                    title = obj.optString("title", ""),
                    emoji = obj.optString("emoji", "🎯"),
                    category = try { EventCategory.valueOf(obj.optString("category", "CUSTOM")) } catch (e: Exception) { EventCategory.CUSTOM },
                    targetYear = obj.optInt("targetYear", 2027),
                    targetMonth = obj.optInt("targetMonth", 1),
                    targetDay = obj.optInt("targetDay", 1),
                    targetHour = obj.optInt("targetHour", 0),
                    targetMinute = obj.optInt("targetMinute", 0),
                    isRecurring = obj.optBoolean("isRecurring", true),
                    isCustom = obj.optBoolean("isCustom", true),
                    notes = obj.optString("notes", "")
                )
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}
