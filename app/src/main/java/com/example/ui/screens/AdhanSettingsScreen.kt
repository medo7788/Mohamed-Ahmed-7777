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
import androidx.compose.foundation.clickable
import com.example.util.AdhanScheduler
import com.example.ui.theme.Spacing
import com.example.viewmodel.MainViewModel
import com.example.ui.theme.AppThemeKey

private const val PREFS_NAME = "clevcalc_adhan_prefs"
private const val KEY_SOUND_URI = "adhan_sound_uri"
private const val KEY_VIBRATE = "adhan_vibrate_enabled"
private const val KEY_ADHAN_VOICE = "adhan_voice_key"
private const val KEY_QURAN_VOICE = "quran_voice_key"

private data class PrayerToggle(val key: String, val label: String)

private val PRAYER_TOGGLES = listOf(
    PrayerToggle("adhan_fajr", "الفجر"),
    PrayerToggle("adhan_dhuhr", "الظهر"),
    PrayerToggle("adhan_asr", "العصر"),
    PrayerToggle("adhan_maghrib", "المغرب"),
    PrayerToggle("adhan_isha", "العشاء"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdhanSettingsScreen(colors: CustomThemeColors, viewModel: MainViewModel) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    val currentThemeKey by viewModel.currentThemeKey.collectAsState()

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

    var selectedAdhanVoice by remember {
        mutableStateOf(prefs.getString(KEY_ADHAN_VOICE, "system") ?: "system")
    }
    var selectedQuranVoice by remember {
        mutableStateOf(prefs.getString(KEY_QURAN_VOICE, "ar.alafasy") ?: "ar.alafasy")
    }

    var showAdhanVoiceDropdown by remember { mutableStateOf(false) }
    var showQuranVoiceDropdown by remember { mutableStateOf(false) }

    val adhanVoices = listOf(
        Pair("system", "افتراضي النظام (نغمة المنبه)"),
        Pair("makkah", "أذان المسجد الحرام (مكة المكرمة)"),
        Pair("madinah", "أذان المسجد النبوي (المدينة)"),
        Pair("alafasy", "أذان الشيخ مشاري العفاسي"),
        Pair("abdulbasit", "أذان الشيخ عبد الباسط عبد الصمد")
    )

    val quranVoices = listOf(
        Pair("ar.alafasy", "الشيخ مشاري العفاسي"),
        Pair("ar.abdulsamad", "الشيخ عبد الباسط عبد الصمد"),
        Pair("ar.ghaamidi", "الشيخ سعد الغامدي"),
        Pair("ar.mahermuaiqly", "الشيخ ماهر المعيقلي")
    )

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
        val uriToPlay = if (selectedAdhanVoice == "system") {
            soundUri
        } else {
            val voiceUrl = when (selectedAdhanVoice) {
                "makkah" -> "https://www.islamcan.com/audio/adhan/adhan-makkah.mp3"
                "madinah" -> "https://www.islamcan.com/audio/adhan/adhan-madinah.mp3"
                "alafasy" -> "https://www.islamcan.com/audio/adhan/adhan-alafasy.mp3"
                "abdulbasit" -> "https://www.islamcan.com/audio/adhan/adhan-abdulbasit.mp3"
                else -> "https://www.islamcan.com/audio/adhan/adhan-makkah.mp3"
            }
            Uri.parse(voiceUrl)
        } ?: return

        try {
            val player = MediaPlayer()
            player.setDataSource(context, uriToPlay)
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            player.setOnCompletionListener { it.release() }
            player.prepareAsync()
            player.setOnPreparedListener { it.start() }
        } catch (_: Exception) { /* best-effort preview only */ }
    }

    ToolScreenScaffold(
        colors = colors,
        icon = AppIcons.forCalc(CalcKey.ADHAN_SETTINGS),
        title = "إعدادات التطبيق الشاملة",
        subtitle = "تخصيص المظهر، تنبيهات الأذان، وأصوات المقرئين والمؤذنين المفضلة لديك"
    ) {
        // --- SECTION 1: Appearance ---
        Text("مظهر التطبيق", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.text)
        Spacer(modifier = Modifier.height(Spacing.Small))
        
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
                    Text(
                        if (currentThemeKey == AppThemeKey.ELEGANT_DARK) "🌙" else "☀️",
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("السمة الحالية", fontSize = 14.sp, color = colors.text, fontWeight = FontWeight.Bold)
                        Text(
                            if (currentThemeKey == AppThemeKey.ELEGANT_DARK) "داكن ملكي فخم" else "فاتح ملكي هادئ",
                            fontSize = 12.sp,
                            color = colors.textMuted
                        )
                    }
                }

                Button(
                    onClick = { viewModel.toggleTheme(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("تبديل", color = colors.appBg, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- SECTION 2: Voices (Adhan & Quran) ---
        Text("تخصيص الأصوات", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.text)
        Spacer(modifier = Modifier.height(Spacing.Small))

        // Adhan Voice Dropdown Selector
        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("المؤذن المفضل", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.text)
                Spacer(modifier = Modifier.height(8.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        color = colors.surface2,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAdhanVoiceDropdown = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val activeLabel = adhanVoices.find { it.first == selectedAdhanVoice }?.second ?: selectedAdhanVoice
                            Text(activeLabel, fontSize = 13.sp, color = colors.text)
                            Text("▼", fontSize = 10.sp, color = colors.textMuted)
                        }
                    }

                    DropdownMenu(
                        expanded = showAdhanVoiceDropdown,
                        onDismissRequest = { showAdhanVoiceDropdown = false },
                        modifier = Modifier.background(colors.surface)
                    ) {
                        adhanVoices.forEach { voice ->
                            DropdownMenuItem(
                                text = { Text(voice.second, color = colors.text) },
                                onClick = {
                                    selectedAdhanVoice = voice.first
                                    prefs.edit().putString(KEY_ADHAN_VOICE, voice.first).apply()
                                    showAdhanVoiceDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quran Voice Dropdown Selector
        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("القارئ المفضل (القرآن الكريم)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.text)
                Spacer(modifier = Modifier.height(8.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        color = colors.surface2,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showQuranVoiceDropdown = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val activeLabel = quranVoices.find { it.first == selectedQuranVoice }?.second ?: selectedQuranVoice
                            Text(activeLabel, fontSize = 13.sp, color = colors.text)
                            Text("▼", fontSize = 10.sp, color = colors.textMuted)
                        }
                    }

                    DropdownMenu(
                        expanded = showQuranVoiceDropdown,
                        onDismissRequest = { showQuranVoiceDropdown = false },
                        modifier = Modifier.background(colors.surface)
                    ) {
                        quranVoices.forEach { voice ->
                            DropdownMenuItem(
                                text = { Text(voice.second, color = colors.text) },
                                onClick = {
                                    selectedQuranVoice = voice.first
                                    prefs.edit().putString(KEY_QURAN_VOICE, voice.first).apply()
                                    showQuranVoiceDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- SECTION 3: Adhan Alerts ---
        Text("تفعيل تنبيهات الأذان", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.text)
        Spacer(modifier = Modifier.height(Spacing.Small))

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

        Text("الرنات والتنبيهات المخصصة", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.text)
        Spacer(modifier = Modifier.height(Spacing.Small))

        // Sound Picker Card
        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(24.dp),
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
                        Text("نغمة الأذان الافتراضية", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.text)
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
                
                if (selectedAdhanVoice == "system") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { openSoundPicker() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                    ) {
                        Text("اختيار نغمة من الجهاز", color = colors.appBg, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "ملاحظة: لقد قمت باختيار نغمة مؤذن مخصص بالأعلى، وسيتم استخدامها كتنبيه بدلاً من نغمة النظام.",
                        fontSize = 11.sp,
                        color = colors.textMuted,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Vibrate Card
        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(20.dp),
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
                    Text("الاهتزاز مع التنبيه", fontSize = 16.sp, color = colors.text, fontWeight = FontWeight.Medium)
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

        Spacer(modifier = Modifier.height(32.dp))

        // --- SECTION 4: About ---
        Text("حول التطبيق", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.text)
        Spacer(modifier = Modifier.height(Spacing.Small))

        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("ClevCalc Pro Ultimate", fontSize = 16.sp, fontWeight = FontWeight.Black, color = colors.accent)
                Text("الإصدار: Version X (الجيل الذكي)", fontSize = 12.sp, color = colors.text, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "تطبيق الأندرويد الشامل والفريد الذي يجمع بين أدوات الحساب والمالية والذكاء الاصطناعي والخدمات الإسلامية والطقس تحت سقف واحد بتجربة تصميم ملكية مذهلة.",
                    fontSize = 12.sp,
                    color = colors.textMuted,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
