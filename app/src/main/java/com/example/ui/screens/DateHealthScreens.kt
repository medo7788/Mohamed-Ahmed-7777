package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.ui.theme.CustomThemeColors
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WorldTimeScreen(colors: CustomThemeColors) {
    // Re-calculate times every minute if we were to make it live, but for now we just compute on render.
    var currentTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            currentTimeMillis = System.currentTimeMillis()
        }
    }

    val worldCities = listOf(
        Triple("مكة المكرمة 🕋", "Asia/Riyadh", "GMT+3"),
        Triple("القاهرة 🇪🇬", "Africa/Cairo", "GMT+3"),
        Triple("الرياض 🇸🇦", "Asia/Riyadh", "GMT+3"),
        Triple("دبي 🇦🇪", "Asia/Dubai", "GMT+4"),
        Triple("الكويت 🇰🇼", "Asia/Kuwait", "GMT+3"),
        Triple("الدوحة 🇶🇦", "Asia/Qatar", "GMT+3"),
        Triple("عمان 🇯🇴", "Asia/Amman", "GMT+3"),
        Triple("بغداد 🇮🇶", "Asia/Baghdad", "GMT+3"),
        Triple("لندن 🇬🇧", "Europe/London", "GMT+0"),
        Triple("باريس 🇫🇷", "Europe/Paris", "GMT+1"),
        Triple("نيويورك 🇺🇸", "America/New_York", "GMT-4"),
        Triple("طوكيو 🇯🇵", "Asia/Tokyo", "GMT+9"),
        Triple("سيدني 🇦🇺", "Australia/Sydney", "GMT+10")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.appBg)
            .padding(12.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
        ) {
            items(worldCities) { (city, tzId, label) ->
                val tz = TimeZone.getTimeZone(tzId)
                val sdf = SimpleDateFormat("hh:mm:ss a", Locale("ar"))
                sdf.timeZone = tz
                val timeStr = sdf.format(Date(currentTimeMillis))

                Surface(
                    color = colors.surface,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(city, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.text)
                            Text(label, fontSize = 11.sp, color = colors.textMuted)
                        }
                        Text(timeStr, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = colors.accent)
                    }
                }
            }
        }
    }
}

@Composable
fun DateCalcScreen(colors: CustomThemeColors) {
    var daysToAdd by remember { mutableStateOf("30") }
    val days = daysToAdd.toIntOrNull() ?: 0

    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, days)
    val sdf = SimpleDateFormat("yyyy/MM/dd (EEEE)", Locale("ar"))
    val futureDateStr = sdf.format(cal.time)

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
                Text("📅 حاسبة إضافة الخروج/الأيام للتاريخ", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.text)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = daysToAdd,
                    onValueChange = { daysToAdd = it },
                    label = { Text("عدد الأيام المراد إضافتها", color = colors.textMuted) },
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
                Text("التاريخ المستهدف بعد $days يوم:", fontSize = 13.sp, color = colors.textMuted)
                Spacer(modifier = Modifier.height(4.dp))
                Text(futureDateStr, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.accent)
            }
        }
    }
}

@Composable
fun AgeCalcScreen(colors: CustomThemeColors) {
    var birthYearText by remember { mutableStateOf("1995") }
    var birthMonthText by remember { mutableStateOf("5") }
    var birthDayText by remember { mutableStateOf("15") }

    val bYear = birthYearText.toIntOrNull() ?: 1995
    val bMonth = birthMonthText.toIntOrNull() ?: 5
    val bDay = birthDayText.toIntOrNull() ?: 15

    val currentYear = 2026
    val currentMonth = 7
    val currentDay = 26

    var ageYears = currentYear - bYear
    var ageMonths = currentMonth - bMonth
    var ageDays = currentDay - bDay

    if (ageDays < 0) {
        ageMonths -= 1
        ageDays += 30
    }
    if (ageMonths < 0) {
        ageYears -= 1
        ageMonths += 12
    }

    val totalDays = ageYears * 365 + ageMonths * 30 + ageDays

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
                Text("🎂 حاسبة العمر الدقيقة والسن الهجري", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.text)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = birthDayText,
                        onValueChange = { birthDayText = it },
                        label = { Text("اليوم") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = birthMonthText,
                        onValueChange = { birthMonthText = it },
                        label = { Text("الشهر") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = birthYearText,
                        onValueChange = { birthYearText = it },
                        label = { Text("السنة") },
                        modifier = Modifier.weight(1.2f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("عمرك بالتفصيل:", fontSize = 13.sp, color = colors.textMuted)
                Text("$ageYears سنة و $ageMonths شهر و $ageDays يوم", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.accent)
                Spacer(modifier = Modifier.height(8.dp))
                Text("• إجمالي الأيام التي عشتها: $totalDays يوم", fontSize = 13.sp, color = colors.text)
                Text("• العمر الهجري التقديري: ${(ageYears * 1.03).toInt()} سنة هجرية", fontSize = 13.sp, color = colors.text)
            }
        }
    }
}

@Composable
fun CountdownScreen(colors: CustomThemeColors) {
    val events = listOf(
        Triple("شهر رمضان المبارك 🌙", "220 يوم", "2027/03/01"),
        Triple("عيد الفطر السعيد 🎉", "250 يوم", "2027/04/01"),
        Triple("عيد الأضحى المبارك 🕋", "315 يوم", "2027/06/05"),
        Triple("رأس السنة الميلادية 🎆", "158 يوم", "2027/01/01")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.appBg)
            .padding(12.dp)
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(events) { (name, count, date) ->
                Surface(
                    color = colors.surface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.text)
                            Text(date, fontSize = 11.sp, color = colors.textMuted)
                        }
                        Text(count, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.accent)
                    }
                }
            }
        }
    }
}

@Composable
fun HealthCalcScreen(colors: CustomThemeColors) {
    var heightText by remember { mutableStateOf("175") }
    var weightText by remember { mutableStateOf("75") }

    val height = heightText.toDoubleOrNull() ?: 175.0
    val weight = weightText.toDoubleOrNull() ?: 75.0

    val heightMeters = height / 100.0
    val bmi = weight / (heightMeters * heightMeters)

    val category = when {
        bmi < 18.5 -> "نقص في الوزن (نحافة)"
        bmi < 25.0 -> "وزن مثالي وصحي ✓"
        bmi < 30.0 -> "زيادة في الوزن"
        else -> "سمنة - يحتاج حمية"
    }

    val categoryColor = when {
        bmi < 18.5 -> Color(0xFFF59E0B)
        bmi < 25.0 -> Color(0xFF10B981)
        else -> Color(0xFFEF4444)
    }

    val bmr = 10 * weight + 6.25 * height - 5 * 25 + 5 // Mifflin-St Jeor formula

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
                Text("❤️ حاسبة كتلة الجسم (BMI) والسعرات", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.text)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = heightText,
                    onValueChange = { heightText = it },
                    label = { Text("الطول (سم)", color = colors.textMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text("الوزن (كيلوجرام)", color = colors.textMuted) },
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
                Text("مؤشر كتلة الجسم (BMI):", fontSize = 13.sp, color = colors.textMuted)
                Text("${String.format("%.1f", bmi)}", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = colors.accent)
                Spacer(modifier = Modifier.height(4.dp))
                Text("التصنيف الصحي: $category", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = categoryColor)
                Spacer(modifier = Modifier.height(8.dp))
                Text("معدل الأيض الأساسي (BMR): ${bmr.toInt()} سعرة حرارية/يوم", fontSize = 12.sp, color = colors.text)
            }
        }
    }
}

@Composable
fun OvulationCalcScreen(colors: CustomThemeColors) {
    var cycleLengthText by remember { mutableStateOf("28") }
    val cycleDays = cycleLengthText.toIntOrNull() ?: 28

    val ovulationDay = cycleDays - 14

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
                Text("🌸 حاسبة حاسبة أيام الإباضة والدورة الشهرية", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.text)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = cycleLengthText,
                    onValueChange = { cycleLengthText = it },
                    label = { Text("طول الدورة الشهرية (بالأيام)", color = colors.textMuted) },
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
                Text("النتائج التقديرية:", fontSize = 13.sp, color = colors.textMuted)
                Spacer(modifier = Modifier.height(4.dp))
                Text("• يوم الإباضة المتوقع: اليوم $ovulationDay من بداية الدورة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.accent)
                Text("• نافذة الخصوبة العالية: من اليوم ${ovulationDay - 2} إلى اليوم ${ovulationDay + 2}", fontSize = 13.sp, color = colors.text)
            }
        }
    }
}
