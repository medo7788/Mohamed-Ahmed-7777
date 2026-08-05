package com.example.data

import android.content.Context
import com.example.domain.QiblaMath
import org.json.JSONArray
import org.json.JSONObject

data class QiblaHistoryItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val cityName: String,
    val lat: Double,
    val lng: Double,
    val qiblaAngle: Double,
    val distanceKm: Double,
    val timestampMs: Long = System.currentTimeMillis()
)

data class MajorCity(
    val nameAr: String,
    val nameEn: String,
    val countryAr: String,
    val lat: Double,
    val lng: Double
)

class QiblaRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("qibla_compass_prefs", Context.MODE_PRIVATE)

    companion object {
        val MAJOR_ISLAMIC_CITIES = listOf(
            MajorCity("مكة المكرمة", "Makkah", "السعودية", 21.422487, 39.826206),
            MajorCity("المدينة المنورة", "Madinah", "السعودية", 24.4672, 39.6112),
            MajorCity("الرياض", "Riyadh", "السعودية", 24.7136, 46.6753),
            MajorCity("جدة", "Jeddah", "السعودية", 21.5433, 39.1728),
            MajorCity("القاهرة", "Cairo", "مصر", 30.0444, 31.2357),
            MajorCity("الإسكندرية", "Alexandria", "مصر", 31.2001, 29.9187),
            MajorCity("القدس الشريف", "Jerusalem", "فلسطين", 31.7683, 35.2137),
            MajorCity("عمان", "Amman", "الأردن", 31.9539, 35.9106),
            MajorCity("دمشق", "Damascus", "سوريا", 33.5138, 36.2765),
            MajorCity("بيروت", "Beirut", "لبنان", 33.8938, 35.5018),
            MajorCity("بغداد", "Baghdad", "العراق", 33.3152, 44.3661),
            MajorCity("الكويت", "Kuwait City", "الكويت", 29.3759, 47.9774),
            MajorCity("المنامة", "Manama", "البحرين", 26.2285, 50.5860),
            MajorCity("الدوحة", "Doha", "قطر", 25.2854, 51.5310),
            MajorCity("أبوظبي", "Abu Dhabi", "الإمارات", 24.4539, 54.3773),
            MajorCity("دبي", "Dubai", "الإمارات", 25.2048, 55.2708),
            MajorCity("مسقط", "Muscat", "عُمان", 23.5880, 58.3829),
            MajorCity("صنعاء", "Sanaa", "اليمن", 15.3694, 44.1910),
            MajorCity("تونس", "Tunis", "تونس", 36.8065, 10.1815),
            MajorCity("الجزائر", "Algiers", "الجزائر", 36.7538, 3.0588),
            MajorCity("الرباط", "Rabat", "المغرب", 34.0209, -6.8416),
            MajorCity("الدار البيضاء", "Casablanca", "المغرب", 33.5731, -7.5898),
            MajorCity("طرابلس", "Tripoli", "ليبيا", 32.8872, 13.1913),
            MajorCity("الخرطوم", "Khartoum", "السودان", 15.5007, 32.5599),
            MajorCity("نواكشوط", "Nouakchott", "موريتانيا", 18.0735, -15.9582),
            MajorCity("إسطنبول", "Istanbul", "تركيا", 41.0082, 28.9784),
            MajorCity("أنقرة", "Ankara", "تركيا", 39.9334, 32.8597),
            MajorCity("جاكرتا", "Jakarta", "إندونيسيا", -6.2088, 106.8456),
            MajorCity("كوالالمبور", "Kuala Lumpur", "ماليزيا", 3.1390, 101.6869),
            MajorCity("إسلام آباد", "Islamabad", "باكستان", 33.6844, 73.0479),
            MajorCity("دكا", "Dhaka", "بنگلاديش", 23.8103, 90.4125),
            MajorCity("لندن", "London", "بريطانيا", 51.5074, -0.1278),
            MajorCity("باريس", "Paris", "فرنسا", 48.8566, 2.3522),
            MajorCity("برلين", "Berlin", "ألمانيا", 52.5200, 13.4050),
            MajorCity("نيويورك", "New York", "أمريكا", 40.7128, -74.0060),
            MajorCity("شيكاغو", "Chicago", "أمريكا", 41.8781, -87.6298),
            MajorCity("سيدني", "Sydney", "أستراليا", -33.8688, 151.2093),
            MajorCity("طوكيو", "Tokyo", "اليابان", 35.6762, 139.6503)
        )
    }

    fun saveHistoryItem(item: QiblaHistoryItem) {
        val currentHistory = getHistory().toMutableList()
        // Remove duplicate city entry if exists
        currentHistory.removeAll { it.cityName == item.cityName }
        currentHistory.add(0, item)

        // Keep maximum 10 items
        val trimmed = currentHistory.take(10)

        val jsonArray = JSONArray()
        trimmed.forEach { h ->
            val obj = JSONObject().apply {
                put("id", h.id)
                put("cityName", h.cityName)
                put("lat", h.lat)
                put("lng", h.lng)
                put("qiblaAngle", h.qiblaAngle)
                put("distanceKm", h.distanceKm)
                put("timestampMs", h.timestampMs)
            }
            jsonArray.put(obj)
        }

        prefs.edit().putString("qibla_history_json", jsonArray.toString()).apply()
    }

    fun getHistory(): List<QiblaHistoryItem> {
        val jsonStr = prefs.getString("qibla_history_json", null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<QiblaHistoryItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    QiblaHistoryItem(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        cityName = obj.getString("cityName"),
                        lat = obj.getDouble("lat"),
                        lng = obj.getDouble("lng"),
                        qiblaAngle = obj.getDouble("qiblaAngle"),
                        distanceKm = obj.getDouble("distanceKm"),
                        timestampMs = obj.optLong("timestampMs", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clearHistory() {
        prefs.edit().remove("qibla_history_json").apply()
    }

    fun saveLastLocation(lat: Double, lng: Double, cityName: String) {
        prefs.edit()
            .putFloat("last_lat", lat.toFloat())
            .putFloat("last_lng", lng.toFloat())
            .putString("last_city", cityName)
            .apply()
    }

    fun getLastLocation(): Triple<Double, Double, String>? {
        if (!prefs.contains("last_lat") || !prefs.contains("last_lng")) return null
        val lat = prefs.getFloat("last_lat", 0f).toDouble()
        val lng = prefs.getFloat("last_lng", 0f).toDouble()
        val city = prefs.getString("last_city", "موقعي الحالية") ?: "موقعي"
        return Triple(lat, lng, city)
    }
}
