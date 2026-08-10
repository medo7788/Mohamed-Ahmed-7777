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
        AIModel("gemini-3.6-flash", "Gemini 3.6 Flash (افتراضي)"),
        AIModel("gemini-3.5-flash-lite", "Gemini 3.5 Flash Lite"),
        AIModel("gemini-3.5-flash", "Gemini 3.5 Flash"),
        AIModel("gemini-3.1-pro-preview", "Gemini 3.1 Pro Preview"),
        AIModel("gemini-3.1-flash-lite", "Gemini 3.1 Flash Lite"),
        AIModel("gemini-3-flash-preview", "Gemini 3 Flash Preview")
    )

    private const val KEY_SELECTED_MODEL = "selected_gemini_model"

    fun getSelectedModel(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SELECTED_MODEL, "gemini-3.6-flash") ?: "gemini-3.6-flash"
    }

    fun saveSelectedModel(context: Context, model: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SELECTED_MODEL, model).apply()
    }


    fun getStoredApiKey(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val customObfuscated = prefs.getString(KEY_CUSTOM_API_KEY, "") ?: ""
        if (customObfuscated.isNotBlank()) {
            return try {
                val decodedBytes = android.util.Base64.decode(customObfuscated, android.util.Base64.DEFAULT)
                String(decodedBytes, Charsets.UTF_8).trim()
            } catch (e: Exception) {
                customObfuscated.trim() // Fallback to plain text if older key was stored before obfuscation
            }
        }
        val buildKey = BuildConfig.GEMINI_API_KEY
        if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") return buildKey.trim()
        return ""
    }

    fun saveApiKey(context: Context, key: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val obfuscated = android.util.Base64.encodeToString(key.trim().toByteArray(Charsets.UTF_8), android.util.Base64.DEFAULT)
        prefs.edit().putString(KEY_CUSTOM_API_KEY, obfuscated).apply()
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
            put("tools", JSONArray().apply {
                put(JSONObject().apply {
                    put("googleSearch", JSONObject())
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
        if (context == null) return@withContext "خطأ: السياق غير متوفر."
        return@withContext queryAi(context, prompt)
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

        val selectedModel = getSelectedModel(context)
        val models = listOf(selectedModel, "gemini-2.5-flash", "gemini-2.0-flash", "gemini-1.5-flash").distinct()
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

        val jsonBodyWithTools = JSONObject().apply {
            put("contents", contentsArray)
            put("systemInstruction", systemInstruction)
            put("tools", JSONArray().apply {
                put(JSONObject().apply {
                    put("googleSearch", JSONObject())
                })
            })
        }

        val jsonBodySimple = JSONObject().apply {
            put("contents", contentsArray)
            put("systemInstruction", systemInstruction)
        }

        for (model in models) {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            val payloads = listOf(jsonBodyWithTools, jsonBodySimple)
            for (payload in payloads) {
                val requestBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
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
                        lastErrorMsg = parseGoogleError(response.code, responseText)
                    }
                } catch (e: Exception) {
                    lastErrorMsg = "عذراً، حدث خطأ أثناء الاتصال بالشبكة: ${e.localizedMessage}"
                }
            }
        }

        return@withContext if (lastErrorMsg.isNotBlank()) lastErrorMsg else "عذراً، تعذر الاتصال بـ Gemini."
    }

    suspend fun testApiKey(apiKey: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) return@withContext Pair(false, "يرجى إدخال مفتاح API أولاً.")

        val testPrompt = "قل 'مرحباً بك' فقط."
        val testModels = listOf("gemini-1.5-flash", "gemini-2.0-flash", "gemini-2.5-flash")
        var lastErr = ""

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply { put(JSONObject().put("text", testPrompt)) })
                })
            })
        }

        for (m in testModels) {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$m:generateContent?key=$cleanKey"
            val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder().url(url).post(requestBody).build()

            try {
                val response = client.newCall(request).execute()
                val text = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    return@withContext Pair(true, "✅ الاتصال ناجح ومفتاح API يعمل بنجاح على نموذج $m!")
                } else {
                    lastErr = parseGoogleError(response.code, text)
                }
            } catch (e: Exception) {
                lastErr = "❌ خطأ في الشبكة: ${e.localizedMessage}"
            }
        }

        return@withContext Pair(false, lastErr.ifBlank { "فشل اختبار الاتصال بمفتاح API." })
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

            val response = queryAi(context, fullPrompt)
            
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
            code == 404 || (code == 400 && msg.contains("model", ignoreCase = true)) ->
                "⚠️ نموذج الذكاء الاصطناعي المحدد غير متاح لقناتك الحالية. تم التبديل إلى النموذج التلقائي."
            code == 429 ->
                "⏱️ استنفدت الحصة المؤقتة لطلبات Gemini المترادفة. يرجى الانتظار بضع ثوانٍ والإعادة."
            code == 403 ->
                "🚫 تم رفض الإذن بطلب هذا النموذج على مفتاحك الحسابي (تأكد من تفعيل Generative Language API)."
            else ->
                "⚠️ خطأ من خادم Google ($code): $msg"
        }
    }
}
