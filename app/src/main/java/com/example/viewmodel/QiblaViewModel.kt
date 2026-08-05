package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.location.Geocoder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MajorCity
import com.example.data.QiblaHistoryItem
import com.example.data.QiblaRepository
import com.example.domain.QiblaMath
import com.example.sensor.SensorAccuracyLevel
import com.example.sensor.SensorFusionManager
import com.example.sensor.SensorFusionState
import com.example.util.AppLocationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs

data class QiblaUiState(
    val azimuth: Float = 0f,
    val qiblaBearing: Double = 136.0, // Default for Mecca from Cairo/Middle East area
    val distanceToMakkah: Double = 1200.0,
    val location: Pair<Double, Double>? = Pair(30.0444, 31.2357), // Default Cairo
    val cityName: String = "القاهرة، مصر",
    val calibrationStatus: SensorAccuracyLevel = SensorAccuracyLevel.HIGH,
    val permissionsGranted: Boolean = true,
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val error: String? = null,
    val history: List<QiblaHistoryItem> = emptyList(),
    val isSimulationMode: Boolean = false,
    val manualHeading: Float = 0f,
    val declination: Float = 4.2f,
    val currentHeading: Float = 0f,
    val relativeAngle: Float = 136.0f,
    val magneticHeading: Float = 0f,
    val trueHeading: Float = 0f,
    val rotationVector: FloatArray = floatArrayOf(0f, 0f, 0f, 0f),
    val quaternion: FloatArray = floatArrayOf(1f, 0f, 0f, 0f),
    val magneticFieldStrength: Float = 45f,
    val isInterferenceDetected: Boolean = false,
    val isAlignedWithQibla: Boolean = false,
    val activeSensorType: Int = android.hardware.Sensor.TYPE_ROTATION_VECTOR,
    val fps: Int = 60,
    val recompositionCount: Long = 0L,
    val isDebugOverlayVisible: Boolean = false,
    val showCalibrationDialog: Boolean = false,
    val showCitySelectorDialog: Boolean = false,
    val showHistoryDrawer: Boolean = false,
    val gpsAccuracyMeters: Float? = null,
    val isWaitingForBetterGps: Boolean = false
)

class QiblaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = QiblaRepository(application)
    private val sensorFusionManager = SensorFusionManager(application)

    private val _uiState = MutableStateFlow(QiblaUiState())
    val uiState: StateFlow<QiblaUiState> = _uiState.asStateFlow()

    private var lastFpsCalculationTime = System.currentTimeMillis()
    private var frameCounter = 0

    init {
        // Load initial history and cached location
        loadHistory()
        loadCachedOrInitialLocation()

        // Configure sensor manager listener
        sensorFusionManager.onSensorStateChanged = { sensorState ->
            processSensorUpdate(sensorState)
        }
    }

    fun startSensors() {
        sensorFusionManager.startListening()
    }

    fun stopSensors() {
        sensorFusionManager.stopListening()
    }

    private fun processSensorUpdate(sensorState: SensorFusionState) {
        frameCounter++
        val now = System.currentTimeMillis()
        var calculatedFps = _uiState.value.fps
        if (now - lastFpsCalculationTime >= 1000) {
            calculatedFps = frameCounter
            frameCounter = 0
            lastFpsCalculationTime = now
        }

        val state = _uiState.value

        val effectiveHeading = if (state.isSimulationMode) {
            state.manualHeading
        } else {
            sensorState.trueHeading
        }

        val qiblaAngle = state.qiblaBearing
        val relativeAngle = QiblaMath.normalizeAngle((qiblaAngle - effectiveHeading).toFloat())

        // Check if aligned within ±2 degrees
        val angularDelta = abs(QiblaMath.getShortestAngularDelta(effectiveHeading, qiblaAngle.toFloat()))
        val isAligned = angularDelta <= 2.0f

        _uiState.update { current ->
            current.copy(
                magneticHeading = sensorState.magneticHeading,
                declination = sensorState.declination,
                trueHeading = effectiveHeading,
                currentHeading = effectiveHeading,
                relativeAngle = relativeAngle,
                rotationVector = sensorState.rotationVector,
                quaternion = sensorState.quaternion,
                magneticFieldStrength = sensorState.magneticFieldStrength,
                calibrationStatus = sensorState.accuracy,
                isInterferenceDetected = sensorState.isInterferenceDetected,
                activeSensorType = sensorState.activeSensorType,
                isAlignedWithQibla = isAligned,
                fps = calculatedFps,
                recompositionCount = current.recompositionCount + 1
            )
        }
    }

    fun loadCachedOrInitialLocation() {
        val lastLoc = repository.getLastLocation()
        if (lastLoc != null) {
            updateCoordinates(lastLoc.first, lastLoc.second, lastLoc.third)
        } else {
            // Default Cairo, Egypt
            updateCoordinates(30.0444, 31.2357, "القاهرة، مصر")
        }
    }

    fun fetchCurrentLocation(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val hasPerm = AppLocationProvider.hasLocationPermission(context)
            if (!hasPerm) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        permissionsGranted = false,
                        error = "إذن الموقع غير متاح. يمكنك تحديد مدينتك يدويًا."
                    )
                }
                return@launch
            }

            _uiState.update { it.copy(permissionsGranted = true) }

            when (val result = AppLocationProvider.fetchCurrentLocation(context)) {
                is AppLocationProvider.Result.Success -> {
                    val lat = result.latitude
                    val lng = result.longitude
                    val acc = result.accuracyMeters

                    val isPoorGps = acc != null && acc > 30f

                    _uiState.update {
                        it.copy(
                            gpsAccuracyMeters = acc,
                            isWaitingForBetterGps = isPoorGps
                        )
                    }

                    // Reverse geocode city name
                    val cityName = resolveCityName(context, lat, lng)
                    updateCoordinates(lat, lng, cityName)

                    repository.saveLastLocation(lat, lng, cityName)

                    _uiState.update { it.copy(isLoading = false, error = null) }
                }
                is AppLocationProvider.Result.PermissionDenied -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            permissionsGranted = false,
                            error = "تم رفض إذن الوصول للموقع."
                        )
                    }
                }
                is AppLocationProvider.Result.LocationDisabled -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "خدمات الموقع (GPS) معطلة على الجهاز."
                        )
                    }
                }
                else -> {
                    // Fallback to cached location if current location times out or fails
                    val cached = AppLocationProvider.getCachedLocation(context)
                    if (cached != null) {
                        updateCoordinates(cached.lat, cached.lng, cached.placeName ?: "الموقع المحفوظ")
                        _uiState.update { it.copy(isLoading = false, isOffline = true, error = null) }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "تعذّر تحديد الموقع الحالي. يرجى اختيار المدينة يدويًا."
                            )
                        }
                    }
                }
            }
        }
    }

    fun selectCity(city: MajorCity) {
        viewModelScope.launch {
            updateCoordinates(city.lat, city.lng, "${city.nameAr}، ${city.countryAr}")
            repository.saveLastLocation(city.lat, city.lng, city.nameAr)
            _uiState.update {
                it.copy(
                    showCitySelectorDialog = false,
                    isWaitingForBetterGps = false,
                    error = null
                )
            }
        }
    }

    private fun updateCoordinates(lat: Double, lng: Double, cityLabel: String) {
        val qiblaAngle = QiblaMath.calculateQiblaBearing(lat, lng)
        val distanceKm = QiblaMath.calculateDistanceToMakkahKm(lat, lng)
        val declination = QiblaMath.getMagneticDeclination(lat, lng)

        sensorFusionManager.setDeclination(declination)

        val currentHeading = _uiState.value.trueHeading
        val relativeAngle = QiblaMath.normalizeAngle((qiblaAngle - currentHeading).toFloat())

        _uiState.update { current ->
            current.copy(
                location = Pair(lat, lng),
                cityName = cityLabel,
                qiblaBearing = qiblaAngle,
                distanceToMakkah = distanceKm,
                declination = declination,
                relativeAngle = relativeAngle
            )
        }
    }

    private suspend fun resolveCityName(context: Context, lat: Double, lng: Double): String {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale("ar"))
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    val city = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: "مدينة"
                    val country = addr.countryName ?: ""
                    if (country.isNotBlank()) "$city، $country" else city
                } else {
                    "موقعي الحالي"
                }
            } catch (e: Exception) {
                "موقعي الحالي"
            }
        }
    }

    fun saveCurrentLocationToHistory() {
        val state = _uiState.value
        val loc = state.location ?: return
        val item = QiblaHistoryItem(
            cityName = state.cityName,
            lat = loc.first,
            lng = loc.second,
            qiblaAngle = state.qiblaBearing,
            distanceKm = state.distanceToMakkah
        )
        repository.saveHistoryItem(item)
        loadHistory()
    }

    private fun loadHistory() {
        val list = repository.getHistory()
        _uiState.update { it.copy(history = list) }
    }

    fun clearHistory() {
        repository.clearHistory()
        _uiState.update { it.copy(history = emptyList()) }
    }

    fun toggleSimulationMode(enabled: Boolean) {
        _uiState.update { it.copy(isSimulationMode = enabled) }
    }

    fun setManualHeading(heading: Float) {
        _uiState.update { it.copy(manualHeading = QiblaMath.normalizeAngle(heading)) }
    }

    fun toggleDebugOverlay() {
        _uiState.update { it.copy(isDebugOverlayVisible = !it.isDebugOverlayVisible) }
    }

    fun setShowCalibrationDialog(show: Boolean) {
        _uiState.update { it.copy(showCalibrationDialog = show) }
    }

    fun setShowCitySelectorDialog(show: Boolean) {
        _uiState.update { it.copy(showCitySelectorDialog = show) }
    }

    fun setShowHistoryDrawer(show: Boolean) {
        _uiState.update { it.copy(showHistoryDrawer = show) }
    }

    override fun onCleared() {
        super.onCleared()
        sensorFusionManager.stopListening()
    }
}
