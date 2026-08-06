package com.example.ui.screens

import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
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
import com.example.ui.components.ToolScreenScaffold
import com.example.ui.theme.AppIcons
import com.example.ui.theme.CustomThemeColors
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

// ==========================================
// LUXURY CYBER OBSIDIAN PALETTE & MODELS
// ==========================================

private val ColorObsidianBgStart = Color(0xFF080A0F)
private val ColorObsidianBgEnd = Color(0xFF121620)
private val ColorGlassCard = Color(0xFF141926).copy(alpha = 0.85f)
private val ColorGoldBorder = Color(0xFFD4AF37)
private val ColorAmberGlow = Color(0xFFF59E0B)
private val ColorIceCyan = Color(0xFF00F2FE)
private val ColorEmeraldGreen = Color(0xFF10B981)
private val ColorCrimsonRed = Color(0xFFEF4444)
private val ColorSlateMuted = Color(0xFF94A3B8)

enum class PercentOperationMode(
    val titleAr: String,
    val shortDesc: String,
    val xLabel: String,
    val yLabel: String,
    val formulaText: String
) {
    VALUE_OF_PERCENT(
        titleAr = "كم يساوي X% من Y؟",
        shortDesc = "حساب قيمة النسبة المئوية من مبلغ",
        xLabel = "النسبة المئوية (%)",
        yLabel = "المبلغ الإجمالي (Y)",
        formulaText = "النتيجة = Y × (X ÷ 100)"
    ),
    PERCENT_SHARE(
        titleAr = "ما هي نسبة X من Y؟",
        shortDesc = "حساب الحصة المئوية لجزء من الكل",
        xLabel = "الجزء (X)",
        yLabel = "الكل الإجمالي (Y)",
        formulaText = "النسبة = (X ÷ Y) × 100"
    ),
    PERCENT_INCREASE(
        titleAr = "زيادة بـ %",
        shortDesc = "إضافة نسبة مئوية إلى مبلغ (السعر بعد الزيادة/الضريبة)",
        xLabel = "نسبة الزيادة (%)",
        yLabel = "المبلغ الأصلي (Y)",
        formulaText = "المبلغ الجديد = Y × (1 + X ÷ 100)"
    ),
    PERCENT_DECREASE(
        titleAr = "خصم بـ %",
        shortDesc = "طرح نسبة مئوية من مبلغ (السعر بعد الخصم)",
        xLabel = "نسبة الخصم (%)",
        yLabel = "السعر الأصلي (Y)",
        formulaText = "السعر بعد الخصم = Y × (1 - X ÷ 100)"
    ),
    PERCENT_CHANGE(
        titleAr = "نسبة التغير بين رقمين",
        shortDesc = "حساب معدل النمو أو الانخفاض المئوي بين قيمتين",
        xLabel = "القيمة القديمة (X)",
        yLabel = "القيمة الجديدة (Y)",
        formulaText = "نسبة التغير = ((Y - X) ÷ X) × 100"
    ),
    REVERSE_BASE(
        titleAr = "المبلغ الأصلي قبل الخصم",
        shortDesc = "استرجاع المبلغ الأساسي بمعرفة السعر النهائي والنسبة",
        xLabel = "نسبة الخصم (%)",
        yLabel = "المبلغ النهائي المدفوع (Y)",
        formulaText = "المبلغ الأصلي = Y ÷ (1 - X ÷ 100)"
    ),
    MARGIN_MARKUP(
        titleAr = "هامش الربح والـ Markup",
        shortDesc = "مقارنة Gross Margin % مع Markup % للتكلفة وسعر البيع",
        xLabel = "التكلفة (X)",
        yLabel = "سعر البيع / الإيراد (Y)",
        formulaText = "Margin % = ((Y - X) ÷ Y) × 100 | Markup % = ((Y - X) ÷ X) × 100"
    )
}

data class PercentCalcResult(
    val primaryResultStr: String,
    val primaryLabel: String,
    val secondaryResultStr: String,
    val secondaryLabel: String,
    val isPositiveChange: Boolean?,
    val changePercentVal: BigDecimal,
    val differenceAmountStr: String,
    val visualPercentage: Float,
    val breakdownSteps: List<String>,
    val isValid: Boolean,
    val errorMessage: String? = null
)

data class PercentHistoryItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: String,
    val modeName: String,
    val modeTitle: String,
    val valX: String,
    val valY: String,
    val primaryResult: String,
    val secondaryResult: String,
    val currency: String
)

// ==========================================
// MAIN COMPOSABLE SCREEN
// ==========================================

@Composable
fun PercentageCalcScreen(colors: CustomThemeColors) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current

    // State Persistence
    var activeMode by rememberSaveable { mutableStateOf(PercentOperationMode.VALUE_OF_PERCENT) }
    var valX by rememberSaveable { mutableStateOf("20") }
    var valY by rememberSaveable { mutableStateOf("250") }
    var selectedCurrency by rememberSaveable { mutableStateOf("EGP") }

    // Swap Rotation Animation State
    var swapRotationAngle by remember { mutableFloatStateOf(0f) }
    val animatedRotation by animateFloatAsState(
        targetValue = swapRotationAngle,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "swapRotation"
    )

    // Expandable Sections State
    var isExplanationExpanded by rememberSaveable { mutableStateOf(true) }
    var isFavoritesExpanded by rememberSaveable { mutableStateOf(false) }
    var isHistoryExpanded by rememberSaveable { mutableStateOf(false) }

    // Toast Feedback Message
    var toastMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2500)
            toastMessage = null
        }
    }

    // Local Storage Data
    var historyList by remember { mutableStateOf(loadPercentHistoryFromPrefs(context)) }
    var favoritesList by remember { mutableStateOf(loadPercentFavoritesFromPrefs(context)) }

    // Derived Calculation Engine with BigDecimal Precision
    val calculationResult by remember(activeMode, valX, valY, selectedCurrency) {
        derivedStateOf { calculatePercentEngine(activeMode, valX, valY, selectedCurrency) }
    }

    // Error shaking animation offset
    val shakeOffset by animateFloatAsState(
        targetValue = if (!calculationResult.isValid) 10f else 0f,
        animationSpec = repeatable(
            iterations = 4,
            animation = tween(durationMillis = 50, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shakeOffset"
    )

    ToolScreenScaffold(
        colors = colors,
        icon = AppIcons.forCalc(CalcKey.PERCENT),
        title = "النسبة المئوية الذكية",
        subtitle = "إجراء كافة عمليات النسب المئوية، التغير المالي وهامش الربح بدقة متناهية",
        isScrollable = false
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(ColorObsidianBgStart, ColorObsidianBgEnd)
                    )
                )
        ) {
            // Background Canvas Grid Pattern with % Symbols
            Canvas(modifier = Modifier.fillMaxSize()) {
                val step = 70.dp.toPx()
                val linePaint = ColorGoldBorder.copy(alpha = 0.025f)
                for (x in 0..size.width.toInt() step step.toInt()) {
                    drawLine(linePaint, Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height), strokeWidth = 1f)
                }
                for (y in 0..size.height.toInt() step step.toInt()) {
                    drawLine(linePaint, Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), strokeWidth = 1f)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Toast Feedback Banner
                if (toastMessage != null) {
                    item(key = "toast_banner") {
                        Surface(
                            color = ColorEmeraldGreen,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(8.dp, RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(toastMessage!!, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }

                // SECTION A1: Header Display Card
                item(key = "header_card") {
                    HeaderCard(
                        selectedCurrency = selectedCurrency,
                        onCurrencySelected = {
                            selectedCurrency = it
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    )
                }

                // SECTION A2: Operation Mode Selector Chips Row
                item(key = "mode_selector_row") {
                    ModeSelectorRow(
                        activeMode = activeMode,
                        onModeSelected = { mode ->
                            activeMode = mode
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    )
                }

                // SECTION B1: Interactive Inputs Panel with Swap Axis & Quick Presets
                item(key = "inputs_panel") {
                    Box(modifier = Modifier.offset(x = shakeOffset.dp)) {
                        InputsPanel(
                            activeMode = activeMode,
                            valX = valX,
                            onValXChange = { valX = it },
                            valY = valY,
                            onValYChange = { valY = it },
                            rotationAngle = animatedRotation,
                            onSwap = {
                                val temp = valX
                                valX = valY
                                valY = temp
                                swapRotationAngle += 180f
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                toastMessage = "تم تبديل القيمتين X و Y"
                            },
                            onApplyXPreset = { preset ->
                                valX = preset
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        )
                    }
                }

                // SECTION B2: Live Result Display Panel & Visual Percentage Ring
                item(key = "result_display_panel") {
                    ResultDisplayPanel(
                        result = calculationResult,
                        activeMode = activeMode,
                        currency = selectedCurrency,
                        onSaveHistory = {
                            if (!calculationResult.isValid) return@ResultDisplayPanel
                            val timeStr = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm - yyyy/MM/dd"))
                            val newItem = PercentHistoryItem(
                                timestamp = timeStr,
                                modeName = activeMode.name,
                                modeTitle = activeMode.titleAr,
                                valX = valX,
                                valY = valY,
                                primaryResult = calculationResult.primaryResultStr,
                                secondaryResult = calculationResult.secondaryResultStr,
                                currency = selectedCurrency
                            )
                            historyList = listOf(newItem) + historyList.take(19)
                            savePercentHistoryToPrefs(context, historyList)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            toastMessage = "تم حفظ العملية في السجل"
                        },
                        onSaveFavorite = {
                            if (!calculationResult.isValid) return@ResultDisplayPanel
                            val timeStr = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm - yyyy/MM/dd"))
                            val newItem = PercentHistoryItem(
                                timestamp = timeStr,
                                modeName = activeMode.name,
                                modeTitle = activeMode.titleAr,
                                valX = valX,
                                valY = valY,
                                primaryResult = calculationResult.primaryResultStr,
                                secondaryResult = calculationResult.secondaryResultStr,
                                currency = selectedCurrency
                            )
                            favoritesList = listOf(newItem) + favoritesList
                            savePercentFavoritesToPrefs(context, favoritesList)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            toastMessage = "تمت إضافة العملية إلى المفضلة ⭐"
                        },
                        onCopyReport = {
                            val shareText = "📊 تقرير النسبة المئوية الذكية (${activeMode.titleAr}):\n" +
                                    "• المعطيات: X = $valX | Y = $valY\n" +
                                    "• ${calculationResult.primaryLabel}: ${calculationResult.primaryResultStr}\n" +
                                    "• ${calculationResult.secondaryLabel}: ${calculationResult.secondaryResultStr}\n" +
                                    "📐 القانون: ${activeMode.formulaText}\n" +
                                    "محسوب عبر حاسبة النسبة المئوية الذكية"
                            clipboardManager.setText(AnnotatedString(shareText))
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            toastMessage = "تم نسخ التقرير للحافظة 📋"
                        },
                        onShare = {
                            val shareText = "📊 نتيجة النسبة المئوية (${activeMode.titleAr}):\n" +
                                    "X = $valX, Y = $valY\n" +
                                    "النتيجة: ${calculationResult.primaryResultStr} ($selectedCurrency)\n" +
                                    "القانون: ${activeMode.formulaText}"
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(intent, "مشاركة نتيجة الحساب"))
                        },
                        onReset = {
                            valX = "20"
                            valY = "250"
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            toastMessage = "تم إعادة ضبط المدخلات"
                        }
                    )
                }

                // SECTION C: Step-by-Step Educational & Math Explanation
                item(key = "educational_explanation") {
                    ExpandableCard(
                        title = "خطوات الحل الرياضي والقانون المستخدم",
                        icon = Icons.Outlined.School,
                        isExpanded = isExplanationExpanded,
                        onToggle = { isExplanationExpanded = !isExplanationExpanded }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, ColorGoldBorder.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("📐 القانون المستعمل:", color = ColorAmberGlow, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(activeMode.formulaText, color = Color.White, fontSize = 12.sp)
                                }
                            }

                            calculationResult.breakdownSteps.forEach { step ->
                                Text("• $step", color = ColorSlateMuted, fontSize = 12.sp)
                            }

                            if (calculationResult.differenceAmountStr.isNotEmpty()) {
                                Text(
                                    "• الفرق المطلق الصافي = ${calculationResult.differenceAmountStr} $selectedCurrency",
                                    color = ColorEmeraldGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // SECTION D: Favorites List Section
                if (favoritesList.isNotEmpty()) {
                    item(key = "favorites_section") {
                        ExpandableCard(
                            title = "العمليات المفضلة (${favoritesList.size})",
                            icon = Icons.Outlined.Star,
                            isExpanded = isFavoritesExpanded,
                            onToggle = { isFavoritesExpanded = !isFavoritesExpanded }
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                favoritesList.forEach { fav ->
                                    HistoryItemRow(
                                        item = fav,
                                        onReuse = {
                                            valX = fav.valX
                                            valY = fav.valY
                                            selectedCurrency = fav.currency
                                            activeMode = PercentOperationMode.values().firstOrNull { it.name == fav.modeName } ?: activeMode
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            toastMessage = "تم تطبيق بيانات المفضلة"
                                        },
                                        onDelete = {
                                            favoritesList = favoritesList.filter { it.id != fav.id }
                                            savePercentFavoritesToPrefs(context, favoritesList)
                                            toastMessage = "تم حذف العنصر من المفضلة"
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // SECTION E: History Log Section
                if (historyList.isNotEmpty()) {
                    item(key = "history_section") {
                        ExpandableCard(
                            title = "سجل العمليات السابقة (${historyList.size})",
                            icon = Icons.Outlined.History,
                            isExpanded = isHistoryExpanded,
                            onToggle = { isHistoryExpanded = !isHistoryExpanded }
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                historyList.forEach { hist ->
                                    HistoryItemRow(
                                        item = hist,
                                        onReuse = {
                                            valX = hist.valX
                                            valY = hist.valY
                                            selectedCurrency = hist.currency
                                            activeMode = PercentOperationMode.values().firstOrNull { it.name == hist.modeName } ?: activeMode
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            toastMessage = "تم استرجاع العملية السابقة"
                                        },
                                        onDelete = {
                                            historyList = historyList.filter { it.id != hist.id }
                                            savePercentHistoryToPrefs(context, historyList)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Offline Notice Footer
                item(key = "offline_footer") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = ColorEmeraldGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("جميع الحسابات تعمل دون اتصال بمحرك BigDecimal بدقة متناهية", fontSize = 11.sp, color = ColorSlateMuted)
                    }
                }
            }
        }
    }
}

// ==========================================
// SUB-COMPONENTS
// ==========================================

@Composable
private fun HeaderCard(
    selectedCurrency: String,
    onCurrencySelected: (String) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Surface(
        color = ColorGlassCard,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, ColorGoldBorder.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Procedural Canvas Percent Vault Icon with Pulse Glow
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(ColorAmberGlow.copy(alpha = 0.35f), Color.Transparent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(36.dp)) {
                    val w = size.width
                    val h = size.height

                    // Percent slash line
                    drawLine(
                        color = ColorGoldBorder,
                        start = Offset(w * 0.25f, h * 0.75f),
                        end = Offset(w * 0.75f, h * 0.25f),
                        strokeWidth = 3.5f,
                        cap = StrokeCap.Round
                    )
                    // Top-Left Circle
                    drawCircle(ColorIceCyan, radius = 5f, center = Offset(w * 0.35f, h * 0.35f))
                    // Bottom-Right Circle
                    drawCircle(ColorIceCyan, radius = 5f, center = Offset(w * 0.65f, h * 0.65f))
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "النسبة المئوية الذكية",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "حساب النسب المئوية، التغير، وهامش الربح",
                    fontSize = 11.sp,
                    color = ColorSlateMuted
                )
            }

            // Currency Selector Chips
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("EGP", "SAR", "USD", "EUR").forEach { curr ->
                    val isSelected = curr == selectedCurrency
                    Surface(
                        color = if (isSelected) ColorAmberGlow.copy(alpha = 0.25f) else Color(0xFF1E2638),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (isSelected) ColorAmberGlow else Color.White.copy(alpha = 0.15f)),
                        modifier = Modifier.clickable { onCurrencySelected(curr) }
                    ) {
                        Text(
                            curr,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) ColorAmberGlow else ColorSlateMuted,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeSelectorRow(
    activeMode: PercentOperationMode,
    onModeSelected: (PercentOperationMode) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(PercentOperationMode.values(), key = { it.name }) { mode ->
            val isSelected = mode == activeMode
            val bg = if (isSelected) ColorAmberGlow.copy(alpha = 0.25f) else ColorGlassCard
            val borderCol = if (isSelected) ColorAmberGlow else Color.White.copy(alpha = 0.1f)

            Surface(
                color = bg,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, borderCol),
                modifier = Modifier.clickable { onModeSelected(mode) }
            ) {
                Text(
                    mode.titleAr,
                    fontSize = 12.sp,
                    color = if (isSelected) Color.White else ColorSlateMuted,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                )
            }
        }
    }
}

@Composable
private fun InputsPanel(
    activeMode: PercentOperationMode,
    valX: String,
    onValXChange: (String) -> Unit,
    valY: String,
    onValYChange: (String) -> Unit,
    rotationAngle: Float,
    onSwap: () -> Unit,
    onApplyXPreset: (String) -> Unit
) {
    Surface(
        color = ColorGlassCard,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, ColorGoldBorder.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                activeMode.shortDesc,
                fontSize = 12.sp,
                color = ColorAmberGlow,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // X Input Field
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(activeMode.xLabel, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                    OutlinedTextField(
                        value = valX,
                        onValueChange = onValXChange,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            if (valX.isNotEmpty()) {
                                IconButton(onClick = { onValXChange("") }) {
                                    Icon(Icons.Filled.Close, contentDescription = "مسح", tint = ColorSlateMuted, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ColorAmberGlow,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                // Swap Axis Button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .rotate(rotationAngle)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(ColorGoldBorder.copy(alpha = 0.3f), Color.Transparent)
                            )
                        )
                        .border(1.dp, ColorGoldBorder, CircleShape)
                        .clickable { onSwap() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.SwapHoriz, contentDescription = "تبديل القيم", tint = ColorGoldBorder)
                }

                // Y Input Field
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(activeMode.yLabel, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                    OutlinedTextField(
                        value = valY,
                        onValueChange = onValYChange,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            if (valY.isNotEmpty()) {
                                IconButton(onClick = { onValYChange("") }) {
                                    Icon(Icons.Filled.Close, contentDescription = "مسح", tint = ColorSlateMuted, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ColorIceCyan,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Percentage Presets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("نسب سريعة:", fontSize = 11.sp, color = ColorSlateMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("5", "10", "15", "20", "25", "50", "75").forEach { preset ->
                        Surface(
                            color = Color(0xFF1E2638),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                            modifier = Modifier.clickable { onApplyXPreset(preset) }
                        ) {
                            Text(
                                "$preset%",
                                fontSize = 10.sp,
                                color = ColorIceCyan,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultDisplayPanel(
    result: PercentCalcResult,
    activeMode: PercentOperationMode,
    currency: String,
    onSaveHistory: () -> Unit,
    onSaveFavorite: () -> Unit,
    onCopyReport: () -> Unit,
    onShare: () -> Unit,
    onReset: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Surface(
        color = ColorGlassCard,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.5.dp, ColorGoldBorder.copy(alpha = glowAlpha)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!result.isValid) {
                // Error State Box
                Surface(
                    color = ColorCrimsonRed.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, ColorCrimsonRed)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = ColorCrimsonRed)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            result.errorMessage ?: "خطأ في المدخلات",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                // Primary Result Big Card
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(result.primaryLabel, fontSize = 12.sp, color = ColorSlateMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        result.primaryResultStr,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = ColorAmberGlow
                    )
                }

                // Secondary Result Badge & Variation Arrow
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(result.secondaryLabel, fontSize = 11.sp, color = ColorSlateMuted)
                        Text(
                            result.secondaryResultStr,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorIceCyan
                        )
                    }

                    if (result.isPositiveChange != null) {
                        val changeColor = if (result.isPositiveChange) ColorEmeraldGreen else ColorCrimsonRed
                        val changeIcon = if (result.isPositiveChange) "▲" else "▼"

                        Surface(
                            color = changeColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, changeColor)
                        ) {
                            Text(
                                "$changeIcon ${result.changePercentVal.toPlainString()}%",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = changeColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Visual Percentage Ring (Procedural Canvas)
                VisualPercentageRing(
                    percentage = result.visualPercentage,
                    isPositive = result.isPositiveChange ?: true
                )

                // Quick Action Toolbar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onCopyReport,
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF1E2638), RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "نسخ التقرير", tint = ColorIceCyan)
                    }

                    IconButton(
                        onClick = onShare,
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF1E2638), RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = "مشاركة", tint = ColorIceCyan)
                    }

                    IconButton(
                        onClick = onSaveFavorite,
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF1E2638), RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Filled.Star, contentDescription = "المفضلة", tint = ColorAmberGlow)
                    }

                    IconButton(
                        onClick = onSaveHistory,
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF1E2638), RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = "حفظ السجل", tint = ColorEmeraldGreen)
                    }

                    IconButton(
                        onClick = onReset,
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF1E2638), RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "إعادة ضبط", tint = ColorSlateMuted)
                    }
                }
            }
        }
    }
}

@Composable
private fun VisualPercentageRing(
    percentage: Float,
    isPositive: Boolean
) {
    val animatedSweep by animateFloatAsState(
        targetValue = (percentage / 100f).coerceIn(0f, 1f) * 360f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "sweepAngle"
    )

    Box(
        modifier = Modifier
            .size(90.dp)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeW = 10.dp.toPx()
            val arcSize = Size(size.width - strokeW, size.height - strokeW)
            val topLeft = Offset(strokeW / 2, strokeW / 2)

            // Background Circle Track
            drawArc(
                color = Color(0xFF1E2638),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )

            // Animated Active Arc
            drawArc(
                color = if (isPositive) ColorAmberGlow else ColorCrimsonRed,
                startAngle = -90f,
                sweepAngle = animatedSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )
        }

        Text(
            "${percentage.toInt()}%",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun ExpandableCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        color = ColorGlassCard,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, ColorGoldBorder.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = ColorAmberGlow, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Icon(
                    if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = ColorSlateMuted
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun HistoryItemRow(
    item: PercentHistoryItem,
    onReuse: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = Color.Black.copy(alpha = 0.3f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.modeTitle, color = ColorAmberGlow, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("X = ${item.valX} | Y = ${item.valY}  ←  النتيجة: ${item.primaryResult}", color = Color.White, fontSize = 11.sp)
                Text(item.timestamp, color = ColorSlateMuted, fontSize = 9.sp)
            }

            Row {
                IconButton(onClick = onReuse) {
                    Icon(Icons.Filled.Input, contentDescription = "تطبيق", tint = ColorIceCyan, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "حذف", tint = ColorCrimsonRed, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// ==========================================
// MATHEMATICAL ENGINE (BIGDECIMAL)
// ==========================================

private fun calculatePercentEngine(
    mode: PercentOperationMode,
    xStr: String,
    yStr: String,
    currency: String
): PercentCalcResult {
    val xVal = xStr.toBigDecimalOrNull()
    val yVal = yStr.toBigDecimalOrNull()

    if (xVal == null || yVal == null) {
        return PercentCalcResult(
            primaryResultStr = "--",
            primaryLabel = "النتيجة",
            secondaryResultStr = "--",
            secondaryLabel = "النتيجة الثانوية",
            isPositiveChange = null,
            changePercentVal = BigDecimal.ZERO,
            differenceAmountStr = "",
            visualPercentage = 0f,
            breakdownSteps = emptyList(),
            isValid = false,
            errorMessage = "يرجى إدخال أرقام صحيحة في كلا الحقلين"
        )
    }

    try {
        return when (mode) {
            PercentOperationMode.VALUE_OF_PERCENT -> {
                // Primary = Y * (X / 100)
                val primary = yVal.multiply(xVal).divide(BigDecimal("100"), 4, RoundingMode.HALF_UP)
                val secondary = yVal.subtract(primary)
                val visPct = xVal.toFloat().coerceIn(0f, 100f)

                PercentCalcResult(
                    primaryResultStr = "${primary.setScale(2, RoundingMode.HALF_UP).toPlainString()} $currency",
                    primaryLabel = "قيمة $xVal% من $yVal",
                    secondaryResultStr = "${secondary.setScale(2, RoundingMode.HALF_UP).toPlainString()} $currency",
                    secondaryLabel = "المبلغ المتبقي بعد اقتطاع النسبة",
                    isPositiveChange = true,
                    changePercentVal = xVal,
                    differenceAmountStr = primary.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                    visualPercentage = visPct,
                    breakdownSteps = listOf(
                        "1. قسمة النسبة المئوية $xVal على 100 = ${xVal.divide(BigDecimal("100"), 4, RoundingMode.HALF_UP)}",
                        "2. ضرب الناتج في المبلغ الإجمالي $yVal = ${primary.setScale(2, RoundingMode.HALF_UP)}",
                        "3. المتبقي لتكملة المبلغ الأصلي $yVal = ${secondary.setScale(2, RoundingMode.HALF_UP)}"
                    ),
                    isValid = true
                )
            }

            PercentOperationMode.PERCENT_SHARE -> {
                if (yVal.compareTo(BigDecimal.ZERO) == 0) {
                    return PercentCalcResult(
                        primaryResultStr = "∞",
                        primaryLabel = "النسبة المئوية",
                        secondaryResultStr = "--",
                        secondaryLabel = "--",
                        isPositiveChange = null,
                        changePercentVal = BigDecimal.ZERO,
                        differenceAmountStr = "",
                        visualPercentage = 0f,
                        breakdownSteps = emptyList(),
                        isValid = false,
                        errorMessage = "لا يمكن القسمة على صفر (المبلغ Y يقع كـ 0)"
                    )
                }
                val pct = xVal.multiply(BigDecimal("100")).divide(yVal, 4, RoundingMode.HALF_UP)
                val secondaryPct = BigDecimal("100").subtract(pct)
                val visPct = pct.toFloat().coerceIn(0f, 100f)

                PercentCalcResult(
                    primaryResultStr = "${pct.setScale(2, RoundingMode.HALF_UP).toPlainString()}%",
                    primaryLabel = "نسبة $xVal من $yVal",
                    secondaryResultStr = "${secondaryPct.setScale(2, RoundingMode.HALF_UP).toPlainString()}%",
                    secondaryLabel = "النسبة المتبقية لتكملة 100%",
                    isPositiveChange = true,
                    changePercentVal = pct,
                    differenceAmountStr = yVal.subtract(xVal).toPlainString(),
                    visualPercentage = visPct,
                    breakdownSteps = listOf(
                        "1. قسمة الجزء $xVal على الكل $yVal = ${xVal.divide(yVal, 4, RoundingMode.HALF_UP)}",
                        "2. ضرب الناتج في 100 = ${pct.setScale(2, RoundingMode.HALF_UP)}%",
                        "3. النسبة المتبقية المكملة لـ 100% = ${secondaryPct.setScale(2, RoundingMode.HALF_UP)}%"
                    ),
                    isValid = true
                )
            }

            PercentOperationMode.PERCENT_INCREASE -> {
                val increaseAmt = yVal.multiply(xVal).divide(BigDecimal("100"), 4, RoundingMode.HALF_UP)
                val primary = yVal.add(increaseAmt)
                val visPct = (BigDecimal("100").add(xVal)).toFloat().coerceIn(0f, 100f)

                PercentCalcResult(
                    primaryResultStr = "${primary.setScale(2, RoundingMode.HALF_UP).toPlainString()} $currency",
                    primaryLabel = "المبلغ النهائي بعد زيادة $xVal%",
                    secondaryResultStr = "${increaseAmt.setScale(2, RoundingMode.HALF_UP).toPlainString()} $currency",
                    secondaryLabel = "مقدار الزيادة المضافة الصافية",
                    isPositiveChange = true,
                    changePercentVal = xVal,
                    differenceAmountStr = increaseAmt.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                    visualPercentage = visPct,
                    breakdownSteps = listOf(
                        "1. حساب قيمة الزيادة الصافية: $yVal × ($xVal ÷ 100) = ${increaseAmt.setScale(2, RoundingMode.HALF_UP)}",
                        "2. إضافة قيمة الزيادة إلى السعر الأصلي $yVal: $yVal + ${increaseAmt.setScale(2, RoundingMode.HALF_UP)} = ${primary.setScale(2, RoundingMode.HALF_UP)}"
                    ),
                    isValid = true
                )
            }

            PercentOperationMode.PERCENT_DECREASE -> {
                val discountAmt = yVal.multiply(xVal).divide(BigDecimal("100"), 4, RoundingMode.HALF_UP)
                val primary = yVal.subtract(discountAmt)
                val visPct = (BigDecimal("100").subtract(xVal)).toFloat().coerceIn(0f, 100f)

                PercentCalcResult(
                    primaryResultStr = "${primary.setScale(2, RoundingMode.HALF_UP).toPlainString()} $currency",
                    primaryLabel = "السعر النهائي بعد خصم $xVal%",
                    secondaryResultStr = "${discountAmt.setScale(2, RoundingMode.HALF_UP).toPlainString()} $currency",
                    secondaryLabel = "مقدار الخصم الموفر الصافي",
                    isPositiveChange = false,
                    changePercentVal = xVal,
                    differenceAmountStr = discountAmt.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                    visualPercentage = visPct,
                    breakdownSteps = listOf(
                        "1. حساب قيمة الخصم: $yVal × ($xVal ÷ 100) = ${discountAmt.setScale(2, RoundingMode.HALF_UP)}",
                        "2. طرح الخصم من السعر الأصلي $yVal: $yVal - ${discountAmt.setScale(2, RoundingMode.HALF_UP)} = ${primary.setScale(2, RoundingMode.HALF_UP)}"
                    ),
                    isValid = true
                )
            }

            PercentOperationMode.PERCENT_CHANGE -> {
                if (xVal.compareTo(BigDecimal.ZERO) == 0) {
                    return PercentCalcResult(
                        primaryResultStr = "∞",
                        primaryLabel = "نسبة التغير",
                        secondaryResultStr = "--",
                        secondaryLabel = "--",
                        isPositiveChange = null,
                        changePercentVal = BigDecimal.ZERO,
                        differenceAmountStr = "",
                        visualPercentage = 0f,
                        breakdownSteps = emptyList(),
                        isValid = false,
                        errorMessage = "القيمة القديمة X لا يمكن أن تكون صفر"
                    )
                }
                val diff = yVal.subtract(xVal)
                val pctChange = diff.multiply(BigDecimal("100")).divide(xVal, 4, RoundingMode.HALF_UP)
                val isPos = pctChange.compareTo(BigDecimal.ZERO) >= 0
                val visPct = pctChange.abs().toFloat().coerceIn(0f, 100f)

                PercentCalcResult(
                    primaryResultStr = "${pctChange.setScale(2, RoundingMode.HALF_UP).toPlainString()}%",
                    primaryLabel = if (isPos) "معدل النمو / الزيادة المئوية" else "معدل الانخفاض المئوي",
                    secondaryResultStr = "${diff.setScale(2, RoundingMode.HALF_UP).toPlainString()} $currency",
                    secondaryLabel = "مقدار التغير المطلق الصافي",
                    isPositiveChange = isPos,
                    changePercentVal = pctChange.abs(),
                    differenceAmountStr = diff.abs().setScale(2, RoundingMode.HALF_UP).toPlainString(),
                    visualPercentage = visPct,
                    breakdownSteps = listOf(
                        "1. حساب الفرق الصافي بين الجديد والقديم: $yVal - $xVal = ${diff.setScale(2, RoundingMode.HALF_UP)}",
                        "2. قسمة الفرق على القيمة القديمة $xVal والضرب في 100 = ${pctChange.setScale(2, RoundingMode.HALF_UP)}%"
                    ),
                    isValid = true
                )
            }

            PercentOperationMode.REVERSE_BASE -> {
                val factor = BigDecimal("100").subtract(xVal)
                if (factor.compareTo(BigDecimal.ZERO) <= 0) {
                    return PercentCalcResult(
                        primaryResultStr = "غير معرف",
                        primaryLabel = "المبلغ الأصلي",
                        secondaryResultStr = "--",
                        secondaryLabel = "--",
                        isPositiveChange = null,
                        changePercentVal = BigDecimal.ZERO,
                        differenceAmountStr = "",
                        visualPercentage = 0f,
                        breakdownSteps = emptyList(),
                        isValid = false,
                        errorMessage = "نسبة الخصم 100% تجعل المبلغ الأصلي غير معرف"
                    )
                }
                val originalBase = yVal.multiply(BigDecimal("100")).divide(factor, 4, RoundingMode.HALF_UP)
                val savedAmt = originalBase.subtract(yVal)

                PercentCalcResult(
                    primaryResultStr = "${originalBase.setScale(2, RoundingMode.HALF_UP).toPlainString()} $currency",
                    primaryLabel = "المبلغ الأصلي قبل خصم $xVal%",
                    secondaryResultStr = "${savedAmt.setScale(2, RoundingMode.HALF_UP).toPlainString()} $currency",
                    secondaryLabel = "إجمالي قيمة الخصم التي تم توفيرها",
                    isPositiveChange = true,
                    changePercentVal = xVal,
                    differenceAmountStr = savedAmt.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                    visualPercentage = xVal.toFloat().coerceIn(0f, 100f),
                    breakdownSteps = listOf(
                        "1. معامل السعر المدفوع المتبقي: 100% - $xVal% = ${factor.toPlainString()}%",
                        "2. قسمة المبلغ المدفوع $yVal على المعامل المتبقي = ${originalBase.setScale(2, RoundingMode.HALF_UP)}"
                    ),
                    isValid = true
                )
            }

            PercentOperationMode.MARGIN_MARKUP -> {
                val profit = yVal.subtract(xVal)
                val isPos = profit.compareTo(BigDecimal.ZERO) >= 0

                val margin = if (yVal.compareTo(BigDecimal.ZERO) != 0) profit.multiply(BigDecimal("100")).divide(yVal, 4, RoundingMode.HALF_UP) else BigDecimal.ZERO
                val markup = if (xVal.compareTo(BigDecimal.ZERO) != 0) profit.multiply(BigDecimal("100")).divide(xVal, 4, RoundingMode.HALF_UP) else BigDecimal.ZERO

                PercentCalcResult(
                    primaryResultStr = "${margin.setScale(2, RoundingMode.HALF_UP).toPlainString()}%",
                    primaryLabel = "هامش الربح الإجمالي (Gross Margin %)",
                    secondaryResultStr = "${markup.setScale(2, RoundingMode.HALF_UP).toPlainString()}%",
                    secondaryLabel = "نسبة الإضافة (Markup % على التكلفة)",
                    isPositiveChange = isPos,
                    changePercentVal = margin.abs(),
                    differenceAmountStr = profit.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                    visualPercentage = margin.abs().toFloat().coerceIn(0f, 100f),
                    breakdownSteps = listOf(
                        "1. صافي الربح المطلق = سعر البيع $yVal - التكلفة $xVal = ${profit.setScale(2, RoundingMode.HALF_UP)} $currency",
                        "2. هامش الربح Gross Margin % = (الربح ÷ سعر البيع) × 100 = ${margin.setScale(2, RoundingMode.HALF_UP)}%",
                        "3. نسبة Markup % = (الربح ÷ التكلفة) × 100 = ${markup.setScale(2, RoundingMode.HALF_UP)}%"
                    ),
                    isValid = true
                )
            }
        }
    } catch (e: Exception) {
        return PercentCalcResult(
            primaryResultStr = "خطأ",
            primaryLabel = "النتيجة",
            secondaryResultStr = "--",
            secondaryLabel = "--",
            isPositiveChange = null,
            changePercentVal = BigDecimal.ZERO,
            differenceAmountStr = "",
            visualPercentage = 0f,
            breakdownSteps = emptyList(),
            isValid = false,
            errorMessage = "حدث خطأ أثناء تنفيذ العملية الرياضية: ${e.localizedMessage}"
        )
    }
}

// ==========================================
// PREFERENCES STORAGE
// ==========================================

private fun savePercentHistoryToPrefs(context: Context, list: List<PercentHistoryItem>) {
    try {
        val prefs = context.getSharedPreferences("percent_calc_prefs", Context.MODE_PRIVATE)
        val arr = JSONArray()
        list.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("timestamp", item.timestamp)
                put("modeName", item.modeName)
                put("modeTitle", item.modeTitle)
                put("valX", item.valX)
                put("valY", item.valY)
                put("primaryResult", item.primaryResult)
                put("secondaryResult", item.secondaryResult)
                put("currency", item.currency)
            }
            arr.put(obj)
        }
        prefs.edit().putString("history_json", arr.toString()).apply()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun loadPercentHistoryFromPrefs(context: Context): List<PercentHistoryItem> {
    val list = mutableListOf<PercentHistoryItem>()
    try {
        val prefs = context.getSharedPreferences("percent_calc_prefs", Context.MODE_PRIVATE)
        val str = prefs.getString("history_json", null) ?: return emptyList()
        val arr = JSONArray(str)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                PercentHistoryItem(
                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                    timestamp = obj.optString("timestamp", ""),
                    modeName = obj.optString("modeName", PercentOperationMode.VALUE_OF_PERCENT.name),
                    modeTitle = obj.optString("modeTitle", ""),
                    valX = obj.optString("valX", ""),
                    valY = obj.optString("valY", ""),
                    primaryResult = obj.optString("primaryResult", ""),
                    secondaryResult = obj.optString("secondaryResult", ""),
                    currency = obj.optString("currency", "EGP")
                )
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}

private fun savePercentFavoritesToPrefs(context: Context, list: List<PercentHistoryItem>) {
    try {
        val prefs = context.getSharedPreferences("percent_calc_prefs", Context.MODE_PRIVATE)
        val arr = JSONArray()
        list.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("timestamp", item.timestamp)
                put("modeName", item.modeName)
                put("modeTitle", item.modeTitle)
                put("valX", item.valX)
                put("valY", item.valY)
                put("primaryResult", item.primaryResult)
                put("secondaryResult", item.secondaryResult)
                put("currency", item.currency)
            }
            arr.put(obj)
        }
        prefs.edit().putString("favorites_json", arr.toString()).apply()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun loadPercentFavoritesFromPrefs(context: Context): List<PercentHistoryItem> {
    val list = mutableListOf<PercentHistoryItem>()
    try {
        val prefs = context.getSharedPreferences("percent_calc_prefs", Context.MODE_PRIVATE)
        val str = prefs.getString("favorites_json", null) ?: return emptyList()
        val arr = JSONArray(str)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                PercentHistoryItem(
                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                    timestamp = obj.optString("timestamp", ""),
                    modeName = obj.optString("modeName", PercentOperationMode.VALUE_OF_PERCENT.name),
                    modeTitle = obj.optString("modeTitle", ""),
                    valX = obj.optString("valX", ""),
                    valY = obj.optString("valY", ""),
                    primaryResult = obj.optString("primaryResult", ""),
                    secondaryResult = obj.optString("secondaryResult", ""),
                    currency = obj.optString("currency", "EGP")
                )
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}
