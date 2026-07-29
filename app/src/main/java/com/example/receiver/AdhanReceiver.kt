package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.service.AdhanPlaybackService

class AdhanReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "الصلاة"
        Log.d("AdhanReceiver", "Received adhan alarm broadcast for $prayerName")

        val serviceIntent = Intent(context, AdhanPlaybackService::class.java).apply {
            action = AdhanPlaybackService.ACTION_START
            putExtra(AdhanPlaybackService.EXTRA_PRAYER_NAME, prayerName)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            Log.e("AdhanReceiver", "Failed to start AdhanPlaybackService", e)
        }
    }
}
