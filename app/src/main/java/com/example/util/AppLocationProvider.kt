package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume

/**
 * Single source of truth for "where is the user right now".
 */
object AppLocationProvider {

    private const val PREFS_NAME = "app_location_cache"
    private const val KEY_LAT = "cached_lat"
    private const val KEY_LNG = "cached_lng"
    private const val KEY_PLACE = "cached_place"

    sealed class Result {
        data class Success(val latitude: Double, val longitude: Double, val accuracyMeters: Float?) : Result()
        object PermissionDenied : Result()
        object LocationDisabled : Result()
        object Timeout : Result()
        data class Error(val message: String) : Result()
    }

    data class CachedLocation(val lat: Double, val lng: Double, val placeName: String?)

    fun saveLocationToCache(context: Context, lat: Double, lng: Double, placeName: String? = null) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            putFloat(KEY_LAT, lat.toFloat())
            putFloat(KEY_LNG, lng.toFloat())
            putString(KEY_PLACE, placeName)
            apply()
        }
    }

    fun getCachedLocation(context: Context): CachedLocation? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_LAT)) return null
        return CachedLocation(
            lat = prefs.getFloat(KEY_LAT, 0f).toDouble(),
            lng = prefs.getFloat(KEY_LNG, 0f).toDouble(),
            placeName = prefs.getString(KEY_PLACE, null)
        )
    }

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun isLocationServiceEnabled(context: Context): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    suspend fun getLastKnownLocation(context: Context): Result {
        if (!hasLocationPermission(context)) return Result.PermissionDenied
        return try {
            val client = LocationServices.getFusedLocationProviderClient(context)
            val loc = client.lastLocation.await()
            if (loc != null) {
                Result.Success(loc.latitude, loc.longitude, loc.accuracy)
            } else {
                Result.Error("No last known location")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error getting last location")
        }
    }

    suspend fun fetchCurrentLocation(context: Context): Result {
        if (!hasLocationPermission(context)) return Result.PermissionDenied
        if (!isLocationServiceEnabled(context)) return Result.LocationDisabled

        return suspendCancellableCoroutine { cont ->
            try {
                val client = LocationServices.getFusedLocationProviderClient(context)
                val request = CurrentLocationRequest.Builder()
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .setDurationMillis(15_000)
                    .build()

                @Suppress("MissingPermission")
                val task = client.getCurrentLocation(request, null)
                task.addOnSuccessListener { loc ->
                    if (cont.isActive) {
                        if (loc != null) {
                            cont.resume(Result.Success(loc.latitude, loc.longitude, loc.accuracy))
                        } else {
                            cont.resume(Result.Timeout)
                        }
                    }
                }
                task.addOnFailureListener { e ->
                    if (cont.isActive) cont.resume(Result.Error(e.message ?: "تعذّر تحديد الموقع"))
                }
            } catch (e: SecurityException) {
                if (cont.isActive) cont.resume(Result.PermissionDenied)
            } catch (e: Exception) {
                if (cont.isActive) cont.resume(Result.Error(e.message ?: "تعذّر تحديد الموقع"))
            }
        }
    }
}
