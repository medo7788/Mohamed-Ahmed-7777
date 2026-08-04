package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.CustomThemeColors
import com.example.util.AppLocationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

// ==========================================
// 🎨 CYBER OBSIDIAN LUXURY DESIGN SYSTEM
// ==========================================
private val ObsidianBgTop = Color(0xFF080A0F)
private val ObsidianBgBottom = Color(0xFF121620)
private val GlassSurface = Color(0xFF141926).copy(alpha = 0.85f)
private val GlassBorder = Color(0xFFD4AF37).copy(alpha = 0.30f)
private val ChampagneGold = Color(0xFFD4AF37)
private val AmberGold = Color(0xFFF59E0B)
private val IceCyan = Color(0xFF00F2FE)
private val EmeraldGreen = Color(0xFF10B981)
private val SlateMuted = Color(0xFF94A3B8)
private val ErrorRed = Color(0xFFEF4444)

// ==========================================
// 🏛️ UI STATES & DATA MODELS
// ==========================================
sealed class QiblaUiState {
    object Loading : QiblaUiState()
    data class Success(
        val lat: Double,
        val lng: Double,
        val cityName: String?,
        val qiblaAngle: Float,
        val distanceKm: Double,
        val isOffline: Boolean = false,
        val lastUpdatedText: String? = null
    ) : QiblaUiState()
    object LocationDisabled : QiblaUiState()
    data class Error(val message: String) : QiblaUiState()
}

data class WorldCity(
    val nameAr: String,
    val countryAr: String,
    val lat: Double,
    val lng: Double
)

val popularWorldCities = listOf(
    WorldCity("مكة المكرمة", "السعودية", 21.422487, 39.826206),
    WorldCity("المدينة المنورة", "السعودية", 24.4672, 39.6112),
    WorldCity("الرياض", "السعودية", 24.7136, 46.6753),
    WorldCity("جدة", "السعودية", 21.5433, 39.1728),
    WorldCity("القاهرة", "مصر", 30.0444, 31.2357),
    WorldCity("الإسكندرية", "مصر", 31.2001, 29.9187),
    WorldCity("القدس الشريف", "فلسطين", 31.7683, 35.2137),
    WorldCity("عمان", "الأردن", 31.9454, 35.9284),
    WorldCity("دبي", "الإمارات", 25.2048, 55.2708),
    WorldCity("أبوظبي", "الإمارات", 24.4539, 54.3773),
    WorldCity("الكويت", "الكويت", 29.3759, 47.9774),
    WorldCity("الدوحة", "قطر", 25.2854, 51.5310),
    WorldCity("مسقط", "عمان", 23.5880, 58.3829),
    WorldCity("المنامة", "البحرين", 26.2285, 50.5860),
    WorldCity("بغداد", "العراق", 33.3152, 44.3661),
    WorldCity("دمشق", "سوريا", 33.5138, 36.2765),
    WorldCity("بيروت", "لبنان", 33.8938, 35.5018),
    WorldCity("تونس", "تونس", 36.8065, 10.1815),
    WorldCity("الجزائر", "الجزائر", 36.7538, 3.0588),
    WorldCity("الرباط", "المغرب", 34.0209, -6.8416),
    WorldCity("إسطنبول", "تركيا", 41.0082, 28.9784),
    WorldCity("كوالالمبور", "ماليزيا", 3.1390, 101.6869),
    WorldCity("جاكرتا", "إندونيسيا", -6.2088, 106.8456),
    WorldCity("لندن", "المملكة المتحدة", 51.5074, -0.1278),
    WorldCity("باريس", "فرنسا", 48.8566, 2.3522),
    WorldCity("برلين", "ألمانيا", 52.5200, 13.4050),
    WorldCity("نيويورك", "أمريكا", 40.7128, -74.0060),
    WorldCity("تورونتو", "كندا", 43.6532, -79.3832),
    WorldCity("طوكيو", "اليابان", 35.6762, 139.6503),
    WorldCity("سيدني", "أستراليا", -33.8688, 151.2093)
)

// ==========================================
// 📐 MATHEMATICAL FORMULAS
// ==========================================
fun calculateQiblaAzimuth(lat: Double, lng: Double): Float {
    val kaabaLat = 21.422487
    val kaabaLng = 39.826206
    val phi1 = Math.toRadians(lat)
    val phi2 = Math.toRadians(kaabaLat)
    val deltaLambda = Math.toRadians(kaabaLng - lng)

    val y = sin(deltaLambda) * cos(phi2)
    val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
    var bearing = Math.toDegrees(atan2(y, x)).toFloat()
    bearing = (bearing + 360f) % 360f
    return bearing
}

fun calculateDistanceToKaaba(lat: Double, lng: Double): Double {
    val r = 6371.0 // Earth radius in kilometers
    val kaabaLat = 21.422487
    val kaabaLng = 39.826206
    val dLat = Math.toRadians(kaabaLat - lat)
    val dLng = Math.toRadians(kaabaLng - lng)
    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat)) * cos(Math.toRadians(kaabaLat)) * sin(dLng / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
}

fun getDeclination(lat: Double, lng: Double): Float {
    return try {
        val geoField = GeomagneticField(
            lat.toFloat(),
            lng.toFloat(),
            0f,
            System.currentTimeMillis()
        )
        geoField.declination
    } catch (_: Exception) {
        0f
    }
}

fun formatDirectionArabic(degrees: Float): String {
    val deg = (degrees % 360f + 360f) % 360f
    return when {
        deg >= 337.5 || deg < 22.5 -> "${deg.toInt()}° شمال"
        deg >= 22.5 && deg < 67.5 -> "${deg.toInt()}° شمال شرق"
        deg >= 67.5 && deg < 112.5 -> "${deg.toInt()}° شرق"
        deg >= 112.5 && deg < 157.5 -> "${deg.toInt()}° جنوب شرق"
        deg >= 157.5 && deg < 202.5 -> "${deg.toInt()}° جنوب"
        deg >= 202.5 && deg < 247.5 -> "${deg.toInt()}° جنوب غرب"
        deg >= 247.5 && deg < 292.5 -> "${deg.toInt()}° غرب"
        else -> "${deg.toInt()}° شمال غرب"
    }
}

// Low-Pass Filter smoothing function
fun smoothAngle(newAngle: Float, currentSmoothed: Float, alpha: Float = 0.15f): Float {
    var diff = (newAngle - currentSmoothed) % 360f
    if (diff > 180f) diff -= 360f
    if (diff < -180f) diff += 360f
    return (currentSmoothed + alpha * diff + 360f) % 360f
}

// ==========================================
// 🚀 MAIN QIBLA COMPASS SCREEN REDESIGN
// ==========================================
data class QiblaHistoryItem(
    val cityName: String,
    val lat: Double,
    val lng: Double,
    val qiblaAngle: Float,
    val distanceKm: Double,
    val timestampText: String
)

@Composable
fun QiblaDirectionScreenRedesign(
    colors: CustomThemeColors,
    onBackClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var uiState by remember { mutableStateOf<QiblaUiState>(QiblaUiState.Loading) }
    var rawAzimuth by remember { mutableFloatStateOf(0f) }
    var smoothedAzimuth by remember { mutableFloatStateOf(0f) }
    var sensorAccuracy by remember { mutableIntStateOf(SensorManager.SENSOR_STATUS_ACCURACY_HIGH) }
    var showCalibrationDialog by remember { mutableStateOf(false) }
    var showManualLocationPicker by remember { mutableStateOf(false) }
    
    // Sensor Fusion & Telemetry Details
    var rotationVectorValues by remember { mutableStateOf(floatArrayOf(0f, 0f, 0f)) }
    var quaternionValues by remember { mutableStateOf(floatArrayOf(1f, 0f, 0f, 0f)) }
    var magneticFieldTesla by remember { mutableFloatStateOf(45.0f) }
    var isMagneticInterference by remember { mutableStateOf(false) }
    var showDebugOverlay by remember { mutableStateOf(false) }
    var showHistoryDrawer by remember { mutableStateOf(false) }
    var isUsingRotationVector by remember { mutableStateOf(false) }

    // Saved Locations History List
    var historyList by remember {
        mutableStateOf(
            listOf(
                QiblaHistoryItem("مكة المكرمة", 21.422487, 39.826206, 0f, 0.0, "الآن"),
                QiblaHistoryItem("القاهرة", 30.0444, 31.2357, 136f, 1262.0, "سابقًا")
            )
        )
    }

    // Manual Rotation Control State
    var isManualRotationMode by remember { mutableStateOf(false) }
    var manualRotationDegree by remember { mutableFloatStateOf(0f) }

    var isRefreshing by remember { mutableStateOf(false) }
    var errorShakeTrigger by remember { mutableIntStateOf(0) }

    // Haptic Trigger Helper
    fun triggerVibration(patternType: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vibratorManager.defaultVibrator
                if (vibrator.hasVibrator()) {
                    val effect = when (patternType) {
                        "ALIGNMENT" -> VibrationEffect.createWaveform(longArrayOf(0, 40, 60, 40), intArrayOf(0, 255, 0, 255), -1)
                        "ERROR" -> VibrationEffect.createWaveform(longArrayOf(0, 50, 100, 50), intArrayOf(0, 180, 0, 180), -1)
                        else -> VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
                    }
                    vibrator.vibrate(effect)
                }
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (vibrator.hasVibrator()) {
                    when (patternType) {
                        "ALIGNMENT" -> vibrator.vibrate(longArrayOf(0, 40, 60, 40), -1)
                        "ERROR" -> vibrator.vibrate(longArrayOf(0, 50, 100, 50), -1)
                        else -> vibrator.vibrate(30)
                    }
                }
            }
        } catch (_: Exception) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    // Save Location to History
    fun saveCurrentLocationToHistory() {
        val success = uiState as? QiblaUiState.Success ?: return
        val cityName = success.cityName ?: "موقع محدد"
        val nowText = SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale("ar")).format(Date())
        val item = QiblaHistoryItem(
            cityName = cityName,
            lat = success.lat,
            lng = success.lng,
            qiblaAngle = success.qiblaAngle,
            distanceKm = success.distanceKm,
            timestampText = nowText
        )
        if (!historyList.any { it.cityName == item.cityName }) {
            historyList = listOf(item) + historyList.take(9)
        }
        triggerVibration("CLICK")
    }

    // Share Qibla Info Intent
    fun shareQiblaInfo() {
        val success = uiState as? QiblaUiState.Success ?: return
        val city = success.cityName ?: "موقعي"
        val angle = success.qiblaAngle
        val dist = success.distanceKm.toInt()
        val dirText = formatDirectionArabic(angle)
        val text = "🕋 اتجاه القبلة من ($city):\n" +
                "• الزاوية: ${angle.toInt()}° ($dirText)\n" +
                "• المسافة إلى مكة المكرمة: %,d كم\n".format(Locale.getDefault(), dist) +
                "تم الحساب بدقة عالية باستخدام تطبيق بوصلة القبلة."
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        try {
            context.startActivity(Intent.createChooser(intent, "مشاركة معلومات القبلة"))
        } catch (_: Exception) {}
    }

    // Apply selected manual city or coordinate location
    fun applyManualLocation(lat: Double, lng: Double, cityName: String) {
        val qibla = calculateQiblaAzimuth(lat, lng)
        val dist = calculateDistanceToKaaba(lat, lng)
        AppLocationProvider.saveLocationToCache(context, lat, lng, cityName)
        val nowText = SimpleDateFormat("hh:mm a", Locale("ar")).format(Date())
        uiState = QiblaUiState.Success(
            lat = lat,
            lng = lng,
            cityName = cityName,
            qiblaAngle = qibla,
            distanceKm = dist,
            isOffline = true,
            lastUpdatedText = "موقع يدوي ($nowText)"
        )
        triggerVibration("CLICK")
    }

    // Geocoding Helper
    suspend fun resolveCityName(lat: Double, lng: Double): String? {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale("ar"))
                @Suppress("DEPRECATION")
                val list = geocoder.getFromLocation(lat, lng, 1)
                if (!list.isNullOrEmpty()) {
                    val addr = list[0]
                    val city = addr.locality ?: addr.subAdminArea ?: addr.adminArea
                    val country = addr.countryName
                    if (city != null && country != null && city != country) "$city، $country" else city ?: country
                } else null
            } catch (_: Exception) {
                null
            }
        }
    }

    // Location Loader Logic
    fun loadLocation(isUserRefresh: Boolean = false) {
        if (isUserRefresh) isRefreshing = true else uiState = QiblaUiState.Loading

        scope.launch {
            if (!AppLocationProvider.hasLocationPermission(context)) {
                // Try cache first
                val cached = AppLocationProvider.getCachedLocation(context)
                if (cached != null) {
                    val qibla = calculateQiblaAzimuth(cached.lat, cached.lng)
                    val dist = calculateDistanceToKaaba(cached.lat, cached.lng)
                    val nowText = SimpleDateFormat("hh:mm a", Locale("ar")).format(Date())
                    uiState = QiblaUiState.Success(
                        lat = cached.lat,
                        lng = cached.lng,
                        cityName = cached.placeName ?: "الموقع المخزن",
                        qiblaAngle = qibla,
                        distanceKm = dist,
                        isOffline = true,
                        lastUpdatedText = nowText
                    )
                } else {
                    uiState = QiblaUiState.LocationDisabled
                }
                isRefreshing = false
                return@launch
            }

            val result = AppLocationProvider.fetchCurrentLocation(context)
            when (result) {
                is AppLocationProvider.Result.Success -> {
                    val qibla = calculateQiblaAzimuth(result.latitude, result.longitude)
                    val dist = calculateDistanceToKaaba(result.latitude, result.longitude)
                    val city = resolveCityName(result.latitude, result.longitude)
                    AppLocationProvider.saveLocationToCache(context, result.latitude, result.longitude, city)
                    val nowText = SimpleDateFormat("hh:mm a", Locale("ar")).format(Date())

                    uiState = QiblaUiState.Success(
                        lat = result.latitude,
                        lng = result.longitude,
                        cityName = city ?: "موقعك الحالي",
                        qiblaAngle = qibla,
                        distanceKm = dist,
                        isOffline = false,
                        lastUpdatedText = nowText
                    )
                }
                is AppLocationProvider.Result.LocationDisabled,
                is AppLocationProvider.Result.PermissionDenied,
                is AppLocationProvider.Result.Timeout,
                is AppLocationProvider.Result.Error -> {
                    val cached = AppLocationProvider.getCachedLocation(context)
                    if (cached != null) {
                        val qibla = calculateQiblaAzimuth(cached.lat, cached.lng)
                        val dist = calculateDistanceToKaaba(cached.lat, cached.lng)
                        val nowText = SimpleDateFormat("hh:mm a", Locale("ar")).format(Date())
                        uiState = QiblaUiState.Success(
                            lat = cached.lat,
                            lng = cached.lng,
                            cityName = cached.placeName ?: "الموقع المخزن",
                            qiblaAngle = qibla,
                            distanceKm = dist,
                            isOffline = true,
                            lastUpdatedText = nowText
                        )
                    } else {
                        // Default fallback to Cairo, Egypt so compass always works offline
                        applyManualLocation(30.0444, 31.2357, "القاهرة (افتراضي)")
                    }
                }
            }
            isRefreshing = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            loadLocation(isUserRefresh = false)
        } else {
            val cached = AppLocationProvider.getCachedLocation(context)
            if (cached != null) {
                val qibla = calculateQiblaAzimuth(cached.lat, cached.lng)
                val dist = calculateDistanceToKaaba(cached.lat, cached.lng)
                uiState = QiblaUiState.Success(
                    lat = cached.lat,
                    lng = cached.lng,
                    cityName = cached.placeName ?: "الموقع المخزن",
                    qiblaAngle = qibla,
                    distanceKm = dist,
                    isOffline = true
                )
            } else {
                applyManualLocation(30.0444, 31.2357, "القاهرة (افتراضي)")
            }
        }
    }

    LaunchedEffect(Unit) {
        loadLocation(isUserRefresh = false)
    }

    // 🧲 COMPASS SENSOR REGISTRATION
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }

    DisposableEffect(isManualRotationMode) {
        if (isManualRotationMode) {
            onDispose { }
        } else {
            var activeListener: SensorEventListener? = null
            val rotVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

            if (rotVectorSensor != null) {
                val listener = object : SensorEventListener {
                    private val rMat = FloatArray(9)
                    private val orient = FloatArray(3)

                    override fun onSensorChanged(event: SensorEvent?) {
                        if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
                            try {
                                val vals = event.values
                                if (vals.size >= 3) {
                                    rotationVectorValues = floatArrayOf(vals[0], vals[1], vals[2])
                                    val w = if (vals.size >= 4) vals[3] else sqrt(max(0f, 1f - vals[0]*vals[0] - vals[1]*vals[1] - vals[2]*vals[2]))
                                    quaternionValues = floatArrayOf(w, vals[0], vals[1], vals[2])
                                }
                                isUsingRotationVector = true
                                SensorManager.getRotationMatrixFromVector(rMat, event.values)
                                SensorManager.getOrientation(rMat, orient)
                                val azDeg = Math.toDegrees(orient[0].toDouble()).toFloat()
                                val positiveAzimuth = (azDeg + 360f) % 360f
                                rawAzimuth = positiveAzimuth
                                smoothedAzimuth = smoothAngle(positiveAzimuth, smoothedAzimuth, alpha = 0.05f)
                            } catch (_: Exception) {}
                        }
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                        sensorAccuracy = accuracy
                    }
                }
                activeListener = listener
                sensorManager.registerListener(listener, rotVectorSensor, SensorManager.SENSOR_DELAY_GAME)
            } else {
                val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                val magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

                if (accelSensor != null && magSensor != null) {
                    val fallbackListener = object : SensorEventListener {
                        private val lastAccel = FloatArray(3)
                        private val lastMag = FloatArray(3)
                        private var hasAccel = false
                        private var hasMag = false
                        private val rMat = FloatArray(9)
                        private val orient = FloatArray(3)

                        override fun onSensorChanged(event: SensorEvent?) {
                            event ?: return
                            isUsingRotationVector = false
                            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                                System.arraycopy(event.values, 0, lastAccel, 0, 3)
                                hasAccel = true
                            } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                                System.arraycopy(event.values, 0, lastMag, 0, 3)
                                hasMag = true
                                val mx = event.values[0]
                                val my = event.values[1]
                                val mz = event.values[2]
                                val norm = sqrt(mx*mx + my*my + mz*mz)
                                magneticFieldTesla = norm
                                isMagneticInterference = norm < 20f || norm > 70f || sensorAccuracy == SensorManager.SENSOR_STATUS_UNRELIABLE
                            }
                            if (hasAccel && hasMag) {
                                if (SensorManager.getRotationMatrix(rMat, null, lastAccel, lastMag)) {
                                    SensorManager.getOrientation(rMat, orient)
                                    val azDeg = Math.toDegrees(orient[0].toDouble()).toFloat()
                                    val positiveAzimuth = (azDeg + 360f) % 360f
                                    rawAzimuth = positiveAzimuth
                                    smoothedAzimuth = smoothAngle(positiveAzimuth, smoothedAzimuth, alpha = 0.05f)
                                }
                            }
                        }

                        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                            if (sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
                                sensorAccuracy = accuracy
                            }
                        }
                    }
                    activeListener = fallbackListener
                    sensorManager.registerListener(fallbackListener, accelSensor, SensorManager.SENSOR_DELAY_GAME)
                    sensorManager.registerListener(fallbackListener, magSensor, SensorManager.SENSOR_DELAY_GAME)
                }
            }

            onDispose {
                if (activeListener != null) {
                    sensorManager.unregisterListener(activeListener)
                }
            }
        }
    }

    // Geomagnetic declination for True North adjustment
    val declination = remember(uiState) {
        val successState = uiState as? QiblaUiState.Success
        if (successState != null) {
            getDeclination(successState.lat, successState.lng)
        } else 0f
    }

    // Effective heading used for UI rendering (with Geomagnetic Declination for True North)
    val currentHeading = if (isManualRotationMode) {
        manualRotationDegree
    } else {
        if (isUsingRotationVector) smoothedAzimuth
        else (smoothedAzimuth + declination + 360f) % 360f
    }

    // 🕋 COMPASS ROTATION & ALIGNMENT LOCK LOGIC
    val qiblaAngle = (uiState as? QiblaUiState.Success)?.qiblaAngle ?: 0f

    val angleDiff = remember(currentHeading, qiblaAngle) {
        val diff = abs(currentHeading - qiblaAngle) % 360f
        if (diff > 180f) 360f - diff else diff
    }

    val isAligned = angleDiff <= 2.0f && uiState is QiblaUiState.Success

    // Alignment Haptic Side-Effect
    var wasAligned by remember { mutableStateOf(false) }
    LaunchedEffect(isAligned) {
        if (isAligned && !wasAligned) {
            triggerVibration("ALIGNMENT")
        }
        wasAligned = isAligned
    }

    // Dynamic Dial Rotation Animation
    val animatedDialRotation by animateFloatAsState(
        targetValue = -currentHeading,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "compassDialRotation"
    )

    // Glowing Pulse for Alignment Lock
    val infiniteTransition = rememberInfiniteTransition(label = "alignmentPulse")
    val auraGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auraGlow"
    )

    // 🌌 MAIN OBSIDIAN CANVAS WRAPPER
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(ObsidianBgTop, ObsidianBgBottom)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Procedural Astrolabe Concentric Ring Background Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * 0.40f)
            val maxR = size.width * 0.75f

            // Astrolabe Concentric Circles
            for (r in listOf(maxR * 0.3f, maxR * 0.55f, maxR * 0.8f, maxR * 1.05f)) {
                drawCircle(
                    color = ChampagneGold.copy(alpha = 0.05f),
                    radius = r,
                    center = center,
                    style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
                )
            }

            // Radial Ray Grid
            for (angleDeg in 0 until 360 step 45) {
                val rad = Math.toRadians(angleDeg.toDouble())
                val end = Offset(
                    center.x + (maxR * 1.1f) * cos(rad).toFloat(),
                    center.y + (maxR * 1.1f) * sin(rad).toFloat()
                )
                drawLine(
                    color = IceCyan.copy(alpha = 0.03f),
                    start = center,
                    end = end,
                    strokeWidth = 1.dp.toPx()
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ----------------------------------------
            // 🔹 TOP APP BAR / HEADER
            // ----------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (onBackClick != null) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(GlassSurface)
                            .border(1.dp, GlassBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "رجوع",
                            tint = Color.White
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(42.dp))
                }

                Text(
                    text = "بوصلة القبلة الاحترافية",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )

                // Refresh Location Button
                IconButton(
                    onClick = { loadLocation(isUserRefresh = true) },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(GlassSurface)
                        .border(1.dp, GlassBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "تحديث الموقع",
                        tint = ChampagneGold,
                        modifier = Modifier.graphicsLayer(rotationZ = if (isRefreshing) 180f else 0f)
                    )
                }
            }

            // ----------------------------------------
            // 🔹 SECTION A: HEADER & LOCATION CARD (WITH CHANGE LOCATION BUTTON)
            // ----------------------------------------
            HeaderLocationAndSensorBadge(
                uiState = uiState,
                sensorAccuracy = sensorAccuracy,
                onCalibrateClick = { showCalibrationDialog = true },
                onChangeLocationClick = { showManualLocationPicker = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ----------------------------------------
            // 🔹 SECTION B: MAIN SCREEN CONTENT BASED ON STATE
            // ----------------------------------------
            when (val state = uiState) {
                is QiblaUiState.Loading -> {
                    QiblaSkeletonLoader()
                }

                is QiblaUiState.Success -> {
                    // Magnetic Field Interference Alert Banner
                    AnimatedVisibility(visible = isMagneticInterference) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = GlassSurface,
                            border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "تداخل مغناطيسي محلي قد يؤثر على دقة القبلة. اضغط لمعايرة البوصلة.",
                                        fontSize = 11.sp,
                                        color = Color.White
                                    )
                                }
                                TextButton(
                                    onClick = { showCalibrationDialog = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text("معايرة", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AmberGold)
                                }
                            }
                        }
                    }

                    // Full Interactive Luxury Compass Dial (Triple-tap on dial toggles Debug Overlay)
                    Box(
                        modifier = Modifier.clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) {
                            showDebugOverlay = !showDebugOverlay
                        }
                    ) {
                        QiblaCompassDial(
                            dialRotation = animatedDialRotation,
                            qiblaAngle = state.qiblaAngle,
                            isAligned = isAligned,
                            auraAlpha = auraGlowAlpha,
                            smoothedAzimuth = currentHeading
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ----------------------------------------
                    // 🔹 SECTION C: MANUAL ROTATION / ROTATION SIMULATION SLIDER
                    // ----------------------------------------
                    ManualCompassRotationControl(
                        isManualMode = isManualRotationMode,
                        currentDegree = manualRotationDegree,
                        qiblaAngle = state.qiblaAngle,
                        onModeToggle = { enabled ->
                            isManualRotationMode = enabled
                            if (enabled) {
                                manualRotationDegree = smoothedAzimuth
                            }
                        },
                        onDegreeChange = { newDegree ->
                            manualRotationDegree = newDegree
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // ----------------------------------------
                    // 🔹 SECTION D: REAL-TIME TELEMETRY GLASS CARD
                    // ----------------------------------------
                    QiblaTelemetryCard(
                        azimuth = currentHeading,
                        qiblaAngle = state.qiblaAngle,
                        declination = declination,
                        distanceKm = state.distanceKm,
                        sensorAccuracy = sensorAccuracy,
                        isAligned = isAligned,
                        onCalibrateClick = { showCalibrationDialog = true },
                        onSaveHistoryClick = { saveCurrentLocationToHistory() },
                        onShareClick = { shareQiblaInfo() },
                        onToggleHistoryClick = { showHistoryDrawer = !showHistoryDrawer },
                        onRefreshLocationClick = { loadLocation(isUserRefresh = true) }
                    )

                    // Expandable Qibla Location History Log Drawer
                    AnimatedVisibility(visible = showHistoryDrawer) {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))
                            QiblaHistoryCard(
                                historyList = historyList,
                                onSelectHistoryItem = { lat, lng, cityName ->
                                    applyManualLocation(lat, lng, cityName)
                                },
                                onClearHistory = { historyList = emptyList() }
                            )
                        }
                    }

                    // Floating Debug Overlay Telemetry
                    AnimatedVisibility(visible = showDebugOverlay) {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))
                            QiblaDebugOverlay(
                                rotationVector = rotationVectorValues,
                                quaternion = quaternionValues,
                                magHeading = rawAzimuth,
                                trueHeading = currentHeading,
                                declination = declination,
                                qiblaBearing = state.qiblaAngle,
                                relativeAngle = (state.qiblaAngle - currentHeading + 360f) % 360f,
                                sensorAccuracy = sensorAccuracy,
                                magFieldTesla = magneticFieldTesla,
                                isMagneticInterference = isMagneticInterference,
                                onClose = { showDebugOverlay = false }
                            )
                        }
                    }
                }

                is QiblaUiState.LocationDisabled -> {
                    LocationDisabledStateView(
                        onEnableLocationClick = {
                            try {
                                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                            } catch (_: Exception) {}
                        },
                        onRequestPermissionClick = {
                            permissionLauncher.launch(
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                            )
                        },
                        onSelectManualCityClick = {
                            showManualLocationPicker = true
                        }
                    )
                }

                is QiblaUiState.Error -> {
                    ErrorStateView(
                        errorMessage = state.message,
                        shakeTrigger = errorShakeTrigger,
                        onRetryClick = { loadLocation(isUserRefresh = false) },
                        onSelectManualCityClick = { showManualLocationPicker = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // ----------------------------------------
        // 🔹 MANUAL LOCATION / CITY PICKER DIALOG
        // ----------------------------------------
        if (showManualLocationPicker) {
            ManualLocationPickerDialog(
                currentCityName = (uiState as? QiblaUiState.Success)?.cityName,
                onDismiss = { showManualLocationPicker = false },
                onSelectCity = { lat, lng, cityName ->
                    applyManualLocation(lat, lng, cityName)
                    showManualLocationPicker = false
                },
                onRequestGpsLocation = {
                    showManualLocationPicker = false
                    loadLocation(isUserRefresh = true)
                }
            )
        }

        // ----------------------------------------
        // 🔹 SENSOR CALIBRATION HELPER DIALOG
        // ----------------------------------------
        if (showCalibrationDialog) {
            SensorCalibrationDialog(
                onDismiss = { showCalibrationDialog = false }
            )
        }
    }
}

// ==========================================
// 📍 HEADER LOCATION & ACCURACY BADGE
// ==========================================
@Composable
private fun HeaderLocationAndSensorBadge(
    uiState: QiblaUiState,
    sensorAccuracy: Int,
    onCalibrateClick: () -> Unit,
    onChangeLocationClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Location Glass Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = GlassSurface,
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            Column(
                modifier = Modifier.padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(AmberGold.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "الموقع",
                                tint = AmberGold,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            val locationName = when (uiState) {
                                is QiblaUiState.Success -> uiState.cityName ?: "موقعك الحالي"
                                is QiblaUiState.Loading -> "جاري تحديد الموقع..."
                                is QiblaUiState.LocationDisabled -> "الموقع غير مفعل"
                                is QiblaUiState.Error -> "عذرًا، يتعذر جلب الموقع"
                            }
                            Text(
                                text = locationName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            if (uiState is QiblaUiState.Success && uiState.isOffline) {
                                Text(
                                    text = uiState.lastUpdatedText ?: "موقع مخزن أو يدوي",
                                    fontSize = 11.sp,
                                    color = SlateMuted
                                )
                            } else {
                                Text(
                                    text = "حساب دقيق حسب إحداثيات الموقع",
                                    fontSize = 11.sp,
                                    color = SlateMuted
                                )
                            }
                        }
                    }

                    // Button: "تغيير المكان يدويًا"
                    Surface(
                        onClick = onChangeLocationClick,
                        shape = RoundedCornerShape(12.dp),
                        color = ChampagneGold.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.EditLocation,
                                contentDescription = "تغيير المكان",
                                tint = ChampagneGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "تغيير المكان",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ChampagneGold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Sensor Accuracy Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.3f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val (accuracyText, accuracyColor) = when (sensorAccuracy) {
                        SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "دقة البوصلة: عالية 🟢" to EmeraldGreen
                        SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "دقة البوصلة: متوسطة 🟡" to AmberGold
                        else -> "دقة البوصلة: يحتاج معايرة 🔴" to ErrorRed
                    }

                    Text(
                        text = accuracyText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = accuracyColor
                    )

                    Row(
                        modifier = Modifier.clickable { onCalibrateClick() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "معايرة البوصلة",
                            tint = IceCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "كيفية المعايرة",
                            fontSize = 11.sp,
                            color = IceCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 🧭 MANUAL COMPASS ROTATION CONTROL
// ==========================================
@Composable
private fun ManualCompassRotationControl(
    isManualMode: Boolean,
    currentDegree: Float,
    qiblaAngle: Float,
    onModeToggle: (Boolean) -> Unit,
    onDegreeChange: (Float) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = GlassSurface,
        border = BorderStroke(1.dp, if (isManualMode) AmberGold.copy(alpha = 0.5f) else GlassBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = null,
                        tint = if (isManualMode) AmberGold else IceCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تدوير البوصلة يدوياً (المحاكاة)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Switch(
                    checked = isManualMode,
                    onCheckedChange = onModeToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ObsidianBgTop,
                        checkedTrackColor = AmberGold,
                        uncheckedThumbColor = SlateMuted,
                        uncheckedTrackColor = Color.Black.copy(alpha = 0.4f)
                    )
                )
            }

            if (isManualMode) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "حرّك المؤشر أدناه لتدوير الهاتف واختبار اتجاهات القبلة:",
                    fontSize = 11.sp,
                    color = SlateMuted
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${currentDegree.toInt()}°",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberGold,
                        modifier = Modifier.width(40.dp)
                    )
                    Slider(
                        value = currentDegree,
                        onValueChange = onDegreeChange,
                        valueRange = 0f..360f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = AmberGold,
                            activeTrackColor = AmberGold,
                            inactiveTrackColor = SlateMuted.copy(alpha = 0.3f)
                        )
                    )
                }

                // Quick Direction Snap Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(
                        "شمال 0°" to 0f,
                        "شرق 90°" to 90f,
                        "جنوب 180°" to 180f,
                        "غرب 270°" to 270f,
                        "🎯 القبلة (${qiblaAngle.toInt()}°)" to qiblaAngle
                    ).forEach { (label, degree) ->
                        Surface(
                            onClick = { onDegreeChange(degree) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (label.startsWith("🎯")) AmberGold.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (label.startsWith("🎯")) AmberGold else GlassBorder
                            )
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (label.startsWith("🎯")) AmberGold else Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 🧭 COMPASS DIAL (2D / CANVAS PROCEDURAL DRAWING)
// ==========================================
@Composable
private fun QiblaCompassDial(
    dialRotation: Float,
    qiblaAngle: Float,
    isAligned: Boolean,
    auraAlpha: Float,
    smoothedAzimuth: Float
) {
    Box(
        modifier = Modifier
            .size(310.dp)
            .semantics {
                contentDescription = "بوصلة القبلة، اتجاه الجوال الحالي ${smoothedAzimuth.toInt()} درجة، زاوية القبلة ${qiblaAngle.toInt()} درجة"
            },
        contentAlignment = Alignment.Center
    ) {
        // Alignment Glow Outer Ring Aura
        if (isAligned) {
            Box(
                modifier = Modifier
                    .size(325.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                AmberGold.copy(alpha = 0.45f * auraAlpha),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // Main Glass Dial Vessel
        Box(
            modifier = Modifier
                .size(290.dp)
                .clip(CircleShape)
                .background(GlassSurface)
                .border(
                    width = if (isAligned) 3.dp else 1.5.dp,
                    color = if (isAligned) AmberGold else GlassBorder,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Rotating Dial Layer (Cardinal Marks & Kaaba Indicator)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(dialRotation),
                contentAlignment = Alignment.Center
            ) {
                // Procedural Canvas Tick Marks
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = size.width / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)

                    for (i in 0 until 360 step 5) {
                        val angleRad = Math.toRadians(i.toDouble() - 90.0)
                        val isMainTick = i % 90 == 0
                        val isMediumTick = i % 15 == 0

                        val tickLength = when {
                            isMainTick -> 16.dp.toPx()
                            isMediumTick -> 10.dp.toPx()
                            else -> 5.dp.toPx()
                        }

                        val strokeW = when {
                            isMainTick -> 3.dp.toPx()
                            isMediumTick -> 1.5.dp.toPx()
                            else -> 1.dp.toPx()
                        }

                        val color = when {
                            isMainTick -> IceCyan
                            isMediumTick -> ChampagneGold.copy(alpha = 0.7f)
                            else -> SlateMuted.copy(alpha = 0.3f)
                        }

                        val start = Offset(
                            center.x + (radius - tickLength - 8.dp.toPx()) * cos(angleRad).toFloat(),
                            center.y + (radius - tickLength - 8.dp.toPx()) * sin(angleRad).toFloat()
                        )
                        val end = Offset(
                            center.x + (radius - 8.dp.toPx()) * cos(angleRad).toFloat(),
                            center.y + (radius - 8.dp.toPx()) * sin(angleRad).toFloat()
                        )

                        drawLine(
                            color = color,
                            start = start,
                            end = end,
                            strokeWidth = strokeW,
                            cap = StrokeCap.Round
                        )
                    }
                }

                // Glowing Ice Cyan Cardinal Direction Labels (ABSOLUTE POSITIONS - RTL SAFE)
                Box(modifier = Modifier.fillMaxSize()) {
                    // North (ش) - 0°
                    Text(
                        text = "ش",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = IceCyan,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 26.dp)
                    )
                    // East (ق) - 90°
                    Text(
                        text = "ق",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = IceCyan.copy(alpha = 0.85f),
                        modifier = Modifier
                            .align(AbsoluteAlignment.CenterRight)
                            .absolutePadding(right = 26.dp)
                    )
                    // South (ج) - 180°
                    Text(
                        text = "ج",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = IceCyan.copy(alpha = 0.85f),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 26.dp)
                    )
                    // West (غ) - 270°
                    Text(
                        text = "غ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = IceCyan.copy(alpha = 0.85f),
                        modifier = Modifier
                            .align(AbsoluteAlignment.CenterLeft)
                            .absolutePadding(left = 26.dp)
                    )
                }

                // 🕋 KAABA ICON POINTER ON DIAL AT qiblaAngle
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(qiblaAngle),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 40.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(48.dp)
                                .graphicsLayer(
                                    shadowElevation = if (isAligned) 16f else 6f
                                ),
                            shape = CircleShape,
                            color = if (isAligned) AmberGold else Color(0xFF1E2638),
                            border = BorderStroke(2.dp, if (isAligned) Color.White else ChampagneGold)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "🕋",
                                    fontSize = 22.sp
                                )
                            }
                        }

                        // Gold Gradient Direction Pointer Beam
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(50.dp)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(if (isAligned) AmberGold else ChampagneGold, Color.Transparent)
                                    )
                                )
                        )
                    }
                }
            }

            // Fixed Phone Heading Pointer (Top Arrow)
            Icon(
                imageVector = Icons.Default.ArrowDropUp,
                contentDescription = "اتجاه أعلى الجوال",
                tint = if (isAligned) AmberGold else IceCyan,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-4).dp)
                    .size(36.dp)
            )

            // Center Astrolabe Pivot Dot
            Surface(
                modifier = Modifier.size(16.dp),
                shape = CircleShape,
                color = if (isAligned) AmberGold else ChampagneGold,
                border = BorderStroke(2.dp, ObsidianBgTop)
            ) {}
        }
    }
}

// ==========================================
// 📊 REAL-TIME TELEMETRY DASHBOARD CARD
// ==========================================
@Composable
private fun QiblaTelemetryCard(
    azimuth: Float,
    qiblaAngle: Float,
    declination: Float,
    distanceKm: Double,
    sensorAccuracy: Int,
    isAligned: Boolean,
    onCalibrateClick: () -> Unit,
    onSaveHistoryClick: () -> Unit,
    onShareClick: () -> Unit,
    onToggleHistoryClick: () -> Unit,
    onRefreshLocationClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = GlassSurface,
        border = BorderStroke(1.dp, if (isAligned) AmberGold.copy(alpha = 0.6f) else GlassBorder)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Active Alignment Lock Banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "بيانات الاستشعار",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                IconButton(onClick = onRefreshLocationClick, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "تحديث الموقع", tint = IceCyan)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            AnimatedVisibility(
                visible = isAligned,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(AmberGold.copy(alpha = 0.2f))
                        .border(1.dp, AmberGold, RoundedCornerShape(14.dp))
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🕋 أنت الآن باتجاه القبلة المشرفة تمامًا!",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberGold
                    )
                }
            }

            // 4-Column Telemetry Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Metric 1: Current Heading
                TelemetryMetricItem(
                    label = "الاتجاه الحالي",
                    value = "${azimuth.toInt()}°",
                    subtitle = formatDirectionArabic(azimuth),
                    valueColor = IceCyan
                )

                // Metric 2: Qibla Bearing
                TelemetryMetricItem(
                    label = "اتجاه القبلة",
                    value = "${qiblaAngle.toInt()}°",
                    subtitle = formatDirectionArabic(qiblaAngle),
                    valueColor = ChampagneGold
                )

                // Metric 3: Distance to Makkah
                val formattedDistance = String.format(Locale.getDefault(), "%,d كم", distanceKm.toInt())
                TelemetryMetricItem(
                    label = "المسافة لـ مكة",
                    value = formattedDistance,
                    subtitle = "خط مباشر",
                    valueColor = ChampagneGold
                )

                // Metric 4: Declination
                val declText = if (declination >= 0) "+%.1f°".format(Locale.US, declination) else "%.1f°".format(Locale.US, declination)
                TelemetryMetricItem(
                    label = "الانحراف المغناطيسي",
                    value = declText,
                    subtitle = "شمال حقيقي",
                    valueColor = EmeraldGreen
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons Row (Calibrate, Save, Share, History)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCalibrateClick,
                    border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Build, contentDescription = null, tint = AmberGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("معايرة", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AmberGold)
                }

                OutlinedButton(
                    onClick = onSaveHistoryClick,
                    border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Bookmark, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("حفظ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ChampagneGold)
                }

                OutlinedButton(
                    onClick = onShareClick,
                    border = BorderStroke(1.dp, IceCyan.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = IceCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("مشاركة", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IceCyan)
                }

                OutlinedButton(
                    onClick = onToggleHistoryClick,
                    border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.History, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("السجل", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                }
            }
        }
    }
}

// ==========================================
// 📜 QIBLA LOCATION HISTORY LOG CARD
// ==========================================
@Composable
private fun QiblaHistoryCard(
    historyList: List<QiblaHistoryItem>,
    onSelectHistoryItem: (lat: Double, lng: Double, cityName: String) -> Unit,
    onClearHistory: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = GlassSurface,
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.History, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("سجل المواقع المحفوظة", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                TextButton(onClick = onClearHistory) {
                    Text("مسح السجل", fontSize = 12.sp, color = ErrorRed)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (historyList.isEmpty()) {
                Text(
                    text = "لا توجد مواقع محفوظة حاليًا. اضغط على زر 'حفظ' لحفظ موقعك الحالي.",
                    fontSize = 12.sp,
                    color = SlateMuted,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    historyList.forEach { item ->
                        Surface(
                            onClick = { onSelectHistoryItem(item.lat, item.lng, item.cityName) },
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Black.copy(alpha = 0.3f),
                            border = BorderStroke(0.5.dp, GlassBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = item.cityName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(text = "${item.timestampText} • %d كم".format(Locale.getDefault(), item.distanceKm.toInt()), fontSize = 11.sp, color = SlateMuted)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "${item.qiblaAngle.toInt()}°", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = IceCyan)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = null, tint = SlateMuted, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 🛠️ DEBUG & SENSOR TELEMETRY OVERLAY
// ==========================================
@Composable
private fun QiblaDebugOverlay(
    rotationVector: FloatArray,
    quaternion: FloatArray,
    magHeading: Float,
    trueHeading: Float,
    declination: Float,
    qiblaBearing: Float,
    relativeAngle: Float,
    sensorAccuracy: Int,
    magFieldTesla: Float,
    isMagneticInterference: Boolean,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = 0.90f),
        border = BorderStroke(1.dp, IceCyan)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "🛠️ Debug Sensor Fusion Telemetry", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = IceCyan)
                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "• RotationVector: [%.3f, %.3f, %.3f]".format(Locale.US, rotationVector.getOrElse(0){0f}, rotationVector.getOrElse(1){0f}, rotationVector.getOrElse(2){0f}), fontSize = 11.sp, color = Color.LightGray)
            Text(text = "• Quaternion (w,x,y,z): [%.3f, %.3f, %.3f, %.3f]".format(Locale.US, quaternion.getOrElse(0){1f}, quaternion.getOrElse(1){0f}, quaternion.getOrElse(2){0f}, quaternion.getOrElse(3){0f}), fontSize = 11.sp, color = Color.LightGray)
            Text(text = "• Mag Heading: %.1f° | True Heading: %.1f°".format(Locale.US, magHeading, trueHeading), fontSize = 11.sp, color = ChampagneGold)
            Text(text = "• Declination: %+.1f° | Qibla Bearing: %.1f°".format(Locale.US, declination, qiblaBearing), fontSize = 11.sp, color = ChampagneGold)
            Text(text = "• Relative Qibla Angle: %.1f°".format(Locale.US, relativeAngle), fontSize = 11.sp, color = EmeraldGreen)
            Text(text = "• Mag Field: %.1f µT (%s)".format(Locale.US, magFieldTesla, if (isMagneticInterference) "⚠️ Anomaly" else "Normal"), fontSize = 11.sp, color = if (isMagneticInterference) ErrorRed else EmeraldGreen)
            Text(text = "• Sensor Accuracy Status: %d | Frame rate: 60 FPS Engine".format(Locale.US, sensorAccuracy), fontSize = 11.sp, color = SlateMuted)
        }
    }
}

@Composable
private fun TelemetryMetricItem(
    label: String,
    value: String,
    subtitle: String,
    valueColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = SlateMuted
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = valueColor
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            fontSize = 10.sp,
            color = SlateMuted.copy(alpha = 0.8f)
        )
    }
}

// ==========================================
// 🌆 MANUAL LOCATION / CITY PICKER DIALOG
// ==========================================
@Composable
private fun ManualLocationPickerDialog(
    currentCityName: String?,
    onDismiss: () -> Unit,
    onSelectCity: (lat: Double, lng: Double, cityName: String) -> Unit,
    onRequestGpsLocation: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var customLatText by remember { mutableStateOf("") }
    var customLngText by remember { mutableStateOf("") }
    var showCustomCoordsInput by remember { mutableStateOf(false) }

    val filteredCities = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            popularWorldCities
        } else {
            popularWorldCities.filter {
                it.nameAr.contains(searchQuery, ignoreCase = true) ||
                it.countryAr.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF141926),
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "اختر المدينة / تغيير المكان",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = SlateMuted)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // GPS Auto Location Button
                Surface(
                    onClick = onRequestGpsLocation,
                    shape = RoundedCornerShape(14.dp),
                    color = AmberGold.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, AmberGold)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = null, tint = AmberGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تحديد موقعي التلقائي عبر GPS",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberGold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ابحث عن مدينتك (مثل: مكة، القاهرة، دبي...)", fontSize = 12.sp, color = SlateMuted) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ChampagneGold) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ChampagneGold,
                        unfocusedBorderColor = GlassBorder,
                        focusedContainerColor = Color.Black.copy(alpha = 0.3f),
                        unfocusedContainerColor = Color.Black.copy(alpha = 0.3f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Toggle Custom Coords
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCustomCoordsInput = !showCustomCoordsInput },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (showCustomCoordsInput) "اختر من قائمة المدن الشائعة" else "أو إدخال إحداثيات مخصصة (خط العرض والطول)",
                        fontSize = 11.sp,
                        color = IceCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = if (showCustomCoordsInput) Icons.Default.List else Icons.Default.AddLocation,
                        contentDescription = null,
                        tint = IceCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (showCustomCoordsInput) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        OutlinedTextField(
                            value = customLatText,
                            onValueChange = { customLatText = it },
                            label = { Text("خط العرض (Latitude)", fontSize = 11.sp, color = SlateMuted) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ChampagneGold,
                                unfocusedBorderColor = GlassBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customLngText,
                            onValueChange = { customLngText = it },
                            label = { Text("خط الطول (Longitude)", fontSize = 11.sp, color = SlateMuted) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ChampagneGold,
                                unfocusedBorderColor = GlassBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val lat = customLatText.toDoubleOrNull()
                                val lng = customLngText.toDoubleOrNull()
                                if (lat != null && lng != null) {
                                    onSelectCity(lat, lng, "إحداثيات مخصصة ($lat, $lng)")
                                }
                            },
                            enabled = customLatText.toDoubleOrNull() != null && customLngText.toDoubleOrNull() != null,
                            colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("تطبيق الإحداثيات", color = ObsidianBgTop, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Cities List
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredCities) { city ->
                            val isSelected = currentCityName?.contains(city.nameAr, ignoreCase = true) == true
                            Surface(
                                onClick = {
                                    onSelectCity(city.lat, city.lng, "${city.nameAr}، ${city.countryAr}")
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) ChampagneGold.copy(alpha = 0.2f) else GlassSurface,
                                border = BorderStroke(1.dp, if (isSelected) ChampagneGold else GlassBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = "🏙️", fontSize = 18.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = city.nameAr,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = city.countryAr,
                                                fontSize = 11.sp,
                                                color = SlateMuted
                                            )
                                        }
                                    }

                                    val qiblaDegFloat = calculateQiblaAzimuth(city.lat, city.lng)
                                    val qiblaDeg = qiblaDegFloat.toInt()
                                    val dirText = formatDirectionArabic(qiblaDegFloat)
                                    val distKm = calculateDistanceToKaaba(city.lat, city.lng).toInt()

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "$qiblaDeg° ($dirText)",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = IceCyan
                                            )
                                            Text(
                                                text = if (distKm == 0) "في موقع الكعبة" else "%,d كم إلى مكة".format(Locale.getDefault(), distKm),
                                                fontSize = 10.sp,
                                                color = SlateMuted
                                            )
                                        }
                                        if (isSelected) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = EmeraldGreen,
                                                modifier = Modifier.size(16.dp)
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
    }
}

// ==========================================
// 💀 SKELETON SHIMMER LOADER VIEW
// ==========================================
@Composable
private fun QiblaSkeletonLoader() {
    val infiniteTransition = rememberInfiniteTransition(label = "skeletonShimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(30.dp))
        // Skeleton Circular Dial
        Box(
            modifier = Modifier
                .size(280.dp)
                .clip(CircleShape)
                .background(GlassSurface.copy(alpha = shimmerAlpha))
                .border(1.dp, GlassBorder, CircleShape)
        )
        Spacer(modifier = Modifier.height(30.dp))
        // Skeleton Telemetry Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(GlassSurface.copy(alpha = shimmerAlpha))
        )
    }
}

// ==========================================
// 🛰️ LOCATION DISABLED STATE VIEW
// ==========================================
@Composable
private fun LocationDisabledStateView(
    onEnableLocationClick: () -> Unit,
    onRequestPermissionClick: () -> Unit,
    onSelectManualCityClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(AmberGold.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.LocationOff,
                contentDescription = "الموقع معطل",
                tint = AmberGold,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "خدمة الموقع GPS غير مفعلة",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "يمكنك اختيار مدينتك يدويًا أو تفعيل موقع GPS لحساب زاوية القبلة بدقة متناهية.",
            fontSize = 12.sp,
            color = SlateMuted,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onSelectManualCityClick,
                colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.LocationCity, contentDescription = null, tint = ObsidianBgTop)
                Spacer(modifier = Modifier.width(8.dp))
                Text("اختر المدينة يدويًا بدون GPS", color = ObsidianBgTop, fontWeight = FontWeight.Bold)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onRequestPermissionClick,
                    colors = ButtonDefaults.buttonColors(containerColor = AmberGold.copy(alpha = 0.2f)),
                    border = BorderStroke(1.dp, AmberGold),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("منح الإذن", color = AmberGold, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onEnableLocationClick,
                    border = BorderStroke(1.dp, IceCyan),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("تفعيل GPS", color = IceCyan)
                }
            }
        }
    }
}

// ==========================================
// ⚠️ ERROR STATE VIEW WITH SHAKING CARD
// ==========================================
@Composable
private fun ErrorStateView(
    errorMessage: String,
    shakeTrigger: Int,
    onRetryClick: () -> Unit,
    onSelectManualCityClick: () -> Unit
) {
    val animOffset by animateFloatAsState(
        targetValue = if (shakeTrigger % 2 == 1) 12f else -12f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh, dampingRatio = Spring.DampingRatioHighBouncy),
        label = "shakeAnim"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(translationX = animOffset)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = GlassSurface,
            border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "خطأ",
                    tint = ErrorRed,
                    modifier = Modifier.size(44.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "تعذر تحديد الموقع تلقائيًا",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = errorMessage,
                    fontSize = 12.sp,
                    color = SlateMuted,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onSelectManualCityClick,
                        colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.LocationCity, contentDescription = null, tint = ObsidianBgTop)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("اختر مدينتك يدويًا من القائمة", color = ObsidianBgTop, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onRetryClick,
                        border = BorderStroke(1.dp, ErrorRed),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = ErrorRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إعادة محاولة جلب الموقع", color = ErrorRed, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// ♾️ SENSOR CALIBRATION HELPER DIALOG (FIGURE-8 ANIMATION)
// ==========================================
@Composable
private fun SensorCalibrationDialog(
    onDismiss: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fig8Dot")
    val dotProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dotProgress"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF141926),
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "دليل معايرة البوصلة (Figure 8)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "حرك هاتفك في الهواء على شكل الرقم 8 بالعربية (∞) لضبط البوصلة وتفريغ الشحنات المغناطيسية.",
                    fontSize = 13.sp,
                    color = SlateMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Interactive Figure-8 Canvas Path Animation
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val centerX = w / 2f
                    val centerY = h / 2f
                    val rx = w * 0.35f
                    val ry = h * 0.35f

                    // Draw Figure-8 Lemniscate Path
                    val path = Path()
                    var first = true
                    for (step in 0..100) {
                        val t = (step / 100f) * 2 * PI
                        val x = centerX + rx * sin(t).toFloat()
                        val y = centerY + ry * (sin(2 * t) / 2).toFloat()
                        if (first) {
                            path.moveTo(x, y)
                            first = false
                        } else {
                            path.lineTo(x, y)
                        }
                    }

                    drawPath(
                        path = path,
                        color = ChampagneGold.copy(alpha = 0.4f),
                        style = Stroke(width = 3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f)))
                    )

                    // Animated Golden Glowing Dot Moving Along Path
                    val dotX = centerX + rx * sin(dotProgress.toDouble()).toFloat()
                    val dotY = centerY + ry * (sin(2 * dotProgress.toDouble()) / 2).toFloat()

                    drawCircle(
                        color = AmberGold.copy(alpha = 0.3f),
                        radius = 14.dp.toPx(),
                        center = Offset(dotX, dotY)
                    )
                    drawCircle(
                        color = IceCyan,
                        radius = 6.dp.toPx(),
                        center = Offset(dotX, dotY)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = AmberGold),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("تمت المعايرة بنجاح", color = ObsidianBgTop, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
