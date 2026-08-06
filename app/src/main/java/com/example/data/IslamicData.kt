package com.example.data

import android.content.Context
import org.json.JSONArray

data class CityPrayerInfo(
    val nameAr: String,
    val nameEn: String,
    val countryAr: String,
    val lat: Double,
    val lng: Double,
    val timezone: String,
    val fajr: String,
    val sunrise: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String
)

data class DhikrItem(
    val id: Int,
    val text: String,
    val countTarget: Int,
    val rewardText: String,
    val reference: String = ""
)

data class SurahInfo(
    val number: Int,
    val nameAr: String,
    val nameEn: String,
    val totalVerses: Int,
    val place: String, // "مكية" or "مدنية"
    val startPage: Int,
    val verses: List<String> = emptyList()
)

object IslamicData {

    // SharedPreferences for Custom Dhikrs in Tasbih
    private const val PREFS_TASBIH = "clevcalc_tasbih_prefs"
    private const val KEY_CUSTOM_DHIKRS = "custom_dhikrs_json"
    private const val KEY_LIFETIME_COUNT = "lifetime_counter"

    fun getCustomDhikrs(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_TASBIH, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_CUSTOM_DHIKRS, "[]") ?: "[]"
        val list = mutableListOf<String>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun addCustomDhikr(context: Context, dhikr: String) {
        val trimmed = dhikr.trim()
        if (trimmed.isBlank()) return
        val current = getCustomDhikrs(context).toMutableList()
        if (!current.contains(trimmed)) {
            current.add(trimmed)
            saveCustomDhikrs(context, current)
        }
    }

    fun deleteCustomDhikr(context: Context, dhikr: String) {
        val current = getCustomDhikrs(context).toMutableList()
        if (current.remove(dhikr)) {
            saveCustomDhikrs(context, current)
        }
    }

    private fun saveCustomDhikrs(context: Context, list: List<String>) {
        val prefs = context.getSharedPreferences(PREFS_TASBIH, Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        list.forEach { jsonArray.put(it) }
        prefs.edit().putString(KEY_CUSTOM_DHIKRS, jsonArray.toString()).apply()
    }

    fun getLifetimeCount(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_TASBIH, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_LIFETIME_COUNT, 0)
    }

    fun incrementLifetimeCount(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_TASBIH, Context.MODE_PRIVATE)
        val current = prefs.getInt(KEY_LIFETIME_COUNT, 0)
        prefs.edit().putInt(KEY_LIFETIME_COUNT, current + 1).apply()
    }

    val cities = listOf(
        CityPrayerInfo("مكة المكرمة", "Makkah", "السعودية", 21.4225, 39.8262, "Asia/Riyadh", "04:38", "05:58", "12:22", "15:42", "18:46", "20:16"),
        CityPrayerInfo("المدينة المنورة", "Madinah", "السعودية", 24.4672, 39.6111, "Asia/Riyadh", "04:34", "05:56", "12:23", "15:46", "18:49", "20:19"),
        CityPrayerInfo("الرياض", "Riyadh", "السعودية", 24.7136, 46.6753, "Asia/Riyadh", "04:05", "05:28", "11:58", "15:23", "18:27", "19:57"),
        CityPrayerInfo("جدة", "Jeddah", "السعودية", 21.5433, 39.1728, "Asia/Riyadh", "04:40", "06:00", "12:24", "15:44", "18:48", "20:18"),
        CityPrayerInfo("القاهرة", "Cairo", "مصر", 30.0444, 31.2357, "Africa/Cairo", "04:32", "06:02", "12:58", "16:32", "19:53", "21:23"),
        CityPrayerInfo("الإسكندرية", "Alexandria", "مصر", 31.2001, 29.9187, "Africa/Cairo", "04:34", "06:06", "13:03", "16:39", "19:59", "21:30"),
        CityPrayerInfo("المنصورة", "Mansoura", "مصر", 31.0409, 31.3785, "Africa/Cairo", "04:30", "06:01", "12:58", "16:33", "19:54", "21:25"),
        CityPrayerInfo("أسيوط", "Asyut", "مصر", 27.1810, 31.1837, "Africa/Cairo", "04:37", "06:04", "12:58", "16:28", "19:51", "21:18"),
        CityPrayerInfo("أسوان", "Aswan", "مصر", 24.0889, 32.8998, "Africa/Cairo", "04:36", "06:00", "12:51", "16:17", "19:42", "21:07"),
        CityPrayerInfo("دبي", "Dubai", "الإمارات", 25.2048, 55.2708, "Asia/Dubai", "04:12", "05:35", "12:22", "15:46", "19:08", "20:38"),
        CityPrayerInfo("أبوظبي", "Abu Dhabi", "الإمارات", 24.4539, 54.3773, "Asia/Dubai", "04:17", "05:39", "12:26", "15:49", "19:12", "20:42"),
        CityPrayerInfo("الشارقة", "Sharjah", "الإمارات", 25.3463, 55.4209, "Asia/Dubai", "04:11", "05:34", "12:21", "15:45", "19:07", "20:37"),
        CityPrayerInfo("عمان", "Amman", "الأردن", 31.9454, 35.9284, "Asia/Amman", "04:22", "05:54", "12:43", "16:21", "19:32", "21:02"),
        CityPrayerInfo("القدس الشريف", "Jerusalem", "فلسطين", 31.7683, 35.2137, "Asia/Jerusalem", "04:24", "05:56", "12:45", "16:23", "19:34", "21:04"),
        CityPrayerInfo("غزة", "Gaza", "فلسطين", 31.5017, 34.4668, "Asia/Gaza", "04:26", "05:58", "12:48", "16:26", "19:37", "21:07"),
        CityPrayerInfo("الكويت", "Kuwait City", "الكويت", 29.3759, 47.9774, "Asia/Kuwait", "03:48", "05:14", "11:54", "15:28", "18:34", "20:04"),
        CityPrayerInfo("بغداد", "Baghdad", "العراق", 33.3152, 44.3661, "Asia/Baghdad", "03:52", "05:24", "12:08", "15:48", "18:52", "20:24"),
        CityPrayerInfo("الدوحة", "Doha", "قطر", 25.2854, 51.5310, "Asia/Qatar", "03:42", "05:07", "11:47", "15:10", "18:27", "19:57"),
        CityPrayerInfo("مسقط", "Muscat", "عمان", 23.5880, 58.3829, "Asia/Muscat", "04:06", "05:27", "12:12", "15:33", "18:57", "20:17"),
        CityPrayerInfo("المنامة", "Manama", "البحرين", 26.2285, 50.5860, "Asia/Bahrain", "03:46", "05:11", "11:51", "15:15", "18:31", "20:01"),
        CityPrayerInfo("صنعاء", "Sanaa", "اليمن", 15.3694, 44.1910, "Asia/Aden", "04:28", "05:46", "12:15", "15:31", "18:43", "20:01"),
        CityPrayerInfo("دمشق", "Damascus", "سوريا", 33.5138, 36.2765, "Asia/Damascus", "04:12", "05:45", "12:38", "16:18", "19:30", "21:02"),
        CityPrayerInfo("بيروت", "Beirut", "لبنان", 33.8938, 35.5018, "Asia/Beirut", "04:15", "05:48", "12:41", "16:21", "19:33", "21:05"),
        CityPrayerInfo("الخرطوم", "Khartoum", "السودان", 15.5007, 32.5599, "Africa/Khartoum", "04:42", "06:01", "12:28", "15:46", "18:54", "20:12"),
        CityPrayerInfo("طرابلس", "Tripoli", "ليبيا", 32.8872, 13.1913, "Africa/Tripoli", "04:38", "06:10", "13:12", "16:50", "20:12", "21:44"),
        CityPrayerInfo("تونس", "Tunis", "تونس", 36.8065, 10.1815, "Africa/Tunis", "03:58", "05:32", "12:30", "16:15", "19:28", "21:02"),
        CityPrayerInfo("الجزائر", "Algiers", "الجزائر", 36.7538, 3.0588, "Africa/Algiers", "04:12", "05:48", "12:52", "16:38", "19:55", "21:31"),
        CityPrayerInfo("الرباط", "Rabat", "المغرب", 34.0209, -6.8416, "Africa/Casablanca", "04:38", "06:12", "13:30", "17:10", "20:47", "22:18"),
        CityPrayerInfo("نواكشوط", "Nouakchott", "موريتانيا", 18.0735, -15.9582, "Africa/Nouakchott", "05:12", "06:34", "13:10", "16:28", "19:45", "21:05"),
        CityPrayerInfo("إسطنبول", "Istanbul", "تركيا", 41.0082, 28.9784, "Europe/Istanbul", "03:56", "05:42", "13:12", "17:08", "20:32", "22:12"),
        CityPrayerInfo("لندن", "London", "بريطانيا", 51.5074, -0.1278, "Europe/London", "03:12", "05:14", "13:08", "17:22", "21:02", "22:52"),
        CityPrayerInfo("باريس", "Paris", "فرنسا", 48.8566, 2.3522, "Europe/Paris", "03:45", "05:48", "13:42", "17:50", "21:35", "23:25"),
        CityPrayerInfo("برلين", "Berlin", "ألمانيا", 52.5200, 13.4050, "Europe/Berlin", "03:10", "05:12", "13:15", "17:30", "21:18", "23:12"),
        CityPrayerInfo("نيويورك", "New York", "أمريكا", 40.7128, -74.0060, "America/New_York", "04:12", "05:52", "13:02", "16:54", "20:12", "21:52"),
        CityPrayerInfo("تورونتو", "Toronto", "كندا", 43.6532, -79.3832, "America/Toronto", "04:08", "05:50", "13:15", "17:10", "20:38", "22:18"),
        CityPrayerInfo("جاكرتا", "Jakarta", "إندونيسيا", -6.2088, 106.8456, "Asia/Jakarta", "04:35", "05:52", "11:54", "15:16", "17:55", "19:08"),
        CityPrayerInfo("كوالالمبور", "Kuala Lumpur", "ماليزيا", 3.1390, 101.6869, "Asia/Kuala_Lumpur", "05:48", "07:05", "13:15", "16:40", "19:24", "20:36")
    )


    data class DynamicPrayerTimes(
        val fajr: String,
        val sunrise: String,
        val dhuhr: String,
        val asr: String,
        val maghrib: String,
        val isha: String
    )

    fun calculatePrayerTimes(
        lat: Double,
        lng: Double,
        timezoneOffsetHours: Double,
        calendar: java.util.Calendar = java.util.Calendar.getInstance()
    ): DynamicPrayerTimes {
        val dayOfYear = calendar.get(java.util.Calendar.DAY_OF_YEAR)
        val year = calendar.get(java.util.Calendar.YEAR)
        val isLeap = (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
        val daysInYear = if (isLeap) 366.0 else 365.0

        val gamma = 2.0 * Math.PI / daysInYear * (dayOfYear - 1)

        val decl = 0.006918 - 0.399912 * Math.cos(gamma) + 0.070257 * Math.sin(gamma) -
                0.006758 * Math.cos(2 * gamma) + 0.000907 * Math.sin(2 * gamma) -
                0.002697 * Math.cos(3 * gamma) + 0.00148 * Math.sin(3 * gamma)

        val eqTime = 229.18 * (0.000075 + 0.001868 * Math.cos(gamma) - 0.032077 * Math.sin(gamma) -
                0.014615 * Math.cos(2 * gamma) - 0.040849 * Math.sin(2 * gamma))

        val noon = 12.0 + timezoneOffsetHours - (lng / 15.0) - (eqTime / 60.0)
        val latRad = Math.toRadians(lat)

        fun hourAngle(angleBelowHorizon: Double): Double {
            val zenithRad = Math.toRadians(90.0 + angleBelowHorizon)
            val cosH = (Math.cos(zenithRad) - Math.sin(latRad) * Math.sin(decl)) / (Math.cos(latRad) * Math.cos(decl))
            val clamped = cosH.coerceIn(-1.0, 1.0)
            return Math.toDegrees(Math.acos(clamped))
        }

        val hSunrise = hourAngle(0.833) / 15.0
        val sunriseVal = noon - hSunrise
        val sunsetVal = noon + hSunrise

        val hFajr = hourAngle(19.5) / 15.0
        val fajrVal = noon - hFajr

        val latMinusDecl = Math.abs(latRad - decl)
        val asrAngleRad = Math.atan(1.0 / (1.0 + Math.tan(latMinusDecl)))
        val asrZenithDeg = 90.0 - Math.toDegrees(asrAngleRad)
        val hAsr = hourAngle(asrZenithDeg - 90.0) / 15.0
        val asrVal = noon + hAsr

        val maghribVal = sunsetVal

        val hIsha = hourAngle(17.5) / 15.0
        val ishaVal = noon + hIsha

        fun formatHours(hours: Double): String {
            val hNorm = (hours % 24.0 + 24.0) % 24.0
            var hrs = Math.floor(hNorm).toInt()
            var mins = Math.round((hNorm - Math.floor(hNorm)) * 60.0).toInt()
            if (mins >= 60) {
                hrs = (hrs + 1) % 24
                mins = 0
            }
            return String.format("%02d:%02d", hrs, mins)
        }

        return DynamicPrayerTimes(
            fajr = formatHours(fajrVal),
            sunrise = formatHours(sunriseVal),
            dhuhr = formatHours(noon),
            asr = formatHours(asrVal),
            maghrib = formatHours(maghribVal),
            isha = formatHours(ishaVal)
        )
    }

    fun getCorrectTimezoneOffset(lat: Double, lng: Double, calendar: java.util.Calendar = java.util.Calendar.getInstance()): Double {
        val timeMillis = calendar.timeInMillis

        // 1. Try to match nearest city from our comprehensive cities database
        val matchedCity = cities.minByOrNull { city ->
            val dLat = city.lat - lat
            val dLng = city.lng - lng
            dLat * dLat + dLng * dLng
        }

        if (matchedCity != null) {
            val distSq = (matchedCity.lat - lat) * (matchedCity.lat - lat) + (matchedCity.lng - lng) * (matchedCity.lng - lng)
            if (distSq < 16.0) { // Within ~400km of a known city
                try {
                    val tz = java.util.TimeZone.getTimeZone(matchedCity.timezone)
                    return tz.getOffset(timeMillis) / 3600000.0
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // 2. Check if location is in Egypt (lat 22 to 32, lng 25 to 37)
        val isEgypt = (lat in 22.0..32.0 && lng in 25.0..37.0)
        if (isEgypt) {
            return try {
                val tzEgypt = java.util.TimeZone.getTimeZone("Africa/Cairo")
                tzEgypt.getOffset(timeMillis) / 3600000.0
            } catch (e: Exception) {
                val year = calendar.get(java.util.Calendar.YEAR)
                val dstStart = getLastFridayOf(year, java.util.Calendar.APRIL)
                val dstEnd = getLastThursdayOf(year, java.util.Calendar.OCTOBER)
                if (timeMillis >= dstStart.timeInMillis && timeMillis <= dstEnd.timeInMillis) {
                    3.0 // UTC+3 Summer Time in Egypt
                } else {
                    2.0 // UTC+2 Winter Time in Egypt
                }
            }
        }

        // 3. Fallback: Estimate standard offset from longitude meridian (15 degrees per hour)
        return Math.round(lng / 15.0).toDouble()
    }

    private fun getLastFridayOf(year: Int, month: Int): java.util.Calendar {
        val cal = java.util.Calendar.getInstance()
        cal.set(year, month + 1, 1, 0, 0, 0)
        cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
        while (cal.get(java.util.Calendar.DAY_OF_WEEK) != java.util.Calendar.FRIDAY) {
            cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
        }
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        return cal
    }

    private fun getLastThursdayOf(year: Int, month: Int): java.util.Calendar {
        val cal = java.util.Calendar.getInstance()
        cal.set(year, month + 1, 1, 23, 59, 59)
        cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
        while (cal.get(java.util.Calendar.DAY_OF_WEEK) != java.util.Calendar.THURSDAY) {
            cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
        }
        cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
        return cal
    }

    fun getTimezoneOffsetForCity(timezoneId: String): Double {
        return try {
            val tz = java.util.TimeZone.getTimeZone(timezoneId)
            tz.getOffset(System.currentTimeMillis()) / 3600000.0
        } catch (e: Exception) {
            3.0
        }
    }

    fun getDynamicPrayerTimesForCity(city: CityPrayerInfo, calendar: java.util.Calendar = java.util.Calendar.getInstance()): DynamicPrayerTimes {
        val tzOffset = getCorrectTimezoneOffset(city.lat, city.lng, calendar)
        return calculatePrayerTimes(city.lat, city.lng, tzOffset, calendar)
    }

    val morningAdhkar = listOf(
        DhikrItem(1, "أَعُوذُ بِاللَّهِ مِنَ الشَّيْطَانِ الرَّجِيمِ\n\nاللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ ۚ لَهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ ۗ مَن ذَا الَّذِي يَشْفَعُ عِندَهُ إِلَّا بِإِذْنِهِ ۚ يَعْلَمُ مَا بَيْنَ أَيْدِيهِمْ وَمَا خَلْفَهُمْ ۖ وَلَا يُحِيطُونَ بِشَيْءٍ مِّنْ عِلْمِهِ إِلَّا بِمَا شَاءَ ۚ وَسِعَ كُرْسِيُّهُ السَّمَاوَاتِ وَالْأَرْضَ ۖ وَلَا يَئُودُهُ حِفْظُهُمَا ۚ وَهُوَ الْعَلِيُّ الْعَظِيمُ", 1, "من قالها حين يصبح أوجير من الجن حتى يمسي", "الحاكم، صحيح الترغيب"),
        DhikrItem(2, "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ\n\nقُلْ هُوَ اللَّهُ أَحَدٌ ۝ اللَّهُ الصَّمَدُ ۝ لَمْ يَلِدْ وَلَمْ يُولَدْ ۝ وَلَمْ يَكُن لَّهُ كُفُوًا أَحَدٌ", 3, "كفته من كل شيء", "أبو داود والترمذي"),
        DhikrItem(3, "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ\n\nقُلْ أَعُوذُ بِرَبِّ الْفَلَقِ ۝ مِن شَرِّ مَا خَلَقَ ۝ وَمِن شَرِّ غَاسِقٍ إِذَا وَقَبَ ۝ وَمِن شَرِّ النَّفَّاثَاتِ فِي الْعُقَدِ ۝ وَمِن شَرِّ حَاسِدٍ إِذَا حَسَدَ", 3, "سورة الفلق", "أبو داود والترمذي"),
        DhikrItem(4, "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ\n\nقُلْ أَعُوذُ بِرَبِّ النَّاسِ ۝ مَلِكِ النَّاسِ ۝ إِلَٰهِ النَّاسِ ۝ مِن شَرِّ الْوَسْوَاسِ الْخَنَّاسِ ۝ الَّذِي يُوَسْوِسُ فِي صُدُورِ النَّاسِ ۝ مِنَ الْجِنَّةِ وَالنَّاسِ", 3, "سورة الناس", "أبو داود والترمذي"),
        DhikrItem(5, "أَصْبَحْنَا وَأَصْبَحَ الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ، لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ، وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ.\nرَبِّ أَسْأَلُكَ خَيْرَ مَا فِي هَذَا الْيَوْمِ وَخَيْرَ مَا بَعْدَهُ، وَأَعُوذُ بِكَ مِنْ شَرِّ مَا فِي هَذَا الْيَوْمِ وَشَرِّ مَا بَعْدَهُ، رَبِّ أَعُوذُ بِكَ مِنَ الْكَسَلِ وَسُوءِ الْكِبَرِ، رَبِّ أَعُوذُ بِكَ مِنْ عَذَابٍ فِي النَّارِ وَعَذَابٍ فِي الْقَبْرِ.", 1, "الحماية والتوكل على الله في اليوم", "مسلم"),
        DhikrItem(6, "اللَّهُمَّ بِكَ أَصْبَحْنَا، وَبِكَ أَمْسَيْنَا، وَبِكَ نَحْيَا، وَبِكَ نَمُوتُ، وَإِلَيْكَ النُّشُورُ.", 1, "الإقرار بنعم الله والشكر عليها", "الترمذي"),
        DhikrItem(7, "اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَٰهَ إِلَّا أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ، وَأَنَا عَلَى عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ، أَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ، أَبُوءُ لَكَ بِنِعْمَتِكَ عَلَيَّ، وَأَبُوءُ بِذَنْبِي فَاغْفِرْ لِي، فَإِنَّهُ لَا يَغْفِرُ الذُّنُوبَ إِلَّا أَنْتَ.", 1, "سيد الاستغفار - من قالها موقناً بها فمات من يومه دخل الجنة", "البخاري"),
        DhikrItem(8, "اللَّهُمَّ إِنِّي أَصْبَحْتُ أُشْهِدُكَ وَأُشْهِدُ حَمَلَةَ عَرْشِكَ، وَمَلَائِكَتَكَ، وَجَمِيعَ خَلْقِكَ، أَنَّكَ أَنْتَ اللَّهُ لَا إِلَٰهَ إِلَّا أَنْتَ وَحْدَكَ لَا شَرِيكَ لَكَ، وَأَنَّ مُحَمَّدًا عَبْدُكَ وَرَسُولُكَ.", 4, "من قالها أربعة أعتقه الله من النار", "أبو داود"),
        DhikrItem(9, "اللَّهُمَّ مَا أَصْبَحَ بِي مِنْ نِعْمَةٍ أَوْ بِأَحَدٍ مِنْ خَلْقِكَ، فَمِنْكَ وَحْدَكَ لَا شَرِيكَ لَكَ، فَلَكَ الْحَمْدُ وَلَكَ الشُّكْرُ.", 1, "من قالها أدى شكر يومه", "أبو داود"),
        DhikrItem(10, "اللَّهُمَّ عَافِنِي فِي بَدَنِي، اللَّهُمَّ عَافِنِي فِي سَمْعِي، اللَّهُمَّ عَافِنِي فِي بَصَرِي، لَا إِلَٰهَ إِلَّا أَنْتَ.\nاللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْكُفْرِ وَالْفَقْرِ، وَأَعُوذُ بِكَ مِنْ عَذَابِ الْقَبْرِ، لَا إِلَٰهَ إِلَّا أَنْتَ.", 3, "عافية البدَن والسلامة من الفقر والعذاب", "أبو داود"),
        DhikrItem(11, "حَسْبِيَ اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ عَلَيْهِ تَوَكَّلْتُ وَهُوَ رَبُّ الْعَرْشِ الْعَظِيمِ.", 7, "من قالها سبعاً كفاه الله ما أهمه من أمر الدنيا والآخرة", "أبو داود"),
        DhikrItem(12, "اللَّهُمَّ إِنِّي أَسْأَلُكَ الْعَفْوَ وَالْعَافِيَةَ فِي الدُّنْيَا وَالْآخِرَةِ، اللَّهُمَّ إِنِّي أَسْأَلُكَ الْعَفْوَ وَالْعَافِيَةَ فِي دِينِي وَدُنْيَايَ وَأَهْلِي وَمَالِي، اللَّهُمَّ اسْتُرْ عَوْرَاتِي وَآمِنْ رَوْعَاتِي، اللَّهُمَّ احْفَظْنِي مِنْ بَيْنِ يَدَيَّ وَمِنْ خَلْفِي وَعَنْ يَمِينِي وَعَنْ شِمَالِي وَمِنْ فَوْقِي، وَأَعُوذُ بِعَظَمَتِكَ أَنْ أُغْتَالَ مِنْ تَحْتِي.", 1, "الدعاء الجامع للعافية في الدين والدنيا والمال", "أبو داود وابن ماجه"),
        DhikrItem(13, "اللَّهُمَّ عَالِمَ الْغَيْبِ وَالشَّهَادَةِ فَاطِرَ السَّمَاوَاتِ وَالْأَرْضِ، رَبَّ كُلِّ شَيْءٍ وَمَلِيكَهُ، أَشْهَدُ أَنْ لَا إِلَٰهَ إِلَّا أَنْتَ، أَعُوذُ بِكَ مِنْ شَرِّ نَفْسِي وَمِنْ شَرِّ الشَّيْطَانِ وَشِرْكِهِ، وَأَنْ أَقْتَرِفَ عَلَى نَفْسِي سُوءًا أَوْ أَجُرَّهُ إِلَى مُسْلِمٍ.", 1, "الحفظ من الشرور والآثام", "الترمذي"),
        DhikrItem(14, "بِسْمِ اللَّهِ الَّذِي لَا يَضُرُّ مَعَ اسْمِهِ شَيْءٌ فِي الْأَرْضِ وَلَا فِي السَّمَاءِ، وَهُوَ السَّمِيعُ الْعَلِيمُ.", 3, "من قالها ثلاثاً لم يضره شيء", "أبو داود والترمذي"),
        DhikrItem(15, "رَضِيتُ بِاللَّهِ رَبًّا، وَبِالْإِسْلَامِ دِينًا، وَبِمُحَمَّدٍ ﷺ نَبِيًّا.", 3, "كان حقاً على الله أن يرضيه يوم القيامة", "أحمد والترمذي"),
        DhikrItem(16, "يَا حَيُّ يَا قَيُّومُ بِرَحْمَتِكَ أَسْتَغِيثُ، أَصْلِحْ لِي شَأْنِي كُلَّهُ، وَلَا تَكِلْنِي إِلَى نَفْسِي طَرْفَةَ عَيْنٍ.", 3, "صلاح الشأن كله وطلب الرحمة", "صحيح الترغيب"),
        DhikrItem(17, "أَصْبَحْنَا وَأَصْبَحَ الْمُلْكُ لِلَّهِ رَبِّ الْعَالَمِينَ، اللَّهُمَّ إِنِّي أَسْأَلُكَ خَيْرَ هَذَا الْيَوْمِ: فَتْحَهُ، وَنَصْرَهُ، وَنُورَهُ، وَبَرَكَتَهُ، وَهُدَاهُ، وَأَعُوذُ بِكَ مِنْ شَرِّ مَا فِيهِ وَشَرِّ مَا بَعْدَهُ.", 1, "طلب خير اليوم وبركته ونوره", "أبو داود"),
        DhikrItem(18, "أَصْبَحْنَا عَلَى فِطْرَةِ الْإِسْلَامِ، وَعَلَى كَلِمَةِ الْإِخْلَاصِ، وَعَلَى دِينِ نَبِيِّنَا مُحَمَّدٍ ﷺ، وَعَلَى مِلَّةِ أَبِينَا إِبْرَاهِيمَ حَنِيفًا مُسْلِمًا وَمَا كَانَ مِنَ الْمُشْرِكِينَ.", 1, "الثبات على الدين والتجرد لله", "أحمد"),
        DhikrItem(19, "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ.", 100, "لم يأت أحد يوم القيامة بأفضل مما جاء به إلا من قال مثل ذلك", "مسلم"),
        DhikrItem(20, "لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ، وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ.", 10, "كتب الله له مئة حسنة ومُحيت عنه مئة سيئة وكانت له عدل عشر رقاب", "أحمد والنسائي"),
        DhikrItem(21, "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ: عَدَدَ خَلْقِهِ، وَرِضَا نَفْسِهِ، وَزِنَةَ عَرْشِهِ، وَمِدَادَ كَلِمَاتِهِ.", 3, "تعدل ساعات من الذكر والعبادة", "مسلم"),
        DhikrItem(22, "اللَّهُمَّ إِنِّي أَسْأَلُكَ عِلْمًا نَافِعًا، وَرِزْقًا طَيِّبًا، وَعَمَلًا مُتَقَبَّلًا.", 1, "طلب العلم النافع والرزق الطيب والعمل المتقبل", "ابن ماجه"),
        DhikrItem(23, "أَسْتَغْفِرُ اللَّهَ وَأَتُوبُ إِلَيْهِ.", 100, "تكفير الذنوب ومحو الخطيئة", "البخاري ومسلم"),
        DhikrItem(24, "اللَّهُمَّ صَلِّ وَسَلِّمْ عَلَى نَبِيِّنَا مُحَمَّدٍ.", 10, "من صلى علي حين يصبح عشراً أدركته شفاعتي", "الطبراني"),
        DhikrItem(25, "أَعُوذُ بِكَلِمَاتِ اللَّهِ التَّامَّاتِ مِنْ شَرِّ مَا خَلَقَ.", 3, "الحماية والسلامة من كل شر", "أحمد والترمذي")
    )

    val eveningAdhkar = listOf(
        DhikrItem(1, "أَعُوذُ بِاللَّهِ مِنَ الشَّيْطَانِ الرَّجِيمِ\n\nاللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ ۚ لَهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ ۗ مَن ذَا الَّذِي يَشْفَعُ عِندَهُ إِلَّا بِإِذْنِهِ ۚ يَعْلَمُ مَا بَيْنَ أَيْدِيهِمْ وَمَا خَلْفَهُمْ ۖ وَلَا يُحِيطُونَ بِشَيْءٍ مِّنْ عِلْمِهِ إِلَّا بِمَا شَاءَ ۚ وَسِعَ كُرْسِيُّهُ السَّمَاوَاتِ وَالْأَرْضَ ۖ وَلَا يَئُودُهُ حِفْظُهُمَا ۚ وَهُوَ الْعَلِيُّ الْعَظِيمُ", 1, "آية الكرسي - من قالها حين يمسي أوجير من الجن حتى يصبح", "الحاكم"),
        DhikrItem(2, "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ\n\nقُلْ هُوَ اللَّهُ أَحَدٌ ۝ اللَّهُ الصَّمَدُ ۝ لَمْ يَلِدْ وَلَمْ يُولَدْ ۝ وَلَمْ يَكُن لَّهُ كُفُوًا أَحَدٌ", 3, "سورة الإخلاص", "أبو داود والترمذي"),
        DhikrItem(3, "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ\n\nقُلْ أَعُوذُ بِرَبِّ الْفَلَقِ ۝ مِن شَرِّ مَا خَلَقَ ۝ وَمِن شَرِّ غَاسِقٍ إِذَا وَقَبَ ۝ وَمِن شَرِّ النَّفَّاثَاتِ فِي الْعُقَدِ ۝ وَمِن شَرِّ حَاسِدٍ إِذَا حَسَدَ", 3, "سورة الفلق", "أبو داود والترمذي"),
        DhikrItem(4, "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ\n\nقُلْ أَعُوذُ بِرَبِّ النَّاسِ ۝ مَلِكِ النَّاسِ ۝ إِلَٰهِ النَّاسِ ۝ مِن شَرِّ الْوَسْوَاسِ الْخَنَّاسِ ۝ الَّذِي يُوَسْوِسُ فِي صُدُورِ النَّاسِ ۝ مِنَ الْجِنَّةِ وَالنَّاسِ", 3, "سورة الناس", "أبو داود والترمذي"),
        DhikrItem(5, "أَمْسَيْنَا وَأَمْسَى الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ، لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ، وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ.\nرَبِّ أَسْأَلُكَ خَيْرَ مَا فِي هَذِهِ اللَّيْلَةِ وَخَيْرَ مَا بَعْدَهَا، وَأَعُوذُ بِكَ مِنْ شَرِّ مَا فِي هَذِهِ اللَّيْلَةِ وَشَرِّ مَا بَعْدَهَا، رَبِّ أَعُوذُ بِكَ مِنَ الْكَسَلِ وَسُوءِ الْكِبَرِ، رَبِّ أَعُوذُ بِكَ مِنْ عَذَابٍ فِي النَّارِ وَعَذَابٍ فِي الْقَبْرِ.", 1, "الحفظ والسلامة طوال الليل", "مسلم"),
        DhikrItem(6, "اللَّهُمَّ بِكَ أَمْسَيْنَا، وَبِكَ أَصْبَحْنَا، وَبِكَ نَحْيَا، وَبِكَ نَمُوتُ، وَإِلَيْكَ الْمَصِيرُ.", 1, "الشكر والتفويض لله تعالى", "الترمذي"),
        DhikrItem(7, "اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَٰهَ إِلَّا أَنْتَ، خَلقتَنِي وَأَنَا عَبْدُكَ، وَأَنَا عَلَى عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ، أَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ، أَبُوءُ لَكَ بِنِعْمَتِكَ عَلَيَّ، وَأَبُوءُ بِذَنْبِي فَاغْفِرْ لِي، فَإِنَّهُ لَا يَغْفِرُ الذُّنُوبَ إِلَّا أَنْتَ.", 1, "سيد الاستغفار - من قالها موقناً بها فمات من ليلته دخل الجنة", "البخاري"),
        DhikrItem(8, "اللَّهُمَّ إِنِّي أَمْسَيْتُ أُشْهِدُكَ وَأُشْهِدُ حَمَلَةَ عَرْشِكَ، وَمَلَائِكَتَكَ، وَجَمِيعَ خَلْقِكَ، أَنَّكَ أَنْتَ اللَّهُ لَا إِلَٰهَ إِلَّا أَنْتَ وَحْدَكَ لَا شَرِيكَ لَكَ، وَأَنَّ مُحَمَّدًا عَبْدُكَ وَرَسُولُكَ.", 4, "أعتقه الله من النار", "أبو داود"),
        DhikrItem(9, "اللَّهُمَّ مَا أَمْسَى بِي مِنْ نِعْمَةٍ أَوْ بِأَحَدٍ مِنْ خَلْقِكَ، فَمِنْكَ وَحْدَكَ لَا شَرِيكَ لَكَ، فَلَكَ الْحَمْدُ وَلَكَ الشُّكْرُ.", 1, "أدى شكر ليلته", "أبو داود"),
        DhikrItem(10, "اللَّهُمَّ عَافِنِي فِي بَدَنِي، اللَّهُمَّ عَافِنِي فِي سَمْعِي، اللَّهُمَّ عَافِنِي فِي بَصَرِي، لَا إِلَٰهَ إِلَّا أَنْتَ.\nاللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْكُفْرِ وَالْفَقْرِ، وَأَعُوذُ بِكَ مِنْ عَذَابِ الْقَبْرِ، لَا إِلَٰهَ إِلَّا أَنْتَ.", 3, "عافية البدن والسمع والبصر", "أبو داود"),
        DhikrItem(11, "حَسْبِيَ اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ عَلَيْهِ تَوَكَّلْتُ وَهُوَ رَبُّ الْعَرْشِ الْعَظِيمِ.", 7, "كفاه الله ما أهمه من أمر الدنيا والآخرة", "أبو داود"),
        DhikrItem(12, "اللَّهُمَّ إِنِّي أَسْأَلُكَ الْعَفْوَ وَالْعَافِيَةَ فِي الدُّنْيَا وَالْآخِرَةِ، اللَّهُمَّ إِنِّي أَسْأَلُكَ الْعَفْوَ وَالْعَافِيَةَ فِي دِينِي وَدُنْيَايَ وَأَهْلِي وَمَالِي، اللَّهُمَّ اسْتُرْ عَوْرَاتِي وَآمِنْ رَوْعَاتِي، اللَّهُمَّ احْفَظْنِي مِنْ بَيْنِ يَدَيَّ وَمِنْ خَلْفِي وَعَنْ يَمِينِي وَعَنْ شِمَالِي وَمِنْ فَوْقِي، وَأَعُوذُ بِعَظَمَتِكَ أَنْ أُغْتَالَ مِنْ تَحْتِي.", 1, "الحفظ والعافية الشاملة", "أبو داود"),
        DhikrItem(13, "اللَّهُمَّ عَالِمَ الْغَيْبِ وَالشَّهَادَةِ فَاطِرَ السَّمَاوَاتِ وَالْأَرْضِ، رَبَّ كُلِّ شَيْءٍ وَمَلِيكَهُ، أَشْهَدُ أَنْ لَا إِلَٰهَ إِلَّا أَنْتَ، أَعُوذُ بِكَ مِنْ شَرِّ نَفْسِي وَمِنْ شَرِّ الشَّيْطَانِ وَشِرْكِهِ، وَأَنْ أَقْتَرِفَ عَلَى نَفْسِي سُوءًا أَوْ أَجُرَّهُ إِلَى مُسْلِمٍ.", 1, "الحفظ من الشرك وشر النفس والشيطان", "الترمذي"),
        DhikrItem(14, "بِسْمِ اللَّهِ الَّذِي لَا يَضُرُّ مَعَ اسْمِهِ شَيْءٌ فِي الْأَرْضِ وَلَا فِي السَّمَاءِ، وَهُوَ السَّمِيعُ الْعَلِيمُ.", 3, "لم يضره شيء", "أبو داود"),
        DhikrItem(15, "رَضِيتُ بِاللَّهِ رَبًّا، وَبِالْإِسْلَامِ دِينًا، وَبِمُحَمَّدٍ ﷺ نَبِيًّا.", 3, "رضوان الله تعالى يوم القيامة", "أحمد"),
        DhikrItem(16, "يَا حَيُّ يَا قَيُّومُ بِرَحْمَتِكَ أَسْتَغِيثُ، أَصْلِحْ لِي شَأْنِي كُلَّهُ، وَلَا تَكِلْنِي إِلَى نَفْسِي طَرْفَةَ عَيْنٍ.", 3, "صلاح الأمور كلها", "صحيح الترغيب"),
        DhikrItem(17, "أَمْسَيْنَا وَأَمْسَى الْمُلْكُ لِلَّهِ رَبِّ الْعَالَمِينَ، اللَّهُمَّ إِنِّي أَسْأَلُكَ خَيْرَ هَذِهِ اللَّيْلَةِ: فَتْحَهَا، وَنَصْرَهَا، وَنُورَهَا، وَبَرَكَتَهَا، وَهُدَاهَا، وَأَعُوذُ بِكَ مِنْ شَرِّ مَا فِيهَا وَشَرِّ مَا بَعْدَهَا.", 1, "طلب خير الليلة وبركتها ونورها", "أبو داود"),
        DhikrItem(18, "أَمْسَيْنَا عَلَى فِطْرَةِ الْإِسْلَامِ، وَعَلَى كَلِمَةِ الْإِخْلَاصِ، وَعَلَى دِينِ نَبِيِّنَا مُحَمَّدٍ ﷺ، وَعَلَى مِلَّةِ أَبِينَا إِبْرَاهِيمَ حَنِيفًا مُسْلِمًا وَمَا كَانَ مِنَ الْمُشْرِكِينَ.", 1, "الثبات والإخلاص", "أحمد"),
        DhikrItem(19, "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ.", 100, "حُطَّت خطاياه وإن كانت مثل زبد البحر", "البخاري ومسلم"),
        DhikrItem(20, "لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ، وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ.", 10, "حفظ من الشيطان وتحصيل الأجر العظيم", "أحمد والنسائي"),
        DhikrItem(21, "أَعُوذُ بِكَلِمَاتِ اللَّهِ التَّامَّاتِ مِنْ شَرِّ مَا خَلَقَ.", 3, "لم تضره حُمَة تلك الليلة (لدغة عقرب أو دابة)", "أحمد والترمذي"),
        DhikrItem(22, "اللَّهُمَّ صَلِّ وَسَلِّمْ عَلَى نَبِيِّنَا مُحَمَّدٍ.", 10, "أدركته شفاعة النبي ﷺ يوم القيامة", "الطبراني"),
        DhikrItem(23, "أَسْتَغْفِرُ اللَّهَ الْعَظِيمَ الَّذِي لَا إِلَٰهَ إِلَّا هُوَ الْحَيَّ الْقَيُّومَ وَأَتُوبُ إِلَيْهِ.", 3, "غُفرت ذنوبه وإن كان قد فرّ من الزحف", "أبو داود والترمذي")
    )

    val surahs = listOf(
        SurahInfo(1, "الفاتحة", "Al-Fatiha", 7, "مكية", 1, listOf(
            "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
            "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ",
            "الرَّحْمَٰنِ الرَّحِيمِ",
            "مَالِكِ يَوْمِ الدِّينِ",
            "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ",
            "اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ",
            "صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ"
        )),
        SurahInfo(2, "البقرة", "Al-Baqarah", 286, "مدنية", 2),
        SurahInfo(3, "آل عمران", "Ali 'Imran", 200, "مدنية", 50),
        SurahInfo(4, "النساء", "An-Nisa", 176, "مدنية", 77),
        SurahInfo(5, "المائدة", "Al-Ma'idah", 120, "مدنية", 106),
        SurahInfo(6, "الأنعام", "Al-An'am", 165, "مكية", 128),
        SurahInfo(7, "الأعراف", "Al-A'raf", 206, "مكية", 151),
        SurahInfo(8, "الأنفال", "Al-Anfal", 75, "مدنية", 177),
        SurahInfo(9, "التوبة", "At-Tawbah", 129, "مدنية", 187),
        SurahInfo(10, "يونس", "Yunus", 109, "مكية", 208),
        SurahInfo(11, "هود", "Hud", 123, "مكية", 221),
        SurahInfo(12, "يوسف", "Yusuf", 111, "مكية", 235),
        SurahInfo(13, "الرعد", "Ar-Ra'd", 43, "مدنية", 249),
        SurahInfo(14, "إبراهيم", "Ibrahim", 52, "مكية", 255),
        SurahInfo(15, "الحجر", "Al-Hijr", 99, "مكية", 262),
        SurahInfo(16, "النحل", "An-Nahl", 128, "مكية", 267),
        SurahInfo(17, "الإسراء", "Al-Isra", 111, "مكية", 282),
        SurahInfo(18, "الكهف", "Al-Kahf", 110, "مكية", 293),
        SurahInfo(19, "مريم", "Maryam", 98, "مكية", 305),
        SurahInfo(20, "طه", "Taha", 135, "مكية", 312),
        SurahInfo(21, "الأنبياء", "Al-Anbiya", 112, "مكية", 322),
        SurahInfo(22, "الحج", "Al-Hajj", 78, "مدنية", 332),
        SurahInfo(23, "المؤمنون", "Al-Mu'minun", 118, "مكية", 342),
        SurahInfo(24, "النور", "An-Nur", 64, "مدنية", 350),
        SurahInfo(25, "الفرقان", "Al-Furqan", 77, "مكية", 359),
        SurahInfo(26, "الشعراء", "Ash-Shu'ara", 227, "مكية", 367),
        SurahInfo(27, "النمل", "An-Naml", 93, "مكية", 377),
        SurahInfo(28, "القصص", "Al-Qasas", 88, "مكية", 385),
        SurahInfo(29, "العنكبوت", "Al-'Ankabut", 69, "مكية", 396),
        SurahInfo(30, "الروم", "Ar-Rum", 60, "مكية", 404),
        SurahInfo(31, "لقمان", "Luqman", 34, "مكية", 411),
        SurahInfo(32, "السجدة", "As-Sajdah", 30, "مكية", 415),
        SurahInfo(33, "الأحزاب", "Al-Ahzab", 73, "مدنية", 418),
        SurahInfo(34, "سبأ", "Saba", 54, "مكية", 428),
        SurahInfo(35, "فاطر", "Fatir", 45, "مكية", 434),
        SurahInfo(36, "يس", "Ya-Sin", 83, "مكية", 440, listOf(
            "يس", "وَالْقُرْآنِ الْحَكِيمِ", "إِنَّكَ لَمِنَ الْمُرْسَلِينَ", "عَلَىٰ صِرَاطٍ مُسْتَقِيمٍ", "تَنْزِيلَ الْعَزِيزِ الرَّحِيمِ", "لِتُنْذِرَ قَوْمًا مَا أُنْذِرَ آبَاؤُهُمْ فَهُمْ غَافِلُونَ"
        )),
        SurahInfo(37, "الصافات", "As-Saffat", 182, "مكية", 446),
        SurahInfo(38, "ص", "Sad", 88, "مكية", 453),
        SurahInfo(39, "الزمر", "Az-Zumar", 75, "مكية", 458),
        SurahInfo(40, "غافر", "Ghafir", 85, "مكية", 467),
        SurahInfo(41, "فصلت", "Fussilat", 54, "مكية", 477),
        SurahInfo(42, "الشورى", "Ash-Shura", 53, "مكية", 483),
        SurahInfo(43, "الزخرف", "Az-Zukhruf", 89, "مكية", 489),
        SurahInfo(44, "الدخان", "Ad-Dukhan", 59, "مكية", 496),
        SurahInfo(45, "الجاثية", "Al-Jathiyah", 37, "مكية", 499),
        SurahInfo(46, "الأحقاف", "Al-Ahqaf", 35, "مكية", 502),
        SurahInfo(47, "محمد", "Muhammad", 38, "مدنية", 507),
        SurahInfo(48, "الفتح", "Al-Fath", 29, "مدنية", 511),
        SurahInfo(49, "الحجرات", "Al-Hujurat", 18, "مدنية", 515),
        SurahInfo(50, "ق", "Qaf", 45, "مكية", 518),
        SurahInfo(51, "الذاريات", "Adh-Dhariyat", 60, "مكية", 520),
        SurahInfo(52, "الطور", "At-Tur", 49, "مكية", 523),
        SurahInfo(53, "النجم", "An-Najm", 62, "مكية", 526),
        SurahInfo(54, "القمر", "Al-Qamar", 55, "مكية", 528),
        SurahInfo(55, "الرحمن", "Ar-Rahman", 78, "مدنية", 531, listOf(
            "الرَّحْمَٰنُ", "عَلَّمَ الْقُرْآنَ", "خَلَقَ الْإِنْسَانَ", "عَلَّمَهُ الْبَيَانَ", "الشَّمْسُ وَالْقَمَرُ بِحُسْبَانٍ"
        )),
        SurahInfo(56, "الواقعة", "Al-Waqi'ah", 96, "مكية", 534),
        SurahInfo(57, "الحديد", "Al-Hadid", 29, "مدنية", 537),
        SurahInfo(58, "المجادلة", "Al-Mujadila", 22, "مدنية", 542),
        SurahInfo(59, "الحشر", "Al-Hashr", 24, "مدنية", 545),
        SurahInfo(60, "الممتحنة", "Al-Mumtahanah", 13, "مدنية", 549),
        SurahInfo(61, "الصف", "As-Saff", 14, "مدنية", 551),
        SurahInfo(62, "الجمعة", "Al-Jumu'ah", 11, "مدنية", 553),
        SurahInfo(63, "المنافقون", "Al-Munafiqun", 11, "مدنية", 554),
        SurahInfo(64, "التغابن", "At-Taghabun", 18, "مدنية", 556),
        SurahInfo(65, "الطلاق", "At-Talaq", 12, "مدنية", 558),
        SurahInfo(66, "التحريم", "At-Tahrim", 12, "مدنية", 560),
        SurahInfo(67, "الملك", "Al-Mulk", 30, "مكية", 562, listOf(
            "تَبَارَكَ الَّذِي بِيَدِهِ الْمُلْكُ وَهُوَ عَلَىٰ كُلِّ شَيْءٍ قَدِيرٌ",
            "الَّذِي خَلَقَ الْمَوْتَ وَالْحَيَاةَ لِيَبْلُوَكُمْ أَيُّكُمْ أَحْسَنُ عَمَلًا"
        )),
        SurahInfo(68, "القلم", "Al-Qalam", 52, "مكية", 564),
        SurahInfo(69, "الحاقة", "Al-Haqqah", 52, "مكية", 566),
        SurahInfo(70, "المعارج", "Al-Ma'arij", 44, "مكية", 568),
        SurahInfo(71, "نوح", "Nuh", 28, "مكية", 570),
        SurahInfo(72, "الجن", "Al-Jinn", 28, "مكية", 572),
        SurahInfo(73, "المزمل", "Al-Muzzammil", 20, "مكية", 574),
        SurahInfo(74, "المدثر", "Al-Muddaththir", 56, "مكية", 575),
        SurahInfo(75, "القيامة", "Al-Qiyamah", 40, "مكية", 577),
        SurahInfo(76, "الإنسان", "Al-Insan", 31, "مدنية", 578),
        SurahInfo(77, "المرسلات", "Al-Mursalat", 50, "مكية", 580),
        SurahInfo(78, "النبأ", "An-Naba", 40, "مكية", 582),
        SurahInfo(79, "النازعات", "An-Nazi'at", 46, "مكية", 583),
        SurahInfo(80, "عبس", "Abasa", 42, "مكية", 585),
        SurahInfo(81, "التكوير", "At-Takwir", 29, "مكية", 586),
        SurahInfo(82, "الانفطار", "Al-Infitar", 19, "مكية", 587),
        SurahInfo(83, "المطففين", "Al-Mutaffifin", 36, "مكية", 587),
        SurahInfo(84, "الانشقاق", "Al-Inshiqaq", 25, "مكية", 589),
        SurahInfo(85, "البروج", "Al-Buruj", 22, "مكية", 590),
        SurahInfo(86, "الطارق", "At-Tariq", 17, "مكية", 591),
        SurahInfo(87, "الأعلى", "Al-A'la", 19, "مكية", 591),
        SurahInfo(88, "الغاشية", "Al-Ghashiyah", 26, "مكية", 592),
        SurahInfo(89, "الفجر", "Al-Fajr", 30, "مكية", 593),
        SurahInfo(90, "البلد", "Al-Balad", 20, "مكية", 594),
        SurahInfo(91, "الشمس", "Ash-Shams", 15, "مكية", 595),
        SurahInfo(92, "الليل", "Al-Layl", 21, "مكية", 595),
        SurahInfo(93, "الضحى", "Ad-Duha", 11, "مكية", 596),
        SurahInfo(94, "الشرح", "Ash-Sharh", 8, "مكية", 596),
        SurahInfo(95, "التين", "At-Tin", 8, "مكية", 597),
        SurahInfo(96, "العلق", "Al-'Alaq", 19, "مكية", 597),
        SurahInfo(97, "القدر", "Al-Qadr", 5, "مكية", 598),
        SurahInfo(98, "البينة", "Al-Bayyinah", 8, "مدنية", 598),
        SurahInfo(99, "الزلزلة", "Az-Zalzalah", 8, "مدنية", 599),
        SurahInfo(100, "العاديات", "Al-'Adiyat", 11, "مكية", 599),
        SurahInfo(101, "القارعة", "Al-Qari'ah", 11, "مكية", 600),
        SurahInfo(102, "التكاثر", "At-Takathur", 8, "مكية", 600),
        SurahInfo(103, "العصر", "Al-'Asr", 3, "مكية", 601),
        SurahInfo(104, "الهمزة", "Al-Humazah", 9, "مكية", 601),
        SurahInfo(105, "الفيل", "Al-Fil", 5, "مكية", 601),
        SurahInfo(106, "قريش", "Quraysh", 4, "مكية", 602),
        SurahInfo(107, "الماعون", "Al-Ma'un", 7, "مكية", 602),
        SurahInfo(108, "الكوثر", "Al-Kawthar", 3, "مكية", 602),
        SurahInfo(109, "الكافرون", "Al-Kafirun", 6, "مكية", 603),
        SurahInfo(110, "النصر", "An-Nasr", 3, "مدنية", 603),
        SurahInfo(111, "المسد", "Al-Masad", 5, "مكية", 603),
        SurahInfo(112, "الإخلاص", "Al-Ikhlas", 4, "مكية", 604, listOf(
            "قُلْ هُوَ اللَّهُ أَحَدٌ", "اللَّهُ الصَّمَدُ", "لَمْ يَلِدْ وَلَمْ يُولَدْ", "وَلَمْ يَكُنْ لَهُ كُفُوًا أَحَدٌ"
        )),
        SurahInfo(113, "الفلق", "Al-Falaq", 5, "مكية", 604, listOf(
            "قُلْ أَعُوذُ بِرَبِّ الْفَلَقِ", "مِنْ شَرِّ مَا خَلَقَ", "وَمِنْ شَرِّ غَاسِقٍ إِذَا وَقَبَ", "وَمِنْ شَرِّ النَّفَّاثَاتِ فِي الْعُقَدِ", "وَمِنْ شَرِّ حَاسِدٍ إِذَا حَسَدَ"
        )),
        SurahInfo(114, "الناس", "An-Nas", 6, "مكية", 604, listOf(
            "قُلْ أَعُوذُ بِرَبِّ النَّاسِ", "مَلِكِ النَّاسِ", "إِلَٰهِ النَّاسِ", "مِنْ شَرِّ الْوَسْوَاسِ الْخَنَّاسِ", "الَّذِي يُوَسْوِسُ فِي صُدُورِ النَّاسِ", "مِنَ الْجِنَّةِ وَالنَّاسِ"
        ))
    )
}
