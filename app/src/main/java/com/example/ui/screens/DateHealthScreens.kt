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

    var searchQuery by remember { mutableStateOf("") }
    val allWorldCities = remember {
        val list = TimeZone.getAvailableIDs().filter { it.contains("/") }.mapNotNull { tzId ->
            val tz = TimeZone.getTimeZone(tzId)
            val name = tzId.substringAfterLast("/").replace("_", " ")
            val gmtOffset = tz.rawOffset / (1000 * 60 * 60f)
            val gmtStr = if (gmtOffset >= 0) "GMT+${gmtOffset.toInt()}" else "GMT${gmtOffset.toInt()}"
            if (name.length > 2) Triple(name, tzId, gmtStr) else null
        }.sortedBy { it.first }.distinctBy { it.first }
        
        // Add some popular ones at the top mapped to arabic
        val arabicPopular = listOf(
            Triple("مكة المكرمة 🕋", "Asia/Riyadh", "GMT+3"),
            Triple("القاهرة 🇪🇬", "Africa/Cairo", "GMT+3"),
            Triple("الرياض 🇸🇦", "Asia/Riyadh", "GMT+3"),
            Triple("دبي 🇦🇪", "Asia/Dubai", "GMT+4"),
            Triple("الكويت 🇰🇼", "Asia/Kuwait", "GMT+3"),
            Triple("الدوحة 🇶🇦", "Asia/Qatar", "GMT+3"),
            Triple("عمان 🇯🇴", "Asia/Amman", "GMT+3"),
            Triple("بغداد 🇮🇶", "Asia/Baghdad", "GMT+3"),
            Triple("دمشق 🇸🇾", "Asia/Damascus", "GMT+3"),
            Triple("بيروت 🇱🇧", "Asia/Beirut", "GMT+2"),
            Triple("القدس 🇵🇸", "Asia/Jerusalem", "GMT+3"),
            Triple("الخرطوم 🇸🇩", "Africa/Khartoum", "GMT+2"),
            Triple("صنعاء 🇾🇪", "Asia/Aden", "GMT+3"),
            Triple("مسقط 🇴🇲", "Asia/Muscat", "GMT+4"),
            Triple("المنامة 🇧🇭", "Asia/Bahrain", "GMT+3"),
            Triple("طرابلس 🇱🇾", "Africa/Tripoli", "GMT+2"),
            Triple("تونس 🇹🇳", "Africa/Tunis", "GMT+1"),
            Triple("الجزائر 🇩🇿", "Africa/Algiers", "GMT+1"),
            Triple("الرباط 🇲🇦", "Africa/Casablanca", "GMT+1"),
            Triple("نواكشوط 🇲🇷", "Africa/Nouakchott", "GMT+0"),
            Triple("لندن 🇬🇧", "Europe/London", "GMT+0"),
            Triple("باريس 🇫🇷", "Europe/Paris", "GMT+1"),
            Triple("نيويورك 🇺🇸", "America/New_York", "GMT-4"),
            Triple("طوكيو 🇯🇵", "Asia/Tokyo", "GMT+9"),
            Triple("سيدني 🇦🇺", "Australia/Sydney", "GMT+10"),
            Triple("موسكو 🇷🇺", "Europe/Moscow", "GMT+3"),
            Triple("بكين 🇨🇳", "Asia/Shanghai", "GMT+8")
        )
        (arabicPopular + list).distinctBy { it.second }
    }
    
    val filteredCities = allWorldCities.filter { 
        it.first.contains(searchQuery, ignoreCase = true) || it.second.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.appBg)
            .padding(12.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("بحث عن دولة أو مدينة...") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.accent,
                focusedLabelColor = colors.accent,
                unfocusedBorderColor = colors.accent.copy(alpha = 0.5f),
                unfocusedLabelColor = colors.textMuted
            ),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
        ) {
            items(filteredCities) { (city, tzId, label) ->
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
    val context = androidx.compose.ui.platform.LocalContext.current
    var startDayText by remember { mutableStateOf("1") }
    var startMonthText by remember { mutableStateOf("7") }
    var startYearText by remember { mutableStateOf("2026") }

    var endDayText by remember { mutableStateOf("5") }
    var endMonthText by remember { mutableStateOf("7") }
    var endYearText by remember { mutableStateOf("2026") }

    var cycleLengthText by remember { mutableStateOf("28") }
    var periodDurationText by remember { mutableStateOf("5") }

    val startDay = startDayText.toIntOrNull() ?: 1
    val startMonth = (startMonthText.toIntOrNull() ?: 7) - 1
    val startYear = startYearText.toIntOrNull() ?: 2026

    val endDay = endDayText.toIntOrNull() ?: 5
    val endMonth = (endMonthText.toIntOrNull() ?: 7) - 1
    val endYear = endYearText.toIntOrNull() ?: 2026

    val cycleDays = cycleLengthText.toIntOrNull() ?: 28
    val periodDays = periodDurationText.toIntOrNull() ?: 5

    // Calculate dates using Calendar
    val startDateCal = Calendar.getInstance().apply {
        set(startYear, startMonth, startDay)
    }

    val endDateCal = Calendar.getInstance().apply {
        set(endYear, endMonth, endDay)
    }

    // Next period start date = start date + cycleDays
    val nextPeriodCal = (startDateCal.clone() as Calendar).apply {
        add(Calendar.DAY_OF_YEAR, cycleDays)
    }

    // Ovulation day = next period start - 14 days (or start date + cycleDays - 14)
    val ovulationCal = (startDateCal.clone() as Calendar).apply {
        add(Calendar.DAY_OF_YEAR, cycleDays - 14)
    }

    // Fertile window: 5 days before ovulation up to ovulation day
    val fertileStartCal = (ovulationCal.clone() as Calendar).apply {
        add(Calendar.DAY_OF_YEAR, -5)
    }
    val fertileEndCal = (ovulationCal.clone() as Calendar).apply {
        add(Calendar.DAY_OF_YEAR, 1)
    }

    val sdf = SimpleDateFormat("yyyy/MM/dd (EEEE)", Locale("ar"))
    val nextPeriodStr = sdf.format(nextPeriodCal.time)
    val ovulationStr = sdf.format(ovulationCal.time)
    val fertileStartStr = SimpleDateFormat("yyyy/MM/dd", Locale("ar")).format(fertileStartCal.time)
    val fertileEndStr = SimpleDateFormat("yyyy/MM/dd", Locale("ar")).format(fertileEndCal.time)

    // Calculate actual period bleeding duration entered by user
    // diff between start and end
    val diffMillis = endDateCal.timeInMillis - startDateCal.timeInMillis
    val calculatedPeriodLen = if (diffMillis >= 0) (diffMillis / (1000 * 60 * 60 * 24)).toInt() + 1 else periodDays

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.appBg)
            .padding(12.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Surface(
                    color = colors.surface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("🌸 حاسبة الإباضة والدورة الشهرية المخصصة", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.text)
                        Text("أدخلي تواريخ دورتك وطولها لحساب أيام التبويض والخصوبة بدقة:", fontSize = 12.sp, color = colors.textMuted)
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📅 تاريخ بدء الدورة الحالية:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.text)
                            TextButton(onClick = {
                                try {
                                    android.app.DatePickerDialog(
                                        context,
                                        { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                                            startDayText = selectedDayOfMonth.toString()
                                            startMonthText = (selectedMonth + 1).toString()
                                            startYearText = selectedYear.toString()
                                        },
                                        startYear,
                                        startMonth,
                                        startDay
                                    ).show()
                                } catch (_: Throwable) {}
                            }) {
                                Text("📅 اختيار من التقويم", color = colors.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = startDayText,
                                onValueChange = { startDayText = it },
                                label = { Text("اليوم") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = startMonthText,
                                onValueChange = { startMonthText = it },
                                label = { Text("الشهر") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = startYearText,
                                onValueChange = { startYearText = it },
                                label = { Text("السنة") },
                                modifier = Modifier.weight(1.2f),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🛑 تاريخ آخر دورة انتهت:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.text)
                            TextButton(onClick = {
                                try {
                                    android.app.DatePickerDialog(
                                        context,
                                        { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                                            endDayText = selectedDayOfMonth.toString()
                                            endMonthText = (selectedMonth + 1).toString()
                                            endYearText = selectedYear.toString()
                                        },
                                        endYear,
                                        endMonth,
                                        endDay
                                    ).show()
                                } catch (_: Throwable) {}
                            }) {
                                Text("📅 اختيار من التقويم", color = colors.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = endDayText,
                                onValueChange = { endDayText = it },
                                label = { Text("اليوم") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = endMonthText,
                                onValueChange = { endMonthText = it },
                                label = { Text("الشهر") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = endYearText,
                                onValueChange = { endYearText = it },
                                label = { Text("السنة") },
                                modifier = Modifier.weight(1.2f),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = cycleLengthText,
                                onValueChange = { cycleLengthText = it },
                                label = { Text("طول الدورة (تأتي كل كام يوم؟)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = periodDurationText,
                                onValueChange = { periodDurationText = it },
                                label = { Text("مدة الطمث (أيام النزول)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }
                }
            }

            item {
                Surface(
                    color = colors.surface2,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("📊 النتائج والتواريخ المتوقعة لدورتك:", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.accent)
                        
                        Divider(color = colors.border.copy(alpha = 0.3f))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("مدة الطمث الفعلية:", fontSize = 13.sp, color = colors.textMuted)
                            Text("$calculatedPeriodLen أيام", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.text)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("طول الدورة الكلي:", fontSize = 13.sp, color = colors.textMuted)
                            Text("$cycleDays يوم", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.text)
                        }

                        Divider(color = colors.border.copy(alpha = 0.3f))

                        Column {
                            Text("🥚 يوم الإباضة المتوقع:", fontSize = 13.sp, color = colors.textMuted)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(ovulationStr, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.accent)
                        }

                        Column {
                            Text("✨ نافذة الخصوبة العالية (أعلى فرصة للحمل):", fontSize = 13.sp, color = colors.textMuted)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("من $fertileStartStr إلى $fertileEndStr", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.text)
                        }

                        Column {
                            Text("🗓️ موعد الدورة القادمة المتوقع:", fontSize = 13.sp, color = colors.textMuted)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(nextPeriodStr, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.accent)
                        }
                    }
                }
            }
        }
    }
}
