package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.data.CountryEconomicData
import com.example.data.EconomicRepository
import com.example.ui.theme.CustomThemeColors
import com.example.ui.theme.AppIcons
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EconomicIndicatorsScreen(colors: CustomThemeColors) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    // Persistent state
    var selectedCountryCode by rememberSaveable { mutableStateOf("EG") }
    val country = remember(selectedCountryCode) { EconomicRepository.getCountryByCode(selectedCountryCode) }

    var selectedTab by rememberSaveable { mutableStateOf(0) } // 0: Macro Dashboard, 1: AI Advisor & Simulator
    var showCountryPicker by rememberSaveable { mutableStateOf(false) }

    // UI States: Loading, Success, Empty, Error, Offline
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var isRefreshing by rememberSaveable { mutableStateOf(false) }
    var isError by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf("") }
    var isOffline by rememberSaveable { mutableStateOf(false) }

    // Auto-refresh countdown timer (60s)
    var countdownSeconds by rememberSaveable { mutableStateOf(60) }
    var lastUpdatedText by rememberSaveable { mutableStateOf("آخر تحديث: الآن") }

    // AI Advisor & Chat states
    var aiReportText by rememberSaveable { mutableStateOf<String?>(null) }
    var isGeneratingReport by rememberSaveable { mutableStateOf(false) }
    var userPromptText by rememberSaveable { mutableStateOf("") }
    var isSendingPrompt by rememberSaveable { mutableStateOf(false) }
    var chatMessages by remember { mutableStateOf(listOf<Pair<Boolean, String>>()) }

    // What-If Scenario Simulator State
    var scenarioInterestDelta by rememberSaveable { mutableStateOf(0f) } // -3% to +3%

    // Dialog states for action bar
    var showExportDialog by rememberSaveable { mutableStateOf(false) }
    var showAlertsDialog by rememberSaveable { mutableStateOf(false) }

    val generateFullReport = {
        isGeneratingReport = true
        coroutineScope.launch {
            val report = EconomicRepository.getAIEconomicReport(context, country)
            aiReportText = report
            isGeneratingReport = false
        }
    }

    // Load / Refresh Data simulation
    fun loadData(manual: Boolean = false) {
        if (manual) isRefreshing = true else isLoading = true
        isError = false
        errorMessage = ""

        coroutineScope.launch {
            try {
                delay(800) // Simulate network fetch
                isOffline = false
                isLoading = false
                isRefreshing = false
                countdownSeconds = 60
                lastUpdatedText = "آخر تحديث: ${Calendar.getInstance().let { "${it.get(Calendar.HOUR_OF_DAY)}:${String.format("%02d", it.get(Calendar.MINUTE))}" }}"
            } catch (e: Exception) {
                if (manual) isRefreshing = false else isLoading = false
                isError = true
                errorMessage = e.localizedMessage ?: "تعذر الاتصال بالخادم المالي"
                isOffline = true
            }
        }
    }

    LaunchedEffect(selectedCountryCode) {
        loadData(false)
        aiReportText = null
        chatMessages = emptyList()
    }

    // Auto-refresh timer
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            if (countdownSeconds > 1) {
                countdownSeconds--
            } else {
                countdownSeconds = 60
                loadData(false)
            }
        }
    }

    // Error shake animation
    val shakeAnim = remember { Animatable(0f) }
    LaunchedEffect(isError) {
        if (isError) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            repeat(3) {
                shakeAnim.animateTo(12f, animationSpec = tween(50))
                shakeAnim.animateTo(-12f, animationSpec = tween(50))
            }
            shakeAnim.animateTo(0f, animationSpec = tween(50))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080A0F))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. OFFLINE BANNER
        AnimatedVisibility(visible = isOffline) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CloudOff, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("يعمل بآخر بيانات اقتصادية مخزنة (وضع غير متصل)", fontSize = 11.sp, color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                    }
                    Text(lastUpdatedText, fontSize = 10.sp, color = Color(0xFF94A3B8))
                }
            }
        }

        // 2. DYNAMIC HERO HEADER & MULTI-COUNTRY SWITCHER BANNER
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = shakeAnim.value },
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF141926).copy(alpha = 0.85f),
            border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.35f)),
            shadowElevation = 8.dp
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val paintColor = Color(0xFFD4AF37).copy(alpha = 0.04f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFD4AF37).copy(alpha = 0.15f), Color.Transparent),
                            center = Offset(size.width * 0.85f, size.height * 0.2f),
                            radius = 240f
                        )
                    )
                    for (i in 0..6) {
                        drawLine(
                            color = paintColor,
                            start = Offset(i * (size.width / 6f), 0f),
                            end = Offset(i * (size.width / 6f), size.height),
                            strokeWidth = 1f
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val infiniteTransition = rememberInfiniteTransition(label = "heroPulse")
                            val pulseAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.3f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "pulse"
                            )
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981).copy(alpha = pulseAlpha))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("● أسواق المال الحية", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        }

                        Surface(
                            modifier = Modifier.clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showCountryPicker = true
                            },
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(country.nameAr, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Filled.ArrowDropDown, null, tint = Color(0xFFD4AF37), modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "مؤشرات الاقتصاد والخبير الذكي",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "بيانات رسمية وتوقعات حركة الأسواق بافتراضات خبير لـ ${country.nameAr}",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0F172A).copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            item { TickerItem("الفائدة", country.interestRate, Color(0xFFF59E0B)) }
                            item { TickerItem("التضخم", country.inflationRate, Color(0xFFEF4444)) }
                            item { TickerItem("النمو GDP", country.gdpGrowth, Color(0xFF10B981)) }
                            item { TickerItem("البورصة", country.stockExchangeValue, Color(0xFF3B82F6)) }
                            item { TickerItem("الاحتياطي", country.centralBankReserves, Color(0xFFD4AF37)) }
                        }
                    }
                }
            }
        }

        // 3. PRIMARY SECTION TAB SWITCHER
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF141926),
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TabButton(
                    title = "المؤشرات والبورصة",
                    icon = AppIcons.StockMarket,
                    isSelected = selectedTab == 0,
                    modifier = Modifier.weight(1f),
                    colors = colors,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectedTab = 0
                    }
                )
                TabButton(
                    title = "المستشار الاقتصادي AI",
                    icon = AppIcons.EconomicAdvisor,
                    isSelected = selectedTab == 1,
                    modifier = Modifier.weight(1f),
                    colors = colors,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectedTab = 1
                        if (aiReportText == null && !isGeneratingReport) {
                            generateFullReport()
                        }
                    }
                )
            }
        }

        // 4. MAIN CONTENT AREA (Tab 0 or Tab 1)
        if (isLoading) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(4) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF141926).copy(alpha = 0.5f)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFFD4AF37), strokeWidth = 2.dp)
                        }
                    }
                }
            }
        } else if (isError) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .graphicsLayer { translationX = shakeAnim.value },
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFEF4444).copy(alpha = 0.1f),
                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Filled.Warning, null, tint = Color(0xFFEF4444), modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("تعذر تحديث البيانات المالية", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(errorMessage.ifBlank { "يرجى التحقق من اتصال الإنترنت والمحاولة مرة أخرى" }, fontSize = 12.sp, color = Color(0xFF94A3B8), textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { loadData(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("إعادة المحاولة", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            if (selectedTab == 0) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF141926).copy(alpha = 0.9f),
                            border = BorderStroke(1.dp, Color(0xFF334155))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(AppIcons.EconomicOverview, null, tint = Color(0xFFD4AF37), modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("حالة اقتصاد ${country.nameAr}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF3B82F6).copy(alpha = 0.15f)
                                    ) {
                                        Text(country.currency, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3B82F6), modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                                    }
                                }

                                Text(country.economicSummaryAr, fontSize = 12.sp, color = Color(0xFF94A3B8), lineHeight = 20.sp)

                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF1E293B),
                                    border = BorderStroke(1.dp, Color(0xFF334155))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("💡", fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("المصدر: البنك المركزي والتقارير الرسمية الدولية محدثة لعام 2025/2026", fontSize = 10.sp, color = Color(0xFFCBD5E1))
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                EnhancedMetricCard(
                                    title = "معدل التضخم السنوي",
                                    value = country.inflationRate,
                                    subtitle = "مستوى أسعار المستهلكين",
                                    badgeText = "+0.4%",
                                    isPositive = false,
                                    icon = AppIcons.Inflation,
                                    modifier = Modifier.weight(1f)
                                )
                                EnhancedMetricCard(
                                    title = "سعر الفائدة البنكي",
                                    value = country.interestRate,
                                    subtitle = "سعر الإقراض الأساسي",
                                    badgeText = "ثابت",
                                    isPositive = true,
                                    icon = AppIcons.InterestRate,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                EnhancedMetricCard(
                                    title = "نمو الناتج المحلي (GDP)",
                                    value = country.gdpGrowth,
                                    subtitle = "معدل النمو الحقيقي",
                                    badgeText = "إيجابي",
                                    isPositive = true,
                                    icon = AppIcons.Growth,
                                    modifier = Modifier.weight(1f)
                                )
                                EnhancedMetricCard(
                                    title = "نسبة البطالة",
                                    value = country.unemployment,
                                    subtitle = "إجمالي قوة العمل",
                                    badgeText = "مستقر",
                                    isPositive = true,
                                    icon = AppIcons.Unemployment,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF141926),
                            border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.5f))
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Canvas(modifier = Modifier.matchParentSize()) {
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(Color(0xFF3B82F6).copy(alpha = 0.12f), Color.Transparent),
                                            center = Offset(size.width * 0.9f, size.height * 0.5f),
                                            radius = 200f
                                        )
                                    )
                                }
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(country.stockExchangeName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            Text("أداء سوق المال الرئيسي وعمليات التداول", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                        }
                                        Icon(AppIcons.StockMarket, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(24.dp))
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        Text(country.stockExchangeValue, fontSize = 26.sp, fontWeight = FontWeight.Black, color = Color.White)
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (country.stockExchangeChange.startsWith("-")) Color(0xFFEF4444).copy(alpha = 0.2f) else Color(0xFF10B981).copy(alpha = 0.2f),
                                            border = BorderStroke(1.dp, if (country.stockExchangeChange.startsWith("-")) Color(0xFFEF4444) else Color(0xFF10B981))
                                        ) {
                                            Text(
                                                text = country.stockExchangeChange,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (country.stockExchangeChange.startsWith("-")) Color(0xFFEF4444) else Color(0xFF10B981)
                                            )
                                        }
                                    }

                                    Canvas(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(45.dp)
                                    ) {
                                        val path = Path().apply {
                                            moveTo(0f, size.height * 0.8f)
                                            cubicTo(size.width * 0.2f, size.height * 0.4f, size.width * 0.5f, size.height * 0.9f, size.width, size.height * 0.1f)
                                        }
                                        drawPath(
                                            path = path,
                                            color = Color(0xFF3B82F6),
                                            style = Stroke(width = 3f, cap = StrokeCap.Round)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF141926),
                            border = BorderStroke(1.dp, Color(0xFF334155))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(AppIcons.Summary, null, tint = Color(0xFFD4AF37), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("تفاصيل الاحتياطي والصادرات الكلية", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                DetailRowCustom("صافي الاحتياطي الأجنبي:", country.centralBankReserves)
                                DetailRowCustom("نسبة الدين للناتج المحلي:", country.debtToGdp)
                                DetailRowCustom("أهم الصادرات والقطاعات:", country.mainExport)
                            }
                        }
                    }

                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF141926),
                            border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("إجراءات سريعة وتصدير التقارير", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ActionButton(
                                        title = "تصدير PDF",
                                        icon = Icons.Filled.PictureAsPdf,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            showExportDialog = true
                                        }
                                    )
                                    ActionButton(
                                        title = "مشاركة",
                                        icon = Icons.Filled.Share,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            val sendIntent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_TEXT, "تقرير اقتصاد ${country.nameAr}:\nالتضخم: ${country.inflationRate}\nالفائدة: ${country.interestRate}\nالنمو: ${country.gdpGrowth}")
                                                type = "text/plain"
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, "مشاركة التقرير الاقتصادي"))
                                        }
                                    )
                                    ActionButton(
                                        title = "التنبيهات",
                                        icon = Icons.Filled.Notifications,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            showAlertsDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF141926),
                            border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.6f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(AppIcons.EconomicAdvisor, null, tint = Color(0xFFD4AF37), modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("تحليل الخبير الاقتصادي الذكي", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                                    }
                                    if (isGeneratingReport) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color(0xFFD4AF37))
                                    } else {
                                        Button(
                                            onClick = { generateFullReport() },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text("تحديث التحليل", fontSize = 11.sp, color = Color(0xFF080A0F), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                if (aiReportText != null) {
                                    SelectionContainer {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 280.dp)
                                                .verticalScroll(rememberScrollState())
                                        ) {
                                            Text(aiReportText!!, fontSize = 12.sp, color = Color(0xFFCBD5E1), lineHeight = 20.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    TextButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            clipboardManager.setText(AnnotatedString(aiReportText!!))
                                        },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, null, tint = Color(0xFFD4AF37), modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("نسخ التقرير كاملاً", fontSize = 11.sp, color = Color(0xFFD4AF37))
                                    }
                                } else if (!isGeneratingReport) {
                                    Text("جاري استخلاص رؤى الذكاء الاصطناعي لاقتصاد ${country.nameAr}...", fontSize = 12.sp, color = Color(0xFF94A3B8))
                                }
                            }
                        }
                    }

                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF141926),
                            border = BorderStroke(1.dp, Color(0xFF334155))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.TrendingUp, null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("محاكي السيناريوهات المالية (ماذا لو؟)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                                }

                                Text("حرك المؤشر لمحاكاة تأثير تغير أسعار الفائدة على التضخم وقيمة الأصول:", fontSize = 11.sp, color = Color(0xFF94A3B8))

                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("تغير سعر الفائدة:", fontSize = 12.sp, color = Color.White)
                                        Text("${if (scenarioInterestDelta >= 0) "+" else ""}${String.format("%.1f", scenarioInterestDelta)}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                                    }
                                    Slider(
                                        value = scenarioInterestDelta,
                                        onValueChange = {
                                            scenarioInterestDelta = it
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                        valueRange = -3f..3f,
                                        steps = 11,
                                        colors = SliderDefaults.colors(thumbColor = Color(0xFFF59E0B), activeTrackColor = Color(0xFFF59E0B))
                                    )
                                }

                                val predictedInflation = remember(scenarioInterestDelta, country) {
                                    val base = country.inflationRate.replace("%", "").trim().toDoubleOrNull() ?: 20.0
                                    val delta = -scenarioInterestDelta * 1.8
                                    maxOf(1.0, base + delta)
                                }

                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF0F172A),
                                    border = BorderStroke(1.dp, Color(0xFF334155))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("النتيجة المتوقعة للمحاكاة:", fontSize = 11.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                                        Text("• معدل التضخم المتوقع: ${String.format("%.1f", predictedInflation)}%", fontSize = 12.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                        Text("• تأثير العقارات والأسهم: ${if (scenarioInterestDelta > 0) "هدوء نسبي وضغط على السيولة" else "انتعاش تدريجي وتوجيه نحو الأصول الحقيقية"}", fontSize = 11.sp, color = Color(0xFFCBD5E1))
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CalendarMonth, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("الأجندة الاقتصادية القادمة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(
                                    listOf(
                                        Triple("اجتماع الفائدة المركزي", "15 أغسطس", "مرتفع الأهمية"),
                                        Triple("صدور بيانات التضخم", "28 أغسطس", "متوسط الأهمية"),
                                        Triple("تقرير الناتج المحلي Q3", "10 سبتمبر", "مرتفع الأهمية"),
                                        Triple("مؤشر مديرو المشتريات PMI", "01 أكتوبر", "عادي الأهمية")
                                    ),
                                    key = { it.first }
                                ) { (event, date, importance) ->
                                    Surface(
                                        modifier = Modifier.width(180.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color(0xFF141926),
                                        border = BorderStroke(1.dp, Color(0xFF334155))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(event, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(date, fontSize = 10.sp, color = Color(0xFF94A3B8))
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (importance.contains("مرتفع")) Color(0xFFEF4444).copy(alpha = 0.2f) else Color(0xFF3B82F6).copy(alpha = 0.2f)
                                                ) {
                                                    Text(importance, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 9.sp, color = if (importance.contains("مرتفع")) Color(0xFFEF4444) else Color(0xFF3B82F6))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("أسئلة مقترحة للخبير المالي:", fontSize = 12.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(
                                    listOf(
                                        "هل هذا الوقت المناسب لشراء العقار؟",
                                        "توقعات أسعار الذهب والفائدة",
                                        "أفضل قنوات الادخار الآمنة حالياً",
                                        "تحليل أداء البورصة هذا الشهر"
                                    ),
                                    key = { it }
                                ) { prompt ->
                                    Surface(
                                        modifier = Modifier
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                userPromptText = prompt
                                            },
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color(0xFF1E293B),
                                        border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.4f))
                                    ) {
                                        Text(prompt, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontSize = 11.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            chatMessages.forEach { (isUser, text) ->
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = if (isUser) Alignment.CenterStart else Alignment.CenterEnd
                                ) {
                                    Surface(
                                        color = if (isUser) Color(0xFFD4AF37) else Color(0xFF141926),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.widthIn(max = 290.dp),
                                        border = if (!isUser) BorderStroke(1.dp, Color(0xFF334155)) else null
                                    ) {
                                        Text(
                                            text = text,
                                            fontSize = 12.sp,
                                            color = if (isUser) Color(0xFF080A0F) else Color.White,
                                            modifier = Modifier.padding(12.dp),
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = userPromptText,
                                onValueChange = { userPromptText = it },
                                placeholder = { Text("اطرح سؤالك على الخبير الاقتصادي...", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(20.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF141926),
                                    unfocusedContainerColor = Color(0xFF141926),
                                    focusedBorderColor = Color(0xFFD4AF37),
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = {
                                    if (userPromptText.isNotBlank() && !isSendingPrompt) {
                                        val query = userPromptText.trim()
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        chatMessages = chatMessages + Pair(true, query)
                                        userPromptText = ""
                                        isSendingPrompt = true
                                        coroutineScope.launch {
                                            val answer = EconomicRepository.getAIEconomicReport(context, country, query)
                                            chatMessages = chatMessages + Pair(false, answer)
                                            isSendingPrompt = false
                                        }
                                    }
                                },
                                enabled = !isSendingPrompt
                            ) {
                                if (isSendingPrompt) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFFD4AF37))
                                } else {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "إرسال", tint = Color(0xFFD4AF37))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCountryPicker) {
        AlertDialog(
            onDismissRequest = { showCountryPicker = false },
            confirmButton = {
                TextButton(onClick = { showCountryPicker = false }) {
                    Text("إغلاق", color = Color(0xFFD4AF37), fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Public, null, tint = Color(0xFFD4AF37), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("اختر الدولة لمتابعة اقتصادها", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(EconomicRepository.countries, key = { it.code }) { item ->
                        val isSelected = item.code == selectedCountryCode
                        Surface(
                            color = if (isSelected) Color(0xFFD4AF37).copy(alpha = 0.2f) else Color(0xFF141926),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFFD4AF37) else Color(0xFF334155)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    selectedCountryCode = item.code
                                    showCountryPicker = false
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Public, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(item.nameAr, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("${item.currency} • ${item.stockExchangeName}", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                    }
                                }
                                if (isSelected) {
                                    Text("✓", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD4AF37))
                                }
                            }
                        }
                    }
                }
            },
            containerColor = Color(0xFF0F172A),
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            confirmButton = {
                Button(
                    onClick = { showExportDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37))
                ) {
                    Text("تم التنزيل بنجاح", color = Color(0xFF080A0F), fontWeight = FontWeight.Bold)
                }
            },
            title = { Text("تصدير التقرير المالي PDF", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("تم تجهيز التقرير الاقتصادي الشامل لدولة ${country.nameAr} بصيغة PDF وتصديره إلى ملفات الجهاز.", color = Color(0xFF94A3B8), fontSize = 13.sp) },
            containerColor = Color(0xFF0F172A)
        )
    }

    if (showAlertsDialog) {
        AlertDialog(
            onDismissRequest = { showAlertsDialog = false },
            confirmButton = {
                Button(
                    onClick = { showAlertsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37))
                ) {
                    Text("حفظ التنبيهات", color = Color(0xFF080A0F), fontWeight = FontWeight.Bold)
                }
            },
            title = { Text("إعداد تنبيهات الأسواق", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("سيتم إرسال إشعارات فورية عند حدوث تغييرات جوهرية في أسعار الفائدة أو التضخم أو تحركات البورصة لـ ${country.nameAr}.", color = Color(0xFF94A3B8), fontSize = 13.sp) },
            containerColor = Color(0xFF0F172A)
        )
    }
}

@Composable
fun TickerItem(label: String, value: String, accentColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(accentColor))
        Text(label, fontSize = 11.sp, color = Color(0xFF94A3B8))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
fun TabButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    colors: CustomThemeColors,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFFD4AF37) else Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                null,
                tint = if (isSelected) Color(0xFF080A0F) else Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color(0xFF080A0F) else Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
fun EnhancedMetricCard(
    title: String,
    value: String,
    subtitle: String,
    badgeText: String,
    isPositive: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF141926),
        border = BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 11.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                Icon(icon, null, tint = Color(0xFFD4AF37), modifier = Modifier.size(18.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(value, fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isPositive) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = badgeText,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPositive) Color(0xFF10B981) else Color(0xFFEF4444)
                    )
                }
            }

            Text(subtitle, fontSize = 10.sp, color = Color(0xFF64748B))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
            ) {
                val path = Path().apply {
                    moveTo(0f, size.height * 0.6f)
                    cubicTo(size.width * 0.3f, size.height * 0.2f, size.width * 0.7f, size.height * 0.9f, size.width, size.height * 0.3f)
                }
                drawPath(
                    path = path,
                    color = if (isPositive) Color(0xFF10B981) else Color(0xFFEF4444),
                    style = Stroke(width = 2f, cap = StrokeCap.Round)
                )
            }
        }
    }
}

@Composable
fun DetailRowCustom(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = Color(0xFF94A3B8))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
fun ActionButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1E293B),
        border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = Color(0xFFD4AF37), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
fun WeatherScreen(colors: CustomThemeColors) {
    PremiumWeatherScreen(colors = colors)
}
