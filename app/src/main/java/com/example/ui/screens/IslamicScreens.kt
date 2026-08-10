package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import com.example.util.AppLocationProvider
import com.example.ui.components.LocationStatusCard
import com.example.ui.components.LocationCardState
import com.example.ui.components.ToolScreenScaffold
import com.example.ui.theme.AppIcons
import com.example.model.CalcKey
import com.example.ui.theme.GradientTokens
import com.example.ui.theme.Spacing
import com.example.R

import android.Manifest
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.IslamicData
import com.example.data.CityPrayerInfo
import com.example.data.LivePricesRepository
import com.example.data.SurahInfo
import com.example.ui.theme.*
import com.example.util.AdhanScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.atan2
import android.annotation.SuppressLint
import com.google.android.gms.location.LocationServices
import android.location.Geocoder
import java.util.Locale
import com.google.android.gms.location.Priority

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerTimesScreen(colors: CustomThemeColors, onNavigate: ((CalcKey) -> Unit)? = null) {
    PrayerTimesScreenRedesign(colors = colors, onNavigate = onNavigate)
}

@Composable
fun AdhkarScreen(colors: CustomThemeColors, onBackClick: () -> Unit = {}) {
    AdkarScreenRedesign(colors = colors, onBackClick = onBackClick)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasbihScreen(colors: CustomThemeColors) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val prefs = remember { context.getSharedPreferences("tasbih_prefs", Context.MODE_PRIVATE) }

    // Today's Date Key
    val todayDateStr = remember {
        java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(java.util.Date())
    }
    val savedDate = prefs.getString("today_date", "")
    if (savedDate != todayDateStr) {
        prefs.edit().putString("today_date", todayDateStr).putInt("today_count", 0).apply()
    }

    // State Variables
    var count by remember { mutableStateOf(prefs.getInt("current_count", 0)) }
    var targetCount by remember { mutableStateOf(prefs.getInt("target_count", 33)) }
    var dhikrName by remember { mutableStateOf(prefs.getString("dhikr_name", "سُبْحَانَ اللهِ") ?: "سُبْحَانَ اللهِ") }
    var lifetimeCount by remember { mutableStateOf(prefs.getInt("lifetime_count", 0)) }
    var todayCount by remember { mutableStateOf(prefs.getInt("today_count", 0)) }
    var soundEnabled by remember { mutableStateOf(prefs.getBoolean("sound_enabled", true)) }
    var vibrationEnabled by remember { mutableStateOf(prefs.getBoolean("vibration_enabled", true)) }
    var isScreenLocked by remember { mutableStateOf(false) }

    var showTargetDialog by remember { mutableStateOf(false) }
    var showAddDhikrDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var customDhikrInput by remember { mutableStateOf("") }

    val defaultDhikrs = remember {
        listOf(
            "سُبْحَانَ اللهِ",
            "الْحَمْدُ لِلَّهِ",
            "اللهُ أَكْبَرُ",
            "لَا إِلٰهَ إِلَّا اللهُ",
            "أَسْتَغْفِرُ اللهَ",
            "سُبْحَانَ اللهِ وَبِحَمْدِهِ",
            "اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ",
            "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللهِ"
        )
    }

    val initialCustomDhikrs = remember(prefs) {
        try {
            val arr = org.json.JSONArray(prefs.getString("custom_dhikrs", "[]"))
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) list.add(arr.getString(i))
            list
        } catch (e: Exception) { emptyList<String>() }
    }
    var customDhikrs by remember { mutableStateOf(initialCustomDhikrs) }

    val allDhikrs = remember(customDhikrs) { defaultDhikrs + customDhikrs }

    val bgColor = ObsidianBlack
    val cardColor = CardColor
    val primaryAccent = MintCyan
    val secondaryAccent = RoyalGold
    val dangerColor = DangerRed

    // Save helpers
    fun saveState(newCount: Int, newLifetime: Int, newToday: Int) {
        prefs.edit()
            .putInt("current_count", newCount)
            .putInt("lifetime_count", newLifetime)
            .putInt("today_count", newToday)
            .apply()
    }

    val toneGenerator = remember {
        try { ToneGenerator(AudioManager.STREAM_MUSIC, 60) } catch (e: Exception) { null }
    }

    // Bounce animation on click
    var isPressed by remember { mutableStateOf(false) }
    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "hero_scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "tasbih_infinite")
    val glowAnim by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "glow"
    )

    val completedRounds = if (targetCount > 0) count / targetCount else 0
    val progressFraction = if (targetCount > 0) (count % targetCount).toFloat() / targetCount.toFloat() else 0f

    val onIncrement = {
        if (!isScreenLocked) {
            val nextCount = count + 1
            val nextLifetime = lifetimeCount + 1
            val nextToday = todayCount + 1
            count = nextCount
            lifetimeCount = nextLifetime
            todayCount = nextToday
            saveState(nextCount, nextLifetime, nextToday)

            if (vibrationEnabled) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            if (soundEnabled) {
                try {
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 30)
                } catch (_: Exception) {}
            }
            // Target completion check
            if (nextCount > 0 && nextCount % targetCount == 0) {
                if (vibrationEnabled) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
        ) {
            // Header
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "المسبحة الرقمية",
                        color = secondaryAccent,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "اذْكُرُوا اللَّهَ ذِكْرًا كَثِيرًا",
                        color = TextSecondaryDark,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // Horizontal Scrollable Dhikr Selector
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(allDhikrs) { itemDhikr ->
                        val isSelected = dhikrName == itemDhikr
                        val chipScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.05f else 1.0f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "chip_scale"
                        )

                        Surface(
                            color = if (isSelected) primaryAccent.copy(alpha = 0.22f) else cardColor.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(22.dp),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) secondaryAccent else secondaryAccent.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier
                                .height(42.dp)
                                .graphicsLayer {
                                    scaleX = chipScale
                                    scaleY = chipScale
                                }
                                .clickable {
                                    dhikrName = itemDhikr
                                    prefs.edit().putString("dhikr_name", itemDhikr).apply()
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = itemDhikr,
                                    color = if (isSelected) Color.White else TextSecondaryDark,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Add Custom Dhikr Button
                    item {
                        Surface(
                            color = cardColor.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(22.dp),
                            border = BorderStroke(1.dp, primaryAccent.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .height(42.dp)
                                .clickable { showAddDhikrDialog = true }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "إضافة ذكر", tint = primaryAccent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ذكر جديد", color = primaryAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Hero Counter Section
            item {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(290.dp)
                        .graphicsLayer {
                            scaleX = scaleAnim
                            scaleY = scaleAnim
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            isPressed = true
                            onIncrement()
                        }
                ) {
                    // Outer Ambient Glow Radial Canvas
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 14.dp.toPx()
                        val radius = (size.minDimension / 2) - strokeWidth

                        // Ambient Pulsing Glow
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(primaryAccent.copy(alpha = 0.15f * glowAnim), Color.Transparent),
                                center = center,
                                radius = radius + 30.dp.toPx()
                            ),
                            radius = radius + 30.dp.toPx()
                        )

                        // Background Track Ring
                        drawCircle(
                            color = secondaryAccent.copy(alpha = 0.15f),
                            radius = radius,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        // Animated Progress Arc
                        val sweepAngle = (progressFraction.coerceIn(0f, 1f)) * 360f
                        val arcSize = Size(size.width - 2 * strokeWidth, size.height - 2 * strokeWidth)
                        val arcTopLeft = Offset(strokeWidth, strokeWidth)
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(primaryAccent, secondaryAccent, primaryAccent),
                                center = center
                            ),
                            startAngle = -90f,
                            sweepAngle = if (sweepAngle == 0f && count > 0) 360f else sweepAngle,
                            useCenter = false,
                            topLeft = arcTopLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    // Inner Glass Disc
                    Surface(
                        color = cardColor.copy(alpha = 0.85f),
                        shape = CircleShape,
                        border = BorderStroke(1.5.dp, secondaryAccent.copy(alpha = 0.6f)),
                        shadowElevation = 8.dp,
                        modifier = Modifier.size(210.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "العدد الحالي",
                                color = TextSecondaryDark,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "$count",
                                color = secondaryAccent,
                                fontSize = 52.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center
                            )
                            Surface(
                                color = primaryAccent.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = "الهدف: $targetCount",
                                    color = primaryAccent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = dhikrName,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                LaunchedEffect(isPressed) {
                    if (isPressed) {
                        delay(100)
                        isPressed = false
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Quick Actions Floating Bar
            item {
                Surface(
                    color = cardColor.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, secondaryAccent.copy(alpha = 0.3f)),
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Undo
                        QuickActionButton(
                            icon = Icons.Default.Undo,
                            label = "تراجع",
                            tint = primaryAccent,
                            onClick = {
                                if (count > 0) {
                                    count--
                                    if (lifetimeCount > 0) lifetimeCount--
                                    if (todayCount > 0) todayCount--
                                    saveState(count, lifetimeCount, todayCount)
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        )

                        // Reset
                        QuickActionButton(
                            icon = Icons.Default.Refresh,
                            label = "إعادة ضبط",
                            tint = dangerColor,
                            onClick = { showResetConfirmDialog = true }
                        )

                        // Target
                        QuickActionButton(
                            icon = Icons.Default.Flag,
                            label = "الهدف",
                            tint = secondaryAccent,
                            onClick = { showTargetDialog = true }
                        )

                        // Sound Toggle
                        QuickActionButton(
                            icon = if (soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            label = if (soundEnabled) "صوت" else "كتم",
                            tint = if (soundEnabled) primaryAccent else TextSecondaryDark,
                            onClick = {
                                soundEnabled = !soundEnabled
                                prefs.edit().putBoolean("sound_enabled", soundEnabled).apply()
                            }
                        )

                        // Vibration Toggle
                        QuickActionButton(
                            icon = if (vibrationEnabled) Icons.Default.Vibration else Icons.Default.PhonelinkRing,
                            label = if (vibrationEnabled) "اهتزاز" else "إيقاف",
                            tint = if (vibrationEnabled) primaryAccent else TextSecondaryDark,
                            onClick = {
                                vibrationEnabled = !vibrationEnabled
                                prefs.edit().putBoolean("vibration_enabled", vibrationEnabled).apply()
                            }
                        )

                        // Lock Screen Toggle
                        QuickActionButton(
                            icon = if (isScreenLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            label = if (isScreenLocked) "مقفل" else "قفل",
                            tint = if (isScreenLocked) secondaryAccent else TextSecondaryDark,
                            onClick = {
                                isScreenLocked = !isScreenLocked
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Statistics Card
            item {
                Surface(
                    color = cardColor.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, secondaryAccent.copy(alpha = 0.35f)),
                    shadowElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "إحصائيات الذكر",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(Icons.Default.Analytics, contentDescription = null, tint = secondaryAccent, modifier = Modifier.size(20.dp))
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatMiniItem(title = "اليوم", value = "$todayCount", accentColor = primaryAccent, modifier = Modifier.weight(1f))
                            StatMiniItem(title = "الدورات", value = "$completedRounds", accentColor = secondaryAccent, modifier = Modifier.weight(1f))
                            StatMiniItem(title = "الإجمالي", value = "$lifetimeCount", accentColor = primaryAccent, modifier = Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Progress Bar
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("إنجاز الجولة الحالية", color = TextSecondaryDark, fontSize = 12.sp)
                                Text("${(progressFraction * 100).toInt()}%", color = primaryAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = primaryAccent,
                                trackColor = secondaryAccent.copy(alpha = 0.2f),
                            )
                        }
                    }
                }
            }
        }

        // Touch Lock Screen Overlay
        if (isScreenLocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable(enabled = true, onClick = {}),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = cardColor,
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, secondaryAccent),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = secondaryAccent, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("الشاشة مقفولة", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("تم قفل الشاشة لمنع اللمسات العشوائية أثناء التسبيح", color = TextSecondaryDark, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 8.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { isScreenLocked = false },
                            colors = ButtonDefaults.buttonColors(containerColor = secondaryAccent, contentColor = ObsidianBlack),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("إلغاء القفل", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Target Dialog
    if (showTargetDialog) {
        AlertDialog(
            onDismissRequest = { showTargetDialog = false },
            containerColor = cardColor,
            title = { Text("اختر هدف التسبيح", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(33, 99, 100, 1000).forEach { targetOption ->
                        Surface(
                            color = if (targetCount == targetOption) primaryAccent.copy(alpha = 0.2f) else ObsidianBlack,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (targetCount == targetOption) secondaryAccent else Color.Transparent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    targetCount = targetOption
                                    prefs.edit().putInt("target_count", targetOption).apply()
                                    showTargetDialog = false
                                }
                        ) {
                            Text("$targetOption تسبيحة", color = Color.White, modifier = Modifier.padding(14.dp), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTargetDialog = false }) {
                    Text("إغلاق", color = primaryAccent)
                }
            }
        )
    }

    // Add Custom Dhikr Dialog
    if (showAddDhikrDialog) {
        AlertDialog(
            onDismissRequest = { showAddDhikrDialog = false; customDhikrInput = "" },
            containerColor = cardColor,
            title = { Text("إضافة ذكر جديد", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = customDhikrInput,
                    onValueChange = { customDhikrInput = it },
                    placeholder = { Text("اكتب اسم الذكر هنا...", color = TextSecondaryDark) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = secondaryAccent,
                        unfocusedBorderColor = secondaryAccent.copy(alpha = 0.4f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = customDhikrInput.trim()
                        if (trimmed.isNotBlank() && !allDhikrs.contains(trimmed)) {
                            val updatedList = customDhikrs + trimmed
                            customDhikrs = updatedList
                            prefs.edit().putString("custom_dhikrs", org.json.JSONArray(updatedList).toString()).apply()
                            dhikrName = trimmed
                            prefs.edit().putString("dhikr_name", trimmed).apply()
                        }
                        customDhikrInput = ""
                        showAddDhikrDialog = false
                    }
                ) {
                    Text("إضافة", color = primaryAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDhikrDialog = false; customDhikrInput = "" }) {
                    Text("إلغاء", color = TextSecondaryDark)
                }
            }
        )
    }

    // Reset Confirmation Dialog
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            containerColor = cardColor,
            title = { Text("إعادة ضبط العداد", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت تأكد من إعادة العداد الحالي إلى 0؟ لن يتم مسح المجموع الكلي.", color = TextSecondaryDark) },
            confirmButton = {
                TextButton(
                    onClick = {
                        count = 0
                        saveState(0, lifetimeCount, todayCount)
                        showResetConfirmDialog = false
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                ) {
                    Text("إعادة ضبط", color = dangerColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("إلغاء", color = TextSecondaryDark)
                }
            }
        )
    }
}

@Composable
private fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, color = TextSecondaryDark, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun StatMiniItem(
    title: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(title, color = TextSecondaryDark, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, color = accentColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun RubElHizbOrnament(
    number: Int,
    modifier: Modifier = Modifier,
    goldColor: Color = Color(0xFFD8B56A),
    textColor: Color = Color.White
) {
    Box(
        modifier = modifier.size(46.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val side = size.minDimension * 0.66f
            val halfSide = side / 2f

            // Square 1 (Standard)
            rotate(degrees = 0f, pivot = center) {
                drawRoundRect(
                    color = goldColor.copy(alpha = 0.35f),
                    topLeft = Offset(center.x - halfSide, center.y - halfSide),
                    size = Size(side, side),
                    cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx()),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
            // Square 2 (Rotated 45 degrees)
            rotate(degrees = 45f, pivot = center) {
                drawRoundRect(
                    color = goldColor,
                    topLeft = Offset(center.x - halfSide, center.y - halfSide),
                    size = Size(side, side),
                    cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx()),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            // Central Circle Background & Ring
            drawCircle(
                color = goldColor.copy(alpha = 0.15f),
                radius = side * 0.44f
            )
            drawCircle(
                color = goldColor,
                radius = side * 0.44f,
                style = Stroke(width = 1.dp.toPx())
            )
        }

        Text(
            text = "$number",
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun VerseStarOrnament(
    number: Int,
    modifier: Modifier = Modifier,
    goldColor: Color = Color(0xFFD8B56A)
) {
    Box(
        modifier = modifier.size(36.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val side = size.minDimension * 0.62f
            val halfSide = side / 2f

            rotate(degrees = 0f, pivot = center) {
                drawRoundRect(
                    color = goldColor.copy(alpha = 0.8f),
                    topLeft = Offset(center.x - halfSide, center.y - halfSide),
                    size = Size(side, side),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                    style = Stroke(width = 1.2.dp.toPx())
                )
            }
            rotate(degrees = 45f, pivot = center) {
                drawRoundRect(
                    color = goldColor,
                    topLeft = Offset(center.x - halfSide, center.y - halfSide),
                    size = Size(side, side),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                    style = Stroke(width = 1.2.dp.toPx())
                )
            }
            drawCircle(
                color = goldColor.copy(alpha = 0.15f),
                radius = side * 0.4f
            )
        }
        Text(
            text = "$number",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = goldColor,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ProceduralIslamicBackground(
    modifier: Modifier = Modifier,
    goldColor: Color = Color(0xFFD8B56A)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val spacing = 72.dp.toPx()
        val numX = (w / spacing).toInt() + 2
        val numY = (h / spacing).toInt() + 2

        for (ix in 0..numX) {
            for (iy in 0..numY) {
                val cx = ix * spacing
                val cy = iy * spacing
                val radius = 18.dp.toPx()

                for (angle in 0 until 360 step 45) {
                    val rad = Math.toRadians(angle.toDouble())
                    val x2 = cx + (radius * cos(rad)).toFloat()
                    val y2 = cy + (radius * sin(rad)).toFloat()
                    drawLine(
                        color = goldColor.copy(alpha = 0.025f),
                        start = Offset(cx, cy),
                        end = Offset(x2, y2),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                drawCircle(
                    color = goldColor.copy(alpha = 0.02f),
                    center = Offset(cx, cy),
                    radius = radius * 0.8f,
                    style = Stroke(width = 0.8.dp.toPx())
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranSettingsBottomSheet(
    selectedReciterKey: String,
    onSelectReciter: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF151A22),
        scrimColor = Color.Black.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "إعدادات القارئ والتلاوة",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "اختر القارئ المفضل لديك للتلاوات الصوتية في التطبيق",
                fontSize = 12.sp,
                color = Color(0xFFBFC8D2),
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            val reciters = listOf(
                "ar.alafasy" to "الشيخ مشاري العفاسي",
                "ar.abdulsamad" to "الشيخ عبد الباسط عبد الصمد",
                "ar.ghaamidi" to "الشيخ سعد الغامدي",
                "ar.mahermuaiqly" to "الشيخ ماهر المعيقلي"
            )

            reciters.forEach { (key, label) ->
                val isSelected = selectedReciterKey == key
                Surface(
                    onClick = {
                        onSelectReciter(key)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    color = if (isSelected) Color(0xFFD8B56A).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f),
                    border = BorderStroke(1.dp, if (isSelected) Color(0xFFD8B56A) else Color.White.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                tint = if (isSelected) Color(0xFFD8B56A) else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = label,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFFD8B56A) else Color.White
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFFD8B56A),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

enum class QuranFilterCategory(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    ALL("الكل", Icons.Default.MenuBook),
    FAVORITES("المفضلة ⭐", Icons.Default.Favorite),
    BOOKMARKS("العلامات 🔖", Icons.Default.Bookmark),
    RECENT("آخر قراءة 🕒", Icons.Default.History),
    DAILY_WIRD("الورد اليومي 📿", Icons.Default.AutoAwesome),
    PLAN("خطة القراءة 📅", Icons.Default.CalendarToday)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranScreen(colors: CustomThemeColors) {
    QuranScreenRedesign(colors = colors)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranScreenOld(colors: CustomThemeColors) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val quranPrefs = remember { context.getSharedPreferences("clevcalc_quran_prefs", Context.MODE_PRIVATE) }

    // Persistent State reading
    var lastSurahNum by remember { mutableStateOf(quranPrefs.getInt("last_surah_num", 1)) }
    var lastSurahName by remember { mutableStateOf(quranPrefs.getString("last_surah_name", "الفاتحة") ?: "الفاتحة") }
    var lastAyahNum by remember { mutableStateOf(quranPrefs.getInt("last_ayah_num", 1)) }

    // Independent Daily Wird Preferences
    var wirdSurahNum by remember { mutableStateOf(quranPrefs.getInt("wird_surah_num", 1)) }
    var wirdSurahName by remember { mutableStateOf(quranPrefs.getString("wird_surah_name", "الفاتحة") ?: "الفاتحة") }
    var wirdAyahNum by remember { mutableStateOf(quranPrefs.getInt("wird_ayah_num", 1)) }
    var wirdStreak by remember { mutableStateOf(quranPrefs.getInt("wird_streak", 5)) }
    var wirdDoneToday by remember { mutableStateOf(quranPrefs.getBoolean("wird_done_today", false)) }

    // Favorites toggling list
    val initialFavs = remember(quranPrefs) {
        quranPrefs.getStringSet("favorite_surahs", setOf("1", "18", "36", "67")) ?: setOf("1", "18", "36", "67")
    }
    var favoritesSet by remember { mutableStateOf(initialFavs) }

    // Reciter Preferences
    val initialReciterKey = remember(quranPrefs) {
        quranPrefs.getString("quran_voice_key", "ar.alafasy") ?: "ar.alafasy"
    }
    var selectedReciterKey by remember { mutableStateOf(initialReciterKey) }
    var showSettingsModal by remember { mutableStateOf(false) }

    // Toggle Favorite helper
    val toggleFavorite: (Int) -> Unit = { num ->
        val current = favoritesSet.toMutableSet()
        val numStr = num.toString()
        if (current.contains(numStr)) current.remove(numStr) else current.add(numStr)
        quranPrefs.edit().putStringSet("favorite_surahs", current).apply()
        favoritesSet = current
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf(QuranFilterCategory.ALL) }
    var selectedSurah by remember { mutableStateOf<SurahInfo?>(null) }
    var isDailyWirdMode by remember { mutableStateOf(false) }

    // Colors Palette
    val bgDark = Color(0xFF090B10)
    val cardGlassBg = Color(0xFF151A22).copy(alpha = 0.85f)
    val mintGlow = Color(0xFF63F4DD)
    val royalGold = Color(0xFFD8B56A)
    val purpleAccent = Color(0xFF9D7CFF)
    val secondaryText = Color(0xFFBFC8D2)
    val borderOverlay = Color.White.copy(alpha = 0.08f)

    // Infinite Floating Motion for Hero Card
    val infiniteTransition = rememberInfiniteTransition(label = "HeroFloat")
    val heroOffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "HeroFloatOffset"
    )

    BackHandler(enabled = selectedSurah != null) {
        selectedSurah = null
    }

    if (selectedSurah != null) {
        SurahDetailReader(
            surah = selectedSurah!!,
            colors = colors,
            isDailyWird = isDailyWirdMode,
            onBack = {
                selectedSurah = null
                // Refresh continue reading positions and wird positions
                lastSurahNum = quranPrefs.getInt("last_surah_num", 1)
                lastSurahName = quranPrefs.getString("last_surah_name", "الفاتحة") ?: "الفاتحة"
                lastAyahNum = quranPrefs.getInt("last_ayah_num", 1)
                wirdSurahNum = quranPrefs.getInt("wird_surah_num", 1)
                wirdSurahName = quranPrefs.getString("wird_surah_name", "الفاتحة") ?: "الفاتحة"
                wirdAyahNum = quranPrefs.getInt("wird_ayah_num", 1)
                wirdStreak = quranPrefs.getInt("wird_streak", 5)
                wirdDoneToday = quranPrefs.getBoolean("wird_done_today", false)
            },
            onSelectSurah = { next ->
                selectedSurah = next
                if (isDailyWirdMode) {
                    quranPrefs.edit()
                        .putInt("wird_surah_num", next.number)
                        .putString("wird_surah_name", next.nameAr)
                        .putInt("wird_ayah_num", 1)
                        .apply()
                } else {
                    quranPrefs.edit()
                        .putInt("last_surah_num", next.number)
                        .putString("last_surah_name", next.nameAr)
                        .putInt("last_ayah_num", 1)
                        .apply()
                }
            }
        )
    } else {
        val filteredSurahs = remember(searchQuery, activeFilter, favoritesSet) {
            val baseList = when (activeFilter) {
                QuranFilterCategory.ALL -> IslamicData.surahs
                QuranFilterCategory.FAVORITES -> IslamicData.surahs.filter { favoritesSet.contains(it.number.toString()) }
                QuranFilterCategory.BOOKMARKS -> IslamicData.surahs.filter { it.number == lastSurahNum || favoritesSet.contains(it.number.toString()) }
                QuranFilterCategory.RECENT -> IslamicData.surahs.filter { it.number == lastSurahNum || it.number == wirdSurahNum }
                QuranFilterCategory.DAILY_WIRD -> IslamicData.surahs.filter { it.number == wirdSurahNum }
                QuranFilterCategory.PLAN -> IslamicData.surahs.take(10)
            }

            if (searchQuery.isBlank()) baseList
            else baseList.filter {
                it.nameAr.contains(searchQuery.trim()) ||
                it.nameEn.lowercase().contains(searchQuery.trim().lowercase()) ||
                it.number.toString() == searchQuery.trim()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF090B10), Color(0xFF151A22))
                    )
                )
        ) {
            // Subtle Geometric Pattern Overlay
            Image(
                painter = painterResource(id = R.drawable.ic_islamic_pattern),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.025f),
                contentScale = ContentScale.Inside,
                colorFilter = ColorFilter.tint(royalGold)
            )

            LazyColumn(
                contentPadding = PaddingValues(
                    top = 16.dp,
                    bottom = 120.dp,
                    start = 16.dp,
                    end = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Top App Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "القرآن الكريم",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "تصفح واقرأ آيات الذكر الحكيم برسم المصحف الشريف",
                                fontSize = 12.sp,
                                color = secondaryText,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        IconButton(
                            onClick = { showSettingsModal = true },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(cardGlassBg)
                                .border(1.dp, borderOverlay, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "الإعدادات والقراء",
                                tint = royalGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // HERO CARD: Continue Reading / Daily Wird
                item {
                    val activeSurahObj = IslamicData.surahs.find { it.number == lastSurahNum } ?: IslamicData.surahs.first()
                    val totalVerses = activeSurahObj.totalVerses
                    val progressRatio = (lastAyahNum.toFloat() / maxOf(1, totalVerses)).coerceIn(0.05f, 1.0f)
                    val estMinutes = maxOf(2, (totalVerses - lastAyahNum) / 12)

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { translationY = heroOffsetY }
                            .clip(RoundedCornerShape(28.dp)),
                        color = cardGlassBg,
                        border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(royalGold.copy(alpha = 0.6f), mintGlow.copy(alpha = 0.4f)))),
                        shadowElevation = 12.dp
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().padding(22.dp)) {
                            // Ambient Glow Ornaments
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(royalGold.copy(alpha = 0.08f), Color.Transparent),
                                        center = Offset(size.width, 0f),
                                        radius = size.width * 0.7f
                                    )
                                )
                            }

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = royalGold.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, royalGold.copy(alpha = 0.3f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = royalGold,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "متابعة القراءة 📖",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = royalGold
                                            )
                                        }
                                    }

                                    Surface(
                                        color = if (wirdDoneToday) Color(0xFF50E3A4).copy(alpha = 0.18f) else mintGlow.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, if (wirdDoneToday) Color(0xFF50E3A4) else mintGlow.copy(alpha = 0.3f))
                                    ) {
                                        Text(
                                            text = if (wirdDoneToday) "✅ تم ورد اليوم" else "🔥 سلسلة $wirdStreak أيام",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (wirdDoneToday) Color(0xFF50E3A4) else mintGlow,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "سورة ${activeSurahObj.nameAr}",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )

                                Text(
                                    text = "وصلت للآية رقم $lastAyahNum من أصل $totalVerses آية • Mتبقي حوالي $estMinutes دقائق",
                                    fontSize = 12.sp,
                                    color = secondaryText,
                                    modifier = Modifier.padding(top = 4.dp)
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Progress Bar
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("نسبة الإنجاز", fontSize = 10.sp, color = secondaryText)
                                        Text("${(progressRatio * 100).toInt()}%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = mintGlow)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.1f))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(progressRatio)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.horizontalGradient(listOf(royalGold, mintGlow))
                                                )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                // Large CTA Button
                                Button(
                                    onClick = {
                                        isDailyWirdMode = false
                                        selectedSurah = activeSurahObj
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .clip(RoundedCornerShape(18.dp)),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(royalGold, mintGlow)
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "ابدأ القراءة الآن",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF090B10)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Icon(
                                                imageVector = Icons.Default.ArrowBack,
                                                contentDescription = null,
                                                tint = Color(0xFF090B10),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // PREMIUM SEARCH BAR
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                "ابحث باسم السورة، بالإنجليزية، أو رقمها...",
                                fontSize = 13.sp,
                                color = secondaryText.copy(alpha = 0.7f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = royalGold
                            )
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, null, tint = secondaryText)
                                    }
                                }
                                IconButton(onClick = {
                                    android.widget.Toast.makeText(context, "الفيشة الصوتية جاهزة لاستقبال البحث 🎙️", android.widget.Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(Icons.Default.Mic, null, tint = mintGlow, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = cardGlassBg,
                            unfocusedContainerColor = cardGlassBg,
                            focusedBorderColor = royalGold,
                            unfocusedBorderColor = borderOverlay,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    )
                }

                // QUICK ACTION CHIPS
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(QuranFilterCategory.values()) { category ->
                            val isSelected = activeFilter == category
                            val interactionSource = remember { MutableInteractionSource() }
                            val isPressed by interactionSource.collectIsPressedAsState()
                            val chipScale by animateFloatAsState(
                                targetValue = if (isPressed) 0.94f else 1.0f,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "ChipScale"
                            )

                            Surface(
                                modifier = Modifier
                                    .scale(chipScale)
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = null
                                    ) {
                                        activeFilter = category
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    },
                                color = if (isSelected) royalGold else cardGlassBg,
                                border = BorderStroke(1.dp, if (isSelected) royalGold else borderOverlay),
                                shadowElevation = if (isSelected) 6.dp else 0.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = category.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color(0xFF090B10) else royalGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = category.title,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = if (isSelected) Color(0xFF090B10) else Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // FAVORITE SURAHS CAROUSEL
                if (favoritesSet.isNotEmpty() && activeFilter != QuranFilterCategory.FAVORITES) {
                    item {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "السور المفضلة",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "${favoritesSet.size} سور",
                                    fontSize = 12.sp,
                                    color = royalGold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(favoritesSet.toList()) { favNumStr ->
                                    val favNum = favNumStr.toIntOrNull() ?: 1
                                    val surah = IslamicData.surahs.find { it.number == favNum }
                                    if (surah != null) {
                                        val interactionSource = remember { MutableInteractionSource() }
                                        val isPressed by interactionSource.collectIsPressedAsState()
                                        val favScale by animateFloatAsState(
                                            targetValue = if (isPressed) 0.95f else 1.0f,
                                            label = "FavScale"
                                        )

                                        Surface(
                                            modifier = Modifier
                                                .width(150.dp)
                                                .scale(favScale)
                                                .clip(RoundedCornerShape(22.dp))
                                                .clickable(
                                                    interactionSource = interactionSource,
                                                    indication = null
                                                ) { selectedSurah = surah },
                                            color = cardGlassBg,
                                            border = BorderStroke(1.dp, royalGold.copy(alpha = 0.25f))
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    RubElHizbOrnament(
                                                        number = surah.number,
                                                        goldColor = royalGold,
                                                        textColor = Color.White
                                                    )
                                                    IconButton(
                                                        onClick = { toggleFavorite(surah.number) },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Favorite,
                                                            contentDescription = "إزالة من المفضلة",
                                                            tint = royalGold,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(10.dp))

                                                Text(
                                                    text = "سورة ${surah.nameAr}",
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )

                                                Text(
                                                    text = "${surah.place} • ${surah.totalVerses} آية",
                                                    fontSize = 11.sp,
                                                    color = secondaryText
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // SURAH LIST HEADER
                item {
                    Text(
                        text = "قائمة السور الشريفة",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp)
                    )
                }

                // SURAH LIST ITEMS
                if (filteredSurahs.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp)
                                .clip(RoundedCornerShape(24.dp)),
                            color = cardGlassBg,
                            border = BorderStroke(1.dp, borderOverlay)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SearchOff,
                                    contentDescription = null,
                                    tint = royalGold.copy(alpha = 0.6f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "لم يتم العثور على نتائج للبحث",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "جرّب البحث باسم آخر أو تأكد من إدخال الرقم الصحيح",
                                    fontSize = 12.sp,
                                    color = secondaryText,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        searchQuery = ""
                                        activeFilter = QuranFilterCategory.ALL
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = royalGold)
                                ) {
                                    Text("عرض كل السور", color = Color(0xFF090B10), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    items(filteredSurahs, key = { it.number }) { surah ->
                        val isFav = favoritesSet.contains(surah.number.toString())
                        val isCurrentRead = surah.number == lastSurahNum
                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()
                        val itemScale by animateFloatAsState(
                            targetValue = if (isPressed) 0.97f else 1.0f,
                            label = "SurahItemScale"
                        )

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .scale(itemScale)
                                .clip(RoundedCornerShape(24.dp))
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null
                                ) {
                                    isDailyWirdMode = false
                                    selectedSurah = surah
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                            color = cardGlassBg,
                            border = BorderStroke(
                                if (isCurrentRead) 1.5.dp else 1.dp,
                                if (isCurrentRead) royalGold else borderOverlay
                            ),
                            shadowElevation = if (isCurrentRead) 8.dp else 2.dp
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Rub El Hizb Canvas Ornament Badge
                                        RubElHizbOrnament(
                                            number = surah.number,
                                            goldColor = royalGold,
                                            textColor = Color.White
                                        )

                                        Spacer(modifier = Modifier.width(16.dp))

                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "سورة ${surah.nameAr}",
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )

                                                if (isCurrentRead) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Surface(
                                                        color = royalGold.copy(alpha = 0.2f),
                                                        shape = RoundedCornerShape(6.dp),
                                                        border = BorderStroke(1.dp, royalGold.copy(alpha = 0.4f))
                                                    ) {
                                                        Text(
                                                            text = "مفتوح الآن 📍",
                                                            fontSize = 9.sp,
                                                            color = royalGold,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Text(
                                                text = "${surah.nameEn} • ${surah.place} • ${surah.totalVerses} آية",
                                                fontSize = 12.sp,
                                                color = secondaryText,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { toggleFavorite(surah.number) },
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                contentDescription = "تفضيل السورة",
                                                tint = if (isFav) royalGold else secondaryText,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Icon(
                                            imageVector = Icons.Default.ChevronLeft,
                                            contentDescription = null,
                                            tint = secondaryText.copy(alpha = 0.6f),
                                            modifier = Modifier.size(22.dp)
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

    // Settings & Reciters Modal
    if (showSettingsModal) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsModal = false },
            containerColor = Color(0xFF151A22),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "إعدادات القارئ والصوتيات",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    IconButton(onClick = { showSettingsModal = false }) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "اختر القارئ المفضل للتحميل والاستماع الصوتيات المباشرة:",
                    fontSize = 13.sp,
                    color = secondaryText
                )

                Spacer(modifier = Modifier.height(12.dp))

                val reciters = listOf(
                    "ar.alafasy" to "الشيخ مشاري العفاسي",
                    "ar.abdulsamad" to "الشيخ عبد الباسط عبد الصمد",
                    "ar.ghaamidi" to "الشيخ سعد الغامدي",
                    "ar.mahermuaiqly" to "الشيخ ماهر المعيقلي"
                )

                reciters.forEach { (key, name) ->
                    val isSelected = selectedReciterKey == key
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                selectedReciterKey = key
                                quranPrefs.edit().putString("quran_voice_key", key).apply()
                                showSettingsModal = false
                                android.widget.Toast.makeText(context, "تم تغيير القارئ المفضل إلى $name", android.widget.Toast.LENGTH_SHORT).show()
                            },
                        color = if (isSelected) royalGold.copy(alpha = 0.2f) else Color(0xFF1E2632),
                        border = BorderStroke(1.dp, if (isSelected) royalGold else borderOverlay)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = royalGold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun SurahDetailReader(
    surah: SurahInfo,
    colors: CustomThemeColors,
    isDailyWird: Boolean = false,
    onBack: () -> Unit,
    onSelectSurah: (SurahInfo) -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val quranPrefs = remember { context.getSharedPreferences("clevcalc_quran_prefs", Context.MODE_PRIVATE) }

    var versesList by remember(surah.number) { mutableStateOf(surah.verses) }
    var isLoading by remember(surah.number) { mutableStateOf(surah.verses.isEmpty()) }
    var loadError by remember(surah.number) { mutableStateOf<String?>(null) }

    val adhanPrefs = remember { context.getSharedPreferences("clevcalc_adhan_prefs", Context.MODE_PRIVATE) }
    val initialReciterKey = remember(adhanPrefs) {
        adhanPrefs.getString("quran_voice_key", "ar.alafasy") ?: "ar.alafasy"
    }
    var selectedReciterKey by remember { mutableStateOf(initialReciterKey) }

    var isAudioPlaying by remember { mutableStateOf(false) }
    var isAudioLoading by remember { mutableStateOf(false) }
    var audioPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    val reciterName = when(selectedReciterKey) {
        "ar.alafasy" -> "الشيخ مشاري العفاسي"
        "ar.abdulsamad" -> "الشيخ عبد الباسط عبد الصمد"
        "ar.ghaamidi" -> "الشيخ سعد الغامدي"
        "ar.mahermuaiqly" -> "الشيخ ماهر المعيقلي"
        else -> "الشيخ مشاري العفاسي"
    }

    fun playSurahAudio() {
        if (isAudioPlaying) {
            audioPlayer?.pause()
            isAudioPlaying = false
        } else {
            if (audioPlayer != null) {
                audioPlayer?.start()
                isAudioPlaying = true
            } else {
                isAudioLoading = true
                val audioUrl = "https://cdn.islamic.network/quran/audio-surah/128/$selectedReciterKey/${surah.number}.mp3"
                
                try {
                    val player = MediaPlayer()
                    player.setDataSource(context, Uri.parse(audioUrl))
                    player.setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    player.setOnPreparedListener {
                        isAudioLoading = false
                        isAudioPlaying = true
                        it.start()
                    }
                    player.setOnCompletionListener {
                        isAudioPlaying = false
                    }
                    player.setOnErrorListener { _, _, _ ->
                        isAudioLoading = false
                        isAudioPlaying = false
                        true
                    }
                    player.prepareAsync()
                    audioPlayer = player
                } catch (e: Exception) {
                    isAudioLoading = false
                    isAudioPlaying = false
                }
            }
        }
    }

    DisposableEffect(surah.number, selectedReciterKey) {
        onDispose {
            try {
                audioPlayer?.stop()
                audioPlayer?.release()
            } catch (_: Exception) {}
            audioPlayer = null
            isAudioPlaying = false
        }
    }

    // Update read position details (Daily Wird vs General Last Read)
    LaunchedEffect(surah.number) {
        if (isDailyWird) {
            quranPrefs.edit()
                .putInt("wird_surah_num", surah.number)
                .putString("wird_surah_name", surah.nameAr)
                .putInt("wird_ayah_num", 1)
                .apply()
        } else {
            quranPrefs.edit()
                .putInt("last_surah_num", surah.number)
                .putString("last_surah_name", surah.nameAr)
                .putInt("last_ayah_num", 1)
                .apply()
        }
    }

    LaunchedEffect(surah.number) {
        if (versesList.isNotEmpty()) {
            isLoading = false
            return@LaunchedEffect
        }

        // Check SharedPreferences cache
        val prefs = context.getSharedPreferences("clevcalc_quran_cache", Context.MODE_PRIVATE)
        val cacheKey = "surah_verses_${surah.number}"
        val cachedJson = prefs.getString(cacheKey, null)

        if (!cachedJson.isNullOrBlank()) {
            try {
                val jsonArr = JSONArray(cachedJson)
                val list = mutableListOf<String>()
                for (i in 0 until jsonArr.length()) {
                    list.add(jsonArr.getString(i))
                }
                versesList = list
                isLoading = false
                return@LaunchedEffect
            } catch (_: Exception) {}
        }

        // Fetch online from Al-Quran Cloud API
        isLoading = true
        loadError = null
        withContext(Dispatchers.IO) {
            try {
                val url = URL("https://api.alquran.cloud/v1/surah/${surah.number}")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.requestMethod = "GET"

                if (conn.responseCode == 200) {
                    val stream = conn.inputStream.bufferedReader().use { it.readText() }
                    val jsonObj = JSONObject(stream)
                    val ayahsArr = jsonObj.getJSONObject("data").getJSONArray("ayahs")
                    val fetchedVerses = mutableListOf<String>()
                    val cacheArray = JSONArray()

                    for (i in 0 until ayahsArr.length()) {
                        var text = ayahsArr.getJSONObject(i).getString("text")
                        // Clean leading basmalah if returned inside ayah text for non-Fatiha/Tawbah
                        if (i == 0 && surah.number != 1 && surah.number != 9) {
                            text = stripBismillahIfPresent(text)
                        }
                        fetchedVerses.add(text)
                        cacheArray.put(text)
                    }

                    // Save cache
                    prefs.edit().putString(cacheKey, cacheArray.toString()).apply()

                    withContext(Dispatchers.Main) {
                        versesList = fetchedVerses
                        isLoading = false
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        loadError = "تعذر الاتصال بالشبكة لتحميل السورة."
                        isLoading = false
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadError = "يرجى الاتصال بالإنترنت لتحميل آيات السورة لأول مرة."
                    isLoading = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.appBg)
            .padding(12.dp)
    ) {
        // Top Navigation Bar (Soft Frosted glass header)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = colors.accent)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "سورة ${surah.nameAr}",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.text
                )
                Text("${surah.place} - ${surah.totalVerses} آية", fontSize = 10.sp, color = colors.textMuted)
            }
            Surface(
                color = colors.accent.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.4f)),
                modifier = Modifier.clickable {
                    quranPrefs.edit()
                        .putInt("wird_surah_num", surah.number)
                        .putString("wird_surah_name", surah.nameAr)
                        .putInt("wird_ayah_num", 1)
                        .apply()
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    android.widget.Toast.makeText(context, "📌 تم حفظ سورة ${surah.nameAr} كموقف لوردك اليومي بنجاح!", android.widget.Toast.LENGTH_SHORT).show()
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.BookmarkAdded, contentDescription = null, tint = colors.accent, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("حفظ للورد 📌", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.accent)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bismillah Banner for non-Tawbah and non-Fatiha surahs
        if (surah.number != 9 && surah.number != 1) {
            Surface(
                color = colors.surface.copy(alpha = 0.75f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.accent
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = colors.accent)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("جاري تحميل آيات سورة ${surah.nameAr}...", fontSize = 13.sp, color = colors.textMuted)
                }
            }
        } else if (loadError != null && versesList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(AppIcons.Warning, null, tint = colors.accent, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(loadError!!, fontSize = 14.sp, color = colors.text, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            isLoading = true
                            loadError = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                    ) {
                        Text("إعادة المحاولة")
                    }
                }
            }
        } else {
            // Verses List with custom scroll listener to update last read position
            val listState = rememberLazyListState()

            // Sync current visible item with read position
            LaunchedEffect(listState.firstVisibleItemIndex) {
                if (versesList.isNotEmpty()) {
                    val currentAyah = listState.firstVisibleItemIndex + 1
                    if (isDailyWird) {
                        quranPrefs.edit().putInt("wird_ayah_num", currentAyah).apply()
                    } else {
                        quranPrefs.edit().putInt("last_ayah_num", currentAyah).apply()
                    }
                }
            }

            // إصلاح: "متابعة القراءة"/"وردك اليومي" كان بيفضل يرجّع نفس السورة اللي
            // فتحتها آخر مرة للأبد، حتى لو خلّصتها بالكامل - لأن مفيش أي منطق بيكتشف
            // إن المستخدم وصل لآخر آية ويحرّك المؤشر للسورة اللي بعدها. دلوقتي لما
            // آخر آية في السورة تظهر على الشاشة (زي سورة الفاتحة القصيرة اللي ممكن
            // تظهر كاملة من غير سكرول أصلًا) ويستمر وضوحها لثواني (عشان نتأكد إنه
            // قراها فعلًا مش بس فتح وقفل بسرعة)، بنحفظ رقم السورة التالية كـ"آخر قراءة".
            val isLastAyahVisible = versesList.isNotEmpty() &&
                listState.layoutInfo.visibleItemsInfo.any { it.index == versesList.lastIndex }

            LaunchedEffect(isLastAyahVisible, surah.number) {
                if (isLastAyahVisible) {
                    kotlinx.coroutines.delay(3000) // تأكيد إن المستخدم فعلًا استقر على آخر آية
                    val stillVisible = listState.layoutInfo.visibleItemsInfo.any { it.index == versesList.lastIndex }
                    if (stillVisible) {
                        val nextSurahNumber = surah.number + 1
                        if (nextSurahNumber <= 114) {
                            val nextSurah = IslamicData.surahs.find { it.number == nextSurahNumber }
                            if (nextSurah != null) {
                                if (isDailyWird) {
                                    quranPrefs.edit()
                                        .putInt("wird_surah_num", nextSurahNumber)
                                        .putString("wird_surah_name", nextSurah.nameAr)
                                        .putInt("wird_ayah_num", 1)
                                        .apply()
                                } else {
                                    quranPrefs.edit()
                                        .putInt("last_surah_num", nextSurahNumber)
                                        .putString("last_surah_name", nextSurah.nameAr)
                                        .putInt("last_ayah_num", 1)
                                        .apply()
                                }
                            }
                        } else {
                            // خلّص المصحف كله (سورة الناس) - يرجع الورد يبدأ من الفاتحة
                            // تاني بدل ما يفضل عالق على آخر سورة
                            if (isDailyWird) {
                                quranPrefs.edit()
                                    .putInt("wird_surah_num", 1)
                                    .putString("wird_surah_name", "الفاتحة")
                                    .putInt("wird_ayah_num", 1)
                                    .putBoolean("wird_done_today", true)
                                    .apply()
                            } else {
                                quranPrefs.edit()
                                    .putInt("last_surah_num", 1)
                                    .putString("last_surah_name", "الفاتحة")
                                    .putInt("last_ayah_num", 1)
                                    .apply()
                            }
                        }
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(versesList.size) { idx ->
                    val rawVerseText = versesList[idx]
                    val verseText = if (idx == 0 && surah.number != 1 && surah.number != 9) {
                        stripBismillahIfPresent(rawVerseText)
                    } else {
                        rawVerseText
                    }
                    Surface(
                        color = colors.surface.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = 1.dp,
                        border = BorderStroke(1.dp, colors.border.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            // Traditional Star Badge for Verse Number
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .padding(top = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val center = Offset(size.width / 2, size.height / 2)
                                    val r = size.width / 2
                                    val path = androidx.compose.ui.graphics.Path()
                                    val points = 8
                                    for (i in 0 until points) {
                                        val angleRad = Math.toRadians((i * (360f / points)).toDouble())
                                        val x = (center.x + r * cos(angleRad)).toFloat()
                                        val y = (center.y + r * sin(angleRad)).toFloat()
                                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                        
                                        val innerAngleRad = Math.toRadians((i * (360f / points) + (180f / points)).toDouble())
                                        val ix = (center.x + (r * 0.7f) * cos(innerAngleRad)).toFloat()
                                        val iy = (center.y + (r * 0.7f) * sin(innerAngleRad)).toFloat()
                                        path.lineTo(ix, iy)
                                    }
                                    path.close()
                                    drawPath(
                                        path = path,
                                        brush = Brush.radialGradient(
                                            colors = listOf(Color(0xFFFFF1C5), Color(0xFFDFB659))
                                        )
                                    )
                                    drawCircle(
                                        color = Color(0xFF9E782F),
                                        radius = r * 0.5f,
                                        center = center,
                                        style = Stroke(width = 0.8.dp.toPx())
                                    )
                                }
                                Text(
                                    text = "${idx + 1}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF451A03)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(14.dp))
                            
                            Text(
                                text = verseText,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.text,
                                lineHeight = 34.sp,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Floating Audio Player Dock
            Surface(
                color = colors.surface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.35f)),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            color = colors.accent.copy(alpha = 0.15f),
                            shape = CircleShape,
                            modifier = Modifier.size(44.dp)
                        ) {
                            IconButton(onClick = { playSurahAudio() }) {
                                if (isAudioLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = colors.accent,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (isAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "تشغيل الصوت",
                                        tint = colors.accent
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "تلاوة بصوت $reciterName",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isAudioPlaying) "جاري تشغيل سورة ${surah.nameAr}..." else "انقر لاستماع السورة كاملاً",
                                fontSize = 11.sp,
                                color = colors.textMuted
                            )
                        }
                    }
                }
            }

            val nextSurah = IslamicData.surahs.find { it.number == surah.number + 1 }
            if (nextSurah != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onSelectSurah(nextSurah) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                ) {
                    Text(
                        text = "الانتقال إلى السورة التالية: سورة ${nextSurah.nameAr} ⬅️",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.appBg
                    )
                }
            }
        }
    }
}


@Composable
fun ZakatCalcScreen(colors: CustomThemeColors) {
    ZakatCalcScreenRedesign(colors = colors)
}

fun stripBismillahIfPresent(ayahText: String): String {
    var text = ayahText.trim()
    val knownPrefixes = listOf(
        "بِسۡمِ ٱللَّهِ ٱلرَّحۡمَـٰنِ ٱلرَّحِيمِ",
        "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
        "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
        "بسم الله الرحمن الرحيم",
        "بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ"
    )
    for (prefix in knownPrefixes) {
        if (text.startsWith(prefix)) {
            text = text.substring(prefix.length).trim()
            // Some apis might have weird invisible chars or an extra space, trim again
            return text
        }
    }
    
    // Fallback: If it starts with bismi, remove up to 4 words.
    // We will do a generic diacritic strip to check.
    val diacriticRegex = Regex("[\\p{Mn}\\p{Me}\\u0640]+")
    val cleanText = text.replace(diacriticRegex, "")
        .replace("ٱ", "ا")
        .replace("ی", "ي")
        .replace("ى", "ي")
        
    if (cleanText.startsWith("بسم الله الرحمن الرحيم")) {
        // Find the index of the 4th space in the original string? It's risky.
        // Let's just find the first character of the next word.
        val words = text.split(Regex("\\s+"))
        if (words.size > 4) {
            return words.drop(4).joinToString(" ").trim()
        }
    }
    return text
}
