package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppIcons
import com.example.ui.theme.CustomThemeColors
import com.example.util.AdhanScheduler

private const val PREFS_NAME = "clevcalc_adhan_prefs"
private const val KEY_SOUND_URI = "adhan_sound_uri"
private const val KEY_VIBRATE = "adhan_vibrate_enabled"

private data class PrayerToggle(val key: String, val label: String)

private val PRAYER_TOGGLES = listOf(
    PrayerToggle("adhan_fajr", "الفجر"),
    PrayerToggle("adhan_dhuhr", "الظهر"),
    PrayerToggle("adhan_asr", "العصر"),
    PrayerToggle("adhan_maghrib", "المغرب"),
    PrayerToggle("adhan_isha", "العشاء"),
)

/**
 * One place to control everything about the Adhan notification: which prayers alert,
 * which sound plays (via the system's own alarm-sound picker — the same professional
 * pattern used by Google Clock / Samsung Clock, no need to bundle audio files), and
 * whether the phone vibrates. Replaces the previous behaviour where the app always
 * played the device's default alarm tone and always vibrated with no way to change it.
 */
@Composable
fun AdhanSettingsScreen(colors: CustomThemeColors) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    var soundUri by remember {
        mutableStateOf(
            prefs.getString(KEY_SOUND_URI, null)?.let { Uri.parse(it) }
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        )
    }
    var vibrateEnabled by remember { mutableStateOf(prefs.getBoolean(KEY_VIBRATE, true)) }
    var togglesState by remember {
        mutableStateOf(PRAYER_TOGGLES.associate { it.key to prefs.getBoolean(it.key, false) })
    }

    val ringtonePicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            soundUri = uri
            prefs.edit().putString(KEY_SOUND_URI, uri?.toString()).apply()
        }
    }

    fun openSoundPicker() {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "اختر صوت الأذان")
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, soundUri)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
        }
        ringtonePicker.launch(intent)
    }

    fun previewSound() {
        val uri = soundUri ?: return
        try {
            val player = MediaPlayer()
            player.setDataSource(context, uri)
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            player.setOnCompletionListener { it.release() }
            player.prepare()
            player.start()
        } catch (_: Exception) { /* best-effort preview only */ }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.appBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("تفعيل الأذان لكل صلاة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.text)
        }

        items(PRAYER_TOGGLES) { toggle ->
            Surface(
                color = colors.surface,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(AppIcons.forCalc(com.example.model.CalcKey.PRAYER), contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(toggle.label, fontSize = 14.sp, color = colors.text)
                    }
                    Switch(
                        checked = togglesState[toggle.key] == true,
                        onCheckedChange = { checked ->
                            togglesState = togglesState.toMutableMap().apply { put(toggle.key, checked) }
                            prefs.edit().putBoolean(toggle.key, checked).apply()
                            AdhanScheduler.rescheduleAllFromPreferences(context)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = colors.accent)
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text("الصوت والتنبيه", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.text)
        }

        item {
            Surface(color = colors.surface, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.accent.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(AppIcons.VolumeUp, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("صوت الأذان", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.text)
                            Text(
                                RingtoneManager.getRingtone(context, soundUri)?.getTitle(context) ?: "افتراضي النظام",
                                fontSize = 11.sp,
                                color = colors.textMuted
                            )
                        }
                        IconButton(onClick = { previewSound() }) {
                            Icon(AppIcons.PlayCircle, contentDescription = "تشغيل تجريبي", tint = colors.accent)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { openSoundPicker() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("تغيير صوت الأذان", color = colors.accent)
                    }
                    Text(
                        "يفتح منتقي الأصوات الرسمي بالنظام — يمكنك اختيار أي نغمة منبّه مثبّتة على جهازك، أو إضافة ملف أذان خاص بك من إعدادات الجهاز.",
                        fontSize = 10.sp,
                        color = colors.textMuted,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }

        item {
            Surface(color = colors.surface, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(AppIcons.Vibration, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("الاهتزاز مع الأذان", fontSize = 13.sp, color = colors.text)
                    }
                    Switch(
                        checked = vibrateEnabled,
                        onCheckedChange = {
                            vibrateEnabled = it
                            prefs.edit().putBoolean(KEY_VIBRATE, it).apply()
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = colors.accent)
                    )
                }
            }
        }

        item {
            OutlinedButton(
                onClick = {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(AppIcons.Notifications, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("إدارة أذونات الإشعارات من إعدادات النظام", color = colors.accent, fontSize = 12.sp)
            }
        }
    }
}
