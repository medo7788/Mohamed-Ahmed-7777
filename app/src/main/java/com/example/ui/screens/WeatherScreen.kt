package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CurrentWeatherData
import com.example.data.DailyForecastItem
import com.example.data.HourlyForecastItem
import com.example.data.WeatherCity
import com.example.data.WeatherRepository
import com.example.ui.components.LocationCardState
import com.example.ui.components.LocationStatusCard
import com.example.ui.theme.AppIcons
import com.example.ui.theme.CustomThemeColors
import com.example.util.AppLocationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

// Design Tokens for Premium Dark Obsidian Weather Layout
private val ObsidianBgStart = Color(0xFF090B10)
private val ObsidianBgEnd = Color(0xFF121821)
private val SurfaceGlassColor = Color(0xFF151A22).copy(alpha = 0.78f)
private val SurfaceGlassBorder = Color.White.copy(alpha = 0.08f)
private val ElectricCyan = Color(0xFF57E6FF)
private val RoyalGold = Color(0xFFD8B56A)
private val MintGreen = Color(0xFF50E3A4)
private val WarningAmber = Color(0xFFFFB347)
private val TextPrimary = Color(0xFFF1F5F9)
private val TextMuted = Color(0xFF94A3B8)

@Composable
fun PremiumWeatherScreen(colors: CustomThemeColors) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedCity by remember { mutableStateOf(WeatherRepository.defaultCities[0]) }
    var weatherData by remember { mutableStateOf<CurrentWeatherData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showCityPicker by remember { mutableStateOf(false) }
    var showErrorLogs by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    var selectedHourlyIndex by remember { mutableStateOf(0) }
    var aiAdviceText by remember { mutableStateOf<String?>(null) }
    var isGeneratingAdvice by remember { mutableStateOf(false) }

    var locState by remember { mutableStateOf(LocationCardState.IDLE) }
    var locName by remember { mutableStateOf<String?>(null) }
    var accuracy by remember { mutableStateOf<Float?>(null) }

    fun loadWeather(city: WeatherCity, forceRefresh: Boolean = false) {
        if (forceRefresh) isRefreshing = true else isLoading = true
        coroutineScope.launch {
            try {
                com.example.util.AppErrorLogger.logInfo("WeatherScreen", "جاري تحميل بيانات الطقس لمدينة: ${city.nameAr}")
                val data = WeatherRepository.fetchRealWeather(context, city.lat, city.lng)
                weatherData = data
                com.example.util.AppErrorLogger.logInfo("WeatherScreen", "تم تحميل الطقس بنجاح لمدينة: ${city.nameAr}")
            } catch (e: Exception) {
                com.example.util.AppErrorLogger.logError("WeatherScreen", "خطأ أثناء تحميل الطقس: ${e.localizedMessage}", e)
                weatherData = WeatherRepository.getFallbackWeather()
            } finally {
                isLoading = false
                isRefreshing = false
            }
        }
    }

    fun fetchGpsLocation() {
        locState = LocationCardState.LOADING
        coroutineScope.launch {
            val result = AppLocationProvider.fetchCurrentLocation(context)
            when (result) {
                is AppLocationProvider.Result.Success -> {
                    locState = LocationCardState.SUCCESS
                    accuracy = result.accuracyMeters
                    var placeName = "موقعي الحالي"
                    try {
                        val geocoder = android.location.Geocoder(context, Locale("ar"))
                        val addresses = withContext(Dispatchers.IO) {
                            geocoder.getFromLocation(result.latitude, result.longitude, 1)
                        }
                        if (!addresses.isNullOrEmpty()) {
                            val addr = addresses[0]
                            val parts = listOfNotNull(addr.locality ?: addr.subAdminArea, addr.adminArea, addr.countryName)
                            if (parts.isNotEmpty()) placeName = parts.distinct().take(2).joinToString("، ")
                        }
                    } catch (e: Exception) {
                        com.example.util.AppErrorLogger.logWarning("WeatherGeocoder", "Geocoder failed: ${e.localizedMessage}")
                    }
                    locName = placeName
                    val gpsCity = WeatherCity(placeName, "موقعي", result.latitude, result.longitude, "")
                    selectedCity = gpsCity
                    loadWeather(gpsCity)
                }
                is AppLocationProvider.Result.PermissionDenied -> {
                    locState = LocationCardState.PERMISSION_DENIED
                    com.example.util.AppErrorLogger.logInfo("WeatherLocation", "لم يتم منح إذن الموقع. جاري عرض طقس المدينة الافتراضية.")
                    loadWeather(selectedCity)
                }
                is AppLocationProvider.Result.LocationDisabled -> {
                    locState = LocationCardState.DISABLED
                    com.example.util.AppErrorLogger.logInfo("WeatherLocation", "خدمة GPS معطلة. جاري عرض طقس المدينة الافتراضية.")
                    loadWeather(selectedCity)
                }
                is AppLocationProvider.Result.Timeout, is AppLocationProvider.Result.Error -> {
                    locState = LocationCardState.ERROR
                    com.example.util.AppErrorLogger.logWarning("WeatherLocation", "تعذر تحديد موقع GPS. جاري عرض طقس المدينة الافتراضية.")
                    loadWeather(selectedCity)
                }
            }
        }
    }

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            fetchGpsLocation()
        } else {
            locState = LocationCardState.PERMISSION_DENIED
            loadWeather(selectedCity)
        }
    }

    LaunchedEffect(Unit) {
        // Load default city weather immediately while fetching GPS
        loadWeather(selectedCity)
        fetchGpsLocation()
    }

    // Main Gradient Canvas Background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(ObsidianBgStart, ObsidianBgEnd)
                )
            )
    ) {
        // Floating Ambient Light Effect
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(ElectricCyan.copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(size.width * 0.7f, size.height * 0.15f),
                    radius = size.width * 0.6f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(RoyalGold.copy(alpha = 0.06f), Color.Transparent),
                    center = Offset(size.width * 0.2f, size.height * 0.5f),
                    radius = size.width * 0.7f
                )
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Weather Screen Top Header Bar
            Surface(
                color = SurfaceGlassColor,
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                border = BorderStroke(1.dp, SurfaceGlassBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            color = ElectricCyan.copy(alpha = 0.15f),
                            shape = CircleShape,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.padding(9.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = selectedCity.nameAr,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (selectedCity.countryAr.isNotBlank()) selectedCity.countryAr else "طقس مباشر دقيق",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = RoyalGold.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, RoyalGold.copy(alpha = 0.3f)),
                            modifier = Modifier.clickable { showCityPicker = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Search, null, tint = RoyalGold, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تغيير", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RoyalGold)
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        val infiniteTransition = rememberInfiniteTransition()
                        val angle by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1200, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            )
                        )

                        IconButton(
                            onClick = { loadWeather(selectedCity, forceRefresh = true) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "تحديث",
                                tint = ElectricCyan,
                                modifier = Modifier
                                    .size(20.dp)
                                    .scale(if (isRefreshing) 1.2f else 1.0f)
                            )
                        }

                        IconButton(
                            onClick = { showErrorLogs = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = "سجل الأخطاء",
                                tint = Color(0xFFFFB347),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Main Weather Content
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // GPS Location status banner if needed
                if (locState != LocationCardState.SUCCESS) {
                    item {
                        LocationStatusCard(
                            colors = colors,
                            state = locState,
                            placeName = locName,
                            accuracyMeters = accuracy,
                            onRequestPermission = {
                                locationLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                            onOpenLocationSettings = {
                                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                            },
                            onRetry = { fetchGpsLocation() }
                        )
                    }
                }

                // Offline Fallback Notification Banner
                if (weatherData?.isOfflineFallback == true) {
                    item {
                        Surface(
                            color = WarningAmber.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CloudOff, null, tint = WarningAmber, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "وضع عدم الاتصال: تعذر الاتصال بالشبكة، يتم عرض أحدث بيانات متوفرة للطقس.",
                                    fontSize = 11.sp,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }

                if (isLoading) {
                    item {
                        WeatherSkeletonLoadingCard()
                    }
                } else if (weatherData != null) {
                    val weather = weatherData!!

                    // 1. Hero Weather Glass Card
                    item {
                        HeroWeatherGlassCard(
                            weather = weather,
                            cityName = selectedCity.nameAr
                        )
                    }

                    // 2. Hourly Weather Forecast Horizontal Row
                    if (weather.hourlyForecast.isNotEmpty()) {
                        item {
                            Column {
                                SectionTitle(title = "التوقعات بالساعة", icon = Icons.Default.Schedule)
                                Spacer(modifier = Modifier.height(8.dp))
                                HourlyForecastLazyRow(
                                    items = weather.hourlyForecast,
                                    selectedIndex = selectedHourlyIndex,
                                    onSelectIndex = { selectedHourlyIndex = it }
                                )
                            }
                        }
                    }

                    // 3. 2-Column Weather Metrics Grid
                    item {
                        Column {
                            SectionTitle(title = "تفاصيل ومؤشرات الطقس", icon = Icons.Default.Grid4x4)
                            Spacer(modifier = Modifier.height(10.dp))
                            WeatherMetricsGrid(weather = weather)
                        }
                    }

                    // 4. Weekly 7-Day Forecast
                    if (weather.dailyForecast.isNotEmpty()) {
                        item {
                            Column {
                                SectionTitle(title = "توقعات الأيام السبعة القادمة", icon = Icons.Default.DateRange)
                                Spacer(modifier = Modifier.height(10.dp))
                                WeeklyForecastCard(dailyItems = weather.dailyForecast)
                            }
                        }
                    }

                    // 5. Smart AI Weather Advisor Card
                    item {
                        SmartAIWeatherAdvisorCard(
                            weather = weather,
                            cityName = selectedCity.nameAr,
                            aiAdviceText = aiAdviceText,
                            isGeneratingAdvice = isGeneratingAdvice,
                            onGenerateAdvice = {
                                isGeneratingAdvice = true
                                coroutineScope.launch {
                                    val advice = WeatherRepository.getAIWeatherAdvice(context, selectedCity.nameAr, weather)
                                    aiAdviceText = advice
                                    isGeneratingAdvice = false
                                }
                            }
                        )
                    }
                } else {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("تعذر جلب بيانات الطقس. يرجى إعادة المحاولة.", color = TextMuted)
                        }
                    }
                }
            }
        }
    }

    // Search and City Picker Modal
    if (showCityPicker) {
        CityPickerDialog(
            currentCity = selectedCity,
            searchQuery = searchQuery,
            onQueryChange = { searchQuery = it },
            onSelectCity = { city ->
                selectedCity = city
                showCityPicker = false
                searchQuery = ""
                loadWeather(city)
            },
            onDismiss = {
                showCityPicker = false
                searchQuery = ""
            }
        )
    }

    // Error Log Viewer Modal
    if (showErrorLogs) {
        com.example.ui.components.ErrorLogViewerModal(
            onDismiss = { showErrorLogs = false }
        )
    }
}

// -----------------------------------------------------------------------------
// Sub-Components
// -----------------------------------------------------------------------------

@Composable
private fun SectionTitle(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}

@Composable
private fun HeroWeatherGlassCard(
    weather: CurrentWeatherData,
    cityName: String
) {
    val (descText, iconKey) = WeatherRepository.decodeWmoCode(weather.weatherCode)

    val maxT = weather.dailyMaxTemp.firstOrNull() ?: weather.tempC
    val minT = weather.dailyMinTemp.firstOrNull() ?: (weather.tempC - 8)

    // Pulsing Halo Animation for Main Weather Icon
    val infiniteTransition = rememberInfiniteTransition()
    val haloPulse by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Surface(
        color = SurfaceGlassColor,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.25f)),
        shadowElevation = 12.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Ambient Star / Glow Effect in background
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(ElectricCyan.copy(alpha = 0.12f), Color.Transparent),
                        center = Offset(size.width * 0.5f, size.height * 0.4f),
                        radius = size.width * 0.5f
                    )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Weather Icon with Halo Ring
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(100.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = ElectricCyan.copy(alpha = 0.12f * haloPulse),
                            radius = (size.minDimension / 2) * haloPulse
                        )
                        drawCircle(
                            color = ElectricCyan.copy(alpha = 0.25f),
                            radius = size.minDimension / 2.3f,
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }

                    Icon(
                        imageVector = AppIcons.forWeather(iconKey),
                        contentDescription = descText,
                        tint = ElectricCyan,
                        modifier = Modifier.size(64.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Big Temperature Text
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "${weather.tempC.toInt()}",
                        fontSize = 60.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    Text(
                        text = "°C",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = RoyalGold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Weather Condition Text
                Text(
                    text = descText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricCyan
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Feels like and Max/Min Pill Card
                Surface(
                    color = Color.Black.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, SurfaceGlassBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("المحسوسة: ", fontSize = 12.sp, color = TextMuted)
                            Text("${weather.feelsLikeC.toInt()}°", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(14.dp)
                                .background(TextMuted.copy(alpha = 0.3f))
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("العظمى / الصغرى: ", fontSize = 12.sp, color = TextMuted)
                            Text("${maxT.toInt()}° / ${minT.toInt()}°", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = RoyalGold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HourlyForecastLazyRow(
    items: List<HourlyForecastItem>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        itemsIndexed(items) { index, item ->
            val isSelected = index == selectedIndex

            val scale by animateFloatAsState(if (isSelected) 1.05f else 1.0f, label = "scale")

            Surface(
                color = if (isSelected) ElectricCyan.copy(alpha = 0.18f) else SurfaceGlassColor,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) ElectricCyan else SurfaceGlassBorder
                ),
                modifier = Modifier
                    .scale(scale)
                    .width(78.dp)
                    .clickable { onSelectIndex(index) }
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = item.timeLabel,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) ElectricCyan else TextMuted
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Icon(
                        imageVector = AppIcons.forWeather(item.icon),
                        contentDescription = null,
                        tint = if (isSelected) ElectricCyan else TextPrimary,
                        modifier = Modifier.size(28.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "${item.tempC.toInt()}°",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(AppIcons.Humidity, null, tint = TextMuted, modifier = Modifier.size(10.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "${item.humidityPercent}%",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherMetricsGrid(weather: CurrentWeatherData) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricGlassCard(
                title = "الرطوبة النسبية",
                value = "${weather.humidityPercent}%",
                subtitle = if (weather.humidityPercent > 65) "رطوبة مرتفعة" else "مستوى مريح",
                icon = AppIcons.Humidity,
                accentColor = ElectricCyan,
                modifier = Modifier.weight(1f)
            )
            MetricGlassCard(
                title = "سرعة الرياح",
                value = "${weather.windSpeedKmh.toInt()} كم/س",
                subtitle = if (weather.windSpeedKmh > 25) "رياح نشطة" else "نسيم خفيف",
                icon = AppIcons.Wind,
                accentColor = MintGreen,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricGlassCard(
                title = "الضغط الجوي",
                value = "${weather.pressureHpa} hPa",
                subtitle = "مستقر في المعدل الطبيعي",
                icon = Icons.Default.Compress,
                accentColor = RoyalGold,
                modifier = Modifier.weight(1f)
            )
            MetricGlassCard(
                title = "الأشعة فوق البنفسجية",
                value = String.format(Locale.US, "%.1f", weather.uvIndex),
                subtitle = when {
                    weather.uvIndex < 3.0 -> "منخفضة - آمنة"
                    weather.uvIndex < 6.0 -> "متوسطة - يفضل نظارة"
                    else -> "عالية - استخدم واقي شمس"
                },
                icon = Icons.Default.WbSunny,
                accentColor = if (weather.uvIndex > 6.0) WarningAmber else RoyalGold,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricGlassCard(
                title = "المدى الرؤية",
                value = "${weather.visibilityKm.toInt()} كم",
                subtitle = "رؤية أفقية واضحة وممتازة",
                icon = Icons.Default.Visibility,
                accentColor = MintGreen,
                modifier = Modifier.weight(1f)
            )
            MetricGlassCard(
                title = "جودة الهواء (AQI)",
                value = "${weather.aqi} AQI",
                subtitle = "ممتازة ونقية للأنشطة",
                icon = Icons.Default.Air,
                accentColor = MintGreen,
                modifier = Modifier.weight(1f)
            )
        }

        // Sunrise & Sunset Sun Arc Card
        SunTrajectoryCard(
            sunriseTime = weather.sunriseTime,
            sunsetTime = weather.sunsetTime
        )
    }
}

@Composable
private fun MetricGlassCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SurfaceGlassColor,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, SurfaceGlassBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                Surface(
                    color = accentColor.copy(alpha = 0.15f),
                    shape = CircleShape,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(icon, null, tint = accentColor, modifier = Modifier.padding(6.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(subtitle, fontSize = 10.sp, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SunTrajectoryCard(
    sunriseTime: String,
    sunsetTime: String
) {
    Surface(
        color = SurfaceGlassColor,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, SurfaceGlassBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WbTwilight, null, tint = RoyalGold, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("مسار الشمس والمشرق والمغرب", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Canvas Drawing Sun Arc
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    val path = Path().apply {
                        moveTo(w * 0.1f, h * 0.9f)
                        cubicTo(
                            w * 0.3f, h * 0.1f,
                            w * 0.7f, h * 0.1f,
                            w * 0.9f, h * 0.9f
                        )
                    }

                    // Dashed track
                    drawPath(
                        path = path,
                        color = RoyalGold.copy(alpha = 0.3f),
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Baseline horizon
                    drawLine(
                        color = TextMuted.copy(alpha = 0.2f),
                        start = Offset(0f, h * 0.9f),
                        end = Offset(w, h * 0.9f),
                        strokeWidth = 1.dp.toPx()
                    )

                    // Sun Position (Midday peak example)
                    val sunX = w * 0.5f
                    val sunY = h * 0.22f
                    drawCircle(color = RoyalGold.copy(alpha = 0.25f), radius = 16.dp.toPx(), center = Offset(sunX, sunY))
                    drawCircle(color = RoyalGold, radius = 7.dp.toPx(), center = Offset(sunX, sunY))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text("الشروق", fontSize = 11.sp, color = TextMuted)
                    Text(sunriseTime, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("الغروب", fontSize = 11.sp, color = TextMuted)
                    Text(sunsetTime, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            }
        }
    }
}

@Composable
private fun WeeklyForecastCard(dailyItems: List<DailyForecastItem>) {
    val overallMax = dailyItems.maxOfOrNull { it.maxTempC } ?: 40.0
    val overallMin = dailyItems.minOfOrNull { it.minTempC } ?: 10.0

    Surface(
        color = SurfaceGlassColor,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, SurfaceGlassBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            dailyItems.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Day name
                    Text(
                        text = item.dayNameAr,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.width(70.dp)
                    )

                    // Weather icon & condition
                    Row(
                        modifier = Modifier.width(110.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = AppIcons.forWeather(item.icon),
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = item.conditionAr,
                            fontSize = 11.sp,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Temperature Range Bar
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val totalRange = (overallMax - overallMin).coerceAtLeast(1.0)
                        val startFraction = ((item.minTempC - overallMin) / totalRange).toFloat().coerceIn(0f, 1f)
                        val endFraction = ((item.maxTempC - overallMin) / totalRange).toFloat().coerceIn(0f, 1f)

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                        ) {
                            val barWidth = size.width
                            val xStart = barWidth * startFraction
                            val xEnd = barWidth * endFraction

                            drawRoundRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(ElectricCyan, RoyalGold)
                                ),
                                topLeft = Offset(xStart, 0f),
                                size = androidx.compose.ui.geometry.Size((xEnd - xStart).coerceAtLeast(10f), size.height)
                            )
                        }
                    }

                    // Min / Max text
                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.width(60.dp)
                    ) {
                        Text(
                            text = "${item.minTempC.toInt()}°",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                        Text(
                            text = " / ",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                        Text(
                            text = "${item.maxTempC.toInt()}°",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }

                if (index < dailyItems.lastIndex) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        }
    }
}

@Composable
private fun SmartAIWeatherAdvisorCard(
    weather: CurrentWeatherData,
    cityName: String,
    aiAdviceText: String?,
    isGeneratingAdvice: Boolean,
    onGenerateAdvice: () -> Unit
) {
    Surface(
        color = SurfaceGlassColor,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, RoyalGold.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = RoyalGold.copy(alpha = 0.15f),
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, tint = RoyalGold, modifier = Modifier.padding(8.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("استشارة خبير الطقس الذكي", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("تحليل ذكاء اصطناعي للأنشطة والملابس", fontSize = 10.sp, color = TextMuted)
                    }
                }

                if (isGeneratingAdvice) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = RoyalGold)
                } else {
                    Button(
                        onClick = onGenerateAdvice,
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalGold),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("استشِر الخبير", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ObsidianBgStart)
                    }
                }
            }

            if (aiAdviceText != null) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(12.dp))

                androidx.compose.foundation.text.selection.SelectionContainer {
                    Text(
                        text = aiAdviceText,
                        fontSize = 12.sp,
                        color = TextPrimary,
                        lineHeight = 20.sp
                    )
                }
            } else if (!isGeneratingAdvice) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "انقر على زر 'استشِر الخبير' للحصول على توصيات سريعة ومخصصة للملابس، القيادة والأنشطة الخارجية بناءً على طقس $cityName الآن.",
                    fontSize = 11.sp,
                    color = TextMuted,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun WeatherSkeletonLoadingCard() {
    Surface(
        color = SurfaceGlassColor,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, SurfaceGlassBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(color = ElectricCyan, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("جارِ جلب بيانات الطقس المباشرة...", fontSize = 13.sp, color = TextMuted)
        }
    }
}

@Composable
private fun CityPickerDialog(
    currentCity: WeatherCity,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onSelectCity: (WeatherCity) -> Unit,
    onDismiss: () -> Unit
) {
    val filteredCities = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            WeatherRepository.defaultCities
        } else {
            WeatherRepository.defaultCities.filter {
                it.nameAr.contains(searchQuery, ignoreCase = true) ||
                it.countryAr.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = ElectricCyan, fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationCity, null, tint = ElectricCyan, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("اختر المدينة لمتابعة الطقس", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onQueryChange,
                    placeholder = { Text("ابحث عن مدينة أو دولة...", fontSize = 12.sp, color = TextMuted) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Black.copy(alpha = 0.3f),
                        unfocusedContainerColor = Color.Black.copy(alpha = 0.3f),
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = SurfaceGlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(modifier = Modifier.height(280.dp)) {
                    items(filteredCities, key = { it.nameAr }) { item ->
                        val isSelected = item.nameAr == currentCity.nameAr
                        Surface(
                            color = if (isSelected) ElectricCyan.copy(alpha = 0.18f) else Color.Transparent,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clickable { onSelectCity(item) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Public, null, tint = TextMuted, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(item.nameAr, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text(item.countryAr, fontSize = 11.sp, color = TextMuted)
                                    }
                                }

                                if (isSelected) {
                                    Text("✓", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ElectricCyan)
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFF151A22),
        titleContentColor = TextPrimary,
        textContentColor = TextPrimary
    )
}
