package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LivePricesRepository
import com.example.model.CalcKey
import com.example.ui.components.ToolScreenScaffold
import com.example.ui.theme.AppIcons
import com.example.ui.theme.CustomThemeColors

// Visual Theme Spec Colors
private val DarkBg = Color(0xFF121212)
private val AmbientNight = Color(0xFF0B1119)
private val RoyalNight = Color(0xFF0C1E33)
private val PremiumGold = Color(0xFFD8B56A)
private val LuminousTurquoise = Color(0xFF1FD0C5)
private val PrimaryText = Color(0xFFFFFFFF)
private val SecondaryText = Color(0xFFA0A0A0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZakatCalcScreenRedesign(colors: CustomThemeColors) {
    var cashText by remember { mutableStateOf("") }
    var goldGramsText by remember { mutableStateOf("") }
    var silverGramsText by remember { mutableStateOf("") }
    var investmentsText by remember { mutableStateOf("") }
    var debtsText by remember { mutableStateOf("") }

    val cash = cashText.toDoubleOrNull() ?: 0.0
    val goldGrams = goldGramsText.toDoubleOrNull() ?: 0.0
    val silverGrams = silverGramsText.toDoubleOrNull() ?: 0.0
    val investments = investmentsText.toDoubleOrNull() ?: 0.0
    val debts = debtsText.toDoubleOrNull() ?: 0.0

    // Reference conversion rates
    val exchangeRate = 48.65 // EGP per USD
    val goldPriceGram24 = LivePricesRepository.getGoldPricePerGramInUsd(24) * exchangeRate
    val silverPriceGram = 0.98 * exchangeRate // estimated silver per gram

    val goldVal = goldGrams * goldPriceGram24
    val silverVal = silverGrams * silverPriceGram

    // Total net eligible wealth
    val netWealth = (cash + goldVal + silverVal + investments) - debts

    // Nisab Threshold is ~85g of 24k gold
    val nisabThreshold = 85.0 * goldPriceGram24
    val isNisabMet = netWealth >= nisabThreshold
    val zakatAmount = if (isNisabMet) netWealth * 0.025 else 0.0

    // Progress towards Nisab
    val progress = if (nisabThreshold > 0.0) {
        (netWealth / nisabThreshold).coerceIn(0.0, 1.0).toFloat()
    } else {
        0f
    }

    // Interactive action to add a lump debt value instantly
    var showAddDebtDialog by remember { mutableStateOf(false) }
    var debtToAddInput by remember { mutableStateOf("") }

    ToolScreenScaffold(
        colors = colors,
        icon = AppIcons.forCalc(CalcKey.ZAKAT),
        title = "مستشار الزكاة الذكي",
        subtitle = "حساب زكاة المال والذهب والفضة والالتزامات وفق أسعار السوق المحدثة",
        isScrollable = false
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // --- 1. NISAB PROGRESS INDICATOR SECTION (ADVISOR INSIGHT) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.02f))
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Circular Progress Canvas
                    Box(
                        modifier = Modifier.size(90.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val infiniteTransition = rememberInfiniteTransition()
                        val pulseGlowAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.15f,
                            targetValue = 0.45f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1500, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            )
                        )

                        Canvas(modifier = Modifier.size(80.dp)) {
                            // Track
                            drawCircle(
                                color = Color.White.copy(alpha = 0.07f),
                                radius = size.minDimension / 2,
                                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                            )
                            // Progress
                            drawArc(
                                color = if (isNisabMet) PremiumGold else LuminousTurquoise,
                                startAngle = -90f,
                                sweepAngle = progress * 360f,
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        // Inside circular progress info
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.W900,
                                color = if (isNisabMet) PremiumGold else LuminousTurquoise
                            )
                        }
                    }

                    // Text status indicator
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isNisabMet) "بلغت النصاب الشرعي ✓" else "دون حد النصاب الشرعي",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.W800,
                            color = if (isNisabMet) PremiumGold else LuminousTurquoise
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isNisabMet) {
                                "وجبت زكاة المال بنسبة 2.5% على كافة أصولك المؤهلة."
                            } else {
                                val remaining = nisabThreshold - netWealth
                                "متبقي للوصول إلى النصاب: ${String.format("%,.0f", remaining)} EGP"
                            },
                            fontSize = 12.sp,
                            color = SecondaryText,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "النصاب الحالي: ${String.format("%,.0f", nisabThreshold)} EGP (عيار 24)",
                            fontSize = 10.sp,
                            color = colors.textMuted,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // --- 2. ZAKAT CALCULATION RESULT PRESTIGE PANEL ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                RoyalNight.copy(alpha = 0.6f),
                                Color.Transparent
                            )
                        )
                    )
                    .background(Color.White.copy(alpha = 0.015f))
                    .border(
                        BorderStroke(1.dp, if (isNisabMet) PremiumGold.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f)),
                        RoundedCornerShape(28.dp)
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "قيمة الزكاة المستحقة الدفع",
                        fontSize = 13.sp,
                        color = SecondaryText,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val infiniteTransition = rememberInfiniteTransition()
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 0.98f,
                        targetValue = 1.02f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1400, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        )
                    )

                    Text(
                        text = String.format("%,.2f EGP", zakatAmount),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.W900,
                        color = PremiumGold,
                        modifier = Modifier.graphicsLayer {
                            scaleX = if (isNisabMet) pulseScale else 1.0f
                            scaleY = if (isNisabMet) pulseScale else 1.0f
                        }
                    )

                    if (isNisabMet) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "طهّر مالك ونمِّ رزقك بالصدقة والبركة",
                            fontSize = 11.sp,
                            color = LuminousTurquoise,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // --- 3. GLASS FORM INPUT BLOCKS ---
            Text(
                text = "المدخلات المالية للأصول والخصوم",
                fontSize = 16.sp,
                fontWeight = FontWeight.W800,
                color = PrimaryText,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Cash & liquidity
                ZakatAdvisorInputRow(
                    label = "النقد والسيولة والمدخرات النقديّة (EGP)",
                    value = cashText,
                    icon = AppIcons.forCalc(CalcKey.CURRENCY),
                    onValueChange = { cashText = it }
                )

                // Gold grams
                ZakatAdvisorInputRow(
                    label = "وزن الذهب عيار 24 (بالجرام)",
                    value = goldGramsText,
                    icon = AppIcons.forCalc(CalcKey.GOLD),
                    onValueChange = { goldGramsText = it }
                )

                // Silver grams
                ZakatAdvisorInputRow(
                    label = "وزن الفضة عيار 999 (بالجرام)",
                    value = silverGramsText,
                    icon = AppIcons.forCalc(CalcKey.UNIT),
                    onValueChange = { silverGramsText = it }
                )

                // Investments
                ZakatAdvisorInputRow(
                    label = "الأسهم والصناديق والأصول الاستثمارية (EGP)",
                    value = investmentsText,
                    icon = AppIcons.forCalc(CalcKey.SAVINGS),
                    onValueChange = { investmentsText = it }
                )

                // Debts & liabilities
                ZakatAdvisorInputRow(
                    label = "الديون المستحقة والالتزامات الماليّة (EGP)",
                    value = debtsText,
                    icon = AppIcons.forCalc(CalcKey.LOAN),
                    onValueChange = { debtsText = it }
                )
            }

            // --- 4. ADD DEBT QUICK-ACTION BUTTON ---
            Button(
                onClick = { showAddDebtDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f)),
                border = BorderStroke(1.dp, PremiumGold.copy(alpha = 0.2f))
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = PremiumGold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("إضافة التزام / دين إضافي", color = PremiumGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            // --- 5. LEGITIMATE FATWA DISCLAIMER FOOTER ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.01f))
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = PremiumGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "تنويه شرعي: النتيجة الحسابية تقديرية وتعتمد على البيانات المالية المدخلة وأسعار الذهب والفضة الحالية من السوق. لا يقدم التطبيق فتوى شخصية مخصصة، ويُستحب الرجوع لأهل العلم للمسائل الدقيقة.",
                        fontSize = 11.sp,
                        color = SecondaryText,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }

    // Interactive Dialog to Append Debt Values
    if (showAddDebtDialog) {
        AlertDialog(
            onDismissRequest = { showAddDebtDialog = false },
            title = {
                Text(
                    text = "إضافة دين مستحق جديد",
                    color = PrimaryText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.W800,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "أدخل قيمة التزامات الديون الإضافية ليتم تصفيتها وخصمها من الأصول الخاضعة للزكاة تلقائياً:",
                        color = SecondaryText,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = debtToAddInput,
                        onValueChange = { debtToAddInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PremiumGold,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedContainerColor = DarkBg,
                            unfocusedContainerColor = DarkBg,
                            focusedTextColor = PrimaryText,
                            unfocusedTextColor = PrimaryText
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val added = debtToAddInput.toDoubleOrNull() ?: 0.0
                        val currentDebts = debtsText.toDoubleOrNull() ?: 0.0
                        debtsText = (currentDebts + added).toString()
                        debtToAddInput = ""
                        showAddDebtDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PremiumGold)
                ) {
                    Text("إضافة وخصم", color = RoyalNight, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDebtDialog = false }) {
                    Text("إلغاء", color = SecondaryText)
                }
            },
            containerColor = Color(0xFF1E1E1E),
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun ZakatAdvisorInputRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onValueChange: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.02f))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)), RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(LuminousTurquoise.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = LuminousTurquoise,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = SecondaryText,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = PrimaryText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.W800,
                        textAlign = TextAlign.Start
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(PremiumGold)
                )
            }
        }
    }
}
