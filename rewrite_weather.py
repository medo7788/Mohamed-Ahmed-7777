import re

with open("app/src/main/java/com/example/ui/screens/EconomicAndWeatherScreens.kt", "r") as f:
    content = f.read()

imports_to_add = """
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
    next_start = content.find("@Composable", start + 10)
    if next_start == -1: next_start = len(content)
    end = next_start
    return content[:start] + new_code + "\n\n" + content[end:]


weather_code = """fun WeatherScreen(colors: CustomThemeColors) {
    val context = LocalContext.current
    var selectedCity by remember { mutableStateOf(WeatherRepository.defaultCities[0]) }
    var weatherData by remember { mutableStateOf<CurrentWeatherData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showCityPicker by remember { mutableStateOf(false) }
    var aiAdviceText by remember { mutableStateOf<String?>(null) }
    var isGeneratingAdvice by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    var locState by remember { mutableStateOf(LocationCardState.IDLE) }
    var locName by remember { mutableStateOf<String?>(null) }
    var lat by remember { mutableStateOf<Double?>(null) }
    var lng by remember { mutableStateOf<Double?>(null) }
    var accuracy by remember { mutableStateOf<Float?>(null) }

    val locationLauncher = rememberLauncherForActivityResult(
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
                        val geocoder = android.location.Geocoder(context, java.util.Locale("ar"))
                        val addresses = withContext(Dispatchers.IO) { geocoder.getFromLocation(result.lat, result.lng, 1) }
                        if (!addresses.isNullOrEmpty()) {
                            val address = addresses[0]
                            val parts = listOfNotNull(address.countryName, address.adminArea, address.locality ?: address.subAdminArea)
                            if (parts.isNotEmpty()) locName = parts.joinToString("، ")
                        }
                    } catch (e: Exception) {}
                    
                    // Update weather based on location
                    selectedCity = CityLocationInfo(locName ?: "موقعي", "My Location", "🌍", result.lat, result.lng)
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

    LaunchedEffect(selectedCity) {
        isLoading = true
        weatherData = WeatherRepository.fetchWeather(selectedCity.lat, selectedCity.lng)
        isLoading = false
    }

    ToolScreenScaffold(
        colors = colors,
        icon = AppIcons.forCalc(CalcKey.WEATHER),
        title = CalcKey.WEATHER.title,
        description = "تحليل وتوقعات الطقس المباشرة",
        gradient = GradientTokens.LivePrices,
        inputContent = {
            LocationStatusCard(
                colors = colors,
                state = locState,
                placeName = locName,
                accuracyMeters = accuracy,
                onRequestPermission = {
                    locationLauncher.launch(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                    )
                },
                onOpenLocationSettings = {
                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                },
                onRetry = { fetchLocation() }
            )

            // City Picker Button
            OutlinedButton(
                onClick = { showCityPicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(selectedCity.icon, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(selectedCity.nameAr, color = colors.text, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                Text("تغيير ▾", color = colors.accent, fontSize = 12.sp)
            }
        },
        extraContent = {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.accent)
                }
            } else if (weatherData != null) {
                val weather = weatherData!!
                val (desc, icon) = WeatherRepository.decodeWmoCode(weather.weatherCode)
                
                // Weather Display
                Surface(
                    color = colors.surface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.Medium)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(Spacing.Medium),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(icon, fontSize = 72.sp)
                        Text(
                            "${weather.currentTemp.toInt()}°C",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.text
                        )
                        Text(desc, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = colors.textMuted)
                        
                        Spacer(modifier = Modifier.height(Spacing.Medium))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            MetricItem("الرطوبة", "${weather.humidityPercent}%", "💧", colors)
                            MetricItem("الرياح", "${weather.windSpeedKmh.toInt()} كم/س", "💨", colors)
                            MetricItem("الأمطار", "${weather.precipitationMm} مم", "🌧️", colors)
                        }
                    }
                }
                
                // AI Advice
                Surface(
                    color = colors.surface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.Medium)
                ) {
                    Column(modifier = Modifier.padding(Spacing.Medium)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🤖 نصيحة الذكاء الاصطناعي", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.text)
                            if (isGeneratingAdvice) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = colors.accent)
                            } else {
                                Button(
                                    onClick = {
                                        isGeneratingAdvice = true
                                        coroutineScope.launch {
                                            aiAdviceText = WeatherRepository.getAIWeatherAdvice(context, selectedCity.nameAr, weather)
                                            isGeneratingAdvice = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("استشِر 💡", fontSize = 11.sp, color = androidx.compose.ui.graphics.Color.White)
                                }
                            }
                        }
                        if (aiAdviceText != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(aiAdviceText!!, fontSize = 12.sp, color = colors.text, lineHeight = 18.sp)
                        }
                    }
                }
                
                // Forecast
                Text("التوقعات 📅", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.text, modifier = Modifier.padding(bottom = 8.dp).align(Alignment.Start))
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    for (idx in weather.dailyMaxTemp.indices) {
                        val maxT = weather.dailyMaxTemp[idx]
                        val minT = weather.dailyMinTemp[idx]
                        val code = weather.dailyWeatherCodes[idx]
                        val (_, fIcon) = WeatherRepository.decodeWmoCode(code)
                        Surface(
                            color = colors.surface,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(fIcon, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("اليوم ${idx + 1}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.text)
                                }
                                Text("${maxT.toInt()}° / ${minT.toInt()}° C", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.accent)
                            }
                        }
                    }
                }
            } else {
                Text("تعذر جلب بيانات الطقس", color = colors.text)
            }
        }
    )

    if (showCityPicker) {
        AlertDialog(
            onDismissRequest = { showCityPicker = false },
            confirmButton = {
                TextButton(onClick = { showCityPicker = false }) { Text("إغلاق", color = colors.accent) }
            },
            title = { Text("🌍 اختر المدينة", color = colors.text) },
            text = {
                androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.height(300.dp)) {
                    androidx.compose.foundation.lazy.items(WeatherRepository.defaultCities) { item ->
                        val isSelected = item.nameAr == selectedCity.nameAr
                        Surface(
                            color = if (isSelected) colors.accent.copy(alpha = 0.15f) else colors.surface,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clickable { selectedCity = item; showCityPicker = false }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(item.icon, fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("${item.nameAr} - ${item.countryAr}", fontSize = 13.sp, color = colors.text)
                            }
                        }
                    }
                }
            },
            containerColor = colors.surface
        )
    }
}

@Composable
fun MetricItem(title: String, value: String, icon: String, colors: CustomThemeColors) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 24.sp)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.text)
        Text(title, fontSize = 11.sp, color = colors.textMuted)
    }
}"""

content = replace_function(content, "WeatherScreen", weather_code)

with open("app/src/main/java/com/example/ui/screens/EconomicAndWeatherScreens.kt", "w") as f:
    f.write(content)
