package com.example.ui.screens

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Size
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.IslamicData
import com.example.data.CityPrayerInfo
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
import android.annotation.SuppressLint
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerTimesScreen(colors: CustomThemeColors) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val prefs = remember { context.getSharedPreferences("clevcalc_adhan_prefs", Context.MODE_PRIVATE) }

    var selectedCityIndex by remember { 
        mutableStateOf(prefs.getInt("selected_city_index", 0)) 
    }
    var cityExpanded by remember { mutableStateOf(false) }
    var showPrivacyNotice by remember { mutableStateOf(false) }
    var showAdhanSettings by remember { mutableStateOf(false) }
    var selectedAdhanSound by remember { mutableStateOf(prefs.getString("selected_adhan_sound", "makkah") ?: "makkah") }
    val baseCity = IslamicData.cities.getOrElse(selectedCityIndex) { IslamicData.cities[0] }

    var customLat by remember { 
        val savedLatStr = prefs.getString("custom_lat", null)
        mutableStateOf(savedLatStr?.toDoubleOrNull()) 
    }
    var customLng by remember { 
        val savedLngStr = prefs.getString("custom_lng", null)
        mutableStateOf(savedLngStr?.toDoubleOrNull()) 
    }
    var customLocationName by remember { 
        mutableStateOf(prefs.getString("custom_location_name", "") ?: "") 
    }

    val city = if (customLat != null && customLng != null) {
        CityPrayerInfo(customLocationName.ifBlank { "موقعي الحالي" }, customLocationName.ifBlank { "My Location" }, "", customLat!!, customLng!!, "", "", "", "", "", "", "")
    } else {
        baseCity
    }

    fun saveLocationState(lat: Double?, lng: Double?, name: String, cityIndex: Int) {
        prefs.edit().apply {
            putInt("selected_city_index", cityIndex)
            if (lat != null && lng != null) {
                putString("custom_lat", lat.toString())
                putString("custom_lng", lng.toString())
                putString("custom_location_name", name)
            } else {
                remove("custom_lat")
                remove("custom_lng")
                remove("custom_location_name")
            }
            apply()
        }
    }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    
    fun fetchGPSLocation() {
        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        customLat = location.latitude
                        customLng = location.longitude
                        customLocationName = "موقعي الحالي"
                        saveLocationState(location.latitude, location.longitude, "موقعي الحالي", selectedCityIndex)
                    } else {
                        fusedLocationClient.lastLocation
                            .addOnSuccessListener { lastLoc ->
                                if (lastLoc != null) {
                                    customLat = lastLoc.latitude
                                    customLng = lastLoc.longitude
                                    customLocationName = "موقعي الحالي"
                                    saveLocationState(lastLoc.latitude, lastLoc.longitude, "موقعي الحالي", selectedCityIndex)
                                }
                            }
                    }
                }
        } catch (e: SecurityException) { }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            fetchGPSLocation()
        }
    }

    LaunchedEffect(Unit) {
        val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) {
            fetchGPSLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    // Calculate prayer times dynamically based on astronomical location and date
    val dynamicTimes = remember(city) {
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

    fun to12HourFormat(timeStr: String): String {
        try {
            val parts = timeStr.split(":")
            val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val amPm = if (h >= 12) "م" else "ص"
            val h12 = if (h % 12 == 0) 12 else h % 12
            return String.format("%02d:%02d %s", h12, m, amPm)
        } catch (e: Exception) {
            return timeStr
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
        "${next.first} - ${to12HourFormat(timeStr)}"
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
        Triple("الفجر", to12HourFormat(dynamicTimes.fajr), Pair("🌅", fajrNotification)),
        Triple("الشروق", to12HourFormat(dynamicTimes.sunrise), Pair("☀️", false)),
        Triple("الظهر", to12HourFormat(dynamicTimes.dhuhr), Pair("🌤️", dhuhrNotification)),
        Triple("العصر", to12HourFormat(dynamicTimes.asr), Pair("🌥️", asrNotification)),
        Triple("المغرب", to12HourFormat(dynamicTimes.maghrib), Pair("🌆", maghribNotification)),
        Triple("العشاء", to12HourFormat(dynamicTimes.isha), Pair("🌙", ishaNotification))
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
                    IconButton(onClick = { showAdhanSettings = true }) {
                        Text("⚙️", fontSize = 18.sp)
                    }
                    IconButton(onClick = { showPrivacyNotice = true }) {
                        Text("🛡️", fontSize = 18.sp)
                    }

                    Box {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                if (hasFine || hasCoarse) {
                                    fetchGPSLocation()
                                } else {
                                    locationPermissionLauncher.launch(
                                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                                    )
                                }
                            }) {
                                Icon(Icons.Default.LocationOn, contentDescription = "تحديد موقعي (GPS)", tint = colors.accent)
                            }
                            Button(
                                onClick = { cityExpanded = true },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("تغيير المدينة ▾", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
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
                                        customLat = null
                                        customLng = null
                                        customLocationName = ""
                                        saveLocationState(null, null, "", idx)
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

    // Adhan Sound & Notification Settings Dialog
    if (showAdhanSettings) {
        AlertDialog(
            onDismissRequest = { showAdhanSettings = false },
            confirmButton = {
                Button(
                    onClick = {
                        prefs.edit().putString("selected_adhan_sound", selectedAdhanSound).apply()
                        showAdhanSettings = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                ) {
                    Text("حفظ الإعدادات", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdhanSettings = false }) {
                    Text("إلغاء", color = colors.textMuted)
                }
            },
            title = {
                Text("🔊 اختيار صوت ونغمة المؤذن", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.text)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("اختر صوت المؤذن المفضل لتنبيهات أوقات الصلوات في مصر والخارج:", fontSize = 12.sp, color = colors.textMuted)
                    
                    val sounds = listOf(
                        "makkah" to "مؤذن الحرم المكي (صوت شجي)",
                        "madinah" to "مؤذن المسجد النبوي الشريف",
                        "mishary" to "القارئ الشيخ مشاري العفاسي",
                        "abdulbasit" to "الشيخ عبد الباسط عبد الصمد",
                        "default" to "التنبيه القياسي للنظام"
                    )

                    sounds.forEach { (key, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedAdhanSound = key }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label, fontSize = 13.sp, color = colors.text)
                            RadioButton(
                                selected = selectedAdhanSound == key,
                                onClick = { selectedAdhanSound = key }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = {
                            try {
                                val alarmUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                                    ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                                val r = android.media.RingtoneManager.getRingtone(context, alarmUri)
                                r?.play()
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("▶️ اختبار صوت التنبيه الآن", color = colors.accent)
                    }
                }
            },
            containerColor = colors.surface,
            titleContentColor = colors.text,
            textContentColor = colors.text
        )
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
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var selectedCityIndex by remember { mutableStateOf(0) }
    var deviceAzimuth by remember { mutableStateOf(0f) }
    var isCompassActive by remember { mutableStateOf(true) }
    var locationName by remember { mutableStateOf("") }

    var customLat by remember { mutableStateOf<Double?>(null) }
    var customLng by remember { mutableStateOf<Double?>(null) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            try {
                fusedLocationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            customLat = location.latitude
                            customLng = location.longitude
                            locationName = "موقعي الحالي"
                        }
                    }
            } catch (e: SecurityException) { }
        }
    }

    LaunchedEffect(Unit) {
        // Request GPS permission on first load
        locationPermissionLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }

    val city = IslamicData.cities[selectedCityIndex]
    val qiblaAngle = if (customLat != null && customLng != null) {
        IslamicData.calculateQiblaAngle(customLat!!, customLng!!)
    } else {
        IslamicData.calculateQiblaAngle(city.lat, city.lng)
    }
    val currentDisplayLocation = if (locationName.isNotBlank()) locationName else city.nameAr

    // Hardware Compass Sensor Listener
    DisposableEffect(isCompassActive) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ORIENTATION)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null || !isCompassActive) return
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    val rotationMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    val azimuthInDegrees = Math.toDegrees(orientation[0].toDouble()).toFloat()
                    deviceAzimuth = (azimuthInDegrees + 360) % 360
                } else if (event.sensor.type == Sensor.TYPE_ORIENTATION) {
                    deviceAzimuth = (event.values[0] + 360) % 360
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (rotationSensor != null) {
            sensorManager?.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    val totalAngle = (qiblaAngle - deviceAzimuth).toFloat()
    val animatedAngle by animateFloatAsState(
        targetValue = totalAngle,
        animationSpec = tween(durationMillis = 300)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.appBg)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Green Banner Header Card
        Surface(
            color = Color(0xFF059669),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🕋", fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("اتجاه القبلة المباشر", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                        Text("نحو الكعبة المشرفة - مكة المكرمة", fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
                    }
                }

                // GPS and City selector
                var cityMenuOpen by remember { mutableStateOf(false) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        locationPermissionLauncher.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                        )
                    }) {
                        Icon(Icons.Default.LocationOn, contentDescription = "تحديد موقعي (GPS)", tint = Color.White)
                    }
                    Box {
                        IconButton(onClick = { cityMenuOpen = true }) {
                            Text("▾", fontSize = 18.sp, color = Color.White)
                        }
                        DropdownMenu(
                            expanded = cityMenuOpen,
                            onDismissRequest = { cityMenuOpen = false }
                        ) {
                            IslamicData.cities.forEachIndexed { index, c ->
                                DropdownMenuItem(
                                    text = { Text(c.nameAr, fontSize = 13.sp) },
                                    onClick = {
                                        selectedCityIndex = index
                                        locationName = c.nameAr
                                        cityMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Main Compass Surface Card
        val normalizedDiff = ((animatedAngle % 360f) + 360f) % 360f
        val isAligned = normalizedDiff < 5f || normalizedDiff > 355f

        // Vibrate when aligned
        LaunchedEffect(isAligned) {
            if (isAligned) {
                try {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                } catch (_: Throwable) {}
            }
        }

        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Compass Visual Canvas Container
                Box(
                    modifier = Modifier
                        .size(250.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = if (isAligned) {
                                    listOf(Color(0xFF022E21), Color(0xFF01140F))
                                } else {
                                    listOf(colors.surface2, colors.appBg)
                                }
                            )
                        )
                        .border(
                            width = 4.dp,
                            brush = Brush.sweepGradient(
                                colors = if (isAligned) {
                                    listOf(Color(0xFF10B981), Color(0xFF34D399), Color(0xFF10B981))
                                } else {
                                    listOf(colors.accent, colors.accent.copy(alpha = 0.5f), colors.accent)
                                }
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // 1. Rotating Compass Face (Ticks and Letters)
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(-deviceAzimuth)
                    ) {
                        val center = Offset(size.width / 2, size.height / 2)
                        val radiusOuter = size.width / 2 - 14.dp.toPx()
                        
                        // Outer Golden Ring
                        drawCircle(
                            color = Color(0xFFDFB659).copy(alpha = 0.4f),
                            radius = radiusOuter,
                            center = center,
                            style = Stroke(width = 1.5.dp.toPx())
                        )

                        // Draw Ticks (every 5 degrees)
                        for (i in 0 until 360 step 5) {
                            val rad = Math.toRadians(i.toDouble())
                            val isMajor = i % 30 == 0
                            val isCardinal = i % 90 == 0
                            
                            val tickLength = if (isCardinal) 15.dp.toPx() else if (isMajor) 10.dp.toPx() else 6.dp.toPx()
                            val tickWidth = if (isCardinal) 2.5.dp.toPx() else if (isMajor) 1.5.dp.toPx() else 1.dp.toPx()
                            val tickColor = if (isCardinal) Color(0xFFDFB659) else if (isMajor) Color(0xFFDFB659).copy(alpha = 0.8f) else Color.Gray.copy(alpha = 0.5f)

                            val startOffset = Offset(
                                (center.x + (radiusOuter - tickLength) * sin(rad)).toFloat(),
                                (center.y - (radiusOuter - tickLength) * cos(rad)).toFloat()
                            )
                            val endOffset = Offset(
                                (center.x + radiusOuter * sin(rad)).toFloat(),
                                (center.y - radiusOuter * cos(rad)).toFloat()
                            )
                            
                            drawLine(
                                color = tickColor,
                                start = startOffset,
                                end = endOffset,
                                strokeWidth = tickWidth
                            )
                        }
                    }

                    // Static direction overlay (N, S, E, W written elegantly in Arabic)
                    // Let's place cardinal texts relative to rotation so they stay on the rotating face
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(-deviceAzimuth)
                    ) {
                        Text(
                            text = "شمال",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDFB659),
                            modifier = Modifier.align(Alignment.TopCenter).padding(top = 22.dp)
                        )
                        Text(
                            text = "جنوب",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDFB659).copy(alpha = 0.8f),
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 22.dp)
                        )
                        Text(
                            text = "شرق",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDFB659).copy(alpha = 0.8f),
                            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 22.dp)
                        )
                        Text(
                            text = "غرب",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDFB659).copy(alpha = 0.8f),
                            modifier = Modifier.align(Alignment.CenterStart).padding(start = 22.dp)
                        )
                    }

                    // 2. Rotating Qibla Arrow / Needle (Pointing to Kaaba)
                    // We draw a gorgeous gold diamond/needle inside a Box rotating towards `animatedAngle`
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(animatedAngle),
                        contentAlignment = Alignment.Center
                    ) {
                        // Custom Canvas to draw the beautiful golden pointer needle
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val centerOffset = Offset(size.width / 2, size.height / 2)
                            val needleLength = size.width / 2 - 40.dp.toPx()
                            
                            // Golden Path pointing UP
                            val path = androidx.compose.ui.graphics.Path().apply {
                                moveTo(centerOffset.x, centerOffset.y - needleLength) // Tip of arrow
                                lineTo(centerOffset.x - 12.dp.toPx(), centerOffset.y - 30.dp.toPx())
                                lineTo(centerOffset.x - 4.dp.toPx(), centerOffset.y)
                                lineTo(centerOffset.x + 4.dp.toPx(), centerOffset.y)
                                lineTo(centerOffset.x + 12.dp.toPx(), centerOffset.y - 30.dp.toPx())
                                close()
                            }
                            
                            // Draw glowing needle background shadow
                            drawPath(
                                path = path,
                                color = Color(0xFFDFB659).copy(alpha = 0.3f),
                                style = androidx.compose.ui.graphics.drawscope.Fill
                            )
                            
                            // Draw gold-gradient filled needle
                            drawPath(
                                path = path,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFFFF1C5), // Shiny Tip
                                        Color(0xFFDFB659), // Metallic Gold
                                        Color(0xFF9E782F)  // Shadow Gold
                                    )
                                )
                            )
                            
                            // Center pivot pin
                            drawCircle(
                                color = Color(0xFFDFB659),
                                radius = 8.dp.toPx(),
                                center = centerOffset
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 3.dp.toPx(),
                                center = centerOffset
                            )
                        }

                        // Rotating Kaaba Icon at the outer edge of the arrow
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 12.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Surface(
                                color = if (isAligned) Color(0xFF10B981) else Color(0xFF1F2937),
                                shape = CircleShape,
                                border = BorderStroke(1.5.dp, Color(0xFFDFB659)),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text("🕋", fontSize = 18.sp)
                                }
                            }
                        }
                    }

                    // Green Alignment Success Banner Overlay
                    if (isAligned) {
                        Surface(
                            color = Color(0xFF10B981).copy(alpha = 0.9f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(bottom = 60.dp).align(Alignment.Center)
                        ) {
                            Text(
                                text = "القبلة صحيحة تماماً ✓",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "زاوية القبلة: ${String.format("%.1f", qiblaAngle)}° | اتجاه الهاتف: ${String.format("%.1f", deviceAzimuth)}°",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isAligned) Color(0xFF10B981) else colors.text
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Location Info Box
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
                Column {
                    Text("📍 المدينة الحالية: $currentDisplayLocation", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.text)
                    Text("الإحداثيات: ${city.lat}, ${city.lng}", fontSize = 11.sp, color = colors.textMuted)
                }
                Text("المسافة للكعبة: ~1,269 كم", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.accent)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Toggle / Recalibrate Live Compass Button
        Button(
            onClick = { isCompassActive = !isCompassActive },
            colors = ButtonDefaults.buttonColors(containerColor = if (isCompassActive) colors.accent else Color.Gray),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.CompassCalibration, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isCompassActive) "🧭 البوصلة الحية مفعلة (حسّاس الموبايل يعمل)" else "🔄 إعادة تفعيل البوصلة", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "💡 ضع الهاتف بشكل افقي وقم بلف الهاتف للتعرف المباشر على الشمال والقبلة.",
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
    val haptic = LocalHapticFeedback.current

    var count by remember { mutableStateOf(0) }
    var targetCount by remember { mutableStateOf(33) }
    var dhikrName by remember { mutableStateOf("سُبْحَانَ اللهِ") }
    var lifetimeCount by remember { mutableStateOf(IslamicData.getLifetimeCount(context)) }

    val defaultDhikrs = remember { listOf("سُبْحَانَ اللهِ", "الْحَمْدُ لِلَّهِ", "اللهُ أَكْبَرُ", "الْعَظِيمُ", "لَا إِلٰهَ إِلَّا اللهُ وَحْدَهُ لَا شَرِيكَ لَهُ", "أَسْتَغْفِرُ اللهَ الْعَظِيمَ") }
    var customDhikrs by remember { mutableStateOf(IslamicData.getCustomDhikrs(context)) }

    var showAddDialog by remember { mutableStateOf(false) }
    var showManageDialog by remember { mutableStateOf(false) }
    var newDhikrText by remember { mutableStateOf("") }

    val allDhikrs = (defaultDhikrs + customDhikrs).distinct()

    val bgOptions = listOf(
        listOf(Color(0xFF582CBA), Color(0xFF33147F)) to Color(0xFF582CBA),
        listOf(Color(0xFF0F766E), Color(0xFF07403B)) to Color(0xFF0F766E),
        listOf(Color(0xFF1D4ED8), Color(0xFF112D82)) to Color(0xFF1D4ED8)
    )
    var selectedBgIndex by remember { mutableStateOf(0) }
    val currentBg = bgOptions[selectedBgIndex]

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
                    .clickable { showManageDialog = true }
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

    // Decorative Main Counter Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(colors = currentBg.first))
        ) {
            
            Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                // Star decorations on corners
                Text("✦", fontSize = 18.sp, color = Color.White.copy(alpha = 0.4f), modifier = Modifier.align(Alignment.TopStart))
                Text("✦", fontSize = 18.sp, color = Color.White.copy(alpha = 0.4f), modifier = Modifier.align(Alignment.TopEnd))
                Text("✦", fontSize = 18.sp, color = Color.White.copy(alpha = 0.4f), modifier = Modifier.align(Alignment.BottomStart))
                Text("✦", fontSize = 18.sp, color = Color.White.copy(alpha = 0.4f), modifier = Modifier.align(Alignment.BottomEnd))

                // Lifetime count badge
                Surface(
                    color = Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "📿 إجمالي التسبيحات: $lifetimeCount",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Goal Badge and BG selector
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            bgOptions.forEachIndexed { index, option ->
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(option.second)
                                        .border(2.dp, if (selectedBgIndex == index) Color.White else Color.Transparent, CircleShape)
                                        .clickable { selectedBgIndex = index }
                                )
                            }
                        }
                        
                        Surface(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "🎯 المستهدف: $targetCount",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Dhikr Title Text with subtle background glow
                    Surface(
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = dhikrName,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }

                    // Tactile scaling animation state
                    val coroutineScope = rememberCoroutineScope()
                    var isPressed by remember { mutableStateOf(false) }
                    val buttonScale by animateFloatAsState(
                        targetValue = if (isPressed) 0.90f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                        label = "tasbihButtonScale"
                    )

                    // Circular Counter Ring with illuminated beads drawing
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .scale(buttonScale)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null // Custom visual feedback through scale & Canvas
                            ) {
                                isPressed = true
                                coroutineScope.launch {
                                    delay(80)
                                    isPressed = false
                                }
                                count++
                                lifetimeCount++
                                try {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                } catch (_: Throwable) {}
                                try {
                                    IslamicData.incrementLifetimeCount(context)
                                } catch (_: Throwable) {}
                                
                                if (count > 0 && count % targetCount == 0) {
                                    try {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    } catch (_: Throwable) {}
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Canvas to draw the gorgeous circular progress sweep & 33 prayer beads!
                        val progressFraction = if (targetCount > 0) (count % targetCount).toFloat() / targetCount else 0f
                        
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val centerOffset = Offset(size.width / 2, size.height / 2)
                            val radiusOuter = size.width / 2 - 12.dp.toPx()
                            val radiusInner = radiusOuter - 14.dp.toPx()
                            
                            // 1. Draw glowing background orbit track
                            drawCircle(
                                color = Color.White.copy(alpha = 0.12f),
                                radius = radiusOuter,
                                center = centerOffset,
                                style = Stroke(width = 4.dp.toPx())
                            )
                            
                            // 2. Draw progress sweep arc (Golden accent / White)
                            drawArc(
                                color = Color(0xFFFBBF24), // Gold glow
                                startAngle = -90f,
                                sweepAngle = progressFraction * 360f,
                                useCenter = false,
                                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
                                size = Size(radiusOuter * 2, radiusOuter * 2),
                                topLeft = Offset(centerOffset.x - radiusOuter, centerOffset.y - radiusOuter)
                            )
                            
                            // 3. Draw 33 physical prayer beads around the ring
                            val totalBeads = 33
                            val completedBeadsCount = (progressFraction * totalBeads).toInt()
                            
                            for (i in 0 until totalBeads) {
                                val angleDegrees = (i * (360f / totalBeads)) - 90f
                                val angleRad = Math.toRadians(angleDegrees.toDouble())
                                val beadCenter = Offset(
                                    (centerOffset.x + radiusOuter * cos(angleRad)).toFloat(),
                                    (centerOffset.y + radiusOuter * sin(angleRad)).toFloat()
                                )
                                
                                val isBeadCompleted = i < completedBeadsCount
                                val beadColor = if (isBeadCompleted) {
                                    Color(0xFFFBBF24) // Gold filled
                                } else {
                                    Color.White.copy(alpha = 0.4f) // Translucent white
                                }
                                val beadRadius = if (isBeadCompleted) 5.dp.toPx() else 3.5.dp.toPx()
                                
                                // Draw bead outer glow if completed
                                if (isBeadCompleted) {
                                    drawCircle(
                                        color = Color(0xFFFBBF24).copy(alpha = 0.4f),
                                        radius = beadRadius + 3.dp.toPx(),
                                        center = beadCenter
                                    )
                                }
                                
                                drawCircle(
                                    color = beadColor,
                                    radius = beadRadius,
                                    center = beadCenter
                                )
                            }
                        }

                        // Inner physical mother-of-pearl central touch button
                        Box(
                            modifier = Modifier
                                .size(136.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.35f),
                                            Color.White.copy(alpha = 0.15f)
                                        )
                                    )
                                )
                                .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$count",
                                    fontSize = 52.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "من $targetCount",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    // Tap hint with pulsing Arabic text
                    Text(
                        text = "— انقر في أي مكان داخل الدائرة للتسبيح —",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
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
        var newDhikrTargetText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("إضافة ذكر جديد", fontWeight = FontWeight.Bold, color = colors.text) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newDhikrText,
                        onValueChange = { newDhikrText = it },
                        label = { Text("اكتب النص الخاص بالذكر") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newDhikrTargetText,
                        onValueChange = { newDhikrTargetText = it },
                        label = { Text("العدد المطلوب (اختياري)") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newDhikrText.isNotBlank()) {
                            IslamicData.addCustomDhikr(context, newDhikrText)
                            customDhikrs = IslamicData.getCustomDhikrs(context)
                            dhikrName = newDhikrText.trim()
                            
                            val engTargetText = newDhikrTargetText
                                .replace("٠", "0").replace("١", "1").replace("٢", "2")
                                .replace("٣", "3").replace("٤", "4").replace("٥", "5")
                                .replace("٦", "6").replace("٧", "7").replace("٨", "8")
                                .replace("٩", "9")
                                
                            val target = engTargetText.toIntOrNull()
                            if (target != null && target > 0) {
                                targetCount = target
                            }
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

    if (showManageDialog) {
        AlertDialog(
            onDismissRequest = { showManageDialog = false },
            title = { Text("إدارة الأذكار", fontWeight = FontWeight.Bold, color = colors.text) },
            text = {
                LazyColumn {
                    items(customDhikrs) { dhikrItem ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(dhikrItem, modifier = Modifier.weight(1f), color = colors.text)
                            IconButton(onClick = {
                                IslamicData.deleteCustomDhikr(context, dhikrItem)
                                customDhikrs = IslamicData.getCustomDhikrs(context)
                                if (dhikrName == dhikrItem) {
                                    dhikrName = defaultDhikrs.first()
                                    count = 0
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red)
                            }
                        }
                    }
                    if (customDhikrs.isEmpty()) {
                        item {
                            Text("لا توجد أذكار مخصصة حالياً.", color = colors.textMuted, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showManageDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                ) {
                    Text("تم")
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
                    val isMeccan = surah.place.contains("مك")
                    Surface(
                        color = colors.surface,
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = 2.dp,
                        border = BorderStroke(1.dp, colors.border.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .clickable { selectedSurah = surah }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Islamic Star Badge for Surah Number
                                Box(
                                    modifier = Modifier.size(42.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val center = Offset(size.width / 2, size.height / 2)
                                        val r = size.width / 2 - 2.dp.toPx()
                                        val path = androidx.compose.ui.graphics.Path()
                                        val points = 8
                                        for (i in 0 until points) {
                                            val angleRad = Math.toRadians((i * (360f / points)).toDouble())
                                            val x = (center.x + r * cos(angleRad)).toFloat()
                                            val y = (center.y + r * sin(angleRad)).toFloat()
                                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                            
                                            val innerAngleRad = Math.toRadians((i * (360f / points) + (180f / points)).toDouble())
                                            val ix = (center.x + (r * 0.72f) * cos(innerAngleRad)).toFloat()
                                            val iy = (center.y + (r * 0.72f) * sin(innerAngleRad)).toFloat()
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
                                            radius = r * 0.52f,
                                            center = center,
                                            style = Stroke(width = 1.dp.toPx())
                                        )
                                    }
                                    Text(
                                        text = "${surah.number}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF451A03)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(14.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "سورة ${surah.nameAr}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = colors.text
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // Styled Meccan/Medinan chip
                                        Surface(
                                            color = if (isMeccan) Color(0xFFFEF3C7) else Color(0xFFD1FAE5),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = if (isMeccan) "مكية" else "مدنية",
                                                color = if (isMeccan) Color(0xFFB45309) else Color(0xFF047857),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = "${surah.totalVerses} آية",
                                            fontSize = 11.sp,
                                            color = colors.textMuted
                                        )
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text(
                                    text = "اقرأ 📖",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.accent
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
fun SurahDetailReader(
    surah: SurahInfo,
    colors: CustomThemeColors,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

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
