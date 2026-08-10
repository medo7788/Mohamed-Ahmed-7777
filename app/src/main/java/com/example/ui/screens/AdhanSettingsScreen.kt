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
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CalcKey
import com.example.ui.components.ToolScreenScaffold
import com.example.ui.theme.AppIcons
import com.example.ui.theme.CustomThemeColors
import com.example.ui.theme.Spacing
import com.example.util.AdhanScheduler
import com.example.util.FileDownloader
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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

// Premium UI Colors matching Spec
private val DarkBg = Color(0xFF121212)
private val AmbientNight = Color(0xFF0B1119)
private val RoyalNight = Color(0xFF0C1E33)
private val WarmGold = Color(0xFFC29C57)
private val PremiumGold = Color(0xFFD8B56A)
private val LuminousTurquoise = Color(0xFF1FD0C5)
private val PrimaryText = Color(0xFFFFFFFF)
private val SecondaryText = Color(0xFFA0A0A0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdhanSettingsScreen(colors: CustomThemeColors, viewModel: MainViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    val initialUri = remember(prefs) {
        prefs.getString(KEY_SOUND_URI, null)?.let { Uri.parse(it) }
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
    }
    var soundUri by remember { mutableStateOf(initialUri) }
    var vibrateEnabled by remember { mutableStateOf(prefs.getBoolean(KEY_VIBRATE, true)) }
    val initialToggles = remember(prefs) {
        PRAYER_TOGGLES.associate { it.key to prefs.getBoolean(it.key, false) }
    }
    var togglesState by remember { mutableStateOf(initialToggles) }

    val initialAdhanVoice = remember(prefs) { prefs.getString(KEY_ADHAN_VOICE, "system") ?: "system" }
    var selectedAdhanVoice by remember { mutableStateOf(initialAdhanVoice) }
    val initialQuranVoice = remember(prefs) { prefs.getString(KEY_QURAN_VOICE, "ar.alafasy") ?: "ar.alafasy" }
    var selectedQuranVoice by remember { mutableStateOf(initialQuranVoice) }

    var showAdhanVoiceDropdown by remember { mutableStateOf(false) }
    var showQuranVoiceDropdown by remember { mutableStateOf(false) }

    // Dynamic Download States
    var downloadProgress by remember { mutableStateOf(0) }
    var isDownloading by remember { mutableStateOf(false) }
    var cacheSizeMb by remember { mutableStateOf(0.0) }

    // Check sizes on launch/refresh
    LaunchedEffect(selectedAdhanVoice) {
        cacheSizeMb = FileDownloader.getTotalCacheSizeInMb(context)
    }

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
            if (uri != null) {
                soundUri = uri
                prefs.edit().putString(KEY_SOUND_URI, uri.toString()).apply()
            }
        }
    }

    fun openSoundPicker() {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, soundUri)
        }
        ringtonePicker.launch(intent)
    }

    // High performance playback (uses cached local file first, falls back to web streaming)
    fun previewSound() {
        val uriToPlay: Uri = if (selectedAdhanVoice == "system") {
            soundUri ?: return
        } else {
            val localFile = FileDownloader.getLocalFile(context, "$selectedAdhanVoice.mp3")
            if (localFile != null) {
                Uri.fromFile(localFile)
            } else {
                // Online backup streaming
                val webUrl = FileDownloader.VOICE_URLS[selectedAdhanVoice] ?: return
                Uri.parse(webUrl)
            }
        }

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
        } catch (_: Exception) {}
    }

    // Handles the automatic caching flow: Check Cache -> Exists? -> Play/Download -> Play
    fun handleReciterSelect(voiceKey: String) {
        selectedAdhanVoice = voiceKey
        prefs.edit().putString(KEY_ADHAN_VOICE, voiceKey).apply()

        if (voiceKey != "system" && !FileDownloader.isVoiceCached(context, voiceKey)) {
            // Trigger background download with real-time UI progress feedback
            coroutineScope.launch {
                isDownloading = true
                downloadProgress = 0
                val downloadedFile = withContext(Dispatchers.IO) {
                    FileDownloader.downloadAdhanVoice(context, voiceKey) { progress ->
                        downloadProgress = progress
                    }
                }
                isDownloading = false
                cacheSizeMb = FileDownloader.getTotalCacheSizeInMb(context)
            }
        }
    }

    ToolScreenScaffold(
        colors = colors,
        icon = AppIcons.forCalc(CalcKey.ADHAN_SETTINGS),
        title = "مركز التحكم بالصوت والأذان",
        subtitle = "تخصيص تنبيهات مواقيت الصلاة والمؤذنين وإدارة ملفات التخزين",
        isScrollable = false
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // --- 1. SELECTED ADHAN VOICE CARD (PRAYER CONTROL CENTER) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.02f))
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("صوت مؤذن الأذان المفضل", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PrimaryText)
                            Text("تحديد المؤذن المعتمد للصلوات الخمس", fontSize = 12.sp, color = SecondaryText)
                        }

                        // Preview player button
                        IconButton(
                            onClick = { previewSound() },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(LuminousTurquoise.copy(alpha = 0.1f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "تشغيل تجريبي",
                                tint = LuminousTurquoise
                            )
                        }
                    }

                    // Dropdown selector
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            color = Color.White.copy(alpha = 0.04f),
                            shape = RoundedCornerShape(14.dp),
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
                                Text(activeLabel, fontSize = 13.sp, color = PrimaryText)
                                Text("▼", fontSize = 10.sp, color = PremiumGold)
                            }
                        }

                        DropdownMenu(
                            expanded = showAdhanVoiceDropdown,
                            onDismissRequest = { showAdhanVoiceDropdown = false },
                            modifier = Modifier.background(Color(0xFF1E1E1E))
                        ) {
                            adhanVoices.forEach { voice ->
                                DropdownMenuItem(
                                    text = { Text(voice.second, color = PrimaryText) },
                                    onClick = {
                                        handleReciterSelect(voice.first)
                                        showAdhanVoiceDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Download progress and cache status info
                    if (selectedAdhanVoice != "system") {
                        val isCached = FileDownloader.isVoiceCached(context, selectedAdhanVoice)
                        if (isDownloading) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("جاري تحميل صوت الأذان...", fontSize = 12.sp, color = LuminousTurquoise, fontWeight = FontWeight.Bold)
                                    Text("$downloadProgress%", fontSize = 12.sp, color = LuminousTurquoise, fontWeight = FontWeight.Bold)
                                }
                                LinearProgressIndicator(
                                    progress = { downloadProgress / 100f },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                                    color = LuminousTurquoise,
                                    trackColor = Color.White.copy(alpha = 0.1f)
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isCached) LuminousTurquoise.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.02f))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isCached) LuminousTurquoise else WarmGold)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isCached) "تم التحميل وجاهز للتشغيل دون اتصال ✓" else "ملف الأذان غير محمل، سيتم التحميل التلقائي.",
                                    fontSize = 11.sp,
                                    color = if (isCached) LuminousTurquoise else SecondaryText
                                )
                            }
                        }
                    }
                }
            }

            // --- 2. CACHE MANAGEMENT PANEL ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.015f))
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.04f)), RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("إدارة الذاكرة المؤقتة للأصوات", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PrimaryText)
                        Text(
                            text = String.format("المساحة المستخدمة حالياً: %.2f MB", cacheSizeMb),
                            fontSize = 12.sp,
                            color = SecondaryText
                        )
                    }

                    // Delete button to empty files
                    Button(
                        onClick = {
                            FileDownloader.clearAllVoiceCache(context)
                            cacheSizeMb = 0.0
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "مسح الملفات",
                            tint = Color.Red,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("حذف الملفات", color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // --- 3. PRAYER TIME ALERT TOGGLES ---
            Text("تفعيل تنبيهات الصلوات والأذان", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PrimaryText)

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PRAYER_TOGGLES.forEach { toggle ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.02f))
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(PremiumGold.copy(alpha = 0.08f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = AppIcons.forCalc(CalcKey.PRAYER),
                                        contentDescription = null,
                                        tint = PremiumGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(toggle.label, fontSize = 15.sp, color = PrimaryText, fontWeight = FontWeight.Bold)
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
                                    checkedTrackColor = PremiumGold,
                                    uncheckedThumbColor = SecondaryText,
                                    uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                                )
                            )
                        }
                    }
                }
            }

            // --- 4. CUSTOM ALARM VIBRATION & SYSTEM PICKER ---
            Text("نغمات النظام والاهتزاز", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PrimaryText)

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Sound System picker
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.02f))
                        .clickable { openSoundPicker() }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("نغمة المنبه الافتراضية", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PrimaryText)
                            Text(
                                RingtoneManager.getRingtone(context, soundUri)?.getTitle(context) ?: "افتراضي النظام",
                                fontSize = 12.sp,
                                color = SecondaryText
                            )
                        }
                        Text("تغيير ➔", color = PremiumGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Vibrate toggle
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.02f))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📳", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("الاهتزاز مع التنبيه بالأذان", fontSize = 14.sp, color = PrimaryText, fontWeight = FontWeight.Bold)
                        }
                        Switch(
                            checked = vibrateEnabled,
                            onCheckedChange = {
                                vibrateEnabled = it
                                prefs.edit().putBoolean(KEY_VIBRATE, it).apply()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PremiumGold,
                                uncheckedThumbColor = SecondaryText,
                                uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            }
        }
    }
}
