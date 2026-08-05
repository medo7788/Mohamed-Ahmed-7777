package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import com.example.util.AppLocationProvider
import com.example.ui.components.LocationStatusCard
import com.example.ui.components.LocationCardState
import com.example.ui.components.ToolScreenScaffold
import com.example.ui.theme.AppIcons
import com.example.model.CalcKey
import com.example.ui.theme.GradientTokens
import com.example.ui.theme.Spacing
import com.example.R

import android.Manifest
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.IslamicData
import com.example.data.CityPrayerInfo
import com.example.data.LivePricesRepository
import com.example.data.SurahInfo
import com.example.ui.theme.*
import com.example.util.AdhanScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.*
import android.annotation.SuppressLint
import com.google.android.gms.location.LocationServices
import android.location.Geocoder
import java.util.Locale
import com.google.android.gms.location.Priority

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerTimesScreen(colors: CustomThemeColors, onNavigate: ((CalcKey) -> Unit)? = null) {
    PrayerTimesScreenRedesign(colors = colors, onNavigate = onNavigate)
}

@Composable
fun QiblaDirectionScreen(colors: CustomThemeColors) {
    QiblaDirectionScreenRedesign(colors = colors)
}

@Composable
fun AdhkarScreen(colors: CustomThemeColors) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Morning, 1: Evening
    val itemsList = if (selectedTab == 0) IslamicData.morningAdhkar else IslamicData.eveningAdhkar
    val counts = remember { mutableStateMapOf<Int, Int>() }

    ToolScreenScaffold(
        colors = colors,
        icon = AppIcons.forCalc(CalcKey.ADHKAR),
        title = if (selectedTab == 0) "أذكار الصباح" else "أذكار المساء",
        subtitle = "حصن المسلم اليومي بذكر الله والاستعاذة من كل شر"
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = colors.surface.copy(alpha = 0.5f),
            contentColor = colors.accent,
            modifier = Modifier.clip(RoundedCornerShape(16.dp)),
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = colors.accent
                )
            },
            divider = {}
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(AppIcons.forCalc(CalcKey.WEATHER), null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("أذكار الصباح", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(AppIcons.forCalc(CalcKey.ADHKAR), null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("أذكار المساء", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            })
        }

        Spacer(modifier = Modifier.height(Spacing.Medium))

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsList.forEach { dhikr ->
                val currentCount = counts[dhikr.id] ?: 0
                val isFinished = currentCount >= dhikr.countTarget

                Surface(
                    color = colors.surface,
                    shape = RoundedCornerShape(20.dp),
                    tonalElevation = if (isFinished) 0.dp else 2.dp,
                    border = if (isFinished) BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)) else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (currentCount < dhikr.countTarget) {
                                counts[dhikr.id] = currentCount + 1
                            }
                        }
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            dhikr.text, 
                            fontSize = 17.sp, 
                            fontWeight = FontWeight.Medium, 
                            color = colors.text, 
                            lineHeight = 28.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (dhikr.rewardText.isNotBlank()) {
                            Surface(
                                color = colors.surface2.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                                    Icon(AppIcons.Info, null, tint = colors.accent, modifier = Modifier.size(14.dp).padding(top = 2.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        dhikr.rewardText, 
                                        fontSize = 12.sp, 
                                        color = colors.textMuted,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                        
                        if (dhikr.reference.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                                Icon(AppIcons.forCalc(CalcKey.ADHKAR), null, tint = colors.accent.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    dhikr.reference, 
                                    fontSize = 11.sp, 
                                    color = colors.accent.copy(alpha = 0.7f), 
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    if (isFinished) "تم الانتهاء ✓" else "التكرار الحالي",
                                    fontSize = 12.sp,
                                    color = if (isFinished) Color(0xFF10B981) else colors.textMuted
                                )
                                Text(
                                    "$currentCount / ${dhikr.countTarget}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isFinished) Color(0xFF10B981) else colors.accent
                                )
                            }

                            Surface(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .clickable(enabled = !isFinished) {
                                        counts[dhikr.id] = currentCount + 1
                                    },
                                color = if (isFinished) Color(0xFF10B981) else colors.accent,
                                shadowElevation = 4.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (isFinished) {
                                        Icon(Icons.Filled.Check, null, tint = Color.White)
                                    } else {
                                        Text("${dhikr.countTarget - currentCount}", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
