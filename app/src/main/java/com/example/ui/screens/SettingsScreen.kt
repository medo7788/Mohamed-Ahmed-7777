package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.MainViewModel
import com.example.data.GeminiRepository
import com.example.ui.components.FrostedGlassCard
import com.example.ui.components.FrostedGlassCardVariant
import com.example.ui.components.SectionHeader
import com.example.ui.components.ToolScreenScaffold
import com.example.ui.theme.AppThemeKey
import com.example.ui.theme.CustomThemeColors
import kotlinx.coroutines.launch

/**
 * شاشة الإعدادات العامة للتطبيق مع خيارات المظهر والأذان ومفتاح الذكاء الاصطناعي (Gemini API)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    colors: CustomThemeColors,
    viewModel: MainViewModel? = null,
    isDarkTheme: Boolean = true,
    onToggleTheme: () -> Unit = {},
    onOpenAdhanSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenPrivacy: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val themeKeyFromVm = viewModel?.currentThemeKey?.collectAsState()?.value
    val activeDarkState = if (themeKeyFromVm != null) (themeKeyFromVm == AppThemeKey.ELEGANT_DARK) else isDarkTheme
    val handleToggleTheme: () -> Unit = {
        if (viewModel != null) {
            viewModel.toggleTheme(context)
        } else {
            onToggleTheme()
        }
    }

    // Clipboard Manager
    val clipboardManager = LocalClipboardManager.current

    // Gemini API Key state
    var apiKeyInput by remember { mutableStateOf(GeminiRepository.getStoredApiKey(context)) }
    var selectedModelId by remember { mutableStateOf(GeminiRepository.getSelectedModel(context)) }
    var isKeyVisible by remember { mutableStateOf(false) }
    var isTestingKey by remember { mutableStateOf(false) }
    var testResultMsg by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var isModelDropdownExpanded by remember { mutableStateOf(false) }

    ToolScreenScaffold(
        colors = colors,
        icon = Icons.Default.Settings,
        title = "إعدادات التطبيق",
        subtitle = "المظهر والتنبيهات ومفتاح الذكاء الاصطناعي والمعلومات العامة"
    ) {
        SectionHeader(colors = colors, title = "المظهر")
        FrostedGlassCard(colors = colors, variant = FrostedGlassCardVariant.Standard) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("الوضع الداكن / الفاتح", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.text)
                    Text(
                        if (activeDarkState) "الوضع الداكن مفعّل حاليًا" else "الوضع الفاتح مفعّل حاليًا",
                        fontSize = 12.sp, color = colors.textMuted
                    )
                }
                Switch(checked = activeDarkState, onCheckedChange = { handleToggleTheme() })
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION: GEMINI AI API KEY SETTINGS
        SectionHeader(colors = colors, title = "الذكاء الاصطناعي (Gemini AI)")
        FrostedGlassCard(colors = colors, variant = FrostedGlassCardVariant.Standard) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFA855F7), modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("مفتاح API الخاص بالذكاء الاصطناعي", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.text)
                        Text("ادخل مفتاح Gemini الخاص بك لتشغيل المساعد والتحليلات مجاناً", fontSize = 11.sp, color = colors.textMuted)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // API Key Outlined TextField
                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = {
                        apiKeyInput = it
                        testResultMsg = null
                    },
                    label = { Text("Gemini API Key (AIStudio)", fontSize = 12.sp) },
                    placeholder = { Text("AIzaSy...", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                                Icon(
                                    imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "تبديل الرؤية",
                                    tint = colors.textMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            if (apiKeyInput.isNotBlank()) {
                                IconButton(onClick = {
                                    clipboardManager.setText(AnnotatedString(apiKeyInput))
                                    Toast.makeText(context, "تم نسخ المفتاح للذاكرة", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "نسخ المفتاح",
                                        tint = colors.textMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(onClick = {
                                    apiKeyInput = ""
                                    GeminiRepository.clearApiKey(context)
                                    Toast.makeText(context, "تم مسح المفتاح", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "مسح المفتاح",
                                        tint = colors.textMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.text,
                        unfocusedTextColor = colors.text,
                        focusedLabelColor = colors.accent
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // AI Model Selection Dropdown
                Text("نموذج المعالجة المختارة:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.text)
                Spacer(modifier = Modifier.height(6.dp))
                ExposedDropdownMenuBox(
                    expanded = isModelDropdownExpanded,
                    onExpandedChange = { isModelDropdownExpanded = !isModelDropdownExpanded }
                ) {
                    val currentModelName = GeminiRepository.AVAILABLE_MODELS.find { it.id == selectedModelId }?.displayName ?: selectedModelId
                    OutlinedTextField(
                        value = currentModelName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isModelDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.text,
                            unfocusedTextColor = colors.text
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = isModelDropdownExpanded,
                        onDismissRequest = { isModelDropdownExpanded = false }
                    ) {
                        GeminiRepository.AVAILABLE_MODELS.forEach { model ->
                            DropdownMenuItem(
                                text = { Text(model.displayName, fontSize = 13.sp) },
                                onClick = {
                                    selectedModelId = model.id
                                    GeminiRepository.saveSelectedModel(context, model.id)
                                    isModelDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons Row (Save, Test, Clear)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isTestingKey = true
                                testResultMsg = null
                                val (success, msg) = GeminiRepository.testApiKey(apiKeyInput)
                                isTestingKey = false
                                testResultMsg = Pair(success, msg)
                                if (success) {
                                    GeminiRepository.saveApiKey(context, apiKeyInput)
                                    GeminiRepository.saveSelectedModel(context, selectedModelId)
                                    Toast.makeText(context, "تم حفظ واختبار المفتاح بنجاح!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = !isTestingKey && apiKeyInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isTestingKey) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("جاري الاختبار...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("اختبار وحفظ المفتاح", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (apiKeyInput.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                GeminiRepository.clearApiKey(context)
                                apiKeyInput = ""
                                testResultMsg = null
                                Toast.makeText(context, "تم مسح المفتاح المخصص", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f))
                        ) {
                            Text("مسح", fontSize = 12.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Test Connection Status Banner
                testResultMsg?.let { (success, msg) ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (success) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, if (success) Color(0xFF10B981).copy(alpha = 0.4f) else Color(0xFFEF4444).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (success) Color(0xFF10B981) else Color(0xFFEF4444),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = msg,
                                fontSize = 11.sp,
                                color = if (success) Color(0xFF10B981) else Color(0xFFEF4444),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SectionHeader(colors = colors, title = "الصلاة والأذان")
        FrostedGlassCard(colors = colors, variant = FrostedGlassCardVariant.Standard, onClick = onOpenAdhanSettings) {
            SettingsRow(colors, Icons.Default.NotificationsActive, "إعدادات الأذان والمؤذن", "صوت التنبيه، الاهتزاز، وأوقات التذكير")
        }

        Spacer(modifier = Modifier.height(16.dp))
        SectionHeader(colors = colors, title = "عن التطبيق")
        FrostedGlassCard(colors = colors, variant = FrostedGlassCardVariant.Standard, onClick = onOpenAbout) {
            SettingsRow(colors, Icons.Default.Info, "عن ClevCalc Pro", "الإصدار الحالي وبيانات التطبيق")
        }
        Spacer(modifier = Modifier.height(10.dp))
        FrostedGlassCard(colors = colors, variant = FrostedGlassCardVariant.Standard, onClick = onOpenPrivacy) {
            SettingsRow(colors, Icons.Default.PrivacyTip, "سياسة الخصوصية", "كيف بنستخدم بيانات موقعك")
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsRow(colors: CustomThemeColors, icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = colors.accent, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.text)
                Text(subtitle, fontSize = 12.sp, color = colors.textMuted)
            }
        }
        Icon(Icons.Default.ChevronLeft, null, tint = colors.textMuted, modifier = Modifier.size(20.dp))
    }
}
