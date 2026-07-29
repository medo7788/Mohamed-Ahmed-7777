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

    val fusedLocationClient = remember(context) {
        val targetContext = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            context.applicationContext.createAttributionContext("default")
        } else {
            context.applicationContext
        }
        LocationServices.getFusedLocationProviderClient(targetContext)
    }
    
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

    val fusedLocationClient = remember(context) {
        val targetContext = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            context.applicationContext.createAttributionContext("default")
        } else {
            context.applicationContext
        }
        LocationServices.getFusedLocationProviderClient(targetContext)
    }
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
    var count by remember { mutableStateOf(0) }
    val maxCount = 33
    val haptic = LocalHapticFeedback.current

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // أنيميشن الانكماش عند الضغط
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = tween(durationMillis = 100),
        label = "PressAnimation"
    )

    val goldGradients = listOf(
        Color(0xFFF3E5AB),
        Color(0xFFD4AF37),
        Color(0xFFAA771C)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // العنوان العلوي
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "الذِّكْرُ الحَالِي",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD4AF37)
            )
        }

        // المنطقة المركزية: زر التسبيح الرقمي الدائري
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(280.dp)
                .scale(scale)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    count = (count + 1) % (maxCount + 1)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 12.dp.toPx()
                val diameter = size.minDimension - strokeWidth
                val radius = diameter / 2
                val sweepAngle = (count.toFloat() / maxCount.toFloat()) * 360f

                // الحلقة الخلفية
                drawCircle(
                    color = Color(0xFF1E293B),
                    radius = radius,
                    style = Stroke(width = strokeWidth)
                )

                // حلقة التقدم المتممة للتسبيح
                drawArc(
                    brush = Brush.sweepGradient(goldGradients),
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // قرص الزر الداخلي
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                    ),
                    radius = radius - strokeWidth
                )
            }

            // عرض الرقم في المنتصف (بدون أي نصوص توجيهية)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$count",
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD4AF37)
                )
                Text(
                    text = "من $maxCount",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }

        // أزرار التحكم الفرعية
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedIconButton(
                onClick = {
                    count = 0
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "إعادة ضبط",
                    tint = Color(0xFFD4AF37)
                )
            }
        }
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
                    val rawVerseText = versesList[idx]
                    val verseText = if (idx == 0 && surah.number != 1 && surah.number != 9) {
                        stripBismillahIfPresent(rawVerseText)
                    } else {
                        rawVerseText
                    }
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

fun stripBismillahIfPresent(ayahText: String): String {
    val diacriticsRegex = Regex("[\u0610-\u061A\u064B-\u065F\u06D6-\u06ED]")
    val words = ayahText.trim().split(Regex("\\s+"))
    if (words.size < 4) return ayahText

    val expectedBase = listOf("بسم", "الله", "الرحمن", "الرحيم")
    val actualBase = words.take(4).map { diacriticsRegex.replace(it, "") }

    return if (actualBase == expectedBase) {
        words.drop(4).joinToString(" ")
    } else {
        ayahText
    }
}
