package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class AdhanPlaybackService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    companion object {
        private const val TAG = "AdhanPlaybackService"
        const val CHANNEL_ID = "adhan_playback_channel_v2"
        const val NOTIFICATION_ID = 5005
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_PRAYER_NAME = "extra_prayer_name"

        private const val PREFS_NAME = "clevcalc_adhan_prefs"
        private const val KEY_SOUND_URI = "adhan_sound_uri"
        private const val KEY_VIBRATE = "adhan_vibrate_enabled"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AdhanPlaybackService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        val prayerName = intent?.getStringExtra(EXTRA_PRAYER_NAME) ?: "الصلاة"

        Log.d(TAG, "onStartCommand: action = $action, prayer = $prayerName")

        if (action == ACTION_STOP) {
            stopAdhan()
            stopSelf()
            return START_NOT_STICKY
        }

        startAdhan(prayerName)

        return START_STICKY
    }

    private fun startAdhan(prayerName: String) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val stopIntent = Intent(this, AdhanPlaybackService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            101,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val mainPendingIntent = PendingIntent.getActivity(
            this,
            102,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        createNotificationChannel()

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("حان الآن موعد صلاة $prayerName")
            .setContentText("الله أكبر الله أكبر - حان وقت صلاة $prayerName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(mainPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "إيقاف الأذان", stopPendingIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        // Play the sound the user picked in Adhan Settings (falls back to the
        // system's default alarm tone the first time, before any choice is made).
        try {
            val chosenUri: Uri = prefs.getString(KEY_SOUND_URI, null)?.let { Uri.parse(it) }
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)!!

            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@AdhanPlaybackService, chosenUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
            Log.d(TAG, "MediaPlayer started playing successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error playing sound via MediaPlayer", e)
        }

        // Vibrate only if the user left "الاهتزاز مع الأذان" enabled (defaults to on).
        val vibrateEnabled = prefs.getBoolean(KEY_VIBRATE, true)
        if (vibrateEnabled) {
            try {
                vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 1000, 500, 1000, 500, 1000, 500, 1000), 0))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000, 500, 1000), 0)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Vibrator error", e)
            }
        }
    }

    private fun stopAdhan() {
        Log.d(TAG, "Stopping adhan playback and vibration")
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (_: Exception) {}

        try {
            vibrator?.cancel()
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        stopAdhan()
        super.onDestroy()
        Log.d(TAG, "AdhanPlaybackService destroyed")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "تنبيهات الأذان والخدمة النشطة"
            val desc = "تشغيل صوت الأذان في الخلفية بشكل مستمر حتى الإيقاف"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = desc
                enableVibration(true)
                enableLights(true)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
