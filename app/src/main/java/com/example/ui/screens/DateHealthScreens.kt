package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.*
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
import com.example.ui.components.ToolScreenScaffold
import com.example.ui.theme.AppIcons
import com.example.ui.theme.Spacing
import com.example.model.CalcKey
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.border

@Composable
fun WorldTimeScreen(colors: CustomThemeColors) {
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
        
        val arabicPopular = listOf(
            Triple("مكة المكرمة", "Asia/Riyadh", "GMT+3"),
            Triple("القاهرة", "Africa/Cairo", "GMT+3"),
            Triple("الرياض", "Asia/Riyadh", "GMT+3"),
            Triple("دبي", "Asia/Dubai", "GMT+4"),
            Triple("الكويت", "Asia/Kuwait", "GMT+3"),
            Triple("الدوحة", "Asia/Qatar", "GMT+3"),
            Triple("عمان", "Asia/Amman", "GMT+3"),
            Triple("بغداد", "Asia/Baghdad", "GMT+3"),
            Triple("دمشق", "Asia/Damascus", "GMT+3"),
            Triple("بيروت", "Asia/Beirut", "GMT+2"),
            Triple("القدس", "Asia/Jerusalem", "GMT+3"),
            Triple("الخرطوم", "Africa/Khartoum", "GMT+2"),
            Triple("صنعاء", "Asia/Aden", "GMT+3"),
            Triple("مسقط", "Asia/Muscat", "GMT+4"),
            Triple("المنامة", "Asia/Bahrain", "GMT+3"),
            Triple("طرابلس", "Africa/Tripoli", "GMT+2"),
            Triple("تونس", "Africa/Tunis", "GMT+1"),
            Triple("الجزائر", "Africa/Algiers", "GMT+1"),
            Triple("الرباط", "Africa/Casablanca", "GMT+1"),
            Triple("نواكشوط", "Africa/Nouakchott", "GMT+0"),
            Triple("لندن", "Europe/London", "GMT+0"),
            Triple("باريس", "Europe/Paris", "GMT+1"),
            Triple("نيويورك", "America/New_York", "GMT-4"),
            Triple("طوكيو", "Asia/Tokyo", "GMT+9"),
            Triple("سيدني", "Australia/Sydney", "GMT+10"),
            Triple("موسكو", "Europe/Moscow", "GMT+3"),
            Triple("بكين", "Asia/Shanghai", "GMT+8")
        )
        (arabicPopular + list).distinctBy { it.second }
    }
    
    val filteredCities = allWorldCities.filter { 
        it.first.contains(searchQuery, ignoreCase = true) || it.second.contains(searchQuery, ignoreCase = true)
    }

    ToolScreenScaffold(
        colors = colors,
        icon = AppIcons.forCalc(CalcKey.WORLD_TIME),
        title = "التوقيت العالمي",
        subtitle = "متابعة الوقت الحالي في مختلف مدن وعواصم العالم"
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("بحث عن مدينة أو دولة...", color = colors.textMuted) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.accent,
                unfocusedBorderColor = colors.border,
                focusedTextColor = colors.text,
                unfocusedTextColor = colors.text
            ),
            trailingIcon = { Icon(Icons.Filled.Search, null, tint = colors.textMuted) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        filteredCities.forEach { (city, tzId, label) ->
            val tz = TimeZone.getTimeZone(tzId)
            val sdf = SimpleDateFormat("hh:mm:ss a", Locale("ar"))
            sdf.timeZone = tz
            val timeStr = sdf.format(Date(currentTimeMillis))

            Surface(
                color = colors.surface,
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(city, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.text)
                        Text(label, fontSize = 12.sp, color = colors.textMuted)
                    }
                    Text(timeStr, fontWeight = FontWeight.Black, fontSize = 20.sp, color = colors.accent)
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

    ToolScreenScaffold(
        colors = colors,
        icon = AppIcons.forCalc(CalcKey.DATE),
        title = "حاسبة التاريخ",
        subtitle = "احسب التواريخ المستقبلية أو الماضية بإضافة أو طرح عدد من الأيام"
    ) {
        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("التاريخ المتوقع", color = colors.textMuted, fontSize = 14.sp)
                Text(
                    futureDateStr,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.accent,
                    textAlign = TextAlign.Center
                )
                Text(
                    "بعد إضافة $days يوم",
                    fontSize = 12.sp,
                    color = colors.textMuted,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = daysToAdd,
            onValueChange = { daysToAdd = it },
            label = { Text("عدد الأيام المراد إضافتها") },
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
fun AgeCalcScreen(colors: CustomThemeColors) {
    var birthYearText by remember { mutableStateOf("1995") }
    var birthMonthText by remember { mutableStateOf("5") }
    var birthDayText by remember { mutableStateOf("15") }

    val bYear = birthYearText.toIntOrNull() ?: 1995
    val bMonth = birthMonthText.toIntOrNull() ?: 5
    val bDay = birthDayText.toIntOrNull() ?: 15

    val calendar = Calendar.getInstance()
    val currentYear = calendar.get(Calendar.YEAR)
    val currentMonth = calendar.get(Calendar.MONTH) + 1
    val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

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

    ToolScreenScaffold(
        colors = colors,
        icon = AppIcons.forCalc(CalcKey.AGE),
        title = "حاسبة العمر",
        subtitle = "احسب عمرك بالتفصيل بالسنوات والشهور والأيام"
    ) {
        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("عمرك الآن", color = colors.textMuted, fontSize = 14.sp)
                Text(
                    "$ageYears سنة و $ageMonths شهر",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.accent
                )
                Text("$ageDays يوم", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.text)
                
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = colors.border.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("إجمالي الأيام", fontSize = 11.sp, color = colors.textMuted)
                        Text("$totalDays", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.text)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("العمر الهجري", fontSize = 11.sp, color = colors.textMuted)
                        Text("${(ageYears * 1.03).toInt()}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.text)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("تاريخ الميلاد", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.text, modifier = Modifier.padding(start = 4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = birthDayText,
                onValueChange = { birthDayText = it },
                label = { Text("يوم") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp)
            )
            OutlinedTextField(
                value = birthMonthText,
                onValueChange = { birthMonthText = it },
                label = { Text("شهر") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp)
            )
            OutlinedTextField(
                value = birthYearText,
                onValueChange = { birthYearText = it },
                label = { Text("سنة") },
                modifier = Modifier.weight(1.5f),
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
fun CountdownScreen(colors: CustomThemeColors) {
    val events = listOf(
        Triple("شهر رمضان المبارك", "220 يوم", "2027/03/01"),
        Triple("عيد الفطر السعيد", "250 يوم", "2027/04/01"),
        Triple("عيد الأضحى المبارك", "315 يوم", "2027/06/05"),
        Triple("رأس السنة الميلادية", "158 يوم", "2027/01/01")
    )

    ToolScreenScaffold(
        colors = colors,
        icon = AppIcons.forCalc(CalcKey.COUNTDOWN),
        title = "العد التنازلي",
        subtitle = "متابعة الوقت المتبقي للمناسبات الدينية والوطنية الهامة"
    ) {
        events.forEach { (name, count, date) ->
            Surface(
                color = colors.surface,
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.text)
                        Text(date, fontSize = 12.sp, color = colors.textMuted)
                    }
                    Text(count, fontWeight = FontWeight.Black, fontSize = 18.sp, color = colors.accent)
                }
            }
        }
    }
}

@Composable
fun HealthCalcScreen(colors: CustomThemeColors) {
    UltimateHealthDashboard(colors = colors)
}

@Composable
fun OvulationCalcScreen(colors: CustomThemeColors) {
    UltimateHealthDashboard(colors = colors)
}
