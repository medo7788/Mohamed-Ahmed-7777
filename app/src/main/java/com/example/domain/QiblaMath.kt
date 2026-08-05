package com.example.domain

import android.hardware.GeomagneticField
import kotlin.math.*

/**
 * Single source of truth for Qibla mathematical geodesy and angle conversions.
 * Kaaba exact coordinates: 21.422487° N, 39.826206° E.
 */
object QiblaMath {

    const val KAABA_LAT = 21.422487
    const val KAABA_LNG = 39.826206
    private const val EARTH_RADIUS_KM = 6371.0088

    /**
     * Calculates the initial Great-Circle Bearing from user coordinates to the Kaaba in Mecca.
     * Returns azimuth in degrees [0.0, 360.0).
     */
    fun calculateQiblaBearing(lat: Double, lng: Double): Double {
        val phi1 = Math.toRadians(lat)
        val lambda1 = Math.toRadians(lng)
        val phi2 = Math.toRadians(KAABA_LAT)
        val lambda2 = Math.toRadians(KAABA_LNG)

        val deltaLambda = lambda2 - lambda1

        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)

        var bearing = Math.toDegrees(atan2(y, x))
        return normalizeAngle(bearing)
    }

    /**
     * Calculates the Great-Circle distance to Mecca in kilometers.
     */
    fun calculateDistanceToMakkahKm(lat: Double, lng: Double): Double {
        val phi1 = Math.toRadians(lat)
        val phi2 = Math.toRadians(KAABA_LAT)
        val deltaPhi = Math.toRadians(KAABA_LAT - lat)
        val deltaLambda = Math.toRadians(KAABA_LNG - lng)

        val a = sin(deltaPhi / 2.0).pow(2.0) +
                cos(phi1) * cos(phi2) * sin(deltaLambda / 2.0).pow(2.0)
        val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))

        return EARTH_RADIUS_KM * c
    }

    /**
     * Normalizes an angle into [0, 360).
     */
    fun normalizeAngle(angle: Double): Double {
        var a = angle % 360.0
        if (a < 0.0) a += 360.0
        return a
    }

    /**
     * Normalizes a float angle into [0, 360).
     */
    fun normalizeAngle(angle: Float): Float {
        var a = angle % 360f
        if (a < 0f) a += 360f
        return a
    }

    /**
     * Calculates the shortest signed angular difference between two angles in degrees (-180 to +180).
     */
    fun getShortestAngularDelta(current: Float, target: Float): Float {
        val delta = (target - current + 540f) % 360f - 180f
        return delta
    }

    /**
     * Converts a 4-element Quaternion [w, x, y, z] to Euler Azimuth (heading angle in degrees [0, 360)).
     */
    fun quaternionToAzimuth(w: Float, x: Float, y: Float, z: Float): Float {
        // Yaw / Azimuth calculation from Quaternion
        val sinyCosp = 2f * (w * z + x * y)
        val cosyCosp = 1f - 2f * (y * y + z * z)
        val yaw = atan2(sinyCosp, cosyCosp)
        val azimuth = Math.toDegrees(yaw.toDouble()).toFloat()
        return normalizeAngle(azimuth)
    }

    /**
     * Gets magnetic declination at specific latitude, longitude, altitude and time using GeomagneticField.
     */
    fun getMagneticDeclination(lat: Double, lng: Double, altitudeMeters: Double = 0.0): Float {
        return try {
            val geomagneticField = GeomagneticField(
                lat.toFloat(),
                lng.toFloat(),
                altitudeMeters.toFloat(),
                System.currentTimeMillis()
            )
            geomagneticField.declination
        } catch (e: Exception) {
            0f
        }
    }
}
