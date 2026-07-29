package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.receiver.AdhanReceiver
import java.util.Calendar

object AdhanScheduler {
    private const val TAG = "AdhanScheduler"

    fun schedulePrayerAlarm(
        context: Context,
        prayerName: String,
        timeStr: String, // format "HH:mm"
        requestCode: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AdhanReceiver::class.java).apply {
            putExtra(AdhanReceiver.EXTRA_PRAYER_NAME, prayerName)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val parts = timeStr.split(":")
        if (parts.size < 2) return
        val hour = parts[0].trim().toIntOrNull() ?: return
        val minute = parts[1].trim().toIntOrNull() ?: return

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1) // Schedule for tomorrow if passed today
            }
        }

        var scheduledExact = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                try {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                    scheduledExact = true
                    Log.d(TAG, "Scheduled exact alarm for $prayerName at ${calendar.time}")
                } catch (e: SecurityException) {
                    Log.e(TAG, "SecurityException scheduling exact alarm", e)
                }
            } else {
                Log.w(TAG, "canScheduleExactAlarms() is false. Launching settings.")
                try {
                    val settingsIntent = Intent(
                        android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        android.net.Uri.parse("package:${context.packageName}")
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(settingsIntent)
                } catch (e: Exception) {
                    try {
                        val settingsIntent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(settingsIntent)
                    } catch (ex: Exception) {
                        Log.e(TAG, "Could not open exact alarm settings", ex)
                    }
                }
            }
        } else {
            // Android 6.0 to 11
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
                scheduledExact = true
                Log.d(TAG, "Scheduled exact alarm for $prayerName at ${calendar.time} (SDK < S)")
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException on older SDK while scheduling exact alarm", e)
            }
        }

        if (!scheduledExact) {
            // Fallback to setAndAllowWhileIdle or set
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
                Log.d(TAG, "Fallback to setAndAllowWhileIdle succeeded for $prayerName")
            } catch (e: Exception) {
                try {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                    Log.d(TAG, "Fallback to standard set() succeeded for $prayerName")
                } catch (ex: Exception) {
                    Log.e(TAG, "All alarm scheduling attempts failed for $prayerName", ex)
                }
            }
        }
    }

    fun cancelPrayerAlarm(context: Context, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AdhanReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun rescheduleAllFromPreferences(context: Context) {
        val prefs = context.getSharedPreferences("clevcalc_adhan_prefs", Context.MODE_PRIVATE)
        val selectedCityIndex = prefs.getInt("selected_city_index", 0)
        val baseCity = com.example.data.IslamicData.cities.getOrElse(selectedCityIndex) { com.example.data.IslamicData.cities[0] }

        val customLat = prefs.getString("custom_lat", null)?.toDoubleOrNull()
        val customLng = prefs.getString("custom_lng", null)?.toDoubleOrNull()
        val customLocationName = prefs.getString("custom_location_name", "") ?: ""

        val city = if (customLat != null && customLng != null) {
            com.example.data.CityPrayerInfo(
                customLocationName.ifBlank { "موقعي الحالي" },
                customLocationName.ifBlank { "My Location" },
                "", customLat, customLng, "", "", "", "", "", "", ""
            )
        } else {
            baseCity
        }

        val dynamicTimes = com.example.data.IslamicData.getDynamicPrayerTimesForCity(city)

        if (prefs.getBoolean("adhan_fajr", false)) {
            schedulePrayerAlarm(context, "الفجر", dynamicTimes.fajr, 1001)
        } else {
            cancelPrayerAlarm(context, 1001)
        }

        if (prefs.getBoolean("adhan_dhuhr", false)) {
            schedulePrayerAlarm(context, "الظهر", dynamicTimes.dhuhr, 1002)
        } else {
            cancelPrayerAlarm(context, 1002)
        }

        if (prefs.getBoolean("adhan_asr", false)) {
            schedulePrayerAlarm(context, "العصر", dynamicTimes.asr, 1003)
        } else {
            cancelPrayerAlarm(context, 1003)
        }

        if (prefs.getBoolean("adhan_maghrib", false)) {
            schedulePrayerAlarm(context, "المغرب", dynamicTimes.maghrib, 1004)
        } else {
            cancelPrayerAlarm(context, 1004)
        }

        if (prefs.getBoolean("adhan_isha", false)) {
            schedulePrayerAlarm(context, "العشاء", dynamicTimes.isha, 1005)
        } else {
            cancelPrayerAlarm(context, 1005)
        }
        Log.d(TAG, "All active alarms rescheduled from preferences")
    }
}
