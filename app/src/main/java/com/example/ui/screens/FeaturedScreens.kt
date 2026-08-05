package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GeminiRepository
import com.example.data.LivePricesRepository
import com.example.model.CalcKey
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChatMessage(
    val sender: String, // "user" or "ai"
    val text: String,
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: String = SimpleDateFormat("hh:mm a", Locale("ar")).format(Date()),
    val isStreaming: Boolean = false,
    val category: String = "general"
)

data class ChatThread(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val messages: List<ChatMessage>
)

// ==========================================================
// SECTION A: OFFLINE FINANCIAL & LEGAL INTELLIGENCE ENGINE
// ==========================================================
object AiFinancialEngine {
    fun generateOfflineResponse(prompt: String): String {
        val lower = prompt.lowercase()
        return when {
            lower.contains("زكاة") || lower.contains("زكاه") -> {
                val amountMatch = Regex("\\d+([.,]\\d+)?").find(prompt)?.value?.replace(",", "")?.toDoubleOrNull() ?: 100000.0
                val zakat = amountMatch * 0.025
                val gold24k = LivePricesRepository.getGoldPricePerGramInUsd(24)
                val nisabGoldUsd = gold24k * 85.0
                
                """
                |✨ **حساب الزكاة الشرعية المحدث (نسبة 2.5%)**
                |
                |📊 **البيانات المالية المدخلة:**
                |- المبلغ الإجمالي الخاضع للزكاة: **${String.format("%,.2f", amountMatch)}**
                |- نصاب الذهب (85 جرام عيار 24): **~$${String.format("%,.2f", nisabGoldUsd)} USD**
                |
                |⚖️ **النتيجة والتوزيع الشرعي:**
                |1. **مقدار الزكاة الواجبة (2.5%):** **${String.format("%,.2f", zakat)}**
                |2. **شرط الحول:** يجب أن يكون المبلغ قد مر عليه عام هجري كامل وهو بنصاب فاصل.
                |3. **مصارف الزكاة:** الفقراء، المساكين، والعاملون عليها، وفي السبيل، والغارمون.
                |
                |💡 *ملاحظة: يمكنك ضبط مفتاح Gemini API من إعدادات (⚙️) للتحليل المتقدم.*
                """.trimMargin()
            }
            lower.contains("ذهب") || lower.contains("سبائك") -> {
                val g24 = LivePricesRepository.getGoldPricePerGramInUsd(24)
                val g21 = LivePricesRepository.getGoldPricePerGramInUsd(21)
                val g18 = LivePricesRepository.getGoldPricePerGramInUsd(18)
                val ounce = LivePricesRepository.goldOunceUsd
                """
                |⚜️ **تحليل أسعار الذهب والسبائك (تحديث حي مباشر)**
                |
                |📈 **أسعار الجرام العالمي (بالدولار الأمريكي):**
                |- **عيار 24 (السبائك النقية):** $${String.format("%.2f", g24)} / جرام
                |- **عيار 21 (الأكثر تداولاً):** $${String.format("%.2f", g21)} / جرام
                |- **عيار 18 (المشغولات):** $${String.format("%.2f", g18)} / جرام
                |- **أونصة الذهب العالمية:** $${String.format("%.2f", ounce)} USD
                |
                |📊 **نصيحة الاستثمار المالي:**
                |• **السبائك والجنيهات الذهبية:** خيار ممتاز للادخار طويل الأجل لقلة نسبة المصنعية.
                |• **تنويع المحفظة:** يُنصح بتخصيص 10% إلى 15% من رأس المال في الذهب المادي للتحوط ضد التضخم.
                """.trimMargin()
            }
            lower.contains("ميراث") || lower.contains("مواريث") || lower.contains("تركة") -> {
                """
                |⚖️ **دليل حساب المواريث والتركات الشرعية (الفرائض)**
                |
                |📌 **القواعد الأساسية لتقسيم التركة:**
                |1. **تصفية التركة أولاً:** (تجهيز المتوفى ⬅️ قضاء الديون ⬅️ تنفيذ الوصية في حدود الثلث).
                |2. **أنصبة أصحاب الفروض:**
                |   • **الزوجة:** الثمن (1/8) عند وجود الفرع الوارث، أو الربع (1/4) عند عدمه.
                |   • **الأم:** السدس (1/6) عند وجود فرع وارث أو جمع من الأخوة.
                |   • **الأب:** السدس فرضاً + الباقي تعصيباً إن لم يوجد ولد ذكر.
                |3. **مبدأ للذكر مثل حظ الأنثيين:** يطبق بين الأبناء والبنات في المتبقي تعصيباً.
                |
                |💡 *أدخل تفاصيل الورثة والمبلغ بدقة للحصول على جدول توزيع مالي مفصل.*
                """.trimMargin()
            }
            lower.contains("عقار") || lower.contains("استثمار") -> {
                """
                |🏠 **تقييم الاستثمار العقاري وحساب العائد (Cap Rate)**
                |
                |📊 **المعادلات الاقتصادية المعتمدة:**
                |1. **معدل العائد السنوي الصافي:**
                |   `معدل العائد = (الدخل الإيجاري السنوي الصافي ÷ سعر الشراء الإجمالي) × 100`
                |2. **فترة استرداد رأس المال (Payback Period):**
                |   `فترة الاسترداد = سعر شراء العقار ÷ صافي الربح السنوي`
                |
                |💡 **النسب المعيارية الممتازة:**
                |• **عائد إيجاري من 7% إلى 10%:** يُعتبر استثماراً عقارياً قوياً جداً.
                """.trimMargin()
            }
            else -> {
                """
                |💡 **المستشار المالي والشرعي الذكي (ClevCalc Pro AI)**
                |
                |أنا مستشارك الذكي المجهز ببيانات الأسواق الحية، حسابات الزكاة، المواريث، وتحليل صفقات الذهب والعملات.
                |
                |🔍 **كيف يمكنني مساعدتك؟**
                |- حساب زكاة الأموال، عروض التجارة والأنعام.
                |- أسعار الذهب والسبائك الحية وتحليلات التضخم.
                |- تقسيم التركات والمواريث الشرعية.
                |- تخطيط الميزانية الشخصية (قاعدة 50/30/20).
                |
                |🔑 *يمكنك إضافة مفتاح Gemini API الخاص بك من زر الإعدادات (⚙️) لتفعيل نماذج GPT-4o و Gemini Flash المتقدمة.*
                """.trimMargin()
            }
        }
    }
}

// ==========================================================
// SECTION B: CHAT LOCAL PERSISTENCE MANAGER
// ==========================================================
object AiChatStorage {
    private const val PREFS_NAME = "clevcalc_ai_chat_store"
    private const val KEY_THREADS_JSON = "key_saved_chat_threads"

    fun saveThreads(context: Context, threads: List<ChatThread>) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonArray = JSONArray()
            for (thread in threads.take(20)) { // Keep last 20 threads
                val threadObj = JSONObject().apply {
                    put("id", thread.id)
                    put("title", thread.title)
                    put("createdAt", thread.createdAt)
                    val msgsArr = JSONArray()
                    for (msg in thread.messages.takeLast(50)) {
                        msgsArr.put(JSONObject().apply {
                            put("id", msg.id)
                            put("sender", msg.sender)
                            put("text", msg.text)
                            put("timestamp", msg.timestamp)
                        })
                    }
                    put("messages", msgsArr)
                }
                jsonArray.put(threadObj)
            }
            prefs.edit().putString(KEY_THREADS_JSON, jsonArray.toString()).apply()
        } catch (_: Exception) {}
    }

    fun loadThreads(context: Context): List<ChatThread> {
        val list = mutableListOf<ChatThread>()
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonStr = prefs.getString(KEY_THREADS_JSON, null) ?: return emptyList()
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val threadObj = jsonArray.getJSONObject(i)
                val id = threadObj.optString("id")
                val title = threadObj.optString("title", "محادثة سابقة")
                val createdAt = threadObj.optLong("createdAt", System.currentTimeMillis())
                val msgsArr = threadObj.optJSONArray("messages") ?: JSONArray()
                val msgs = mutableListOf<ChatMessage>()
                for (j in 0 until msgsArr.length()) {
                    val mObj = msgsArr.getJSONObject(j)
                    msgs.add(
                        ChatMessage(
                            id = mObj.optString("id"),
                            sender = mObj.optString("sender"),
                            text = mObj.optString("text"),
                            timestamp = mObj.optString("timestamp")
                        )
                    )
                }
                list.add(ChatThread(id, title, createdAt, msgs))
            }
        } catch (_: Exception) {}
        return list
    }
}

// ==========================================================
// SECTION C: PDF EXPORTER FOR CONVERSATION REPORT
// ==========================================================
object AiPdfExporter {
    fun exportChatToPdf(context: Context, messages: List<ChatMessage>, title: String = "تقرير المستشار الذكي"): File? {
        return try {
            val pdfDoc = android.graphics.pdf.PdfDocument()
            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
            val page = pdfDoc.startPage(pageInfo)
            val canvas = page.canvas

            val headerPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#141926")
                style = android.graphics.Paint.Style.FILL
            }
            val goldPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#D4AF37")
                strokeWidth = 3f
            }
            val titleTextPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#D4AF37")
                textSize = 18f
                isFakeBoldText = true
                textAlign = android.graphics.Paint.Align.RIGHT
            }
            val subTitlePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 11f
                textAlign = android.graphics.Paint.Align.RIGHT
            }
            val msgTextPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#1A202C")
                textSize = 10f
            }

            // Draw Header Banner
            canvas.drawRect(0f, 0f, 595f, 70f, headerPaint)
            canvas.drawLine(0f, 70f, 595f, 70f, goldPaint)

            val dateStr = SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale("ar")).format(Date())
            canvas.drawText("ClevCalc Pro AI - $title", 570f, 32f, titleTextPaint)
            canvas.drawText("تاريخ التقرير: $dateStr | المستشار المالي والشرعي", 570f, 52f, subTitlePaint)

            var y = 100f
            for (msg in messages) {
                if (y > 780f) break // Stop if page overflows
                val isAi = msg.sender == "ai"
                val boxPaint = android.graphics.Paint().apply {
                    color = if (isAi) android.graphics.Color.parseColor("#F1F5F9") else android.graphics.Color.parseColor("#FEF3C7")
                    style = android.graphics.Paint.Style.FILL
                }
                val borderPaint = android.graphics.Paint().apply {
                    color = if (isAi) android.graphics.Color.parseColor("#CBD5E1") else android.graphics.Color.parseColor("#F59E0B")
                    style = android.graphics.Paint.Style.STROKE
                    strokeWidth = 1.5f
                }

                val senderLabel = if (isAi) "المستشار الذكي AI" else "المستخدم"
                val textLines = msg.text.chunked(75)
                val boxHeight = (textLines.size * 14f) + 28f

                val rect = android.graphics.RectF(30f, y, 565f, y + boxHeight)
                canvas.drawRoundRect(rect, 8f, 8f, boxPaint)
                canvas.drawRoundRect(rect, 8f, 8f, borderPaint)

                val labelPaint = android.graphics.Paint().apply {
                    color = if (isAi) android.graphics.Color.parseColor("#0F172A") else android.graphics.Color.parseColor("#B45309")
                    textSize = 10f
                    isFakeBoldText = true
                    textAlign = android.graphics.Paint.Align.RIGHT
                }
                canvas.drawText("$senderLabel (${msg.timestamp})", 550f, y + 16f, labelPaint)

                var lineY = y + 30f
                for (line in textLines) {
                    canvas.drawText(line, 40f, lineY, msgTextPaint)
                    lineY += 14f
                }
                y += boxHeight + 12f
            }

            // Footer
            val footerPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.GRAY
                textSize = 9f
                textAlign = android.graphics.Paint.Align.CENTER
            }
            canvas.drawText("تم إنشاء هذا التقرير بواسطة تطبيق ClevCalc Pro AI - جميع الحقوق محفوظة", 297.5f, 820f, footerPaint)

            pdfDoc.finishPage(page)

            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val pdfFile = File(downloadsDir, "ClevCalc_AI_Report_${System.currentTimeMillis()}.pdf")
            pdfDoc.writeTo(FileOutputStream(pdfFile))
            pdfDoc.close()
            pdfFile
        } catch (_: Exception) {
            null
        }
    }
}

// ==========================================================
// SECTION D: SPEECH RECOGNITION HELPER
// ==========================================================
class AiSpeechRecognizerHelper(private val context: Context) {
    private var speechRecognizer: SpeechRecognizer? = null

    fun startListening(onResult: (String) -> Unit, onError: (String) -> Unit, onStateChange: (Boolean) -> Unit) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("اليتعرف الصوتي غير مدعوم في هذا الجهاز.")
            return
        }
        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) { onStateChange(true) }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() { onStateChange(false) }
                    override fun onError(error: Int) {
                        onStateChange(false)
                        onError("لم نتمكن من التقاط الصوت بشكل واضح، حاول مرة أخرى.")
                    }
                    override fun onResults(results: Bundle?) {
                        onStateChange(false)
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            onResult(matches[0])
                        }
                    }
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar")
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            onStateChange(false)
            onError("خطأ أثناء تفعيل الميكروفون: ${e.localizedMessage}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (_: Exception) {}
    }
}

// ==========================================================
// SECTION E: TEXT TO SPEECH HELPER
// ==========================================================
class AiTtsHelper(private val context: Context) {
    private var tts: TextToSpeech? = null
    private var isReady = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val res = tts?.setLanguage(Locale("ar"))
                if (res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED) {
                    isReady = true
                }
            }
        }
    }

    fun speak(text: String) {
        if (isReady) {
            val cleanText = text.replace("*", "").replace("`", "").take(500)
            tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "AiTts")
        }
    }

    fun stop() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
    }
}

// ==========================================================
// SECTION F: RICH MARKDOWN RENDERER COMPOSABLE
// ==========================================================
@Composable
fun AiMarkdownText(
    text: String,
    colors: CustomThemeColors,
    modifier: Modifier = Modifier
) {
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val context = LocalContext.current

    val codeBlockRegex = Regex("```(.*?)```", RegexOption.DOT_MATCHES_ALL)
    val matches = codeBlockRegex.findAll(text).toList()

    if (matches.isEmpty()) {
        val annotatedString = buildAnnotatedString {
            var cursor = 0
            val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
            boldRegex.findAll(text).forEach { match ->
                append(text.substring(cursor, match.range.first))
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = colors.accent)) {
                    append(match.groupValues[1])
                }
                cursor = match.range.last + 1
            }
            if (cursor < text.length) {
                append(text.substring(cursor))
            }
        }
        Text(
            text = annotatedString,
            fontSize = 13.sp,
            color = colors.text,
            lineHeight = 20.sp,
            modifier = modifier
        )
    } else {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            var cursor = 0
            for (match in matches) {
                val beforeCode = text.substring(cursor, match.range.first).trim()
                if (beforeCode.isNotBlank()) {
                    Text(
                        text = beforeCode,
                        fontSize = 13.sp,
                        color = colors.text,
                        lineHeight = 20.sp
                    )
                }

                val codeContent = match.groupValues[1].trim()
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("جدول / كود برمجي", fontSize = 10.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(codeContent))
                                    Toast.makeText(context, "تم نسخ الكود للحافظة", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Outlined.Assignment, null, tint = Color(0xFF38BDF8), modifier = Modifier.size(12.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = codeContent,
                            fontSize = 12.sp,
                            color = Color(0xFFF8FAFC),
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            lineHeight = 18.sp
                        )
                    }
                }
                cursor = match.range.last + 1
            }
            if (cursor < text.length) {
                val remaining = text.substring(cursor).trim()
                if (remaining.isNotBlank()) {
                    Text(
                        text = remaining,
                        fontSize = 13.sp,
                        color = colors.text,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

// ==========================================================
// SECTION G: PROCEDURAL NEURAL ORB AVATAR
// ==========================================================
@Composable
fun NeuralOrbAvatar(colors: CustomThemeColors, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_anim")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.width / 2f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(colors.accent.copy(alpha = 0.4f * pulseScale), Color.Transparent),
                    center = center,
                    radius = radius * pulseScale
                ),
                radius = radius
            )

            val path = Path()
            val points = 5
            for (i in 0 until points) {
                val angle = Math.toRadians((rotationAngle + i * (360 / points)).toDouble())
                val x = center.x + (radius * 0.55f) * Math.cos(angle).toFloat()
                val y = center.y + (radius * 0.55f) * Math.sin(angle).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()

            drawPath(
                path = path,
                color = colors.accent,
                style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
    }
}

// ==========================================================
// SECTION H: MAIN AI ASSISTANT COMMERCIAL SCREEN
// ==========================================================
@Composable
fun AIAssistantScreen(colors: CustomThemeColors) {
    val context = LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current

    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var isListeningVoice by remember { mutableStateOf(false) }

    val speechHelper = remember { AiSpeechRecognizerHelper(context) }
    val ttsHelper = remember { AiTtsHelper(context) }

    val savedThreads = remember { mutableStateListOf<ChatThread>().apply { addAll(AiChatStorage.loadThreads(context)) } }

    val messages = remember {
        mutableStateListOf(
            ChatMessage("ai", "أهلاً بك في المساعد المالي والشرعي الذكي (ClevCalc Pro AI)! ⚜️\n\nأنا مجهز بخوارزميات لحساب الزكاة الشرعية، تحليل أسعار الذهب، تقسيم المواريث، وتقييم الاستثمار العقاري.\n\nكيف يمكنني خدمتك اليوم؟")
        )
    }

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    DisposableEffect(Unit) {
        onDispose {
            speechHelper.stopListening()
            ttsHelper.stop()
        }
    }

    fun saveCurrentThreadToStore() {
        if (messages.size > 1) {
            val firstUserText = messages.firstOrNull { it.sender == "user" }?.text ?: "محادثة ماليّة"
            val title = if (firstUserText.length > 25) firstUserText.take(25) + "..." else firstUserText
            val thread = ChatThread(title = title, messages = messages.toList())
            savedThreads.removeAll { it.id == thread.id }
            savedThreads.add(0, thread)
            AiChatStorage.saveThreads(context, savedThreads)
        }
    }

    fun sendMessage(msgText: String) {
        if (msgText.isBlank() || isLoading) return
        val userMsg = msgText.trim()
        inputText = ""
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        messages.add(0, ChatMessage("user", userMsg))
        isLoading = true

        coroutineScope.launch {
            listState.animateScrollToItem(0)

            val history = messages.drop(1).chunked(2).mapNotNull {
                if (it.size == 2) Pair(it[0].text, it[1].text) else null
            }

            var responseText = GeminiRepository.queryAi(context, userMsg, history)

            if (responseText.contains("لم يتم ضبط مفتاح") || responseText.contains("تعذر الاتصال")) {
                val offlineResp = AiFinancialEngine.generateOfflineResponse(userMsg)
                if (!offlineResp.contains("المستشار المالي والشرعي الذكي")) {
                    responseText = offlineResp
                }
            }

            isLoading = false
            messages.add(0, ChatMessage("ai", responseText))
            saveCurrentThreadToStore()
            listState.animateScrollToItem(0)
        }
    }

    val isKeyboardOpen by remember {
        derivedStateOf { false }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF080A0F), Color(0xFF121620))
                )
            )
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                Surface(
                    color = Color(0xFF141926).copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            NeuralOrbAvatar(colors = colors, modifier = Modifier.size(42.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("المستشار الذكي AI", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.text)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("متصل | GPT-4o / Claude 3.5", fontSize = 10.sp, color = Color(0xFF10B981))
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { showHistorySheet = true }) {
                                Icon(Icons.Filled.History, contentDescription = "السجل", tint = colors.textMuted, modifier = Modifier.size(20.dp))
                            }

                            IconButton(
                                onClick = {
                                    if (messages.size > 1) {
                                        val file = AiPdfExporter.exportChatToPdf(context, messages)
                                        if (file != null) {
                                            Toast.makeText(context, "✅ تم تصدير التقرير إلى مجلد التحميلات:\n${file.name}", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "تعذر تصدير ملف PDF", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "لا توجد رسائل لتصديرها", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Icon(Icons.Filled.PictureAsPdf, contentDescription = "تصدير PDF", tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                            }

                            IconButton(
                                onClick = {
                                    saveCurrentThreadToStore()
                                    messages.clear()
                                    messages.add(
                                        ChatMessage("ai", "أهلاً بك مجدداً! تم بدء محادثة جديدة. كيف أساعدك اليوم؟")
                                    )
                                    Toast.makeText(context, "تم بدء محادثة جديدة", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "جديد", tint = colors.accent, modifier = Modifier.size(20.dp))
                            }

                            IconButton(onClick = { showSettingsDialog = true }) {
                                Icon(Icons.Filled.Settings, contentDescription = "إعدادات", tint = colors.accent, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            },
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F141F).copy(alpha = 0.92f))
                            .border(BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.25f)))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        AnimatedVisibility(visible = inputText.isBlank()) {
                            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                                Text("💡 اقتراحات مالية وشرعية سريعة:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.accent)
                                Spacer(modifier = Modifier.height(4.dp))
                                val quickPrompts = listOf(
                                    "💰 احسب لي زكاة مال 100,000 ريال",
                                    "⚜️ سعر الذهب عيار 24 والسبائك اليوم",
                                    "⚖️ حساب المواريث (زوجة وأبناء)",
                                    "🏠 تقييم عقار استثماري بعائد 8%"
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(quickPrompts) { prompt ->
                                        Surface(
                                            color = Color(0xFF1E2638),
                                            shape = RoundedCornerShape(16.dp),
                                            border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.4f)),
                                            modifier = Modifier.clickable { sendMessage(prompt) }
                                        ) {
                                            Text(
                                                text = prompt,
                                                fontSize = 11.sp,
                                                color = Color(0xFFF1F5F9),
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (isListeningVoice) {
                                        speechHelper.stopListening()
                                        isListeningVoice = false
                                    } else {
                                        speechHelper.startListening(
                                            onResult = { text ->
                                                inputText = text
                                                Toast.makeText(context, "تم الالتقاط الصوتي بنجاح!", Toast.LENGTH_SHORT).show()
                                            },
                                            onError = { err ->
                                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                            },
                                            onStateChange = { isListeningVoice = it }
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(
                                        if (isListeningVoice) Color(0xFFEF4444).copy(alpha = 0.2f) else Color(0xFF1A2234),
                                        CircleShape
                                    )
                                    .border(1.dp, if (isListeningVoice) Color(0xFFEF4444) else Color(0xFF334155), CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isListeningVoice) Icons.Filled.MicOff else Icons.Filled.Mic,
                                    contentDescription = "صوت",
                                    tint = if (isListeningVoice) Color(0xFFEF4444) else colors.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                placeholder = { Text("أسأل المستشار الذكي عن أي شيء...", fontSize = 12.sp, color = colors.textMuted) },
                                modifier = Modifier.weight(1f),
                                maxLines = 4,
                                shape = RoundedCornerShape(20.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF141A29),
                                    unfocusedContainerColor = Color(0xFF141A29),
                                    focusedBorderColor = Color(0xFFD4AF37),
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            val isSendActive = inputText.isNotBlank() && !isLoading
                            val sendScale by animateFloatAsState(targetValue = if (isSendActive) 1.05f else 1.0f, label = "sendScale")

                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .graphicsLayer(scaleX = sendScale, scaleY = sendScale)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSendActive) Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFFD4AF37))) else Brush.linearGradient(listOf(Color(0xFF1E293B), Color(0xFF1E293B)))
                                    )
                                    .clickable(enabled = isSendActive) { sendMessage(inputText) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "إرسال",
                                    tint = if (isSendActive) Color(0xFF080A0F) else colors.textMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            LazyColumn(
                state = listState,
                reverseLayout = true,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isLoading) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color(0xFFD4AF37),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("جاري استخلاص الإجابة الحسابية والمالية المحدثة...", fontSize = 12.sp, color = Color(0xFF00F2FE))
                        }
                    }
                }

                items(messages.size, key = { index -> messages[index].id }) { index ->
                    val msg = messages[index]
                    val isAi = msg.sender == "ai"

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isAi) Arrangement.Start else Arrangement.End,
                        verticalAlignment = Alignment.Top
                    ) {
                        if (isAi) {
                            NeuralOrbAvatar(colors = colors, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Column(horizontalAlignment = if (isAi) Alignment.Start else Alignment.End) {
                            Surface(
                                color = if (isAi) Color(0xFF141926).copy(alpha = 0.9f) else Color(0xFFD4AF37),
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isAi) 4.dp else 16.dp,
                                    bottomEnd = if (isAi) 16.dp else 4.dp
                                ),
                                border = BorderStroke(1.dp, if (isAi) Color(0xFFD4AF37).copy(alpha = 0.3f) else Color(0xFFF59E0B)),
                                modifier = Modifier.widthIn(max = 320.dp)
                            ) {
                                SelectionContainer {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        if (isAi) {
                                            AiMarkdownText(text = msg.text, colors = colors)
                                        } else {
                                            Text(
                                                text = msg.text,
                                                fontSize = 13.sp,
                                                color = Color(0xFF080A0F),
                                                fontWeight = FontWeight.Medium,
                                                lineHeight = 20.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = msg.timestamp,
                                                fontSize = 9.sp,
                                                color = if (isAi) colors.textMuted else Color(0xFF334155)
                                            )

                                            if (isAi) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    IconButton(
                                                        onClick = { ttsHelper.speak(msg.text) },
                                                        modifier = Modifier.size(22.dp)
                                                    ) {
                                                        Icon(Icons.Filled.VolumeUp, "قراءة", tint = Color(0xFF38BDF8), modifier = Modifier.size(13.dp))
                                                    }

                                                    IconButton(
                                                        onClick = {
                                                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(msg.text))
                                                            Toast.makeText(context, "تم نسخ النص بالحافظة", Toast.LENGTH_SHORT).show()
                                                        },
                                                        modifier = Modifier.size(22.dp)
                                                    ) {
                                                        Icon(Icons.AutoMirrored.Outlined.Assignment, "نسخ", tint = Color(0xFFD4AF37), modifier = Modifier.size(13.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Saved Conversations Drawer/Sheet Dialog
    if (showHistorySheet) {
        AlertDialog(
            onDismissRequest = { showHistorySheet = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.History, contentDescription = null, tint = colors.accent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("سجل المحادثات السابقة", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                if (savedThreads.isEmpty()) {
                    Text("لا توجد محادثات محفوظة حتى الآن.", fontSize = 13.sp, color = colors.textMuted)
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(savedThreads) { thread ->
                            Surface(
                                color = colors.surface2,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        messages.clear()
                                        messages.addAll(thread.messages)
                                        showHistorySheet = false
                                        Toast
                                            .makeText(context, "تم تحميل محادثة: ${thread.title}", Toast.LENGTH_SHORT)
                                            .show()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(thread.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = colors.text)
                                        Text("${thread.messages.size} رسائل", fontSize = 10.sp, color = colors.textMuted)
                                    }
                                    Icon(Icons.Filled.ArrowForwardIos, null, tint = colors.accent, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHistorySheet = false }) {
                    Text("إغلاق", color = colors.accent)
                }
            },
            containerColor = colors.surface,
            titleContentColor = colors.text,
            textContentColor = colors.text
        )
    }

    // AI Settings Dialog
    if (showSettingsDialog) {
        var keyInput by remember { mutableStateOf(GeminiRepository.getStoredApiKey(context)) }
        var testStatus by remember { mutableStateOf<String?>(null) }
        var isTesting by remember { mutableStateOf(false) }

        var selectedModelId by remember { mutableStateOf(GeminiRepository.getSelectedModel(context)) }
        var showModelDropdown by remember { mutableStateOf(false) }
        val models = GeminiRepository.AVAILABLE_MODELS

        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Settings, contentDescription = null, tint = colors.accent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إعدادات الذكاء الاصطناعي", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column {
                    Text(
                        "للحصول على أفضل أداء، يرجى إدخال مفتاح Gemini API الخاص بك. يمكنك الحصول عليه مجاناً من Google AI Studio.",
                        fontSize = 12.sp,
                        color = colors.textMuted,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        label = { Text("Gemini API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.accent,
                            focusedLabelColor = colors.accent
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("النموذج المفضل:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.text)
                    Spacer(modifier = Modifier.height(8.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            color = colors.surface2,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showModelDropdown = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val selectedName = models.find { it.id == selectedModelId }?.displayName ?: selectedModelId
                                Text(selectedName, fontSize = 14.sp, color = colors.text)
                                Text("▼", fontSize = 10.sp, color = colors.textMuted)
                            }
                        }
                        DropdownMenu(
                            expanded = showModelDropdown,
                            onDismissRequest = { showModelDropdown = false },
                            modifier = Modifier.background(colors.surface)
                        ) {
                            models.forEach { model ->
                                DropdownMenuItem(
                                    text = { Text(model.displayName, color = colors.text) },
                                    onClick = {
                                        selectedModelId = model.id
                                        showModelDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    if (testStatus != null) {
                        Text(
                            text = testStatus ?: "",
                            color = if (testStatus!!.contains("نجاح")) Color(0xFF10B981) else Color(0xFFEF4444),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = {
                                keyInput = ""
                                GeminiRepository.clearApiKey(context)
                                testStatus = "تم مسح المفتاح والعودة للافتراضي"
                            }
                        ) {
                            Text("مسح المفتاح", color = Color(0xFFEF4444))
                        }

                        OutlinedButton(
                            onClick = {
                                if (keyInput.isNotBlank()) {
                                    isTesting = true
                                    testStatus = "جاري الاختبار..."
                                    coroutineScope.launch {
                                        val (success, msg) = GeminiRepository.testApiKey(keyInput)
                                        testStatus = if (success) "✅ نجاح الاتصال: $msg" else "❌ فشل الاتصال: $msg"
                                        isTesting = false
                                        if (success) {
                                            GeminiRepository.saveApiKey(context, keyInput)
                                            GeminiRepository.saveSelectedModel(context, selectedModelId)
                                        }
                                    }
                                }
                            },
                            enabled = !isTesting && keyInput.isNotBlank(),
                            border = BorderStroke(1.dp, colors.accent)
                        ) {
                            if (isTesting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Text("اختبار الاتصال", color = colors.accent)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        GeminiRepository.saveApiKey(context, keyInput)
                        GeminiRepository.saveSelectedModel(context, selectedModelId)
                        showSettingsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                ) {
                    Text("حفظ وإغلاق")
                }
            },
            containerColor = colors.surface,
            titleContentColor = colors.text,
            textContentColor = colors.text
        )
    }
}

@Composable
fun LivePricesScreen(colors: CustomThemeColors) {
    val context = LocalContext.current
    
    var selectedCurrency by remember { mutableStateOf<com.example.data.CurrencyRate>(LivePricesRepository.getSelectedCurrency(context)) }
    var showCurrencyPicker by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var selectedKarat by remember { mutableStateOf(24) }
    val coroutineScope = rememberCoroutineScope()
    
    // Category state selector
    var activeCategory by remember { mutableStateOf("العملات") }

    fun refreshData() {
        isRefreshing = true
        coroutineScope.launch {
            LivePricesRepository.refreshLivePrices(context)
            isRefreshing = false
        }
    }

    LaunchedEffect(Unit) {
        if (!LivePricesRepository.isLiveDataLoaded) {
            refreshData()
        }
    }

    // Calculations based on selected currency
    val currRate = selectedCurrency.rateVsUsd
    val currCode = selectedCurrency.code

    // Gold Prices
    val goldGramUsd = LivePricesRepository.getGoldPricePerGramInUsd(selectedKarat)
    val goldGramCurr = goldGramUsd * currRate

    // Silver Prices
    val silverGramUsd = LivePricesRepository.silverGramUsd
    val silverGramCurr = silverGramUsd * currRate

    ToolScreenScaffold(
        colors = colors,
        icon = AppIcons.forCalc(CalcKey.LIVE_PRICES),
        title = "الأسعار الحية الفورية",
        subtitle = "متابعة أسعار صرف العملات والذهب والفضة والمعادن الثمينة والنفط العالمية"
    ) {
        // 1. Live Price Status Header
        FrostedGlassCard(
            colors = colors,
            variant = FrostedGlassCardVariant.Compact,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(colors.positive)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("مباشر ومحدّث", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = colors.positive)
                }

                Text(
                    if (LivePricesRepository.isLiveDataLoaded) LivePricesRepository.lastUpdatedText else "آخر تحديث: جاري التحميل...",
                    fontSize = 11.sp,
                    color = colors.textMuted
                )

                IconButton(
                    onClick = { refreshData() },
                    enabled = !isRefreshing,
                    modifier = Modifier.size(36.dp)
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = colors.accent)
                    } else {
                        Icon(Icons.Filled.Refresh, contentDescription = "تحديث", tint = colors.accent)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Preferred Currency Picker Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surface.copy(alpha = 0.75f))
                    .border(1.dp, colors.border.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .clickable { showCurrencyPicker = true }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(selectedCurrency.flag, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(selectedCurrency.nameAr, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.text)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Filled.ArrowDropDown, null, tint = colors.textMuted, modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Gold & Silver Realtime Market Summary
        SectionHeader(colors = colors, title = "أسعار الذهب والمعادن الثمينة اليوم")
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Gold Gram Card
            FrostedGlassCard(
                colors = colors,
                variant = FrostedGlassCardVariant.Standard,
                modifier = Modifier.weight(1f)
            ) {
                Text("الذهب (عيار $selectedKarat)", fontSize = 11.sp, color = colors.textMuted)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$currCode ${LivePricesRepository.formatNumber(goldGramCurr)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.accent
                )
                Text("لكل جرام", fontSize = 9.sp, color = colors.textMuted)
            }

            // Silver Gram Card
            FrostedGlassCard(
                colors = colors,
                variant = FrostedGlassCardVariant.Standard,
                modifier = Modifier.weight(1f)
            ) {
                Text("الفضة الصافية", fontSize = 11.sp, color = colors.textMuted)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$currCode ${LivePricesRepository.formatNumber(silverGramCurr)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.text
                )
                Text("لكل جرام", fontSize = 9.sp, color = colors.textMuted)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Karat Preferred Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("تخصيص العيار:", fontSize = 11.sp, color = colors.textMuted)
            listOf(24, 22, 21, 18).forEach { k ->
                GlassChip(
                    colors = colors,
                    label = "عيار $k",
                    selected = selectedKarat == k,
                    onClick = { selectedKarat = k }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 4. Live Currencies Grid with Search filter
        var currencySearchQuery by remember { mutableStateOf("") }
        SectionHeader(colors = colors, title = "صرف العملات الأجنبية مقابل $currCode")
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = currencySearchQuery,
            onValueChange = { currencySearchQuery = it },
            placeholder = { Text("ابحث عن عملة أو دولة معينة...", fontSize = 12.sp, color = colors.textMuted) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.accent,
                unfocusedBorderColor = colors.border.copy(alpha = 0.3f),
                focusedTextColor = colors.text,
                unfocusedTextColor = colors.text
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        val filteredCurrencies = remember(currencySearchQuery, currCode) {
            LivePricesRepository.currencies.filter { c ->
                c.code != currCode && (
                    currencySearchQuery.isBlank() ||
                    c.nameAr.contains(currencySearchQuery, ignoreCase = true) ||
                    c.code.contains(currencySearchQuery, ignoreCase = true) ||
                    c.countryAr.contains(currencySearchQuery, ignoreCase = true)
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            filteredCurrencies.forEach { c ->
                val convertedRate = LivePricesRepository.convertCurrency(1.0, c.code, currCode)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.surface.copy(alpha = 0.5f))
                        .border(1.dp, colors.border.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(c.flag, fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(c.nameAr, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = colors.text)
                                Text("${c.countryAr} (${c.code})", fontSize = 10.sp, color = colors.textMuted)
                            }
                        }
                        Text(
                            text = "= $currCode ${LivePricesRepository.formatNumber(convertedRate, 2)}",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = colors.accent
                        )
                    }
                }
            }
        }
    }

    // Currency Picker Dialog
    if (showCurrencyPicker) {
        var searchQuery by remember { mutableStateOf("") }
        val filteredList = LivePricesRepository.currencies.filter { c ->
            searchQuery.isBlank() ||
            c.nameAr.contains(searchQuery, ignoreCase = true) ||
            c.code.contains(searchQuery, ignoreCase = true) ||
            c.countryAr.contains(searchQuery, ignoreCase = true)
        }

        AlertDialog(
            onDismissRequest = { showCurrencyPicker = false },
            confirmButton = {
                TextButton(onClick = { showCurrencyPicker = false }) {
                    Text("إغلاق", color = colors.accent, fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Public, null, tint = colors.accent, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("اختر دولة العملة", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("ابحث عن الدولة أو العملة...", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    ) {
                        items(filteredList) { item ->
                            val isSelected = item.code == selectedCurrency.code
                            Surface(
                                color = if (isSelected) colors.accent.copy(alpha = 0.15f) else colors.surface,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clickable {
                                        selectedCurrency = item
                                        LivePricesRepository.setSelectedCurrency(context, item.code)
                                        showCurrencyPicker = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(item.flag, fontSize = 20.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(item.nameAr, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.text)
                                            Text("${item.countryAr} (${item.code})", fontSize = 11.sp, color = colors.textMuted)
                                        }
                                    }

                                    if (isSelected) {
                                        Text("✓", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.accent)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            containerColor = colors.surface,
            titleContentColor = colors.text,
            textContentColor = colors.text
        )
    }
}
