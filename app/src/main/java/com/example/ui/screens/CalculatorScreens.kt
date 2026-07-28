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
import com.example.data.LivePricesRepository
import com.example.ui.theme.CustomThemeColors
import kotlin.math.*

@Composable
fun BasicCalculatorScreen(colors: CustomThemeColors) {
    var displayExpression by remember { mutableStateOf("0") }
    var historyExpression by remember { mutableStateOf("") }
    var showScientific by remember { mutableStateOf(false) }

    fun onBtnClick(label: String) {
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
                    displayExpression = if (eval == eval.toLong().toDouble()) eval.toLong().toString() else String.format("%.4f", eval)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.appBg)
            .padding(12.dp)
    ) {
        // Display Box
        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.35f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(historyExpression, fontSize = 16.sp, color = colors.textMuted, textAlign = TextAlign.End)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    displayExpression,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.text,
                    textAlign = TextAlign.End,
                    maxLines = 2
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Toggle Scientific
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = { showScientific = !showScientific }) {
                Text(if (showScientific) "إخفاء الدقائق العلمية 📐" else "الآلة العلمية 📐", color = colors.accent, fontSize = 12.sp)
            }
        }

        if (showScientific) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                items(sciBtns) { btn ->
                    Surface(
                        color = colors.surface2,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(42.dp)
                            .clickable { onBtnClick(btn) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(btn, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.accent)
                        }
                    }
                }
            }
        }

        // Keypad Grid
        androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(0.65f)
            ) {
                items(basicBtns) { btn ->
                    val isOp = btn in listOf("÷", "×", "-", "+", "=")
                    val isAction = btn in listOf("C", "⌫", "%", "±")

                    val btnBg = when {
                        btn == "=" -> colors.accent
                        isOp -> colors.surface2
                        isAction -> colors.surface2.copy(alpha = 0.7f)
                        else -> colors.surface
                    }

                    val btnFg = when {
                        btn == "=" -> Color.White
                        isOp -> colors.accent
                        else -> colors.text
                    }

                    Surface(
                        color = btnBg,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.2f)
                            .clickable { onBtnClick(btn) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(btn, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = btnFg)
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
    var amountText by remember { mutableStateOf("") }
    var fromCode by remember { mutableStateOf("USD") }
    var toCode by remember { mutableStateOf("EGP") }

    val amount = amountText.toDoubleOrNull() ?: 0.0
    val converted = LivePricesRepository.convertCurrency(amount, fromCode, toCode)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.appBg)
            .padding(12.dp)
    ) {
        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("💱 محول العملات المباشر", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.text)
                Text("أسعار صرف حية محدثة من الأسواق المباشرة", fontSize = 11.sp, color = colors.textMuted)

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("المبلغ المراد تحويله", color = colors.textMuted) },
                    placeholder = { Text("0", color = colors.textMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // From Dropdown
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
                            label = { Text("من", color = colors.textMuted) },
                            modifier = Modifier.menuAnchor()
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
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Icon(Icons.Default.SwapVert, contentDescription = "تبديل", tint = colors.accent)
                    }

                    // To Dropdown
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
                            label = { Text("إلى", color = colors.textMuted) },
                            modifier = Modifier.menuAnchor()
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

        Spacer(modifier = Modifier.height(16.dp))

        // Result Card
        Surface(
            color = colors.surface2,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("النتيجة المحولة:", fontSize = 13.sp, color = colors.textMuted)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${LivePricesRepository.formatNumber(converted)} $toCode",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.accent
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoldCalcScreen(colors: CustomThemeColors) {
    val coroutineScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (!LivePricesRepository.isLiveDataLoaded) {
            isRefreshing = true
            LivePricesRepository.refreshLivePrices(context)
            isRefreshing = false
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

    val totalGoldCost = grams * (pricePerGramLocal + fee)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.appBg)
            .padding(12.dp)
    ) {
        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("🥇 حاسبة أسعار ومصنعية الذهب", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.text)
                Text(
                    "سعر جرام عيار $selectedKarat الحالي: ${LivePricesRepository.formatNumber(pricePerGramLocal)} $selectedCurrencyCode",
                    fontSize = 12.sp,
                    color = colors.accent
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Currency Dropdown Selector
                ExposedDropdownMenuBox(
                    expanded = currencyExpanded,
                    onExpandedChange = { currencyExpanded = !currencyExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = LivePricesRepository.currencies.find { it.code == selectedCurrencyCode }?.let { "${it.flag} ${it.nameAr} (${it.code})" } ?: selectedCurrencyCode,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("عملة الشراء والأسعار", color = colors.textMuted) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.surface2,
                            unfocusedContainerColor = colors.surface2,
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.text,
                            unfocusedTextColor = colors.text
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

                Spacer(modifier = Modifier.height(12.dp))

                // Karat Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(24, 22, 21, 18, 14).forEach { k ->
                        Surface(
                            color = if (selectedKarat == k) colors.accent else colors.surface2,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedKarat = k }
                        ) {
                            Text(
                                "عيار $k",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedKarat == k) Color.White else colors.text,
                                modifier = Modifier.padding(8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = gramsText,
                    onValueChange = { gramsText = it },
                    label = { Text("الوزن بالجرام", color = colors.textMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = makingFeeGrams,
                    onValueChange = { makingFeeGrams = it },
                    label = { Text("مصنعية الجرام الواحد ($selectedCurrencyCode)", color = colors.textMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            color = colors.surface2,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("إجمالي التكلفة المتوقعة للشراء:", fontSize = 13.sp, color = colors.textMuted)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${LivePricesRepository.formatNumber(totalGoldCost)} $selectedCurrencyCode",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.accent
                )
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
        "الطول" -> Pair("${input * 1000} متر", "${String.format("%.2f", input * 0.621371)} ميل")
        "الكتلة" -> Pair("${input * 1000} جرام", "${String.format("%.2f", input * 2.20462)} رطل")
        "المساحة" -> Pair("${input * 10000} م²", "${String.format("%.2f", input * 0.238)} فدان")
        "الحرارة" -> Pair("${(input * 9/5) + 32} °F", "${input + 273.15} K")
        else -> Pair("${String.format("%.2f", input * 0.277778)} م/ث", "${String.format("%.2f", input * 0.621371)} ميل/س")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.appBg)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            categories.forEach { cat ->
                Surface(
                    color = if (selectedCategory == cat) colors.accent else colors.surface,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedCategory = cat }
                ) {
                    Text(
                        cat,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedCategory == cat) Color.White else colors.text,
                        modifier = Modifier.padding(8.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = inputValue,
            onValueChange = { inputValue = it },
            label = { Text("القيمة المراد تحويلها", color = colors.textMuted) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("نتائج التحويل:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.text)
                Spacer(modifier = Modifier.height(8.dp))
                Text("• ${converted.first}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.accent)
                Spacer(modifier = Modifier.height(4.dp))
                Text("• ${converted.second}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.accent)
            }
        }
    }
}
