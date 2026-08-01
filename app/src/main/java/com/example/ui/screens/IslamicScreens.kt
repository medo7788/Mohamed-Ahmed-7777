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
    
    fun fetchLocation(silent: Boolean = false) {
        if (!silent) locState = LocationCardState.LOADING
        coroutineScope.launch {
            val result = AppLocationProvider.fetchCurrentLocation(context)
            when (result) {
                is AppLocationProvider.Result.Success -> {
                    lat = result.latitude
                    lng = result.longitude
                    accuracy = result.accuracyMeters
                    locState = LocationCardState.SUCCESS
                    
                    // Geocoding to get place name
                    try {
                        val geocoder = Geocoder(context, Locale("ar"))
                        val addresses = withContext(Dispatchers.IO) { geocoder.getFromLocation(result.latitude, result.longitude, 1) }
                        if (!addresses.isNullOrEmpty()) {
                            val address = addresses[0]
                            val parts = listOfNotNull(address.countryName, address.adminArea, address.locality ?: address.subAdminArea)
                            locName = parts.joinToString("، ")
                            // Save to cache
                            AppLocationProvider.saveLocationToCache(context, result.latitude, result.longitude, locName)
                        }
                    } catch (e: Exception) {
                        AppLocationProvider.saveLocationToCache(context, result.latitude, result.longitude, locName)
                    }
                }
                is AppLocationProvider.Result.PermissionDenied -> if (!silent) locState = LocationCardState.PERMISSION_DENIED
                is AppLocationProvider.Result.LocationDisabled -> if (!silent) locState = LocationCardState.DISABLED
                is AppLocationProvider.Result.Timeout, is AppLocationProvider.Result.Error -> {
                    if (!silent) locState = LocationCardState.ERROR
                }
            }
        }
    }

    // --- 1. Caching & Fallback Logic ---
    LaunchedEffect(Unit) {
        // Load from cache immediately
        val cached = AppLocationProvider.getCachedLocation(context)
        if (cached != null) {
            lat = cached.lat
            lng = cached.lng
            locName = cached.placeName
            locState = LocationCardState.SUCCESS
        }
        // Start silent refresh
        fetchLocation(silent = cached != null)
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            fetchLocation()
        } else {
            locState = LocationCardState.PERMISSION_DENIED
        }
    }

    val dynamicTimes = remember(lat, lng) {
        if (lat != null && lng != null) {
            IslamicData.calculatePrayerTimes(lat!!, lng!!, 3.0)
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
        subtitle = "مواقيت الصلاة الدقيقة بناءً على موقعك الحالي",
        onPrimaryActionClick = { showAdhanSettings = true },
        primaryActionText = "إعدادات الأذان والتنبيهات"
    ) {
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

        Spacer(modifier = Modifier.height(Spacing.Medium))

        // Times List
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(Spacing.Medium),
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
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.surface2.copy(alpha = 0.5f))
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(name, fontWeight = FontWeight.Bold, color = colors.text, fontSize = 16.sp)
                        Text(time, color = colors.accent, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    }
                }
            }
        }
    }

    if (showAdhanSettings) {
        AlertDialog(
            onDismissRequest = { showAdhanSettings = false },
            confirmButton = {
                Button(
                    onClick = { showAdhanSettings = false },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                ) { Text("حسناً", color = Color.White) }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(AppIcons.Notifications, null, tint = colors.accent, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("إعدادات الأذان", color = colors.text)
                }
            },
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
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(AppIcons.Info, null, tint = colors.accent, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("الشفافية والخصوصية", color = colors.text)
                }
            },
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

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            fetchLocation()
        } else {
            locState = LocationCardState.PERMISSION_DENIED
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
    var sensorAccuracy by remember { mutableStateOf(SensorManager.SENSOR_STATUS_ACCURACY_HIGH) }

    // إصلاح: Sensor.TYPE_ORIENTATION حساس مهجور (deprecated) وغير دقيق/غير مدعوم
    // بشكل موثوق على الأجهزة الحديثة. الطريقة الصحيحة الحالية هي قراءة
    // TYPE_ROTATION_VECTOR وتحويله لمصفوفة دوران ثم لزوايا اتجاه (azimuth) عن طريق
    // SensorManager.getRotationMatrix() + getOrientation().
    DisposableEffect(isCompassActive) {
        val listener = object : SensorEventListener {
            private val rotationMatrix = FloatArray(9)
            private val orientationAngles = FloatArray(3)

            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null && event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)
                    // azimuth راديان → درجات، وتطبيع للمدى 0..360
                    val azimuthDegrees = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                    azimuth = (azimuthDegrees + 360) % 360
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                sensorAccuracy = accuracy
            }
        }
        if (isCompassActive) {
            val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose { sensorManager.unregisterListener(listener) }
    }

    val rotation = -azimuth + qiblaAngle
    // حركة أنعم بدل tween الخطي القديم - قريبة من إحساس إبرة بوصلة حقيقية
    val animatedRotation by animateFloatAsState(
        targetValue = rotation,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    // المحاذاة الدقيقة مع اتجاه القبلة (±2 درجة)، مع حساب الفرق الدائري الصحيح
    // (عشان مثلاً 359° و1° يبقوا فرق 2 درجة مش 358)
    val angleDiff = remember(azimuth, qiblaAngle) {
        val diff = kotlin.math.abs(azimuth - qiblaAngle) % 360f
        if (diff > 180f) 360f - diff else diff
    }
    val isAligned = angleDiff <= 2f
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    LaunchedEffect(isAligned) {
        if (isAligned) {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
        }
    }

    ToolScreenScaffold(
        colors = colors,
        icon = AppIcons.forCalc(CalcKey.QIBLA),
        title = CalcKey.QIBLA.title,
        subtitle = "تحديد اتجاه الكعبة المشرفة بدقة عالية باستخدام البوصلة المدمجة وموقعك الحالي",
        onPrimaryActionClick = { isCompassActive = !isCompassActive },
        primaryActionText = if (isCompassActive) "إيقاف البوصلة" else "تشغيل البوصلة"
    ) {
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

        Spacer(modifier = Modifier.height(Spacing.Large))

        // Compass View
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp),
            contentAlignment = Alignment.Center
        ) {
            if (lat != null) {
                // Background Glow
                Surface(
                    modifier = Modifier.size(280.dp),
                    shape = CircleShape,
                    color = colors.accent.copy(alpha = 0.05f)
                ) {}

                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .clip(CircleShape)
                        .background(colors.surface2.copy(alpha = 0.3f))
                        .border(
                            width = if (isAligned) 2.dp else 1.dp,
                            // توهج ذهبي عند المحاذاة الدقيقة (±2 درجة) مع اتجاه القبلة
                            color = if (isAligned) colors.accent else colors.border.copy(alpha = 0.5f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Compass Dial marks
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val radius = size.width / 2
                        for (i in 0 until 360 step 30) {
                            val angleRad = Math.toRadians(i.toDouble() - 90.0)
                            val start = Offset(
                                center.x + (radius - 10.dp.toPx()) * cos(angleRad).toFloat(),
                                center.y + (radius - 10.dp.toPx()) * sin(angleRad).toFloat()
                            )
                            val end = Offset(
                                center.x + radius * cos(angleRad).toFloat(),
                                center.y + radius * sin(angleRad).toFloat()
                            )
                            drawLine(colors.textMuted.copy(alpha = 0.3f), start, end, strokeWidth = 2.dp.toPx())
                        }
                    }

                    // Rotating Qibla Indicator
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.rotate(animatedRotation)
                    ) {
                        Icon(
                            imageVector = AppIcons.forCalc(CalcKey.PRAYER),
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(64.dp).padding(bottom = 12.dp)
                        )
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(60.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(colors.accent, Color.Transparent)
                                    )
                                )
                        )
                    }

                    // Center Point
                    Surface(
                        modifier = Modifier.size(12.dp),
                        shape = CircleShape,
                        color = colors.accent,
                        border = BorderStroke(2.dp, Color.White)
                    ) {}
                }

                // Bearing Info
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 20.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = colors.surface,
                    tonalElevation = 4.dp
                ) {
                    Text(
                        "${qiblaAngle.toInt()}° درجة باتجاه الكعبة",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.accent
                    )
                }
            } else {
                Text("يرجى تفعيل الموقع أولاً لتحديد القبلة", color = colors.textMuted, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun AdhkarScreen(colors: CustomThemeColors) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Morning, 1: Evening
    val itemsList = if (selectedTab == 0) IslamicData.morningAdhkar else IslamicData.eveningAdhkar
    val counts = remember { mutableStateMapOf<Int, Int>() }

    ToolScreenScaffold(
        colors = colors,
        icon = AppIcons.forCalc(CalcKey.ADHKAR),
        title = if (selectedTab == 0) "أذكار الصباح" else "أذكار المساء",
        subtitle = "حصن المسلم اليومي بذكر الله والاستعاذة من كل شر"
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = colors.surface.copy(alpha = 0.5f),
            contentColor = colors.accent,
            modifier = Modifier.clip(RoundedCornerShape(16.dp)),
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = colors.accent
                )
            },
            divider = {}
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(AppIcons.forCalc(CalcKey.WEATHER), null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("أذكار الصباح", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(AppIcons.forCalc(CalcKey.ADHKAR), null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("أذكار المساء", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            })
        }

        Spacer(modifier = Modifier.height(Spacing.Medium))

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsList.forEach { dhikr ->
                val currentCount = counts[dhikr.id] ?: 0
                val isFinished = currentCount >= dhikr.countTarget

                Surface(
                    color = colors.surface,
                    shape = RoundedCornerShape(20.dp),
                    tonalElevation = if (isFinished) 0.dp else 2.dp,
                    border = if (isFinished) BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)) else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (currentCount < dhikr.countTarget) {
                                counts[dhikr.id] = currentCount + 1
                            }
                        }
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            dhikr.text, 
                            fontSize = 17.sp, 
                            fontWeight = FontWeight.Medium, 
                            color = colors.text, 
                            lineHeight = 28.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (dhikr.rewardText.isNotBlank()) {
                            Surface(
                                color = colors.surface2.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                                    Icon(AppIcons.Info, null, tint = colors.accent, modifier = Modifier.size(14.dp).padding(top = 2.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        dhikr.rewardText, 
                                        fontSize = 12.sp, 
                                        color = colors.textMuted,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                        
                        if (dhikr.reference.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                                Icon(AppIcons.forCalc(CalcKey.ADHKAR), null, tint = colors.accent.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    dhikr.reference, 
                                    fontSize = 11.sp, 
                                    color = colors.accent.copy(alpha = 0.7f), 
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    if (isFinished) "تم الانتهاء ✓" else "التكرار الحالي",
                                    fontSize = 12.sp,
                                    color = if (isFinished) Color(0xFF10B981) else colors.textMuted
                                )
                                Text(
                                    "$currentCount / ${dhikr.countTarget}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isFinished) Color(0xFF10B981) else colors.accent
                                )
                            }

                            Surface(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .clickable(enabled = !isFinished) {
                                        counts[dhikr.id] = currentCount + 1
                                    },
                                color = if (isFinished) Color(0xFF10B981) else colors.accent,
                                shadowElevation = 4.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (isFinished) {
                                        Icon(Icons.Filled.Check, null, tint = Color.White)
                                    } else {
                                        Text("${dhikr.countTarget - currentCount}", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
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

    ToolScreenScaffold(
        colors = colors,
        icon = AppIcons.forCalc(CalcKey.TASBIH),
        title = "المسبحة الإلكترونية",
        subtitle = "اذكر الله في كل وقت ومكان مع تتبع المجموع الكلي لأذكارك",
        onPrimaryActionClick = { 
            count = 0
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        },
        primaryActionText = "إعادة ضبط العداد الحالي"
    ) {
        // Counters Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                color = colors.surface,
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 4.dp,
                modifier = Modifier
                    .weight(1f)
                    .clickable { 
                        targetCount = when(targetCount) {
                            33 -> 99
                            99 -> 1000
                            else -> 33
                        }
                    }
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("الهدف", color = colors.textMuted, fontSize = 12.sp)
                    Text("$targetCount", color = colors.accent, fontSize = 22.sp, fontWeight = FontWeight.Black)
                }
            }
            
            Surface(
                color = colors.surface,
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 4.dp,
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("المجموع الكلي", color = colors.textMuted, fontSize = 12.sp)
                    Text("$lifetimeCount", color = colors.text, fontSize = 22.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Dhikr Selector Button
        Surface(
            color = colors.surface2.copy(alpha = 0.3f),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDhikrSelector = true }
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = dhikrName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.accent,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(12.dp))
                Icon(Icons.Default.Settings, null, tint = colors.accent, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Main Clicker Bead
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
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
                val strokeWidth = 16.dp.toPx()
                val radius = (size.minDimension / 2) - strokeWidth
                
                // Outer Glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(colors.accent.copy(alpha = 0.15f), Color.Transparent),
                        center = center,
                        radius = radius + 40.dp.toPx()
                    ),
                    radius = radius + 40.dp.toPx()
                )

                // Main Circular Path
                drawCircle(
                    color = colors.border.copy(alpha = 0.3f),
                    radius = radius,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Beads on the ring
                val beadCount = 33
                for (i in 0 until beadCount) {
                    val angleRad = Math.toRadians((i.toFloat() / beadCount) * 360.0 - 90.0)
                    val cx = center.x + radius * cos(angleRad).toFloat()
                    val cy = center.y + radius * sin(angleRad).toFloat()
                    
                    val isHighlighted = if (count == 0) false else (i < (count % beadCount)) || (count > 0 && count % beadCount == 0)
                    
                    drawCircle(
                        color = if (isHighlighted) colors.accent else colors.surface2,
                        radius = if (isHighlighted) 10.dp.toPx() else 7.dp.toPx(),
                        center = Offset(cx, cy)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$count",
                    fontSize = 96.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.text
                )
                Text(
                    text = "تكرار",
                    fontSize = 18.sp,
                    color = colors.textMuted,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showDhikrSelector) {
        AlertDialog(
            onDismissRequest = { showDhikrSelector = false },
            containerColor = colors.surface,
            title = { Text("اختر الذكر", color = colors.text, fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(defaultDhikrs) { dhikr ->
                        Surface(
                            color = if (dhikrName == dhikr) colors.accent.copy(alpha = 0.1f) else Color.Transparent,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().clickable {
                                dhikrName = dhikr
                                count = 0
                                showDhikrSelector = false
                            }
                        ) {
                            Text(dhikr, modifier = Modifier.padding(16.dp), color = colors.text, fontSize = 18.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDhikrSelector = false }) { Text("إغلاق") }
            }
        )
    }
}

@Composable
fun QuranScreen(colors: CustomThemeColors) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val quranPrefs = remember { context.getSharedPreferences("clevcalc_quran_prefs", Context.MODE_PRIVATE) }

    // Persistent State reading
    var lastSurahNum by remember { mutableStateOf(quranPrefs.getInt("last_surah_num", 0)) }
    var lastSurahName by remember { mutableStateOf(quranPrefs.getString("last_surah_name", "الفاتحة") ?: "الفاتحة") }
    var lastAyahNum by remember { mutableStateOf(quranPrefs.getInt("last_ayah_num", 1)) }

    // Favorites toggling list
    var favoritesSet by remember {
        mutableStateOf(
            quranPrefs.getStringSet("favorite_surahs", emptySet()) ?: emptySet()
        )
    }

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
    var selectedSurah by remember { mutableStateOf<SurahInfo?>(null) }
    var showBookmarks by remember { mutableStateOf(false) }

    if (selectedSurah != null) {
        SurahDetailReader(
            surah = selectedSurah!!,
            colors = colors,
            onBack = {
                selectedSurah = null
                // Refresh continue reading positions
                lastSurahNum = quranPrefs.getInt("last_surah_num", 0)
                lastSurahName = quranPrefs.getString("last_surah_name", "الفاتحة") ?: "الفاتحة"
                lastAyahNum = quranPrefs.getInt("last_ayah_num", 1)
            }
        )
    } else {
        val filteredSurahs = remember(searchQuery) {
            if (searchQuery.isBlank()) IslamicData.surahs
            else IslamicData.surahs.filter {
                it.nameAr.contains(searchQuery.trim()) ||
                it.nameEn.lowercase().contains(searchQuery.trim().lowercase()) ||
                it.number.toString() == searchQuery.trim()
            }
        }

        // Deep Charcoal Serenity Background Matching System Guidelines
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.appBg)
        ) {
            // Elegant pattern at 2-3% opacity for spiritual focus
            Image(
                painter = painterResource(id = R.drawable.ic_islamic_pattern),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.02f),
                contentScale = ContentScale.Inside,
                colorFilter = ColorFilter.tint(colors.accent.copy(alpha = 0.5f))
            )

            LazyColumn(
                contentPadding = PaddingValues(
                    top = 16.dp,
                    bottom = 120.dp,
                    start = 16.dp,
                    end = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Header Details
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "القرآن الكريم",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.text
                        )
                        Text(
                            text = "تصفح واقرأ آيات الذكر الحكيم برسم المصحف الشريف",
                            fontSize = 12.sp,
                            color = colors.textMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(1.dp)
                                .background(colors.accent.copy(alpha = 0.3f))
                        )
                    }
                }

                // 3. Premium Hero Continue Reading Card
                item {
                    val hasHistory = lastSurahNum > 0
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .clickable {
                                if (hasHistory) {
                                    val target = IslamicData.surahs.find { it.number == lastSurahNum }
                                    if (target != null) selectedSurah = target
                                } else {
                                    selectedSurah = IslamicData.surahs.firstOrNull()
                                }
                            },
                        color = Color(0xFF1E262C).copy(alpha = 0.75f), // Obsidian Glass
                        border = BorderStroke(1.dp, colors.accent), // Royal Gold Border
                        shadowElevation = 4.dp
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                            // Subtle Decorative sparkles in Background
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = colors.accent.copy(alpha = 0.12f),
                                modifier = Modifier
                                    .size(90.dp)
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 10.dp, y = 10.dp)
                            )

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.MenuBook,
                                            contentDescription = null,
                                            tint = colors.accent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "متابعة القراءة",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.accent
                                        )
                                    }

                                    // Simulated progress
                                    Surface(
                                        color = colors.accent.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "وردك اليومي",
                                            fontSize = 9.sp,
                                            color = colors.accent,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                if (hasHistory) {
                                    Text(
                                        text = "سورة $lastSurahName",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "الآية رقم $lastAyahNum",
                                        fontSize = 13.sp,
                                        color = colors.textMuted
                                    )
                                } else {
                                    Text(
                                        text = "ابدأ رحلتك مع القرآن الكريم",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "تلاوة هادئة وحفظ للتقدم تلقائياً",
                                        fontSize = 12.sp,
                                        color = colors.textMuted
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        if (hasHistory) {
                                            val target = IslamicData.surahs.find { it.number == lastSurahNum }
                                            if (target != null) selectedSurah = target
                                        } else {
                                            selectedSurah = IslamicData.surahs.firstOrNull()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.align(Alignment.Start)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = if (hasHistory) "متابعة القراءة" else "ابدأ القراءة الآن",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.appBg
                                        )
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = colors.appBg,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Premium Search Section
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("ابحث باسم السورة، بالإنجليزية، أو رقمها...", fontSize = 13.sp, color = colors.textMuted) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = colors.accent) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, null, tint = colors.textMuted)
                                }
                            }
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.surface.copy(alpha = 0.75f),
                            unfocusedContainerColor = colors.surface.copy(alpha = 0.75f),
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.accent.copy(alpha = 0.2f),
                            focusedTextColor = colors.text,
                            unfocusedTextColor = colors.text
                        ),
                        modifier = Modifier.fillMaxWidth().height(54.dp)
                    )
                }

                // Premium Action Buttons / Actions Grid
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Bookmarks Action Button
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(22.dp))
                                .clickable { showBookmarks = !showBookmarks },
                            color = colors.surface.copy(alpha = 0.75f),
                            border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Bookmark, null, tint = colors.accent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("العلامات", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.text)
                            }
                        }

                        // Insights Card
                        Surface(
                            modifier = Modifier
                                .weight(1.2f),
                            color = colors.surface.copy(alpha = 0.75f),
                            shape = RoundedCornerShape(22.dp),
                            border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Lightbulb, null, tint = colors.accent, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "لا تنس قراءة سورة الملك الليلة 🌌",
                                    fontSize = 10.sp,
                                    color = colors.text,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Expandable Bookmarks List
                if (showBookmarks) {
                    item {
                        Surface(
                            color = colors.surface.copy(alpha = 0.75f),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "العلامات المحفوظة والتقدم",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.text
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                if (lastSurahNum > 0) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(colors.appBg.copy(alpha = 0.5f))
                                            .clickable {
                                                val target = IslamicData.surahs.find { it.number == lastSurahNum }
                                                if (target != null) selectedSurah = target
                                            }
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.BookmarkAdded, null, tint = colors.accent, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("سورة $lastSurahName", fontSize = 13.sp, color = colors.text, fontWeight = FontWeight.Bold)
                                        }
                                        Text("آية $lastAyahNum", fontSize = 12.sp, color = colors.accent)
                                    }
                                } else {
                                    Text("لا توجد علامات مرجعية محفوظة حالياً.", fontSize = 11.sp, color = colors.textMuted)
                                }
                            }
                        }
                    }
                }

                // 5. Favorites & Recent reading Grid (Horizontal list)
                if (favoritesSet.isNotEmpty()) {
                    item {
                        Text(
                            "السور المفضلة",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.text,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            items(favoritesSet.toList()) { favNumStr ->
                                val favNum = favNumStr.toIntOrNull() ?: 1
                                val surah = IslamicData.surahs.find { it.number == favNum }
                                if (surah != null) {
                                    Surface(
                                        modifier = Modifier
                                            .width(130.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable { selectedSurah = surah },
                                        color = colors.surface.copy(alpha = 0.75f),
                                        border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.2f))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clip(CircleShape)
                                                        .background(colors.accent.copy(alpha = 0.1f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("${surah.number}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colors.accent)
                                                }
                                                IconButton(
                                                    onClick = { toggleFavorite(surah.number) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.Favorite, null, tint = colors.accent, modifier = Modifier.size(14.dp))
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(surah.nameAr, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.text)
                                            Text("${surah.totalVerses} آية", fontSize = 10.sp, color = colors.textMuted)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 6. Complete Surah List Header
                item {
                    Text(
                        "قائمة السور الشريفة",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.text,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                }

                if (filteredSurahs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("لم يتم العثور على نتائج للبحث.", color = colors.textMuted)
                        }
                    }
                } else {
                    items(filteredSurahs) { surah ->
                        val isFavorite = favoritesSet.contains(surah.number.toString())
                        val isCurrentRead = surah.number == lastSurahNum

                        Surface(
                            color = colors.surface.copy(alpha = 0.75f),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(
                                if (isCurrentRead) 1.5.dp else 1.dp,
                                if (isCurrentRead) colors.accent else colors.accent.copy(alpha = 0.2f)
                            ),
                            tonalElevation = if (isCurrentRead) 6.dp else 2.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedSurah = surah }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Islamic Star Badge
                                    Box(
                                        modifier = Modifier.size(44.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            val path = androidx.compose.ui.graphics.Path().apply {
                                                val r = size.minDimension / 2
                                                for (i in 0 until 8) {
                                                    val angleRad = Math.toRadians(i * 45.0)
                                                    val x = center.x + r * cos(angleRad).toFloat()
                                                    val y = center.y + r * sin(angleRad).toFloat()
                                                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                                                }
                                                close()
                                            }
                                            drawPath(path, colors.accent.copy(alpha = 0.1f))
                                            drawPath(path, colors.accent, style = Stroke(width = 1.dp.toPx()))
                                        }
                                        Text(
                                            text = "${surah.number}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = colors.accent
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = surah.nameAr,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.text
                                            )
                                            if (isCurrentRead) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    color = colors.accent.copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        "مفتوح",
                                                        fontSize = 8.sp,
                                                        color = colors.accent,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "${surah.place} • ${surah.totalVerses} آية",
                                            fontSize = 12.sp,
                                            color = colors.textMuted
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { toggleFavorite(surah.number) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = "تفضيل",
                                            tint = colors.accent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.Default.ChevronLeft,
                                        contentDescription = null,
                                        tint = colors.textMuted,
                                        modifier = Modifier.size(20.dp)
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

@Composable
fun SurahDetailReader(
    surah: SurahInfo,
    colors: CustomThemeColors,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val quranPrefs = remember { context.getSharedPreferences("clevcalc_quran_prefs", Context.MODE_PRIVATE) }

    var versesList by remember(surah.number) { mutableStateOf(surah.verses) }
    var isLoading by remember(surah.number) { mutableStateOf(surah.verses.isEmpty()) }
    var loadError by remember(surah.number) { mutableStateOf<String?>(null) }

    // Update last read position details
    LaunchedEffect(surah.number) {
        quranPrefs.edit()
            .putInt("last_surah_num", surah.number)
            .putString("last_surah_name", surah.nameAr)
            .putInt("last_ayah_num", 1)
            .apply()
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

            // Sync current visible item with last read position
            LaunchedEffect(listState.firstVisibleItemIndex) {
                if (versesList.isNotEmpty()) {
                    val currentAyah = listState.firstVisibleItemIndex + 1
                    quranPrefs.edit().putInt("last_ayah_num", currentAyah).apply()
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
                                quranPrefs.edit()
                                    .putInt("last_surah_num", nextSurahNumber)
                                    .putString("last_surah_name", nextSurah.nameAr)
                                    .putInt("last_ayah_num", 1)
                                    .apply()
                            }
                        } else {
                            // خلّص المصحف كله (سورة الناس) - يرجع الورد يبدأ من الفاتحة
                            // تاني بدل ما يفضل عالق على آخر سورة
                            quranPrefs.edit()
                                .putInt("last_surah_num", 1)
                                .putString("last_surah_name", "الفاتحة")
                                .putInt("last_ayah_num", 1)
                                .putBoolean("khatma_completed_flag", true)
                                .apply()
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

    ToolScreenScaffold(
        colors = colors,
        icon = AppIcons.forCalc(CalcKey.ZAKAT),
        title = "حاسبة الزكاة",
        subtitle = "احسب زكاة مالك بدقة بناءً على أسعار الذهب والفضة الحالية"
    ) {
        // Result Card
        Surface(
            color = if (isNisabMet) colors.accent.copy(alpha = 0.1f) else colors.surface,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, if (isNisabMet) colors.accent else colors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (isNisabMet) "قيمة الزكاة المستحقة" else "لم يبلغ النصاب بعد", color = colors.textMuted, fontSize = 14.sp)
                Text(
                    String.format("%.2f EGP", zakatAmount),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isNisabMet) colors.accent else colors.text
                )
                if (isNisabMet) {
                    Text("الحمد لله على سعة الرزق", color = colors.accent, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.Large))

        // Input Fields
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
            ZakatInputField("النقد والسيولة (EGP)", cashText, colors) { cashText = it }
            ZakatInputField("وزن الذهب (جرام عيار 24)", goldGramsText, colors) { goldGramsText = it }
            ZakatInputField("وزن الفضة (جرام)", silverGramsText, colors) { silverGramsText = it }
            ZakatInputField("الديون والالتزامات (EGP)", debtsText, colors) { debtsText = it }
        }

        Spacer(modifier = Modifier.height(Spacing.Large))

        // Nisab Info
        Surface(
            color = colors.surface2.copy(alpha = 0.5f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Settings, null, tint = colors.accent, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "قيمة النصاب الحالية: ${String.format("%.0f", nisabUsd)} EGP (85 جرام ذهب عيار 24)",
                    fontSize = 12.sp,
                    color = colors.textMuted
                )
            }
        }
    }
}

@Composable
fun ZakatInputField(label: String, value: String, colors: CustomThemeColors, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.accent,
            unfocusedBorderColor = colors.border,
            focusedTextColor = colors.text,
            unfocusedTextColor = colors.text,
            focusedLabelColor = colors.accent
        ),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
    )
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
