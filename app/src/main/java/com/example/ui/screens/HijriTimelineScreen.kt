package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.IslamicEvent
import com.example.data.IslamicEventsService
import com.example.model.CalcKey
import com.example.ui.components.ToolScreenScaffold
import com.example.ui.theme.AppIcons
import com.example.ui.theme.CustomThemeColors
import java.text.SimpleDateFormat
import java.util.*

// Visual Palette Coordinates matching Spec
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
fun HijriTimelineScreen(
    colors: CustomThemeColors,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val eventsList = remember { IslamicEventsService.getUpcomingEvents(context) }

    // Nearest event is the first one in the sorted list
    val nearestEvent = eventsList.firstOrNull()

    // Bottom sheet state for interactions
    var selectedEventForSheet by remember { mutableStateOf<IslamicEvent?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }

    ToolScreenScaffold(
        colors = colors,
        icon = AppIcons.forCalc(CalcKey.COUNTDOWN),
        title = "التقويم والمناسبات الإسلامية",
        subtitle = "متابعة المواعيد والمناسبات الهجرية والعد التنازلي مع التذكير التلقائي",
        isScrollable = false
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // --- 1. COUNTDOWN HERO COMPOSABLE (NEAREST EVENT) ---
            if (nearestEvent != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(RoyalNight, DarkBg)
                            )
                        )
                        .border(BorderStroke(1.dp, PremiumGold.copy(alpha = 0.25f)), RoundedCornerShape(24.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "أقرب مناسبة إسلامية قادمة",
                            fontSize = 12.sp,
                            color = SecondaryText,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = nearestEvent.titleAr,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.W900,
                            color = PrimaryText,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val infiniteTransition = rememberInfiniteTransition()
                        val pulseFactor by infiniteTransition.animateFloat(
                            initialValue = 0.98f,
                            targetValue = 1.02f,
                            animationSpec = infiniteRepeatable(
                                tween(1500, easing = FastOutSlowInEasing),
                                RepeatMode.Reverse
                            )
                        )

                        // Large Gold Typography Countdown
                        Text(
                            text = "${nearestEvent.getDaysRemaining()}",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.W900,
                            color = PremiumGold,
                            modifier = Modifier.graphicsLayer {
                                scaleX = pulseFactor
                                scaleY = pulseFactor
                            }
                        )
                        Text(
                            text = "أيام متبقية",
                            fontSize = 12.sp,
                            color = LuminousTurquoise,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // --- 2. CURVED TIMELINE WITH TURQUOISE PATH & GLOWING NODES ---
            Text(
                text = "جدول المناسبات الهجرية الزمنية",
                fontSize = 15.sp,
                fontWeight = FontWeight.W800,
                color = PrimaryText,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Drawing background curved timeline line using Canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(44.dp)
                        .align(Alignment.CenterStart)
                        .padding(start = 20.dp)
                ) {
                    val path = Path().apply {
                        moveTo(size.width / 2, 0f)
                        // A beautiful smooth wave/curve down the vertical path
                        cubicTo(
                            size.width / 2 - 15.dp.toPx(), size.height / 4,
                            size.width / 2 + 15.dp.toPx(), size.height * 3 / 4,
                            size.width / 2, size.height
                        )
                    }
                    drawPath(
                        path = path,
                        color = LuminousTurquoise.copy(alpha = 0.4f),
                        style = Stroke(width = 3.dp.toPx())
                    )
                }

                // Scrollable Events Timeline Node List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 48.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    itemsIndexed(eventsList) { index, event ->
                        val daysRemaining = event.getDaysRemaining()
                        val gregorianDate = event.getGregorianDateForCurrentYear()
                        val formatter = SimpleDateFormat("dd MMMM yyyy", Locale("ar"))
                        val dateFormatted = formatter.format(gregorianDate)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White.copy(alpha = 0.02f))
                                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), RoundedCornerShape(20.dp))
                                .clickable {
                                    selectedEventForSheet = event
                                    showBottomSheet = true
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Glowing Node indicator
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(LuminousTurquoise)
                                    .border(BorderStroke(3.dp, DarkBg), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = event.titleAr,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryText
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "التاريخ المتوقع: $dateFormatted",
                                    fontSize = 11.sp,
                                    color = SecondaryText
                                )
                            }

                            // Days remaining tag
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(RoyalNight)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "$daysRemaining يوم",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PremiumGold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modern Islamic bottom sheet details & reminder integration
    if (showBottomSheet && selectedEventForSheet != null) {
        val event = selectedEventForSheet!!
        val gregorianDate = event.getGregorianDateForCurrentYear()
        val formatter = SimpleDateFormat("dd MMMM yyyy", Locale("ar"))
        val dateFormatted = formatter.format(gregorianDate)

        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = Color(0xFF1E1E1E),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 44.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = event.titleAr,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.W900,
                    color = PremiumGold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // Hijri specifications
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("التاريخ المتوقع (ميلادي)", fontSize = 11.sp, color = SecondaryText)
                        Text(dateFormatted, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PrimaryText)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("الموعد الهجري الثابت", fontSize = 11.sp, color = SecondaryText)
                        Text("${event.hijriDay}/${event.hijriMonth}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LuminousTurquoise)
                    }
                }

                Divider(color = Color.White.copy(alpha = 0.08f))

                Text(
                    text = event.description,
                    fontSize = 13.sp,
                    color = SecondaryText,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )

                // Add Device Calendar Integration Reminder Button
                Button(
                    onClick = {
                        // Native Android Calendar integration contract intent insertion
                        val intent = Intent(Intent.ACTION_INSERT).apply {
                            data = CalendarContract.Events.CONTENT_URI
                            putExtra(CalendarContract.Events.TITLE, event.titleAr)
                            putExtra(CalendarContract.Events.DESCRIPTION, event.description)
                            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, gregorianDate.time)
                            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, gregorianDate.time + 3600000) // 1 Hour length
                            putExtra(CalendarContract.Events.ALL_DAY, true)
                        }
                        context.startActivity(intent)
                        showBottomSheet = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PremiumGold)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = RoyalNight,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "إضافة تذكير لتقويم الهاتف",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = RoyalNight
                    )
                }
            }
        }
    }
}
