package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LivePricesRepository
import com.example.ui.theme.CustomThemeColors
import com.example.util.TafqeetArabic

@Composable
fun FuelCostCalcScreen(colors: CustomThemeColors) {
    var distanceText by remember { mutableStateOf("300") }
    var consumptionText by remember { mutableStateOf("8") } // L/100km
    var pricePerLiterText by remember { mutableStateOf("13.5") }

    val dist = distanceText.toDoubleOrNull() ?: 0.0
    val cons = consumptionText.toDoubleOrNull() ?: 0.0
    val price = pricePerLiterText.toDoubleOrNull() ?: 0.0

    val litersNeeded = (dist / 100.0) * cons
    val totalCost = litersNeeded * price

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
                Text("⛽ حاسبة تكلفة الوقود والبنزين للرحلات", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.text)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = distanceText,
                    onValueChange = { distanceText = it },
                    label = { Text("المسافة المستهدفة (كم)", color = colors.textMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = consumptionText,
                    onValueChange = { consumptionText = it },
                    label = { Text("استهلاك السيارة (لتر / 100 كم)", color = colors.textMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = pricePerLiterText,
                    onValueChange = { pricePerLiterText = it },
                    label = { Text("سعر لتر البنزين/الوقود", color = colors.textMuted) },
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
            Column(modifier = Modifier.padding(20.dp)) {
                Text("التكلفة الإجمالية للرحلة:", fontSize = 13.sp, color = colors.textMuted)
                Text("${LivePricesRepository.formatNumber(totalCost)} EGP", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = colors.accent)
                Spacer(modifier = Modifier.height(6.dp))
                Text("كمية الوقود المطلوبة: ${LivePricesRepository.formatNumber(litersNeeded)} لتر", fontSize = 13.sp, color = colors.text)
            }
        }
    }
}

@Composable
fun FuelEfficiencyCalcScreen(colors: CustomThemeColors) {
    var distanceText by remember { mutableStateOf("450") }
    var litersText by remember { mutableStateOf("36") }

    val dist = distanceText.toDoubleOrNull() ?: 1.0
    val liters = litersText.toDoubleOrNull() ?: 0.0

    val lPer100km = (liters / dist) * 100.0
    val kmPerL = if (liters > 0) dist / liters else 0.0

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
                Text("🏎️ حاسبة معدل استهلاك الوقود", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.text)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = distanceText,
                    onValueChange = { distanceText = it },
                    label = { Text("المسافة المقطوعة (كم)", color = colors.textMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = litersText,
                    onValueChange = { litersText = it },
                    label = { Text("كمية الوقود المستهلكة (لتر)", color = colors.textMuted) },
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
            Column(modifier = Modifier.padding(20.dp)) {
                Text("معدل الكفاءة والاستهلاك:", fontSize = 13.sp, color = colors.textMuted)
                Spacer(modifier = Modifier.height(4.dp))
                Text("${LivePricesRepository.formatNumber(lPer100km)} لتر / 100 كم", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = colors.accent)
                Spacer(modifier = Modifier.height(6.dp))
                Text("أو: ${LivePricesRepository.formatNumber(kmPerL)} كم / لتر واحد", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.text)
            }
        }
    }
}

@Composable
fun NumberToWordsScreen(colors: CustomThemeColors) {
    var numberText by remember { mutableStateOf("1250.50") }
    var selectedCurrency by remember { mutableStateOf("EGP") }

    val num = numberText.toDoubleOrNull() ?: 0.0
    val tafqeetResult = TafqeetArabic.convertToWords(num, selectedCurrency)

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
                Text("✍️ تحويل الأرقام إلى كلمات (تفقيط الشيكات)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.text)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = numberText,
                    onValueChange = { numberText = it },
                    label = { Text("اكتب المبلغ الرقمي هنا", color = colors.textMuted) },
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
            Column(modifier = Modifier.padding(20.dp)) {
                Text("الصياغة اللفظية الرسمية (التفقيط):", fontSize = 13.sp, color = colors.textMuted)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    tafqeetResult,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.accent,
                    lineHeight = 26.sp
                )
            }
        }
    }
}

@Composable
fun GPACalcScreen(colors: CustomThemeColors) {
    var gpaSumText by remember { mutableStateOf("3.45") }

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
                Text("🎓 حاسبة المعدل التراكمي الجامعي (GPA)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.text)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = gpaSumText,
                    onValueChange = { gpaSumText = it },
                    label = { Text("المعدل من 4.0", color = colors.textMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val gpaVal = gpaSumText.toDoubleOrNull() ?: 0.0
        val gpa5 = (gpaVal / 4.0) * 5.0
        val percentage = (gpaVal / 4.0) * 100.0

        Surface(
            color = colors.surface2,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("تحويلات المعدل:", fontSize = 13.sp, color = colors.textMuted)
                Spacer(modifier = Modifier.height(4.dp))
                Text("• المعدل من 5.0: ${String.format("%.2f", gpa5)} / 5.00", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.accent)
                Text("• النسبة المئوية: ${String.format("%.1f", percentage)}%", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.accent)
            }
        }
    }
}

@Composable
fun HexConverterScreen(colors: CustomThemeColors) {
    var decInputText by remember { mutableStateOf("255") }
    val decVal = decInputText.toIntOrNull() ?: 0

    val hexVal = decVal.toString(16).uppercase()
    val binVal = decVal.toString(2)
    val octVal = decVal.toString(8)

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
                Text("💻 محول الأنظمة العددية (Hex/Dec/Bin/Oct)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.text)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = decInputText,
                    onValueChange = { decInputText = it },
                    label = { Text("الرقم بالنظام العشري (Decimal)", color = colors.textMuted) },
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
            Column(modifier = Modifier.padding(20.dp)) {
                Text("• النظام الست عشري (Hexadecimal): $hexVal", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.accent)
                Spacer(modifier = Modifier.height(6.dp))
                Text("• النظام الثنائي (Binary): $binVal", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.text)
                Spacer(modifier = Modifier.height(6.dp))
                Text("• النظام الثماني (Octal): $octVal", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.text)
            }
        }
    }
}
