package com.example.ui.screens
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import java.util.Calendar
import android.content.Intent
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

data class CalculatorRecord(
    val expression: String,
    val result: String,
    val timestamp: String
)

@Composable
fun BasicCalculatorScreen(colors: CustomThemeColors) {
    val haptic = LocalHapticFeedback.current
    var expression by rememberSaveable { mutableStateOf("0") }
    var historyExpression by rememberSaveable { mutableStateOf("") }
    var showScientific by rememberSaveable { mutableStateOf(false) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var isDegree by rememberSaveable { mutableStateOf(true) }
    var historyList by remember { mutableStateOf(listOf<CalculatorRecord>()) }

    // Live evaluation result
    val liveResult = remember(expression, isDegree) {
        try {
            if (expression == "0" || expression.isBlank()) "0"
            else {
                val eval = evaluateAdvancedExpr(expression, isDegree)
                if (eval.isNaN() || eval.isInfinite()) "خطأ رياضي"
                else if (eval == eval.toLong().toDouble()) eval.toLong().toString()
                else String.format(java.util.Locale.US, "%.4f", eval).trimEnd('0').trimEnd('.')
            }
        } catch (e: Exception) {
            "..."
        }
    }

    fun onBtnClick(label: String) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        when (label) {
            "C" -> {
                expression = "0"
                historyExpression = ""
            }
            "⌫" -> {
                expression = if (expression.length <= 1) "0" else expression.dropLast(1)
            }
            "=" -> {
                try {
                    historyExpression = expression
                    val eval = evaluateAdvancedExpr(expression, isDegree)
                    val resultStr = if (eval.isNaN() || eval.isInfinite()) {
                        "خطأ رياضي"
                    } else if (eval == eval.toLong().toDouble()) {
                        eval.toLong().toString()
                    } else {
                        String.format(java.util.Locale.US, "%.4f", eval).trimEnd('0').trimEnd('.')
                    }
                    val record = CalculatorRecord(
                        expression = expression,
                        result = resultStr,
                        timestamp = Calendar.getInstance().let { "${it.get(Calendar.HOUR_OF_DAY)}:${String.format("%02d", it.get(Calendar.MINUTE))}" }
                    )
                    historyList = listOf(record) + historyList
                    expression = resultStr
                } catch (e: Exception) {
                    expression = "خطأ رياضي"
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }
            "±" -> {
                expression = if (expression.startsWith("-")) expression.drop(1) else "-$expression"
            }
            "sin", "cos", "tan", "log", "ln", "√" -> {
                expression = if (expression == "0") "$label(" else expression + "$label("
            }
            "x²" -> {
                expression = "$expression^2"
            }
            else -> {
                expression = if (expression == "0" && label !in listOf("+", "-", "×", "÷", ".")) label else expression + label
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
        subtitle = "حاسبة علمية متقدمة مع ذاكرة وسجل عمليات (Amber Gold & Obsidian)",
        isScrollable = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. DYNAMIC DUAL-LINE DISPLAY BOX (Obsidian Glass + Gold Border)
            Surface(
                color = Color(0xFF141926).copy(alpha = 0.85f),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.5.dp, Color(0xFFD4AF37).copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Top Bar inside Display: DEG/RAD toggle & History Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.clickable {
                                isDegree = !isDegree
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFD4AF37).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color(0xFFD4AF37))
                        ) {
                            Text(
                                if (isDegree) "DEG" else "RAD",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD4AF37)
                            )
                        }

                        IconButton(
                            onClick = {
                                showHistory = !showHistory
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.History, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                        }
                    }

                    if (historyExpression.isNotEmpty()) {
                        Text(
                            historyExpression,
                            fontSize = 16.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.End,
                            maxLines = 1
                        )
                    }

                    val dynamicFontSize = when {
                        expression.length > 15 -> 22.sp
                        expression.length > 10 -> 28.sp
                        else -> 38.sp
                    }

                    Text(
                        expression,
                        fontSize = dynamicFontSize,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.End,
                        maxLines = 2
                    )

                    HorizontalDivider(color = Color(0xFF334155), thickness = 1.dp)

                    // Live Result Preview
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("النتيجة الحية:", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        Text(
                            liveResult,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFF59E0B),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }

            // 2. HISTORY DRAWER / BOTTOM SHEET (AnimatedVisibility)
            AnimatedVisibility(visible = showHistory) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF141926),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("سجل العمليات السابقة", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            TextButton(onClick = { historyList = emptyList() }) {
                                Text("مسح السجل", fontSize = 11.sp, color = Color(0xFFEF4444))
                            }
                        }

                        if (historyList.isNotEmpty()) {
                            historyList.take(5).forEach { rec ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF0F172A),
                                    modifier = Modifier.clickable {
                                        expression = rec.result
                                        showHistory = false
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(rec.expression, fontSize = 11.sp, color = Color(0xFF94A3B8))
                                            Text("= ${rec.result}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                                        }
                                        Text(rec.timestamp, fontSize = 9.sp, color = Color(0xFF64748B))
                                    }
                                }
                            }
                        } else {
                            Text("لا توجد عمليات سابقة محفوظة", fontSize = 11.sp, color = Color(0xFF94A3B8), textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth().padding(8.dp))
                        }
                    }
                }
            }

            // 3. SCIENTIFIC PANEL TOGGLE & ANIMATED GRID
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("اللوحة العلمية المتقدمة", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                TextButton(onClick = { showScientific = !showScientific }) {
                    Text(if (showScientific) "إخفاء ✕" else "إظهار 📐", color = Color(0xFFD4AF37), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            AnimatedVisibility(visible = showScientific) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                ) {
                    items(sciBtns) { btn ->
                        Surface(
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)),
                            modifier = Modifier
                                .height(48.dp)
                                .clickable { onBtnClick(btn) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(btn, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                            }
                        }
                    }
                }
            }

            // 4. STANDARD NUMERIC KEYPAD (4x5 Grid Layout)
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val rows = basicBtns.chunked(4)
                    rows.forEach { rowBtns ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowBtns.forEach { btn ->
                                val isOp = btn in listOf("÷", "×", "-", "+", "=")
                                val isAction = btn in listOf("C", "⌫", "%", "±")

                                val btnBg = when {
                                    btn == "=" -> Color.Transparent
                                    isOp -> Color(0xFF1E293B)
                                    isAction -> Color(0xFF1E293B)
                                    else -> Color(0xFF141926)
                                }

                                val btnFg = when {
                                    btn == "=" -> Color.White
                                    isOp -> Color(0xFFF59E0B)
                                    isAction -> Color(0xFFEF4444)
                                    else -> Color.White
                                }

                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (btn == "=") Color(0xFFF59E0B) else Color(0xFF334155)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(18.dp))
                                        .clickable { onBtnClick(btn) }
                                        .then(
                                            if (btn == "=") {
                                                Modifier.background(Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFD4AF37))))
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
                                        Text(btn, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = btnFg)
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

private fun evaluateAdvancedExpr(expr: String, isDegree: Boolean): Double {
    val clean = expr
        .replace("×", "*")
        .replace("÷", "/")
        .replace("π", Math.PI.toString())
        .replace("e", Math.E.toString())
        .replace("√", "sqrt")

    return evaluateExpression(clean, isDegree)
}

private fun evaluateExpression(expr: String, isDegree: Boolean): Double {
    if (expr.isBlank() || expr == "0") return 0.0

    class Parser(val s: String) {
        var pos = 0
        fun peek(): Char = if (pos < s.length) s[pos] else '\u0000'
        fun get(): Char = if (pos < s.length) s[pos++] else '\u0000'

        fun parse(): Double {
            return parseExpression()
        }

        fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                when {
                    eat('+') -> x += parseTerm()
                    eat('-') -> x -= parseTerm()
                    else -> return x
                }
            }
        }

        fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                when {
                    eat('*') -> x *= parseFactor()
                    eat('/') -> {
                        val div = parseFactor()
                        if (div == 0.0) throw ArithmeticException("Division by zero")
                        x /= div
                    }
                    eat('%') -> x %= parseFactor()
                    else -> return x
                }
            }
        }

        fun parseFactor(): Double {
            if (eat('+')) return parseFactor()
            if (eat('-')) return -parseFactor()

            var x: Double
            if (eat('(')) {
                x = parseExpression()
                eat(')')
            } else if (peek().isLetter() || peek() == '√' || peek() == 'π') {
                val sb = StringBuilder()
                while (peek().isLetter() || peek() == '√' || peek() == 'π' || peek() == '_') {
                    sb.append(get())
                }
                val func = sb.toString()
                if (peek() == '(') {
                    get()
                    val arg = parseExpression()
                    eat(')')
                    x = applyFunction(func, arg, isDegree)
                } else {
                    x = applyConstant(func)
                }
            } else if (peek().isDigit() || peek() == '.') {
                val sb = StringBuilder()
                while (pos < s.length && (s[pos].isDigit() || s[pos] == '.')) {
                    sb.append(get())
                }
                x = sb.toString().toDoubleOrNull() ?: 0.0
            } else {
                throw IllegalArgumentException("Unexpected character")
            }

            if (eat('^')) {
                val exponent = parseFactor()
                x = x.pow(exponent)
            }
            return x
        }

        fun eat(char: Char): Boolean {
            while (pos < s.length && s[pos] == ' ') pos++
            if (pos < s.length && s[pos] == char) {
                pos++
                return true
            }
            return false
        }
    }

    return Parser(expr.replace(" ", "")).parse()
}

private fun applyFunction(func: String, arg: Double, isDegree: Boolean): Double {
    val radArg = if (isDegree) Math.toRadians(arg) else arg
    return when (func.lowercase()) {
        "sin" -> sin(radArg)
        "cos" -> cos(radArg)
        "tan" -> tan(radArg)
        "log" -> log10(arg)
        "ln" -> ln(arg)
        "sqrt", "√" -> sqrt(arg)
        else -> arg
    }
}

private fun applyConstant(const: String): Double {
    return when (const.lowercase()) {
        "pi", "π" -> PI
        "e" -> E
        else -> 0.0
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyConverterScreen(colors: CustomThemeColors) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current

    var amountText by rememberSaveable { mutableStateOf("100") }
    var fromCode by rememberSaveable { mutableStateOf("USD") }
    var toCode by rememberSaveable { mutableStateOf("EGP") }
    var isHistoryExpanded by rememberSaveable { mutableStateOf(false) }
    var favoritesList by rememberSaveable { mutableStateOf(setOf<String>()) }
    var conversionHistory by remember { mutableStateOf(listOf<CurrencyRecord>()) }

    val amount = amountText.toDoubleOrNull() ?: 0.0
    val converted = LivePricesRepository.convertCurrency(amount, fromCode, toCode)
    val baseRate = LivePricesRepository.convertCurrency(1.0, fromCode, toCode)
    val inverseRate = LivePricesRepository.convertCurrency(1.0, toCode, fromCode)

    var swapRotation by remember { mutableStateOf(0f) }
    val animatedRotation by animateFloatAsState(
        targetValue = swapRotation,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "swapRotation"
    )

    ToolScreenScaffold(
        colors = colors,
        icon = AppIcons.forCalc(CalcKey.CURRENCY),
        title = "محول العملات الذكي",
        subtitle = "تحويل فوري دقيق مع تغذية حية من الأسواق العالمية (Cyber Emerald & Gold)"
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. LIVE MARKET STATUS INDICATOR BAR
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF141926).copy(alpha = 0.85f),
                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val pulseAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.3f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "dot"
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981).copy(alpha = pulseAlpha))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("● الأسعار حية ومحدثة مباشرة من البورصة", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                    }
                    Text(LivePricesRepository.lastUpdatedText.ifEmpty { "آخر تحديث: الآن" }, fontSize = 10.sp, color = Color(0xFF94A3B8))
                }
            }

            // 2. HERO BANNER WITH PROCEDURAL CANVAS GRAPHICS
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFF141926),
                border = BorderStroke(1.5.dp, Color(0xFFD4AF37).copy(alpha = 0.6f))
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        for (i in 0..4) {
                            drawLine(
                                color = Color(0xFF10B981).copy(alpha = 0.1f),
                                start = Offset(0f, h * (i.toFloat() / 4f)),
                                end = Offset(w, h * (i.toFloat() / 4f)),
                                strokeWidth = 1f
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CurrencyExchange, null, tint = Color(0xFFD4AF37), modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("محول العملات المتقدم", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("أعلى دقة مالية لأسعار الصرف العالمية والمحلية", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFD4AF37).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color(0xFFD4AF37))
                        ) {
                            Text("$fromCode ⇄ $toCode", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD4AF37))
                        }
                    }
                }
            }

            // 3. QUICK CURRENCY FAVORITES CHIPS (LazyRow)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val commonCodes = listOf("USD", "EUR", "EGP", "SAR", "AED", "KWD", "GBP")
                items(commonCodes) { code ->
                    val isSelected = fromCode == code || toCode == code
                    Surface(
                        modifier = Modifier.clickable {
                            if (fromCode != code) {
                                fromCode = code
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) Color(0xFFD4AF37).copy(alpha = 0.25f) else Color(0xFF141926),
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFFD4AF37) else Color(0xFF334155))
                    ) {
                        Text(
                            code,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color(0xFFD4AF37) else Color(0xFF94A3B8)
                        )
                    }
                }
            }

            // 4. PRIMARY EXCHANGE CALCULATOR CARD
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF141926),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("المبلغ المراد تحويله") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF10B981),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedLabelColor = Color(0xFF10B981),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        trailingIcon = {
                            if (amountText.isNotEmpty()) {
                                IconButton(onClick = { 
                                    amountText = ""
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }) {
                                    Icon(Icons.Default.Clear, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    )

                    // Quick amount preset chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val presets = listOf("100", "500", "1000", "5000", "10000")
                        presets.forEach { preset ->
                            item {
                                Surface(
                                    modifier = Modifier.clickable {
                                        amountText = preset
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (amountText == preset) Color(0xFF10B981).copy(alpha = 0.25f) else Color(0xFF1E293B),
                                    border = BorderStroke(1.dp, if (amountText == preset) Color(0xFF10B981) else Color(0xFF334155))
                                ) {
                                    Text(
                                        preset,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (amountText == preset) Color(0xFF10B981) else Color.White
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // From Currency Selector
                        var fromExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable { fromExpanded = true },
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF1E293B),
                                border = BorderStroke(1.dp, Color(0xFF334155))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val currObj = LivePricesRepository.currencies.firstOrNull { it.code == fromCode }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(currObj?.flag ?: "🌐", fontSize = 18.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("من العملة", fontSize = 9.sp, color = Color(0xFF94A3B8))
                                            Text(fromCode, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                    Icon(Icons.Default.ArrowDropDown, null, tint = Color(0xFF94A3B8))
                                }
                            }

                            DropdownMenu(expanded = fromExpanded, onDismissRequest = { fromExpanded = false }) {
                                LivePricesRepository.currencies.forEach { c ->
                                    DropdownMenuItem(
                                        text = { Text("${c.flag} ${c.code} - ${c.nameAr}", fontSize = 13.sp) },
                                        onClick = {
                                            fromCode = c.code
                                            fromExpanded = false
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                    )
                                }
                            }
                        }

                        // Swap Button with Rotation
                        IconButton(
                            onClick = {
                                val tmp = fromCode
                                fromCode = toCode
                                toCode = tmp
                                swapRotation += 180f
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF3B82F6).copy(alpha = 0.2f))
                                .graphicsLayer { rotationZ = animatedRotation }
                        ) {
                            Icon(Icons.Default.SwapHoriz, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(20.dp))
                        }

                        // To Currency Selector
                        var toExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable { toExpanded = true },
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF1E293B),
                                border = BorderStroke(1.dp, Color(0xFF334155))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val currObj = LivePricesRepository.currencies.firstOrNull { it.code == toCode }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(currObj?.flag ?: "🌐", fontSize = 18.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("إلى العملة", fontSize = 9.sp, color = Color(0xFF94A3B8))
                                            Text(toCode, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                    Icon(Icons.Default.ArrowDropDown, null, tint = Color(0xFF94A3B8))
                                }
                            }

                            DropdownMenu(expanded = toExpanded, onDismissRequest = { toExpanded = false }) {
                                LivePricesRepository.currencies.forEach { c ->
                                    DropdownMenuItem(
                                        text = { Text("${c.flag} ${c.code} - ${c.nameAr}", fontSize = 13.sp) },
                                        onClick = {
                                            toCode = c.code
                                            toExpanded = false
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. CONVERTED RESULT GLASS PANEL & SPARKLINE CHART
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF141926),
                border = BorderStroke(1.5.dp, Color(0xFFF59E0B).copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("القيمة المعادلة بدقة فائقة", fontSize = 12.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                    Text(
                        "${LivePricesRepository.formatNumber(converted)} $toCode",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFF59E0B)
                    )

                    // Rates Breakdown & Sparkline Chart
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("1 $fromCode = ${LivePricesRepository.formatNumber(baseRate, 4)} $toCode", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("1 $toCode = ${LivePricesRepository.formatNumber(inverseRate, 4)} $fromCode", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        }

                        // Mini Sparkline Canvas
                        Box(
                            modifier = Modifier
                                .width(110.dp)
                                .height(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0F172A))
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height
                                val points = listOf(0.4f, 0.6f, 0.5f, 0.8f, 0.7f, 0.9f, 0.85f)
                                val path = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(0f, h * (1f - points[0]))
                                    for (i in 1 until points.size) {
                                        val x = (i.toFloat() / (points.size - 1)) * w
                                        lineTo(x, h * (1f - points[i]))
                                    }
                                }
                                drawPath(path, color = Color(0xFF10B981), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))
                            }
                        }
                    }
                }
            }

            // 6. QUICK ACTION BUTTONS ROW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val favKey = "$fromCode:$toCode"
                val isFav = favoritesList.contains(favKey)

                Button(
                    onClick = {
                        favoritesList = if (isFav) favoritesList - favKey else favoritesList + favKey
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isFav) Color(0xFFF59E0B) else Color(0xFF1E293B))
                ) {
                    Icon(if (isFav) Icons.Default.Star else Icons.Default.StarBorder, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isFav) "مفضل" else "إضافة للمفضلة", fontSize = 11.sp, color = Color.White)
                }

                Button(
                    onClick = {
                        val formatted = LivePricesRepository.formatNumber(converted)
                        clipboardManager.setText(AnnotatedString("$amountText $fromCode = $formatted $toCode"))
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                ) {
                    Icon(Icons.Default.ContentCopy, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("نسخ النتيجة", fontSize = 11.sp, color = Color.White)
                }
            }

            // 7. CONVERSION HISTORY & DRAWER
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF141926),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                isHistoryExpanded = !isHistoryExpanded
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, null, tint = Color(0xFFD4AF37), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("سجل عمليات التحويل والمفضلة", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Icon(
                            if (isHistoryExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            null,
                            tint = Color(0xFF94A3B8)
                        )
                    }

                    AnimatedVisibility(visible = isHistoryExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val record = CurrencyRecord(
                                        from = fromCode,
                                        to = toCode,
                                        amount = amountText,
                                        result = LivePricesRepository.formatNumber(converted),
                                        timestamp = Calendar.getInstance().let { "${it.get(Calendar.HOUR_OF_DAY)}:${String.format("%02d", it.get(Calendar.MINUTE))}" }
                                    )
                                    conversionHistory = listOf(record) + conversionHistory
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                            ) {
                                Text("حفظ التحويل الحالي للسجل", fontSize = 12.sp, color = Color.White)
                            }

                            if (conversionHistory.isNotEmpty()) {
                                Text("السجل الأخير", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                                conversionHistory.take(5).forEach { rec ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF0F172A)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text("${rec.amount} ${rec.from} ➔ ${rec.result} ${rec.to}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                Text(rec.timestamp, fontSize = 9.sp, color = Color(0xFF94A3B8))
                                            }
                                            TextButton(onClick = {
                                                fromCode = rec.from
                                                toCode = rec.to
                                                amountText = rec.amount
                                                isHistoryExpanded = false
                                            }) {
                                                Text("استعادة", fontSize = 10.sp, color = Color(0xFF3B82F6))
                                            }
                                        }
                                    }
                                }
                            } else {
                                Text("لا توجد سجلات محفوظة بعد", fontSize = 11.sp, color = Color(0xFF94A3B8), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

data class CurrencyRecord(
    val from: String,
    val to: String,
    val amount: String,
    val result: String,
    val timestamp: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoldCalcScreen(colors: CustomThemeColors) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (!LivePricesRepository.isLiveDataLoaded) {
            LivePricesRepository.refreshLivePrices(context)
        }
    }

    // Persistent state using rememberSaveable
    var selectedMode by rememberSaveable { mutableStateOf("BUY") } // "BUY" or "SELL"
    var selectedKarat by rememberSaveable { mutableStateOf(21) }
    var gramsText by rememberSaveable { mutableStateOf("10") }
    var makingFeeText by rememberSaveable { mutableStateOf("100") }
    var stampTaxText by rememberSaveable { mutableStateOf("25") }
    var merchantSpread by rememberSaveable { mutableStateOf(5f) } // 0% to 20%
    var selectedCurrencyCode by rememberSaveable { mutableStateOf("EGP") }
    var currencyExpanded by rememberSaveable { mutableStateOf(false) }

    // History and saved calculations
    var calculationHistory by remember { mutableStateOf(listOf<GoldCalcRecord>()) }

    val grams = gramsText.toDoubleOrNull() ?: 0.0
    val fee = makingFeeText.toDoubleOrNull() ?: 0.0
    val stampTax = stampTaxText.toDoubleOrNull() ?: 0.0

    val pricePerGramUsd = LivePricesRepository.getGoldPricePerGramInUsd(selectedKarat)
    val pricePerGramLocal = LivePricesRepository.convertCurrency(pricePerGramUsd, "USD", selectedCurrencyCode)

    val rawGoldCost = grams * pricePerGramLocal
    val makingFeeTotal = grams * fee
    
    val totalCost = if (selectedMode == "BUY") {
        (rawGoldCost + makingFeeTotal + stampTax).coerceAtLeast(0.0)
    } else {
        (rawGoldCost * (1.0 - (merchantSpread / 100.0))).coerceAtLeast(0.0)
    }

    // Animation for grand total
    val animatedTotal by animateFloatAsState(
        targetValue = totalCost.toFloat(),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "totalAnim"
    )

    // Error shake animation for negative or invalid inputs
    val isError = grams < 0 || fee < 0
    val shakeAnim = remember { Animatable(0f) }
    LaunchedEffect(isError) {
        if (isError) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            repeat(3) {
                shakeAnim.animateTo(10f, animationSpec = tween(50))
                shakeAnim.animateTo(-10f, animationSpec = tween(50))
            }
            shakeAnim.animateTo(0f, animationSpec = tween(50))
        }
    }

    ToolScreenScaffold(
        colors = colors,
        icon = AppIcons.forCalc(CalcKey.GOLD),
        title = "حاسبة الذهب الذكية",
        subtitle = "أوپولنت وشامبانيا غولد — حساب دقيق لعمليات الشراء والبيع الفوري للذهب"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = shakeAnim.value },
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. LIVE MARKET STATUS & PRICE BAR
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF141926).copy(alpha = 0.85f),
                border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val pulseAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.3f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "dot"
                        )
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981).copy(alpha = pulseAlpha))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("الأسعار حية ومحدثة", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                            Text("جرام ع$selectedKarat: ${LivePricesRepository.formatNumber(pricePerGramLocal)} $selectedCurrencyCode", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f))
                    ) {
                        Text("▲ +2.4%", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                    }
                }
            }

            // 2. CURRENCY SELECTOR & MODE SWITCHER
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF141926),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Currency dropdown
                    ExposedDropdownMenuBox(
                        expanded = currencyExpanded,
                        onExpandedChange = { currencyExpanded = !currencyExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = LivePricesRepository.currencies.find { it.code == selectedCurrencyCode }?.let { "${it.flag} ${it.nameAr} (${it.code})" } ?: selectedCurrencyCode,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("عملة التداول والتقييم") },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFD4AF37),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedLabelColor = Color(0xFFD4AF37),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
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
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                )
                            }
                        }
                    }

                    // Mode Switcher (Buy vs Sell)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isBuy = selectedMode == "BUY"
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedMode = "BUY"
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isBuy) Color(0xFF3B82F6).copy(alpha = 0.25f) else Color(0xFF1E293B),
                            border = BorderStroke(1.5.dp, if (isBuy) Color(0xFF3B82F6) else Color(0xFF334155))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ShoppingCart, null, tint = if (isBuy) Color(0xFF3B82F6) else Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("شراء مشغولات وسبائك", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isBuy) Color.White else Color(0xFF94A3B8))
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedMode = "SELL"
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                            shape = RoundedCornerShape(14.dp),
                            color = if (!isBuy) Color(0xFFEF4444).copy(alpha = 0.25f) else Color(0xFF1E293B),
                            border = BorderStroke(1.5.dp, if (!isBuy) Color(0xFFEF4444) else Color(0xFF334155))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Sell, null, tint = if (!isBuy) Color(0xFFEF4444) else Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("بيع ذهب مستعمل", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (!isBuy) Color.White else Color(0xFF94A3B8))
                            }
                        }
                    }
                }
            }

            // 3. KARAT SELECTOR GRID (5 KARATS)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF141926),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("اختر عيار الذهب", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    
                    val karats = listOf(
                        Triple(24, "عيار 24", "سبائك استثمارية (نقاء 99.9%)"),
                        Triple(22, "عيار 22", "مشغولات خليجية عالية النقاء"),
                        Triple(21, "عيار 21", "الخيار الأول والأكثر شهرة للزينة"),
                        Triple(18, "عيار 18", "أطقم عصرية وألماس ومجوهرات"),
                        Triple(14, "عيار 14", "اقتصادية وعالمية خفيفة")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        karats.forEach { (k, title, desc) ->
                            val isSelected = selectedKarat == k
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedKarat = k
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) Color(0xFFF59E0B).copy(alpha = 0.15f) else Color(0xFF1E293B).copy(alpha = 0.5f),
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) Color(0xFFF59E0B) else Color(0xFF334155)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) Color(0xFFF59E0B) else Color(0xFF334155)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("$k", fontSize = 11.sp, fontWeight = FontWeight.Black, color = if (isSelected) Color.Black else Color.White)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text(desc, fontSize = 10.sp, color = Color(0xFF94A3B8))
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. INPUT PARAMETERS & QUICK WEIGHT PRESETS
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF141926),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("الوزن والكمية", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)

                    OutlinedTextField(
                        value = gramsText,
                        onValueChange = { gramsText = it },
                        label = { Text("الوزن الإجمالي (بالجرام)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFD4AF37),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedLabelColor = Color(0xFFD4AF37),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )

                    // Quick Weight Presets LazyRow
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val presets = listOf("1", "5", "10", "31.1", "8")
                        val labels = listOf("1 جرام", "5 جرام", "10 جرام", "أونصة (31.1g)", "جنيه ذهب (8g)")
                        presets.forEachIndexed { index, preset ->
                            item {
                                Surface(
                                    modifier = Modifier.clickable {
                                        gramsText = preset
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (gramsText == preset) Color(0xFFD4AF37).copy(alpha = 0.25f) else Color(0xFF1E293B),
                                    border = BorderStroke(1.dp, if (gramsText == preset) Color(0xFFD4AF37) else Color(0xFF334155))
                                ) {
                                    Text(
                                        labels[index],
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (gramsText == preset) Color(0xFFD4AF37) else Color.White
                                    )
                                }
                            }
                        }
                    }

                    // Conditional Inputs for Buy Mode vs Sell Mode
                    AnimatedVisibility(visible = selectedMode == "BUY") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = makingFeeText,
                                onValueChange = { makingFeeText = it },
                                label = { Text("قيمة المصنعية لكل جرام ($selectedCurrencyCode)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFD4AF37),
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedLabelColor = Color(0xFFD4AF37),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                            )

                            OutlinedTextField(
                                value = stampTaxText,
                                onValueChange = { stampTaxText = it },
                                label = { Text("ضريبة الدمغة والقيمة المضافة الإجمالية ($selectedCurrencyCode)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFD4AF37),
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedLabelColor = Color(0xFFD4AF37),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                            )
                        }
                    }

                    AnimatedVisibility(visible = selectedMode == "SELL") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("نسبة خصم هامش التاجر (الكسر/المستعمل):", fontSize = 12.sp, color = Color.White)
                                Text("${String.format("%.1f", merchantSpread)}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                            }
                            Slider(
                                value = merchantSpread,
                                onValueChange = {
                                    merchantSpread = it
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                valueRange = 0f..20f,
                                steps = 20,
                                colors = SliderDefaults.colors(thumbColor = Color(0xFFEF4444), activeTrackColor = Color(0xFFEF4444))
                            )
                        }
                    }
                }
            }

            // 5. DETAILED COST BREAKDOWN GLASS PANEL
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF141926),
                border = BorderStroke(1.5.dp, Color(0xFFD4AF37))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (selectedMode == "BUY") "التكلفة الإجمالية التقديرية للشراء" else "إجمالي صافي سعر البيع",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${LivePricesRepository.formatNumber(animatedTotal.toDouble())} $selectedCurrencyCode",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFF59E0B)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF0F172A))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BreakdownRow("سعر الجرام الصافي:", "${LivePricesRepository.formatNumber(pricePerGramLocal)} $selectedCurrencyCode")
                        BreakdownRow("قيمة الذهب الخام (${grams}g):", "${LivePricesRepository.formatNumber(rawGoldCost)} $selectedCurrencyCode")
                        if (selectedMode == "BUY") {
                            BreakdownRow("إجمالي المصنعية:", "${LivePricesRepository.formatNumber(makingFeeTotal)} $selectedCurrencyCode")
                            BreakdownRow("ضريبة الدمغة:", "${LivePricesRepository.formatNumber(stampTax)} $selectedCurrencyCode")
                        } else {
                            BreakdownRow("خصم التاجر (${merchantSpread}%):", "- ${LivePricesRepository.formatNumber(rawGoldCost * (merchantSpread / 100.0))} $selectedCurrencyCode")
                        }
                    }
                }
            }

            // 6. QUICK ACTION BUTTONS PANEL
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionButtonCustom(
                    title = "حفظ النتيجة",
                    icon = Icons.Default.Bookmark,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val record = GoldCalcRecord(
                            mode = if (selectedMode == "BUY") "شراء" else "بيع",
                            karat = selectedKarat,
                            grams = grams,
                            total = totalCost,
                            currency = selectedCurrencyCode,
                            timestamp = Calendar.getInstance().let { "${it.get(Calendar.HOUR_OF_DAY)}:${String.format("%02d", it.get(Calendar.MINUTE))}" }
                        )
                        calculationHistory = listOf(record) + calculationHistory
                    }
                )
                ActionButtonCustom(
                    title = "مشاركة",
                    icon = Icons.Default.Share,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, "حاسبة الذهب الذكية:\nالنوع: ${if (selectedMode == "BUY") "شراء" else "بيع"}\nالعيار: ع$selectedKarat\nالوزن: ${grams}g\nالإجمالي: ${LivePricesRepository.formatNumber(totalCost)} $selectedCurrencyCode")
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "مشاركة تفاصيل حساب الذهب"))
                    }
                )
                ActionButtonCustom(
                    title = "تصدير PDF",
                    icon = Icons.Default.PictureAsPdf,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        clipboardManager.setText(AnnotatedString("تقرير حاسبة الذهب:\nالإجمالي: ${LivePricesRepository.formatNumber(totalCost)} $selectedCurrencyCode\nالتاريخ: ${Calendar.getInstance().time}"))
                    }
                )
                ActionButtonCustom(
                    title = "مسح",
                    icon = Icons.Default.Delete,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        gramsText = "10"
                        makingFeeText = "100"
                        stampTaxText = "25"
                    }
                )
            }

            // 7. HISTORY SECTION IF AVAILABLE
            if (calculationHistory.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF141926),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("سجل العمليات المحفوظة", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            TextButton(onClick = { calculationHistory = emptyList() }) {
                                Text("مسح السجل", fontSize = 11.sp, color = Color(0xFFEF4444))
                            }
                        }

                        calculationHistory.take(5).forEach { record ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1E293B),
                                border = BorderStroke(1.dp, Color(0xFF334155))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("${record.mode} عيار ${record.karat} (${record.grams}g)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text(record.timestamp, fontSize = 10.sp, color = Color(0xFF94A3B8))
                                    }
                                    Text("${LivePricesRepository.formatNumber(record.total)} ${record.currency}", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFFF59E0B))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class GoldCalcRecord(
    val mode: String,
    val karat: Int,
    val grams: Double,
    val total: Double,
    val currency: String,
    val timestamp: String
)

@Composable
fun BreakdownRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 11.sp, color = Color(0xFF94A3B8))
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
fun ActionButtonCustom(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF141926),
        border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, null, tint = Color(0xFFD4AF37), modifier = Modifier.size(18.dp))
            Text(title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
        }
    }
}

@Composable
fun UnitConverterScreen(colors: CustomThemeColors) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current

    // State persistence with rememberSaveable
    var selectedCategory by rememberSaveable { mutableStateOf("الطول") }
    var inputValue by rememberSaveable { mutableStateOf("100") }
    var selectedSourceUnit by rememberSaveable { mutableStateOf("") }
    var isHistoryExpanded by rememberSaveable { mutableStateOf(false) }
    
    // History and Favorites lists
    var historyList by remember { mutableStateOf(listOf<UnitConversionRecord>()) }
    var favoritesList by remember { mutableStateOf(setOf<String>()) } // format: "category:unit"

    val categories = listOf(
        "الطول", "الكتلة", "المساحة", "الحرارة", 
        "السرعة", "الحجم", "البيانات", "الضغط", 
        "الوقت", "الزاوية", "الطاقة", "القدرة"
    )

    // Unit definitions per category with conversion factors to base unit
    val unitDefinitions = remember(selectedCategory) {
        when (selectedCategory) {
            "الطول" -> listOf(
                UnitDef("مليمتر", "مم", 0.001, "Metric"),
                UnitDef("سنتيمتر", "سم", 0.01, "Metric"),
                UnitDef("متر", "م", 1.0, "Metric"),
                UnitDef("كيلومتر", "كم", 1000.0, "Metric"),
                UnitDef("بوصة", "in", 0.0254, "Imperial"),
                UnitDef("قدم", "ft", 0.3048, "Imperial"),
                UnitDef("ياردة", "yd", 0.9144, "Imperial"),
                UnitDef("ميل", "mi", 1609.344, "Imperial")
            )
            "الكتلة" -> listOf(
                UnitDef("مليجرام", "مج", 0.001, "Metric"),
                UnitDef("جرام", "ججم", 1.0, "Metric"),
                UnitDef("كيلوجرام", "كجم", 1000.0, "Metric"),
                UnitDef("طن", "طن", 1000000.0, "Metric"),
                UnitDef("أونصة", "oz", 28.3495, "Imperial"),
                UnitDef("رطل", "lb", 453.592, "Imperial")
            )
            "المساحة" -> listOf(
                UnitDef("سنتيمتر مربع", "سم²", 0.0001, "Metric"),
                UnitDef("متر مربع", "م²", 1.0, "Metric"),
                UnitDef("كيلومتر مربع", "كم²", 1000000.0, "Metric"),
                UnitDef("هكتار", "هكتار", 10000.0, "Metric"),
                UnitDef("فدان", "فدان", 4200.0, "Local"),
                UnitDef("قدم مربع", "ft²", 0.0929, "Imperial")
            )
            "الحرارة" -> listOf(
                UnitDef("مئوي", "°C", 1.0, "Metric"),
                UnitDef("فهرنهايت", "°F", 1.0, "Imperial"),
                UnitDef("كلفن", "K", 1.0, "Scientific")
            )
            "السرعة" -> listOf(
                UnitDef("متر/ثانية", "م/ث", 1.0, "Metric"),
                UnitDef("كيلومتر/ساعة", "كم/س", 0.277778, "Metric"),
                UnitDef("ميل/ساعة", "mph", 0.44704, "Imperial"),
                UnitDef("عقدة", "knot", 0.514444, "Nautical")
            )
            "الحجم" -> listOf(
                UnitDef("مليلتر", "مل", 0.001, "Metric"),
                UnitDef("لتر", "لتر", 1.0, "Metric"),
                UnitDef("متر مكعب", "م³", 1000.0, "Metric"),
                UnitDef("جالون أمريكي", "US gal", 3.78541, "Imperial"),
                UnitDef("كوب", "كوب", 0.24, "Metric")
            )
            "البيانات" -> listOf(
                UnitDef("بايت", "B", 1.0, "Digital"),
                UnitDef("كيلوبايت", "KB", 1024.0, "Digital"),
                UnitDef("ميجابايت", "MB", 1048576.0, "Digital"),
                UnitDef("جيجابايت", "GB", 1073741824.0, "Digital"),
                UnitDef("تيرابايت", "TB", 1099511627776.0, "Digital")
            )
            "الضغط" -> listOf(
                UnitDef("باسكال", "Pa", 1.0, "Metric"),
                UnitDef("كيلوباسكال", "kPa", 1000.0, "Metric"),
                UnitDef("بار", "bar", 100000.0, "Metric"),
                UnitDef("جوي", "atm", 101325.0, "Standard"),
                UnitDef("مم زئبق", "mmHg", 133.322, "Medical")
            )
            "الوقت" -> listOf(
                UnitDef("ثانية", "ث", 1.0, "Metric"),
                UnitDef("دقيقة", "د", 60.0, "Metric"),
                UnitDef("ساعة", "س", 3600.0, "Metric"),
                UnitDef("يوم", "يوم", 86400.0, "Metric"),
                UnitDef("أسبوع", "أسبوع", 604800.0, "Metric")
            )
            "الزاوية" -> listOf(
                UnitDef("درجة", "°", 1.0, "Geometry"),
                UnitDef("راديان", "rad", 57.2958, "Geometry"),
                UnitDef("غراد", "grad", 0.9, "Geometry")
            )
            "الطاقة" -> listOf(
                UnitDef("جول", "J", 1.0, "Metric"),
                UnitDef("كيلوجول", "kJ", 1000.0, "Metric"),
                UnitDef("كالوري", "cal", 4.184, "Metric"),
                UnitDef("كيلوكالوري", "kcal", 4184.0, "Metric"),
                UnitDef("واط ساعة", "Wh", 3600.0, "Metric")
            )
            else -> listOf(
                UnitDef("واط", "W", 1.0, "Metric"),
                UnitDef("كيلوواط", "kW", 1000.0, "Metric"),
                UnitDef("حصان", "HP", 745.7, "Mechanical")
            )
        }
    }

    // Default source unit if not set or invalid for category
    val currentSourceUnit = if (unitDefinitions.any { it.name == selectedSourceUnit }) {
        selectedSourceUnit
    } else {
        unitDefinitions.first().name
    }

    val inputNumber = inputValue.toDoubleOrNull() ?: 0.0
    val isError = inputNumber < 0 && selectedCategory != "الحرارة" // Temperature can be negative

    // Error shake animation
    val shakeAnim = remember { Animatable(0f) }
    LaunchedEffect(isError) {
        if (isError) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            repeat(3) {
                shakeAnim.animateTo(10f, animationSpec = tween(50))
                shakeAnim.animateTo(-10f, animationSpec = tween(50))
            }
            shakeAnim.animateTo(0f, animationSpec = tween(50))
        }
    }

    // Calculation logic
    val conversionResults = remember(inputNumber, currentSourceUnit, selectedCategory, unitDefinitions) {
        val sourceDef = unitDefinitions.firstOrNull { it.name == currentSourceUnit } ?: unitDefinitions.first()
        
        // Convert input to base unit first
        val baseValue = if (selectedCategory == "الحرارة") {
            when (currentSourceUnit) {
                "فهرنهايت" -> (inputNumber - 32.0) * 5.0 / 9.0
                "كلفن" -> inputNumber - 273.15
                else -> inputNumber // Celsius
            }
        } else {
            inputNumber * sourceDef.factor
        }

        unitDefinitions.map { targetDef ->
            val convertedVal = if (selectedCategory == "الحرارة") {
                when (targetDef.name) {
                    "فهرنهايت" -> (baseValue * 9.0 / 5.0) + 32.0
                    "كلفن" -> baseValue + 273.15
                    else -> baseValue
                }
            } else {
                if (targetDef.factor != 0.0) baseValue / targetDef.factor else 0.0
            }
            targetDef.name to convertedVal
        }
    }

    ToolScreenScaffold(
        colors = colors,
        icon = AppIcons.forCalc(CalcKey.UNIT),
        title = "محول الوحدات الشامل",
        subtitle = "محرك تحويلات فوري عالي الدقة (Cyber Sapphire & Gold)"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = shakeAnim.value },
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. OFFLINE STATUS BANNER
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF141926).copy(alpha = 0.85f),
                border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val pulseAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.4f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1200, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "dot"
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981).copy(alpha = pulseAlpha))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("يعمل بدون اتصال — جميع التحويلات متاحة محلياً", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                    }
                    Text("v2.4 Pro", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD4AF37))
                }
            }

            // 2. CANVAS PROCEDURAL HERO BANNER
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFF141926),
                border = BorderStroke(1.5.dp, Color(0xFFD4AF37).copy(alpha = 0.6f))
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(110.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        // Draw sci-fi grid lines
                        for (i in 0..5) {
                            drawLine(
                                color = Color(0xFF3B82F6).copy(alpha = 0.15f),
                                start = Offset(0f, h * (i.toFloat() / 5f)),
                                end = Offset(w, h * (i.toFloat() / 5f)),
                                strokeWidth = 1f
                            )
                        }
                        // Draw glowing measurement ruler wave
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(0f, h * 0.7f)
                            cubicTo(w * 0.25f, h * 0.2f, w * 0.75f, h * 1.1f, w, h * 0.4f)
                            lineTo(w, h)
                            lineTo(0f, h)
                            close()
                        }
                        drawPath(path, color = Color(0xFFD4AF37).copy(alpha = 0.1f))
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Straighten, null, tint = Color(0xFFD4AF37), modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("محول الوحدات الذكي", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("اختر الفئة وأدخل القيمة للحصول على مصفوفة النتائج المتزامنة", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    }
                }
            }

            // 3. SCROLLABLE CATEGORY SELECTOR CHIPS (LazyRow)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    Surface(
                        modifier = Modifier
                            .clickable {
                                selectedCategory = cat
                                selectedSourceUnit = "" // reset source unit
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) Color(0xFF3B82F6).copy(alpha = 0.25f) else Color(0xFF141926),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) Color(0xFF3B82F6) else Color(0xFF334155)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF59E0B))
                                )
                            }
                            Text(
                                cat,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }

            // 4. SOURCE VALUE INPUT CARD & UNIT SELECTOR
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF141926),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("القيمة المدخلة", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        
                        // Source Unit Dropdown Menu or Selector
                        var unitMenuExpanded by remember { mutableStateOf(false) }
                        Box {
                            Surface(
                                modifier = Modifier.clickable { unitMenuExpanded = true },
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF1E293B),
                                border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("الوحدة: $currentSourceUnit", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD4AF37))
                                    Icon(Icons.Default.ArrowDropDown, null, tint = Color(0xFFD4AF37), modifier = Modifier.size(16.dp))
                                }
                            }

                            DropdownMenu(
                                expanded = unitMenuExpanded,
                                onDismissRequest = { unitMenuExpanded = false }
                            ) {
                                unitDefinitions.forEach { def ->
                                    DropdownMenuItem(
                                        text = { Text("${def.name} (${def.symbol})", fontSize = 13.sp) },
                                        onClick = {
                                            selectedSourceUnit = def.name
                                            unitMenuExpanded = false
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Numeric Input Field
                    OutlinedTextField(
                        value = inputValue,
                        onValueChange = { inputValue = it },
                        placeholder = { Text("أدخل القيمة المراد تحويلها") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFD4AF37),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedLabelColor = Color(0xFFD4AF37),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        trailingIcon = {
                            if (inputValue.isNotEmpty()) {
                                IconButton(onClick = { 
                                    inputValue = "" 
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }) {
                                    Icon(Icons.Default.Clear, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    )

                    // Quick Preset Chips (1, 10, 100, 1000, 10000)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val presets = listOf("1", "10", "100", "1000", "10000")
                        presets.forEach { preset ->
                            item {
                                Surface(
                                    modifier = Modifier.clickable {
                                        inputValue = preset
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (inputValue == preset) Color(0xFFD4AF37).copy(alpha = 0.25f) else Color(0xFF1E293B),
                                    border = BorderStroke(1.dp, if (inputValue == preset) Color(0xFFD4AF37) else Color(0xFF334155))
                                ) {
                                    Text(
                                        preset,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (inputValue == preset) Color(0xFFD4AF37) else Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. MULTI-UNIT OUTPUT MATRIX (LazyVerticalGrid - 2 Columns)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("مصفوفة النتائج المتزامنة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("${conversionResults.size} وحدة متاحة", fontSize = 11.sp, color = Color(0xFF94A3B8))
                }

                // Grid of results
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(conversionResults) { (unitName, convertedValue) ->
                        val unitDef = unitDefinitions.firstOrNull { it.name == unitName }
                        val symbol = unitDef?.symbol ?: ""
                        val badgeType = unitDef?.badge ?: "Metric"
                        val favKey = "$selectedCategory:$unitName"
                        val isFavorite = favoritesList.contains(favKey)

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF141926),
                            border = BorderStroke(1.dp, Color(0xFF334155))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(unitName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), maxLines = 1)
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        // Favorite toggle
                                        IconButton(
                                            onClick = {
                                                favoritesList = if (isFavorite) favoritesList - favKey else favoritesList + favKey
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                                null,
                                                tint = if (isFavorite) Color(0xFFF59E0B) else Color(0xFF94A3B8),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        // Copy action
                                        IconButton(
                                            onClick = {
                                                val formatted = String.format("%.6f", convertedValue).trimEnd('0').trimEnd('.')
                                                clipboardManager.setText(AnnotatedString("$formatted $symbol"))
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }

                                Text(
                                    text = String.format("%.4f", convertedValue).trimEnd('0').trimEnd('.'),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFF59E0B)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = when (badgeType) {
                                            "Metric" -> Color(0xFF10B981).copy(alpha = 0.2f)
                                            "Imperial" -> Color(0xFF3B82F6).copy(alpha = 0.2f)
                                            else -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                                        }
                                    ) {
                                        Text(
                                            badgeType,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (badgeType) {
                                                "Metric" -> Color(0xFF10B981)
                                                "Imperial" -> Color(0xFF3B82F6)
                                                else -> Color(0xFFF59E0B)
                                            }
                                        )
                                    }
                                    Text(symbol, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // 6. CONVERSION HISTORY & FAVORITES DRAWER
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF141926),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                isHistoryExpanded = !isHistoryExpanded
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, null, tint = Color(0xFFD4AF37), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("سجل التحويلات والمفضلة", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Icon(
                            if (isHistoryExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            null,
                            tint = Color(0xFF94A3B8)
                        )
                    }

                    AnimatedVisibility(visible = isHistoryExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (favoritesList.isNotEmpty()) {
                                Text("العناصر المفضلة (${favoritesList.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                                favoritesList.take(3).forEach { fav ->
                                    val parts = fav.split(":")
                                    if (parts.size == 2) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFF1E293B)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("الفئة: ${parts[0]} | الوحدة: ${parts[1]}", fontSize = 11.sp, color = Color.White)
                                                IconButton(
                                                    onClick = { favoritesList = favoritesList - fav },
                                                    modifier = Modifier.size(16.dp)
                                                ) {
                                                    Icon(Icons.Default.Close, null, tint = Color(0xFFEF4444), modifier = Modifier.size(12.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            // Add current to history button
                            Button(
                                onClick = {
                                    val record = UnitConversionRecord(
                                        category = selectedCategory,
                                        sourceValue = inputValue,
                                        sourceUnit = currentSourceUnit,
                                        timestamp = Calendar.getInstance().let { "${it.get(Calendar.HOUR_OF_DAY)}:${String.format("%02d", it.get(Calendar.MINUTE))}" }
                                    )
                                    historyList = listOf(record) + historyList
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                            ) {
                                Text("حفظ عملية التحويل الحالية للسجل", fontSize = 12.sp, color = Color.White)
                            }

                            if (historyList.isNotEmpty()) {
                                Text("آخر عمليات التحويل المحفوظة", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                                historyList.take(5).forEach { rec ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF0F172A)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text("${rec.category}: ${rec.sourceValue} ${rec.sourceUnit}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                Text(rec.timestamp, fontSize = 9.sp, color = Color(0xFF94A3B8))
                                            }
                                            TextButton(onClick = {
                                                selectedCategory = rec.category
                                                inputValue = rec.sourceValue
                                                selectedSourceUnit = rec.sourceUnit
                                                isHistoryExpanded = false
                                            }) {
                                                Text("استعادة", fontSize = 10.sp, color = Color(0xFF3B82F6))
                                            }
                                        }
                                    }
                                }
                            } else {
                                Text("لا توجد سجلات محفوظة بعد", fontSize = 11.sp, color = Color(0xFF94A3B8), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

data class UnitDef(
    val name: String,
    val symbol: String,
    val factor: Double,
    val badge: String
)

data class UnitConversionRecord(
    val category: String,
    val sourceValue: String,
    val sourceUnit: String,
    val timestamp: String
)

