package com.example.data

import android.content.Context
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    private const val PREFS_NAME = "clevcalc_ai_prefs"
    private const val KEY_CUSTOM_API_KEY = "custom_gemini_api_key"

    
    data class AIModel(val id: String, val displayName: String)

    val AVAILABLE_MODELS = listOf(
        AIModel("gemini-3.1-flash-lite-preview", "Gemini 3.1 Flash Lite")
    )

    private const val KEY_SELECTED_MODEL = "selected_gemini_model"

    fun getSelectedModel(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SELECTED_MODEL, "gemini-3.1-flash-lite-preview") ?: "gemini-3.1-flash-lite-preview"
    }

    fun saveSelectedModel(context: Context, model: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SELECTED_MODEL, model).apply()
    }


    fun getStoredApiKey(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val custom = prefs.getString(KEY_CUSTOM_API_KEY, "") ?: ""
        if (custom.isNotBlank()) return custom.trim()
        val buildKey = BuildConfig.GEMINI_API_KEY
        if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") return buildKey.trim()
        return "AIzaSyD3pTDbGJlv9yTn40lkDvtAl12W6pdkXJc"
    }

    fun saveApiKey(context: Context, key: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CUSTOM_API_KEY, key.trim()).apply()
    }

    fun clearApiKey(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_CUSTOM_API_KEY).apply()
    }

    suspend fun fetchGroundedData(context: Context?, prompt: String): String? = withContext(Dispatchers.IO) {
        val apiKey = context?.let { getStoredApiKey(it) }
            ?: BuildConfig.GEMINI_API_KEY.takeIf { it.isNotBlank() && it != "MY_GEMINI_API_KEY" }
            ?: ""
        if (apiKey.isBlank()) return@withContext null

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply { put(JSONObject().put("text", prompt)) })
                })
            })
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val model = getSelectedModel(context!!)
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
        val request = Request.Builder().url(url).post(requestBody).build()
        try {
            val response = client.newCall(request).execute()
            val responseText = response.body?.string() ?: ""
            if (response.isSuccessful && responseText.isNotBlank()) {
                val jsonResponse = JSONObject(responseText)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCand = candidates.getJSONObject(0)
                    val content = firstCand.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val text = parts.getJSONObject(0).optString("text")
                        if (text.isNotBlank()) return@withContext text
                    }
                }
            }
        } catch (_: Exception) {
        }
        return@withContext null
    }

    suspend fun generateContent(context: Context?, prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = context?.let { getStoredApiKey(it) }
            ?: BuildConfig.GEMINI_API_KEY.takeIf { it.isNotBlank() && it != "MY_GEMINI_API_KEY" }
            ?: ""

        if (apiKey.isBlank()) {
            return@withContext "🔑 تتطلب هذه الميزة إضافة مفتاح Gemini API. يرجى إدخال مفتاحك الخاص في شاشة إعدادات المساعد الذكي."
        }

        val primaryModel = getSelectedModel(context!!)
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$primaryModel:generateContent?key=$apiKey"

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply { put(JSONObject().put("text", prompt)) })
                })
            })
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder().url(url).post(requestBody).build()

        try {
            val response = client.newCall(request).execute()
            val responseText = response.body?.string() ?: ""
            if (response.isSuccessful && responseText.isNotBlank()) {
                val jsonResponse = JSONObject(responseText)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCand = candidates.getJSONObject(0)
                    val content = firstCand.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val text = parts.getJSONObject(0).optString("text")
                        if (text.isNotBlank()) return@withContext text
                    }
                }
            } else {
                return@withContext parseGoogleError(response.code, responseText)
            }
        } catch (e: Exception) {
            return@withContext "عذراً، حدث خطأ في الاتصال بالشبكة: ${e.localizedMessage}"
        }

        return@withContext "تعذر الحصول على الاستجابة الحية من الذكاء الاصطناعي."
    }

    suspend fun queryAi(
        context: Context,
        prompt: String,
        conversationHistory: List<Pair<String, String>> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getStoredApiKey(context)
        if (apiKey.isBlank()) {
            return@withContext "🔑 لم يتم ضبط مفتاح API للذكاء الاصطناعي.\n\nاضغط على أيقونة الإعدادات (⚙️) بالأعلى لإدخال مفتاح Gemini الخاص بك مجاناً من Google AI Studio."
        }

        val models = listOf(getSelectedModel(context))
        var lastErrorMsg = ""

        // Build live market context string to inject into AI prompt
        val gold24k = LivePricesRepository.getGoldPricePerGramInUsd(24)
        val egpRate = LivePricesRepository.currencies.firstOrNull { c -> c.code == "EGP" }?.rateVsUsd ?: 48.65
        val sarRate = LivePricesRepository.currencies.firstOrNull { c -> c.code == "SAR" }?.rateVsUsd ?: 3.75

        val systemPromptText = """
            أنت "ClevCalc AI" — المساعد الذكي الخبير المالي والديني والحسابي في تطبيق ClevCalc Pro.
            
            📊 بيانات السوق الحية المحدثة حالياً في التطبيق:
            - سعر الذهب عيار 24: ${String.format("%.2f", gold24k)} دولار/جرام (الأونصة = ${String.format("%.2f", LivePricesRepository.goldOunceUsd)} USD)
            - سعر الذهب عيار 21: ${String.format("%.2f", LivePricesRepository.getGoldPricePerGramInUsd(21))} دولار/جرام
            - سعر صرف USD = $egpRate جنيه مصري | $sarRate ريال سعودي.
            
            🎯 قواعد الرد:
            - تجيب باللغة العربية الفصحى الواضحة والمنظمة.
            - استخدم الجداول والنقاط والرموز عند الحاجة.
            - قدّم إجابات دقيقة ومباشرة في الرياضيات والزكاة والأسعار والحسابات الصحية.
        """.trimIndent()

        val contentsArray = JSONArray()

        // System Instruction
        val systemInstruction = JSONObject().apply {
            put("parts", JSONArray().apply {
                put(JSONObject().put("text", systemPromptText))
            })
        }

        // Add history
        for ((userMsg, modelMsg) in conversationHistory) {
            val userContent = JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply { put(JSONObject().put("text", userMsg)) })
            }
            val modelContent = JSONObject().apply {
                put("role", "model")
                put("parts", JSONArray().apply { put(JSONObject().put("text", modelMsg)) })
            }
            contentsArray.put(userContent)
            contentsArray.put(modelContent)
        }

        // Current user prompt
        val currentContent = JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().apply { put(JSONObject().put("text", prompt)) })
        }
        contentsArray.put(currentContent)

        val jsonBody = JSONObject().apply {
            put("contents", contentsArray)
            put("systemInstruction", systemInstruction)
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        for (model in models) {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            try {
                val response = client.newCall(request).execute()
                val responseText = response.body?.string() ?: ""

                if (response.isSuccessful && responseText.isNotBlank()) {
                    val jsonResponse = JSONObject(responseText)
                    val candidates = jsonResponse.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val firstCand = candidates.getJSONObject(0)
                        val content = firstCand.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            val text = parts.getJSONObject(0).optString("text")
                            if (text.isNotBlank()) return@withContext text
                        }
                    }
                } else {
                    lastErrorMsg = parseGoogleError(response.code, responseText)
                }
            } catch (e: Exception) {
                lastErrorMsg = "عذراً، حدث خطأ أثناء الاتصال بالشبكة: ${e.localizedMessage}"
            }
        }

        return@withContext if (lastErrorMsg.isNotBlank()) lastErrorMsg else "عذراً، تعذر الاتصال بـ Gemini."
    }

    suspend fun testApiKey(apiKey: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val testPrompt = "قل 'مرحباً بك' فقط."
        val primaryModel = "gemini-3.1-flash-lite-preview"
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$primaryModel:generateContent?key=${apiKey.trim()}"

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply { put(JSONObject().put("text", testPrompt)) })
                })
            })
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder().url(url).post(requestBody).build()

        try {
            val response = client.newCall(request).execute()
            val text = response.body?.string() ?: ""
            if (response.isSuccessful) {
                return@withContext Pair(true, "✅ الاتصال ناجح ومفتاح API يعمل بشكل ممتاز!")
            } else {
                return@withContext Pair(false, parseGoogleError(response.code, text))
            }
        } catch (e: Exception) {
            return@withContext Pair(false, "❌ خطأ في الاتصال بالشبكة: ${e.localizedMessage}")
        }
    }

    suspend fun askEconomicExpert(context: Context, prompt: String, countryContext: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val fullPrompt = """
                أنت خبير اقتصادي ومالي احترافي مدمج في تطبيق زكي.
                الدولة المستهدفة بالتحليل: $countryContext.
                قم بالبحث عن أحدث البيانات الاقتصادية الحية (أسعار الذهب، العملات المحلية مقابل الدولار، معدلات التضخم، المؤشرات) لعام 2026.
                
                سؤال المستخدم: $prompt
                
                شروط الإجابة:
                1. تقديم أرقام ومؤشرات دقيقة ومحدثة مع التنويه بأحدث التطورات.
                2. صياغة واضحة ومباشرة باللغة العربية.
            """.trimIndent()

            val response = generateContent(context, fullPrompt)
            
            if (response.contains("حدث خطأ") || response.contains("تعذر الحصول")) {
                throw Exception(response)
            }
            
            response.ifBlank { "عذراً، لم أتمكن من الحصول على إجابة حالياً. يرجى المحاولة لاحقاً." }
        }
    }

    private fun parseGoogleError(code: Int, body: String): String {
        var msg = "HTTP $code"
        try {
            val obj = JSONObject(body)
            val errObj = obj.optJSONObject("error")
            if (errObj != null) {
                msg = errObj.optString("message", msg)
            }
        } catch (_: Exception) {}

        return when {
            code == 400 && msg.contains("key", ignoreCase = true) ->
                "❌ مفتاح API غير صحيح. يرجى التأكد من نسخه بشكل صحيح من Google AI Studio."
            code == 429 ->
                "⏱️ تجاوزت الحصة اليومية المتاحة لمفتاح Gemini المجاني. يرجى الانتظار قليلاً أو إدخال مفتاح آخر."
            code == 403 ->
                "🚫 تم رفض الإذن بطلب هذا النموذج على مفتاحك الحسابي."
            else ->
                "⚠️ خطأ من خادم Google ($code): $msg"
        }
    }
}
