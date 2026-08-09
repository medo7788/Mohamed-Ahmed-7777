package com.example.ui.screens

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CalcKey
import com.example.ui.theme.AppIcons
import com.example.ui.theme.CustomThemeColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

// ==========================================
// CHAMBER OF TIME BRANDING SEMANTICS
// ==========================================
private val ColorAmbientBg = Color(0xFF0B1119)
private val ColorRoyalNightBlue = Color(0xFF0C1E33)
private val ColorWarmGold = Color(0xFFC29C57)
private val ColorLuminousTurquoise = Color(0xFF1FD0C5)
private val ColorPrimaryText = Color(0xFFF8F4EA)
private val ColorSecondaryText = Color(0xFF98A3AD)

private val ColorGlassSurface = Color.White.copy(alpha = 0.03f)
private val ColorGlassBorder = Color.White.copy(alpha = 0.08f)

data class TimeToolModel(
    val id: String,
    val title: String,
    val description: String,
    val calcKey: CalcKey,
    val status: String? = null // "LIVE", "SYNC", "TICKING"
)

// Clean Data Model Separation
val CHAMBER_TOOLS_LIST = listOf(
    TimeToolModel(
        id = "date-calc",
        title = "حاسبة التاريخ",
        description = "احسب الفواصل والتواريخ بدقة",
        calcKey = CalcKey.DATE,
        status = "SYNC"
    ),
    TimeToolModel(
        id = "world-clock",
        title = "التوقيت العالمي",
        description = "اعرف الوقت في مدن العالم",
        calcKey = CalcKey.WORLD_TIME,
        status = "LIVE"
    ),
    TimeToolModel(
        id = "countdown",
        title = "العد التنازلي",
        description = "أنشئ مؤقتًا لأي موعد مهم",
        calcKey = CalcKey.COUNTDOWN,
        status = "TICKING"
    ),
    TimeToolModel(
        id = "age-calc",
        title = "حاسبة العمر",
        description = "اعرف عمرك بالتفصيل",
        calcKey = CalcKey.AGE
    )
)

// ==========================================
// MAIN CHAMBER OF TIME SCREEN
// ==========================================
@Composable
fun TimeDatesHubScreen(
    colors: CustomThemeColors,
    favoriteTools: Set<String>,
    onToggleFavorite: (CalcKey) -> Unit,
    onToolClick: (CalcKey) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var isSearchExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Live Clock ticking state (re-triggers every second)
    var currentTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var triggerMidnightPulse by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            val now = System.currentTimeMillis()
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = now
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            val second = calendar.get(Calendar.SECOND)

            // Trigger Midnight Pulse Event at 00:00:00
            if (hour == 0 && minute == 0 && second == 0) {
                triggerMidnightPulse = true
                delay(1200)
                triggerMidnightPulse = false
            }

            currentTimeMillis = now
            delay(1000)
        }
    }

    // Filter tools based on search query
    val filteredTools = remember(searchQuery) {
        CHAMBER_TOOLS_LIST.filter { tool ->
            searchQuery.isBlank() ||
                    tool.title.contains(searchQuery, ignoreCase = true) ||
                    tool.description.contains(searchQuery, ignoreCase = true)
        }
    }

    // Responsive columns (2 columns on mobile, 4 on tablet)
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val columnsCount = if (screenWidthDp >= 600) 4 else 2

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ColorAmbientBg)
    ) {
        // 1. Ambient Background Layer (Celestial glowing stars)
        AmbientChamberStarsBackdrop()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 2. Arch AppBar
            TimeChamberAppBar(
                title = "الوقت والتاريخ",
                onBackClick = onBackClick,
                isSearchExpanded = isSearchExpanded,
                onToggleSearch = { isSearchExpanded = !isSearchExpanded }
            )

            // Dynamic search field
            AnimatedVisibility(
                visible = isSearchExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = ColorRoyalNightBlue.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, ColorGlassBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, null, tint = ColorWarmGold, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("ابحث عن أي أداة زمنية...", fontSize = 12.sp, color = ColorSecondaryText) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = ColorPrimaryText,
                                unfocusedTextColor = ColorPrimaryText
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 3. The Time Arch & Live Clock Core (Perspective 3D Skew)
            TimeArchGateway(
                currentTimeMillis = currentTimeMillis,
                midnightPulse = triggerMidnightPulse
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 4. Glassmorphic Squircle Tool Grid
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (filteredTools.isEmpty()) {
                    Text(
                        text = "لم يتم العثور على نتائج للبحث",
                        color = ColorSecondaryText,
                        modifier = Modifier.align(Alignment.Center),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columnsCount),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 90.dp, top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredTools, key = { it.id }) { tool ->
                            TimeChamberToolCard(
                                tool = tool,
                                isPinned = favoriteTools.contains(tool.calcKey.name),
                                onTogglePin = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onToggleFavorite(tool.calcKey)
                                },
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onToolClick(tool.calcKey)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. AMBIENT BACKGROUND WITH TWINKLING STARS
// ==========================================
@Composable
fun AmbientChamberStarsBackdrop() {
    val infiniteTransition = rememberInfiniteTransition(label = "stars")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "twinkle"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Subtle dark blue radial halo in the background center
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(ColorRoyalNightBlue.copy(alpha = 0.35f), Color.Transparent),
                center = Offset(width * 0.5f, height * 0.35f),
                radius = width * 0.65f
            )
        )

        // Draw twinkling celestial points (clean procedural stars)
        val stars = listOf(
            Offset(width * 0.15f, height * 0.15f) to 2f,
            Offset(width * 0.85f, height * 0.12f) to 2.5f,
            Offset(width * 0.25f, height * 0.45f) to 1.5f,
            Offset(width * 0.72f, height * 0.38f) to 3f,
            Offset(width * 0.08f, height * 0.68f) to 1.8f,
            Offset(width * 0.92f, height * 0.72f) to 2.2f,
            Offset(width * 0.5f, height * 0.85f) to 2f
        )

        stars.forEach { (pos, sizeVal) ->
            drawCircle(
                color = ColorPrimaryText.copy(alpha = alphaAnim),
                radius = sizeVal.dp.toPx(),
                center = pos
            )
        }
    }
}

// ==========================================
// 4. ARCH APP BAR COMPOSABLE
// ==========================================
@Composable
fun TimeChamberAppBar(
    title: String,
    onBackClick: () -> Unit,
    isSearchExpanded: Boolean,
    onToggleSearch: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left Architectural Halo Menu button
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(ColorRoyalNightBlue.copy(alpha = 0.25f), Color.Transparent)
                    )
                )
                .border(1.dp, ColorGlassBorder, CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "رجوع",
                tint = ColorPrimaryText,
                modifier = Modifier.size(20.dp)
            )
        }

        // Center centered title
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = ColorPrimaryText,
            textAlign = TextAlign.Center
        )

        // Right Architectural Halo Search button
        IconButton(
            onClick = onToggleSearch,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            if (isSearchExpanded) ColorLuminousTurquoise.copy(alpha = 0.15f) else ColorRoyalNightBlue.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    )
                )
                .border(1.dp, if (isSearchExpanded) ColorLuminousTurquoise.copy(alpha = 0.5f) else ColorGlassBorder, CircleShape)
        ) {
            Icon(
                imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                contentDescription = "بحث",
                tint = if (isSearchExpanded) ColorLuminousTurquoise else ColorPrimaryText,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ==========================================
// 5. THE TIME ARCH GATEWAY (PORTAL LOOK)
// ==========================================
@Composable
fun TimeArchGateway(
    currentTimeMillis: Long,
    midnightPulse: Boolean
) {
    // Clock formatted string
    val formattedTime = remember(currentTimeMillis) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale("ar"))
        sdf.format(Date(currentTimeMillis))
    }

    // Dates formatted string
    val gregorianStr = remember(currentTimeMillis) {
        val sdf = SimpleDateFormat("d MMMM yyyy", Locale("ar"))
        sdf.format(Date(currentTimeMillis))
    }

    val hijriStr = remember(currentTimeMillis) {
        val hc = GregorianCalendar()
        val hYear = hc.get(Calendar.YEAR) - 579
        val hMonths = listOf("محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة")
        "15 ${hMonths[((hc.get(Calendar.MONTH) + 5) % 12)]} $hYear هـ"
    }

    // Midnight Pulse state transition
    val pulseSize by animateFloatAsState(
        targetValue = if (midnightPulse) 1.15f else 1.0f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "pulse"
    )

    val pulseGlowAlpha by animateFloatAsState(
        targetValue = if (midnightPulse) 0.9f else 0.25f,
        animationSpec = tween(600, easing = LinearOutSlowInEasing),
        label = "glow"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Arch Background Portal & Celestial Dots
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Glow Behind the Arch Portal
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(ColorLuminousTurquoise.copy(alpha = pulseGlowAlpha), Color.Transparent),
                    center = Offset(width / 2f, height / 2f),
                    radius = 90.dp.toPx()
                )
            )

            // Draw Celestial Arch Gateway path
            val path = Path().apply {
                val rect = Rect(width * 0.12f, height * 0.1f, width * 0.88f, height * 1.5f)
                addOval(rect)
            }

            // Draw outer and inner portal lines
            drawPath(
                path = path,
                color = ColorWarmGold.copy(alpha = 0.22f),
                style = Stroke(width = 3.dp.toPx())
            )
            drawPath(
                path = path,
                color = ColorLuminousTurquoise.copy(alpha = 0.15f),
                style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 15f), 0f))
            )
        }

        // Live Clock Core with 3D Skew / Perspective Matrix transform
        Column(
            modifier = Modifier
                .scale(pulseSize)
                .graphicsLayer {
                    // Apply 15-degree subtle structural skew as requested
                    rotationX = 15f
                    cameraDistance = 8 * density
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Ticking time with Gold -> Turquoise gradient text look
            Text(
                text = formattedTime,
                fontSize = 44.sp,
                fontWeight = FontWeight.W900,
                letterSpacing = 2.sp,
                color = ColorPrimaryText,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer { alpha = 0.95f }
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Gregorian • Hijri Date line
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = gregorianStr,
                    fontSize = 12.sp,
                    color = ColorSecondaryText,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Golden Glowing Dot separator
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(ColorWarmGold)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = hijriStr,
                    fontSize = 11.sp,
                    color = ColorWarmGold,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ==========================================
// 6. ASYMMETRIC GLASS SQUIRCLE CARD
// ==========================================
@Composable
fun TimeChamberToolCard(
    tool: TimeToolModel,
    isPinned: Boolean,
    onTogglePin: () -> Unit,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "cardScale"
    )

    // Setup active glows depending on type
    val infiniteTransition = rememberInfiniteTransition(label = "rim")

    // LIVE Moving Pulse
    val livePulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "live"
    )

    // SYNC Breathing Glow
    val syncPulse by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sync"
    )

    // TICKING Seconds Hand Dot
    val tickingDotAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ticking"
    )

    Surface(
        modifier = Modifier
            .scale(cardScale)
            .fillMaxWidth()
            .height(145.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            }
            .semantics {
                role = Role.Button
                contentDescription = "${tool.title}: ${tool.description}"
            },
        color = ColorGlassSurface,
        shape = RoundedCornerShape(
            topStart = 25.dp, // Asymmetric Squircle curves
            bottomEnd = 25.dp,
            topEnd = 16.dp,
            bottomStart = 16.dp
        ),
        border = BorderStroke(1.dp, ColorGlassBorder)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Glowing Rim Systems (Using low overhead gradients instead of heavy filters)
            if (tool.status == "LIVE") {
                // Moving Turquoise Pulse border indicator
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val archPath = Path().apply {
                        addRoundRect(
                            androidx.compose.ui.geometry.RoundRect(
                                rect = Rect(0f, 0f, size.width, size.height),
                                topLeft = androidx.compose.ui.geometry.CornerRadius(25.dp.toPx()),
                                bottomRight = androidx.compose.ui.geometry.CornerRadius(25.dp.toPx()),
                                topRight = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                                bottomLeft = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx())
                            )
                        )
                    }
                    drawPath(
                        path = archPath,
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                ColorLuminousTurquoise.copy(alpha = 0.7f),
                                Color.Transparent,
                                Color.Transparent
                            ),
                            center = Offset(size.width / 2f, size.height / 2f)
                        ),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }

            if (tool.status == "SYNC") {
                // Gold breathing rim indicator
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val archPath = Path().apply {
                        addRoundRect(
                            androidx.compose.ui.geometry.RoundRect(
                                rect = Rect(0f, 0f, size.width, size.height),
                                topLeft = androidx.compose.ui.geometry.CornerRadius(25.dp.toPx()),
                                bottomRight = androidx.compose.ui.geometry.CornerRadius(25.dp.toPx()),
                                topRight = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                                bottomLeft = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx())
                            )
                        )
                    }
                    drawPath(
                        path = archPath,
                        color = ColorWarmGold.copy(alpha = syncPulse * 0.45f),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }

            if (tool.status == "TICKING") {
                // Single revolving turquoise dot trailing along boundary
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val rad = Math.toRadians(tickingDotAngle.toDouble()).toFloat()
                    val pathCenterX = size.width / 2f
                    val pathCenterY = size.height / 2f
                    val dotX = pathCenterX + cos(rad) * (size.width / 2f)
                    val dotY = pathCenterY + sin(rad) * (size.height / 2f)

                    drawCircle(
                        color = ColorLuminousTurquoise,
                        radius = 4.dp.toPx(),
                        center = Offset(dotX.coerceIn(4f, size.width - 4f), dotY.coerceIn(4f, size.height - 4f))
                    )
                }
            }

            // Pin / Favorite action button
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable { onTogglePin() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                    contentDescription = "تثبيت الأداة",
                    tint = if (isPinned) ColorWarmGold else ColorSecondaryText,
                    modifier = Modifier.size(14.dp)
                )
            }

            // Main content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Fine geometry vectors instead of flat icons
                Icon(
                    imageVector = AppIcons.forCalc(tool.calcKey),
                    contentDescription = null,
                    tint = ColorWarmGold,
                    modifier = Modifier.size(38.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = tool.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorPrimaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = tool.description,
                    fontSize = 11.sp,
                    color = ColorSecondaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
