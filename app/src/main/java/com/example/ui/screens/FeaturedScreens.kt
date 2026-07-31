package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.ui.theme.AppIcons
import com.example.ui.theme.Spacing
import com.example.model.CalcKey
import kotlinx.coroutines.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.alpha

data class ChatMessage(
    val sender: String, // "user" or "ai"
    val text: String,
    val timestamp: String = "الآن"
)

@Composable
fun AIAssistantScreen(colors: CustomThemeColors) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    
    val messages = remember {
        mutableStateListOf(
            ChatMessage("ai", "أهلاً بك! أنا المساعد الذكي الخاص بك المدعوم بـ Gemini. يمكنني مساعدتك في الحسابات المعقدة، فتاوى الزكاة، تحليل أسعار السوق، وأكثر من ذلك. كيف يمكنني مساعدتك اليوم؟")
        )
    }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

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
    ) {
        // Professional Header
        Surface(
            color = colors.surface,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF6366F1)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(AppIcons.AI, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("ClevCalc AI", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.text)
                        Text("powered by Gemini", fontSize = 11.sp, color = colors.accent)
                    }
                }
                IconButton(onClick = { showSettingsDialog = true }) {
                    Icon(Icons.Filled.Settings, contentDescription = "الإعدادات", tint = colors.textMuted)
                }
            }
        }

        // Chat Area
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(messages.size) { index ->
                val msg = messages[index]
                val isAi = msg.sender == "ai"
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = if (isAi) Arrangement.Start else Arrangement.End,
                    verticalAlignment = Alignment.Bottom
                ) {
                    if (isAi) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF6366F1)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(AppIcons.AI, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    Surface(
                        color = if (isAi) colors.surface2 else colors.accent,
                        shape = RoundedCornerShape(
                            topStart = 20.dp,
                            topEnd = 20.dp,
                            bottomStart = if (isAi) 4.dp else 20.dp,
                            bottomEnd = if (isAi) 20.dp else 4.dp
                        ),
                        modifier = Modifier
                            .widthIn(max = 280.dp)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = {
                                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(msg.text))
                                        android.widget.Toast.makeText(context, "تم نسخ الرسالة", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Text(
                                text = msg.text,
                                fontSize = 15.sp,
                                lineHeight = 24.sp,
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
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF6366F1)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(AppIcons.AI, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = colors.surface2,
                            shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = colors.textMuted,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("جاري معالجة الرد...", fontSize = 13.sp, color = colors.textMuted)
                            }
                        }
                    }
                }
            }
        }

        // Modern Input Area
        Surface(
            color = colors.surface,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("اكتب رسالتك هنا...", color = colors.textMuted) },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(colors.appBg),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = colors.text,
                        unfocusedTextColor = colors.text
                    ),
                    maxLines = 4,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences
                    )
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (inputText.isNotBlank() && !isLoading) colors.accent else colors.surface2)
                        .clickable(enabled = inputText.isNotBlank() && !isLoading) { sendMessage(inputText) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = colors.textMuted, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.Send, contentDescription = "إرسال", tint = if (inputText.isNotBlank()) Color.White else colors.textMuted, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }

    // AI Key Settings Dialog
    if (showSettingsDialog) {
        var keyInput by remember { mutableStateOf(GeminiRepository.getStoredApiKey(context)) }
        var testStatus by remember { mutableStateOf<String?>(null) }
        var isTesting by remember { mutableStateOf(false) }
        
        var selectedModelId by remember { mutableStateOf(GeminiRepository.getSelectedModel(context)) }
        var showModelDropdown by remember { mutableStateOf(false) }
        val models = GeminiRepository.AVAILABLE_MODELS

        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Settings, contentDescription = null, tint = colors.accent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إعدادات الذكاء الاصطناعي", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column {
                    Text(
                        "للحصول على أفضل أداء، يرجى إدخال مفتاح Gemini API الخاص بك. يمكنك الحصول عليه مجاناً من Google AI Studio.",
                        fontSize = 12.sp,
                        color = colors.textMuted,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        label = { Text("Gemini API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.accent,
                            focusedLabelColor = colors.accent
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("النموذج المفضل:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.text)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Model Dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            color = colors.surface2,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showModelDropdown = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val selectedName = models.find { it.id == selectedModelId }?.displayName ?: selectedModelId
                                Text(selectedName, fontSize = 14.sp, color = colors.text)
                                Text("▼", fontSize = 10.sp, color = colors.textMuted)
                            }
                        }
                        DropdownMenu(
                            expanded = showModelDropdown,
                            onDismissRequest = { showModelDropdown = false },
                            modifier = Modifier.background(colors.surface)
                        ) {
                            models.forEach { model ->
                                DropdownMenuItem(
                                    text = { Text(model.displayName, color = colors.text) },
                                    onClick = {
                                        selectedModelId = model.id
                                        showModelDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    if (testStatus != null) {
                        Text(
                            text = testStatus ?: "",
                            color = if (testStatus!!.contains("نجاح")) Color(0xFF10B981) else Color(0xFFEF4444),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = {
                                keyInput = ""
                                GeminiRepository.clearApiKey(context)
                                testStatus = "تم مسح المفتاح والعودة للافتراضي"
                            }
                        ) {
                            Text("مسح المفتاح", color = Color(0xFFEF4444))
                        }
                        
                        OutlinedButton(
                            onClick = {
                                if (keyInput.isNotBlank()) {
                                    isTesting = true
                                    testStatus = "جاري الاختبار..."
                                    coroutineScope.launch {
                                        val (success, msg) = GeminiRepository.testApiKey(keyInput)
                                        testStatus = if (success) "✅ نجاح الاتصال: \$msg" else "❌ فشل الاتصال: \$msg"
                                        isTesting = false
                                        if (success) {
                                            GeminiRepository.saveApiKey(context, keyInput)
                                            GeminiRepository.saveSelectedModel(context, selectedModelId)
                                        }
                                    }
                                }
                            },
                            enabled = !isTesting && keyInput.isNotBlank(),
                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.accent)
                        ) {
                            if (isTesting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Text("اختبار الاتصال", color = colors.accent)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        GeminiRepository.saveApiKey(context, keyInput)
                        GeminiRepository.saveSelectedModel(context, selectedModelId)
                        showSettingsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                ) {
                    Text("حفظ وإغلاق")
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
    val context = androidx.compose.ui.platform.LocalContext.current
    
    var selectedCurrency by remember { mutableStateOf(LivePricesRepository.getSelectedCurrency(context)) }
    var showCurrencyPicker by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var selectedKarat by remember { mutableIntStateOf(24) }
    val coroutineScope = rememberCoroutineScope()
    
    // Category state selector
    var activeCategory by remember { mutableStateOf("العملات") }

    fun refreshData() {
        isRefreshing = true
        coroutineScope.launch {
            LivePricesRepository.refreshLivePrices(context)
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
            color = colors.surface.copy(alpha = 0.75f),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.2f)),
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
                    color = colors.accent,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.clickable(enabled = !isRefreshing) { refreshData() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = colors.appBg)
                        } else {
                            Icon(AppIcons.Refresh, contentDescription = null, tint = colors.appBg, modifier = Modifier.size(14.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تحديث", fontSize = 12.sp, color = colors.appBg, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Premium Hero Market Summary (Obsidian Glass #1E262C, 75% Opacity, 1dp Royal Gold border)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp)),
            color = Color(0xFF1E262C).copy(alpha = 0.75f),
            border = BorderStroke(1.dp, colors.accent),
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "ملخص الأسعار والأسواق",
                    fontSize = 12.sp,
                    color = colors.accent,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("الذهب عيار 24", fontSize = 11.sp, color = colors.textMuted)
                        Text("$currCode ${LivePricesRepository.formatNumber(goldGramCurr)}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("سعر الفضة الصافي", fontSize = 11.sp, color = colors.textMuted)
                        Text("$currCode ${LivePricesRepository.formatNumber(silverGramCurr)}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 5. Category Selector Capsule Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        ) {
            val categories = listOf("العملات", "الذهب", "الفضة", "البرمجة")
            items(categories) { cat ->
                val isActive = activeCategory == cat
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { activeCategory = cat },
                    color = if (isActive) colors.accent else colors.surface.copy(alpha = 0.75f),
                    border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = cat,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) colors.appBg else colors.text,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 90.dp) // Scroll safety padding
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
                        Icon(AppIcons.forCalc(CalcKey.CURRENCY), null, tint = colors.accent, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("المعادن الثمينة والأسعار", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.text)
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
                    Text("عيار الذهب المفضل:", fontSize = 11.sp, color = colors.textMuted)
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
                                    Icon(AppIcons.Gold, null, tint = Color.White, modifier = Modifier.size(24.dp))
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
                                    Icon(Icons.Filled.Circle, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(24.dp))
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
                                    Icon(Icons.Filled.Diamond, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(24.dp))
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
                                    Icon(Icons.Filled.Circle, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(24.dp))
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
                    color = colors.surface.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.15f)),
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
                            Icon(AppIcons.forCommodity(commodity.icon), null, tint = colors.accent, modifier = Modifier.size(24.dp))
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CurrencyExchange, null, tint = colors.accent, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "أسعار العملات (مقابل ${selectedCurrency.nameAr})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.text
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(LivePricesRepository.currencies.filter { it.code != currCode }) { c ->
                val convertedRate = LivePricesRepository.convertCurrency(1.0, c.code, currCode)
                Surface(
                    color = colors.surface.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.15f)),
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
                            Icon(Icons.Filled.Public, null, tint = colors.textMuted, modifier = Modifier.size(20.dp))
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Public, null, tint = colors.accent, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("اختر دولة العملة", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
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
