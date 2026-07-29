package com.example.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Single source of truth for "where is the user right now".
 *
 * Every screen that previously rolled its own permission-check + LocationManager /
 * FusedLocationProviderClient call (Prayer times, Qibla, Weather...) should go through
 * this instead. It gives one consistent, professional flow:
 *   1. hasLocationPermission()      -> check before doing anything
 * 2. fetchCurrentLocation()       -> fresh GPS/network fix with a sane timeout,
 *                                     never a stale getLastKnownLocation() value.
 */
object AppLocationProvider {

    sealed class Result {
        data class Success(val latitude: Double, val longitude: Double, val accuracyMeters: Float?) : Result()
        object PermissionDenied : Result()
        object LocationDisabled : Result()
        object Timeout : Result()
        data class Error(val message: String) : Result()
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
