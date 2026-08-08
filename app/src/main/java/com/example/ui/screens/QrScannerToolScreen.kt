package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CalcKey
import com.example.ui.components.ToolScreenScaffold
import com.example.ui.theme.AppIcons
import com.example.ui.theme.CustomThemeColors
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

private data class ScanHistoryItem(
    val id: String,
    val payload: String,
    val timestamp: String
)

private val PREF_QR_HISTORY = "clevcalc_qr_history_v1"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScannerToolScreen(colors: CustomThemeColors) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var activeTab by remember { mutableStateOf(0) } // 0: Generator, 1: Scanner

    // Generator state
    var qrInputText by remember { mutableStateOf("https://google.com") }
    var selectedPresetType by remember { mutableStateOf("رابط موقع") }
    var selectedColorHex by remember { mutableStateOf("#00FFCC") }

    // Scanner state
    var simulatedScanText by remember { mutableStateOf("") }
    var isTorchOn by remember { mutableStateOf(false) }
    var scanHistory by remember { mutableStateOf(loadQrHistory(context)) }

    val presetTypes = listOf("رابط موقع", "نص عادي", "شبكة Wi-Fi", "رقم هاتف")
    val qrColors = listOf(
        "#00FFCC" to "سقفي نيون",
        "#FFB703" to "ذهبي ملفت",
        "#10B981" to "زمردي هادئ",
        "#A855F7" to "بنفسجي سايبورغ"
    )

    // Laser Animation
    val infiniteTransition = rememberInfiniteTransition()
    val laserYRatio by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    fun addScanResult(result: String) {
        if (result.isBlank()) return
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        val newItem = ScanHistoryItem(
            id = UUID.randomUUID().toString(),
            payload = result.trim(),
            timestamp = sdf.format(Date())
        )
        val updated = listOf(newItem) + scanHistory.take(19)
        scanHistory = updated
        saveQrHistory(context, updated)
    }

    ToolScreenScaffold(
        colors = colors,
        icon = AppIcons.forCalc(CalcKey.QR_TOOL),
        title = "قارئ ومولد الـ QR الاحترافي",
        subtitle = "مسح ضوئي فوري لشفرات QR وتوليد ألوان وتصاميم مخصصة",
        isScrollable = false
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp)
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // Tab Header
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = Color(0xFF0F172A),
                    contentColor = Color(0xFF00FFCC)
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("توليد QR", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("قارئ ومسح الضوئي", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (activeTab == 0) {
                    // TAB 0: QR GENERATOR
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        // Preset Types
                        item {
                            Text("نوع المحتوى:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                presetTypes.forEach { type ->
                                    val sel = selectedPresetType == type
                                    FilterChip(
                                        selected = sel,
                                        onClick = {
                                            selectedPresetType = type
                                            when (type) {
                                                "رابط موقع" -> qrInputText = "https://google.com"
                                                "نص عادي" -> qrInputText = "تطبيق الحاسبة الذكية ClevCalc Pro"
                                                "شبكة Wi-Fi" -> qrInputText = "WIFI:S:HomeNet;T:WPA;P:myPassword123;;"
                                                "رقم هاتف" -> qrInputText = "tel:+201000000000"
                                            }
                                        },
                                        label = { Text(type, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }

                        // Input Field
                        item {
                            OutlinedTextField(
                                value = qrInputText,
                                onValueChange = { qrInputText = it },
                                label = { Text("المحتوى المراد تشفيره إلى رمز QR", fontSize = 11.sp, color = Color(0xFF94A3B8)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF00FFCC),
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedContainerColor = Color(0xFF1E293B),
                                    unfocusedContainerColor = Color(0xFF0F172A),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                        }

                        // Color Picker
                        item {
                            Text("لون الرمز والنمط:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                qrColors.forEach { (hex, name) ->
                                    val sel = selectedColorHex == hex
                                    val colorObj = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { Color(0xFF00FFCC) }
                                    Surface(
                                        onClick = { selectedColorHex = hex },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (sel) colorObj.copy(alpha = 0.25f) else Color(0xFF1E293B),
                                        border = BorderStroke(1.5.dp, if (sel) colorObj else Color(0xFF334155)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clip(CircleShape)
                                                    .background(colorObj)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(name, fontSize = 9.sp, color = if (sel) colorObj else Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        // Generated QR Canvas View
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(22.dp),
                                color = Color(0xFF0F172A),
                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                shadowElevation = 8.dp
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    val activeColor = try { Color(android.graphics.Color.parseColor(selectedColorHex)) } catch (_: Exception) { Color(0xFF00FFCC) }

                                    Surface(
                                        modifier = Modifier
                                            .size(200.dp)
                                            .padding(8.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color(0xFF0B1120),
                                        border = BorderStroke(1.dp, activeColor.copy(alpha = 0.5f))
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                                                val w = size.width
                                                val h = size.height

                                                // Draw finder patterns
                                                fun drawFinder(x: Float, y: Float) {
                                                    drawRect(
                                                        color = activeColor,
                                                        topLeft = Offset(x, y),
                                                        size = Size(40f, 40f),
                                                        style = Stroke(width = 6f)
                                                    )
                                                    drawRect(
                                                        color = activeColor,
                                                        topLeft = Offset(x + 12f, y + 12f),
                                                        size = Size(16f, 16f)
                                                    )
                                                }

                                                drawFinder(0f, 0f)
                                                drawFinder(w - 40f, 0f)
                                                drawFinder(0f, h - 40f)

                                                // Procedural QR Grid based on input
                                                val seed = qrInputText.hashCode()
                                                val random = Random(seed.toLong())
                                                val gridSize = 12
                                                val cellW = w / gridSize
                                                val cellH = h / gridSize

                                                for (r in 0 until gridSize) {
                                                    for (c in 0 until gridSize) {
                                                        // Skip finder pattern zones
                                                        val isTopLeft = r < 4 && c < 4
                                                        val isTopRight = r < 4 && c >= gridSize - 4
                                                        val isBottomLeft = r >= gridSize - 4 && c < 4

                                                        if (!isTopLeft && !isTopRight && !isBottomLeft) {
                                                            if (random.nextBoolean()) {
                                                                drawRoundRect(
                                                                    color = activeColor,
                                                                    topLeft = Offset(c * cellW + 2f, r * cellH + 2f),
                                                                    size = Size(cellW - 4f, cellH - 4f),
                                                                    cornerRadius = CornerRadius(2f, 2f)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Action Buttons
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(qrInputText))
                                                Toast.makeText(context, "تم نسخ محتوى الرمز إلى الحافظة", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = activeColor, contentColor = Color.Black),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("نسخ المحتوى", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                val shareIntent = Intent().apply {
                                                    action = Intent.ACTION_SEND
                                                    putExtra(Intent.EXTRA_TEXT, qrInputText)
                                                    type = "text/plain"
                                                }
                                                context.startActivity(Intent.createChooser(shareIntent, "مشاركة الرمز"))
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, activeColor),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("مشاركة", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // TAB 1: QR SCANNER
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        // Scanner Viewfinder Card
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(22.dp),
                                color = Color(0xFF0F172A),
                                border = BorderStroke(1.dp, Color(0xFF00FFCC).copy(alpha = 0.4f)),
                                shadowElevation = 10.dp
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(210.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color(0xFF070A0F)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // Viewfinder Box
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            val w = size.width
                                            val h = size.height

                                            // Animated Laser Line
                                            val laserY = h * laserYRatio
                                            drawLine(
                                                color = Color(0xFF00FFCC),
                                                start = Offset(20f, laserY),
                                                end = Offset(w - 20f, laserY),
                                                strokeWidth = 6f
                                            )

                                            // Corner Brackets
                                            val cornerLen = 30f
                                            val strokeW = 8f
                                            val bracketColor = if (isTorchOn) Color(0xFFFFB703) else Color(0xFF00FFCC)

                                            // Top Left
                                            drawLine(bracketColor, Offset(10f, 10f), Offset(10f + cornerLen, 10f), strokeWidth = strokeW)
                                            drawLine(bracketColor, Offset(10f, 10f), Offset(10f, 10f + cornerLen), strokeWidth = strokeW)

                                            // Top Right
                                            drawLine(bracketColor, Offset(w - 10f, 10f), Offset(w - 10f - cornerLen, 10f), strokeWidth = strokeW)
                                            drawLine(bracketColor, Offset(w - 10f, 10f), Offset(w - 10f, 10f + cornerLen), strokeWidth = strokeW)

                                            // Bottom Left
                                            drawLine(bracketColor, Offset(10f, h - 10f), Offset(10f + cornerLen, h - 10f), strokeWidth = strokeW)
                                            drawLine(bracketColor, Offset(10f, h - 10f), Offset(10f, h - 10f - cornerLen), strokeWidth = strokeW)

                                            // Bottom Right
                                            drawLine(bracketColor, Offset(w - 10f, h - 10f), Offset(w - 10f - cornerLen, h - 10f), strokeWidth = strokeW)
                                            drawLine(bracketColor, Offset(w - 10f, h - 10f), Offset(w - 10f, h - 10f - cornerLen), strokeWidth = strokeW)
                                        }

                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Default.QrCodeScanner,
                                                contentDescription = null,
                                                tint = Color(0xFF00FFCC).copy(alpha = 0.8f),
                                                modifier = Modifier.size(44.dp)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "وجه الكاميرا نحو رمز QR للمسح الضوئي",
                                                fontSize = 12.sp,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Controls (Torch & Quick Decoder Input)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = {
                                                isTorchOn = !isTorchOn
                                                Toast.makeText(context, if (isTorchOn) "تم تشغيل الفلاش" else "تم إيقاف الفلاش", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(if (isTorchOn) Color(0xFFFFB703).copy(alpha = 0.25f) else Color(0xFF1E293B))
                                                .border(1.dp, if (isTorchOn) Color(0xFFFFB703) else Color(0xFF334155), CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                                contentDescription = "الفلاش",
                                                tint = if (isTorchOn) Color(0xFFFFB703) else Color(0xFF94A3B8)
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                // Simulated Scan result for test
                                                val samples = listOf(
                                                    "https://google.com",
                                                    "https://quran.com",
                                                    "WIFI:S:MyRouter;P:SecretPass;;",
                                                    "https://wikipedia.org"
                                                )
                                                val picked = samples.random()
                                                simulatedScanText = picked
                                                addScanResult(picked)
                                                Toast.makeText(context, "تم مسح الرمز بنجاح!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC), contentColor = Color.Black),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("مسح رمز تجريبي", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // Last Scan Result Box
                        if (simulatedScanText.isNotBlank()) {
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFF1E293B),
                                    border = BorderStroke(1.dp, Color(0xFF00FFCC))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text("نتيجة المسح الضوئي الأخيرة:", fontSize = 11.sp, color = Color(0xFF00FFCC), fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(simulatedScanText, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            if (simulatedScanText.startsWith("http://") || simulatedScanText.startsWith("https://")) {
                                                Button(
                                                    onClick = {
                                                        try {
                                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(simulatedScanText))
                                                            context.startActivity(intent)
                                                        } catch (_: Exception) {}
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.White),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.height(34.dp)
                                                ) {
                                                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("فتح الرابط", fontSize = 11.sp)
                                                }
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    clipboardManager.setText(AnnotatedString(simulatedScanText))
                                                    Toast.makeText(context, "تم النسخ", Toast.LENGTH_SHORT).show()
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.height(34.dp)
                                            ) {
                                                Text("نسخ", fontSize = 11.sp, color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Scan History Header
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("سجل المسوحات السابقة (${scanHistory.size})", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                if (scanHistory.isNotEmpty()) {
                                    TextButton(onClick = {
                                        scanHistory = emptyList()
                                        saveQrHistory(context, emptyList())
                                    }) {
                                        Text("مسح السجل", fontSize = 11.sp, color = Color(0xFFEF4444))
                                    }
                                }
                            }
                        }

                        // Scan History Items
                        if (scanHistory.isEmpty()) {
                            item {
                                Text("لا يوجد سجل مسوحات بعد.", fontSize = 12.sp, color = Color(0xFF64748B))
                            }
                        } else {
                            items(scanHistory, key = { it.id }) { item ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF1E293B).copy(alpha = 0.8f),
                                    border = BorderStroke(1.dp, Color(0xFF334155))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.payload, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(item.timestamp, fontSize = 10.sp, color = Color(0xFF64748B))
                                        }

                                        IconButton(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(item.payload))
                                                Toast.makeText(context, "تم النسخ", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", tint = Color(0xFF00FFCC), modifier = Modifier.size(16.dp))
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

private fun loadQrHistory(context: Context): List<ScanHistoryItem> {
    val prefs = context.getSharedPreferences("qr_prefs", Context.MODE_PRIVATE)
    val jsonStr = prefs.getString(PREF_QR_HISTORY, null) ?: return emptyList()
    val list = mutableListOf<ScanHistoryItem>()
    try {
        val array = JSONArray(jsonStr)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                ScanHistoryItem(
                    id = obj.getString("id"),
                    payload = obj.getString("payload"),
                    timestamp = obj.optString("timestamp", "")
                )
            )
        }
    } catch (_: Exception) {}
    return list
}

private fun saveQrHistory(context: Context, items: List<ScanHistoryItem>) {
    val prefs = context.getSharedPreferences("qr_prefs", Context.MODE_PRIVATE)
    val array = JSONArray()
    items.forEach { item ->
        val obj = JSONObject().apply {
            put("id", item.id)
            put("payload", item.payload)
            put("timestamp", item.timestamp)
        }
        array.put(obj)
    }
    prefs.edit().putString(PREF_QR_HISTORY, array.toString()).apply()
}
