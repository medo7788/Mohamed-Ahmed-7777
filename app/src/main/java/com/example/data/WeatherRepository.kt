package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class WeatherCity(
    val nameAr: String,
    val countryAr: String,
    val lat: Double,
    val lng: Double,
    val icon: String
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
            val urlString = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lng&current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,wind_speed_10m&daily=weather_code,temperature_2m_max,temperature_2m_min&timezone=auto"
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.requestMethod = "GET"

            if (conn.responseCode == 200) {
                val jsonText = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(jsonText)
                val current = root.getJSONObject("current")
                val daily = root.getJSONObject("daily")
                
                val temp = current.getDouble("temperature_2m")
                val feelsLike = current.getDouble("apparent_temperature")
                val humidity = current.getInt("relative_humidity_2m")
                val wind = current.getDouble("wind_speed_10m")
                val precip = current.optDouble("precipitation", 0.0)
                val code = current.getInt("weather_code")
                
                val maxList = mutableListOf<Double>()
                val minList = mutableListOf<Double>()
                val codeList = mutableListOf<Int>()
                
                val maxArr = daily.getJSONArray("temperature_2m_max")
                val minArr = daily.getJSONArray("temperature_2m_min")
                val codeArr = daily.getJSONArray("weather_code")
                
                for (i in 0 until minOf(maxArr.length(), 7)) {
                    maxList.add(maxArr.getDouble(i))
                    minList.add(minArr.getDouble(i))
                    codeList.add(codeArr.getInt(i))
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
                    dailyWeatherCodes = codeList
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
                    
                    for (i in 0 until minOf(maxArr.length(), 7)) {
                        maxList.add(maxArr.getDouble(i))
                        minList.add(minArr.getDouble(i))
                        codeList.add(codesArr.getInt(i))
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
                        isOfflineFallback = false
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return@withContext result ?: getFallbackWeather()
    }

    private fun getFallbackWeather(): CurrentWeatherData {
        val (conditionText, iconText) = decodeWmoCode(1)
        return CurrentWeatherData(
            tempC = 32.0,
            feelsLikeC = 34.5,
            humidityPercent = 45,            
            windSpeedKmh = 14.0,            
            precipitationMm = 0.0,            
            weatherCode = 1,            
            conditionAr = conditionText,            
            icon = iconText,            
            dailyMaxTemp = listOf(33.0, 34.0, 32.0, 31.0, 35.0, 33.0, 32.0),            
            dailyMinTemp = listOf(22.0, 23.0, 21.0, 20.0, 24.0, 22.0, 21.0),            
            dailyWeatherCodes = listOf(0, 1, 0, 2, 0, 1, 0),            
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
            يرجى تقديم نصيحة مقتضبة وجذابة ومفيدة للمستخدم باللغة العربية تشمل:
            1. ملابس ومعدات موصى بها اليوم
            2. حالة السفر والقيادة على الطرقات
            3. مدى ملائمة الطقس للأنشطة الخارجية والرياضة
        """.trimIndent()        
        return GeminiRepository.generateContent(context, prompt)    
    }
}
