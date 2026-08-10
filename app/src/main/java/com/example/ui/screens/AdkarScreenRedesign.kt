package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AdkarCategory
import com.example.data.AdkarItem
import com.example.data.AdkarService
import com.example.ui.components.ToolScreenScaffold
import com.example.ui.theme.CustomThemeColors
import kotlinx.coroutines.launch

// Premium Dark-Mode Colors matching Spec
private val DarkBg = Color(0xFF121212)
private val AmbientNight = Color(0xFF0B1119)
private val RoyalNight = Color(0xFF0C1E33)
private val WarmGold = Color(0xFFC29C57)
private val PremiumGold = Color(0xFFD8B56A)
private val LuminousTurquoise = Color(0xFF1FD0C5)
private val PrimaryText = Color(0xFFFFFFFF)
private val SecondaryText = Color(0xFFA0A0A0)

@Composable
fun AdkarScreenRedesign(
    colors: CustomThemeColors,
    onBackClick: () -> Unit
) {
    var activeCategory by remember { mutableStateOf(AdkarCategory.MORNING) }

    // Persistent item progress counts (maps id to current progress)
    val progressMap = remember { mutableStateMapOf<Int, Int>() }

    // Trigger animations upon completion of items
    val completedAnims = remember { mutableStateMapOf<Int, Boolean>() }

    val currentItems = remember(activeCategory) {
        AdkarService.getItemsForCategory(activeCategory)
    }

    // Determine if the current category is fully complete
    val isCategoryFullyCompleted = remember(currentItems, progressMap.size) {
        currentItems.isNotEmpty() && currentItems.all { item ->
            val current = progressMap[item.id] ?: 0
            current >= item.countTarget
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(DarkBg, AmbientNight)
                )
            )
    ) {
        // Subtle cyber ambient background grid
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 64.dp.toPx()
            val gridColor = LuminousTurquoise.copy(alpha = 0.015f)
            for (x in 0..size.width.toInt() step step.toInt()) {
                drawLine(gridColor, Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height), strokeWidth = 1f)
            }
            for (y in 0..size.height.toInt() step step.toInt()) {
                drawLine(gridColor, Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), strokeWidth = 1f)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 1. TOP APP BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "رجوع",
                        tint = PrimaryText,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = "حصن المسلم — الأذكار",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.W800,
                    color = PrimaryText,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.size(44.dp)) // Equalizer
            }

            // 2. FLOATING STICKY GLASS TAB BAR
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.03f))
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.07f)), RoundedCornerShape(24.dp))
                    .padding(vertical = 8.dp)
            ) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(AdkarCategory.values()) { category ->
                        val isSelected = category == activeCategory
                        val bgAlpha by animateFloatAsState(if (isSelected) 0.12f else 0.0f)
                        val textColor = if (isSelected) PremiumGold else SecondaryText
                        val fontWeight = if (isSelected) FontWeight.W800 else FontWeight.Normal

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(PremiumGold.copy(alpha = bgAlpha))
                                .clickable { activeCategory = category }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = category.titleAr,
                                    color = textColor,
                                    fontSize = 13.sp,
                                    fontWeight = fontWeight
                                )
                                if (isSelected) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(LuminousTurquoise)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. MAIN ADKAR LIST WITH NO FRAMES UI
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                if (isCategoryFullyCompleted) {
                    item {
                        // Success Banner when current category litany is finished
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Brush.horizontalGradient(listOf(RoyalNight, Color.Transparent)))
                                .border(BorderStroke(1.dp, PremiumGold.copy(alpha = 0.2f)), RoundedCornerShape(20.dp))
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val infiniteTransition = rememberInfiniteTransition()
                                val pulseScale by infiniteTransition.animateFloat(
                                    initialValue = 0.96f,
                                    targetValue = 1.04f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1200, easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Reverse
                                    )
                                )

                                Text(
                                    text = "أتممت وردك اليومي",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.W900,
                                    color = PremiumGold,
                                    modifier = Modifier.graphicsLayer {
                                        scaleX = pulseScale
                                        scaleY = pulseScale
                                    },
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "تقبل الله منا ومنكم صالح الأعمال وغفر لنا ولكم.",
                                    fontSize = 12.sp,
                                    color = LuminousTurquoise,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                itemsIndexed(currentItems) { index, item ->
                    val currentCount = progressMap[item.id] ?: 0
                    val isFinished = currentCount >= item.countTarget

                    // Animated Rising Light effect
                    var startCompletedAnimation by remember { mutableStateOf(false) }
                    val riseProgress by animateFloatAsState(
                        targetValue = if (isFinished) 1.0f else 0.0f,
                        animationSpec = tween(700, easing = FastOutSlowInEasing)
                    )

                    var isPressed by remember { mutableStateOf(false) }
                    val itemScale by animateFloatAsState(if (isPressed) 0.98f else 1.0f)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(itemScale)
                            .pointerInput(item.id) {
                                detectTapGestures(
                                    onPress = {
                                        if (!isFinished) {
                                            isPressed = true
                                            tryAwaitRelease()
                                            isPressed = false
                                        }
                                    },
                                    onTap = {
                                        if (!isFinished) {
                                            val next = currentCount + 1
                                            progressMap[item.id] = next
                                            if (next >= item.countTarget) {
                                                completedAnims[item.id] = true
                                            }
                                        }
                                    }
                                )
                            }
                    ) {
                        // NO FRAMES BACKGROUND: Free floating canvas element with dynamic rising light gradient
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.White.copy(alpha = 0.02f))
                        ) {
                            // Gradient Light Rise: Ambient glowing backdrop that slowly ascends on finish
                            if (riseProgress > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(riseProgress)
                                        .align(Alignment.BottomCenter)
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    LuminousTurquoise.copy(alpha = 0.08f * riseProgress),
                                                    PremiumGold.copy(alpha = 0.04f * riseProgress)
                                                )
                                            )
                                        )
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp)
                            ) {
                                // Arabic Litany Text
                                Text(
                                    text = item.text,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryText,
                                    lineHeight = 32.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Reward Statement
                                if (item.rewardText.isNotBlank()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.Black.copy(alpha = 0.15f))
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 2.dp)
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(PremiumGold)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = item.rewardText,
                                            fontSize = 12.sp,
                                            color = SecondaryText,
                                            lineHeight = 18.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                }

                                // Reference footnote
                                if (item.reference.isNotBlank()) {
                                    Text(
                                        text = item.reference,
                                        fontSize = 11.sp,
                                        color = WarmGold,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.align(Alignment.End)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                }

                                // Bottom Row Controls & Progress Counter
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Progress Indicators
                                    Column {
                                        Text(
                                            text = if (isFinished) "تمت قراءته ✓" else "التكرار المطلوب",
                                            fontSize = 11.sp,
                                            color = if (isFinished) LuminousTurquoise else SecondaryText,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "$currentCount / ${item.countTarget}",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.W900,
                                            color = if (isFinished) LuminousTurquoise else PremiumGold
                                        )
                                    }

                                    // Action Round Button with progress arc
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                            .background(if (isFinished) LuminousTurquoise else Color.White.copy(alpha = 0.05f))
                                            .border(
                                                BorderStroke(1.5.dp, if (isFinished) LuminousTurquoise else PremiumGold.copy(alpha = 0.3f)),
                                                CircleShape
                                            )
                                            .clickable(enabled = !isFinished) {
                                                val next = currentCount + 1
                                                progressMap[item.id] = next
                                                if (next >= item.countTarget) {
                                                    completedAnims[item.id] = true
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isFinished) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "مكتمل",
                                                tint = RoyalNight,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        } else {
                                            Text(
                                                text = "${item.countTarget - currentCount}",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Black,
                                                color = PremiumGold
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
    }
}
