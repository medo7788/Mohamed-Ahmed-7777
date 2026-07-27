package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GeminiRepository
import com.example.data.LivePricesRepository
import com.example.ui.theme.CustomThemeColors
import kotlinx.coroutines.launch

data class ChatMessage(
    val sender: String, // "user" or "ai"
    val text: String,
    val timestamp: String = "الآن"
)

@Composable
fun AIAssistantScreen(colors: CustomThemeColors) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val messages = remember {
        mutableStateListOf(
            ChatMessage("ai", "أهلاً بك! أنا المساعد الذكي في ClevCalc Pro. يمكنني مساعدتك برأيك المالي، حسابات الذهب والزكاة، وفهم المعادلة الرياضية الصعبة! ✨")
        )
    }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val quickPrompts = listOf(
        "احسب زكاة مال 100,000 جنيه مصري",
        "كيف أحسب فائدة القرض المركبة؟",
        "ما هو عيار الذهب الأفضل للاستثمار؟",
        "احسب لقمة تكلفة سفر لسيارة 500 كم"
    )

    fun sendMessage(msgText: String) {
        if (msgText.isBlank() || isLoading) return
        val userMsg = msgText.trim()
        inputText = ""
        messages.add(ChatMessage("user", userMsg))
        isLoading = true

        coroutineScope.launch {
            listState.animateScrollToItem(messages.size - 1)
            val history = messages.drop(1).chunked(2).mapNotNull {
                if (it.size == 2) Pair(it[0].text, it[1].text) else null
            }
            val response = GeminiRepository.queryAi(context, userMsg, history)
            messages.add(ChatMessage("ai", response))
            isLoading = false
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.appBg)
            .padding(12.dp)
    ) {
        // AI Banner Card with Settings Button
        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF6366F1)))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✨", fontSize = 22.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("المساعد الذكي (Gemini AI)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.text)
                        Text("إجابات دقيقة للعمليات الحسابية والمالية والإسلامية", fontSize = 11.sp, color = colors.textMuted)
                    }
                }

                IconButton(onClick = { showSettingsDialog = true }) {
                    Icon(Icons.Default.Settings, contentDescription = "إعدادات AI", tint = colors.accent)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Suggestion Chips + Chat Messages
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            item {
                Text(
                    text = "أسئلة مقترحة:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMuted,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickPrompts.take(2).forEach { prompt ->
                        Surface(
                            color = colors.surface2,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { sendMessage(prompt) }
                        ) {
                            Text(
                                text = prompt,
                                fontSize = 11.sp,
                                color = colors.text,
                                modifier = Modifier.padding(8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            items(messages) { msg ->
                val isAi = msg.sender == "ai"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = if (isAi) Arrangement.Start else Arrangement.End
                ) {
                    Surface(
                        color = if (isAi) colors.surface else colors.accent,
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isAi) 2.dp else 16.dp,
                            bottomEnd = if (isAi) 16.dp else 2.dp
                        ),
                        modifier = Modifier.widthIn(max = 280.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = msg.text,
                                fontSize = 13.sp,
                                color = if (isAi) colors.text else Color.White
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Surface(
                            color = colors.surface,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = colors.accent,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("جاري التفكير وصياغة الإجابة...", fontSize = 12.sp, color = colors.textMuted)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Input Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("اسأل الذكاء الاصطناعي أي سؤال...", fontSize = 13.sp) },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp)),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface,
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.border,
                    focusedTextColor = colors.text,
                    unfocusedTextColor = colors.text
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = { sendMessage(inputText) },
                enabled = inputText.isNotBlank() && !isLoading,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (inputText.isNotBlank() && !isLoading) colors.accent else colors.surface2)
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "إرسال",
                    tint = if (inputText.isNotBlank() && !isLoading) Color.White else colors.textMuted
                )
            }
        }
    }

    // AI Key Settings Dialog
    if (showSettingsDialog) {
        var keyInput by remember { mutableStateOf(GeminiRepository.getStoredApiKey(context)) }
        var testStatus by remember { mutableStateOf<String?>(null) }
        var isTesting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        GeminiRepository.saveApiKey(context, keyInput)
                        showSettingsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                ) {
                    Text("حفظ المفتاح")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("إلغاء", color = colors.textMuted)
                }
            },
            title = {
                Text("⚙️ إعدادات Gemini API Key", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("احصل على مفتاح مجاني تماماً بدون بطاقة ائتمان من Google AI Studio:", fontSize = 12.sp, color = colors.textMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("aistudio.google.com/app/apikey", fontSize = 11.sp, color = colors.accent, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it; testStatus = null },
                        label = { Text("Gemini API Key") },
                        placeholder = { Text("AIzaSy...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = {
                                isTesting = true
                                coroutineScope.launch {
                                    val (success, msg) = GeminiRepository.testApiKey(keyInput)
                                    testStatus = msg
                                    isTesting = false
                                }
                            },
                            enabled = keyInput.isNotBlank() && !isTesting,
                            colors = ButtonDefaults.buttonColors(containerColor = colors.surface2)
                        ) {
                            if (isTesting) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            } else {
                                Text("🧪 اختبار المفتاح", fontSize = 11.sp, color = colors.text)
                            }
                        }

                        if (keyInput.isNotBlank()) {
                            TextButton(
                                onClick = {
                                    keyInput = ""
                                    GeminiRepository.clearApiKey(context)
                                    testStatus = "تم مسح المفتاح والعودة للافتراضي"
                                }
                            ) {
                                Text("مسح", fontSize = 11.sp, color = Color.Red)
                            }
                        }
                    }

                    if (testStatus != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = testStatus!!,
                            fontSize = 11.sp,
                            color = if (testStatus!!.startsWith("✅")) Color(0xFF10B981) else Color.Red
                        )
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
fun LivePricesScreen(colors: CustomThemeColors) {
    val context = LocalContext.current
    var selectedCurrency by remember { mutableStateOf(LivePricesRepository.getSelectedCurrency(context)) }
    var showCurrencyPicker by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var selectedKarat by remember { mutableIntStateOf(24) }
    val coroutineScope = rememberCoroutineScope()

    fun refreshData() {
        isRefreshing = true
        coroutineScope.launch {
            LivePricesRepository.refreshLivePrices()
            isRefreshing = false
        }
    }

    LaunchedEffect(Unit) {
        if (!LivePricesRepository.isLiveDataLoaded) {
            refreshData()
        }
    }

    // Calculations based on selected currency
    val currRate = selectedCurrency.rateVsUsd
    val currCode = selectedCurrency.code

    // Gold Prices
    val goldGramUsd = LivePricesRepository.getGoldPricePerGramInUsd(selectedKarat)
    val goldGramCurr = goldGramUsd * currRate
    val goldOunceCurr = LivePricesRepository.goldOunceUsd * currRate

    // Silver Prices
    val silverGramUsd = LivePricesRepository.silverGramUsd
    val silverGramCurr = silverGramUsd * currRate
    val silverOunceCurr = silverGramUsd * LivePricesRepository.GRAMS_PER_OUNCE * currRate

    // Platinum Prices
    val platinumGramUsd = LivePricesRepository.platinumGramUsd
    val platinumGramCurr = platinumGramUsd * currRate
    val platinumOunceCurr = platinumGramUsd * LivePricesRepository.GRAMS_PER_OUNCE * currRate

    // Palladium Prices
    val palladiumGramUsd = LivePricesRepository.palladiumGramUsd
    val palladiumGramCurr = palladiumGramUsd * currRate
    val palladiumOunceCurr = palladiumGramUsd * LivePricesRepository.GRAMS_PER_OUNCE * currRate

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.appBg)
            .padding(12.dp)
    ) {
        // 1. Live Price Status Header
        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("مباشر", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF10B981))
                }

                Text(
                    if (LivePricesRepository.isLiveDataLoaded) LivePricesRepository.lastUpdatedText else "آخر تحديث: جاري التحميل...",
                    fontSize = 11.sp,
                    color = colors.textMuted
                )

                Surface(
                    color = Color(0xFF2563EB),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.clickable(enabled = !isRefreshing) { refreshData() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تحديث", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Country / Currency Selector Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Surface(
                color = colors.surface,
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                modifier = Modifier.clickable { showCurrencyPicker = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(selectedCurrency.flag, fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(selectedCurrency.nameAr, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.text)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("▾", fontSize = 12.sp, color = colors.textMuted)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Section 1: Precious Metals Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("المعادن الثمينة 💰", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.text)
                    }

                    Surface(
                        color = Color(0xFF10B981),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "LIVE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // Karat Selector for Gold
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("عيار الذهب:", fontSize = 11.sp, color = colors.textMuted)
                    listOf(24, 22, 21, 18).forEach { k ->
                        Surface(
                            color = if (selectedKarat == k) colors.accent else colors.surface,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.clickable { selectedKarat = k }
                        ) {
                            Text(
                                text = "عيار $k",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedKarat == k) Color.White else colors.text,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // 2x2 Grid Row 1 (Gold & Silver)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Gold Card
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(130.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFFEAB308), Color(0xFFCA8A04))
                                    )
                                )
                                .padding(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column {
                                        Text("الذهب", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("XAU", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                                    }
                                    Text("🥇", fontSize = 20.sp)
                                }

                                Column {
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            "$currCode ${LivePricesRepository.formatNumber(goldGramCurr)}",
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    Text("/ جرام (عيار $selectedKarat)", fontSize = 10.sp, color = Color.White.copy(alpha = 0.9f))
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "$currCode ${LivePricesRepository.formatNumber(goldOunceCurr)} / أونصة",
                                        fontSize = 10.sp,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }

                    // Silver Card
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(130.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF94A3B8), Color(0xFF64748B))
                                    )
                                )
                                .padding(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column {
                                        Text("الفضة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("XAG", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                                    }
                                    Text("🥈", fontSize = 20.sp)
                                }

                                Column {
                                    Text(
                                        "$currCode ${LivePricesRepository.formatNumber(silverGramCurr)}",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text("/ جرام", fontSize = 10.sp, color = Color.White.copy(alpha = 0.9f))
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "$currCode ${LivePricesRepository.formatNumber(silverOunceCurr)} / أونصة",
                                        fontSize = 10.sp,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2x2 Grid Row 2 (Platinum & Palladium)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Platinum Card
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(130.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF475569), Color(0xFF334155))
                                    )
                                )
                                .padding(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column {
                                        Text("البلاتين", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("XPT", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                                    }
                                    Text("💎", fontSize = 20.sp)
                                }

                                Column {
                                    Text(
                                        "$currCode ${LivePricesRepository.formatNumber(platinumGramCurr)}",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text("/ جرام", fontSize = 10.sp, color = Color.White.copy(alpha = 0.9f))
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "$currCode ${LivePricesRepository.formatNumber(platinumOunceCurr)} / أونصة",
                                        fontSize = 10.sp,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }

                    // Palladium Card
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(130.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFFF87171), Color(0xFFEF4444))
                                    )
                                )
                                .padding(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column {
                                        Text("البلاديوم", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("XPD", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                                    }
                                    Text("⚪", fontSize = 20.sp)
                                }

                                Column {
                                    Text(
                                        "$currCode ${LivePricesRepository.formatNumber(palladiumGramCurr)}",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text("/ جرام", fontSize = 10.sp, color = Color.White.copy(alpha = 0.9f))
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "$currCode ${LivePricesRepository.formatNumber(palladiumOunceCurr)} / أونصة",
                                        fontSize = 10.sp,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: Commodities (النفط والسلع)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("النفط والسلع 🛢️", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.text)

                    Surface(
                        color = Color(0xFFF59E0B),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "تقديري ⚠️",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            items(LivePricesRepository.commodityPrices) { commodity ->
                val commPriceCurr = commodity.priceUsd * currRate
                Surface(
                    color = colors.surface,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(commodity.icon, fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(commodity.nameAr, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.text)
                                Text(commodity.symbol, fontSize = 11.sp, color = colors.textMuted)
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "≈ $currCode ${LivePricesRepository.formatNumber(commPriceCurr, 1)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = colors.text
                            )
                            Text("/ ${commodity.unit} (تقديري)", fontSize = 11.sp, color = colors.textMuted)
                        }
                    }
                }
            }

            // Section 3: Currencies vs Selected Currency
            item {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    "أسعار العملات (مقابل ${selectedCurrency.nameAr}) 💱",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.text,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(LivePricesRepository.currencies.filter { it.code != currCode }) { c ->
                val convertedRate = LivePricesRepository.convertCurrency(1.0, c.code, currCode)
                Surface(
                    color = colors.surface,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(c.flag, fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(c.nameAr, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = colors.text)
                                Text("1 ${c.code}", fontSize = 11.sp, color = colors.textMuted)
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "= $currCode ${LivePricesRepository.formatNumber(convertedRate, if (convertedRate < 1) 4 else 2)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = colors.accent
                            )
                        }
                    }
                }
            }
        }
    }

    // Currency Selection Dialog
    if (showCurrencyPicker) {
        var searchQuery by remember { mutableStateOf("") }
        val filteredList = LivePricesRepository.currencies.filter { c ->
            searchQuery.isBlank() ||
            c.nameAr.contains(searchQuery, ignoreCase = true) ||
            c.code.contains(searchQuery, ignoreCase = true) ||
            c.countryAr.contains(searchQuery, ignoreCase = true)
        }

        AlertDialog(
            onDismissRequest = { showCurrencyPicker = false },
            confirmButton = {
                TextButton(onClick = { showCurrencyPicker = false }) {
                    Text("إغلاق", color = colors.accent, fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Text("🌐 اختر دولة العملة", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("ابحث عن الدولة أو العملة...", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    ) {
                        items(filteredList) { item ->
                            val isSelected = item.code == selectedCurrency.code
                            Surface(
                                color = if (isSelected) colors.accent.copy(alpha = 0.15f) else colors.surface,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clickable {
                                        selectedCurrency = item
                                        LivePricesRepository.setSelectedCurrency(context, item.code)
                                        showCurrencyPicker = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(item.flag, fontSize = 20.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(item.nameAr, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.text)
                                            Text("${item.countryAr} (${item.code})", fontSize = 11.sp, color = colors.textMuted)
                                        }
                                    }

                                    if (isSelected) {
                                        Text("✓", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.accent)
                                    }
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
fun ThemeSelector(current: com.example.ui.theme.AppThemeKey, onChange: (com.example.ui.theme.AppThemeKey) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("اختر المظهر المفضل لديك ✨", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 12.dp))

        com.example.ui.theme.AppThemeKey.values().forEach { themeKey ->
            val isSelected = current == themeKey
            Surface(
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable { onChange(themeKey) }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(themeKey.icon, fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(themeKey.titleAr, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    if (isSelected) {
                        Text("مُفعّل ✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
