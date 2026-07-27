package com.example.ui.screens

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.IslamicData
import com.example.data.LivePricesRepository
import com.example.data.SurahInfo
import com.example.ui.theme.CustomThemeColors
import com.example.util.AdhanScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerTimesScreen(colors: CustomThemeColors) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("clevcalc_adhan_prefs", Context.MODE_PRIVATE) }

    var selectedCityIndex by remember { mutableStateOf(0) }
    var cityExpanded by remember { mutableStateOf(false) }
    var showPrivacyNotice by remember { mutableStateOf(false) }
    val city = IslamicData.cities[selectedCityIndex]

    // Calculate prayer times dynamically based on astronomical location and date
    val dynamicTimes = remember(selectedCityIndex) {
        IslamicData.getDynamicPrayerTimesForCity(city)
    }

    // Dynamic Gregorian and Hijri Date Calculation
    val nowCalendar = remember { java.util.Calendar.getInstance() }
    val gregorianDateStr = remember {
        String.format("%02d-%02d-%04d",
            nowCalendar.get(java.util.Calendar.DAY_OF_MONTH),
            nowCalendar.get(java.util.Calendar.MONTH) + 1,
            nowCalendar.get(java.util.Calendar.YEAR)
        )
    }
    val hijriDateStr = remember {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            try {
                val hijrahDate = java.time.chrono.HijrahDate.now()
                val formatter = java.time.format.DateTimeFormatter.ofPattern("EEEE d MMMM yyyy هـ", java.util.Locale("ar"))
                hijrahDate.format(formatter)
            } catch (_: Exception) {
                "اليوم الهجري المبارك"
            }
        } else {
            "اليوم الهجري المبارك"
        }
    }

    // Calculate Next Prayer dynamically
    val nextPrayerDisplay = remember(dynamicTimes) {
        val currentMins = nowCalendar.get(java.util.Calendar.HOUR_OF_DAY) * 60 + nowCalendar.get(java.util.Calendar.MINUTE)
        fun toMins(s: String): Int {
            val parts = s.split(":")
            val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
            return h * 60 + m
        }
        val pList = listOf(
            "الفجر" to toMins(dynamicTimes.fajr),
            "الظهر" to toMins(dynamicTimes.dhuhr),
            "العصر" to toMins(dynamicTimes.asr),
            "المغرب" to toMins(dynamicTimes.maghrib),
            "العشاء" to toMins(dynamicTimes.isha)
        )
        val next = pList.firstOrNull { it.second > currentMins } ?: pList.first()
        val timeStr = when(next.first) {
            "الفجر" -> dynamicTimes.fajr
            "الظهر" -> dynamicTimes.dhuhr
            "العصر" -> dynamicTimes.asr
            "المغرب" -> dynamicTimes.maghrib
            else -> dynamicTimes.isha
        }
        "${next.first} - $timeStr"
    }

    // Notification states for prayers
    var fajrNotification by remember { mutableStateOf(prefs.getBoolean("adhan_fajr", true)) }
    var dhuhrNotification by remember { mutableStateOf(prefs.getBoolean("adhan_dhuhr", true)) }
    var asrNotification by remember { mutableStateOf(prefs.getBoolean("adhan_asr", true)) }
    var maghribNotification by remember { mutableStateOf(prefs.getBoolean("adhan_maghrib", true)) }
    var ishaNotification by remember { mutableStateOf(prefs.getBoolean("adhan_isha", true)) }

    // Permission launcher
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    val prayers = listOf(
        Triple("الفجر", dynamicTimes.fajr, Pair("🌅", fajrNotification)),
        Triple("الشروق", dynamicTimes.sunrise, Pair("☀️", false)),
        Triple("الظهر", dynamicTimes.dhuhr, Pair("🌤️", dhuhrNotification)),
        Triple("العصر", dynamicTimes.asr, Pair("🌥️", asrNotification)),
        Triple("المغرب", dynamicTimes.maghrib, Pair("🌆", maghribNotification)),
        Triple("العشاء", dynamicTimes.isha, Pair("🌙", ishaNotification))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.appBg)
            .padding(14.dp)
    ) {
        // Location Banner Card
        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📍", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(city.nameAr, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.text)
                        Text(city.countryAr, fontSize = 11.sp, color = colors.textMuted)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showPrivacyNotice = true }) {
                        Text("🛡️", fontSize = 18.sp)
                    }

                    Box {
                        Button(
                            onClick = { cityExpanded = true },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("تغيير المدينة ▾", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        DropdownMenu(
                            expanded = cityExpanded,
                            onDismissRequest = { cityExpanded = false },
                            modifier = Modifier.background(colors.surface)
                        ) {
                            IslamicData.cities.forEachIndexed { idx, c ->
                                DropdownMenuItem(
                                    text = { Text("${c.nameAr} - ${c.countryAr}", color = colors.text) },
                                    onClick = {
                                        selectedCityIndex = idx
                                        cityExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Hijri Date Blue Card
        Surface(
            color = Color(0xFF2563EB),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(hijriDateStr, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("التاريخ الهجري الحالي", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                }
                Surface(
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(gregorianDateStr, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Next Prayer Hero Card
        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("الصلاة القادمة", fontSize = 12.sp, color = colors.textMuted)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(nextPrayerDisplay, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.accent)
                }
                Text("🌙", fontSize = 32.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Prayer Times List with Adhan Toggles
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(prayers) { (name, time, extra) ->
                val (icon, isNotifEnabled) = extra
                val isSunrise = name == "الشروق"

                Surface(
                    color = colors.surface,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(icon, fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.text)
                                if (!isSunrise) {
                                    Text(if (isNotifEnabled) "تنبيه الأذان مفعل 🔔" else "التنبيه صامت 🔕", fontSize = 10.sp, color = colors.textMuted)
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(time, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.accent)
                            if (!isSunrise) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Switch(
                                    checked = isNotifEnabled,
                                    onCheckedChange = { checked ->
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                        when (name) {
                                            "الفجر" -> {
                                                fajrNotification = checked
                                                prefs.edit().putBoolean("adhan_fajr", checked).apply()
                                                if (checked) AdhanScheduler.schedulePrayerAlarm(context, "الفجر", dynamicTimes.fajr, 1001)
                                                else AdhanScheduler.cancelPrayerAlarm(context, 1001)
                                            }
                                            "الظهر" -> {
                                                dhuhrNotification = checked
                                                prefs.edit().putBoolean("adhan_dhuhr", checked).apply()
                                                if (checked) AdhanScheduler.schedulePrayerAlarm(context, "الظهر", dynamicTimes.dhuhr, 1002)
                                                else AdhanScheduler.cancelPrayerAlarm(context, 1002)
                                            }
                                            "العصر" -> {
                                                asrNotification = checked
                                                prefs.edit().putBoolean("adhan_asr", checked).apply()
                                                if (checked) AdhanScheduler.schedulePrayerAlarm(context, "العصر", dynamicTimes.asr, 1003)
                                                else AdhanScheduler.cancelPrayerAlarm(context, 1003)
                                            }
                                            "المغرب" -> {
                                                maghribNotification = checked
                                                prefs.edit().putBoolean("adhan_maghrib", checked).apply()
                                                if (checked) AdhanScheduler.schedulePrayerAlarm(context, "المغرب", dynamicTimes.maghrib, 1004)
                                                else AdhanScheduler.cancelPrayerAlarm(context, 1004)
                                            }
                                            "العشاء" -> {
                                                ishaNotification = checked
                                                prefs.edit().putBoolean("adhan_isha", checked).apply()
                                                if (checked) AdhanScheduler.schedulePrayerAlarm(context, "العشاء", dynamicTimes.isha, 1005)
                                                else AdhanScheduler.cancelPrayerAlarm(context, 1005)
                                            }
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = colors.accent,
                                        uncheckedThumbColor = colors.textMuted,
                                        uncheckedTrackColor = colors.surface2
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Transparent Privacy & Permissions Explanation Dialog
    if (showPrivacyNotice) {
        AlertDialog(
            onDismissRequest = { showPrivacyNotice = false },
            confirmButton = {
                Button(
                    onClick = { showPrivacyNotice = false },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                ) {
                    Text("موافق وفهمت ذلك", color = Color.White)
                }
            },
            title = {
                Text("🛡️ الشفافية والخصوصية التامة", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column {
                    Text(
                        "نحن نلتزم بأعلى معايير الخصوصية وحماية بيانات المستخدمين وفقاً لإرشادات Google Play:",
                        fontSize = 12.sp,
                        color = colors.text,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• 📍 **إذن الموقع (GPS)**: يُستخدم فقط لحساب خطوط الطول والعرض وتحديد اتجاه القبلة ومواقيت الصلاة بدقة. لا يتم حفظ موقعك أو مشاركته مع أي طرف خارجي إطلاقاً.", fontSize = 11.sp, color = colors.textMuted, lineHeight = 16.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("• 🔔 **إذن الإشعارات**: يُستخدم فقط لإرسال تنبيهات صوت الأذان في الوقت المحدد لصلاوتك المفضلة.", fontSize = 11.sp, color = colors.textMuted, lineHeight = 16.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("• 🔒 **معالجة محلية**: جميع الحسابات الإسلامية تتم محلياً على جهازك 100% دون الحاجة إلى إرسال أي بيانات شخصية.", fontSize = 11.sp, color = colors.textMuted, lineHeight = 16.sp)
                }
            },
            containerColor = colors.surface,
            titleContentColor = colors.text,
            textContentColor = colors.text
        )
    }
}

@Composable
fun QiblaDirectionScreen(colors: CustomThemeColors) {
    var selectedCityIndex by remember { mutableStateOf(0) }
    val city = IslamicData.cities[selectedCityIndex]
    val qiblaAngle = IslamicData.calculateQiblaAngle(city.lat, city.lng)

    val animatedAngle by animateFloatAsState(
        targetValue = qiblaAngle.toFloat(),
        animationSpec = tween(durationMillis = 800)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.appBg)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Green Banner Header Card (Image 26 & 27)
        Surface(
            color = Color(0xFF059669),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🕋", fontSize = 28.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("اتجاه القبلة", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    Text("نحو الكعبة المشرفة - مكة المكرمة", fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Main White Compass Card
        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Compass Visual Canvas
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .clip(CircleShape)
                        .background(colors.surface2)
                        .border(3.dp, colors.accent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2, size.height / 2)
                        val radius = size.width / 2 - 16.dp.toPx()
                        for (i in 0 until 360 step 30) {
                            val rad = Math.toRadians(i.toDouble())
                            val start = Offset(
                                (center.x + (radius - 12) * sin(rad)).toFloat(),
                                (center.y - (radius - 12) * cos(rad)).toFloat()
                            )
                            val end = Offset(
                                (center.x + radius * sin(rad)).toFloat(),
                                (center.y - radius * cos(rad)).toFloat()
                            )
                            drawLine(Color.Gray.copy(alpha = 0.5f), start, end, strokeWidth = 2.dp.toPx())
                        }
                    }

                    // Rotating Kaaba Indicator
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(animatedAngle),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text("🕋", fontSize = 32.sp)
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(50.dp)
                                    .background(colors.accent)
                            )
                        }
                    }

                    Text("N", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.accent, modifier = Modifier.align(Alignment.TopCenter).padding(top = 6.dp))
                    Text("S", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.textMuted, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp))
                    Text("E", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.textMuted, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 6.dp))
                    Text("W", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.textMuted, modifier = Modifier.align(Alignment.CenterStart).padding(start = 6.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "${String.format("%.0f", qiblaAngle)}° من الشمال الحقيقي (باتجاه الشرق)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.text
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Location Box Card
        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("موقعك (${city.lat}, ${city.lng})", fontSize = 12.sp, color = colors.textMuted)
                Text("المسافة للكعبة: 1,269 كم", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.accent)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Live Compass Button
        Button(
            onClick = { },
            colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🧭 تفعيل البوصلة الحية (على الموبايل)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "💡 لأفضل دقة، وجّه أعلى الجهاز نحو الشمال ثم انظر إلى السهم.",
            fontSize = 11.sp,
            color = colors.textMuted,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AdhkarScreen(colors: CustomThemeColors) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Morning, 1: Evening
    val itemsList = if (selectedTab == 0) IslamicData.morningAdhkar else IslamicData.eveningAdhkar
    val counts = remember { mutableStateMapOf<Int, Int>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.appBg)
            .padding(12.dp)
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = colors.surface,
            contentColor = colors.accent,
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("أذكار الصباح ☀️", fontSize = 13.sp, fontWeight = FontWeight.Bold) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("أذكار المساء 🌙", fontSize = 13.sp, fontWeight = FontWeight.Bold) })
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(itemsList) { dhikr ->
                val currentCount = counts[dhikr.id] ?: 0
                val isFinished = currentCount >= dhikr.countTarget

                Surface(
                    color = if (isFinished) colors.surface2 else colors.surface,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            if (currentCount < dhikr.countTarget) {
                                counts[dhikr.id] = currentCount + 1
                            }
                        }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(dhikr.text, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.text, lineHeight = 22.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (dhikr.rewardText.isNotBlank()) {
                            Text("💡 " + dhikr.rewardText, fontSize = 11.sp, color = colors.textMuted)
                        }
                        if (dhikr.reference.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("📖 المصدر: " + dhikr.reference, fontSize = 10.sp, color = colors.accent, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                if (isFinished) "تم الانتهاء ✓" else "التكرار: $currentCount / ${dhikr.countTarget}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isFinished) Color(0xFF10B981) else colors.accent
                            )

                            Button(
                                onClick = {
                                    if (currentCount < dhikr.countTarget) {
                                        counts[dhikr.id] = currentCount + 1
                                    }
                                },
                                enabled = !isFinished,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isFinished) Color(0xFF10B981) else colors.accent
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(if (isFinished) "مكتمل" else "اضغط للعد", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TasbihScreen(colors: CustomThemeColors) {
    val context = LocalContext.current
    var count by remember { mutableStateOf(0) }
    var targetCount by remember { mutableStateOf(33) }
    var dhikrName by remember { mutableStateOf("سُبْحَانَ اللهِ") }
    var lifetimeCount by remember { mutableStateOf(IslamicData.getLifetimeCount(context)) }

    val defaultDhikrs = remember { listOf("سُبْحَانَ اللهِ", "الْحَمْدُ لِلَّهِ", "اللهُ أَكْبَرُ", "الْعَظِيمُ", "لَا إِلٰهَ إِلَّا اللهُ وَحْدَهُ لَا شَرِيكَ لَهُ", "أَسْتَغْفِرُ اللهَ الْعَظِيمَ") }
    var customDhikrs by remember { mutableStateOf(IslamicData.getCustomDhikrs(context)) }

    var showAddDialog by remember { mutableStateOf(false) }
    var newDhikrText by remember { mutableStateOf("") }

    val allDhikrs = (defaultDhikrs + customDhikrs).distinct()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.appBg)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Horizontal Scrollable Dhikr Selection Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = colors.surface,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .clickable { showAddDialog = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp), tint = colors.textMuted)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إدارة", fontSize = 12.sp, color = colors.text)
                }
            }

            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                items(allDhikrs) { d ->
                    val isSelected = dhikrName == d
                    Surface(
                        color = if (isSelected) colors.accent else colors.surface,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.clickable {
                            dhikrName = d
                            count = 0
                        }
                    ) {
                        Text(
                            text = d,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else colors.text,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }

                item {
                    OutlinedButton(
                        onClick = { showAddDialog = true },
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.accent)
                    ) {
                        Text("+ إضافة ذكر", fontSize = 12.sp, color = colors.accent, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Target goal chips row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("الهدف:", fontSize = 12.sp, color = colors.textMuted, fontWeight = FontWeight.Bold)
            listOf(33, 99, 100, 500, 1000).forEach { target ->
                Surface(
                    color = if (targetCount == target) colors.accent else colors.surface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.clickable { targetCount = target }
                ) {
                    Text(
                        text = "$target",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (targetCount == target) Color.White else colors.text,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Purple Decorative Main Counter Card (Image 2 style)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF582CBA), Color(0xFF381B80))
                    )
                )
                .clickable {
                    count++
                    IslamicData.incrementLifetimeCount(context)
                    lifetimeCount = IslamicData.getLifetimeCount(context)
                }
                .padding(20.dp)
        ) {
            // Star decorations on corners
            Text("✦", fontSize = 18.sp, color = Color.White.copy(alpha = 0.4f), modifier = Modifier.align(Alignment.TopStart))
            Text("✦", fontSize = 18.sp, color = Color.White.copy(alpha = 0.4f), modifier = Modifier.align(Alignment.TopEnd))
            Text("✦", fontSize = 18.sp, color = Color.White.copy(alpha = 0.4f), modifier = Modifier.align(Alignment.BottomStart))
            Text("✦", fontSize = 18.sp, color = Color.White.copy(alpha = 0.4f), modifier = Modifier.align(Alignment.BottomEnd))

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Goal Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "🎯 $targetCount",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Dhikr Title Text
                Text(
                    text = dhikrName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                // Circular Counter Ring
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                        .border(4.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$count",
                            fontSize = 54.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "من $targetCount",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                // Tap anywhere hint
                Text(
                    text = "— اضغط في أي مكان للعد —",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { count = 0 },
                colors = ButtonDefaults.buttonColors(containerColor = colors.surface, contentColor = colors.text),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("إعادة العد 🔄", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = {
                    count = 0
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("تصفير الكل 🗑️", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // Add Dhikr Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("إضافة ذكر جديد", fontWeight = FontWeight.Bold, color = colors.text) },
            text = {
                OutlinedTextField(
                    value = newDhikrText,
                    onValueChange = { newDhikrText = it },
                    label = { Text("اكتب النص الخاص بالذكر") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newDhikrText.isNotBlank()) {
                            IslamicData.addCustomDhikr(context, newDhikrText)
                            customDhikrs = IslamicData.getCustomDhikrs(context)
                            dhikrName = newDhikrText.trim()
                            count = 0
                            newDhikrText = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("إلغاء", color = colors.textMuted)
                }
            },
            containerColor = colors.surface
        )
    }
}

@Composable
fun QuranScreen(colors: CustomThemeColors) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedSurah by remember { mutableStateOf<SurahInfo?>(null) }

    if (selectedSurah != null) {
        // Show Offline Surah Reader Detail View
        SurahDetailReader(
            surah = selectedSurah!!,
            colors = colors,
            onBack = { selectedSurah = null }
        )
    } else {
        // Show Surahs List View
        val filteredSurahs = remember(searchQuery) {
            if (searchQuery.isBlank()) IslamicData.surahs
            else IslamicData.surahs.filter { it.nameAr.contains(searchQuery.trim()) || it.nameEn.lowercase().contains(searchQuery.trim().lowercase()) }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.appBg)
                .padding(12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("ابحث عن سورة...", fontSize = 13.sp, color = colors.textMuted) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = colors.textMuted) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface,
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.border,
                    focusedTextColor = colors.text,
                    unfocusedTextColor = colors.text
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(filteredSurahs) { surah ->
                    Surface(
                        color = colors.surface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { selectedSurah = surah }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(colors.surface2),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("${surah.number}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.accent)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("سورة ${surah.nameAr}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.text)
                                    Text("${surah.place} • ${surah.totalVerses} آية", fontSize = 11.sp, color = colors.textMuted)
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("اقرأ الآن 📖", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.accent)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SurahDetailReader(
    surah: SurahInfo,
    colors: CustomThemeColors,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var versesList by remember(surah.number) { mutableStateOf(surah.verses) }
    var isLoading by remember(surah.number) { mutableStateOf(surah.verses.isEmpty()) }
    var loadError by remember(surah.number) { mutableStateOf<String?>(null) }

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
                            text = text.removePrefix("بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ").trim()
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
        // Top Navigation Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = colors.accent)
            }
            Text(
                "سورة ${surah.nameAr}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colors.text
            )
            Text("${surah.place} - ${surah.totalVerses} آية", fontSize = 11.sp, color = colors.textMuted)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bismillah Banner for non-Tawbah surahs
        if (surah.number != 9) {
            Surface(
                color = colors.surface,
                shape = RoundedCornerShape(14.dp),
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
                    Text("🌐", fontSize = 48.sp)
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
            // Verses List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(versesList.size) { idx ->
                    val verseText = versesList[idx]
                    Surface(
                        color = colors.surface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(colors.accent.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${idx + 1}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.accent)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                verseText,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.text,
                                lineHeight = 32.sp,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun ZakatCalcScreen(colors: CustomThemeColors) {
    var cashText by remember { mutableStateOf("") }
    var goldGramsText by remember { mutableStateOf("") }
    var silverGramsText by remember { mutableStateOf("") }
    var debtsText by remember { mutableStateOf("") }

    val cash = cashText.toDoubleOrNull() ?: 0.0
    val goldGrams = goldGramsText.toDoubleOrNull() ?: 0.0
    val silverGrams = silverGramsText.toDoubleOrNull() ?: 0.0
    val debts = debtsText.toDoubleOrNull() ?: 0.0

    val goldVal = goldGrams * LivePricesRepository.getGoldPricePerGramInUsd(24) * 48.65 // in EGP
    val silverVal = silverGrams * 0.98 * 48.65

    val netWealth = (cash + goldVal + silverVal) - debts
    val nisabUsd = 85 * LivePricesRepository.getGoldPricePerGramInUsd(24) * 48.65 // ~ 85g gold
    val isNisabMet = netWealth >= nisabUsd
    val zakatAmount = if (isNisabMet) netWealth * 0.025 else 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.appBg)
            .padding(12.dp)
    ) {
        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("💎 حاسبة الزكاة الشرعية", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.text)
                Text("نسبة الزكاة 2.5% عند بلوغ النصاب وحولان الحول", fontSize = 11.sp, color = colors.textMuted)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = cashText,
            onValueChange = { cashText = it },
            label = { Text("الأموال النقدية والمدخرات", color = colors.textMuted) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = goldGramsText,
            onValueChange = { goldGramsText = it },
            label = { Text("وزن الذهب المملوك (بالجرام)", color = colors.textMuted) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = debtsText,
            onValueChange = { debtsText = it },
            label = { Text("الديون المستحقة عليك خصمها", color = colors.textMuted) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            color = if (isNisabMet) colors.surface2 else colors.surface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("إجمالي صافي الثروة: ${LivePricesRepository.formatNumber(netWealth)} EGP", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.text)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    if (isNisabMet) "✅ بلغ النصاب! مقدار الزكاة الواجبة:" else "⚠️ لم يبلغ النصاب الشرعي بعد",
                    fontSize = 12.sp,
                    color = if (isNisabMet) Color(0xFF10B981) else Color(0xFFF59E0B)
                )

                if (isNisabMet) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${LivePricesRepository.formatNumber(zakatAmount)} EGP", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = colors.accent)
                }
            }
        }
    }
}
