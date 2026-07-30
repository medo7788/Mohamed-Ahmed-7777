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
import com.example.ui.components.ToolScreenScaffold
import com.example.ui.theme.AppIcons
import com.example.ui.theme.Spacing
import com.example.model.CalcKey
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

    ToolScreenScaffold(
        colors = colors,
        icon = AppIcons.forCalc(CalcKey.FUEL_COST),
        title = "تكلفة الوقود",
        subtitle = "حساب تكلفة البنزين المقدرة للرحلات والمسافات الطويلة"
    ) {
        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("التكلفة التقديرية", color = colors.textMuted, fontSize = 14.sp)
                Text(
                    "${LivePricesRepository.formatNumber(totalCost)} EGP",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.accent
                )
                Text(
                    "كمية الوقود: ${LivePricesRepository.formatNumber(litersNeeded)} لتر",
                    fontSize = 13.sp,
                    color = colors.text,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = distanceText,
            onValueChange = { distanceText = it },
            label = { Text("المسافة (كم)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.accent,
                unfocusedBorderColor = colors.border,
                focusedTextColor = colors.text,
                unfocusedTextColor = colors.text
            ),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = consumptionText,
                onValueChange = { consumptionText = it },
                label = { Text("استهلاك (لتر/100كم)") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            )
            OutlinedTextField(
                value = pricePerLiterText,
                onValueChange = { pricePerLiterText = it },
                label = { Text("سعر اللتر") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            )
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

    ToolScreenScaffold(
        colors = colors,
        icon = AppIcons.forCalc(CalcKey.FUEL_EFF),
        title = "كفاءة الوقود",
        subtitle = "احسب معدل استهلاك سيارتك للبنزين لكل 100 كيلومتر"
    ) {
        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("معدل الاستهلاك", color = colors.textMuted, fontSize = 14.sp)
                Text(
                    "${LivePricesRepository.formatNumber(lPer100km)} لتر",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.accent
                )
                Text("لكل 100 كيلومتر", fontSize = 14.sp, color = colors.textMuted)
                
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = colors.border.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    "أو: ${LivePricesRepository.formatNumber(kmPerL)} كم / لتر",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.text
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = distanceText,
                onValueChange = { distanceText = it },
                label = { Text("المسافة (كم)") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            )
            OutlinedTextField(
                value = litersText,
                onValueChange = { litersText = it },
                label = { Text("لتر مستهلك") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            )
        }
    }
}

@Composable
fun NumberToWordsScreen(colors: CustomThemeColors) {
    var numberText by remember { mutableStateOf("1250.50") }
    var selectedCurrency by remember { mutableStateOf("EGP") }

    val num = numberText.toDoubleOrNull() ?: 0.0
    val tafqeetResult = TafqeetArabic.convertToWords(num, selectedCurrency)

    ToolScreenScaffold(
        colors = colors,
        icon = AppIcons.forCalc(CalcKey.NUM_WORDS),
        title = "تحويل الأرقام لكلمات",
        subtitle = "تحويل المبالغ المالية إلى صيغة نصية باللغة العربية (التفقيط)"
    ) {
        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("الصياغة اللفظية", color = colors.textMuted, fontSize = 12.sp)
                Text(
                    tafqeetResult,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.accent,
                    lineHeight = 28.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = numberText,
            onValueChange = { numberText = it },
            label = { Text("المبلغ الرقمي") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.accent,
                unfocusedBorderColor = colors.border,
                focusedTextColor = colors.text,
                unfocusedTextColor = colors.text
            ),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
        )
    }
}

@Composable
fun GPACalcScreen(colors: CustomThemeColors) {
    var gpaSumText by remember { mutableStateOf("3.45") }
    val gpaVal = gpaSumText.toDoubleOrNull() ?: 0.0
    val gpa5 = (gpaVal / 4.0) * 5.0
    val percentage = (gpaVal / 4.0) * 100.0

    ToolScreenScaffold(
        colors = colors,
        icon = AppIcons.forCalc(CalcKey.GPA),
        title = "حاسبة المعدل",
        subtitle = "تحويل المعدل الجامعي بين الأنظمة المختلفة (4.0، 5.0، مئوي)"
    ) {
        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("النتائج المحولة", color = colors.textMuted, fontSize = 14.sp)
                Text(
                    "${String.format("%.2f", gpa5)} / 5.0",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.accent
                )
                Text(
                    "النسبة المئوية: ${String.format("%.1f", percentage)}%",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.text,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = gpaSumText,
            onValueChange = { gpaSumText = it },
            label = { Text("المعدل الحالي (من 4.0)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.accent,
                unfocusedBorderColor = colors.border,
                focusedTextColor = colors.text,
                unfocusedTextColor = colors.text
            ),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
        )
    }
}

@Composable
fun HexConverterScreen(colors: CustomThemeColors) {
    var decInputText by remember { mutableStateOf("255") }
    val decVal = decInputText.toIntOrNull() ?: 0

    val hexVal = decVal.toString(16).uppercase()
    val binVal = decVal.toString(2)
    val octVal = decVal.toString(8)

    ToolScreenScaffold(
        colors = colors,
        icon = AppIcons.forCalc(CalcKey.HEX),
        title = "محول الأنظمة",
        subtitle = "تحويل الأرقام بين الأنظمة (العشري، الست عشري، الثنائي، الثماني)"
    ) {
        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    Text("Hexadecimal", color = colors.textMuted, fontSize = 11.sp)
                    Text(hexVal, fontSize = 24.sp, fontWeight = FontWeight.Black, color = colors.accent)
                }
                Divider(color = colors.border.copy(alpha = 0.3f))
                Column {
                    Text("Binary", color = colors.textMuted, fontSize = 11.sp)
                    Text(binVal, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.text)
                }
                Column {
                    Text("Octal", color = colors.textMuted, fontSize = 11.sp)
                    Text(octVal, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.text)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = decInputText,
            onValueChange = { decInputText = it },
            label = { Text("الرقم العشري (Decimal)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.accent,
                unfocusedBorderColor = colors.border,
                focusedTextColor = colors.text,
                unfocusedTextColor = colors.text
            ),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
        )
    }
}
