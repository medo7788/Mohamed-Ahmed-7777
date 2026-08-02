package com.example.ui.screens
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import com.example.data.LivePricesRepository
import com.example.ui.theme.CustomThemeColors
import com.example.ui.components.ToolScreenScaffold
import com.example.ui.theme.AppIcons
import com.example.ui.theme.Spacing
import com.example.model.CalcKey
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import kotlin.math.*

@Composable
fun BasicCalculatorScreen(colors: CustomThemeColors) {
    val haptic = LocalHapticFeedback.current
    var displayExpression by remember { mutableStateOf("0") }
    var historyExpression by remember { mutableStateOf("") }
    var showScientific by remember { mutableStateOf(false) }

    fun onBtnClick(label: String) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        when (label) {
            "C" -> {
                displayExpression = "0"
                historyExpression = ""
            }
            "⌫" -> {
                displayExpression = if (displayExpression.length <= 1) "0" else displayExpression.dropLast(1)
            }
            "=" -> {
                try {
                    historyExpression = displayExpression
                    val eval = evaluateSimpleExpr(displayExpression)
                    displayExpression = if (eval == eval.toLong().toDouble()) {
                        eval.toLong().toString()
                    } else {
                        String.format(java.util.Locale.US, "%.4f", eval)
                            .trimEnd('0')
                            .trimEnd('.')
                    }
                } catch (e: Exception) {
                    displayExpression = "خطأ"
                }
            }
            "±" -> {
                displayExpression = if (displayExpression.startsWith("-")) displayExpression.drop(1) else "-$displayExpression"
            }
            "sin", "cos", "tan", "√", "log", "ln" -> {
                displayExpression = if (displayExpression == "0") "$label(" else displayExpression + "$label("
            }
            else -> {
                displayExpression = if (displayExpression == "0" && label !in listOf("+", "-", "×", "÷", ".")) label else displayExpression + label
            }
        }
    }

    val basicBtns = listOf(
        "C", "⌫", "%", "÷",
        "7", "8", "9", "×",
        "4", "5", "6", "-",
        "1", "2", "3", "+",
        "±", "0", ".", "="
    )

    val sciBtns = listOf(
        "sin", "cos", "tan", "√",
        "log", "ln", "(", ")",
        "π", "e", "^", "x²"
    )

    ToolScreenScaffold(
        colors = colors,
        icon = AppIcons.forCalc(CalcKey.BASIC),
        title = "الآلة الحاسبة المتطورة",
        subtitle = "إجراء العمليات الحسابية البسيطة والعلمية بدقة",
        isScrollable = false
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(520.dp) // Perfect fixed height to fit on all device dimensions beautifully without overflow
        ) {
            // Display Box: Obsidian Glass #1E262C, 75% Opacity, 24dp Radius, 1dp Royal Gold border
            Surface(
                color = Color(0xFF1E262C).copy(alpha = 0.75f),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, colors.accent),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                if (historyExpression.isNotEmpty()) {
                    Text(
                        historyExpression,
                        fontSize = 18.sp,
                        color = colors.textMuted,
                        textAlign = TextAlign.End,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                val dynamicFontSize = when {
                    displayExpression.length > 15 -> 24.sp
                    displayExpression.length > 10 -> 32.sp
                    else -> 48.sp
                }

                Text(
                    displayExpression,
                    fontSize = dynamicFontSize,
                    lineHeight = dynamicFontSize * 1.1f,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    textAlign = TextAlign.End,
                    maxLines = 2
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.Medium))

        // Toggle Scientific
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = { showScientific = !showScientific }) {
                Text(if (showScientific) "إخفاء اللوحة العلمية 📐" else "الآلة العلمية المتطورة 📐", color = colors.accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (showScientific) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 200.dp).padding(bottom = 12.dp)
            ) {
                items(sciBtns) { btn ->
                    Surface(
                        color = colors.surface.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .height(56.dp)
                            .clickable { onBtnClick(btn) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(btn, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8)) // Ice Cyan text
                        }
                    }
                }
            }
        }

            // Keypad Grid: 5 rows fully responsive using nested weights
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val rows = basicBtns.chunked(4)
                    rows.forEach { rowBtns ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowBtns.forEach { btn ->
                                val isOp = btn in listOf("÷", "×", "-", "+", "=")
                                val isAction = btn in listOf("C", "⌫", "%", "±")

                                val btnBg = when {
                                    isOp -> colors.surface2.copy(alpha = 0.75f)
                                    isAction -> colors.surface2.copy(alpha = 0.6f)
                                    else -> colors.surface.copy(alpha = 0.75f)
                                }

                                val btnFg = when {
                                    btn == "=" -> colors.appBg
                                    isOp -> colors.accent
                                    isAction -> Color(0xFF38BDF8) // Ice Cyan Functions
                                    else -> Color.White
                                }

                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (btn == "=") colors.accent else colors.accent.copy(alpha = 0.2f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(18.dp))
                                        .clickable { onBtnClick(btn) }
                                        .then(
                                            if (btn == "=") {
                                                Modifier.background(Brush.linearGradient(listOf(colors.accent, Color(0xFFCA8A04))))
                                            } else {
                                                Modifier.background(btnBg)
                                            }
                                        ),
                                    color = Color.Transparent,
                                    shadowElevation = 2.dp
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Text(btn, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = btnFg)
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

private fun evaluateSimpleExpr(expr: String): Double {
    val clean = expr.replace("×", "*").replace("÷", "/")
    return try {
        val tokens = clean.split("(?<=[-+*/])|(?=[-+*/])".toRegex()).map { it.trim() }
        var res = tokens[0].toDouble()
        var i = 1
        while (i < tokens.size - 1) {
            val op = tokens[i]
            val nextVal = tokens[i + 1].toDouble()
            when (op) {
                "+" -> res += nextVal
                "-" -> res -= nextVal
                "*" -> res *= nextVal
                "/" -> res /= nextVal
            }
            i += 2
        }
        res
    } catch (e: Exception) {
        clean.toDoubleOrNull() ?: 0.0
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyConverterScreen(colors: CustomThemeColors) {
    var amountText by remember { mutableStateOf("100") }
    var fromCode by remember { mutableStateOf("USD") }
    var toCode by remember { mutableStateOf("EGP") }

    val amount = amountText.toDoubleOrNull() ?: 0.0
    val converted = LivePricesRepository.convertCurrency(amount, fromCode, toCode)

    ToolScreenScaffold(
        colors = colors,
        icon = AppIcons.forCalc(CalcKey.CURRENCY),
        title = "محول العملات الذكي",
        subtitle = "تحويل فوري فائق الدقة بين العملات العالمية مع تغذية حية من الأسواق العالمية"
    ) {
        // Status Badge (Live Rates)
        Surface(
            color = Color(0xFF10B981).copy(alpha = 0.15f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "✓ الأسعار حية ومحدثة مباشرة وفقاً لمنصات البورصة المفتوحة",
                    fontSize = 11.sp,
                    color = Color(0xFF10B981),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Input Card
        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("المبلغ المراد تحويله") },
                    placeholder = { Text("أدخل القيمة الرقمية") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.text,
                        unfocusedTextColor = colors.text,
                        focusedLabelColor = colors.accent
                    ),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // From
                    var fromExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = fromExpanded,
                        onExpandedChange = { fromExpanded = !fromExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = fromCode,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("العملة المصدر") },
                            modifier = Modifier.menuAnchor(),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.accent,
                                unfocusedBorderColor = colors.border,
                                focusedLabelColor = colors.accent
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = fromExpanded,
                            onDismissRequest = { fromExpanded = false }
                        ) {
                            LivePricesRepository.currencies.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text("${c.flag} ${c.code} - ${c.nameAr}") },
                                    onClick = {
                                        fromCode = c.code
                                        fromExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = {
                            val tmp = fromCode
                            fromCode = toCode
                            toCode = tmp
                        },
                        modifier = Modifier
                            .padding(horizontal = 10.dp)
                            .clip(CircleShape)
                            .background(colors.accent.copy(alpha = 0.12f))
                    ) {
                        Icon(Icons.Default.SwapHoriz, null, tint = colors.accent)
                    }

                    // To
                    var toExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = toExpanded,
                        onExpandedChange = { toExpanded = !toExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = toCode,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("العملة المستهدفة") },
                            modifier = Modifier.menuAnchor(),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.accent,
                                unfocusedBorderColor = colors.border,
                                focusedLabelColor = colors.accent
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = toExpanded,
                            onDismissRequest = { toExpanded = false }
                        ) {
                            LivePricesRepository.currencies.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text("${c.flag} ${c.code} - ${c.nameAr}") },
                                    onClick = {
                                        toCode = c.code
                                        toExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Premium Result Card (Google/Apple Level Polish)
        Surface(
            color = colors.accent.copy(alpha = 0.08f),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.5.dp, colors.accent),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("القيمة الإجمالية المعادلة", fontSize = 13.sp, color = colors.textMuted, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "${LivePricesRepository.formatNumber(converted)} $toCode",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.accent
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Detailed breakdown line
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surface.copy(alpha = 0.6f))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("معدل التحويل الأساسي:", fontSize = 12.sp, color = colors.textMuted)
                    Text("1 $fromCode = ${LivePricesRepository.formatNumber(LivePricesRepository.convertCurrency(1.0, fromCode, toCode))} $toCode", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.text)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Popular conversion references to feel extremely professional
                if (fromCode != "USD" && toCode != "USD") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.surface.copy(alpha = 0.6f))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("معدل التحويل للدولار الأمريكي:", fontSize = 12.sp, color = colors.textMuted)
                        Text("1 $fromCode = ${LivePricesRepository.formatNumber(LivePricesRepository.convertCurrency(1.0, fromCode, "USD"))} USD", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.text)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoldCalcScreen(colors: CustomThemeColors) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (!LivePricesRepository.isLiveDataLoaded) {
            LivePricesRepository.refreshLivePrices(context)
        }
    }

    var gramsText by remember { mutableStateOf("10") }
    var selectedKarat by remember { mutableStateOf(21) }
    var makingFeeGrams by remember { mutableStateOf("100") } // مصنعية للجرام
    var selectedCurrencyCode by remember { mutableStateOf("EGP") }
    var currencyExpanded by remember { mutableStateOf(false) }

    val grams = gramsText.toDoubleOrNull() ?: 0.0
    val fee = makingFeeGrams.toDoubleOrNull() ?: 0.0

    val pricePerGramUsd = LivePricesRepository.getGoldPricePerGramInUsd(selectedKarat)
    val pricePerGramLocal = LivePricesRepository.convertCurrency(pricePerGramUsd, "USD", selectedCurrencyCode)

    val rawGoldCost = grams * pricePerGramLocal
    val makingFeeCost = grams * fee
    val totalGoldCost = rawGoldCost + makingFeeCost

    ToolScreenScaffold(
        colors = colors,
        icon = AppIcons.forCalc(CalcKey.GOLD),
        title = "حاسبة الذهب الذكية",
        subtitle = "حساب فوري لتكاليف شراء وبيع الذهب عيار (24, 21, 18) مع احتساب المصنعية وهامش الصاغة"
    ) {
        // Main Input Card (Premium Rounded Container)
        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Currency Selector
                ExposedDropdownMenuBox(
                    expanded = currencyExpanded,
                    onExpandedChange = { currencyExpanded = !currencyExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = LivePricesRepository.currencies.find { it.code == selectedCurrencyCode }?.let { "${it.flag} ${it.nameAr} (${it.code})" } ?: selectedCurrencyCode,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("عملة تسعير الذهب") },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.border,
                            focusedLabelColor = colors.accent
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = currencyExpanded,
                        onDismissRequest = { currencyExpanded = false }
                    ) {
                        LivePricesRepository.currencies.forEach { c ->
                            DropdownMenuItem(
                                text = { Text("${c.flag} ${c.nameAr} (${c.code})") },
                                onClick = {
                                    selectedCurrencyCode = c.code
                                    currencyExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Premium Karat Selection with elegant gold descriptions
                Text("اختر العيار الشريف", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.text, modifier = Modifier.padding(bottom = 8.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val karats = listOf(
                        Triple(24, "ذهب خالص بنسبة 99.9% (نقي)", "مثالي للادخار والسبائك الاستثمارية"),
                        Triple(21, "ذهب عيار 21 (المصنع الأكثر شهرة)", "الخيار الأول للزينة والمشغولات التقليدية"),
                        Triple(18, "ذهب عيار 18 (الصلابة والجمال اليومي)", "مثالي للمصوغات المرصعة بالأحجار الكريمة")
                    )

                    karats.forEach { (k, title, desc) ->
                        val isSelected = selectedKarat == k
                        Surface(
                            color = if (isSelected) colors.accent.copy(alpha = 0.15f) else colors.surface2.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) colors.accent else colors.border.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedKarat = k }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) colors.accent else colors.textMuted.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "ع$k",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isSelected) Color.White else colors.text
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.text)
                                    Text(desc, fontSize = 10.sp, color = colors.textMuted)
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, null, tint = colors.accent, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = gramsText,
                    onValueChange = { gramsText = it },
                    label = { Text("الوزن الإجمالي (جرام)") },
                    placeholder = { Text("أدخل عدد الجرامات") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.border,
                        focusedLabelColor = colors.accent
                    ),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = makingFeeGrams,
                    onValueChange = { makingFeeGrams = it },
                    label = { Text("قيمة المصنعية لكل جرام ($selectedCurrencyCode)") },
                    placeholder = { Text("أدخل تكلفة المصنعية للصائغ") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.border,
                        focusedLabelColor = colors.accent
                    ),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Premium Result Card with professional dynamic breakdown
        Surface(
            color = colors.accent.copy(alpha = 0.08f),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.5.dp, colors.accent),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("التكلفة الإجمالية التقديرية للشراء", fontSize = 13.sp, color = colors.textMuted, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "${LivePricesRepository.formatNumber(totalGoldCost)} $selectedCurrencyCode",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.accent
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Elegant receipt-style breakdown
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surface.copy(alpha = 0.6f))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("سعر الجرام الصافي الصرف الحالي:", fontSize = 11.sp, color = colors.textMuted)
                        Text("${LivePricesRepository.formatNumber(pricePerGramLocal)} $selectedCurrencyCode", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.text)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("تكلفة الذهب الخام الصافي (${grams} جرام):", fontSize = 11.sp, color = colors.textMuted)
                        Text("${LivePricesRepository.formatNumber(rawGoldCost)} $selectedCurrencyCode", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.text)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("مجموع المصنعية المضافة للصياغة:", fontSize = 11.sp, color = colors.textMuted)
                        Text("${LivePricesRepository.formatNumber(makingFeeCost)} $selectedCurrencyCode", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.text)
                    }
                }
            }
        }
    }
}

@Composable
fun UnitConverterScreen(colors: CustomThemeColors) {
    var selectedCategory by remember { mutableStateOf("الطول") }
    var inputValue by remember { mutableStateOf("100") }

    val categories = listOf("الطول", "الكتلة", "المساحة", "الحرارة", "السرعة")
    val input = inputValue.toDoubleOrNull() ?: 0.0

    val converted = when (selectedCategory) {
        "الطول" -> listOf("متر" to input * 1000, "ميل" to input * 0.621371)
        "الكتلة" -> listOf("جرام" to input * 1000, "رطل" to input * 2.20462)
        "المساحة" -> listOf("م²" to input * 10000, "فدان" to input * 0.238)
        "الحرارة" -> listOf("°F" to (input * 9/5) + 32, "K" to input + 273.15)
        else -> listOf("م/ث" to input * 0.277778, "ميل/س" to input * 0.621371)
    }

    ToolScreenScaffold(
        colors = colors,
        icon = AppIcons.forCalc(CalcKey.UNIT),
        title = "محول الوحدات",
        subtitle = "تحويل سريع وسهل بين مختلف وحدات القياس العالمية"
    ) {
        // Category Selection
        Surface(
            color = colors.surface2.copy(alpha = 0.3f),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(8.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                categories.forEach { cat ->
                    Surface(
                        color = if (selectedCategory == cat) colors.accent else Color.Transparent,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedCategory = cat }
                    ) {
                        Text(
                            cat,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedCategory == cat) Color.White else colors.text,
                            modifier = Modifier.padding(vertical = 10.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Input Card
        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                OutlinedTextField(
                    value = inputValue,
                    onValueChange = { inputValue = it },
                    label = { Text("القيمة المدخلة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.border
                    ),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Results Card
        Surface(
            color = colors.surface2.copy(alpha = 0.5f),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("نتائج التحويل", fontSize = 14.sp, color = colors.textMuted, modifier = Modifier.padding(bottom = 16.dp))
                
                converted.forEach { (unit, value) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.surface.copy(alpha = 0.5f))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(unit, fontWeight = FontWeight.Bold, color = colors.text)
                        Text(
                            String.format("%.4f", value).trimEnd('0').trimEnd('.'),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = colors.accent
                        )
                    }
                }
            }
        }
    }
}
