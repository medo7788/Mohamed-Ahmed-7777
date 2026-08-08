package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.MajorCity
import com.example.data.QiblaHistoryItem
import com.example.data.QiblaRepository
import com.example.domain.QiblaMath
import com.example.sensor.SensorAccuracyLevel
import com.example.ui.theme.CustomThemeColors
import com.example.viewmodel.QiblaUiState
import com.example.viewmodel.QiblaViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

// Commercial Grade Color Palette Tokens
private val ObsidianNightDark = Color(0xFF080A0F)
private val ObsidianNightGradientEnd = Color(0xFF121620)
private val GlassContainerBg = Color(0xD9141926) // 85% opacity #141926
private val ChampagneGold = Color(0xFFD4AF37)
private val ChampagneGoldBorder = Color(0x4DD4AF37) // 30% opacity gold border
private val AmberGoldGlow = Color(0xFFF59E0B)
private val IceCyanAccent = Color(0xFF00F2FE)
private val EmeraldGreenSuccess = Color(0xFF10B981)
private val CrimsonRedWarning = Color(0xFFEF4444)
private val SlateTextMuted = Color(0xFF94A3B8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QiblaCompassScreen(
    colors: CustomThemeColors,
    onBack: (() -> Unit)? = null,
    viewModel: QiblaViewModel = viewModel()
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Manage sensor lifecycle
    DisposableEffect(Unit) {
        viewModel.startSensors()
        viewModel.fetchCurrentLocation(context)
        onDispose {
            viewModel.stopSensors()
        }
    }

    // Trigger haptic pulse when alignment lock is achieved
    var hasTriggeredAlignmentHaptic by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.isAlignedWithQibla) {
        if (uiState.isAlignedWithQibla && !hasTriggeredAlignmentHaptic) {
            triggerCommercialHapticPulse(context)
            hasTriggeredAlignmentHaptic = true
        } else if (!uiState.isAlignedWithQibla) {
            hasTriggeredAlignmentHaptic = false
        }
    }

    // Handle back button
    BackHandler(enabled = onBack != null) {
        onBack?.invoke()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(ObsidianNightDark, ObsidianNightGradientEnd)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Procedural Astrolabe Background Canvas
        ProceduralAstrolabeBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Screen Top Header with Back Navigation if applicable
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (onBack != null) {
                    IconButton(
                        onClick = { onBack() },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(GlassContainerBg)
                            .border(1.dp, ChampagneGoldBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "رجوع",
                            tint = ChampagneGold
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(40.dp))
                }

                Text(
                    text = "بوصلة القبلة الاحترافية",
                    color = ChampagneGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                IconButton(
                    onClick = { viewModel.setShowHistoryDrawer(true) },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(GlassContainerBg)
                        .border(1.dp, ChampagneGoldBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "السجل",
                        tint = ChampagneGold
                    )
                }
            }

            // SECTION A: Fixed Header & Sensor Accuracy Badge Card
            SectionAHeaderAndBadge(
                cityName = uiState.cityName,
                accuracy = uiState.calibrationStatus,
                isInterference = uiState.isInterferenceDetected,
                isWaitingGps = uiState.isWaitingForBetterGps,
                isLoading = uiState.isLoading,
                onRecalibrateClick = { viewModel.setShowCalibrationDialog(true) },
                onChangeCityClick = { viewModel.setShowCitySelectorDialog(true) },
                onRefreshLocation = { viewModel.fetchCurrentLocation(context) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // SECTION B: Interactive 2D Canvas Qibla Compass Dial (Expanded weight)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                SectionBCompassDial(
                    uiState = uiState,
                    onTripleTap = { viewModel.toggleDebugOverlay() }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // SECTION C: Real-Time Telemetry Glass Card
            SectionCTelemetryCard(
                uiState = uiState,
                onCalibrate = { viewModel.setShowCalibrationDialog(true) },
                onSaveLocation = {
                    viewModel.saveCurrentLocationToHistory()
                    triggerCommercialHapticPulse(context)
                },
                onShare = { shareQiblaInfo(context, uiState) },
                onToggleSim = { viewModel.toggleSimulationMode(!uiState.isSimulationMode) }
            )

            Spacer(modifier = Modifier.height(12.dp))
        }

        // SECTION D: Qibla History Drawer / Modal
        if (uiState.showHistoryDrawer) {
            SectionDHistoryModal(
                history = uiState.history,
                onDismiss = { viewModel.setShowHistoryDrawer(false) },
                onClearHistory = { viewModel.clearHistory() },
                onSelectHistoryItem = { item ->
                    viewModel.selectCity(
                        MajorCity(item.cityName, item.cityName, "سابق", item.lat, item.lng)
                    )
                    viewModel.setShowHistoryDrawer(false)
                }
            )
        }

        // SECTION E: Developer Debug Overlay
        if (uiState.isDebugOverlayVisible) {
            SectionEDebugOverlay(
                uiState = uiState,
                onClose = { viewModel.toggleDebugOverlay() },
                onHeadingSliderChange = { viewModel.setManualHeading(it) }
            )
        }

        // Interactive Figure-8 Calibration Dialog
        if (uiState.showCalibrationDialog) {
            Figure8CalibrationDialog(
                onDismiss = { viewModel.setShowCalibrationDialog(false) }
            )
        }

        // City Selector Dialog
        if (uiState.showCitySelectorDialog) {
            CitySelectorDialog(
                onDismiss = { viewModel.setShowCitySelectorDialog(false) },
                onSelectCity = { city -> viewModel.selectCity(city) }
            )
        }
    }
}

// ==========================================
// SECTION A: Fixed Header & Sensor Accuracy Badge
// ==========================================
@Composable
private fun SectionAHeaderAndBadge(
    cityName: String,
    accuracy: SensorAccuracyLevel,
    isInterference: Boolean,
    isWaitingGps: Boolean,
    isLoading: Boolean,
    onRecalibrateClick: () -> Unit,
    onChangeCityClick: () -> Unit,
    onRefreshLocation: () -> Unit
) {
    Surface(
        color = GlassContainerBg,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ChampagneGoldBorder),
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("qibla_header_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(ChampagneGold.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🕋", fontSize = 20.sp)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = cityName,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isLoading) {
                                Spacer(modifier = Modifier.width(6.dp))
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = ChampagneGold,
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                        Text(
                            text = "الموقع الحالي المعتمد لحساب الاتجاه",
                            color = SlateTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                // Change City Button
                Surface(
                    color = ChampagneGold.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.4f)),
                    modifier = Modifier.clickable { onChangeCityClick() }
                ) {
                    Text(
                        text = "تغيير المكان",
                        color = ChampagneGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sensor Accuracy & Status Pill Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val (badgeBg, badgeText, badgeColor) = when {
                    isInterference -> Triple(
                        CrimsonRedWarning.copy(alpha = 0.2f),
                        "تداخل مغناطيسي! أبعد الأجهزة",
                        CrimsonRedWarning
                    )
                    accuracy == SensorAccuracyLevel.HIGH -> Triple(
                        EmeraldGreenSuccess.copy(alpha = 0.2f),
                        "دقة الحساس: عالية (ممتاز)",
                        EmeraldGreenSuccess
                    )
                    accuracy == SensorAccuracyLevel.MEDIUM -> Triple(
                        AmberGoldGlow.copy(alpha = 0.2f),
                        "دقة الحساس: متوسطة",
                        AmberGoldGlow
                    )
                    else -> Triple(
                        CrimsonRedWarning.copy(alpha = 0.2f),
                        "الحساس بحاجة إلى معايرة",
                        CrimsonRedWarning
                    )
                }

                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f)),
                    modifier = Modifier.clickable { onRecalibrateClick() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(badgeColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = badgeText,
                            color = badgeColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = "اضغط للمعايرة 🔄",
                    color = SlateTextMuted,
                    fontSize = 10.sp,
                    modifier = Modifier.clickable { onRecalibrateClick() }
                )
            }

            if (isWaitingGps) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "⚠️ جاري انتظار دقة GPS أعلى من 30م...",
                    color = AmberGoldGlow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ==========================================
// SECTION B: Canvas Qibla Compass Dial
// ==========================================
@Composable
private fun SectionBCompassDial(
    uiState: QiblaUiState,
    onTripleTap: () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        // Smooth animated spring physics rotation for dial
        val animatedHeading by animateFloatAsState(
            targetValue = uiState.trueHeading,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "heading_spring"
        )

        // Animated pulsing aura glow when aligned with Qibla
        val infiniteTransition = rememberInfiniteTransition(label = "qibla_glow")
        val pulseGlowAlpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.9f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_alpha"
        )

        var tapCount by remember { mutableStateOf(0) }
        var lastTapTime by remember { mutableStateOf(0L) }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            val now = System.currentTimeMillis()
                            if (now - lastTapTime < 500) {
                                tapCount++
                                if (tapCount >= 3) {
                                    onTripleTap()
                                    tapCount = 0
                                }
                            } else {
                                tapCount = 1
                            }
                            lastTapTime = now
                        }
                    )
                }
                .testTag("qibla_compass_dial")
        ) {
        // Alignment Glow Ring if within ±2°
        if (uiState.isAlignedWithQibla) {
            Canvas(modifier = Modifier.fillMaxSize(0.92f)) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            AmberGoldGlow.copy(alpha = 0.45f * pulseGlowAlpha),
                            ChampagneGold.copy(alpha = 0.15f * pulseGlowAlpha),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension / 1.8f
                    ),
                    radius = size.minDimension / 1.8f
                )
            }
        }

        // Hardware-Accelerated Rotatable Compass Canvas
        Canvas(modifier = Modifier.fillMaxSize(0.88f)) {
            val centerOffset = center
            val radius = size.minDimension / 2.1f

            // 1. Outer Astrolabe Fixed Glass Ring
            drawCircle(
                color = GlassContainerBg,
                radius = radius,
                center = centerOffset
            )
            drawCircle(
                color = ChampagneGoldBorder,
                radius = radius,
                center = centerOffset,
                style = Stroke(width = 3.dp.toPx())
            )
            drawCircle(
                color = ChampagneGold.copy(alpha = 0.12f),
                radius = radius * 0.88f,
                center = centerOffset,
                style = Stroke(width = 1.dp.toPx())
            )

            // 2. Rotatable Dial (Rotates by -animatedHeading so dial points to World Directions)
            rotate(degrees = -animatedHeading, pivot = centerOffset) {
                // Draw 360 degree ticks
                for (angle in 0 until 360 step 5) {
                    val angleRad = Math.toRadians(angle.toDouble())
                    val isMajor = angle % 30 == 0
                    val isMedium = angle % 10 == 0

                    val tickLength = when {
                        isMajor -> 16.dp.toPx()
                        isMedium -> 10.dp.toPx()
                        else -> 5.dp.toPx()
                    }

                    val tickColor = when {
                        isMajor -> ChampagneGold
                        isMedium -> ChampagneGold.copy(alpha = 0.6f)
                        else -> SlateTextMuted.copy(alpha = 0.35f)
                    }

                    val tickWidth = if (isMajor) 2.5.dp.toPx() else 1.dp.toPx()

                    val startX = (centerOffset.x + (radius - tickLength) * sin(angleRad)).toFloat()
                    val startY = (centerOffset.y - (radius - tickLength) * cos(angleRad)).toFloat()
                    val endX = (centerOffset.x + radius * sin(angleRad)).toFloat()
                    val endY = (centerOffset.y - radius * cos(angleRad)).toFloat()

                    drawLine(
                        color = tickColor,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = tickWidth
                    )
                }

                // 3. Kaaba Marker on Outer Rim at Qibla Bearing angle
                val qiblaRad = Math.toRadians(uiState.qiblaBearing)
                val kaabaMarkerDistance = radius * 0.94f
                val kaabaX = (centerOffset.x + kaabaMarkerDistance * sin(qiblaRad)).toFloat()
                val kaabaY = (centerOffset.y - kaabaMarkerDistance * cos(qiblaRad)).toFloat()

                // Glowing Gold Aura behind Kaaba Icon
                drawCircle(
                    color = AmberGoldGlow.copy(alpha = 0.4f),
                    radius = 22.dp.toPx(),
                    center = Offset(kaabaX, kaabaY)
                )
                drawCircle(
                    color = ChampagneGold,
                    radius = 14.dp.toPx(),
                    center = Offset(kaabaX, kaabaY)
                )
                drawCircle(
                    color = ObsidianNightDark,
                    radius = 11.dp.toPx(),
                    center = Offset(kaabaX, kaabaY)
                )
            }

            // 4. Three-Layer Precision Qibla Needle (Rotates to relative Qibla angle)
            val relativeNeedleAngle = uiState.relativeAngle
            rotate(degrees = relativeNeedleAngle, pivot = centerOffset) {
                val needleLength = radius * 0.72f
                val needleWidth = 14.dp.toPx()

                // Layer 1: Needle Shadow & Outer Radial Glow
                val needlePath = Path().apply {
                    moveTo(centerOffset.x, centerOffset.y - needleLength)
                    lineTo(centerOffset.x + needleWidth / 2f, centerOffset.y)
                    lineTo(centerOffset.x, centerOffset.y + needleLength * 0.25f)
                    lineTo(centerOffset.x - needleWidth / 2f, centerOffset.y)
                    close()
                }

                drawPath(
                    path = needlePath,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            AmberGoldGlow,
                            ChampagneGold,
                            AmberGoldGlow.copy(alpha = 0.3f)
                        ),
                        start = Offset(centerOffset.x, centerOffset.y - needleLength),
                        end = Offset(centerOffset.x, centerOffset.y + needleLength * 0.25f)
                    )
                )

                // Layer 2: Metallic Highlight Spine
                drawLine(
                    color = Color.White.copy(alpha = 0.8f),
                    start = Offset(centerOffset.x, centerOffset.y - needleLength + 8.dp.toPx()),
                    end = Offset(centerOffset.x, centerOffset.y),
                    strokeWidth = 2.dp.toPx()
                )

                // Layer 3: Holy Kaaba Structure at Needle Tip
                val tipX = centerOffset.x
                val tipY = centerOffset.y - needleLength
                val kaabaSize = 28.dp.toPx()
                val rectLeft = tipX - kaabaSize / 2f
                val rectTop = tipY - kaabaSize / 2f

                // Golden Glow behind Kaaba
                drawCircle(
                    color = AmberGoldGlow.copy(alpha = 0.5f),
                    radius = kaabaSize * 0.85f,
                    center = Offset(tipX, tipY)
                )

                // Kaaba Black Cube Body
                drawRoundRect(
                    color = Color(0xFF121216),
                    topLeft = Offset(rectLeft, rectTop),
                    size = Size(kaabaSize, kaabaSize),
                    cornerRadius = CornerRadius(4.dp.toPx())
                )

                // Gold Border around Kaaba
                drawRoundRect(
                    color = ChampagneGold,
                    topLeft = Offset(rectLeft, rectTop),
                    size = Size(kaabaSize, kaabaSize),
                    cornerRadius = CornerRadius(4.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx())
                )

                // Golden Kiswa Band (upper part)
                drawRect(
                    color = ChampagneGold,
                    topLeft = Offset(rectLeft, rectTop + kaabaSize * 0.22f),
                    size = Size(kaabaSize, kaabaSize * 0.16f)
                )

                // Golden Door (Bab al-Kaaba)
                drawRoundRect(
                    color = AmberGoldGlow,
                    topLeft = Offset(rectLeft + kaabaSize * 0.58f, rectTop + kaabaSize * 0.45f),
                    size = Size(kaabaSize * 0.25f, kaabaSize * 0.45f),
                    cornerRadius = CornerRadius(2.dp.toPx())
                )

                // Top Crescent Ornament on Kaaba
                drawCircle(
                    color = ChampagneGold,
                    radius = 3.dp.toPx(),
                    center = Offset(tipX, rectTop - 3.dp.toPx())
                )

                // Layer 4: Polished Center Pivot Disc
                drawCircle(
                    color = ObsidianNightDark,
                    radius = 18.dp.toPx(),
                    center = centerOffset
                )
                drawCircle(
                    color = ChampagneGold,
                    radius = 14.dp.toPx(),
                    center = centerOffset,
                    style = Stroke(width = 3.dp.toPx())
                )
                drawCircle(
                    color = AmberGoldGlow,
                    radius = 6.dp.toPx(),
                    center = centerOffset
                )
            }
        }

        // Cardinal Points Overlay (English: N, S, E, W in Ice Cyan)
        Box(modifier = Modifier.fillMaxSize(0.72f), contentAlignment = Alignment.Center) {
            // Top Cardinal (North / True North)
            Text(
                text = "N",
                color = IceCyanAccent,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.align(Alignment.TopCenter)
            )
            // Bottom Cardinal (South)
            Text(
                text = "S",
                color = SlateTextMuted,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
            // Right Cardinal (East)
            Text(
                text = "E",
                color = SlateTextMuted,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
            // Left Cardinal (West)
            Text(
                text = "W",
                color = SlateTextMuted,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterStart)
            )

            // Center Qibla Alignment Status Text
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(top = 80.dp)
            ) {
                if (uiState.isAlignedWithQibla) {
                    Surface(
                        color = AmberGoldGlow.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AmberGoldGlow)
                    ) {
                        Text(
                            text = "✨ أنت باتجاه القبلة تمامًا ✨",
                            color = AmberGoldGlow,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Text(
                        text = "${uiState.relativeAngle.toInt()}°",
                        color = ChampagneGold,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "الانحراف عن القبلة",
                        color = SlateTextMuted,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
}

// ==========================================
// SECTION C: Real-Time Telemetry Glass Card
// ==========================================
@Composable
private fun SectionCTelemetryCard(
    uiState: QiblaUiState,
    onCalibrate: () -> Unit,
    onSaveLocation: () -> Unit,
    onShare: () -> Unit,
    onToggleSim: () -> Unit
) {
    Surface(
        color = GlassContainerBg,
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ChampagneGoldBorder),
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("qibla_telemetry_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // 4 Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TelemetryMetricItem(
                    label = "الاتجاه الحالي",
                    value = "${uiState.trueHeading.toInt()}°",
                    accentColor = IceCyanAccent,
                    modifier = Modifier.weight(1f)
                )
                TelemetryMetricItem(
                    label = "زاوية القبلة",
                    value = "${uiState.qiblaBearing.toInt()}°",
                    accentColor = ChampagneGold,
                    modifier = Modifier.weight(1f)
                )
                TelemetryMetricItem(
                    label = "المسافة للمكة",
                    value = "${uiState.distanceToMakkah.toInt()} كم",
                    accentColor = AmberGoldGlow,
                    modifier = Modifier.weight(1f)
                )
                TelemetryMetricItem(
                    label = "الانحراف المغناطيسي",
                    value = "${String.format(Locale.US, "%.1f", uiState.declination)}°",
                    accentColor = SlateTextMuted,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TelemetryActionButton(
                    icon = Icons.Default.Refresh,
                    label = "معايرة",
                    tint = ChampagneGold,
                    modifier = Modifier.weight(1f),
                    onClick = onCalibrate
                )

                TelemetryActionButton(
                    icon = Icons.Default.BookmarkAdd,
                    label = "حفظ الموقع",
                    tint = EmeraldGreenSuccess,
                    modifier = Modifier.weight(1f),
                    onClick = onSaveLocation
                )

                TelemetryActionButton(
                    icon = Icons.Default.Share,
                    label = "مشاركة",
                    tint = IceCyanAccent,
                    modifier = Modifier.weight(1f),
                    onClick = onShare
                )

                TelemetryActionButton(
                    icon = if (uiState.isSimulationMode) Icons.Default.DirectionsRun else Icons.Default.Tune,
                    label = if (uiState.isSimulationMode) "محاكاة ON" else "محاكاة",
                    tint = if (uiState.isSimulationMode) AmberGoldGlow else SlateTextMuted,
                    modifier = Modifier.weight(1f),
                    onClick = onToggleSim
                )
            }
        }
    }
}

@Composable
private fun TelemetryMetricItem(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = label,
            color = SlateTextMuted,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = accentColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TelemetryActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = tint.copy(alpha = 0.12f),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, tint.copy(alpha = 0.35f)),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = label, color = tint, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

// ==========================================
// SECTION D: Qibla History Modal / Drawer
// ==========================================
@Composable
private fun SectionDHistoryModal(
    history: List<QiblaHistoryItem>,
    onDismiss: () -> Unit,
    onClearHistory: () -> Unit,
    onSelectHistoryItem: (QiblaHistoryItem) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianNightGradientEnd,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "سجل المواقع المحفوظة",
                    color = ChampagneGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                if (history.isNotEmpty()) {
                    TextButton(onClick = onClearHistory) {
                        Text("مسح الكل", color = CrimsonRedWarning, fontSize = 12.sp)
                    }
                }
            }
        },
        text = {
            if (history.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد مواقع محفوظة بعد.\nاضغط على 'حفظ الموقع' لحفظ اتجاه المدينة الحالية.",
                        color = SlateTextMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(history, key = { it.timestampMs }) { item ->
                        val dateFormatted = remember(item.timestampMs) {
                            SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(item.timestampMs))
                        }

                        Surface(
                            color = GlassContainerBg,
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ChampagneGoldBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectHistoryItem(item) }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.cityName,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "$dateFormatted • ${item.distanceKm.toInt()} كم إلى مكة",
                                        color = SlateTextMuted,
                                        fontSize = 11.sp
                                    )
                                }

                                Surface(
                                    color = ChampagneGold.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "${item.qiblaAngle.toInt()}°",
                                        color = ChampagneGold,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
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

// ==========================================
// SECTION E: Developer Debug Overlay
// ==========================================
@Composable
private fun SectionEDebugOverlay(
    uiState: QiblaUiState,
    onClose: () -> Unit,
    onHeadingSliderChange: (Float) -> Unit
) {
    Surface(
        color = Color.Black.copy(alpha = 0.88f),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, IceCyanAccent),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("qibla_debug_overlay")
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(IceCyanAccent, CircleShape)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("DEBUG", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("بيانات المطورين الحية", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            DebugDataRow("FPS (معدل الإطارات)", "${uiState.fps} FPS")
            DebugDataRow("عدد إعادة الرسم (Recompositions)", "${uiState.recompositionCount}")
            DebugDataRow("نوع الحساس النشط", "TYPE_ROTATION_VECTOR (${uiState.activeSensorType})")
            DebugDataRow("شدة المجال المغناطيسي", "${String.format(Locale.US, "%.1f", uiState.magneticFieldStrength)} µT")
            DebugDataRow("Quaternion [w,x,y,z]", "[${uiState.quaternion.joinToString(", ") { String.format(Locale.US, "%.2f", it) }}]")
            DebugDataRow("Rotation Vector", "[${uiState.rotationVector.take(3).joinToString(", ") { String.format(Locale.US, "%.2f", it) }}]")
            DebugDataRow("الاتجاه المغناطيسي / الحقيقي", "${uiState.magneticHeading.toInt()}° / ${uiState.trueHeading.toInt()}°")
            DebugDataRow("دقة GPS", "${uiState.gpsAccuracyMeters?.toInt() ?: "غير معروف"} م")

            if (uiState.isSimulationMode) {
                Spacer(modifier = Modifier.height(10.dp))
                Text("شريط محاكاة الاتجاه (0° - 360°):", color = AmberGoldGlow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Slider(
                    value = uiState.manualHeading,
                    onValueChange = onHeadingSliderChange,
                    valueRange = 0f..360f,
                    colors = SliderDefaults.colors(
                        thumbColor = AmberGoldGlow,
                        activeTrackColor = AmberGoldGlow
                    )
                )
            }
        }
    }
}

@Composable
private fun DebugDataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = SlateTextMuted, fontSize = 11.sp)
        Text(value, color = IceCyanAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

// ==========================================
// Interactive Figure-8 Calibration Dialog
// ==========================================
@Composable
private fun Figure8CalibrationDialog(onDismiss: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "fig8_anim")
    val animProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fig8_progress"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianNightGradientEnd,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "دليل معايرة البوصلة (الشكل 8)",
                color = ChampagneGold,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "حرّك هاتفك في الهواء على شكل رقم (8) باللغة الإنجليزية كالموضح أدناه لمعايرة المجال المغناطيسي وإزالة التداخلات.",
                    color = SlateTextMuted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Canvas drawing Figure-8 Path and moving dot
                Canvas(
                    modifier = Modifier
                        .size(180.dp, 100.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val cx = w / 2f
                    val cy = h / 2f
                    val a = w / 2.5f

                    val path = Path()
                    var first = true
                    val pointsCount = 100

                    for (i in 0..pointsCount) {
                        val t = (i.toFloat() / pointsCount) * 2f * Math.PI
                        // Lemniscate of Gerono formula: x = a * sin(t), y = a * sin(t) * cos(t)
                        val x = cx + (a * sin(t)).toFloat()
                        val y = cy + (a * sin(t) * cos(t)).toFloat()

                        if (first) {
                            path.moveTo(x, y)
                            first = false
                        } else {
                            path.lineTo(x, y)
                        }
                    }

                    // Draw Figure-8 track
                    drawPath(
                        path = path,
                        color = ChampagneGoldBorder,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Calculate position of moving dot along t
                    val tCurrent = animProgress * 2f * Math.PI
                    val dotX = cx + (a * sin(tCurrent)).toFloat()
                    val dotY = cy + (a * sin(tCurrent) * cos(tCurrent)).toFloat()

                    // Draw moving dot with glow
                    drawCircle(
                        color = AmberGoldGlow.copy(alpha = 0.4f),
                        radius = 12.dp.toPx(),
                        center = Offset(dotX, dotY)
                    )
                    drawCircle(
                        color = AmberGoldGlow,
                        radius = 7.dp.toPx(),
                        center = Offset(dotX, dotY)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "كرر الحركة 3 مرات متتالية حتى يصبح مؤشر الدقة باللون الأخضر.",
                    color = EmeraldGreenSuccess,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = Color.Black),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("تمت المعايرة", fontWeight = FontWeight.Bold)
            }
        }
    )
}

// ==========================================
// City Selector Dialog
// ==========================================
@Composable
private fun CitySelectorDialog(
    onDismiss: () -> Unit,
    onSelectCity: (MajorCity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredCities = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            QiblaRepository.MAJOR_ISLAMIC_CITIES
        } else {
            QiblaRepository.MAJOR_ISLAMIC_CITIES.filter {
                it.nameAr.contains(searchQuery, ignoreCase = true) ||
                        it.countryAr.contains(searchQuery, ignoreCase = true) ||
                        it.nameEn.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianNightGradientEnd,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "اختر المدينة لحساب القبلة",
                color = ChampagneGold,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ابحث عن المدينة أو الدولة...", color = SlateTextMuted) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ChampagneGold) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ChampagneGold,
                        unfocusedBorderColor = ChampagneGoldBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredCities, key = { it.nameAr }) { city ->
                        val qiblaAngle = remember(city) { QiblaMath.calculateQiblaBearing(city.lat, city.lng) }

                        Surface(
                            color = GlassContainerBg,
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ChampagneGoldBorder.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectCity(city) }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = city.nameAr,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = city.countryAr,
                                        color = SlateTextMuted,
                                        fontSize = 11.sp
                                    )
                                }

                                Text(
                                    text = "${qiblaAngle.toInt()}°",
                                    color = ChampagneGold,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = SlateTextMuted)
            }
        }
    )
}

// ==========================================
// Astrolabe Procedural Canvas Background
// ==========================================
@Composable
private fun ProceduralAstrolabeBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2.3f

        // Draw concentric subtle astrolabe grid circles
        val radii = listOf(120.dp.toPx(), 220.dp.toPx(), 320.dp.toPx())
        radii.forEach { r ->
            drawCircle(
                color = ChampagneGold.copy(alpha = 0.04f),
                radius = r,
                center = Offset(cx, cy),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // Radial star grid lines
        for (i in 0 until 12) {
            val angleRad = Math.toRadians((i * 30).toDouble())
            val x = cx + 350.dp.toPx() * sin(angleRad).toFloat()
            val y = cy - 350.dp.toPx() * cos(angleRad).toFloat()
            drawLine(
                color = ChampagneGold.copy(alpha = 0.03f),
                start = Offset(cx, cy),
                end = Offset(x, y),
                strokeWidth = 1.dp.toPx()
            )
        }
    }
}

// Helper: Haptic Vibration Trigger
private fun triggerCommercialHapticPulse(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(50)
            }
        }
    } catch (_: Exception) {}
}

// Helper: Share Qibla Info
private fun shareQiblaInfo(context: Context, state: QiblaUiState) {
    val shareText = """
        🕋 اتجاه القبلة من ${state.cityName}:
        - زاوية القبلة: ${state.qiblaBearing.toInt()}°
        - المسافة إلى الكعبة المشرفة: ${state.distanceToMakkah.toInt()} كم
        تم الحساب بواسطة تطبيق ClevCalc Pro - بوصلة القبلة الاحترافية.
    """.trimIndent()

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(intent, "مشاركة اتجاه القبلة"))
}
