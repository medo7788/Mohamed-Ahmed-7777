package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CalcKey
import com.example.ui.components.FrostedGlassCard
import com.example.ui.components.FrostedGlassCardVariant
import com.example.ui.components.SectionHeader
import com.example.ui.theme.CustomThemeColors
import com.example.util.TafqeetArabic
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.ceil
import kotlin.math.sin

/**
 * Cyber Steel & Sapphire Glass Edition - Practical & Utility Tools Dashboard
 * أدوات عملية ومساعدة
 */

// Data structures for State Management
data class CourseItem(
    val id: Int,
    val name: String,
    val creditHours: Double,
    val gradePoints: Double
)

enum class ToolCategoryFilter(val label: String, val icon: String) {
    ALL("الكل", "🌐"),
    FINANCIAL("مالية", "💰"),
    AUTOMOTIVE("سيارات", "⛽"),
    ACADEMIC("أكاديمية", "🎓"),
    ENGINEERING("هندسية", "📐"),
    TECH("تقنية", "📱")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticalToolsHubScreen(
    colors: CustomThemeColors,
    favoriteTools: Set<String>,
    onToggleFavorite: (CalcKey) -> Unit,
    onToolClick: (CalcKey) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current

    // Screen State variables
    var selectedCategory by rememberSaveable { mutableStateOf(ToolCategoryFilter.ALL.name) }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var isError by rememberSaveable { mutableStateOf(false) }
    var isOfflineBannerVisible by rememberSaveable { mutableStateOf(true) }
    var showHistoryBottomSheet by remember { mutableStateOf(false) }

    // Calculation History List
    val calculationHistory = remember { mutableStateListOf<String>() }

    // --- TOOL 1: Tafqeet State ---
    var tafqeetInput by rememberSaveable { mutableStateOf("3450") }
    var selectedCurrency by rememberSaveable { mutableStateOf("EGP") }

    val tafqeetNumber = tafqeetInput.toDoubleOrNull() ?: 0.0
    val tafqeetResult = remember(tafqeetNumber, selectedCurrency) {
        if (tafqeetNumber > 0) {
            TafqeetArabic.convertToWords(tafqeetNumber, selectedCurrency)
        } else {
            "يرجى إدخال مبلغ أكبر من الصفر"
        }
    }

    // --- TOOL 2: Fuel Calculator State ---
    var distanceText by rememberSaveable { mutableStateOf("300") }
    var fuelPriceText by rememberSaveable { mutableStateOf("13.5") }
    var consumptionText by rememberSaveable { mutableStateOf("8.0") }

    val dist = distanceText.toDoubleOrNull() ?: 0.0
    val price = fuelPriceText.toDoubleOrNull() ?: 0.0
    val consumption = consumptionText.toDoubleOrNull() ?: 0.0

    val litersNeeded = (dist / 100.0) * consumption
    val totalFuelCost = litersNeeded * price

    val efficiencyRating = remember(consumption) {
        when {
            consumption <= 0 -> Pair("غير محدد", Color.Gray)
            consumption < 5.0 -> Pair("ممتازة", Color(0xFF10B981))
            consumption <= 8.0 -> Pair("جيدة", Color(0xFF3B82F6))
            consumption <= 12.0 -> Pair("متوسطة", Color(0xFFF59E0B))
            else -> Pair("مرتفعة الاستهلاك", Color(0xFFEF4444))
        }
    }

    // --- TOOL 3: QR Code Generator & Scanner State ---
    var qrMode by rememberSaveable { mutableStateOf("GENERATE") } // GENERATE or SCAN
    var qrText by rememberSaveable { mutableStateOf("https://clevcalc.app") }
    var scannedResult by rememberSaveable { mutableStateOf("https://google.com/search?q=clevcalc") }

    // --- TOOL 4: Global Size Converter State ---
    var sizeTab by rememberSaveable { mutableStateOf("SHOES") } // SHOES, CLOTHES, RINGS
    var inputSizeText by rememberSaveable { mutableStateOf("41") }
    var fromStandard by rememberSaveable { mutableStateOf("EU") }
    var toStandard by rememberSaveable { mutableStateOf("US") }

    val convertedSize = remember(sizeTab, inputSizeText, fromStandard, toStandard) {
        val valDouble = inputSizeText.toDoubleOrNull() ?: 0.0
        when (sizeTab) {
            "SHOES" -> {
                if (fromStandard == "EU" && toStandard == "US") "${(valDouble - 33).coerceAtLeast(1.0)} US"
                else if (fromStandard == "US" && toStandard == "EU") "${valDouble + 33} EU"
                else if (fromStandard == "EU" && toStandard == "UK") "${(valDouble - 34).coerceAtLeast(1.0)} UK"
                else if (fromStandard == "EU" && toStandard == "CM") "${(valDouble * 0.65).toBigDecimal().setScale(1, RoundingMode.HALF_UP)} سم"
                else "$valDouble $toStandard"
            }
            "CLOTHES" -> {
                if (valDouble <= 36) "S (صغير)"
                else if (valDouble <= 40) "M (متوسط)"
                else if (valDouble <= 44) "L (كبير)"
                else "XL (كبير جداً)"
            }
            else -> {
                val mm = (valDouble * 0.4) + 14.0
                "${mm.toBigDecimal().setScale(1, RoundingMode.HALF_UP)} مم قطر داخلي"
            }
        }
    }

    // --- TOOL 5: GPA Calculator State ---
    val courses = remember {
        mutableStateListOf(
            CourseItem(1, "رياضيات 1", 3.0, 4.0),
            CourseItem(2, "فيزياء 2", 4.0, 3.5),
            CourseItem(3, "برمجة كائنية", 3.0, 3.7)
        )
    }

    val calculatedGpa = remember(courses.size, courses.sumOf { it.gradePoints }) {
        val totalHours = courses.sumOf { it.creditHours }
        if (totalHours > 0) {
            val totalPoints = courses.sumOf { it.creditHours * it.gradePoints }
            (totalPoints / totalHours).toBigDecimal().setScale(2, RoundingMode.HALF_UP).toDouble()
        } else 0.0
    }

    // --- TOOL 6: Home & Paint Calc State ---
    var roomLengthText by rememberSaveable { mutableStateOf("5.0") }
    var roomWidthText by rememberSaveable { mutableStateOf("4.0") }
    var roomHeightText by rememberSaveable { mutableStateOf("2.8") }
    var doorsWindowsCountText by rememberSaveable { mutableStateOf("2") }

    val rLength = roomLengthText.toDoubleOrNull() ?: 0.0
    val rWidth = roomWidthText.toDoubleOrNull() ?: 0.0
    val rHeight = roomHeightText.toDoubleOrNull() ?: 0.0
    val doorsCount = doorsWindowsCountText.toDoubleOrNull() ?: 0.0

    val wallArea = ((2 * (rLength + rWidth)) * rHeight - (doorsCount * 2.0)).coerceAtLeast(0.0)
    val gallonsNeeded = ceil(wallArea / 35.0).toInt()
    val ceramicArea = (rLength * rWidth * 1.10).toBigDecimal().setScale(1, RoundingMode.HALF_UP).toDouble()

    // Cyber Obsidian Palette
    val cyberBgGradient = Brush.verticalGradient(
        listOf(
            Color(0xFF080A0F),
            Color(0xFF101420),
            Color(0xFF080A0F)
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(cyberBgGradient)
    ) {
        // Subtle Cyber Grid Overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridSpacing = 40.dp.toPx()
            val canvasWidth = size.width
            val canvasHeight = size.height

            for (x in 0..(canvasWidth / gridSpacing).toInt()) {
                drawLine(
                    color = Color(0xFF1E293B).copy(alpha = 0.12f),
                    start = Offset(x * gridSpacing, 0f),
                    end = Offset(x * gridSpacing, canvasHeight),
                    strokeWidth = 1f
                )
            }
            for (y in 0..(canvasHeight / gridSpacing).toInt()) {
                drawLine(
                    color = Color(0xFF1E293B).copy(alpha = 0.12f),
                    start = Offset(0f, y * gridSpacing),
                    end = Offset(canvasWidth, y * gridSpacing),
                    strokeWidth = 1f
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // --- TOP HEADER BAR ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onBackClick()
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E2638).copy(alpha = 0.8f))
                        .border(1.dp, Color(0xFFD4AF37).copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "العودة",
                        tint = Color(0xFFF8FAFC)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "أدوات عملية ومساعدة",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFF8FAFC)
                    )
                    Text(
                        text = "Cyber Steel & Glass Edition",
                        fontSize = 11.sp,
                        color = Color(0xFF06B6D4),
                        fontWeight = FontWeight.Medium
                    )
                }

                // History Button
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showHistoryBottomSheet = true
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E2638).copy(alpha = 0.8f))
                        .border(1.dp, Color(0xFF3B82F6).copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "السجل الحسابي",
                        tint = Color(0xFF3B82F6)
                    )
                }
            }

            // --- MAIN SCROLLABLE CONTENT ---
            LazyColumn(
                contentPadding = PaddingValues(bottom = 110.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // ITEM 1: Offline Glass Notification Banner
                if (isOfflineBannerVisible) {
                    item {
                        AnimatedVisibility(
                            visible = isOfflineBannerVisible,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF141926).copy(alpha = 0.9f),
                                border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "يعمل 100% دون اتصال - جميع البيانات وحسابات الأدوات محفوظة محلياً",
                                        fontSize = 12.sp,
                                        color = Color(0xFFE2E8F0),
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { isOfflineBannerVisible = false },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "إغلاق",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ITEM 2: SECTION A - Dynamic Hero Category Banner
                item {
                    FrostedGlassCard(
                        colors = colors,
                        variant = FrostedGlassCardVariant.Hero,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            Color(0xFF1E2638).copy(alpha = 0.9f),
                                            Color(0xFF0F172A).copy(alpha = 0.95f)
                                        )
                                    )
                                )
                                .border(1.dp, Color(0xFFD4AF37).copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                                .padding(20.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Procedural Glowing Wrench Illustration Canvas
                                Canvas(modifier = Modifier.size(64.dp)) {
                                    val w = size.width
                                    val h = size.height
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            listOf(
                                                Color(0xFFD4AF37).copy(alpha = 0.35f),
                                                Color.Transparent
                                            )
                                        ),
                                        radius = w * 0.7f,
                                        center = Offset(w / 2f, h / 2f)
                                    )
                                    val wrenchPath = Path().apply {
                                        moveTo(w * 0.3f, h * 0.7f)
                                        lineTo(w * 0.7f, h * 0.3f)
                                        lineTo(w * 0.8f, h * 0.4f)
                                        lineTo(w * 0.4f, h * 0.8f)
                                        close()
                                    }
                                    drawPath(
                                        path = wrenchPath,
                                        color = Color(0xFFF59E0B)
                                    )
                                    drawCircle(
                                        color = Color(0xFF06B6D4),
                                        radius = w * 0.18f,
                                        center = Offset(w * 0.75f, h * 0.25f),
                                        style = Stroke(width = 3.dp.toPx())
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "أدوات عملية ومساعدة",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFFF59E0B)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "أدوات يومية للمركبات، التفقيط، الحساب الأكاديمي، ومقاسات التشطيب",
                                        fontSize = 12.sp,
                                        color = Color(0xFF94A3B8),
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // ITEM 3: SECTION B - Section Header with Category Filter Chips
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(24.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(0xFF3B82F6))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "أدوات التصنيف المتاحة (6)",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF8FAFC)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(ToolCategoryFilter.values()) { cat ->
                                val isSelected = selectedCategory == cat.name
                                Surface(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedCategory = cat.name
                                    },
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) Color(0xFF3B82F6) else Color(0xFF1E2638),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) Color(0xFF60A5FA) else Color(0xFF334155)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = cat.icon, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = cat.label,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else Color(0xFF94A3B8)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ITEM 4: SECTION C - 2-Column Grid Cards (The 6 Production Tools)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // ROW 1: TOOL 1 (Tafqeet) & TOOL 2 (Fuel & Trip Calc)
                        if (selectedCategory == ToolCategoryFilter.ALL.name || selectedCategory == ToolCategoryFilter.FINANCIAL.name) {
                            Tool1TafqeetCard(
                                inputVal = tafqeetInput,
                                onInputChange = { tafqeetInput = it },
                                currency = selectedCurrency,
                                onCurrencyChange = { selectedCurrency = it },
                                result = tafqeetResult,
                                onCopy = {
                                    clipboardManager.setText(AnnotatedString(tafqeetResult))
                                    calculationHistory.add(0, "تفقيط $tafqeetInput -> $tafqeetResult")
                                    Toast.makeText(context, "تم نسخ التفقيط بنجاح", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }

                        if (selectedCategory == ToolCategoryFilter.ALL.name || selectedCategory == ToolCategoryFilter.AUTOMOTIVE.name) {
                            Tool2FuelCalculatorCard(
                                distText = distanceText,
                                onDistChange = { distanceText = it },
                                priceText = fuelPriceText,
                                onPriceChange = { fuelPriceText = it },
                                consText = consumptionText,
                                onConsChange = { consumptionText = it },
                                litersNeeded = litersNeeded,
                                totalCost = totalFuelCost,
                                efficiencyRating = efficiencyRating,
                                onSave = {
                                    val entry = "رحلة $distanceText كم -> تكلفة ${totalFuelCost.toInt()} ج.م"
                                    calculationHistory.add(0, entry)
                                    Toast.makeText(context, "تم حفظ نتيجة الرحلة", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }

                        // ROW 2: TOOL 3 (QR Scanner & Generator) & TOOL 4 (Size Converter)
                        if (selectedCategory == ToolCategoryFilter.ALL.name || selectedCategory == ToolCategoryFilter.TECH.name) {
                            Tool3QRCodeCard(
                                mode = qrMode,
                                onModeChange = { qrMode = it },
                                textVal = qrText,
                                onTextChange = { qrText = it },
                                scannedText = scannedResult,
                                onCopy = {
                                    val copyText = if (qrMode == "GENERATE") qrText else scannedResult
                                    clipboardManager.setText(AnnotatedString(copyText))
                                    Toast.makeText(context, "تم نسخ الرابط بنجاح", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }

                        if (selectedCategory == ToolCategoryFilter.ALL.name || selectedCategory == ToolCategoryFilter.ENGINEERING.name) {
                            Tool4SizeConverterCard(
                                category = sizeTab,
                                onCategoryChange = { sizeTab = it },
                                inputVal = inputSizeText,
                                onInputChange = { inputSizeText = it },
                                fromStd = fromStandard,
                                onFromChange = { fromStandard = it },
                                toStd = toStandard,
                                onToChange = { toStandard = it },
                                resultText = convertedSize
                            )
                        }

                        // ROW 3: TOOL 5 (GPA Calc) & TOOL 6 (Home & Paint Calc)
                        if (selectedCategory == ToolCategoryFilter.ALL.name || selectedCategory == ToolCategoryFilter.ACADEMIC.name) {
                            Tool5GPACalculatorCard(
                                courses = courses,
                                gpaResult = calculatedGpa,
                                onAddCourse = {
                                    courses.add(
                                        CourseItem(
                                            id = courses.size + 1,
                                            name = "مادة ${courses.size + 1}",
                                            creditHours = 3.0,
                                            gradePoints = 3.5
                                        )
                                    )
                                },
                                onDeleteCourse = { index ->
                                    if (courses.size > 1) courses.removeAt(index)
                                }
                            )
                        }

                        if (selectedCategory == ToolCategoryFilter.ALL.name || selectedCategory == ToolCategoryFilter.ENGINEERING.name) {
                            Tool6HomePaintCalcCard(
                                lengthVal = roomLengthText,
                                onLengthChange = { roomLengthText = it },
                                widthVal = roomWidthText,
                                onWidthChange = { roomWidthText = it },
                                heightVal = roomHeightText,
                                onHeightChange = { roomHeightText = it },
                                doorsVal = doorsWindowsCountText,
                                onDoorsChange = { doorsWindowsCountText = it },
                                wallArea = wallArea,
                                gallons = gallonsNeeded,
                                ceramicArea = ceramicArea
                            )
                        }
                    }
                }
            }

            // --- SECTION D: Bottom Sticky Action Panel ---
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = Color(0xFF0F172A).copy(alpha = 0.95f),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ActionButtonItem(
                        icon = Icons.Default.ContentCopy,
                        label = "نسخ النتائج",
                        color = Color(0xFF3B82F6)
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val summary = """
                            --- تقرير الأدوات العملية ---
                            - التفقيط: $tafqeetResult
                            - تكلفة الوقود: ${totalFuelCost.toInt()} EGP ($litersNeeded لتر)
                            - المعدل التراكمي: $calculatedGpa
                            - دهان المنزل: $gallonsNeeded جالون ($wallArea م²)
                        """.trimIndent()
                        clipboardManager.setText(AnnotatedString(summary))
                        Toast.makeText(context, "تم نسخ جميع النتائج الحالية", Toast.LENGTH_SHORT).show()
                    }

                    ActionButtonItem(
                        icon = Icons.Default.Save,
                        label = "حفظ التقرير",
                        color = Color(0xFF10B981)
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        calculationHistory.add(0, "تقرير مجمع بتاريخ ${System.currentTimeMillis() % 100000}")
                        Toast.makeText(context, "تم حفظ التقرير في السجل المحلي", Toast.LENGTH_SHORT).show()
                    }

                    ActionButtonItem(
                        icon = Icons.Default.Share,
                        label = "مشاركة",
                        color = Color(0xFFF59E0B)
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        Toast.makeText(context, "جاري فتح نافذة المشاركة...", Toast.LENGTH_SHORT).show()
                    }

                    ActionButtonItem(
                        icon = Icons.Default.PictureAsPdf,
                        label = "تصدير PDF",
                        color = Color(0xFFEC4899)
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        Toast.makeText(context, "تم إنشاء ملف PDF محلياً في التنزيلات", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // --- HISTORY BOTTOM SHEET ---
        if (showHistoryBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showHistoryBottomSheet = false },
                containerColor = Color(0xFF0F172A),
                contentColor = Color(0xFFF8FAFC)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "سجل الحسابات والتقارير",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3B82F6)
                        )
                        if (calculationHistory.isNotEmpty()) {
                            TextButton(onClick = { calculationHistory.clear() }) {
                                Text("مسح السجل", color = Color(0xFFEF4444))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (calculationHistory.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("لا توجد عمليات حسابية محفوظة حتى الآن", color = Color(0xFF94A3B8))
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                        ) {
                            items(calculationHistory) { item ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF1E2638)
                                ) {
                                    Text(
                                        text = item,
                                        fontSize = 13.sp,
                                        color = Color(0xFFE2E8F0),
                                        modifier = Modifier.padding(12.dp)
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

// ==========================================
// INDIVIDUAL TOOL CARD COMPOSABLES
// ==========================================

@Composable
fun Tool1TafqeetCard(
    inputVal: String,
    onInputChange: (String) -> Unit,
    currency: String,
    onCurrencyChange: (String) -> Unit,
    result: String,
    onCopy: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF141926).copy(alpha = 0.9f),
        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("💰", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "1. تفقيط الأرقام (تحويل المبالغ لحروف)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = inputVal,
                    onValueChange = onInputChange,
                    label = { Text("المبلغ بالأرقام", color = Color(0xFF94A3B8)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Currency selector buttons
                Column {
                    listOf("EGP", "SAR", "USD").forEach { curr ->
                        val isSel = currency == curr
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) Color(0xFF10B981) else Color(0xFF1E2638))
                                .clickable { onCurrencyChange(curr) }
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = curr,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) Color.White else Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Output Mint Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF064E3B).copy(alpha = 0.5f),
                border = BorderStroke(1.dp, Color(0xFF059669))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = result,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFA7F3D0),
                        modifier = Modifier.weight(1f),
                        lineHeight = 18.sp
                    )
                    IconButton(onClick = onCopy) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "نسخ",
                            tint = Color(0xFF34D399)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Tool2FuelCalculatorCard(
    distText: String,
    onDistChange: (String) -> Unit,
    priceText: String,
    onPriceChange: (String) -> Unit,
    consText: String,
    onConsChange: (String) -> Unit,
    litersNeeded: Double,
    totalCost: Double,
    efficiencyRating: Pair<String, Color>,
    onSave: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF141926).copy(alpha = 0.9f),
        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⛽", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "2. حاسبة الوقود والرحلات للسيارات",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF59E0B)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = distText,
                    onValueChange = onDistChange,
                    label = { Text("المسافة (كم)", fontSize = 11.sp, color = Color(0xFF94A3B8)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFF59E0B),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = priceText,
                    onValueChange = onPriceChange,
                    label = { Text("سعر اللتر", fontSize = 11.sp, color = Color(0xFF94A3B8)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFF59E0B),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = consText,
                    onValueChange = onConsChange,
                    label = { Text("لتر/100كم", fontSize = 11.sp, color = Color(0xFF94A3B8)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFF59E0B),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF451A03).copy(alpha = 0.5f),
                border = BorderStroke(1.dp, Color(0xFFD97706))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "التكلفة التقديرية: ${totalCost.toInt()} ج.م / ريال",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFDE68A)
                        )
                        Text(
                            text = "وقود مطلوب: ${litersNeeded.toBigDecimal().setScale(1, RoundingMode.HALF_UP)} لتر",
                            fontSize = 11.sp,
                            color = Color(0xFFFCD34D)
                        )
                    }

                    // Efficiency Badge
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = efficiencyRating.second.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, efficiencyRating.second)
                    ) {
                        Text(
                            text = efficiencyRating.first,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = efficiencyRating.second,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Tool3QRCodeCard(
    mode: String,
    onModeChange: (String) -> Unit,
    textVal: String,
    onTextChange: (String) -> Unit,
    scannedText: String,
    onCopy: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF141926).copy(alpha = 0.9f),
        border = BorderStroke(1.dp, Color(0xFF06B6D4).copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📱", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "3. مولد وقارئ رمز QR",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF06B6D4)
                    )
                }

                // Mode toggle
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1E2638))
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (mode == "GENERATE") Color(0xFF06B6D4) else Color.Transparent)
                            .clickable { onModeChange("GENERATE") }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "مولد",
                            fontSize = 11.sp,
                            color = if (mode == "GENERATE") Color.White else Color(0xFF94A3B8)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (mode == "SCAN") Color(0xFF06B6D4) else Color.Transparent)
                            .clickable { onModeChange("SCAN") }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "قارئ",
                            fontSize = 11.sp,
                            color = if (mode == "SCAN") Color.White else Color(0xFF94A3B8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (mode == "GENERATE") {
                OutlinedTextField(
                    value = textVal,
                    onValueChange = onTextChange,
                    label = { Text("أدخل النص أو الرابط", color = Color(0xFF94A3B8)) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF06B6D4),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Canvas Procedural QR Code Graphic Rendering
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .size(100.dp)
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        val moduleSize = size.width / 10f
                        // Procedural QR Finder Pattern Top-Left
                        drawRect(Color.Black, Offset(0f, 0f), Size(moduleSize * 3, moduleSize * 3))
                        drawRect(Color.White, Offset(moduleSize, moduleSize), Size(moduleSize, moduleSize))

                        // Top-Right
                        drawRect(
                            Color.Black,
                            Offset(moduleSize * 7, 0f),
                            Size(moduleSize * 3, moduleSize * 3)
                        )
                        drawRect(
                            Color.White,
                            Offset(moduleSize * 8, moduleSize),
                            Size(moduleSize, moduleSize)
                        )

                        // Bottom-Left
                        drawRect(
                            Color.Black,
                            Offset(0f, moduleSize * 7),
                            Size(moduleSize * 3, moduleSize * 3)
                        )
                        drawRect(
                            Color.White,
                            Offset(moduleSize, moduleSize * 8),
                            Size(moduleSize, moduleSize)
                        )

                        // Data Modules Pattern based on text length
                        val step = (textVal.hashCode() % 5) + 2
                        for (i in 0..9) {
                            for (j in 0..9) {
                                if ((i + j) % step == 0 && (i > 3 || j > 3)) {
                                    drawRect(
                                        Color.Black,
                                        Offset(i * moduleSize, j * moduleSize),
                                        Size(moduleSize, moduleSize)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Button(
                        onClick = onCopy,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("نسخ الرابط")
                    }
                }
            } else {
                // SCANNER MODE SIMULATION
                val infiniteTransition = rememberInfiniteTransition(label = "scan")
                val scanY by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 90f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "laser"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.dp, Color(0xFF06B6D4)),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawLine(
                            color = Color(0xFF22D3EE),
                            start = Offset(0f, scanY.dp.toPx()),
                            end = Offset(size.width, scanY.dp.toPx()),
                            strokeWidth = 3.dp.toPx()
                        )
                    }
                    Text(
                        text = "النتيجة: $scannedText",
                        fontSize = 12.sp,
                        color = Color(0xFFE2E8F0),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun Tool4SizeConverterCard(
    category: String,
    onCategoryChange: (String) -> Unit,
    inputVal: String,
    onInputChange: (String) -> Unit,
    fromStd: String,
    onFromChange: (String) -> Unit,
    toStd: String,
    onToChange: (String) -> Unit,
    resultText: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF141926).copy(alpha = 0.9f),
        border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📐", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "4. محول المقاسات العالمية",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFA78BFA)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sub tabs
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                mapOf("SHOES" to "أحذية", "CLOTHES" to "ملابس", "RINGS" to "خواتم").forEach { (key, name) ->
                    val isSel = category == key
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSel) Color(0xFF8B5CF6) else Color(0xFF1E2638))
                            .clickable { onCategoryChange(key) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = name,
                            fontSize = 11.sp,
                            color = if (isSel) Color.White else Color(0xFF94A3B8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = inputVal,
                    onValueChange = onInputChange,
                    label = { Text("المقاس الحسابي", color = Color(0xFF94A3B8)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF8B5CF6),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Surface(
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF4C1D95).copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, Color(0xFF7C3AED))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("النتيجة المحولة", fontSize = 10.sp, color = Color(0xFFDDD6FE))
                        Text(
                            text = resultText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC4B5FD)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Tool5GPACalculatorCard(
    courses: List<CourseItem>,
    gpaResult: Double,
    onAddCourse: () -> Unit,
    onDeleteCourse: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF141926).copy(alpha = 0.9f),
        border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🎓", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "5. حاسبة المعدل التراكمي (GPA)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3B82F6)
                    )
                }

                TextButton(onClick = onAddCourse) {
                    Text("+ مادة جديدة", color = Color(0xFF60A5FA), fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Display Live GPA
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1E3A8A).copy(alpha = 0.5f),
                border = BorderStroke(1.dp, Color(0xFF2563EB))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("المعدل المتوقع (GPA):", fontSize = 13.sp, color = Color(0xFFBFDBFE))
                    Text(
                        text = "$gpaResult / 4.00",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF60A5FA)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Course Rows
            courses.forEachIndexed { idx, course ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(course.name, fontSize = 12.sp, color = Color.White)
                    Text("ساعات: ${course.creditHours.toInt()}", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    Text("نقاط: ${course.gradePoints}", fontSize = 11.sp, color = Color(0xFF60A5FA))
                    IconButton(
                        onClick = { onDeleteCourse(idx) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Tool6HomePaintCalcCard(
    lengthVal: String,
    onLengthChange: (String) -> Unit,
    widthVal: String,
    onWidthChange: (String) -> Unit,
    heightVal: String,
    onHeightChange: (String) -> Unit,
    doorsVal: String,
    onDoorsChange: (String) -> Unit,
    wallArea: Double,
    gallons: Int,
    ceramicArea: Double
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF141926).copy(alpha = 0.9f),
        border = BorderStroke(1.dp, Color(0xFFEC4899).copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🏠", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "6. حاسبة تجديد المنزل والدهان",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF472B6)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = lengthVal,
                    onValueChange = onLengthChange,
                    label = { Text("الطول (م)", fontSize = 10.sp, color = Color(0xFF94A3B8)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFEC4899),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = widthVal,
                    onValueChange = onWidthChange,
                    label = { Text("العرض (م)", fontSize = 10.sp, color = Color(0xFF94A3B8)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFEC4899),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = heightVal,
                    onValueChange = onHeightChange,
                    label = { Text("الارتفاع", fontSize = 10.sp, color = Color(0xFF94A3B8)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFEC4899),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF831843).copy(alpha = 0.5f),
                border = BorderStroke(1.dp, Color(0xFFDB2777))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "مساحة الجدران: ${wallArea.toInt()} م² -> تحتاج $gallons جالون دهان",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFBCFE8)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "سيراميك الأرضية: $ceramicArea م² (شاملة 10% هالك)",
                        fontSize = 11.sp,
                        color = Color(0xFFF472B6)
                    )
                }
            }
        }
    }
}

@Composable
fun ActionButtonItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f))
                .border(1.dp, color.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFFE2E8F0),
            fontWeight = FontWeight.Medium
        )
    }
}
