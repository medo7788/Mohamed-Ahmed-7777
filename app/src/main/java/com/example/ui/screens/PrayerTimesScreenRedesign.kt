package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CalcKey
import com.example.ui.components.LocationCardState
import com.example.data.CityPrayerInfo
import com.example.data.IslamicData
import com.example.ui.theme.CustomThemeColors
import com.example.util.AdhanScheduler
import com.example.util.AppLocationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

// --- PRAYER DATA MODEL ---
data class PrayerItemInfo(
    val key: String,
    val nameAr: String,
    val nameEn: String,
    val timeStr: String,
    val isNext: Boolean,
    val isActive: Boolean
)

data class NextPrayerStatus(
    val nextNameAr: String,
    val nextTimeStr: String,
    val secondsRemaining: Long,
    val isImminent: Boolean,
    val progressRatio: Float,
    val currentPhase: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerTimesScreenRedesign(
    colors: CustomThemeColors,
    onNavigate: ((CalcKey) -> Unit)? = null
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("prayer_prefs_v2", Context.MODE_PRIVATE) }

    // State Variables
    var isLoading by remember { mutableStateOf(true) }
    var locState by remember { mutableStateOf<LocationCardState>(LocationCardState.IDLE) }
    var locName by remember { mutableStateOf<String?>(prefs.getString("cached_loc_name", "القاهرة، مصر")) }
    var lat by remember { mutableStateOf<Double?>(prefs.getFloat("cached_lat", 30.0444f).toDouble()) }
    var lng by remember { mutableStateOf<Double?>(prefs.getFloat("cached_lng", 31.2357f).toDouble()) }
    var isManualLocation by remember { mutableStateOf(prefs.getBoolean("is_manual_location", false)) }
    var isOffline by remember { mutableStateOf(!isNetworkAvailable(context)) }
    var isRefreshing by remember { mutableStateOf(false) }

    // Modal Sheet Visibility
    var showAdhanSettingsSheet by remember { mutableStateOf(false) }
    var showMuazzinSheet by remember { mutableStateOf(false) }
    var showCityPickerSheet by remember { mutableStateOf(false) }
    var showHijriCalendarSheet by remember { mutableStateOf(false) }

    // Audio Preview State
    var playingPrayerKey by remember { mutableStateOf<String?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    // Selected Voice / Muazzin
    var selectedMuazzinKey by remember { mutableStateOf(prefs.getString("adhan_voice_key", "makkah") ?: "makkah") }

    // Cleanup Media Player
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    // Location fetching function
    fun fetchLocationAndTimes(silent: Boolean = false, forceGPS: Boolean = false) {
        if (!silent) isLoading = true
        isOffline = !isNetworkAvailable(context)

        coroutineScope.launch {
            if (!silent) delay(300) // Smooth UX entrance

            // If user previously selected a manual city and didn't force GPS, retain selected city
            if (isManualLocation && !forceGPS) {
                val cachedLat = prefs.getFloat("cached_lat", 0f).toDouble()
                val cachedLng = prefs.getFloat("cached_lng", 0f).toDouble()
                val cachedName = prefs.getString("cached_loc_name", null)
                if (cachedLat != 0.0 && cachedLng != 0.0) {
                    lat = cachedLat
                    lng = cachedLng
                    if (!cachedName.isNullOrBlank()) locName = cachedName
                    locState = LocationCardState.SUCCESS
                    isLoading = false
                    isRefreshing = false
                    return@launch
                }
            }

            if (forceGPS) {
                isManualLocation = false
                prefs.edit().putBoolean("is_manual_location", false).apply()
            }

            val result = AppLocationProvider.fetchCurrentLocation(context)
            when (result) {
                is AppLocationProvider.Result.Success -> {
                    lat = result.latitude
                    lng = result.longitude
                    locState = LocationCardState.SUCCESS

                    // Save cache
                    AppLocationProvider.saveLocationToCache(context, result.latitude, result.longitude, locName)
                    prefs.edit()
                        .putFloat("cached_lat", result.latitude.toFloat())
                        .putFloat("cached_lng", result.longitude.toFloat())
                        .apply()

                    // Geocoding to get city name
                    try {
                        val geocoder = Geocoder(context, Locale("ar"))
                        val addresses = withContext(Dispatchers.IO) {
                            geocoder.getFromLocation(result.latitude, result.longitude, 1)
                        }
                        if (!addresses.isNullOrEmpty()) {
                            val address = addresses[0]
                            val city = address.locality ?: address.subAdminArea ?: address.adminArea ?: "موقعك الحالي"
                            val country = address.countryName ?: ""
                            val formatted = if (country.isNotBlank()) "$city، $country" else city
                            locName = formatted
                            prefs.edit().putString("cached_loc_name", formatted).apply()
                            AppLocationProvider.saveLocationToCache(context, result.latitude, result.longitude, formatted)
                        }
                    } catch (_: Exception) {}
                }
                is AppLocationProvider.Result.PermissionDenied -> {
                    if (lat == null) locState = LocationCardState.PERMISSION_DENIED
                }
                is AppLocationProvider.Result.LocationDisabled -> {
                    if (lat == null) locState = LocationCardState.DISABLED
                }
                is AppLocationProvider.Result.Timeout, is AppLocationProvider.Result.Error -> {
                    if (lat == null) locState = LocationCardState.ERROR
                }
            }
            isLoading = false
            isRefreshing = false
        }
    }

    // Initial Load
    LaunchedEffect(Unit) {
        val cachedLat = prefs.getFloat("cached_lat", 0f).toDouble()
        val cachedLng = prefs.getFloat("cached_lng", 0f).toDouble()
        val cachedName = prefs.getString("cached_loc_name", null)

        if (cachedLat != 0.0 && cachedLng != 0.0) {
            lat = cachedLat
            lng = cachedLng
            if (!cachedName.isNullOrBlank()) locName = cachedName
            locState = LocationCardState.SUCCESS
        }

        if (!isManualLocation) {
            fetchLocationAndTimes(silent = lat != null, forceGPS = false)
        } else {
            isLoading = false
        }
    }

    // Permission Launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            fetchLocationAndTimes(forceGPS = true)
        } else {
            locState = LocationCardState.PERMISSION_DENIED
            isLoading = false
        }
    }

    // Dynamic Calculated Times
    val dynamicTimes = remember(lat, lng, locName, isManualLocation) {
        val selectedCity = if (locName != null) {
            IslamicData.cities.find { city ->
                locName!!.contains(city.nameAr) || locName!!.contains(city.nameEn, ignoreCase = true)
            }
        } else null

        if (selectedCity != null && isManualLocation) {
            IslamicData.DynamicPrayerTimes(
                fajr = selectedCity.fajr,
                sunrise = selectedCity.sunrise,
                dhuhr = selectedCity.dhuhr,
                asr = selectedCity.asr,
                maghrib = selectedCity.maghrib,
                isha = selectedCity.isha
            )
        } else if (lat != null && lng != null) {
            val tzOffset = IslamicData.getCorrectTimezoneOffset(lat!!, lng!!)
            IslamicData.calculatePrayerTimes(lat!!, lng!!, tzOffset)
        } else {
            IslamicData.getDynamicPrayerTimesForCity(IslamicData.cities.first())
        }
    }

    // Ticking Timer State (Updates every 1 second)
    var currentTimeMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTimeMs = System.currentTimeMillis()
        }
    }

    // Computed Countdown & Next Prayer
    val nextPrayerInfo = remember(currentTimeMs, dynamicTimes) {
        calculateNextPrayerInfo(dynamicTimes)
    }

    // Colors & Theme Tokens
    val royalGold = Color(0xFFD8B56A)
    val mintGlow = Color(0xFF63F4DD)
    val sunsetOrange = Color(0xFFFF9F43)
    val secondaryText = Color(0xFFBFC8D2)
    val cardGlassBg = Color(0xFF151A22).copy(alpha = 0.85f)
    val borderOverlay = Color.White.copy(alpha = 0.08f)

    // Modal Bottom Sheets / Dialogs
    if (showAdhanSettingsSheet) {
        AdhanSettingsModalSheet(
            colors = colors,
            context = context,
            onDismiss = { showAdhanSettingsSheet = false }
        )
    }

    if (showMuazzinSheet) {
        MuazzinSelectorModalSheet(
            colors = colors,
            selectedKey = selectedMuazzinKey,
            onSelect = { newKey ->
                selectedMuazzinKey = newKey
                prefs.edit().putString("adhan_voice_key", newKey).apply()
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            onDismiss = { showMuazzinSheet = false }
        )
    }

    if (showCityPickerSheet) {
        CityPickerModalSheet(
            colors = colors,
            currentCityName = locName ?: "القاهرة",
            onSelectCity = { city ->
                lat = city.lat
                lng = city.lng
                locName = "${city.nameAr}، ${city.countryAr}"
                locState = LocationCardState.SUCCESS
                isManualLocation = true
                prefs.edit()
                    .putBoolean("is_manual_location", true)
                    .putFloat("cached_lat", city.lat.toFloat())
                    .putFloat("cached_lng", city.lng.toFloat())
                    .putString("cached_loc_name", locName)
                    .apply()
                AppLocationProvider.saveLocationToCache(context, city.lat, city.lng, locName)
                showCityPickerSheet = false
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            onUseGPS = {
                showCityPickerSheet = false
                isManualLocation = false
                prefs.edit().putBoolean("is_manual_location", false).apply()
                if (AppLocationProvider.hasLocationPermission(context)) {
                    fetchLocationAndTimes(forceGPS = true)
                } else {
                    locationPermissionLauncher.launch(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                    )
                }
            },
            onDismiss = { showCityPickerSheet = false }
        )
    }

    if (showHijriCalendarSheet) {
        HijriCalendarModalSheet(
            colors = colors,
            onDismiss = { showHijriCalendarSheet = false }
        )
    }

    // MAIN SCREEN CANVAS & CONTENT
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF090B10), Color(0xFF151A22))
                )
            )
    ) {
        // Procedural Dynamic Sky & Geometry Background
        ProceduralSkyAndIslamicGeometryBackground(
            modifier = Modifier.fillMaxSize(),
            phase = nextPrayerInfo.currentPhase,
            royalGold = royalGold
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
            // 1. TOP HEADER & APP TITLE
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(cardGlassBg)
                        .border(1.dp, borderOverlay, RoundedCornerShape(22.dp))
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "مواقيت الصلاة والعبادات",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = royalGold.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, royalGold.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "المركز الإسلامي",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = royalGold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "تتبع الصلوات، الأذكار، والتقويم الهجري بدقة",
                            fontSize = 11.sp,
                            color = secondaryText,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isRefreshing = true
                            fetchLocationAndTimes()
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.06f))
                            .border(1.dp, borderOverlay, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "تحديث",
                            tint = royalGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // 2. OFFLINE BANNER (IF APPLICABLE)
            if (isOffline) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp)),
                        color = sunsetOrange.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, sunsetOrange.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.WifiOff,
                                contentDescription = null,
                                tint = sunsetOrange,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "وضع عدم الاتصال: يتم استخدام مواقيت الصلاة المحسوبة محلياً والمخزنة مؤقتاً.",
                                fontSize = 11.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // 3. UI STATE ROUTING (LOADING / ERROR / PERMISSION / SUCCESS)
            if (isLoading) {
                item { PrayerDashboardSkeleton() }
            } else if (locState == LocationCardState.PERMISSION_DENIED || locState == LocationCardState.DISABLED) {
                item {
                    LocationPermissionRequiredCard(
                        locState = locState,
                        onRequestPermission = {
                            locationPermissionLauncher.launch(
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                            )
                        },
                        onOpenSettings = {
                            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                        },
                        onSelectCityManually = {
                            showCityPickerSheet = true
                        },
                        royalGold = royalGold,
                        cardGlassBg = cardGlassBg,
                        borderOverlay = borderOverlay
                    )
                }
            } else if (locState == LocationCardState.ERROR && lat == null) {
                item {
                    ErrorRetryCard(
                        onRetry = { fetchLocationAndTimes() },
                        onSelectCity = { showCityPickerSheet = true },
                        royalGold = royalGold,
                        cardGlassBg = cardGlassBg
                    )
                }
            } else {
                // SUCCESS STATE CONTENT

                // A. HERO PRAYER SUMMARY CARD
                item {
                    DynamicHeroPrayerCard(
                        nextInfo = nextPrayerInfo,
                        locationName = locName ?: "القاهرة، مصر",
                        royalGold = royalGold,
                        mintGlow = mintGlow,
                        sunsetOrange = sunsetOrange,
                        secondaryText = secondaryText,
                        cardGlassBg = cardGlassBg,
                        onChangeLocation = { showCityPickerSheet = true }
                    )
                }

                // B. QUICK WORSHIP ACTIONS BAR
                item {
                    QuickWorshipActionsRow(
                        colors = colors,
                        royalGold = royalGold,
                        cardGlassBg = cardGlassBg,
                        borderOverlay = borderOverlay,
                        onOpenHijriCalendar = { showHijriCalendarSheet = true },
                        onNavigate = onNavigate
                    )
                }

                // C. SMART SPIRITUAL AI INSIGHTS CARD
                item {
                    SmartSpiritualInsightCard(
                        nextInfo = nextPrayerInfo,
                        royalGold = royalGold,
                        mintGlow = mintGlow,
                        cardGlassBg = cardGlassBg,
                        borderOverlay = borderOverlay
                    )
                }

                // D. PRAYER TIMES INTERACTIVE GRID
                item {
                    Text(
                        text = "مواقيت الصلوات الخمس والشروق 🕌",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp, bottom = 4.dp)
                    )
                }

                val pList = listOf(
                    PrayerItemInfo("fajr", "الفجر", "Fajr", dynamicTimes.fajr, nextPrayerInfo.nextNameAr == "الفجر", nextPrayerInfo.currentPhase == "fajr"),
                    PrayerItemInfo("sunrise", "الشروق", "Sunrise", dynamicTimes.sunrise, false, nextPrayerInfo.currentPhase == "sunrise"),
                    PrayerItemInfo("dhuhr", "الظهر", "Dhuhr", dynamicTimes.dhuhr, nextPrayerInfo.nextNameAr == "الظهر", nextPrayerInfo.currentPhase == "dhuhr"),
                    PrayerItemInfo("asr", "العصر", "Asr", dynamicTimes.asr, nextPrayerInfo.nextNameAr == "العصر", nextPrayerInfo.currentPhase == "asr"),
                    PrayerItemInfo("maghrib", "المغرب", "Maghrib", dynamicTimes.maghrib, nextPrayerInfo.nextNameAr == "المغرب", nextPrayerInfo.currentPhase == "maghrib"),
                    PrayerItemInfo("isha", "العشاء", "Isha", dynamicTimes.isha, nextPrayerInfo.nextNameAr == "العشاء", nextPrayerInfo.currentPhase == "isha")
                )

                items(pList, key = { it.key }) { prayer ->
                    val isPlayingThis = playingPrayerKey == prayer.key

                    PrayerTimeInteractiveCard(
                        prayer = prayer,
                        isPlaying = isPlayingThis,
                        royalGold = royalGold,
                        mintGlow = mintGlow,
                        cardGlassBg = cardGlassBg,
                        borderOverlay = borderOverlay,
                        onToggleAudioPreview = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (isPlayingThis) {
                                mediaPlayer?.stop()
                                mediaPlayer?.release()
                                mediaPlayer = null
                                playingPrayerKey = null
                            } else {
                                mediaPlayer?.stop()
                                mediaPlayer?.release()
                                mediaPlayer = try {
                                    val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                                    MediaPlayer.create(context, uri)?.apply {
                                        setOnCompletionListener {
                                            playingPrayerKey = null
                                        }
                                        start()
                                    }
                                } catch (_: Exception) { null }
                                playingPrayerKey = prayer.key
                            }
                        }
                    )
                }

                // E. QUICK BOTTOM ACTION BUTTONS BAR
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    QuickBottomControlsBar(
                        royalGold = royalGold,
                        cardGlassBg = cardGlassBg,
                        borderOverlay = borderOverlay,
                        onOpenSettings = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showAdhanSettingsSheet = true
                        },
                        onOpenMuazzin = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showMuazzinSheet = true
                        },
                        onChangeLocation = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showCityPickerSheet = true
                        }
                    )
                }
            }
        }
    }
}

// --- DYNAMIC HERO PRAYER SUMMARY CARD ---
@Composable
fun DynamicHeroPrayerCard(
    nextInfo: NextPrayerStatus,
    locationName: String,
    royalGold: Color,
    mintGlow: Color,
    sunsetOrange: Color,
    secondaryText: Color,
    cardGlassBg: Color,
    onChangeLocation: () -> Unit
) {
    val hijriFormatted = remember { getFormattedHijriDate() }
    val gregorianFormatted = remember {
        val sdf = java.text.SimpleDateFormat("EEEE، d MMMM yyyy", Locale("ar"))
        sdf.format(Calendar.getInstance().time)
    }

    // Infinite float translation animation
    val infiniteTransition = rememberInfiniteTransition(label = "HeroFloat")
    val heroOffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "HeroFloatOffset"
    )

    val formattedCountdown = remember(nextInfo.secondsRemaining) {
        val hrs = nextInfo.secondsRemaining / 3600
        val mins = (nextInfo.secondsRemaining % 3600) / 60
        val secs = nextInfo.secondsRemaining % 60
        String.format("%02d:%02d:%02d", hrs, mins, secs)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { translationY = heroOffsetY }
            .clip(RoundedCornerShape(28.dp)),
        color = cardGlassBg,
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(royalGold.copy(alpha = 0.6f), mintGlow.copy(alpha = 0.4f), royalGold.copy(alpha = 0.2f))
            )
        ),
        shadowElevation = 12.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(22.dp)) {
            // Procedural Subtle Glowing Canvas Vector in Card Corner
            Canvas(modifier = Modifier.size(100.dp).align(Alignment.TopEnd)) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(royalGold.copy(alpha = 0.15f), Color.Transparent)
                    ),
                    radius = size.width / 2
                )
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                // Top Tag Bar: Location & GPS Pulse
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onChangeLocation,
                        color = royalGold.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, royalGold.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(mintGlow)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = locationName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = royalGold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = royalGold,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Surface(
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = royalGold,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = hijriFormatted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Hero Countdown Status Block
                Text(
                    text = if (nextInfo.isImminent) "حان الآن أو اقترب موعد" else "الصلاة القادمة",
                    fontSize = 13.sp,
                    color = secondaryText,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "صلاة ${nextInfo.nextNameAr}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )

                    Surface(
                        color = if (nextInfo.isImminent) sunsetOrange.copy(alpha = 0.2f) else mintGlow.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, if (nextInfo.isImminent) sunsetOrange else mintGlow.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = nextInfo.nextTimeStr,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (nextInfo.isImminent) sunsetOrange else mintGlow,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Live Timer Display Block
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.35f))
                        .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "الوقت المتبقي للأذان",
                            fontSize = 11.sp,
                            color = secondaryText
                        )
                        Text(
                            text = formattedCountdown,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = royalGold,
                            letterSpacing = 2.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(royalGold.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = royalGold,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Progress Bar
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = gregorianFormatted, fontSize = 10.sp, color = secondaryText)
                        Text(text = "${(nextInfo.progressRatio * 100).toInt()}% من الفترة", fontSize = 10.sp, color = mintGlow, fontWeight = FontWeight.Bold)
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
                                .fillMaxWidth(nextInfo.progressRatio)
                                .clip(CircleShape)
                                .background(Brush.horizontalGradient(listOf(royalGold, mintGlow)))
                        )
                    }
                }
            }
        }
    }
}

// --- QUICK WORSHIP ACTIONS ROW ---
@Composable
fun QuickWorshipActionsRow(
    colors: CustomThemeColors,
    royalGold: Color,
    cardGlassBg: Color,
    borderOverlay: Color,
    onOpenHijriCalendar: () -> Unit,
    onNavigate: ((CalcKey) -> Unit)?
) {
    val haptic = LocalHapticFeedback.current

    val actions = remember {
        listOf(
            QuickActionItem("prayer", "الصلاة ومواقيتها", Icons.Default.AccessTime, CalcKey.PRAYER),
            QuickActionItem("quran", "القرآن الكريم", Icons.Default.MenuBook, CalcKey.QURAN),
            QuickActionItem("azkar", "الأذكار والأدعية", Icons.Default.AutoAwesome, CalcKey.ADHKAR),
            QuickActionItem("tasbih", "المسبحة الرقمية", Icons.Default.TouchApp, CalcKey.TASBIH),
            QuickActionItem("zakat", "حاسبة الزكاة", Icons.Default.AttachMoney, CalcKey.ZAKAT),
            QuickActionItem("qibla", "اتجاه القبلة", Icons.Default.Explore, CalcKey.QIBLA),
            QuickActionItem("hijri", "التقويم الهجري", Icons.Default.CalendarMonth, null),
            QuickActionItem("adhan_settings", "إعدادات الأذان", Icons.Default.Notifications, CalcKey.ADHAN_SETTINGS)
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "وصول سريع للعبادات ✦",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(actions, key = { it.id }) { action ->
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (action.id == "hijri") {
                            onOpenHijriCalendar()
                        } else if (action.navKey != null) {
                            onNavigate?.invoke(action.navKey)
                        }
                    },
                    modifier = Modifier
                        .height(52.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    color = cardGlassBg,
                    border = BorderStroke(1.dp, borderOverlay)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(royalGold.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = action.icon,
                                contentDescription = action.label,
                                tint = royalGold,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = action.label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

private data class QuickActionItem(
    val id: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val navKey: CalcKey?
)

// --- SMART SPIRITUAL AI INSIGHTS CARD ---
@Composable
fun SmartSpiritualInsightCard(
    nextInfo: NextPrayerStatus,
    royalGold: Color,
    mintGlow: Color,
    cardGlassBg: Color,
    borderOverlay: Color
) {
    val insightText = remember(nextInfo.currentPhase) {
        val cal = Calendar.getInstance()
        val isFriday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY

        when {
            isFriday -> "✨ يوم الجمعة المبارك: قال رسول الله ﷺ: «أكثروا علي من الصلاة فيه». لا تنس قراءة سورة الكهف."
            nextInfo.nextNameAr == "العصر" -> "⏳ «حافظوا على الصلوات والصلاة الوسطى وقوموا لله قانتين». اقترب موعد صلاة العصر."
            nextInfo.nextNameAr == "الفجر" -> "🌌 «وقرآن الفجر إن قرآن الفجر كان مشهوداً». صلاة الفجر خير من الدنيا وما فيها."
            nextInfo.currentPhase == "maghrib" -> "🌅 «ألا بذكر الله تطمئن القلوب». وقت أذكار المساء بعد صلاة المغرب."
            else -> "🌿 «فاذكروني أذكركم واشكروا لي ولا تكفرون». حافظ على أذكارك اليومية لطمأنينة قلبك."
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp)),
        color = cardGlassBg,
        border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(royalGold.copy(alpha = 0.4f), Color.Transparent)))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(royalGold.copy(alpha = 0.18f))
                    .border(1.dp, royalGold.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = royalGold,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "نفحة إيمانية 🕊️",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = royalGold
                )
                Text(
                    text = insightText,
                    fontSize = 12.sp,
                    color = Color.White,
                    modifier = Modifier.padding(top = 2.dp),
                    lineHeight = 17.sp
                )
            }
        }
    }
}

// --- PRAYER TIME INTERACTIVE CARD ---
@Composable
fun PrayerTimeInteractiveCard(
    prayer: PrayerItemInfo,
    isPlaying: Boolean,
    royalGold: Color,
    mintGlow: Color,
    cardGlassBg: Color,
    borderOverlay: Color,
    onToggleAudioPreview: () -> Unit
) {
    val isNext = prayer.isNext
    val isActive = prayer.isActive

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp)),
        color = if (isNext) mintGlow.copy(alpha = 0.12f) else if (isActive) royalGold.copy(alpha = 0.12f) else cardGlassBg,
        border = BorderStroke(
            1.dp,
            if (isNext) mintGlow.copy(alpha = 0.6f) else if (isActive) royalGold.copy(alpha = 0.5f) else borderOverlay
        ),
        shadowElevation = if (isNext) 8.dp else 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Procedural Vector Icon Container
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isNext) mintGlow.copy(alpha = 0.2f)
                            else if (isActive) royalGold.copy(alpha = 0.2f)
                            else Color.White.copy(alpha = 0.06f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(26.dp)) {
                        drawPrayerIconVector(prayer.key, if (isNext) mintGlow else if (isActive) royalGold else Color.White)
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = prayer.nameAr,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (isNext) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = mintGlow.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "الصلاة القادمة",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = mintGlow,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = prayer.nameEn,
                        fontSize = 11.sp,
                        color = Color(0xFFBFC8D2)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = prayer.timeStr,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isNext) mintGlow else if (isActive) royalGold else Color.White
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Audio Preview Button Chip
                IconButton(
                    onClick = onToggleAudioPreview,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (isPlaying) royalGold.copy(alpha = 0.25f)
                            else Color.White.copy(alpha = 0.06f)
                        )
                        .border(
                            1.dp,
                            if (isPlaying) royalGold else borderOverlay,
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.VolumeUp else Icons.Outlined.VolumeUp,
                        contentDescription = "معاينة الأذان",
                        tint = if (isPlaying) royalGold else Color(0xFFBFC8D2),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// --- QUICK BOTTOM CONTROLS BAR ---
@Composable
fun QuickBottomControlsBar(
    royalGold: Color,
    cardGlassBg: Color,
    borderOverlay: Color,
    onOpenSettings: () -> Unit,
    onOpenMuazzin: () -> Unit,
    onChangeLocation: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        BottomControlButton(
            modifier = Modifier.weight(1f),
            label = "التنبيهات",
            icon = Icons.Default.Notifications,
            royalGold = royalGold,
            cardGlassBg = cardGlassBg,
            borderOverlay = borderOverlay,
            onClick = onOpenSettings
        )
        BottomControlButton(
            modifier = Modifier.weight(1f),
            label = "المؤذن",
            icon = Icons.Default.RecordVoiceOver,
            royalGold = royalGold,
            cardGlassBg = cardGlassBg,
            borderOverlay = borderOverlay,
            onClick = onOpenMuazzin
        )
        BottomControlButton(
            modifier = Modifier.weight(1f),
            label = "الموقع",
            icon = Icons.Default.MyLocation,
            royalGold = royalGold,
            cardGlassBg = cardGlassBg,
            borderOverlay = borderOverlay,
            onClick = onChangeLocation
        )
    }
}

@Composable
private fun BottomControlButton(
    modifier: Modifier = Modifier,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    royalGold: Color,
    cardGlassBg: Color,
    borderOverlay: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp)),
        color = cardGlassBg,
        border = BorderStroke(1.dp, borderOverlay)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = royalGold,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

// --- LOCATION PERMISSION REQUIRED CARD ---
@Composable
fun LocationPermissionRequiredCard(
    locState: LocationCardState,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectCityManually: () -> Unit,
    royalGold: Color,
    cardGlassBg: Color,
    borderOverlay: Color
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp)),
        color = cardGlassBg,
        border = BorderStroke(1.dp, royalGold.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(royalGold.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOff,
                    contentDescription = null,
                    tint = royalGold,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (locState == LocationCardState.DISABLED) "موقع GPS غير مفعل" else "يتطلب إذن الموقع الجغرافي",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "لحساب مواقيت الصلاة والقبلة بدقة متناهية وفق خطوط الطول والعرض الخاصة بموقعك الحالي، يُرجى تفعيل الموقع.",
                fontSize = 12.sp,
                color = Color(0xFFBFC8D2),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = if (locState == LocationCardState.DISABLED) onOpenSettings else onRequestPermission,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = royalGold),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = if (locState == LocationCardState.DISABLED) "فتح الإعدادات" else "تمكين الموقع",
                        color = Color(0xFF090B10),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                OutlinedButton(
                    onClick = onSelectCityManually,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    border = BorderStroke(1.dp, royalGold.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "اختر مدينتك يدوياً",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

// --- ERROR RETRY CARD ---
@Composable
fun ErrorRetryCard(
    onRetry: () -> Unit,
    onSelectCity: () -> Unit,
    royalGold: Color,
    cardGlassBg: Color
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp)),
        color = cardGlassBg,
        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = Color(0xFFFF6B6B),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "تعذر تحديد الموقع الجغرافي حالياً",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "يرجى التحقق من الاتصال أو إعدادات موقع الهاتف والتجربة مرة أخرى.",
                fontSize = 12.sp,
                color = Color(0xFFBFC8D2),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = royalGold)
                ) {
                    Text("إعادة المحاولة", color = Color(0xFF090B10), fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onSelectCity,
                    border = BorderStroke(1.dp, royalGold.copy(alpha = 0.5f))
                ) {
                    Text("اختيار مدينة", color = Color.White)
                }
            }
        }
    }
}

// --- SKELETON SHIMMER LOADING ---
@Composable
fun PrayerDashboardSkeleton() {
    val infiniteTransition = rememberInfiniteTransition(label = "SkeletonShimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SkeletonAlpha"
    )

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White.copy(alpha = alpha))
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(4) {
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = alpha))
                )
            }
        }
        repeat(5) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White.copy(alpha = alpha))
            )
        }
    }
}

// --- MODAL SHEETS ---

// 1. Adhan Settings Sheet
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdhanSettingsModalSheet(
    colors: CustomThemeColors,
    context: Context,
    onDismiss: () -> Unit
) {
    val prefs = remember { context.getSharedPreferences("adhan_settings_prefs", Context.MODE_PRIVATE) }
    var fajrAlert by remember { mutableStateOf(prefs.getBoolean("adhan_fajr", true)) }
    var dhuhrAlert by remember { mutableStateOf(prefs.getBoolean("adhan_dhuhr", true)) }
    var asrAlert by remember { mutableStateOf(prefs.getBoolean("adhan_asr", true)) }
    var maghribAlert by remember { mutableStateOf(prefs.getBoolean("adhan_maghrib", true)) }
    var ishaAlert by remember { mutableStateOf(prefs.getBoolean("adhan_isha", true)) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF151A22),
        scrimColor = Color.Black.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "إعدادات الأذان والتنبيهات 🔔",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "قم بتفعيل أو إيقاف صوت الأذان لكل صلاة بشكل مستقل:",
                fontSize = 12.sp,
                color = Color(0xFFBFC8D2),
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            val items = listOf(
                "الفجر" to fajrAlert,
                "الظهر" to dhuhrAlert,
                "العصر" to asrAlert,
                "المغرب" to maghribAlert,
                "العشاء" to ishaAlert
            )

            items.forEach { (name, enabled) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "صلاة $name", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                    Switch(
                        checked = enabled,
                        onCheckedChange = { check ->
                            val key = when (name) {
                                "الفجر" -> "adhan_fajr"
                                "الظهر" -> "adhan_dhuhr"
                                "العصر" -> "adhan_asr"
                                "المغرب" -> "adhan_maghrib"
                                else -> "adhan_isha"
                            }
                            prefs.edit().putBoolean(key, check).apply()
                            when (name) {
                                "الفجر" -> fajrAlert = check
                                "الظهر" -> dhuhrAlert = check
                                "العصر" -> asrAlert = check
                                "المغرب" -> maghribAlert = check
                                else -> ishaAlert = check
                            }
                            AdhanScheduler.rescheduleAllFromPreferences(context)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFD8B56A), checkedTrackColor = Color(0xFFD8B56A).copy(alpha = 0.3f))
                    )
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD8B56A))
            ) {
                Text("حفظ وإغلاق", color = Color(0xFF090B10), fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// 2. Muazzin Selector Sheet
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MuazzinSelectorModalSheet(
    colors: CustomThemeColors,
    selectedKey: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val options = remember {
        listOf(
            "makkah" to "أذان الحرم المكي الشريف",
            "madinah" to "أذان المسجد النبوي الشريف",
            "aqsa" to "أذان المسجد الأقصى المبارك",
            "abdulbasit" to "الشيخ عبد الباسط عبد الصمد",
            "alafasy" to "الشيخ مشاري راشد العفاسي"
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF151A22),
        scrimColor = Color.Black.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "اختر صوت المؤذن 🎙️",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))

            options.forEach { (key, name) ->
                val isSelected = selectedKey == key
                Surface(
                    onClick = {
                        onSelect(key)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    color = if (isSelected) Color(0xFFD8B56A).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, if (isSelected) Color(0xFFD8B56A) else Color.White.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = name, fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.Medium)
                        if (isSelected) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color(0xFFD8B56A))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// 3. City Picker Sheet
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CityPickerModalSheet(
    colors: CustomThemeColors,
    currentCityName: String,
    onSelectCity: (CityPrayerInfo) -> Unit,
    onUseGPS: () -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredCities = remember(searchQuery) {
        if (searchQuery.isBlank()) IslamicData.cities
        else IslamicData.cities.filter {
            it.nameAr.contains(searchQuery.trim()) ||
            it.nameEn.lowercase().contains(searchQuery.trim().lowercase()) ||
            it.countryAr.contains(searchQuery.trim())
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF151A22),
        scrimColor = Color.Black.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(
                text = "اختر مدينتك 🌍",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onUseGPS,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF63F4DD).copy(alpha = 0.2f)),
                border = BorderStroke(1.dp, Color(0xFF63F4DD))
            ) {
                Icon(imageVector = Icons.Default.MyLocation, contentDescription = null, tint = Color(0xFF63F4DD))
                Spacer(modifier = Modifier.width(8.dp))
                Text("تحديد التلقائي عبر GPS", color = Color(0xFF63F4DD), fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)),
                placeholder = { Text("ابحث باسم المدينة أو الدولة...", color = Color(0xFFBFC8D2), fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFFD8B56A)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFD8B56A),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.height(300.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredCities, key = { it.nameEn }) { city ->
                    Surface(
                        onClick = { onSelectCity(city) },
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)),
                        color = Color.White.copy(alpha = 0.05f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = city.nameAr, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(text = city.countryAr, fontSize = 11.sp, color = Color(0xFFBFC8D2))
                            }
                            Text(text = "الفجر ${city.fajr} • المغرب ${city.maghrib}", fontSize = 10.sp, color = Color(0xFFD8B56A))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// 4. Hijri Calendar Sheet
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HijriCalendarModalSheet(
    colors: CustomThemeColors,
    onDismiss: () -> Unit
) {
    val occasions = remember {
        listOf(
            "بداية شهر رمضان المبارك" to "1 رمضان 1447",
            "عيد الفطر السعيد" to "1 شوال 1447",
            "يوم عرفة المبارك" to "9 ذو الحجة 1447",
            "عيد الأضحى المبارك" to "10 ذو الحجة 1447",
            "رأس السنة الهجرية" to "1 محرم 1448",
            "يوم عاشوراء" to "10 محرم 1448",
            "المولد النبوي الشريف" to "12 ربيع الأول 1448"
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF151A22),
        scrimColor = Color.Black.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "التقويم الهجري والمناسبات الإسلامية 📅",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "التاريخ الهجري اليوم: ${getFormattedHijriDate()}",
                fontSize = 13.sp,
                color = Color(0xFFD8B56A),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            Text(
                text = "أبرز المناسبات الدينية القادمة:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            occasions.forEach { (name, date) ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    color = Color.White.copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = name, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium)
                        Text(text = date, fontSize = 12.sp, color = Color(0xFF63F4DD), fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// --- HELPER LOGIC ---

private fun String?.isNullBrok(): Boolean = this == null || this.isBlank()

private fun isNetworkAvailable(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val capabilities = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
    return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
           capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
           capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
}

private fun getFormattedHijriDate(): String {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        try {
            val hijrahDate = java.time.chrono.HijrahDate.now()
            val formatter = java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ar"))
            return "${hijrahDate.format(formatter)} هـ"
        } catch (_: Exception) {}
    }
    val cal = Calendar.getInstance()
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val month = cal.get(Calendar.MONTH) + 1
    val year = cal.get(Calendar.YEAR)
    val hijriYear = ((year - 622) * 1.0307).toInt()
    val hijriMonths = listOf("محرم", "صفر", "ربيع الأول", "ربيع الثاني", "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة")
    val hijriMonthName = hijriMonths[(month - 1) % 12]
    return "$day $hijriMonthName $hijriYear هـ"
}

private fun calculateNextPrayerInfo(times: IslamicData.DynamicPrayerTimes): NextPrayerStatus {
    val cal = Calendar.getInstance()
    val nowHour = cal.get(Calendar.HOUR_OF_DAY)
    val nowMin = cal.get(Calendar.MINUTE)
    val nowSec = cal.get(Calendar.SECOND)
    val nowMins = nowHour * 60 + nowMin

    fun timeToMins(t: String): Int {
        val parts = t.split(":")
        if (parts.size < 2) return 0
        return parts[0].toInt() * 60 + parts[1].toInt()
    }

    val fajrMins = timeToMins(times.fajr)
    val sunriseMins = timeToMins(times.sunrise)
    val dhuhrMins = timeToMins(times.dhuhr)
    val asrMins = timeToMins(times.asr)
    val maghribMins = timeToMins(times.maghrib)
    val ishaMins = timeToMins(times.isha)

    val (nextName, nextTimeStr, targetMins, phase) = when {
        nowMins < fajrMins -> Quad("الفجر", times.fajr, fajrMins, "isha")
        nowMins < sunriseMins -> Quad("الشروق", times.sunrise, sunriseMins, "fajr")
        nowMins < dhuhrMins -> Quad("الظهر", times.dhuhr, dhuhrMins, "sunrise")
        nowMins < asrMins -> Quad("العصر", times.asr, asrMins, "dhuhr")
        nowMins < maghribMins -> Quad("المغرب", times.maghrib, maghribMins, "asr")
        nowMins < ishaMins -> Quad("العشاء", times.isha, ishaMins, "maghrib")
        else -> Quad("الفجر", times.fajr, fajrMins + 24 * 60, "isha")
    }

    val totalSecsRemaining = (targetMins * 60L - (nowMins * 60L + nowSec)).coerceAtLeast(0L)
    val isImminent = totalSecsRemaining <= 15 * 60 // Less than 15 mins

    val startMins = when (phase) {
        "isha" -> if (nowMins < fajrMins) ishaMins - 24 * 60 else ishaMins
        "fajr" -> fajrMins
        "sunrise" -> sunriseMins
        "dhuhr" -> dhuhrMins
        "asr" -> asrMins
        else -> maghribMins
    }
    val elapsed = (nowMins - startMins).coerceAtLeast(1)
    val duration = (targetMins - startMins).coerceAtLeast(1)
    val ratio = (elapsed.toFloat() / duration.toFloat()).coerceIn(0.05f, 1.0f)

    return NextPrayerStatus(
        nextNameAr = nextName,
        nextTimeStr = nextTimeStr,
        secondsRemaining = totalSecsRemaining,
        isImminent = isImminent,
        progressRatio = ratio,
        currentPhase = phase
    )
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

// --- PROCEDURAL BACKGROUND & VECTOR DRAWING ---

@Composable
fun ProceduralSkyAndIslamicGeometryBackground(
    modifier: Modifier = Modifier,
    phase: String,
    royalGold: Color
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Dynamic Sky Gradient Accent depending on Phase
        val skyGlowColor = when (phase) {
            "fajr" -> Color(0xFF63F4DD).copy(alpha = 0.10f)
            "sunrise" -> Color(0xFFFFB300).copy(alpha = 0.12f)
            "dhuhr" -> Color(0xFF29B6F6).copy(alpha = 0.08f)
            "asr" -> Color(0xFFFF9800).copy(alpha = 0.12f)
            "maghrib" -> Color(0xFFE65100).copy(alpha = 0.14f)
            else -> Color(0xFF3F51B5).copy(alpha = 0.10f)
        }

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(skyGlowColor, Color.Transparent),
                center = Offset(width / 2, height / 3),
                radius = width * 0.8f
            )
        )

        // Procedural Islamic Geometric Backdrop (Rub El Hizb 8-pointed star tessellation)
        val strokeColor = royalGold.copy(alpha = 0.05f)
        val step = 160f
        var x = 0f
        while (x < width + step) {
            var y = 0f
            while (y < height + step) {
                // Draw 8-pointed star outline
                val radius = 35f
                val path = Path()
                for (i in 0 until 8) {
                    val angle = Math.toRadians((i * 45).toDouble())
                    val outerX = (x + radius * cos(angle)).toFloat()
                    val outerY = (y + radius * sin(angle)).toFloat()
                    if (i == 0) path.moveTo(outerX, outerY) else path.lineTo(outerX, outerY)
                }
                path.close()
                drawPath(path, strokeColor, style = Stroke(width = 1f))
                y += step
            }
            x += step
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPrayerIconVector(key: String, color: Color) {
    val center = Offset(size.width / 2, size.height / 2)
    val r = size.width / 2.2f

    when (key) {
        "fajr" -> { // Crescent + Horizon line
            drawCircle(color = color.copy(alpha = 0.2f), radius = r, center = center)
            drawArc(color, 180f, 180f, false, topLeft = Offset(center.x - r, center.y - r), size = androidx.compose.ui.geometry.Size(r * 2, r * 2), style = Stroke(3f))
        }
        "sunrise" -> { // Sun rising over horizon
            drawLine(color, Offset(center.x - r, center.y + r / 2), Offset(center.x + r, center.y + r / 2), strokeWidth = 3f)
            drawCircle(color, radius = r / 2, center = Offset(center.x, center.y))
        }
        "dhuhr" -> { // Full zenith sun with rays
            drawCircle(color, radius = r / 2.5f, center = center)
            for (i in 0 until 8) {
                val angle = Math.toRadians((i * 45).toDouble())
                val startX = (center.x + (r / 2) * cos(angle)).toFloat()
                val startY = (center.y + (r / 2) * sin(angle)).toFloat()
                val endX = (center.x + r * cos(angle)).toFloat()
                val endY = (center.y + r * sin(angle)).toFloat()
                drawLine(color, Offset(startX, startY), Offset(endX, endY), strokeWidth = 2.5f)
            }
        }
        "asr" -> { // Afternoon sun
            drawCircle(color, radius = r / 2.2f, center = Offset(center.x - r / 4, center.y - r / 4))
        }
        "maghrib" -> { // Sunset sun sinking
            drawLine(color, Offset(center.x - r, center.y + r / 3), Offset(center.x + r, center.y + r / 3), strokeWidth = 3f)
            drawArc(color, 180f, 180f, true, topLeft = Offset(center.x - r / 2, center.y - r / 2), size = androidx.compose.ui.geometry.Size(r, r))
        }
        else -> { // Isha Moon & Stars
            drawCircle(color, radius = r / 2, center = center)
            drawCircle(Color.Transparent, radius = r / 2.2f, center = Offset(center.x + 8f, center.y - 8f))
        }
    }
}
