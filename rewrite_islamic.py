import re

with open("app/src/main/java/com/example/ui/screens/IslamicScreens.kt", "r") as f:
    content = f.read()

# ADD IMPORTS
imports_to_add = """
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
"""

if "import com.example.util.AppLocationProvider" not in content:
    content = content.replace("package com.example.ui.screens\n", f"package com.example.ui.screens\n{imports_to_add}")

def replace_function(content, func_name, new_code):
    start = content.find(f"fun {func_name}(")
    if start == -1: return content
    # Find end of function (assuming it's a top level composable, finding the next @Composable or end of file)
    next_start = content.find("@Composable", start + 10)
    if next_start == -1: next_start = len(content)
    end = next_start
    return content[:start] + new_code + "\n\n" + content[end:]

prayer_times_code = """fun PrayerTimesScreen(colors: CustomThemeColors) {
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
                    lat = result.lat
                    lng = result.lng
                    accuracy = result.accuracyMeters
                    locState = LocationCardState.SUCCESS
                    try {
                        val geocoder = Geocoder(context, Locale("ar"))
                        val addresses = withContext(Dispatchers.IO) { geocoder.getFromLocation(result.lat, result.lng, 1) }
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
            IslamicData.getDynamicPrayerTimesForLocation(lat!!, lng!!)
        } else {
            IslamicData.getDynamicPrayerTimesForCity(IslamicData.egyptCities.first())
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
}"""

qibla_code = """fun QiblaDirectionScreen(colors: CustomThemeColors) {
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
                    lat = result.lat
                    lng = result.lng
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
}"""

content = replace_function(content, "PrayerTimesScreen", prayer_times_code)
content = replace_function(content, "QiblaDirectionScreen", qibla_code)

with open("app/src/main/java/com/example/ui/screens/IslamicScreens.kt", "w") as f:
    f.write(content)

