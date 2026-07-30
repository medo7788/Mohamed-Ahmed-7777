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
import com.example.ui.components.ToolScreenScaffold
import com.example.model.CalcKey
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.BorderStroke
import com.example.util.AdhanScheduler
import com.example.ui.theme.Spacing

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

    ToolScreenScaffold(
        colors = colors,
        icon = AppIcons.forCalc(CalcKey.ADHAN_SETTINGS),
        title = "إعدادات الأذان",
        subtitle = "تخصيص تنبيهات الصلاة واختيار أصوات الأذان والاهتزاز"
    ) {
        Text("تفعيل التنبيهات", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.text)
        
        Spacer(modifier = Modifier.height(Spacing.Medium))

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            PRAYER_TOGGLES.forEach { toggle ->
                Surface(
                    color = colors.surface,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.Medium),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(AppIcons.forCalc(CalcKey.PRAYER), null, tint = colors.accent, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(toggle.label, fontSize = 16.sp, color = colors.text, fontWeight = FontWeight.Medium)
                        }
                        Switch(
                            checked = togglesState[toggle.key] == true,
                            onCheckedChange = { checked ->
                                togglesState = togglesState.toMutableMap().apply { put(toggle.key, checked) }
                                prefs.edit().putBoolean(toggle.key, checked).apply()
                                AdhanScheduler.rescheduleAllFromPreferences(context)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = colors.accent,
                                uncheckedThumbColor = colors.textMuted,
                                uncheckedTrackColor = colors.surface2
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("تخصيص التنبيه", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.text)

        // Sound Picker Card
        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.accent.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(AppIcons.VolumeUp, null, tint = colors.accent, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("نغمة الأذان", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.text)
                        Text(
                            RingtoneManager.getRingtone(context, soundUri)?.getTitle(context) ?: "افتراضي النظام",
                            fontSize = 12.sp,
                            color = colors.textMuted
                        )
                    }
                    IconButton(onClick = { previewSound() }) {
                        Icon(AppIcons.PlayCircle, null, tint = colors.accent, modifier = Modifier.size(28.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { openSoundPicker() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                ) {
                    Text("اختيار نغمة من الجهاز", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Vibrate Card
        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(AppIcons.Vibration, null, tint = colors.accent, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("الاهتزاز", fontSize = 16.sp, color = colors.text, fontWeight = FontWeight.Medium)
                }
                Switch(
                    checked = vibrateEnabled,
                    onCheckedChange = {
                        vibrateEnabled = it
                        prefs.edit().putBoolean(KEY_VIBRATE, it).apply()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = colors.accent
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.5f))
        ) {
            Icon(AppIcons.Notifications, null, tint = colors.accent, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("إعدادات إشعارات النظام", color = colors.accent, fontWeight = FontWeight.Bold)
        }
    }
}
