package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CustomThemeColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

// ==========================================
// 1. CYBER OBSIDIAN & CHAMPAGNE GOLD PALETTE
// ==========================================

private val ObsidianBgStart = Color(0xFF080A0F)
private val ObsidianBgEnd = Color(0xFF121620)
private val GlassSurfaceDark = Color(0xFF141926)
private val GlassCardHighlight = Color(0xFF1A1D2E)
private val ChampagneGold = Color(0xFFD4AF37)
private val GlowingAmber = Color(0xFFF59E0B)
private val IceCyan = Color(0xFF00F2FE)
private val EmeraldGreen = Color(0xFF10B981)
private val CrimsonRed = Color(0xFFEF4444)
private val SoftSunYellow = Color(0xFFFBBF24)
private val DeepIndigoNight = Color(0xFF6366F1)
private val MutedSlateText = Color(0xFF94A3B8)
private val SecondaryText = Color(0xFFBFC8D2)

// ==========================================
// 2. DATA MODELS
// ==========================================

data class WorldCity(
    val id: String,
    val cityNameAr: String,
    val cityNameEn: String,
    val countryNameAr: String,
    val flagEmoji: String,
    val timezoneId: String,
    val regionAr: String
)

data class TimeSimulationHistory(
    val id: String = java.util.UUID.randomUUID().toString(),
    val baseCityName: String,
    val simulatedTimeStr: String,
    val simulatedMinuteOffset: Int,
    val timestamp: String
)

data class CityGroup(
    val name: String,
    val cityIds: List<String>
)

// Global database of popular world cities with timezone IDs
val GLOBAL_WORLD_CITIES = listOf(
    WorldCity("cairo", "القاهرة", "Cairo", "مصر", "🇪🇬", "Africa/Cairo", "أفريقيا"),
    WorldCity("mecca", "مكة المكرمة", "Mecca", "المملكة العربية السعودية", "🇸🇦", "Asia/Riyadh", "الشرق الأوسط"),
    WorldCity("riyadh", "الرياض", "Riyadh", "المملكة العربية السعودية", "🇸🇦", "Asia/Riyadh", "الشرق الأوسط"),
    WorldCity("dubai", "دبي", "Dubai", "الإمارات العربية المتحدة", "🇦🇪", "Asia/Dubai", "الشرق الأوسط"),
    WorldCity("kuwait", "الكويت", "Kuwait City", "الكويت", "🇰🇼", "Asia/Kuwait", "الشرق الأوسط"),
    WorldCity("doha", "الدوحة", "Doha", "قطر", "🇶🇦", "Asia/Qatar", "الشرق الأوسط"),
    WorldCity("amman", "عمان", "Amman", "الأردن", "🇯🇴", "Asia/Amman", "الشرق الأوسط"),
    WorldCity("baghdad", "بغداد", "Baghdad", "العراق", "🇮🇶", "Asia/Baghdad", "الشرق الأوسط"),
    WorldCity("damascus", "دمشق", "Damascus", "سوريا", "🇸🇾", "Asia/Damascus", "الشرق الأوسط"),
    WorldCity("beirut", "بيروت", "Beirut", "لبنان", "🇱🇧", "Asia/Beirut", "الشرق الأوسط"),
    WorldCity("jerusalem", "القدس الشريف", "Jerusalem", "فلسطين", "🇵🇸", "Asia/Gaza", "الشرق الأوسط"),
    WorldCity("khartoum", "الخرطوم", "Khartoum", "السودان", "🇸🇩", "Africa/Khartoum", "أفريقيا"),
    WorldCity("sanaa", "صنعاء", "Sana'a", "اليمن", "🇾🇪", "Asia/Aden", "الشرق الأوسط"),
    WorldCity("muscat", "مسقط", "Muscat", "عمان", "🇴🇲", "Asia/Muscat", "الشرق الأوسط"),
    WorldCity("manama", "المنامة", "Manama", "البحرين", "🇧🇭", "Asia/Bahrain", "الشرق الأوسط"),
    WorldCity("tripoli", "طرابلس", "Tripoli", "ليبيا", "🇱🇾", "Africa/Tripoli", "أفريقيا"),
    WorldCity("tunis", "تونس", "Tunis", "تونس", "🇹🇳", "Africa/Tunis", "أفريقيا"),
    WorldCity("algiers", "الجزائر", "Algiers", "الجزائر", "🇩🇿", "Africa/Algiers", "أفريقيا"),
    WorldCity("rabat", "الرباط", "Rabat", "المغرب", "🇲🇦", "Africa/Casablanca", "أفريقيا"),
    WorldCity("london", "لندن", "London", "المملكة المتحدة", "🇬🇧", "Europe/London", "أوروبا"),
    WorldCity("paris", "باريس", "Paris", "فرنسا", "🇫🇷", "Europe/Paris", "أوروبا"),
    WorldCity("istanbul", "إسطنبول", "Istanbul", "تركيا", "🇹🇷", "Europe/Istanbul", "أوروبا"),
    WorldCity("new_york", "نيويورك", "New York", "الولايات المتحدة", "🇺🇸", "America/New_York", "الأمريكتان"),
    WorldCity("tokyo", "طوكيو", "Tokyo", "اليابان", "🇯🇵", "Asia/Tokyo", "آسيا"),
    WorldCity("sydney", "سيدني", "Sydney", "أستراليا", "🇦🇺", "Australia/Sydney", "أوقيانوسيا"),
    WorldCity("moscow", "موسكو", "Moscow", "روسيا", "🇷🇺", "Europe/Moscow", "أوروبا"),
    WorldCity("beijing", "بكين", "Beijing", "الصين", "🇨🇳", "Asia/Shanghai", "آسيا"),
    WorldCity("toronto", "تورونتو", "Toronto", "كندا", "🇨🇦", "America/Toronto", "الأمريكتان"),
    WorldCity("berlin", "برلين", "Berlin", "ألمانيا", "🇩🇪", "Europe/Berlin", "أوروبا"),
    WorldCity("rome", "روما", "Rome", "إيطاليا", "🇮🇹", "Europe/Rome", "أوروبا")
)

// Default city groups
val DEFAULT_CITY_GROUPS = listOf(
    CityGroup("الكل", GLOBAL_WORLD_CITIES.map { it.id }),
    CityGroup("العالم العربي", listOf("cairo", "mecca", "riyadh", "dubai", "kuwait", "doha", "amman", "baghdad", "damascus", "beirut", "jerusalem", "khartoum", "sanaa", "muscat", "manama", "tripoli", "tunis", "algiers", "rabat")),
    CityGroup("الأعمال الدولية", listOf("london", "paris", "new_york", "tokyo", "beijing", "sydney", "toronto", "berlin", "moscow", "istanbul")),
    CityGroup("أوروبا والأمريكتان", listOf("london", "paris", "berlin", "rome", "istanbul", "moscow", "new_york", "toronto"))
)

// ==========================================
// 3. MAIN WORLD TIME SCREEN
// ==========================================

@Composable
fun WorldTimeScreen(colors: CustomThemeColors) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    // Real-time ticking engine using StateFlow effect
    var nowInstant by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowInstant = Instant.now()
            delay(1000)
        }
    }

    // Settings & State Management
    var is24HourFormat by rememberSaveable { mutableStateOf(false) }
    var baseCityId by rememberSaveable { mutableStateOf("cairo") }
    var timeScrubberMinuteOffset by rememberSaveable { mutableIntStateOf(0) } // 0 = real-time, +/- minutes
    var selectedGroupIndex by rememberSaveable { mutableIntStateOf(0) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedRegionFilter by rememberSaveable { mutableStateOf("الكل") }

    // Pinned City IDs List
    var pinnedCityIds by rememberSaveable {
        mutableStateOf(listOf("cairo", "mecca", "dubai", "london", "new_york", "tokyo"))
    }

    // UI Drawers & Dialog States
    var showAddCityDialog by remember { mutableStateOf(false) }
    var showTimeDiffCalcCard by remember { mutableStateOf(false) }
    var showBreakdownCard by remember { mutableStateOf(false) }
    var showHistoryDrawer by remember { mutableStateOf(false) }

    // History Log
    var historyList by remember { mutableStateOf(listOf<TimeSimulationHistory>()) }

    // Time Difference Calculator selections
    var diffCityAId by rememberSaveable { mutableStateOf("cairo") }
    var diffCityBId by rememberSaveable { mutableStateOf("dubai") }

    // UI Loading & Error Simulation State
    var isLoading by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(400) // Smooth initial load skeleton
        isLoading = false
    }

    // Base Reference City Object
    val baseCity = remember(baseCityId) {
        GLOBAL_WORLD_CITIES.find { it.id == baseCityId } ?: GLOBAL_WORLD_CITIES.first()
    }

    // Calculated Base ZonedDateTime considering real-time + scrubber offset
    val baseZonedDateTime = remember(nowInstant, baseCity, timeScrubberMinuteOffset) {
        val baseZone = try { ZoneId.of(baseCity.timezoneId) } catch (e: Exception) { ZoneId.systemDefault() }
        ZonedDateTime.ofInstant(nowInstant, baseZone).plusMinutes(timeScrubberMinuteOffset.toLong())
    }

    // Filtered Cities for Main Display
    val activeCityList by remember(pinnedCityIds, searchQuery, selectedRegionFilter, selectedGroupIndex) {
        derivedStateOf {
            val group = DEFAULT_CITY_GROUPS.getOrNull(selectedGroupIndex)
            val currentBaseList = if (selectedGroupIndex == 0 || group?.name == "الكل") {
                GLOBAL_WORLD_CITIES
            } else {
                val groupCityIds = group?.cityIds ?: emptyList()
                GLOBAL_WORLD_CITIES.filter { city ->
                    groupCityIds.contains(city.id) || pinnedCityIds.contains(city.id)
                }
            }

            currentBaseList.filter { city ->
                val matchesSearch = searchQuery.isBlank() ||
                        city.cityNameAr.contains(searchQuery, ignoreCase = true) ||
                        city.cityNameEn.contains(searchQuery, ignoreCase = true) ||
                        city.countryNameAr.contains(searchQuery, ignoreCase = true)

                val matchesRegion = selectedRegionFilter == "الكل" || city.regionAr == selectedRegionFilter

                matchesSearch && matchesRegion
            }.distinctBy { it.id }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(ObsidianBgStart, ObsidianBgEnd)
                )
            )
    ) {
        // --- A. Procedural World Longitude Lines & Glow Canvas ---
        ProceduralWorldMapCanvas()

        // --- B. Main Screen Unified LazyColumn Layout ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 1. Top Bar Controls Header
            WorldClockTopHeader(
                title = "التوقيت العالمي الذكي",
                subtitle = "متابعة الوقت ومحاكاة التوقيتات عالمياً",
                onBackClick = { /* Back handled by root navigation */ }
            )

            // 2. Offline Mode Banner
            WorldClockOfflineBanner()

            LazyColumn(
                contentPadding = PaddingValues(bottom = 90.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // ITEM 1: Header Banner & Global Control Chips Row
                item(key = "header_controls") {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        WorldClockHeroBanner()

                        Spacer(modifier = Modifier.height(12.dp))

                        // Global Control Chips Bar (12H/24H, Reset, Add City, Groups)
                        WorldClockGlobalControlChips(
                            is24HourFormat = is24HourFormat,
                            isScrubberActive = timeScrubberMinuteOffset != 0,
                            selectedGroupIndex = selectedGroupIndex,
                            onToggleFormat = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                is24HourFormat = !is24HourFormat
                            },
                            onResetToRealtime = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                timeScrubberMinuteOffset = 0
                            },
                            onOpenAddCity = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showAddCityDialog = true
                            },
                            onSelectGroup = { idx ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedGroupIndex = idx
                            }
                        )
                    }
                }

                // ITEM 2: Time-Travel Scrubber Engine Card
                item(key = "time_travel_scrubber") {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        TimeTravelScrubberCard(
                            baseCity = baseCity,
                            allCities = GLOBAL_WORLD_CITIES,
                            baseZonedDateTime = baseZonedDateTime,
                            minuteOffset = timeScrubberMinuteOffset,
                            is24HourFormat = is24HourFormat,
                            onSelectBaseCity = { city ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                baseCityId = city.id
                            },
                            onScrubberChange = { newOffset ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                timeScrubberMinuteOffset = newOffset
                            },
                            onQuickPreset = { presetOffset ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                timeScrubberMinuteOffset = presetOffset

                                // Log simulation to history
                                val formatter = if (is24HourFormat) DateTimeFormatter.ofPattern("HH:mm") else DateTimeFormatter.ofPattern("hh:mm a", Locale("ar"))
                                historyList = (listOf(
                                    TimeSimulationHistory(
                                        baseCityName = baseCity.cityNameAr,
                                        simulatedTimeStr = baseZonedDateTime.plusMinutes((presetOffset - timeScrubberMinuteOffset).toLong()).format(formatter),
                                        simulatedMinuteOffset = presetOffset,
                                        timestamp = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss a", Locale("ar")))
                                    )
                                ) + historyList).take(20)
                            },
                            onReset = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                timeScrubberMinuteOffset = 0
                            }
                        )
                    }
                }

                // ITEM 3: Expandable Time Difference Calculator & Breakdown Toggles
                item(key = "calculators_toggles") {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Time Difference Calc Toggle
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showTimeDiffCalcCard = !showTimeDiffCalcCard },
                                shape = RoundedCornerShape(16.dp),
                                color = GlassSurfaceDark.copy(alpha = 0.85f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CompareArrows,
                                        contentDescription = null,
                                        tint = ChampagneGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "حاسبة الفرق بين مدينتين",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            // Step-by-Step Breakdown Toggle
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showBreakdownCard = !showBreakdownCard },
                                shape = RoundedCornerShape(16.dp),
                                color = GlassSurfaceDark.copy(alpha = 0.85f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, IceCyan.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Calculate,
                                        contentDescription = null,
                                        tint = IceCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "تفصيل المعادلة الزمانية",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // Expandable Time Difference Calculator Card
                        AnimatedVisibility(
                            visible = showTimeDiffCalcCard,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Spacer(modifier = Modifier.height(10.dp))
                            TimeDifferenceCalculatorCard(
                                allCities = GLOBAL_WORLD_CITIES,
                                selectedCityAId = diffCityAId,
                                selectedCityBId = diffCityBId,
                                baseZonedDateTime = baseZonedDateTime,
                                onSelectCityA = { diffCityAId = it },
                                onSelectCityB = { diffCityBId = it }
                            )
                        }

                        // Expandable Step-by-Step Breakdown Card
                        AnimatedVisibility(
                            visible = showBreakdownCard,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Spacer(modifier = Modifier.height(10.dp))
                            StepByStepBreakdownCard(
                                baseCity = baseCity,
                                baseZonedDateTime = baseZonedDateTime,
                                targetCity = GLOBAL_WORLD_CITIES.find { it.id == diffCityBId } ?: GLOBAL_WORLD_CITIES[3]
                            )
                        }
                    }
                }

                // ITEM 4: Search & Region Filter Panel
                item(key = "search_and_filters") {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        WorldClockSearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            onClearSearch = { searchQuery = "" }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Region Filters LazyRow
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val regions = listOf("الكل", "الشرق الأوسط", "أفريقيا", "أوروبا", "آسيا", "الأمريكتان", "أوقيانوسيا")
                            items(regions) { region ->
                                val isSelected = selectedRegionFilter == region
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedRegionFilter = region
                                    },
                                    label = { Text(region, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ChampagneGold,
                                        selectedLabelColor = ObsidianBgStart,
                                        containerColor = GlassSurfaceDark,
                                        labelColor = Color.White
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = ChampagneGold.copy(alpha = 0.3f),
                                        selectedBorderColor = ChampagneGold,
                                        enabled = true,
                                        selected = isSelected
                                    )
                                )
                            }
                        }
                    }
                }

                // ITEM 5: History Log & Drawer Toggle
                item(key = "history_drawer_toggle") {
                    if (historyList.isNotEmpty()) {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showHistoryDrawer = !showHistoryDrawer },
                                shape = RoundedCornerShape(16.dp),
                                color = GlassSurfaceDark.copy(alpha = 0.85f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GlowingAmber.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = null,
                                            tint = GlowingAmber,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "سجل المحاكاة والتجارب الزمنية (${historyList.size})",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }

                                    Icon(
                                        imageVector = if (showHistoryDrawer) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = GlowingAmber
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = showHistoryDrawer,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                historyList.forEach { history ->
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        color = GlassCardHighlight.copy(alpha = 0.6f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.2f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(
                                                    text = "محاكاة: ${history.baseCityName} @ ${history.simulatedTimeStr}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = "تمت في: ${history.timestamp}",
                                                    fontSize = 10.sp,
                                                    color = MutedSlateText
                                                )
                                            }

                                            TextButton(
                                                onClick = {
                                                    timeScrubberMinuteOffset = history.simulatedMinuteOffset
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                }
                                            ) {
                                                Text("إعادة تطبيق", fontSize = 11.sp, color = ChampagneGold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ITEM 6: Section Header for Pinned World Cities
                item(key = "section_header_cities") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(18.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(ChampagneGold)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ساعات المدن النشطة (${activeCityList.size})",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        TextButton(onClick = { showAddCityDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إضافة مدينة", fontSize = 12.sp, color = ChampagneGold, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // ITEM 7: 5 CORE UI STATES & CITY CARDS LIST
                when {
                    // State 1: Skeleton Loading
                    isLoading -> {
                        items(4, key = { "skeleton_$it" }) {
                            WorldClockSkeletonCard()
                        }
                    }

                    // State 2: Error State
                    isError -> {
                        item(key = "error_state") {
                            WorldClockErrorStateCard(
                                onRetry = {
                                    isLoading = true
                                    isError = false
                                }
                            )
                        }
                    }

                    // State 3: Empty Cities List
                    activeCityList.isEmpty() -> {
                        item(key = "empty_state") {
                            WorldClockEmptyStateCard(
                                onAddCityClick = { showAddCityDialog = true }
                            )
                        }
                    }

                    // State 4: Success State (Active World Clocks)
                    else -> {
                        items(activeCityList, key = { it.id }) { city ->
                            val isPinned = pinnedCityIds.contains(city.id)

                            WorldCityClockCard(
                                city = city,
                                baseCity = baseCity,
                                baseZonedDateTime = baseZonedDateTime,
                                is24HourFormat = is24HourFormat,
                                isPinned = isPinned,
                                onTogglePin = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    pinnedCityIds = if (isPinned) {
                                        pinnedCityIds - city.id
                                    } else {
                                        pinnedCityIds + city.id
                                    }
                                },
                                onRemoveCity = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    pinnedCityIds = pinnedCityIds - city.id
                                }
                            )
                        }
                    }
                }
            }
        }

        // --- C. Add City Selection Glass Dialog Modal ---
        if (showAddCityDialog) {
            AddCitySelectionModal(
                allCities = GLOBAL_WORLD_CITIES,
                pinnedCityIds = pinnedCityIds,
                onAddCity = { cityId ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (!pinnedCityIds.contains(cityId)) {
                        pinnedCityIds = pinnedCityIds + cityId
                    }
                    showAddCityDialog = false
                },
                onDismiss = { showAddCityDialog = false }
            )
        }
    }
}

// ==========================================
// 4. PROCEDURAL CANVAS WORLD MAP BACKDROP
// ==========================================

@Composable
fun ProceduralWorldMapCanvas() {
    val infiniteTransition = rememberInfiniteTransition(label = "globeRotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(90000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "globeRotation"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = 0.99f }
    ) {
        val width = size.width
        val height = size.height

        // Top-Right Champagne Gold Radial Glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(ChampagneGold.copy(alpha = 0.08f), Color.Transparent),
                center = Offset(width * 0.85f, height * 0.12f),
                radius = width * 0.65f
            )
        )

        // Bottom-Left Cyan Radial Glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(IceCyan.copy(alpha = 0.06f), Color.Transparent),
                center = Offset(width * 0.15f, height * 0.85f),
                radius = width * 0.7f
            )
        )

        // Longitude / Latitude Grid Lines
        val centerX = width * 0.5f
        val centerY = height * 0.25f
        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 8f), 0f)

        // Concentric World Spheres
        drawCircle(
            color = ChampagneGold.copy(alpha = 0.05f),
            center = Offset(centerX, centerY),
            radius = 180.dp.toPx(),
            style = Stroke(width = 1.5f, pathEffect = pathEffect)
        )

        // Rotating Meridian Lines
        for (i in 0 until 8) {
            val angle = Math.toRadians((i * 45.0 + rotationAngle)).toFloat()
            val x = centerX + cos(angle) * 180.dp.toPx()
            val y = centerY + sin(angle) * 180.dp.toPx()

            drawLine(
                color = ChampagneGold.copy(alpha = 0.06f),
                start = Offset(centerX, centerY),
                end = Offset(x, y),
                strokeWidth = 1f
            )
        }
    }
}

// ==========================================
// 5. TOP HEADER & BANNERS
// ==========================================

@Composable
fun WorldClockTopHeader(
    title: String,
    subtitle: String,
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = MutedSlateText,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun WorldClockOfflineBanner() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        color = EmeraldGreen.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = EmeraldGreen,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "جميع التوقيتات وحسابات المناطق الزمنية تعمل دون اتصال 100%.",
                fontSize = 11.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun WorldClockHeroBanner() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = GlassSurfaceDark.copy(alpha = 0.85f),
        border = androidx.compose.foundation.BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.35f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            ChampagneGold.copy(alpha = 0.15f),
                            GlassSurfaceDark.copy(alpha = 0.85f),
                            GlassCardHighlight.copy(alpha = 0.5f)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Procedural Rotating Globe Canvas
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(ChampagneGold.copy(alpha = 0.25f), GlowingAmber.copy(alpha = 0.12f))
                            )
                        )
                        .border(1.dp, ChampagneGold.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        tint = ChampagneGold,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "محرك التوقيت الدولي ومحاكاة الزمان",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "حساب دقيق لفرق الساعات والتوقيت الصيفي مع محاكي السفر عبر الزمن",
                        fontSize = 11.sp,
                        color = SecondaryText,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

// ==========================================
// 6. GLOBAL CONTROL CHIPS (12H/24H, RESET, ADD, GROUPS)
// ==========================================

@Composable
fun WorldClockGlobalControlChips(
    is24HourFormat: Boolean,
    isScrubberActive: Boolean,
    selectedGroupIndex: Int,
    onToggleFormat: () -> Unit,
    onResetToRealtime: () -> Unit,
    onOpenAddCity: () -> Unit,
    onSelectGroup: (Int) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // 12H / 24H Switcher
        item(key = "format_switcher") {
            Surface(
                modifier = Modifier
                    .height(36.dp)
                    .clickable { onToggleFormat() },
                shape = RoundedCornerShape(12.dp),
                color = GlassSurfaceDark.copy(alpha = 0.85f),
                border = androidx.compose.foundation.BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = ChampagneGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (is24HourFormat) "صيغة 24 ساعة" else "صيغة 12 ساعة (ص/م)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Reset to Real-time Device Clock Button
        if (isScrubberActive) {
            item(key = "reset_clock") {
                Surface(
                    modifier = Modifier
                        .height(36.dp)
                        .clickable { onResetToRealtime() },
                    shape = RoundedCornerShape(12.dp),
                    color = GlowingAmber.copy(alpha = 0.25f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlowingAmber)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = null,
                            tint = GlowingAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "إعادة للوقت الحالي",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlowingAmber
                        )
                    }
                }
            }
        }

        // City Groups Selector Chips
        items(DEFAULT_CITY_GROUPS.size, key = { "group_$it" }) { idx ->
            val group = DEFAULT_CITY_GROUPS[idx]
            val isSelected = selectedGroupIndex == idx

            Surface(
                modifier = Modifier
                    .height(36.dp)
                    .clickable { onSelectGroup(idx) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) ChampagneGold else GlassSurfaceDark.copy(alpha = 0.85f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSelected) ChampagneGold else ChampagneGold.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = group.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) ObsidianBgStart else Color.White
                    )
                }
            }
        }
    }
}

// ==========================================
// 7. TIME-TRAVEL SCRUBBER ENGINE CARD
// ==========================================

@Composable
fun TimeTravelScrubberCard(
    baseCity: WorldCity,
    allCities: List<WorldCity>,
    baseZonedDateTime: ZonedDateTime,
    minuteOffset: Int,
    is24HourFormat: Boolean,
    onSelectBaseCity: (WorldCity) -> Unit,
    onScrubberChange: (Int) -> Unit,
    onQuickPreset: (Int) -> Unit,
    onReset: () -> Unit
) {
    var isDropdownExpanded by remember { mutableStateOf(false) }

    val formattedBaseTime = remember(baseZonedDateTime, is24HourFormat) {
        val pattern = if (is24HourFormat) "HH:mm:ss" else "hh:mm:ss a"
        baseZonedDateTime.format(DateTimeFormatter.ofPattern(pattern, Locale("ar")))
    }

    val formattedBaseDate = remember(baseZonedDateTime) {
        baseZonedDateTime.format(DateTimeFormatter.ofPattern("EEEE، d MMMM yyyy", Locale("ar")))
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = GlassSurfaceDark.copy(alpha = 0.85f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (minuteOffset != 0) GlowingAmber.copy(alpha = 0.8f) else ChampagneGold.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Reference City Selector & Simulation Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = ChampagneGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "سلايدر السفر عبر الزمن",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Reference Base City Selector Dropdown Button
                Box {
                    Surface(
                        modifier = Modifier.clickable { isDropdownExpanded = true },
                        shape = RoundedCornerShape(10.dp),
                        color = GlassCardHighlight,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(baseCity.flagEmoji, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(baseCity.cityNameAr, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ChampagneGold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(16.dp))
                        }
                    }

                    DropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false },
                        modifier = Modifier.background(GlassSurfaceDark)
                    ) {
                        allCities.take(12).forEach { city ->
                            DropdownMenuItem(
                                text = { Text("${city.flagEmoji} ${city.cityNameAr}", color = Color.White, fontSize = 12.sp) },
                                onClick = {
                                    onSelectBaseCity(city)
                                    isDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Simulated Base Time Banner
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.45f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (minuteOffset != 0) GlowingAmber.copy(alpha = 0.5f) else ChampagneGold.copy(alpha = 0.25f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "الوقت المحاكى في ${baseCity.cityNameAr}:",
                            fontSize = 11.sp,
                            color = MutedSlateText
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formattedBaseTime,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = if (minuteOffset != 0) GlowingAmber else ChampagneGold
                        )
                        Text(
                            text = formattedBaseDate,
                            fontSize = 10.sp,
                            color = SecondaryText
                        )
                    }

                    if (minuteOffset != 0) {
                        Column(horizontalAlignment = Alignment.End) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = GlowingAmber.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GlowingAmber)
                            ) {
                                Text(
                                    text = if (minuteOffset > 0) "+${minuteOffset / 60}س ${minuteOffset % 60}د" else "${minuteOffset / 60}س ${minuteOffset % 60}د",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GlowingAmber,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            TextButton(onClick = onReset) {
                                Text("إلغاء المحاكاة", fontSize = 10.sp, color = CrimsonRed)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Interactive Minute Scrubber Slider (-12h to +12h, i.e. -720m to +720m)
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("-12 ساعة", fontSize = 10.sp, color = MutedSlateText)
                    Text("سحب لتقديم/تأخير الوقت", fontSize = 10.sp, color = ChampagneGold, fontWeight = FontWeight.Bold)
                    Text("+12 ساعة", fontSize = 10.sp, color = MutedSlateText)
                }

                Slider(
                    value = minuteOffset.toFloat(),
                    onValueChange = { onScrubberChange(it.toInt()) },
                    valueRange = -720f..720f,
                    steps = 47, // 30-minute intervals
                    colors = SliderDefaults.colors(
                        thumbColor = if (minuteOffset != 0) GlowingAmber else ChampagneGold,
                        activeTrackColor = if (minuteOffset != 0) GlowingAmber else ChampagneGold,
                        inactiveTrackColor = GlassCardHighlight
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Preset Time Jump Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    AssistChip(
                        onClick = { onQuickPreset(0) },
                        label = { Text("الآن", fontSize = 10.sp) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = GlassCardHighlight, labelColor = Color.White)
                    )
                }
                item {
                    AssistChip(
                        onClick = { onQuickPreset(60) },
                        label = { Text("+1 ساعة", fontSize = 10.sp) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = GlassCardHighlight, labelColor = Color.White)
                    )
                }
                item {
                    AssistChip(
                        onClick = { onQuickPreset(180) },
                        label = { Text("+3 ساعات", fontSize = 10.sp) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = GlassCardHighlight, labelColor = Color.White)
                    )
                }
                item {
                    AssistChip(
                        onClick = { onQuickPreset(-180) },
                        label = { Text("-3 ساعات", fontSize = 10.sp) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = GlassCardHighlight, labelColor = Color.White)
                    )
                }
            }
        }
    }
}

// ==========================================
// 8. TIME DIFFERENCE CALCULATOR CARD
// ==========================================

@Composable
fun TimeDifferenceCalculatorCard(
    allCities: List<WorldCity>,
    selectedCityAId: String,
    selectedCityBId: String,
    baseZonedDateTime: ZonedDateTime,
    onSelectCityA: (String) -> Unit,
    onSelectCityB: (String) -> Unit
) {
    val cityA = remember(selectedCityAId) { allCities.find { it.id == selectedCityAId } ?: allCities[0] }
    val cityB = remember(selectedCityBId) { allCities.find { it.id == selectedCityBId } ?: allCities[3] }

    val zdtA = remember(baseZonedDateTime, cityA) {
        val zoneA = try { ZoneId.of(cityA.timezoneId) } catch (e: Exception) { ZoneId.systemDefault() }
        baseZonedDateTime.withZoneSameInstant(zoneA)
    }

    val zdtB = remember(baseZonedDateTime, cityB) {
        val zoneB = try { ZoneId.of(cityB.timezoneId) } catch (e: Exception) { ZoneId.systemDefault() }
        baseZonedDateTime.withZoneSameInstant(zoneB)
    }

    val diffSeconds = zdtB.offset.totalSeconds - zdtA.offset.totalSeconds
    val diffHours = diffSeconds / 3600f

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = GlassSurfaceDark.copy(alpha = 0.9f),
        border = androidx.compose.foundation.BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "حاسبة الفرق الزمني المباشر بين مدينتين",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = ChampagneGold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // City A Selector
                Column(modifier = Modifier.weight(1f)) {
                    Text("المدينة الأولى", fontSize = 10.sp, color = MutedSlateText)
                    Text("${cityA.flagEmoji} ${cityA.cityNameAr}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(24.dp))

                // City B Selector
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text("المدينة الثانية", fontSize = 10.sp, color = MutedSlateText)
                    Text("${cityB.flagEmoji} ${cityB.cityNameAr}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Result Display Box
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ChampagneGold.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val diffText = when {
                        diffHours > 0 -> "${cityB.cityNameAr} متقدمة بـ +${diffHours.toInt()} ساعات عن ${cityA.cityNameAr}"
                        diffHours < 0 -> "${cityB.cityNameAr} متأخرة بـ ${diffHours.toInt()} ساعات عن ${cityA.cityNameAr}"
                        else -> "المدينتان تقعان في نفس التوقيت تماماً"
                    }

                    Text(
                        text = diffText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ==========================================
// 9. STEP-BY-STEP BREAKDOWN CARD
// ==========================================

@Composable
fun StepByStepBreakdownCard(
    baseCity: WorldCity,
    baseZonedDateTime: ZonedDateTime,
    targetCity: WorldCity
) {
    val targetZone = try { ZoneId.of(targetCity.timezoneId) } catch (e: Exception) { ZoneId.systemDefault() }
    val targetZdt = baseZonedDateTime.withZoneSameInstant(targetZone)

    val baseOffsetStr = baseZonedDateTime.offset.id
    val targetOffsetStr = targetZdt.offset.id

    val isDstBase = baseZonedDateTime.zone.rules.isDaylightSavings(baseZonedDateTime.toInstant())
    val isDstTarget = targetZone.rules.isDaylightSavings(targetZdt.toInstant())

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = GlassSurfaceDark.copy(alpha = 0.9f),
        border = androidx.compose.foundation.BorderStroke(1.dp, IceCyan.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "المعادلة الفلكية والرياضية لفرق التوقيت",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = IceCyan
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text("1. إزاحة خط الجرينتش (Base GMT): $baseOffsetStr (${baseCity.cityNameAr})", fontSize = 11.sp, color = Color.White)
            Text("2. إزاحة خط الجرينتش (Target GMT): $targetOffsetStr (${targetCity.cityNameAr})", fontSize = 11.sp, color = Color.White)
            Text("3. حالة التوقيت الصيفي (DST): ${if (isDstTarget) "مفعل ☀️" else "غير مفعل 🌙"}", fontSize = 11.sp, color = Color.White)
            Text("4. المعادلة: Δt = Target GMT Offset - Base GMT Offset", fontSize = 11.sp, color = IceCyan, fontWeight = FontWeight.Bold)
        }
    }
}

// ==========================================
// 10. WORLD CITY CLOCK CARD
// ==========================================

@Composable
fun WorldCityClockCard(
    city: WorldCity,
    baseCity: WorldCity,
    baseZonedDateTime: ZonedDateTime,
    is24HourFormat: Boolean,
    isPinned: Boolean,
    onTogglePin: () -> Unit,
    onRemoveCity: () -> Unit
) {
    val cityZone = remember(city.timezoneId) {
        try { ZoneId.of(city.timezoneId) } catch (e: Exception) { ZoneId.systemDefault() }
    }

    val cityZdt = remember(baseZonedDateTime, cityZone) {
        baseZonedDateTime.withZoneSameInstant(cityZone)
    }

    // DST Check using java.time
    val isDstActive = remember(cityZdt, cityZone) {
        cityZone.rules.isDaylightSavings(cityZdt.toInstant())
    }

    // Day / Night Check (Daytime between 6:00 AM and 6:00 PM)
    val isDaytime = remember(cityZdt) {
        val hour = cityZdt.hour
        hour in 6..17
    }

    // Date Shift Check relative to base city date
    val dateShiftText = remember(cityZdt, baseZonedDateTime) {
        val baseDay = baseZonedDateTime.toLocalDate()
        val cityDay = cityZdt.toLocalDate()

        when {
            cityDay.isAfter(baseDay) -> "غداً (+1d)"
            cityDay.isBefore(baseDay) -> "أمس (-1d)"
            else -> null
        }
    }

    // Hour Difference Relative to Base City
    val hourDiff = remember(cityZdt, baseZonedDateTime) {
        val secondsDiff = cityZdt.offset.totalSeconds - baseZonedDateTime.offset.totalSeconds
        secondsDiff / 3600f
    }

    val formattedTime = remember(cityZdt, is24HourFormat) {
        val pattern = if (is24HourFormat) "HH:mm:ss" else "hh:mm:ss a"
        cityZdt.format(DateTimeFormatter.ofPattern(pattern, Locale("ar")))
    }

    val gmtOffsetStr = remember(cityZdt) {
        val offset = cityZdt.offset.id
        if (offset == "Z") "GMT+0" else "GMT$offset"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = GlassSurfaceDark.copy(alpha = 0.85f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isPinned) ChampagneGold.copy(alpha = 0.7f) else ChampagneGold.copy(alpha = 0.3f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            GlassCardHighlight.copy(alpha = if (isPinned) 0.6f else 0.3f),
                            GlassSurfaceDark.copy(alpha = 0.85f)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column {
                // Top Row: Flag, City Name, Country Subtitle & Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(city.flagEmoji, fontSize = 28.sp)

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = city.cityNameAr,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                // GMT Offset Chip
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = IceCyan.copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, IceCyan.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = gmtOffsetStr,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = IceCyan,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text(
                                text = city.countryNameAr,
                                fontSize = 11.sp,
                                color = MutedSlateText
                            )
                        }
                    }

                    // Action Buttons (Pin & Remove)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = onTogglePin, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = if (isPinned) Icons.Default.Star else Icons.Outlined.StarBorder,
                                contentDescription = "تثبيت المدينة",
                                tint = if (isPinned) ChampagneGold else MutedSlateText,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(onClick = onRemoveCity, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "حذف",
                                tint = MutedSlateText,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Row: Digital Time Display + Day/Night & Date Shift Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Status Badges (Day/Night, DST, Date Shift)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Day / Night Status Chip
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDaytime) SoftSunYellow.copy(alpha = 0.2f) else DeepIndigoNight.copy(alpha = 0.3f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isDaytime) SoftSunYellow.copy(alpha = 0.5f) else DeepIndigoNight.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(if (isDaytime) "☀️ نهار" else "🌙 ليل", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        // Date Shift Badge (غداً / أمس)
                        if (dateShiftText != null) {
                            val badgeColor = if (dateShiftText.contains("غداً")) EmeraldGreen else CrimsonRed
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = badgeColor.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor)
                            ) {
                                Text(
                                    text = dateShiftText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // DST Badge
                        if (isDstActive) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = GlowingAmber.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GlowingAmber.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "توقيت صيفي ☀️",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GlowingAmber,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    // Digital Clock Digits & Difference Tag
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = formattedTime,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = GlowingAmber
                        )

                        val diffTag = when {
                            hourDiff > 0 -> "+${hourDiff.toInt()}س عن ${baseCity.cityNameAr}"
                            hourDiff < 0 -> "${hourDiff.toInt()}س عن ${baseCity.cityNameAr}"
                            else -> "نفس التوقيت"
                        }

                        Text(
                            text = diffTag,
                            fontSize = 10.sp,
                            color = SecondaryText,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 11. SEARCH & SKELETON / EMPTY STATES
// ==========================================

@Composable
fun WorldClockSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = GlassSurfaceDark.copy(alpha = 0.85f),
        border = androidx.compose.foundation.BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(20.dp))

            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("بحث عن مدينة أو دولة أو منطقة زمنية...", fontSize = 12.sp, color = MutedSlateText) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.weight(1f)
            )

            if (query.isNotEmpty()) {
                IconButton(onClick = onClearSearch) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = MutedSlateText, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun WorldClockSkeletonCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(22.dp),
        color = GlassSurfaceDark.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.15f))
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(ChampagneGold.copy(alpha = 0.1f))
            )
        }
    }
}

@Composable
fun WorldClockEmptyStateCard(onAddCityClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = GlassSurfaceDark.copy(alpha = 0.85f),
            border = androidx.compose.foundation.BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.PublicOff, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("أضف مدنك المفضلة للمتابعة", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(6.dp))
                Text("اختر من قائمة العواصم والمدن العالمية لمتابعة ساعاتها فوراً", fontSize = 11.sp, color = MutedSlateText, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onAddCityClick, colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold)) {
                    Text("إضافة مدينة الآن +", color = ObsidianBgStart, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun WorldClockErrorStateCard(onRetry: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = GlassSurfaceDark.copy(alpha = 0.85f),
        border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = CrimsonRed, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text("حدث خطأ أثناء تحميل بيانات المنطقة الزمنية", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed)) {
                Text("إعادة المحاولة", color = Color.White)
            }
        }
    }
}

// ==========================================
// 12. ADD CITY SELECTION MODAL DIALOG
// ==========================================

@Composable
fun AddCitySelectionModal(
    allCities: List<WorldCity>,
    pinnedCityIds: List<String>,
    onAddCity: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var modalSearch by remember { mutableStateOf("") }

    val filtered = remember(modalSearch, pinnedCityIds) {
        allCities.filter { city ->
            !pinnedCityIds.contains(city.id) && (
                    modalSearch.isBlank() ||
                            city.cityNameAr.contains(modalSearch, ignoreCase = true) ||
                            city.countryNameAr.contains(modalSearch, ignoreCase = true)
                    )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GlassSurfaceDark,
        titleContentColor = Color.White,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AddLocation, contentDescription = null, tint = ChampagneGold)
                Spacer(modifier = Modifier.width(8.dp))
                Text("إضافة مدينة إلى قائمة الساعات", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.height(350.dp)) {
                OutlinedTextField(
                    value = modalSearch,
                    onValueChange = { modalSearch = it },
                    placeholder = { Text("بحث عن مدينة...", fontSize = 12.sp, color = MutedSlateText) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ChampagneGold,
                        unfocusedBorderColor = ChampagneGold.copy(alpha = 0.3f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(filtered, key = { it.id }) { city ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAddCity(city.id) },
                            shape = RoundedCornerShape(12.dp),
                            color = GlassCardHighlight.copy(alpha = 0.7f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(city.flagEmoji, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(city.cityNameAr, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text(city.countryNameAr, fontSize = 10.sp, color = MutedSlateText)
                                    }
                                }

                                Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق", color = ChampagneGold, fontWeight = FontWeight.Bold)
            }
        }
    )
}
