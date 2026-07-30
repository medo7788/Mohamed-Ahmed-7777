package com.example.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit

data class CurrencyRate(
    val code: String,
    val nameAr: String,
    val flag: String,
    val countryAr: String,
    val rateVsUsd: Double // 1 USD = rateVsUsd
)

data class MetalPrice(
    val symbol: String,
    val nameAr: String,
    val unit: String,
    val priceUsd: Double,
    val change24h: Double
)

data class CommodityPrice(
    val nameAr: String,
    val symbol: String,
    val icon: String,
    val unit: String,
    val priceUsd: Double,
    val change24h: Double,
    val isEstimated: Boolean = true
)

object LivePricesRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    var isLiveDataLoaded by mutableStateOf(false)
        private set

    var lastUpdatedText by mutableStateOf("غير محدث")
        private set

    val currencies = mutableStateListOf(
        CurrencyRate("EGP", "جنيه مصري", "", "مصر", 48.65),
        CurrencyRate("SAR", "ريال سعودي", "", "السعودية", 3.75),
        CurrencyRate("AED", "درهم إماراتي", "", "الإمارات", 3.67),
        CurrencyRate("KWD", "دينار كويتي", "", "الكويت", 0.306),
        CurrencyRate("QAR", "ريال قطري", "", "قطر", 3.64),
        CurrencyRate("BHD", "دينار بحريني", "", "البحرين", 0.376),
        CurrencyRate("OMR", "ريال عماني", "", "عمان", 0.385),
        CurrencyRate("JOD", "دينار أردني", "", "الأردن", 0.709),
        CurrencyRate("DZD", "دينار جزائري", "", "الجزائر", 134.2),
        CurrencyRate("MAD", "درهم مغربي", "", "المغرب", 9.85),
        CurrencyRate("TND", "دينار تونسي", "", "تونس", 3.12),
        CurrencyRate("LYD", "دينار ليبي", "", "ليبيا", 4.82),
        CurrencyRate("SDG", "جنيه سوداني", "", "السودان", 601.0),
        CurrencyRate("IQD", "دينار عراقي", "", "العراق", 1310.0),
        CurrencyRate("LBP", "ليرة لبنانية", "", "لبنان", 89500.0),
        CurrencyRate("SYP", "ليرة سورية", "", "سوريا", 13000.0),
        CurrencyRate("YER", "ريال يمني", "", "اليمن", 250.0),
        CurrencyRate("ILS", "شيكل", "", "فلسطين", 3.65),
        CurrencyRate("MRU", "أوقية موريتانية", "", "موريتانيا", 39.5),
        CurrencyRate("SOS", "شلن صومالي", "", "الصومال", 570.0),
        CurrencyRate("TRY", "ليرة تركية", "", "تركيا", 32.85),
        CurrencyRate("USD", "دولار أمريكي", "", "الولايات المتحدة", 1.0),
        CurrencyRate("EUR", "يورو", "", "أوروبا", 0.922),
        CurrencyRate("GBP", "جنيه إسترليني", "", "المملكة المتحدة", 0.778),
        CurrencyRate("CAD", "دولار كندي", "", "كندا", 1.368),
        CurrencyRate("AUD", "دولار أسترالي", "", "أستراليا", 1.512),
        CurrencyRate("JPY", "ين ياباني", "", "اليابان", 155.4),
        CurrencyRate("CNY", "يوان صيني", "", "الصين", 7.25),
        CurrencyRate("INR", "روبية هندية", "", "الهند", 83.5),
        CurrencyRate("MYR", "رينغيت ماليزي", "", "ماليزيا", 4.70)
    )

    fun getSelectedCurrency(context: Context): CurrencyRate {
        val prefs = context.getSharedPreferences("clevcalc_prefs", Context.MODE_PRIVATE)
        val code = prefs.getString("selected_currency", "EGP") ?: "EGP"
        return currencies.firstOrNull { it.code == code } ?: currencies[0]
    }

    fun setSelectedCurrency(context: Context, code: String) {
        val prefs = context.getSharedPreferences("clevcalc_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("selected_currency", code).apply()
    }

    fun convertCurrency(amount: Double, fromCode: String, toCode: String): Double {
        val fromRate = currencies.firstOrNull { c -> c.code == fromCode }?.rateVsUsd ?: 1.0
        val toRate = currencies.firstOrNull { c -> c.code == toCode }?.rateVsUsd ?: 1.0
        val amountInUsd = amount / fromRate
        return amountInUsd * toRate
    }

    // Base Gold Price per Ounce in USD
    var goldOunceUsd by mutableDoubleStateOf(2380.0)
        private set

    var silverGramUsd by mutableDoubleStateOf(2.038) // ~ 99.16 EGP / 48.65
        private set

    var platinumGramUsd by mutableDoubleStateOf(55.38) // ~ 2,694.53 EGP / 48.65
        private set

    var palladiumGramUsd by mutableDoubleStateOf(44.19) // ~ 2,150.01 EGP / 48.65
        private set

    const val GRAMS_PER_OUNCE = 31.1034768

    fun getGoldPricePerGram24KInUsd(): Double = goldOunceUsd / GRAMS_PER_OUNCE

    fun getGoldPricePerGramInUsd(karat: Int): Double {
        val base24k = getGoldPricePerGram24KInUsd()
        return base24k * (karat.toDouble() / 24.0)
    }

    val metalPrices = mutableStateListOf(
        MetalPrice("XAU-24", "ذهب عيار 24", "جرام", getGoldPricePerGramInUsd(24), 0.45),
        MetalPrice("XAU-22", "ذهب عيار 22", "جرام", getGoldPricePerGramInUsd(22), 0.42),
        MetalPrice("XAU-21", "ذهب عيار 21", "جرام", getGoldPricePerGramInUsd(21), 0.40),
        MetalPrice("XAU-18", "ذهب عيار 18", "جرام", getGoldPricePerGramInUsd(18), 0.38),
        MetalPrice("XAG", "الفضة النقية", "جرام", silverGramUsd, -0.12),
        MetalPrice("XPT", "البلاتين", "جرام", platinumGramUsd, 0.20),
        MetalPrice("XPD", "البلاديوم", "جرام", palladiumGramUsd, -0.80)
    )

    val commodityPrices = mutableStateListOf(
        CommodityPrice("خام برنت", "BRENT", "oil", "برميل", 84.015, 0.65),
        CommodityPrice("نفط غرب تكساس", "WTI", "gasoline", "برميل", 79.95, 0.55),
        CommodityPrice("الغاز الطبيعي", "GAS", "natural_gas", "MMBtu", 2.44, -1.20),
        CommodityPrice("النحاس الخام", "COPPER", "copper", "رطل", 4.34, 0.15)
    )

    private fun updateMetalList() {
        if (metalPrices.size >= 4) {
            metalPrices[0] = MetalPrice("XAU-24", "ذهب عيار 24", "جرام", getGoldPricePerGramInUsd(24), 0.45)
            metalPrices[1] = MetalPrice("XAU-22", "ذهب عيار 22", "جرام", getGoldPricePerGramInUsd(22), 0.42)
            metalPrices[2] = MetalPrice("XAU-21", "ذهب عيار 21", "جرام", getGoldPricePerGramInUsd(21), 0.40)
            metalPrices[3] = MetalPrice("XAU-18", "ذهب عيار 18", "جرام", getGoldPricePerGramInUsd(18), 0.38)
        }
    }

    suspend fun refreshLivePrices(appContext: Context? = null): Boolean = withContext(Dispatchers.IO) {
        var success = false
        try {
            // 1. Fetch Currency Rates from open.er-api.com
            val currReq = Request.Builder().url("https://open.er-api.com/v6/latest/USD").build()
            val currResp = client.newCall(currReq).execute()
            if (currResp.isSuccessful) {
                val jsonStr = currResp.body?.string() ?: ""
                if (jsonStr.isNotBlank()) {
                    val jObj = JSONObject(jsonStr)
                    val ratesObj = jObj.optJSONObject("rates")
                    if (ratesObj != null) {
                        val updatedList = currencies.map { c ->
                            if (ratesObj.has(c.code)) {
                                val newRate = ratesObj.getDouble(c.code)
                                c.copy(rateVsUsd = newRate)
                            } else c
                        }
                        currencies.clear()
                        currencies.addAll(updatedList)
                        success = true
                    }
                }
            }

            // 2. Fetch Gold Price from gold-api.com
            val goldReq = Request.Builder().url("https://api.gold-api.com/price/XAU").build()
            val goldResp = client.newCall(goldReq).execute()
            if (goldResp.isSuccessful) {
                val jsonStr = goldResp.body?.string() ?: ""
                if (jsonStr.isNotBlank()) {
                    val jObj = JSONObject(jsonStr)
                    val price = jObj.optDouble("price", 0.0)
                    if (price > 1000.0) {
                        goldOunceUsd = price
                        updateMetalList()
                        success = true
                    }
                }
            }

            // 3. Fetch Silver Price from gold-api.com
            val silverReq = Request.Builder().url("https://api.gold-api.com/price/XAG").build()
            val silverResp = client.newCall(silverReq).execute()
            if (silverResp.isSuccessful) {
                val jsonStr = silverResp.body?.string() ?: ""
                if (jsonStr.isNotBlank()) {
                    val jObj = JSONObject(jsonStr)
                    val price = jObj.optDouble("price", 0.0)
                    if (price > 0) {
                        silverGramUsd = price / GRAMS_PER_OUNCE
                        if (metalPrices.size > 4) {
                            metalPrices[4] = MetalPrice("XAG", "الفضة النقية", "جرام", silverGramUsd, -0.12)
                        }
                    }
                }
            }

            if (success) {
                isLiveDataLoaded = true
                val sdf = java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale("ar"))
                lastUpdatedText = "آخر تحديث: ${sdf.format(java.util.Date())}"
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 4. Fallback/Enhancement using Gemini Search Grounding
        if (appContext != null) {
            try {
                val prompt = """
                    استخدم أداة Google Search للبحث عن أحدث أسعار اليوم بالدولار الأمريكي. 
                    قم بإرجاع كائن JSON حصراً يحتوي على المفاتيح التالية بأرقام فقط (بدون أي نصوص أو markdown):
                    {
                        "gold_ounce_usd": 2400.50,
                        "silver_ounce_usd": 30.15,
                        "platinum_ounce_usd": 950.00,
                        "palladium_ounce_usd": 1000.00,
                        "brent_oil_usd": 85.20,
                        "wti_oil_usd": 81.10,
                        "natural_gas_usd": 2.50,
                        "copper_lb_usd": 4.10,
                        "usd_to_egp": 48.50,
                        "usd_to_sar": 3.75
                    }
                """.trimIndent()
                val response = GeminiRepository.fetchGroundedData(appContext, prompt)
                if (response != null) {
                    val cleaned = response.replace("```json", "").replace("```", "").trim()
                    val jObj = JSONObject(cleaned)
                    
                    if (jObj.has("gold_ounce_usd") && !success) {
                        goldOunceUsd = jObj.getDouble("gold_ounce_usd")
                        updateMetalList()
                        success = true
                    }
                    if (jObj.has("silver_ounce_usd") && !success) {
                        silverGramUsd = jObj.getDouble("silver_ounce_usd") / GRAMS_PER_OUNCE
                        if (metalPrices.size > 4) metalPrices[4] = metalPrices[4].copy(priceUsd = silverGramUsd)
                    }
                    if (jObj.has("platinum_ounce_usd")) {
                        platinumGramUsd = jObj.getDouble("platinum_ounce_usd") / GRAMS_PER_OUNCE
                        if (metalPrices.size > 5) metalPrices[5] = metalPrices[5].copy(priceUsd = platinumGramUsd)
                    }
                    if (jObj.has("palladium_ounce_usd")) {
                        palladiumGramUsd = jObj.getDouble("palladium_ounce_usd") / GRAMS_PER_OUNCE
                        if (metalPrices.size > 6) metalPrices[6] = metalPrices[6].copy(priceUsd = palladiumGramUsd)
                    }
                    if (jObj.has("brent_oil_usd")) commodityPrices[0] = commodityPrices[0].copy(priceUsd = jObj.getDouble("brent_oil_usd"))
                    if (jObj.has("wti_oil_usd")) commodityPrices[1] = commodityPrices[1].copy(priceUsd = jObj.getDouble("wti_oil_usd"))
                    if (jObj.has("natural_gas_usd")) commodityPrices[2] = commodityPrices[2].copy(priceUsd = jObj.getDouble("natural_gas_usd"))
                    if (jObj.has("copper_lb_usd")) commodityPrices[3] = commodityPrices[3].copy(priceUsd = jObj.getDouble("copper_lb_usd"))
                    
                    if (!success) {
                        if (jObj.has("usd_to_egp")) {
                            val egpRate = jObj.getDouble("usd_to_egp")
                            val updatedList = currencies.map { c ->
                                if (c.code == "EGP") c.copy(rateVsUsd = egpRate)
                                else if (c.code == "SAR" && jObj.has("usd_to_sar")) c.copy(rateVsUsd = jObj.getDouble("usd_to_sar"))
                                else c
                            }
                            currencies.clear()
                            currencies.addAll(updatedList)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (success) {
            isLiveDataLoaded = true
            val sdf = java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale("ar"))
            lastUpdatedText = "آخر تحديث: ${sdf.format(java.util.Date())}"
        }

        return@withContext success
    }

    fun formatNumber(valNumber: Double, decimals: Int = 2): String {
        val pattern = if (decimals == 0) "#,##0" else "#,##0." + "0".repeat(decimals)
        val symbols = java.text.DecimalFormatSymbols(java.util.Locale.US)
        val df = java.text.DecimalFormat(pattern, symbols)
        return df.format(valNumber)
    }
}
