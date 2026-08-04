package com.example.ui.screens
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.location.Geocoder
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.CountryEconomicData
import com.example.data.EconomicRepository
import com.example.data.WeatherCity
import com.example.data.WeatherRepository
import com.example.data.CurrentWeatherData
import com.example.ui.theme.CustomThemeColors
import androidx.compose.foundation.BorderStroke

@Composable
fun EconomicIndicatorsScreen(colors: CustomThemeColors) {
    val context = LocalContext.current
    var selectedCountryCode by remember { mutableStateOf("EG") }
    val country = remember(selectedCountryCode) { EconomicRepository.getCountryByCode(selectedCountryCode) }

    var showCountryPicker by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Overview & Metrics, 1: AI Economic Advisor

    // AI Advisor Chat state
    var aiReportText by remember { mutableStateOf<String?>(null) }
    var isGeneratingReport by remember { mutableStateOf(false) }
    var userPromptText by remember { mutableStateOf("") }
    var isSendingPrompt by remember { mutableStateOf(false) }
    var chatMessages by remember { mutableStateOf(listOf<Pair<Boolean, String>>()) } // Pair(isUser, text)

    val coroutineScope = rememberCoroutineScope()

    fun generateFullReport() {
        isGeneratingReport = true
        coroutineScope.launch {
            val report = EconomicRepository.getAIEconomicReport(context, country)
            aiReportText = report
            isGeneratingReport = false
        }
    }

    LaunchedEffect(country.code) {
        aiReportText = null
        chatMessages = emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.appBg)
            .padding(12.dp)
    ) {
        // Header Banner (Purple Theme)
        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(AppIcons.EconomicOverview, null, tint = colors.accent, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("مؤشرات الاقتصاد والخبير الذكي", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.text)
                        Text("بيانات رسمية وتوقع حركة الأسواق بافتراضات خبير", fontSize = 11.sp, color = colors.textMuted)
                    }
                }

                Surface(
                    color = colors.accent,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.clickable { showCountryPicker = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(country.nameAr, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.ArrowDropDown, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = colors.surface,
            contentColor = colors.accent,
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(AppIcons.StockMarket, null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("المؤشرات والبورصة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(AppIcons.EconomicAdvisor, null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("المستشار الاقتصادي AI", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (selectedTab == 0) {
            // Screen 1: Economic Overview & Metrics
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Summary Card
                item {
                    Surface(
                        color = colors.surface,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("حالة اقتصاد ${country.nameAr}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.text)
                                Surface(
                                    color = colors.accent.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(country.currency, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.accent, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(country.economicSummaryAr, fontSize = 12.sp, color = colors.textMuted, lineHeight = 18.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = colors.surface2,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "💡 أرقام مؤشرات الاقتصاد المذكورة هي بيانات إحصائية مرجعية تحديث 2024/2025 صادرة عن التقارير الرسمية للبنوك المركزية.",
                                    fontSize = 10.sp,
                                    color = colors.textMuted,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }

                // 2x2 Grid Row 1: Inflation & Interest Rate
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricCard(
                            title = "معدل التضخم السنوي",
                            value = country.inflationRate,
                            subtitle = "مستوى أسعار المستهلكين",
                            icon = AppIcons.Inflation,
                            colors = colors,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "سعر الفائدة البنكي",
                            value = country.interestRate,
                            subtitle = "البنك المركزي",
                            icon = AppIcons.InterestRate,
                            colors = colors,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 2x2 Grid Row 2: GDP Growth & Unemployment
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricCard(
                            title = "نمو الناتج المحلي (GDP)",
                            value = country.gdpGrowth,
                            subtitle = "معدل النمو السنوي",
                            icon = AppIcons.Growth,
                            colors = colors,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "نسبة البطالة",
                            value = country.unemployment,
                            subtitle = "إجمالي قوة العمل",
                            icon = AppIcons.Unemployment,
                            colors = colors,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Stock Exchange Card
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                                    )
                                )
                                .padding(16.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(country.stockExchangeName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("أداء سوق المال المحلي", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                                    }
                                    Icon(AppIcons.StockMarket, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(24.dp))
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(country.stockExchangeValue, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Surface(
                                        color = if (country.stockExchangeChange.startsWith("-")) Color(0xFFEF4444) else Color(0xFF10B981),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            country.stockExchangeChange,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Reserve & Debt Details
                item {
                    Surface(
                        color = colors.surface,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(AppIcons.Summary, null, tint = colors.accent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("تفاصيل المؤشرات الكلية", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.text)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            DetailRow("الاحتياطي الأجنبي:", country.centralBankReserves, colors)
                            DetailRow("نسبة الدين للناتج:", country.debtToGdp, colors)
                            DetailRow("أهم الصادرات والقطاعات:", country.mainExport, colors)
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            selectedTab = 1
                            if (aiReportText == null) generateFullReport()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(AppIcons.EconomicAdvisor, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("اطلب تقرير واستشارة الخبير الاقتصادي الذكي", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        } else {
            // Screen 2: AI Economic Advisor Chat & Analysis
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Initial Generate Report Button Card
                Surface(
                    color = colors.surface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(AppIcons.EconomicAdvisor, null, tint = colors.accent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("المستشار الاقتصادي الذكي", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.text)
                            }
                            if (isGeneratingReport) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = colors.accent)
                            } else {
                                Button(
                                    onClick = { generateFullReport() },
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("توليد تقرير كامل", fontSize = 11.sp, color = Color.White)
                                }
                            }
                        }

                        if (aiReportText != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            // إصلاح: كان التقرير Text عادي جوه Column غير قابل للسكرول
                            // وبدون إمكانية نسخ - يعني تقرير طويل كان بيتقطع ومفيش
                            // طريقة تقرأ باقيه أو تنسخه.
                            val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 320.dp)
                                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
                            ) {
                                androidx.compose.foundation.text.selection.SelectionContainer {
                                    Text(aiReportText!!, fontSize = 12.sp, color = colors.text, lineHeight = 19.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            TextButton(
                                onClick = {
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(aiReportText!!))
                                },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(androidx.compose.material.icons.Icons.Default.ContentCopy, null, tint = colors.accent, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("نسخ التقرير كامل", fontSize = 11.sp, color = colors.accent)
                            }
                        } else if (!isGeneratingReport) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("اضغط على الزر أعلاه للحصول على تقرير شامل عن فرص الاستثمار والتضخم والأسواق في ${country.nameAr}، أو اسأل الخبير المالي أي سؤال مباشرة.", fontSize = 12.sp, color = colors.textMuted)
                        }
                    }
                }

                // Chat Messages List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(chatMessages) { (isUser, text) ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            contentAlignment = if (isUser) Alignment.CenterStart else Alignment.CenterEnd
                        ) {
                            Surface(
                                color = if (isUser) colors.accent else colors.surface,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.widthIn(max = 280.dp)
                            ) {
                                Text(
                                    text = text,
                                    fontSize = 12.sp,
                                    color = if (isUser) Color.White else colors.text,
                                    modifier = Modifier.padding(12.dp),
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                // Chat Input Bar
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = userPromptText,
                        onValueChange = { userPromptText = it },
                        placeholder = { Text("اسأل الخبير الاقتصادي...", fontSize = 12.sp, color = colors.textMuted) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.surface,
                            unfocusedContainerColor = colors.surface,
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.text,
                            unfocusedTextColor = colors.text
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (userPromptText.isNotBlank() && !isSendingPrompt) {
                                val query = userPromptText.trim()
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
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = colors.accent)
                        } else {
                            Icon(Icons.Default.Send, contentDescription = "إرسال", tint = colors.accent)
                        }
                    }
                }
            }
        }
    }

    // Country Picker Dialog
    if (showCountryPicker) {
        AlertDialog(
            onDismissRequest = { showCountryPicker = false },
            confirmButton = {
                TextButton(onClick = { showCountryPicker = false }) {
                    Text("إغلاق", color = colors.accent, fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(androidx.compose.material.icons.Icons.Default.Public, null, tint = colors.accent, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("اختر الدولة لمتابعة اقتصادها", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                ) {
                    items(EconomicRepository.countries) { item ->
                        val isSelected = item.code == selectedCountryCode
                        Surface(
                            color = if (isSelected) colors.accent.copy(alpha = 0.15f) else colors.surface,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clickable {
                                    selectedCountryCode = item.code
                                    showCountryPicker = false
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(androidx.compose.material.icons.Icons.Default.Public, null, tint = colors.textMuted, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(item.nameAr, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.text)
                                        Text("${item.currency} • بورصة ${item.stockExchangeName}", fontSize = 11.sp, color = colors.textMuted)
                                    }
                                }
                                if (isSelected) {
                                    Text("✓", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.accent)
                                }
                            }
                        }
                    }
                }
            },
            containerColor = colors.surface,
            titleContentColor = colors.text,
            textContentColor = colors.text
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    colors: CustomThemeColors,
    modifier: Modifier = Modifier
) {
    Surface(
        color = colors.surface,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 11.sp, color = colors.textMuted, fontWeight = FontWeight.Medium)
                Icon(icon, null, tint = colors.accent, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.accent)
            Text(subtitle, fontSize = 10.sp, color = colors.textMuted)
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, colors: CustomThemeColors) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = colors.textMuted)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.text)
    }
}

@Composable
fun WeatherScreen(colors: CustomThemeColors) {
    PremiumWeatherScreen(colors = colors)
}


@Composable
fun MetricItem(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, colors: CustomThemeColors) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = colors.accent, modifier = Modifier.size(24.dp))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.text)
        Text(title, fontSize = 11.sp, color = colors.textMuted)
    }
}
