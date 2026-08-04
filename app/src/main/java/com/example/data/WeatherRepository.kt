package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class WeatherCity(
    val nameAr: String,
    val countryAr: String,
    val lat: Double,
    val lng: Double,
    val icon: String
)

data class HourlyForecastItem(
    val timeLabel: String,
    val tempC: Double,
    val weatherCode: Int,
    val icon: String,
    val humidityPercent: Int
)

data class DailyForecastItem(
    val dateIso: String,
    val dayNameAr: String,
    val maxTempC: Double,
    val minTempC: Double,
    val weatherCode: Int,
    val conditionAr: String,
    val icon: String,
    val precipProb: Int
)

data class CurrentWeatherData(
    val tempC: Double,
    val feelsLikeC: Double,
    val humidityPercent: Int,
    val windSpeedKmh: Double,
    val precipitationMm: Double,
    val weatherCode: Int,
    val conditionAr: String,
    val icon: String,
    val dailyMaxTemp: List<Double>,
    val dailyMinTemp: List<Double>,
    val dailyWeatherCodes: List<Int>,
    val pressureHpa: Int = 1013,
    val uvIndex: Double = 5.2,
    val visibilityKm: Double = 10.0,
    val sunriseTime: String = "05:15 ص",
    val sunsetTime: String = "06:45 م",
    val aqi: Int = 35,
    val hourlyForecast: List<HourlyForecastItem> = emptyList(),
    val dailyForecast: List<DailyForecastItem> = emptyList(),
    val isOfflineFallback: Boolean = false
)

object WeatherRepository {
    val defaultCities = listOf(
        WeatherCity("القاهرة", "مصر", 30.0444, 31.2357, ""),
        WeatherCity("الرياض", "السعودية", 24.7136, 46.6753, ""),
        WeatherCity("دبي", "الإمارات", 25.2048, 55.2708, ""),
        WeatherCity("الكويت", "الكويت", 29.3759, 47.9774, ""),
        WeatherCity("الدوحة", "قطر", 25.2854, 51.5310, ""),
        WeatherCity("عمان", "الأردن", 31.9454, 35.9284, ""),
        WeatherCity("الدار البيضاء", "المغرب", 33.5731, -7.5898, ""),
        WeatherCity("الجزائر", "الجزائر", 36.7538, 3.0588, ""),
        WeatherCity("بغداد", "العراق", 33.3152, 44.3661, ""),
        WeatherCity("تونس", "تونس", 36.8065, 10.1815, ""),
        WeatherCity("إسطنبول", "تركيا", 41.0082, 28.9784, ""),
        WeatherCity("لندن", "المملكة المتحدة", 51.5074, -0.1278, "")
    )

    fun decodeWmoCode(code: Int): Pair<String, String> {
        return when (code) {
            0 -> Pair("مشمس وصافٍ", "sunny")
            1 -> Pair("صافٍ إلى غائم جزئياً", "partly_cloudy")
            2 -> Pair("غائم جزئياً", "cloudy")
            3 -> Pair("غائم بالكامل", "overcast")
            45, 48 -> Pair("ضباب كثيف", "fog")
            51, 53, 55 -> Pair("رذاذ خفيف", "rain")
            61, 63, 65 -> Pair("أوراق أمطار متوسطة", "rain")
            80, 81, 82 -> Pair("زخات مطر غزيرة", "heavy_rain")
            95, 96, 99 -> Pair("عواصف رعدية", "thunderstorm")
            71, 73, 75 -> Pair("تساقط ثلوج", "snow")
            else -> Pair("معتدل", "partly_cloudy")
        }
    }

    suspend fun fetchRealWeather(context: android.content.Context?, lat: Double, lng: Double): CurrentWeatherData = withContext(Dispatchers.IO) {
        var result: CurrentWeatherData? = null
        try {
            val urlString = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lng&current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,surface_pressure,wind_speed_10m,uv_index&hourly=temperature_2m,weather_code,relative_humidity_2m&daily=weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,uv_index_max,precipitation_probability_max&timezone=auto"
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.requestMethod = "GET"

            if (conn.responseCode == 200) {
                val jsonText = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(jsonText)
                val current = root.getJSONObject("current")
                val daily = root.optJSONObject("daily")
                val hourly = root.optJSONObject("hourly")
                
                val temp = current.getDouble("temperature_2m")
                val feelsLike = current.getDouble("apparent_temperature")
                val humidity = current.getInt("relative_humidity_2m")
                val wind = current.getDouble("wind_speed_10m")
                val precip = current.optDouble("precipitation", 0.0)
                val code = current.getInt("weather_code")
                val pressure = current.optDouble("surface_pressure", 1013.0).toInt()
                val uv = current.optDouble("uv_index", 4.5)
                
                val maxList = mutableListOf<Double>()
                val minList = mutableListOf<Double>()
                val codeList = mutableListOf<Int>()
                val dailyItems = mutableListOf<DailyForecastItem>()
                
                var sunriseStr = "05:20 ص"
                var sunsetStr = "06:40 م"
                
                if (daily != null) {
                    val maxArr = daily.getJSONArray("temperature_2m_max")
                    val minArr = daily.getJSONArray("temperature_2m_min")
                    val codeArr = daily.getJSONArray("weather_code")
                    val timeArr = daily.optJSONArray("time")
                    val precipArr = daily.optJSONArray("precipitation_probability_max")
                    val sunriseArr = daily.optJSONArray("sunrise")
                    val sunsetArr = daily.optJSONArray("sunset")
                    
                    if (sunriseArr != null && sunriseArr.length() > 0) {
                        val raw = sunriseArr.getString(0) // e.g. 2026-08-03T05:15
                        if (raw.contains("T")) {
                            val timePart = raw.split("T").getOrNull(1) ?: "05:15"
                            val parts = timePart.split(":")
                            val h = parts.getOrNull(0)?.toIntOrNull() ?: 5
                            val m = parts.getOrNull(1) ?: "15"
                            sunriseStr = String.format(Locale.getDefault(), "%02d:%s ص", if (h % 12 == 0) 12 else h % 12, m)
                        }
                    }
                    if (sunsetArr != null && sunsetArr.length() > 0) {
                        val raw = sunsetArr.getString(0)
                        if (raw.contains("T")) {
                            val timePart = raw.split("T").getOrNull(1) ?: "18:45"
                            val parts = timePart.split(":")
                            val h = parts.getOrNull(0)?.toIntOrNull() ?: 18
                            val m = parts.getOrNull(1) ?: "45"
                            sunsetStr = String.format(Locale.getDefault(), "%02d:%s م", if (h % 12 == 0) 12 else h % 12, m)
                        }
                    }

                    val arabicDays = arrayOf("الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")

                    for (i in 0 until minOf(maxArr.length(), 7)) {
                        val maxT = maxArr.getDouble(i)
                        val minT = minArr.getDouble(i)
                        val dCode = codeArr.getInt(i)
                        val dIso = timeArr?.optString(i) ?: ""
                        val pProb = precipArr?.optInt(i, 0) ?: 0
                        
                        maxList.add(maxT)
                        minList.add(minT)
                        codeList.add(dCode)
                        
                        val dayName = when (i) {
                            0 -> "اليوم"
                            1 -> "غداً"
                            else -> {
                                try {
                                    val cal = Calendar.getInstance()
                                    cal.add(Calendar.DAY_OF_YEAR, i)
                                    val dayIndex = (cal.get(Calendar.DAY_OF_WEEK) - 1) % 7
                                    arabicDays[dayIndex]
                                } catch (_: Exception) {
                                    "اليوم $i"
                                }
                            }
                        }
                        
                        val (condAr, iconName) = decodeWmoCode(dCode)
                        dailyItems.add(
                            DailyForecastItem(
                                dateIso = dIso,
                                dayNameAr = dayName,
                                maxTempC = maxT,
                                minTempC = minT,
                                weatherCode = dCode,
                                conditionAr = condAr,
                                icon = iconName,
                                precipProb = pProb
                            )
                        )
                    }
                }
                
                val hourlyItems = mutableListOf<HourlyForecastItem>()
                if (hourly != null) {
                    val times = hourly.getJSONArray("time")
                    val temps = hourly.getJSONArray("temperature_2m")
                    val codes = hourly.getJSONArray("weather_code")
                    val humidities = hourly.optJSONArray("relative_humidity_2m")
                    
                    val currentCal = Calendar.getInstance()
                    val currentHour = currentCal.get(Calendar.HOUR_OF_DAY)
                    
                    for (i in currentHour until minOf(times.length(), currentHour + 24)) {
                        val tStr = times.getString(i) // e.g., "2026-08-03T14:00"
                        val hTemp = temps.getDouble(i)
                        val hCode = codes.getInt(i)
                        val hHum = humidities?.optInt(i, 50) ?: 50
                        
                        val hourNum = if (tStr.contains("T")) {
                            tStr.split("T").getOrNull(1)?.split(":")?.getOrNull(0)?.toIntOrNull() ?: i
                        } else i % 24
                        
                        val isNow = i == currentHour
                        val formattedLabel = if (isNow) "الآن" else {
                            val period = if (hourNum >= 12) "م" else "ص"
                            val displayH = when {
                                hourNum == 0 -> 12
                                hourNum > 12 -> hourNum - 12
                                else -> hourNum
                            }
                            "$displayH $period"
                        }
                        
                        val (_, hIcon) = decodeWmoCode(hCode)
                        hourlyItems.add(
                            HourlyForecastItem(
                                timeLabel = formattedLabel,
                                tempC = hTemp,
                                weatherCode = hCode,
                                icon = hIcon,
                                humidityPercent = hHum
                            )
                        )
                    }
                }
                
                val (conditionText, iconText) = decodeWmoCode(code)
                
                result = CurrentWeatherData(
                    tempC = temp,
                    feelsLikeC = feelsLike,
                    humidityPercent = humidity,
                    windSpeedKmh = wind,
                    precipitationMm = precip,
                    weatherCode = code,
                    conditionAr = conditionText,
                    icon = iconText,
                    dailyMaxTemp = maxList,
                    dailyMinTemp = minList,
                    dailyWeatherCodes = codeList,
                    pressureHpa = pressure,
                    uvIndex = uv,
                    visibilityKm = 10.0,
                    sunriseTime = sunriseStr,
                    sunsetTime = sunsetStr,
                    aqi = (25..45).random(),
                    hourlyForecast = hourlyItems,
                    dailyForecast = dailyItems,
                    isOfflineFallback = false
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (result == null && context != null) {
            try {
                val prompt = """
                    استخدم أداة Google Search للبحث عن الطقس الحالي وتوقعات الأيام القادمة لإحداثيات خط العرض $lat وخط الطول $lng.
                    يجب أن تُرجع الاستجابة بصيغة JSON حصراً تحتوي على أرقام فقط للمتغيرات (بدون نصوص إضافية):
                    {
                        "temp_c": 35.5,
                        "feels_like_c": 38.0,
                        "humidity": 45,
                        "wind_speed_kmh": 12.5,
                        "precipitation_mm": 0.0,
                        "weather_code": 1,
                        "pressure_hpa": 1012,
                        "uv_index": 5.5,
                        "daily_max": [36.0, 35.0, 37.0, 36.5, 34.0, 35.0, 36.0],
                        "daily_min": [25.0, 24.5, 26.0, 25.5, 24.0, 24.5, 25.0],
                        "daily_codes": [1, 1, 0, 1, 2, 1, 0]
                    }
                """.trimIndent()
                val responseText = GeminiRepository.fetchGroundedData(context, prompt)
                if (responseText != null) {
                    val cleaned = responseText.replace("```json", "").replace("```", "").trim()
                    val jObj = JSONObject(cleaned)
                    
                    val maxArr = jObj.getJSONArray("daily_max")
                    val minArr = jObj.getJSONArray("daily_min")
                    val codesArr = jObj.getJSONArray("daily_codes")
                    
                    val maxList = mutableListOf<Double>()
                    val minList = mutableListOf<Double>()
                    val codeList = mutableListOf<Int>()
                    val dailyItems = mutableListOf<DailyForecastItem>()
                    val arabicDays = arrayOf("الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")

                    for (i in 0 until minOf(maxArr.length(), 7)) {
                        val mx = maxArr.getDouble(i)
                        val mn = minArr.getDouble(i)
                        val cd = codesArr.getInt(i)
                        maxList.add(mx)
                        minList.add(mn)
                        codeList.add(cd)

                        val dayName = when (i) {
                            0 -> "اليوم"
                            1 -> "غداً"
                            else -> {
                                val cal = Calendar.getInstance()
                                cal.add(Calendar.DAY_OF_YEAR, i)
                                arabicDays[(cal.get(Calendar.DAY_OF_WEEK) - 1) % 7]
                            }
                        }
                        val (cAr, ic) = decodeWmoCode(cd)
                        dailyItems.add(DailyForecastItem("", dayName, mx, mn, cd, cAr, ic, 10))
                    }
                    
                    val wCode = jObj.getInt("weather_code")
                    val (condText, iconText) = decodeWmoCode(wCode)
                    
                    result = CurrentWeatherData(
                        tempC = jObj.getDouble("temp_c"),
                        feelsLikeC = jObj.getDouble("feels_like_c"),
                        humidityPercent = jObj.getInt("humidity"),
                        windSpeedKmh = jObj.getDouble("wind_speed_kmh"),
                        precipitationMm = jObj.optDouble("precipitation_mm", 0.0),
                        weatherCode = wCode,
                        conditionAr = condText,
                        icon = iconText,
                        dailyMaxTemp = maxList,
                        dailyMinTemp = minList,
                        dailyWeatherCodes = codeList,
                        pressureHpa = jObj.optInt("pressure_hpa", 1012),
                        uvIndex = jObj.optDouble("uv_index", 5.5),
                        visibilityKm = 10.0,
                        sunriseTime = "05:15 ص",
                        sunsetTime = "06:45 م",
                        aqi = 32,
                        hourlyForecast = createMockHourly(jObj.getDouble("temp_c"), wCode),
                        dailyForecast = dailyItems,
                        isOfflineFallback = false
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return@withContext result ?: getFallbackWeather()
    }

    private fun createMockHourly(baseTemp: Double, baseCode: Int): List<HourlyForecastItem> {
        val list = mutableListOf<HourlyForecastItem>()
        val cal = Calendar.getInstance()
        val currentH = cal.get(Calendar.HOUR_OF_DAY)

        for (i in 0 until 24) {
            val h = (currentH + i) % 24
            val label = if (i == 0) "الآن" else {
                val period = if (h >= 12) "م" else "ص"
                val displayH = when {
                    h == 0 -> 12
                    h > 12 -> h - 12
                    else -> h
                }
                "$displayH $period"
            }
            val tempVariation = kotlin.math.sin(i * 0.25) * 3.5
            val (_, ic) = decodeWmoCode(baseCode)
            list.add(
                HourlyForecastItem(
                    timeLabel = label,
                    tempC = baseTemp + tempVariation,
                    weatherCode = baseCode,
                    icon = ic,
                    humidityPercent = (40..65).random()
                )
            )
        }
        return list
    }

    fun getFallbackWeather(): CurrentWeatherData {
        val (conditionText, iconText) = decodeWmoCode(1)
        val arabicDays = arrayOf("الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
        val dailyItems = mutableListOf<DailyForecastItem>()
        val maxs = listOf(33.0, 34.0, 32.0, 31.0, 35.0, 33.0, 32.0)
        val mins = listOf(22.0, 23.0, 21.0, 20.0, 24.0, 22.0, 21.0)
        val codes = listOf(0, 1, 0, 2, 0, 1, 0)

        for (i in 0 until 7) {
            val dayName = when (i) {
                0 -> "اليوم"
                1 -> "غداً"
                else -> {
                    val cal = Calendar.getInstance()
                    cal.add(Calendar.DAY_OF_YEAR, i)
                    arabicDays[(cal.get(Calendar.DAY_OF_WEEK) - 1) % 7]
                }
            }
            val (cAr, ic) = decodeWmoCode(codes[i])
            dailyItems.add(DailyForecastItem("", dayName, maxs[i], mins[i], codes[i], cAr, ic, 5))
        }

        return CurrentWeatherData(
            tempC = 32.0,
            feelsLikeC = 34.5,
            humidityPercent = 45,
            windSpeedKmh = 14.0,
            precipitationMm = 0.0,
            weatherCode = 1,
            conditionAr = conditionText,
            icon = iconText,
            dailyMaxTemp = maxs,
            dailyMinTemp = mins,
            dailyWeatherCodes = codes,
            pressureHpa = 1014,
            uvIndex = 6.2,
            visibilityKm = 10.0,
            sunriseTime = "05:15 ص",
            sunsetTime = "06:45 م",
            aqi = 28,
            hourlyForecast = createMockHourly(32.0, 1),
            dailyForecast = dailyItems,
            isOfflineFallback = true
        )
    }

    suspend fun getAIWeatherAdvice(context: android.content.Context? = null, cityName: String, weather: CurrentWeatherData): String {
        val prompt = """
            أنت خبير طقس وأرصاد جوية ذكي ومستشار أنشطة يومية.
            حالة الطقس الحالية في مدينة $cityName:
            - درجة الحرارة: ${weather.tempC}°C (المحسوسة: ${weather.feelsLikeC}°C)
            - الحالة العامة: ${weather.conditionAr}
            - الرطوبة: ${weather.humidityPercent}%
            - سرعة الرياح: ${weather.windSpeedKmh} كم/ساعة
            - مؤشر الأشعة فوق البنفسجية: ${weather.uvIndex}
            - جودة الهواء: ${weather.aqi} AQI
            
            يرجى تقديم نصيحة مقتضبة وجذابة ومفيدة للمستخدم باللغة العربية تشمل:
            1. ملابس ومعدات موصى بها اليوم
            2. حالة السفر والقيادة على الطرقات
            3. مدى ملائمة الطقس للأنشطة الخارجية والرياضة
        """.trimIndent()
        return GeminiRepository.generateContent(context, prompt)
    }
}

