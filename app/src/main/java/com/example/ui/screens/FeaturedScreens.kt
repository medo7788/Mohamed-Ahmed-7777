package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GeminiRepository
import com.example.data.LivePricesRepository
import com.example.model.CalcKey
import com.example.ui.theme.AppIcons
import com.example.ui.theme.CustomThemeColors
import com.example.ui.theme.DesignTokens
import com.example.ui.theme.Spacing
import com.example.ui.components.ToolScreenScaffold
import com.example.ui.components.FrostedGlassCard
import com.example.ui.components.FrostedGlassCardVariant
import com.example.ui.components.GoldPrimaryButton
import com.example.ui.components.GlassSecondaryButton
import com.example.ui.components.GlassChip
import com.example.ui.components.SectionHeader
import com.example.ui.components.PremiumInfoRow
import kotlinx.coroutines.launch
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.automirrored.outlined.Assignment

data class ChatMessage(
    val sender: String, // "user" or "ai"
    val text: String,
    val timestamp: String = "الآن"
)

@Composable
fun AIAssistantScreen(colors: CustomThemeColors) {
    val context = LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    
    val messages = remember {
        mutableStateListOf(
            ChatMessage("ai", "أهلاً بك في المساعد المالي والإسلامي الذكي! يمكنني حساب الزكاة والوصايا والمواريث، وتحليل صفقات شراء الذهب والمعادن بدقة متناهية. كيف أساعدك اليوم؟")
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
            listState.animateScrollToItem(0)
            val history = messages.drop(1).chunked(2).mapNotNull {
                if (it.size == 2) Pair(it[0].text, it[1].text) else null
            }
            val response = GeminiRepository.queryAi(context, userMsg, history)
            messages.add(ChatMessage("ai", response))
            isLoading = false
            listState.animateScrollToItem(0)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.appBg)
    ) {
        Scaffold(
            topBar = {
                Surface(
                    color = colors.surface.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, colors.border.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(colors.accent.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(AppIcons.forCalc(CalcKey.AI), null, tint = colors.accent, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("المساعد الذكي AI", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.text)
                                Text("مستشارك المالي والشرعي الفوري", fontSize = 11.sp, color = colors.textMuted)
                            }
                        }

                        IconButton(onClick = { showSettingsDialog = true }) {
                            Icon(Icons.Filled.Settings, contentDescription = "إعدادات", tint = colors.accent)
                        }
                    }
                }
            },
            bottomBar = {
                Surface(
                    color = colors.surface.copy(alpha = 0.9f),
                    border = BorderStroke(1.dp, colors.border.copy(alpha = 0.15f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("اسأل المستشار الذكي عن أي شيء...", fontSize = 13.sp, color = colors.textMuted) },
                            modifier = Modifier.weight(1f),
                            maxLines = 4,
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = colors.surface2,
                                unfocusedContainerColor = colors.surface2,
                                focusedBorderColor = colors.accent,
                                unfocusedBorderColor = colors.border.copy(alpha = 0.5f),
                                focusedTextColor = colors.text,
                                unfocusedTextColor = colors.text
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
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "إرسال",
                                tint = if (inputText.isNotBlank() && !isLoading) colors.appBg else colors.textMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            val reversedMessages = remember(messages.size) { messages.toList().reversed() }

            LazyColumn(
                state = listState,
                reverseLayout = true,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isLoading) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = colors.accent,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جاري البحث والاستنباط وصياغة الرد الحصري لعام 2026...", fontSize = 11.sp, color = colors.textMuted)
                        }
                    }
                }

                items(reversedMessages.size) { index ->
                    val msg = reversedMessages[index]
                    val isAi = msg.sender == "ai"

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isAi) Arrangement.Start else Arrangement.End,
                        verticalAlignment = Alignment.Top
                    ) {
                        if (isAi) {
                            Box(
                                modifier = Modifier
                                    .padding(end = 8.dp, top = 4.dp)
                                    .size(28.dp)
                                    .background(colors.accent.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.AutoAwesome, null, tint = colors.accent, modifier = Modifier.size(14.dp))
                            }
                        }

                        Column(horizontalAlignment = if (isAi) Alignment.Start else Alignment.End) {
                            Surface(
                                color = if (isAi) colors.surface else colors.accent,
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isAi) 4.dp else 16.dp,
                                    bottomEnd = if (isAi) 16.dp else 4.dp
                                ),
                                border = BorderStroke(1.dp, colors.border.copy(alpha = 0.2f)),
                                modifier = Modifier.widthIn(max = 280.dp)
                            ) {
                                SelectionContainer {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = msg.text,
                                            fontSize = 13.sp,
                                            color = if (isAi) colors.text else colors.appBg,
                                            lineHeight = 20.sp
                                        )

                                        if (isAi) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                IconButton(
                                                    onClick = {
                                                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(msg.text))
                                                        android.widget.Toast.makeText(context, "تم نسخ الرد بالكامل", android.widget.Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Outlined.Assignment,
                                                        contentDescription = "نسخ",
                                                        tint = colors.accent,
                                                        modifier = Modifier.size(14.dp)
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
                                        testStatus = if (success) "✅ نجاح الاتصال: $msg" else "❌ فشل الاتصال: $msg"
                                        isTesting = false
                                        if (success) {
                                            GeminiRepository.saveApiKey(context, keyInput)
                                            GeminiRepository.saveSelectedModel(context, selectedModelId)
                                        }
                                    }
                                }
                            },
                            enabled = !isTesting && keyInput.isNotBlank(),
                            border = BorderStroke(1.dp, colors.accent)
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
    val context = LocalContext.current
    
    var selectedCurrency by remember { mutableStateOf<com.example.data.CurrencyRate>(LivePricesRepository.getSelectedCurrency(context)) }
    var showCurrencyPicker by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var selectedKarat by remember { mutableStateOf(24) }
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

    // Silver Prices
    val silverGramUsd = LivePricesRepository.silverGramUsd
    val silverGramCurr = silverGramUsd * currRate

    ToolScreenScaffold(
        colors = colors,
        icon = AppIcons.forCalc(CalcKey.LIVE_PRICES),
        title = "الأسعار الحية الفورية",
        subtitle = "متابعة أسعار صرف العملات والذهب والفضة والمعادن الثمينة والنفط العالمية"
    ) {
        // 1. Live Price Status Header
        FrostedGlassCard(
            colors = colors,
            variant = FrostedGlassCardVariant.Compact,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(colors.positive)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("مباشر ومحدّث", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = colors.positive)
                }

                Text(
                    if (LivePricesRepository.isLiveDataLoaded) LivePricesRepository.lastUpdatedText else "آخر تحديث: جاري التحميل...",
                    fontSize = 11.sp,
                    color = colors.textMuted
                )

                IconButton(
                    onClick = { refreshData() },
                    enabled = !isRefreshing,
                    modifier = Modifier.size(36.dp)
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = colors.accent)
                    } else {
                        Icon(Icons.Filled.Refresh, contentDescription = "تحديث", tint = colors.accent)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Preferred Currency Picker Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surface.copy(alpha = 0.75f))
                    .border(1.dp, colors.border.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .clickable { showCurrencyPicker = true }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(selectedCurrency.flag, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(selectedCurrency.nameAr, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.text)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Filled.ArrowDropDown, null, tint = colors.textMuted, modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Gold & Silver Realtime Market Summary
        SectionHeader(colors = colors, title = "أسعار الذهب والمعادن الثمينة اليوم")
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Gold Gram Card
            FrostedGlassCard(
                colors = colors,
                variant = FrostedGlassCardVariant.Standard,
                modifier = Modifier.weight(1f)
            ) {
                Text("الذهب (عيار $selectedKarat)", fontSize = 11.sp, color = colors.textMuted)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$currCode ${LivePricesRepository.formatNumber(goldGramCurr)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.accent
                )
                Text("لكل جرام", fontSize = 9.sp, color = colors.textMuted)
            }

            // Silver Gram Card
            FrostedGlassCard(
                colors = colors,
                variant = FrostedGlassCardVariant.Standard,
                modifier = Modifier.weight(1f)
            ) {
                Text("الفضة الصافية", fontSize = 11.sp, color = colors.textMuted)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$currCode ${LivePricesRepository.formatNumber(silverGramCurr)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.text
                )
                Text("لكل جرام", fontSize = 9.sp, color = colors.textMuted)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Karat Preferred Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("تخصيص العيار:", fontSize = 11.sp, color = colors.textMuted)
            listOf(24, 22, 21, 18).forEach { k ->
                GlassChip(
                    colors = colors,
                    label = "عيار $k",
                    selected = selectedKarat == k,
                    onClick = { selectedKarat = k }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 4. Live Currencies Grid with Search filter
        var currencySearchQuery by remember { mutableStateOf("") }
        SectionHeader(colors = colors, title = "صرف العملات الأجنبية مقابل $currCode")
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = currencySearchQuery,
            onValueChange = { currencySearchQuery = it },
            placeholder = { Text("ابحث عن عملة أو دولة معينة...", fontSize = 12.sp, color = colors.textMuted) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.accent,
                unfocusedBorderColor = colors.border.copy(alpha = 0.3f),
                focusedTextColor = colors.text,
                unfocusedTextColor = colors.text
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        val filteredCurrencies = remember(currencySearchQuery, currCode) {
            LivePricesRepository.currencies.filter { c ->
                c.code != currCode && (
                    currencySearchQuery.isBlank() ||
                    c.nameAr.contains(currencySearchQuery, ignoreCase = true) ||
                    c.code.contains(currencySearchQuery, ignoreCase = true) ||
                    c.countryAr.contains(currencySearchQuery, ignoreCase = true)
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            filteredCurrencies.forEach { c ->
                val convertedRate = LivePricesRepository.convertCurrency(1.0, c.code, currCode)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.surface.copy(alpha = 0.5f))
                        .border(1.dp, colors.border.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(c.flag, fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(c.nameAr, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = colors.text)
                                Text("${c.countryAr} (${c.code})", fontSize = 10.sp, color = colors.textMuted)
                            }
                        }
                        Text(
                            text = "= $currCode ${LivePricesRepository.formatNumber(convertedRate, 2)}",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = colors.accent
                        )
                    }
                }
            }
        }
    }

    // Currency Picker Dialog
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
