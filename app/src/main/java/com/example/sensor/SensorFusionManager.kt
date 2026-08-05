package com.example.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.domain.QiblaMath
import kotlin.math.abs
import kotlin.math.sqrt

enum class SensorAccuracyLevel {
    HIGH,
    MEDIUM,
    UNRELIABLE
}

data class SensorFusionState(
    val magneticHeading: Float = 0f,
    val declination: Float = 0f,
    val trueHeading: Float = 0f,
    val rotationVector: FloatArray = floatArrayOf(0f, 0f, 0f, 0f),
    val quaternion: FloatArray = floatArrayOf(1f, 0f, 0f, 0f),
    val magneticFieldStrength: Float = 45f,
    val accuracy: SensorAccuracyLevel = SensorAccuracyLevel.HIGH,
    val isInterferenceDetected: Boolean = false,
    val activeSensorType: Int = Sensor.TYPE_ROTATION_VECTOR
)

class SensorFusionManager(context: Context) : SensorEventListener {

    private val sensorManager = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private var rotationVectorSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private var gameRotationVectorSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
    private var accelerometerSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var magnetometerSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private var activeSensorType = Sensor.TYPE_ROTATION_VECTOR

    // Sensor raw values
    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private val rawQuaternion = FloatArray(4) { if (it == 0) 1f else 0f }

    private var filteredHeading = 0f
    private var lastEmittedTime = 0L
    private var declination = 0f

    private var isRegistered = false

    var onSensorStateChanged: ((SensorFusionState) -> Unit)? = null

    fun setDeclination(declinationDegrees: Float) {
        this.declination = declinationDegrees
    }

    fun startListening() {
        if (isRegistered || sensorManager == null) return

        var registered = false

        if (rotationVectorSensor != null) {
            registered = sensorManager.registerListener(
                this,
                rotationVectorSensor,
                SensorManager.SENSOR_DELAY_GAME
            )
            activeSensorType = Sensor.TYPE_ROTATION_VECTOR
        }

        if (!registered && gameRotationVectorSensor != null) {
            registered = sensorManager.registerListener(
                this,
                gameRotationVectorSensor,
                SensorManager.SENSOR_DELAY_GAME
            )
            activeSensorType = Sensor.TYPE_GAME_ROTATION_VECTOR
        }

        if (!registered) {
            val accelReg = accelerometerSensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            } ?: false
            val magReg = magnetometerSensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            } ?: false
            registered = accelReg && magReg
            activeSensorType = Sensor.TYPE_MAGNETIC_FIELD
        }

        isRegistered = registered
    }

    fun stopListening() {
        if (!isRegistered || sensorManager == null) return
        sensorManager.unregisterListener(this)
        isRegistered = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        val now = System.currentTimeMillis()
        // Throttle updates to max ~60 FPS (16ms)
        if (now - lastEmittedTime < 16) return

        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR, Sensor.TYPE_GAME_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getQuaternionFromVector(rawQuaternion, event.values)
                SensorManager.getOrientation(rotationMatrix, orientationAngles)

                val rawAzimuthRad = orientationAngles[0]
                val magneticHeadingDeg = QiblaMath.normalizeAngle(Math.toDegrees(rawAzimuthRad.toDouble()).toFloat())

                processNewHeading(
                    magneticHeading = magneticHeadingDeg,
                    rotationVector = event.values.clone(),
                    quaternion = rawQuaternion.clone(),
                    magneticFieldStrength = calculateMagneticFieldStrength(),
                    accuracy = mapAccuracyLevel(event.accuracy),
                    sensorType = event.sensor.type,
                    currentTime = now
                )
            }
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
                processAccelMag(now)
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
                processAccelMag(now)
            }
        }
    }

    private fun processAccelMag(now: Long) {
        val success = SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            accelerometerReading,
            magnetometerReading
        )
        if (success) {
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            val rawAzimuthRad = orientationAngles[0]
            val magneticHeadingDeg = QiblaMath.normalizeAngle(Math.toDegrees(rawAzimuthRad.toDouble()).toFloat())

            val fieldStrength = sqrt(
                magnetometerReading[0] * magnetometerReading[0] +
                        magnetometerReading[1] * magnetometerReading[1] +
                        magnetometerReading[2] * magnetometerReading[2]
            )

            processNewHeading(
                magneticHeading = magneticHeadingDeg,
                rotationVector = floatArrayOf(0f, 0f, 0f, 0f),
                quaternion = rawQuaternion.clone(),
                magneticFieldStrength = fieldStrength,
                accuracy = mapAccuracyLevel(SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM),
                sensorType = Sensor.TYPE_MAGNETIC_FIELD,
                currentTime = now
            )
        }
    }

    private fun calculateMagneticFieldStrength(): Float {
        if (magnetometerReading[0] != 0f || magnetometerReading[1] != 0f || magnetometerReading[2] != 0f) {
            return sqrt(
                magnetometerReading[0] * magnetometerReading[0] +
                        magnetometerReading[1] * magnetometerReading[1] +
                        magnetometerReading[2] * magnetometerReading[2]
            )
        }
        return 45f // Standard average earth field in microteslas
    }

    private fun processNewHeading(
        magneticHeading: Float,
        rotationVector: FloatArray,
        quaternion: FloatArray,
        magneticFieldStrength: Float,
        accuracy: SensorAccuracyLevel,
        sensorType: Int,
        currentTime: Long
    ) {
        // Angle-aware smoothing filter (prevent 0°/360° flip artifacts)
        val delta = QiblaMath.getShortestAngularDelta(filteredHeading, magneticHeading)
        
        // Dynamic alpha: faster tracking for large movements, smooth filtering for subtle drift
        val absDelta = abs(delta)
        val alpha = when {
            absDelta > 30f -> 0.45f
            absDelta > 10f -> 0.25f
            absDelta > 2f -> 0.12f
            else -> 0.08f
        }

        filteredHeading = QiblaMath.normalizeAngle(filteredHeading + delta * alpha)

        // Ignore micro oscillations smaller than 0.15°
        if (absDelta < 0.15f && currentTime - lastEmittedTime < 100) return

        lastEmittedTime = currentTime

        val trueHeading = QiblaMath.normalizeAngle(filteredHeading + declination)
        
        // Detect abnormal magnetic interference (outside normal 20.0 to 70.0 µT range)
        val isInterference = magneticFieldStrength < 20f || magneticFieldStrength > 70f

        val state = SensorFusionState(
            magneticHeading = filteredHeading,
            declination = declination,
            trueHeading = trueHeading,
            rotationVector = rotationVector,
            quaternion = quaternion,
            magneticFieldStrength = magneticFieldStrength,
            accuracy = accuracy,
            isInterferenceDetected = isInterference,
            activeSensorType = sensorType
        )

        onSensorStateChanged?.invoke(state)
    }

    private fun mapAccuracyLevel(accuracyStatus: Int): SensorAccuracyLevel {
        return when (accuracyStatus) {
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> SensorAccuracyLevel.HIGH
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> SensorAccuracyLevel.MEDIUM
            else -> SensorAccuracyLevel.UNRELIABLE
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Updated via accuracy mapping in onSensorChanged
    }
}
