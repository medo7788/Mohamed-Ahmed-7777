package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Divider
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
fun PaintCalculatorScreen(colors: CustomThemeColors) {
    var length by remember { mutableStateOf(5f) } // 2m to 20m
    var width by remember { mutableStateOf(4f) }  // 2m to 20m
    var height by remember { mutableStateOf(3f) } // 2m to 6m
    var coats by remember { mutableStateOf(2) }   // 1, 2, 3 coats

    val wallArea = 2 * (length + width) * height
    val litersNeeded = (wallArea * coats) * 0.15f // 0.15 liters per sq meter per coat

    ToolScreenScaffold(
        colors = colors,
        icon = AppIcons.forCalc(CalcKey.PAINT),
        title = "حاسبة الدهان",
        subtitle = "حساب دقيق لمساحة الجدران وكميات الطلاء المطلوبة للغرف والمنازل"
    ) {
        Surface(
            color = colors.surface2.copy(alpha = 0.85f),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("النتائج التقديرية للطلاء", color = colors.textMuted, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${String.format("%.1f", wallArea)} م²",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.accent
                )
                Text(
                    text = "المساحة الإجمالية للجدران",
                    fontSize = 12.sp,
                    color = colors.textMuted
                )

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = colors.border.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "كمية الدهان المقدرة: ${String.format("%.1f", litersNeeded)} لتر",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1FD0C5) // Turquoise neon look
                )
                Text(
                    text = "تقريباً ${String.format("%.1f", litersNeeded / 3.785f)} جالون دهان",
                    fontSize = 11.sp,
                    color = colors.textMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Length Slider
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("طول الغرفة (متر)", color = colors.text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("${String.format("%.1f", length)} م", color = colors.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = length,
                onValueChange = { length = it },
                valueRange = 2f..20f,
                colors = SliderDefaults.colors(
                    thumbColor = colors.accent,
                    activeTrackColor = colors.accent
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Width Slider
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("عرض الغرفة (متر)", color = colors.text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("${String.format("%.1f", width)} م", color = colors.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = width,
                onValueChange = { width = it },
                valueRange = 2f..20f,
                colors = SliderDefaults.colors(
                    thumbColor = colors.accent,
                    activeTrackColor = colors.accent
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Height Slider
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("ارتفاع السقف (متر)", color = colors.text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("${String.format("%.1f", height)} م", color = colors.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = height,
                onValueChange = { height = it },
                valueRange = 2f..6f,
                colors = SliderDefaults.colors(
                    thumbColor = colors.accent,
                    activeTrackColor = colors.accent
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Coats Selector Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("عدد طبقات الدهان (Coats)", color = colors.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 2, 3).forEach { coatVal ->
                    val isSelected = coats == coatVal
                    Surface(
                        onClick = { coats = coatVal },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) colors.accent else colors.surface,
                        border = BorderStroke(1.dp, colors.border.copy(alpha = 0.4f)),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = coatVal.toString(),
                                color = if (isSelected) Color.Black else colors.text,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
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
