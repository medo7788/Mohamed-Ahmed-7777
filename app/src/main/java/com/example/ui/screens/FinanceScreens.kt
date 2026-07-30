package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LivePricesRepository
import com.example.ui.theme.CustomThemeColors
import com.example.ui.components.ToolScreenScaffold
import com.example.ui.theme.AppIcons
import com.example.ui.theme.Spacing
import com.example.model.CalcKey
import androidx.compose.foundation.BorderStroke
import kotlin.math.max
import kotlin.math.pow

@Composable
fun FinanceInputField(label: String, value: String, colors: CustomThemeColors, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
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
}

@Composable
fun DiscountCalcScreen(colors: CustomThemeColors) {
    var priceText by remember { mutableStateOf("1000") }
    var discountText by remember { mutableStateOf("20") }

    val price = priceText.toDoubleOrNull() ?: 0.0
    val discountPercent = discountText.toDoubleOrNull() ?: 0.0

    val savedAmount = price * (discountPercent / 100.0)
    val finalPrice = price - savedAmount

    ToolScreenScaffold(
        colors = colors,
        icon = AppIcons.forCalc(CalcKey.DISCOUNT),
        title = "حاسبة الخصم",
        subtitle = "احسب السعر النهائي بعد التخفيض ومقدار التوفير المحقق"
    ) {
        // Result Card
        Surface(
            color = colors.accent.copy(alpha = 0.05f),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("السعر النهائي", color = colors.textMuted, fontSize = 14.sp)
                Text(
                    "${LivePricesRepository.formatNumber(finalPrice)} EGP",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.accent
                )
                Surface(
                    color = Color(0xFF10B981).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    Text(
                        "وفرت ${LivePricesRepository.formatNumber(savedAmount)} EGP",
                        color = Color(0xFF10B981),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        FinanceInputField("السعر الأصلي", priceText, colors) { priceText = it }
        Spacer(modifier = Modifier.height(16.dp))
        FinanceInputField("نسبة الخصم (%)", discountText, colors) { discountText = it }
    }
}

@Composable
fun LoanCalcScreen(colors: CustomThemeColors) {
    var loanText by remember { mutableStateOf("100000") }
    var rateText by remember { mutableStateOf("12") }
    var monthsText by remember { mutableStateOf("24") }

    val principal = loanText.toDoubleOrNull() ?: 0.0
    val annualRate = rateText.toDoubleOrNull() ?: 0.0
    val months = monthsText.toDoubleOrNull() ?: 1.0

    val monthlyRate = (annualRate / 100.0) / 12.0
    val emi = if (monthlyRate > 0) {
        (principal * monthlyRate * (1 + monthlyRate).pow(months)) / ((1 + monthlyRate).pow(months) - 1)
    } else {
        principal / months
    }

    val totalPayment = emi * months
    val totalInterest = totalPayment - principal

    ToolScreenScaffold(
        colors = colors,
        icon = AppIcons.forCalc(CalcKey.LOAN),
        title = "حاسبة القروض",
        subtitle = "احسب الأقساط الشهرية وإجمالي الفوائد المترتبة على القروض"
    ) {
        // Result Card
        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("القسط الشهري (EMI)", color = colors.textMuted, fontSize = 14.sp)
                Text(
                    "${LivePricesRepository.formatNumber(emi)} EGP",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.accent
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = colors.border.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("إجمالي الفائدة", fontSize = 12.sp, color = colors.textMuted)
                        Text("${LivePricesRepository.formatNumber(totalInterest)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.text)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("إجمالي السداد", fontSize = 12.sp, color = colors.textMuted)
                        Text("${LivePricesRepository.formatNumber(totalPayment)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.text)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        FinanceInputField("مبلغ القرض", loanText, colors) { loanText = it }
        Spacer(modifier = Modifier.height(12.dp))
        FinanceInputField("الفائدة السنوية (%)", rateText, colors) { rateText = it }
        Spacer(modifier = Modifier.height(12.dp))
        FinanceInputField("المدة (بالشهور)", monthsText, colors) { monthsText = it }
    }
}

@Composable
fun SavingsCalcScreen(colors: CustomThemeColors) {
    var depositText by remember { mutableStateOf("50000") }
    var rateText by remember { mutableStateOf("15") }
    var yearsText by remember { mutableStateOf("3") }

    val deposit = depositText.toDoubleOrNull() ?: 0.0
    val rate = rateText.toDoubleOrNull() ?: 0.0
    val years = yearsText.toDoubleOrNull() ?: 1.0

    val futureValue = deposit * (1 + (rate / 100.0)).pow(years)
    val netProfit = futureValue - deposit

    ToolScreenScaffold(
        colors = colors,
        icon = AppIcons.forCalc(CalcKey.SAVINGS),
        title = "حاسبة الادخار",
        subtitle = "توقع نمو مدخراتك بمرور الوقت مع الفوائد المركبة"
    ) {
        // Result Card
        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("القيمة المستقبلية المتوقعة", color = colors.textMuted, fontSize = 14.sp)
                Text(
                    "${LivePricesRepository.formatNumber(futureValue)} EGP",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.accent
                )
                Surface(
                    color = Color(0xFF10B981).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    Text(
                        "ربح صافي +${LivePricesRepository.formatNumber(netProfit)}",
                        color = Color(0xFF10B981),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        FinanceInputField("المبلغ الأولي", depositText, colors) { depositText = it }
        Spacer(modifier = Modifier.height(12.dp))
        FinanceInputField("نسبة الربح السنوي (%)", rateText, colors) { rateText = it }
        Spacer(modifier = Modifier.height(12.dp))
        FinanceInputField("عدد السنوات", yearsText, colors) { yearsText = it }
    }
}

@Composable
fun SalesTaxCalcScreen(colors: CustomThemeColors) {
    var amountText by remember { mutableStateOf("500") }
    var taxRateText by remember { mutableStateOf("14") }

    val amount = amountText.toDoubleOrNull() ?: 0.0
    val taxRate = taxRateText.toDoubleOrNull() ?: 0.0

    val taxVal = amount * (taxRate / 100.0)
    val totalWithTax = amount + taxVal

    ToolScreenScaffold(
        colors = colors,
        icon = AppIcons.forCalc(CalcKey.SALES_TAX),
        title = "ضريبة المبيعات",
        subtitle = "احسب ضريبة القيمة المضافة وإجمالي السعر شامل الضريبة"
    ) {
        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("الإجمالي شامل الضريبة", color = colors.textMuted, fontSize = 14.sp)
                Text(
                    "${LivePricesRepository.formatNumber(totalWithTax)} EGP",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.accent
                )
                Text(
                    "مقدار الضريبة (VAT): ${LivePricesRepository.formatNumber(taxVal)}",
                    fontSize = 13.sp,
                    color = colors.textMuted,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        FinanceInputField("المبلغ قبل الضريبة", amountText, colors) { amountText = it }
        Spacer(modifier = Modifier.height(12.dp))
        FinanceInputField("نسبة الضريبة (%)", taxRateText, colors) { taxRateText = it }
    }
}

@Composable
fun TipCalcScreen(colors: CustomThemeColors) {
    var billText by remember { mutableStateOf("250") }
    var tipPercentText by remember { mutableStateOf("12") }
    var personsText by remember { mutableStateOf("2") }

    val bill = billText.toDoubleOrNull() ?: 0.0
    val tipPct = tipPercentText.toDoubleOrNull() ?: 0.0
    val persons = personsText.toDoubleOrNull() ?: 1.0

    val tipTotal = bill * (tipPct / 100.0)
    val grandTotal = bill + tipTotal
    val perPerson = grandTotal / max(1.0, persons)

    ToolScreenScaffold(
        colors = colors,
        icon = AppIcons.forCalc(CalcKey.TIP),
        title = "حاسبة البقشيش",
        subtitle = "احسب البقشيش المناسب وقسّم الفاتورة بسهولة بين الأصدقاء"
    ) {
        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("المستحق للشخص الواحد", color = colors.textMuted, fontSize = 14.sp)
                Text(
                    "${LivePricesRepository.formatNumber(perPerson)} EGP",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.accent
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = colors.border.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("إجمالي البقشيش", fontSize = 12.sp, color = colors.textMuted)
                        Text("${LivePricesRepository.formatNumber(tipTotal)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.text)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("الإجمالي الكلي", fontSize = 12.sp, color = colors.textMuted)
                        Text("${LivePricesRepository.formatNumber(grandTotal)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.text)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        FinanceInputField("قيمة الفاتورة", billText, colors) { billText = it }
        Spacer(modifier = Modifier.height(12.dp))
        FinanceInputField("نسبة البقشيش (%)", tipPercentText, colors) { tipPercentText = it }
        Spacer(modifier = Modifier.height(12.dp))
        FinanceInputField("عدد الأشخاص", personsText, colors) { personsText = it }
    }
}

@Composable
fun PercentageCalcScreen(colors: CustomThemeColors) {
    var val1 by remember { mutableStateOf("20") }
    var val2 by remember { mutableStateOf("250") }

    val v1 = val1.toDoubleOrNull() ?: 0.0
    val v2 = val2.toDoubleOrNull() ?: 0.0

    val result1 = (v1 / 100.0) * v2
    val result2 = if (v2 != 0.0) (v1 / v2) * 100.0 else 0.0

    ToolScreenScaffold(
        colors = colors,
        icon = AppIcons.forCalc(CalcKey.PERCENT),
        title = "النسبة المئوية",
        subtitle = "إجراء عمليات النسبة المئوية المختلفة بسهولة ودقة"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(color = colors.surface, shape = RoundedCornerShape(24.dp), tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("كم يساوي $v1% من $v2 ؟", color = colors.textMuted, fontSize = 13.sp)
                    Text("${LivePricesRepository.formatNumber(result1)}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = colors.accent)
                }
            }
            
            Surface(color = colors.surface, shape = RoundedCornerShape(24.dp), tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("ما هي نسبة $v1 من $v2 ؟", color = colors.textMuted, fontSize = 13.sp)
                    Text("${LivePricesRepository.formatNumber(result2)}%", fontSize = 24.sp, fontWeight = FontWeight.Black, color = colors.accent)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        FinanceInputField("الرقم الأول (X)", val1, colors) { val1 = it }
        Spacer(modifier = Modifier.height(12.dp))
        FinanceInputField("الرقم الثاني (Y)", val2, colors) { val2 = it }
    }
}

@Composable
fun UnitPriceCalcScreen(colors: CustomThemeColors) {
    var priceA by remember { mutableStateOf("100") }
    var qtyA by remember { mutableStateOf("500") }

    var priceB by remember { mutableStateOf("180") }
    var qtyB by remember { mutableStateOf("1000") }

    val pA = priceA.toDoubleOrNull() ?: 0.0
    val qA = qtyA.toDoubleOrNull() ?: 1.0

    val pB = priceB.toDoubleOrNull() ?: 0.0
    val qB = qtyB.toDoubleOrNull() ?: 1.0

    val unitA = pA / qA
    val unitB = pB / qB

    val isABetter = unitA < unitB

    ToolScreenScaffold(
        colors = colors,
        icon = AppIcons.forCalc(CalcKey.UNIT_PRICE),
        title = "مقارنة الأسعار",
        subtitle = "قارن بين منتجين لمعرفة العرض الأوفر بناءً على سعر الوحدة"
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("المنتج (أ)", fontWeight = FontWeight.Bold, color = colors.text, modifier = Modifier.padding(start = 4.dp))
                FinanceInputField("السعر", priceA, colors) { priceA = it }
                FinanceInputField("الكمية", qtyA, colors) { qtyA = it }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("المنتج (ب)", fontWeight = FontWeight.Bold, color = colors.text, modifier = Modifier.padding(start = 4.dp))
                FinanceInputField("السعر", priceB, colors) { priceB = it }
                FinanceInputField("الكمية", qtyB, colors) { qtyB = it }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            color = Color(0xFF10B981).copy(alpha = 0.05f),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (isABetter) "المنتج (أ) هو الأوفر ✅" else "المنتج (ب) هو الأوفر ✅",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF10B981)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("سعر وحدة (أ)", fontSize = 11.sp, color = colors.textMuted)
                        Text(String.format("%.3f", unitA), fontWeight = FontWeight.Bold, color = colors.text)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("سعر وحدة (ب)", fontSize = 11.sp, color = colors.textMuted)
                        Text(String.format("%.3f", unitB), fontWeight = FontWeight.Bold, color = colors.text)
                    }
                }
            }
        }
    }
}
