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
import androidx.compose.ui.text.style.TextOverflow
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
import kotlin.math.atan2
import android.annotation.SuppressLint
import com.google.android.gms.location.LocationServices
import android.location.Geocoder
import java.util.Locale
import kotlinx.coroutines.withContext
import com.google.android.gms.location.Priority

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerTimesScreen(colors: CustomThemeColors) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = context.getSharedPreferences("prayer_prefs", Context.MODE_PRIVATE)

    var locState by remember { mutableStateOf(LocationCardState.IDLE) }
    var locName by remember { mutableStateOf<String?>(null) }
    var lat by remember { mutableStateOf<Double?>(null) }
    var lng by remember { mutableStateOf<Double?>(null) }
    var accuracy by remember { mutableStateOf<Float?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            locState = LocationCardState.LOADING
        } else {
            locState = LocationCardState.PERMISSION_DENIED
        }
    }

    fun fetchLocation() {
        locState = LocationCardState.LOADING
        coroutineScope.launch {
            val result = AppLocationProvider.fetchCurrentLocation(context)
            when (result) {
                is AppLocationProvider.Result.Success -> {
                    lat = result.latitude
                    lng = result.longitude
                    accuracy = result.accuracyMeters
                    locState = LocationCardState.SUCCESS
                    try {
                        val geocoder = Geocoder(context, Locale("ar"))
                        val addresses = withContext(Dispatchers.IO) { geocoder.getFromLocation(result.latitude, result.longitude, 1) }
                        if (!addresses.isNullOrEmpty()) {
                            val address = addresses[0]
                            val parts = listOfNotNull(address.countryName, address.adminArea, address.locality ?: address.subAdminArea)
                            if (parts.isNotEmpty()) locName = parts.joinToString("، ")
                        }
                    } catch (e: Exception) {}
                }
                is AppLocationProvider.Result.PermissionDenied -> locState = LocationCardState.PERMISSION_DENIED
                is AppLocationProvider.Result.LocationDisabled -> locState = LocationCardState.DISABLED
                is AppLocationProvider.Result.Timeout -> { locState = LocationCardState.ERROR; errorMessage = "انتهى وقت الطلب" }
                is AppLocationProvider.Result.Error -> { locState = LocationCardState.ERROR; errorMessage = result.message }
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchLocation()
    }

    val dynamicTimes = remember(lat, lng) {
        if (lat != null && lng != null) {
            IslamicData.calculatePrayerTimes(lat!!, lng!!, 3.0) // fallback offset
        } else {
            IslamicData.getDynamicPrayerTimesForCity(IslamicData.cities.first())
        }
    }

    var showAdhanSettings by remember { mutableStateOf(false) }
    var showPrivacyNotice by remember { mutableStateOf(prefs.getBoolean("show_privacy", true)) }

    ToolScreenScaffold(
        colors = colors,
        icon = AppIcons.forCalc(CalcKey.PRAYER),
        title = CalcKey.PRAYER.title,
        description = "مواقيت الصلاة الدقيقة بناءً على موقعك",
        gradient = GradientTokens.LivePrices,
        inputContent = {
            LocationStatusCard(
                colors = colors,
                state = locState,
                placeName = locName,
                accuracyMeters = accuracy,
                onRequestPermission = {
                    locationPermissionLauncher.launch(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                    )
                },
                onOpenLocationSettings = {
                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                },
                onRetry = { fetchLocation() }
            )

            // Times List
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.Small)
            ) {
                val pList = listOf(
                    "الفجر" to dynamicTimes.fajr,
                    "الشروق" to dynamicTimes.sunrise,
                    "الظهر" to dynamicTimes.dhuhr,
                    "العصر" to dynamicTimes.asr,
                    "المغرب" to dynamicTimes.maghrib,
                    "العشاء" to dynamicTimes.isha
                )
                pList.forEach { (name, time) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.surface2, RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(name, fontWeight = FontWeight.Bold, color = colors.text, fontSize = 16.sp)
                        Text(time, color = colors.accent, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            }
        },
        extraContent = {
            Spacer(modifier = Modifier.height(Spacing.Medium))
            OutlinedButton(
                onClick = { showAdhanSettings = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(AppIcons.Settings, contentDescription = null, tint = colors.accent)
                Spacer(modifier = Modifier.width(8.dp))
                Text("إعدادات الأذان", color = colors.accent)
            }
        }
    )

    if (showAdhanSettings) {
        AlertDialog(
            onDismissRequest = { showAdhanSettings = false },
            confirmButton = {
                Button(
                    onClick = { showAdhanSettings = false },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                ) { Text("حسناً", color = Color.White) }
            },
            title = { Text("🔊 إعدادات الأذان", color = colors.text) },
            text = { Text("قريباً: تخصيص الأذان لكل صلاة", color = colors.textMuted) },
            containerColor = colors.surface,
            titleContentColor = colors.text,
            textContentColor = colors.text
        )
    }

    if (showPrivacyNotice) {
        AlertDialog(
            onDismissRequest = { 
                showPrivacyNotice = false
                prefs.edit().putBoolean("show_privacy", false).apply()
            },
            confirmButton = {
                Button(
                    onClick = { 
                        showPrivacyNotice = false
                        prefs.edit().putBoolean("show_privacy", false).apply()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                ) { Text("موافق", color = Color.White) }
            },
            title = { Text("🛡️ الشفافية والخصوصية", color = colors.text) },
            text = { Text("نحن نستخدم موقعك لحساب مواقيت الصلاة والقبلة بدقة ولا يتم مشاركته مع أي طرف خارجي.", color = colors.textMuted) },
            containerColor = colors.surface,
            titleContentColor = colors.text,
            textContentColor = colors.text
        )
    }
}

@Composable
fun QiblaDirectionScreen(colors: CustomThemeColors) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var locState by remember { mutableStateOf(LocationCardState.IDLE) }
    var locName by remember { mutableStateOf<String?>(null) }
    var lat by remember { mutableStateOf<Double?>(null) }
    var lng by remember { mutableStateOf<Double?>(null) }
    var accuracy by remember { mutableStateOf<Float?>(null) }
    
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            locState = LocationCardState.LOADING
        } else {
            locState = LocationCardState.PERMISSION_DENIED
        }
    }

    fun fetchLocation() {
        locState = LocationCardState.LOADING
        coroutineScope.launch {
            val result = AppLocationProvider.fetchCurrentLocation(context)
            when (result) {
                is AppLocationProvider.Result.Success -> {
                    lat = result.latitude
                    lng = result.longitude
                    accuracy = result.accuracyMeters
                    locState = LocationCardState.SUCCESS
                }
                is AppLocationProvider.Result.PermissionDenied -> locState = LocationCardState.PERMISSION_DENIED
                is AppLocationProvider.Result.LocationDisabled -> locState = LocationCardState.DISABLED
                is AppLocationProvider.Result.Timeout, is AppLocationProvider.Result.Error -> locState = LocationCardState.ERROR
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchLocation()
    }

    // Compass Logic
    var azimuth by remember { mutableStateOf(0f) }
    var qiblaAngle by remember { mutableStateOf(0f) }
    
    LaunchedEffect(lat, lng) {
        if (lat != null && lng != null) {
            val kaabaLat = 21.422487
            val kaabaLng = 39.826206
            val lat1 = Math.toRadians(lat!!)
            val lng1 = Math.toRadians(lng!!)
            val lat2 = Math.toRadians(kaabaLat)
            val lng2 = Math.toRadians(kaabaLng)
            val dLng = lng2 - lng1
            val y = sin(dLng) * cos(lat2)
            val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLng)
            var bearing = Math.toDegrees(atan2(y, x).toDouble()).toFloat()
            bearing = (bearing + 360) % 360
            qiblaAngle = bearing
        }
    }

    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    var isCompassActive by remember { mutableStateOf(true) }

    DisposableEffect(isCompassActive) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null && event.sensor.type == Sensor.TYPE_ORIENTATION) {
                    azimuth = event.values[0]
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (isCompassActive) {
            val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION)
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose { sensorManager.unregisterListener(listener) }
    }

    val rotation = -azimuth + qiblaAngle
    val animatedRotation by animateFloatAsState(targetValue = rotation, animationSpec = tween(500))

    ToolScreenScaffold(
        colors = colors,
        icon = AppIcons.forCalc(CalcKey.QIBLA),
        title = CalcKey.QIBLA.title,
        description = "تحديد اتجاه الكعبة المشرفة",
        gradient = GradientTokens.LivePrices,
        inputContent = {
            LocationStatusCard(
                colors = colors,
                state = locState,
                placeName = null,
                accuracyMeters = accuracy,
                onRequestPermission = {
                    locationPermissionLauncher.launch(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                    )
                },
                onOpenLocationSettings = {
                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                },
                onRetry = { fetchLocation() }
            )

            // Compass View
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentAlignment = Alignment.Center
            ) {
                if (lat != null) {
                    Box(
                        modifier = Modifier
                            .size(250.dp)
                            .clip(CircleShape)
                            .background(colors.surface2)
                            .border(4.dp, colors.accent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "🕋",
                            fontSize = 64.sp,
                            modifier = Modifier
                                .offset(y = (-80).dp)
                                .rotate(animatedRotation)
                        )
                        Icon(AppIcons.Location, contentDescription = null, tint = colors.accent, modifier = Modifier.size(32.dp))
                    }
                } else {
                    Text("يرجى تفعيل الموقع أولاً", color = colors.textMuted)
                }
            }
        },
        primaryActionText = if (isCompassActive) "إيقاف البوصلة" else "تشغيل البوصلة",
        onPrimaryActionClick = { isCompassActive = !isCompassActive }
    )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasbihScreen(colors: CustomThemeColors) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val prefs = context.getSharedPreferences("tasbih_prefs", Context.MODE_PRIVATE)

    var count by remember { mutableStateOf(0) }
    var targetCount by remember { mutableStateOf(33) }
    var dhikrName by remember { mutableStateOf("سُبْحَانَ اللهِ") }
    
    var lifetimeCount by remember { mutableStateOf(prefs.getInt("lifetime_count", 0)) }
    
    var customDhikrs by remember { 
        mutableStateOf(
            try {
                val arr = org.json.JSONArray(prefs.getString("custom_dhikrs", "[]"))
                val list = mutableListOf<String>()
                for (i in 0 until arr.length()) list.add(arr.getString(i))
                list
            } catch (e: Exception) { emptyList() }
        ) 
    }
    
    val defaultDhikrs = listOf("سُبْحَانَ اللهِ", "الْحَمْدُ لِلَّهِ", "اللهُ أَكْبَرُ", "لَا إِلٰهَ إِلَّا اللهُ", "أَسْتَغْفِرُ اللهَ")
    
    var showDhikrSelector by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newDhikrText by remember { mutableStateOf("") }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = tween(durationMillis = 150),
        label = "PressAnimation"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.appBg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top section: Counters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = colors.surface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier.clickable { targetCount = if (targetCount == 33) 99 else if (targetCount == 99) 1000 else 33 }
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("الهدف", color = colors.textMuted, fontSize = 12.sp)
                    Text("$targetCount", color = colors.accent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Surface(
                color = colors.surface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, colors.border)
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("المجموع", color = colors.textMuted, fontSize = 12.sp)
                    Text("$lifetimeCount", color = colors.accentSecondary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Current Dhikr Name & Selector
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "الذِّكْرُ الحَالِي",
                style = MaterialTheme.typography.labelMedium,
                color = colors.textMuted
            )
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                color = colors.surface,
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 8.dp,
                modifier = Modifier.clickable { showDhikrSelector = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dhikrName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.accent,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "تغيير الذكر",
                        tint = colors.textMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Central Bead Clicker - Modern 3D Design
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(300.dp)
                .scale(scale)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    count++
                    lifetimeCount++
                    prefs.edit().putInt("lifetime_count", lifetimeCount).apply()
                    if (count > targetCount) count = 1
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 14.dp.toPx()
                val diameter = size.minDimension - strokeWidth
                val radius = diameter / 2
                
                // Outer beautiful glowing ring
                drawCircle(
                    color = colors.accent.copy(alpha = 0.15f),
                    radius = radius + 8.dp.toPx()
                )

                // Thread
                drawCircle(
                    color = colors.border,
                    radius = radius,
                    style = Stroke(width = 3.dp.toPx())
                )

                // Beads
                val beadCount = if (targetCount > 100) 33 else targetCount
                for (i in 0 until beadCount) {
                    val angleStr = (i.toFloat() / beadCount.toFloat()) * 360f - 90f
                    val angleRad = Math.toRadians(angleStr.toDouble())
                    val cx = center.x + radius * Math.cos(angleRad).toFloat()
                    val cy = center.y + radius * Math.sin(angleRad).toFloat()
                    
                    val isCounted = if (count == 0) false else if (count % beadCount == 0) true else i < (count % beadCount)
                    val beadColor = if (isCounted) colors.accent else colors.surface2
                    val beadRadius = if (isCounted) 12.dp.toPx() else 8.dp.toPx()
                    
                    // Main bead
                    drawCircle(
                        color = beadColor,
                        radius = beadRadius,
                        center = Offset(cx, cy)
                    )
                    
                    // Highlight for 3D effect
                    drawCircle(
                        color = Color.White.copy(alpha = if (isCounted) 0.6f else 0.2f),
                        radius = beadRadius * 0.35f,
                        center = Offset(cx - beadRadius * 0.3f, cy - beadRadius * 0.3f)
                    )
                    
                    // Shadow for 3D effect
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.2f),
                        radius = beadRadius * 0.8f,
                        center = Offset(cx + beadRadius * 0.1f, cy + beadRadius * 0.1f),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }

                // Inner Elegant Center
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(colors.surface.copy(alpha=0.9f), colors.surface.copy(alpha=0.4f), Color.Transparent)
                    ),
                    radius = radius - strokeWidth * 2
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$count",
                    fontSize = 84.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.text
                )
                if (targetCount <= 100) {
                    Text(
                        text = "من $targetCount",
                        fontSize = 16.sp,
                        color = colors.textMuted
                    )
                }
            }
        }

        // Reset Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = colors.surface,
                shadowElevation = 4.dp,
                modifier = Modifier.size(64.dp)
            ) {
                IconButton(
                    onClick = {
                        count = 0
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "إعادة ضبط",
                        tint = colors.accent,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }

    if (showDhikrSelector) {
        AlertDialog(
            onDismissRequest = { showDhikrSelector = false },
            containerColor = colors.appBg,
            title = { Text("اختر الذكر", color = colors.text, fontWeight = FontWeight.Bold, fontSize = 22.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp)) {
                    items(defaultDhikrs) { dhikr ->
                        Surface(
                            color = colors.surface,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable {
                                    dhikrName = dhikr
                                    count = 0
                                    showDhikrSelector = false
                                }
                        ) {
                            Text(
                                text = dhikr,
                                modifier = Modifier.padding(16.dp),
                                color = colors.text,
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    if (customDhikrs.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("أذكاري المخصصة", color = colors.textMuted, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                        }
                        
                        items(customDhikrs) { dhikr ->
                            Surface(
                                color = colors.surface,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        dhikrName = dhikr
                                        count = 0
                                        showDhikrSelector = false
                                    }.padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = dhikr,
                                        color = colors.accent,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Start
                                    )
                                    IconButton(onClick = {
                                        val updated = customDhikrs.filter { it != dhikr }
                                        customDhikrs = updated
                                        val arr = org.json.JSONArray()
                                        for (item in updated) arr.put(item)
                                        prefs.edit().putString("custom_dhikrs", arr.toString()).apply()
                                        if (dhikrName == dhikr) dhikrName = defaultDhikrs[0]
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red.copy(alpha = 0.7f))
                                    }
                                }
                            }
                        }
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showAddDialog = true; showDhikrSelector = false },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("إضافة ذكر مخصص", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDhikrSelector = false }) {
                    Text("إغلاق", color = colors.textMuted)
                }
            }
        )
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = colors.appBg,
            title = { Text("ذكر جديد", color = colors.text, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
            text = {
                OutlinedTextField(
                    value = newDhikrText,
                    onValueChange = { newDhikrText = it },
                    label = { Text("أدخل الذكر هنا") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        focusedLabelColor = colors.accent,
                        unfocusedBorderColor = colors.border,
                        unfocusedLabelColor = colors.textMuted,
                        focusedTextColor = colors.text,
                        unfocusedTextColor = colors.text
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newDhikrText.isNotBlank()) {
                            if (!customDhikrs.contains(newDhikrText)) {
                                val updated = customDhikrs + newDhikrText
                                customDhikrs = updated
                                val arr = org.json.JSONArray()
                                for (item in updated) arr.put(item)
                                prefs.edit().putString("custom_dhikrs", arr.toString()).apply()
                            }
                            dhikrName = newDhikrText
                            count = 0
                            newDhikrText = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("إضافة", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("إلغاء", color = colors.textMuted)
                }
            }
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

        // Bismillah Banner for non-Tawbah and non-Fatiha surahs
        if (surah.number != 9 && surah.number != 1) {
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
    var text = ayahText.trim()
    val knownPrefixes = listOf(
        "بِسۡمِ ٱللَّهِ ٱلرَّحۡمَـٰنِ ٱلرَّحِیمِ",
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
