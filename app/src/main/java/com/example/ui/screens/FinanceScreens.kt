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
import kotlin.math.max
import kotlin.math.pow

@Composable
fun DiscountCalcScreen(colors: CustomThemeColors) {
    var priceText by remember { mutableStateOf("1000") }
    var discountText by remember { mutableStateOf("20") }

    val price = priceText.toDoubleOrNull() ?: 0.0
    val discountPercent = discountText.toDoubleOrNull() ?: 0.0

    val savedAmount = price * (discountPercent / 100.0)
    val finalPrice = price - savedAmount

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.appBg)
            .padding(12.dp)
    ) {
        OutlinedTextField(
            value = priceText,
            onValueChange = { priceText = it },
            label = { Text("السعر الأصلي (قبل الخصم)", color = colors.textMuted) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = discountText,
            onValueChange = { discountText = it },
            label = { Text("نسبة الخصم (%)", color = colors.textMuted) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("السعر النهائي بعد الخصم:", fontSize = 13.sp, color = colors.textMuted)
                Text("${LivePricesRepository.formatNumber(finalPrice)} EGP", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = colors.accent)
                Spacer(modifier = Modifier.height(8.dp))
                Text("مبلغ الخصم المحفوظ: ${LivePricesRepository.formatNumber(savedAmount)} EGP", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
            }
        }
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.appBg)
            .padding(12.dp)
    ) {
        OutlinedTextField(
            value = loanText,
            onValueChange = { loanText = it },
            label = { Text("مبلغ القرض الكلي", color = colors.textMuted) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = rateText,
            onValueChange = { rateText = it },
            label = { Text("نسبة الفائدة السنوية (%)", color = colors.textMuted) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = monthsText,
            onValueChange = { monthsText = it },
            label = { Text("مدة القرض بالشهور", color = colors.textMuted) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("القسط الشهري المستحق (EMI):", fontSize = 13.sp, color = colors.textMuted)
                Text("${LivePricesRepository.formatNumber(emi)} EGP", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = colors.accent)
                Spacer(modifier = Modifier.height(10.dp))
                Text("• إجمالي الفوائد: ${LivePricesRepository.formatNumber(totalInterest)} EGP", fontSize = 13.sp, color = colors.text)
                Text("• إجمالي المبلغ المسدد: ${LivePricesRepository.formatNumber(totalPayment)} EGP", fontSize = 13.sp, color = colors.text)
            }
        }
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.appBg)
            .padding(12.dp)
    ) {
        OutlinedTextField(
            value = depositText,
            onValueChange = { depositText = it },
            label = { Text("مبلغ الوديعة / الادخار الأولية", color = colors.textMuted) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = rateText,
            onValueChange = { rateText = it },
            label = { Text("نسبة الربح/الفائدة السنوية (%)", color = colors.textMuted) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = yearsText,
            onValueChange = { yearsText = it },
            label = { Text("عدد السنوات", color = colors.textMuted) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("القيمة المستقبلية للمدخرات:", fontSize = 13.sp, color = colors.textMuted)
                Text("${LivePricesRepository.formatNumber(futureValue)} EGP", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = colors.accent)
                Spacer(modifier = Modifier.height(8.dp))
                Text("صافي الأرباح المحققة: +${LivePricesRepository.formatNumber(netProfit)} EGP", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
            }
        }
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.appBg)
            .padding(12.dp)
    ) {
        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it },
            label = { Text("المبلغ قبل الضريبة", color = colors.textMuted) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = taxRateText,
            onValueChange = { taxRateText = it },
            label = { Text("نسبة ضريبة القيمة المضافة VAT (%)", color = colors.textMuted) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("المبلغ الإجمالي شاملاً الضريبة:", fontSize = 13.sp, color = colors.textMuted)
                Text("${LivePricesRepository.formatNumber(totalWithTax)} EGP", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = colors.accent)
                Spacer(modifier = Modifier.height(6.dp))
                Text("مقدار الضريبة المضافة: ${LivePricesRepository.formatNumber(taxVal)} EGP", fontSize = 13.sp, color = colors.text)
            }
        }
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.appBg)
            .padding(12.dp)
    ) {
        OutlinedTextField(
            value = billText,
            onValueChange = { billText = it },
            label = { Text("قيمة الفاتورة الإجمالية", color = colors.textMuted) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = tipPercentText,
            onValueChange = { tipPercentText = it },
            label = { Text("نسبة البقشيش (%)", color = colors.textMuted) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = personsText,
            onValueChange = { personsText = it },
            label = { Text("عدد الأشخاص للتقسيم", color = colors.textMuted) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("المبلغ المستحق للشخص الواحد:", fontSize = 13.sp, color = colors.textMuted)
                Text("${LivePricesRepository.formatNumber(perPerson)} EGP", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = colors.accent)
                Spacer(modifier = Modifier.height(8.dp))
                Text("• إجمالي البقشيش: ${LivePricesRepository.formatNumber(tipTotal)} EGP", fontSize = 13.sp, color = colors.text)
                Text("• المبلغ الكلي شامل البقشيش: ${LivePricesRepository.formatNumber(grandTotal)} EGP", fontSize = 13.sp, color = colors.text)
            }
        }
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.appBg)
            .padding(12.dp)
    ) {
        OutlinedTextField(
            value = val1,
            onValueChange = { val1 = it },
            label = { Text("الرقم الأول (X)", color = colors.textMuted) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = val2,
            onValueChange = { val2 = it },
            label = { Text("الرقم الثاني (Y)", color = colors.textMuted) },
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
                Text("1. كم يساوي $v1% من $v2 ؟", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = colors.text)
                Text("${LivePricesRepository.formatNumber(result1)}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.accent)

                Spacer(modifier = Modifier.height(12.dp))

                Text("2. ما هي النسبة المئوية لـ $v1 من $v2 ؟", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = colors.text)
                Text("${LivePricesRepository.formatNumber(result2)}%", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.accent)
            }
        }
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.appBg)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(color = colors.surface, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("المنتج (أ)", fontWeight = FontWeight.Bold, color = colors.text)
                    OutlinedTextField(value = priceA, onValueChange = { priceA = it }, label = { Text("السعر") }, singleLine = true)
                    OutlinedTextField(value = qtyA, onValueChange = { qtyA = it }, label = { Text("الكمية/الوزن") }, singleLine = true)
                }
            }

            Surface(color = colors.surface, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("المنتج (ب)", fontWeight = FontWeight.Bold, color = colors.text)
                    OutlinedTextField(value = priceB, onValueChange = { priceB = it }, label = { Text("السعر") }, singleLine = true)
                    OutlinedTextField(value = qtyB, onValueChange = { qtyB = it }, label = { Text("الكمية/الوزن") }, singleLine = true)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    if (isABetter) "🎉 المنتج (أ) هو الصفقة الأوفر والأنسب!" else "🎉 المنتج (ب) هو الصفقة الأوفر والأنسب!",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("سعر الوحدة أ: ${String.format("%.4f", unitA)} / وحدة", fontSize = 12.sp, color = colors.text)
                Text("سعر الوحدة ب: ${String.format("%.4f", unitB)} / وحدة", fontSize = 12.sp, color = colors.text)
            }
        }
    }
}
