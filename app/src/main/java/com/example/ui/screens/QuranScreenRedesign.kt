package com.example.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.IslamicData
import com.example.data.SurahInfo
import com.example.ui.theme.CustomThemeColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranScreenRedesign(colors: CustomThemeColors) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val quranPrefs = remember { context.getSharedPreferences("clevcalc_quran_prefs", Context.MODE_PRIVATE) }

    // Persistent State reading
    var lastSurahNum by remember { mutableStateOf(quranPrefs.getInt("last_surah_num", 1)) }
    var lastSurahName by remember { mutableStateOf(quranPrefs.getString("last_surah_name", "الفاتحة") ?: "الفاتحة") }
    var lastAyahNum by remember { mutableStateOf(quranPrefs.getInt("last_ayah_num", 1)) }

    // Independent Daily Wird Preferences
    var wirdSurahNum by remember { mutableStateOf(quranPrefs.getInt("wird_surah_num", 1)) }
    var wirdSurahName by remember { mutableStateOf(quranPrefs.getString("wird_surah_name", "الفاتحة") ?: "الفاتحة") }
    var wirdAyahNum by remember { mutableStateOf(quranPrefs.getInt("wird_ayah_num", 1)) }
    var wirdStreak by remember { mutableStateOf(quranPrefs.getInt("wird_streak", 5)) }
    var wirdDoneToday by remember { mutableStateOf(quranPrefs.getBoolean("wird_done_today", false)) }

    // Favorites toggling list
    val initialFavs = remember(quranPrefs) {
        quranPrefs.getStringSet("favorite_surahs", setOf("1", "18", "36", "67")) ?: setOf("1", "18", "36", "67")
    }
    var favoritesSet by remember { mutableStateOf(initialFavs) }

    // Reciter Preferences
    val initialReciterKey = remember(quranPrefs) {
        quranPrefs.getString("quran_voice_key", "ar.alafasy") ?: "ar.alafasy"
    }
    var selectedReciterKey by remember { mutableStateOf(initialReciterKey) }
    var showSettingsModal by remember { mutableStateOf(false) }

    // Toggle Favorite helper
    val toggleFavorite: (Int) -> Unit = { num ->
        val current = favoritesSet.toMutableSet()
        val numStr = num.toString()
        if (current.contains(numStr)) current.remove(numStr) else current.add(numStr)
        quranPrefs.edit().putStringSet("favorite_surahs", current).apply()
        favoritesSet = current
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf(QuranFilterCategory.ALL) }
    var selectedSurah by remember { mutableStateOf<SurahInfo?>(null) }
    var isDailyWirdMode by remember { mutableStateOf(false) }

    // Colors Palette
    val cardGlassBg = Color(0xFF151A22).copy(alpha = 0.85f)
    val mintGlow = Color(0xFF63F4DD)
    val royalGold = Color(0xFFD8B56A)
    val secondaryText = Color(0xFFBFC8D2)
    val borderOverlay = Color.White.copy(alpha = 0.08f)

    // Infinite Floating Motion for Hero Card
    val infiniteTransition = rememberInfiniteTransition(label = "HeroFloat")
    val heroOffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "HeroFloatOffset"
    )

    BackHandler(enabled = selectedSurah != null) {
        selectedSurah = null
    }

    if (showSettingsModal) {
        QuranSettingsBottomSheet(
            selectedReciterKey = selectedReciterKey,
            onSelectReciter = { key ->
                selectedReciterKey = key
                quranPrefs.edit().putString("quran_voice_key", key).apply()
            },
            onDismiss = { showSettingsModal = false }
        )
    }

    if (selectedSurah != null) {
        SurahDetailReader(
            surah = selectedSurah!!,
            colors = colors,
            isDailyWird = isDailyWirdMode,
            onBack = {
                selectedSurah = null
                lastSurahNum = quranPrefs.getInt("last_surah_num", 1)
                lastSurahName = quranPrefs.getString("last_surah_name", "الفاتحة") ?: "الفاتحة"
                lastAyahNum = quranPrefs.getInt("last_ayah_num", 1)
                wirdSurahNum = quranPrefs.getInt("wird_surah_num", 1)
                wirdSurahName = quranPrefs.getString("wird_surah_name", "الفاتحة") ?: "الفاتحة"
                wirdAyahNum = quranPrefs.getInt("wird_ayah_num", 1)
                wirdStreak = quranPrefs.getInt("wird_streak", 5)
                wirdDoneToday = quranPrefs.getBoolean("wird_done_today", false)
            },
            onSelectSurah = { next ->
                selectedSurah = next
                if (isDailyWirdMode) {
                    quranPrefs.edit()
                        .putInt("wird_surah_num", next.number)
                        .putString("wird_surah_name", next.nameAr)
                        .putInt("wird_ayah_num", 1)
                        .apply()
                } else {
                    quranPrefs.edit()
                        .putInt("last_surah_num", next.number)
                        .putString("last_surah_name", next.nameAr)
                        .putInt("last_ayah_num", 1)
                        .apply()
                }
            }
        )
    } else {
        val filteredSurahs = remember(searchQuery, activeFilter, favoritesSet) {
            val baseList = when (activeFilter) {
                QuranFilterCategory.ALL -> IslamicData.surahs
                QuranFilterCategory.FAVORITES -> IslamicData.surahs.filter { favoritesSet.contains(it.number.toString()) }
                QuranFilterCategory.BOOKMARKS -> IslamicData.surahs.filter { it.number == lastSurahNum || favoritesSet.contains(it.number.toString()) }
                QuranFilterCategory.RECENT -> IslamicData.surahs.filter { it.number == lastSurahNum || it.number == wirdSurahNum }
                QuranFilterCategory.DAILY_WIRD -> IslamicData.surahs.filter { it.number == wirdSurahNum }
                QuranFilterCategory.PLAN -> IslamicData.surahs.take(10)
            }

            if (searchQuery.isBlank()) baseList
            else baseList.filter {
                it.nameAr.contains(searchQuery.trim()) ||
                it.nameEn.lowercase().contains(searchQuery.trim().lowercase()) ||
                it.number.toString() == searchQuery.trim()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF090B10), Color(0xFF151A22))
                    )
                )
        ) {
            ProceduralIslamicBackground(
                modifier = Modifier.fillMaxSize(),
                goldColor = royalGold
            )

            LazyColumn(
                contentPadding = PaddingValues(
                    top = 16.dp,
                    bottom = 120.dp,
                    start = 16.dp,
                    end = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Top App Header Bar
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(cardGlassBg)
                            .border(1.dp, borderOverlay, RoundedCornerShape(20.dp))
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "القرآن الكريم",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = royalGold.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, royalGold.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = "برسم المصحف",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = royalGold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "تلاوة وتدبر لآيات الذكر الحكيم",
                                fontSize = 12.sp,
                                color = secondaryText,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showSettingsModal = true
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.06f))
                                .border(1.dp, borderOverlay, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "إعدادات التلاوة",
                                tint = royalGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // HERO CARD: Continue Reading
                item {
                    val activeSurahObj = IslamicData.surahs.find { it.number == lastSurahNum } ?: IslamicData.surahs.first()
                    val totalVerses = activeSurahObj.totalVerses
                    val progressRatio = (lastAyahNum.toFloat() / maxOf(1, totalVerses)).coerceIn(0.05f, 1.0f)
                    val estMinutes = maxOf(2, (totalVerses - lastAyahNum) / 12)

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { translationY = heroOffsetY }
                            .clip(RoundedCornerShape(26.dp)),
                        color = cardGlassBg,
                        border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(royalGold.copy(alpha = 0.6f), mintGlow.copy(alpha = 0.3f)))),
                        shadowElevation = 10.dp
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = royalGold.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, royalGold.copy(alpha = 0.3f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = royalGold,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "متابعة القراءة 📖",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = royalGold
                                            )
                                        }
                                    }

                                    Surface(
                                        color = if (wirdDoneToday) Color(0xFF50E3A4).copy(alpha = 0.18f) else mintGlow.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, if (wirdDoneToday) Color(0xFF50E3A4) else mintGlow.copy(alpha = 0.3f))
                                    ) {
                                        Text(
                                            text = if (wirdDoneToday) "✅ تم ورد اليوم" else "🔥 سلسلة $wirdStreak أيام",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (wirdDoneToday) Color(0xFF50E3A4) else mintGlow,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "سورة ${activeSurahObj.nameAr}",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )

                                Text(
                                    text = "وصلت للآية رقم $lastAyahNum من أصل $totalVerses آية • متبقي حوالي $estMinutes دقائق",
                                    fontSize = 12.sp,
                                    color = secondaryText,
                                    modifier = Modifier.padding(top = 4.dp)
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("نسبة الإنجاز", fontSize = 10.sp, color = secondaryText)
                                        Text("${(progressRatio * 100).toInt()}%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = mintGlow)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.1f))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(progressRatio)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.horizontalGradient(listOf(royalGold, mintGlow))
                                                )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                Button(
                                    onClick = {
                                        isDailyWirdMode = false
                                        selectedSurah = activeSurahObj
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(16.dp)),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(royalGold, mintGlow)
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "ابدأ القراءة الآن",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF090B10)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Icon(
                                                imageVector = Icons.Default.ArrowForward,
                                                contentDescription = null,
                                                tint = Color(0xFF090B10),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Premium Search Bar
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(cardGlassBg),
                        placeholder = {
                            Text(
                                "ابحث باسم السورة، بالإنجليزية، أو رقمها...",
                                fontSize = 12.sp,
                                color = secondaryText
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "بحث",
                                tint = royalGold
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "مسح",
                                        tint = secondaryText
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = royalGold,
                            unfocusedBorderColor = borderOverlay,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(22.dp)
                    )
                }

                // Quick Action Chips Filter
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(QuranFilterCategory.values()) { category ->
                            val isSelected = activeFilter == category
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    activeFilter = category
                                },
                                label = {
                                    Text(
                                        text = category.title,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color(0xFF090B10) else Color.White
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = category.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color(0xFF090B10) else royalGold,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = royalGold,
                                    containerColor = cardGlassBg
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = borderOverlay,
                                    selectedBorderColor = royalGold
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }
                }

                // Favorite Surahs Carousel Row
                if (favoritesSet.isNotEmpty() && searchQuery.isBlank() && activeFilter == QuranFilterCategory.ALL) {
                    item {
                        Column {
                            Text(
                                text = "السور المفضلة ⭐",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val favSurahs = IslamicData.surahs.filter { favoritesSet.contains(it.number.toString()) }
                                items(favSurahs) { surah ->
                                    Surface(
                                        onClick = {
                                            isDailyWirdMode = false
                                            selectedSurah = surah
                                        },
                                        modifier = Modifier
                                            .width(150.dp)
                                            .clip(RoundedCornerShape(20.dp)),
                                        color = cardGlassBg,
                                        border = BorderStroke(1.dp, borderOverlay)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RubElHizbOrnament(
                                                    number = surah.number,
                                                    modifier = Modifier.size(32.dp),
                                                    goldColor = royalGold
                                                )
                                                IconButton(
                                                    onClick = { toggleFavorite(surah.number) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Star,
                                                        contentDescription = "مفضلة",
                                                        tint = royalGold,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                text = "سورة ${surah.nameAr}",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "${surah.totalVerses} آية • ${surah.place}",
                                                fontSize = 10.sp,
                                                color = secondaryText,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Surahs List / Empty State
                if (filteredSurahs.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = royalGold.copy(alpha = 0.5f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "لم يتم العثور على نتائج للبحث",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "تأكد من كتابة اسم السورة أو رقمها بشكل صحيح",
                                fontSize = 12.sp,
                                color = secondaryText,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    searchQuery = ""
                                    activeFilter = QuranFilterCategory.ALL
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = royalGold)
                            ) {
                                Text("عرض كل السور", color = Color(0xFF090B10), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    items(filteredSurahs, key = { it.number }) { surah ->
                        val isFav = favoritesSet.contains(surah.number.toString())
                        val isCurrentLastRead = surah.number == lastSurahNum

                        Surface(
                            onClick = {
                                isDailyWirdMode = false
                                selectedSurah = surah
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp)),
                            color = if (isCurrentLastRead) royalGold.copy(alpha = 0.12f) else cardGlassBg,
                            border = BorderStroke(
                                1.dp,
                                if (isCurrentLastRead) royalGold.copy(alpha = 0.5f) else borderOverlay
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    RubElHizbOrnament(
                                        number = surah.number,
                                        modifier = Modifier.size(42.dp),
                                        goldColor = if (isCurrentLastRead) royalGold else Color(0xFFD8B56A).copy(alpha = 0.7f)
                                    )

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "سورة ${surah.nameAr}",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            if (isCurrentLastRead) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    color = mintGlow.copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = "مفتوح الآن 📍",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = mintGlow,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.padding(top = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${surah.nameEn} • ${surah.totalVerses} آية",
                                                fontSize = 11.sp,
                                                color = secondaryText
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                color = Color.White.copy(alpha = 0.08f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = surah.place,
                                                    fontSize = 9.sp,
                                                    color = royalGold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { toggleFavorite(surah.number) }) {
                                        Icon(
                                            imageVector = if (isFav) Icons.Default.Star else Icons.Outlined.StarBorder,
                                            contentDescription = "المفضلة",
                                            tint = if (isFav) royalGold else secondaryText.copy(alpha = 0.5f),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = secondaryText.copy(alpha = 0.5f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
