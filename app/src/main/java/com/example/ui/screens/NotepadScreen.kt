package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

private data class NoteItem(
    val id: String,
    val title: String,
    val body: String,
    val category: String,
    val isPinned: Boolean,
    val date: String
)

private val PREF_NOTES_DATA = "clevcalc_notes_items_v1"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotepadScreen(colors: CustomThemeColors) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var notesList by remember { mutableStateOf(loadNotes(context)) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("الكل") }
    var showEditorModal by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<NoteItem?>(null) }

    // Form inputs
    var inputTitle by remember { mutableStateOf("") }
    var inputBody by remember { mutableStateOf("") }
    var inputCategory by remember { mutableStateOf("عام") }
    var inputIsPinned by remember { mutableStateOf(false) }

    val categories = listOf("الكل", "شخصي", "عمل", "أفكار", "مهام", "عام")

    fun updateAndSaveNotes(newList: List<NoteItem>) {
        notesList = newList
        saveNotes(context, newList)
    }

    val filteredNotes = notesList.filter { note ->
        val matchesCategory = selectedCategory == "الكل" || note.category == selectedCategory
        val matchesSearch = searchQuery.isBlank() ||
                note.title.contains(searchQuery, ignoreCase = true) ||
                note.body.contains(searchQuery, ignoreCase = true) ||
                note.category.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }.sortedWith(compareByDescending<NoteItem> { it.isPinned }.thenByDescending { it.date })

    ToolScreenScaffold(
        colors = colors,
        icon = AppIcons.forCalc(CalcKey.NOTES),
        title = "المفكرة الذكية",
        subtitle = "تدوين الملاحظات والأفكار وقوائم المهام بكل سهولة وأمان",
        isScrollable = false
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp)
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // 1. Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("بحث في الملاحظات...", fontSize = 12.sp, color = Color(0xFF64748B)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF00FFCC)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "مسح", tint = Color(0xFF94A3B8))
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00FFCC),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF0F172A),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Category Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val sel = selectedCategory == cat
                        FilterChip(
                            selected = sel,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 11.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF00FFCC).copy(alpha = 0.25f),
                                selectedLabelColor = Color(0xFF00FFCC),
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color(0xFF94A3B8)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = if (sel) Color(0xFF00FFCC) else Color(0xFF334155),
                                selectedBorderColor = Color(0xFF00FFCC),
                                enabled = true,
                                selected = sel
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Notes List
                if (filteredNotes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.StickyNote2,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(52.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (notesList.isEmpty()) "المفكرة فارغة حالياً" else "لا توجد ملاحظات تطابق البحث",
                                fontSize = 14.sp,
                                color = Color(0xFF94A3B8)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "اضغط على زر '+' بالأسفل لإنشاء ملاحظة جديدة",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        items(filteredNotes, key = { it.id }) { note ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        editingNote = note
                                        inputTitle = note.title
                                        inputBody = note.body
                                        inputCategory = note.category
                                        inputIsPinned = note.isPinned
                                        showEditorModal = true
                                    },
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF1E293B).copy(alpha = 0.90f),
                                border = BorderStroke(
                                    1.dp,
                                    if (note.isPinned) Color(0xFFFFB703).copy(alpha = 0.5f) else Color(0xFF334155)
                                )
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (note.isPinned) {
                                                Icon(
                                                    imageVector = Icons.Default.PushPin,
                                                    contentDescription = "مثبتة",
                                                    tint = Color(0xFFFFB703),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }
                                            Text(
                                                text = note.title.ifBlank { "بدون عنوان" },
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF0F172A)
                                        ) {
                                            Text(
                                                text = note.category,
                                                fontSize = 10.sp,
                                                color = Color(0xFF00FFCC),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    if (note.body.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = note.body,
                                            fontSize = 12.sp,
                                            color = Color(0xFFCBD5E1),
                                            maxLines = 3
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = note.date,
                                            fontSize = 10.sp,
                                            color = Color(0xFF64748B)
                                        )

                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            IconButton(
                                                onClick = {
                                                    clipboardManager.setText(AnnotatedString("${note.title}\n${note.body}"))
                                                    Toast.makeText(context, "تم نسخ الملاحظة إلى الحافظة", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                                            }

                                            IconButton(
                                                onClick = {
                                                    val sendIntent = Intent().apply {
                                                        action = Intent.ACTION_SEND
                                                        putExtra(Intent.EXTRA_TEXT, "${note.title}\n\n${note.body}")
                                                        type = "text/plain"
                                                    }
                                                    context.startActivity(Intent.createChooser(sendIntent, "مشاركة الملاحظة"))
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Share, contentDescription = "مشاركة", tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                                            }

                                            IconButton(
                                                onClick = {
                                                    val updated = notesList.filterNot { it.id == note.id }
                                                    updateAndSaveNotes(updated)
                                                    Toast.makeText(context, "تم حذف الملاحظة", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Floating Add Note Button
            Surface(
                onClick = {
                    editingNote = null
                    inputTitle = ""
                    inputBody = ""
                    inputCategory = "عام"
                    inputIsPinned = false
                    showEditorModal = true
                },
                color = Color(0xFF0F172A).copy(alpha = 0.90f),
                shape = RoundedCornerShape(30.dp),
                border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(Color(0xFF00FFCC), Color(0xFFA855F7)))),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
                    .shadow(16.dp, RoundedCornerShape(30.dp), spotColor = Color(0xFF00FFCC))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(0xFF00FFCC), Color(0xFFA855F7)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "إنشاء ملاحظة جديدة",
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "إنشاء ملاحظة جديدة +",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
            }

            // Editor Dialog
            if (showEditorModal) {
                AlertDialog(
                    onDismissRequest = { showEditorModal = false },
                    title = {
                        Text(
                            text = if (editingNote == null) "ملاحظة جديدة" else "تعديل الملاحظة",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = inputTitle,
                                onValueChange = { inputTitle = it },
                                label = { Text("عنوان الملاحظة", fontSize = 11.sp, color = Color(0xFF94A3B8)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = inputBody,
                                onValueChange = { inputBody = it },
                                label = { Text("محتوى الملاحظة...", fontSize = 11.sp, color = Color(0xFF94A3B8)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 120.dp, max = 220.dp),
                                shape = RoundedCornerShape(10.dp)
                            )

                            // Word & Char Count
                            val wordCount = inputBody.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }.size
                            val charCount = inputBody.length
                            Text(
                                text = "الكلمات: $wordCount | الحروف: $charCount",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B)
                            )

                            // Category Selector
                            Text("التصنيف:", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("شخصي", "عمل", "أفكار", "مهام", "عام").forEach { cat ->
                                    val sel = inputCategory == cat
                                    FilterChip(
                                        selected = sel,
                                        onClick = { inputCategory = cat },
                                        label = { Text(cat, fontSize = 10.sp) }
                                    )
                                }
                            }

                            // Pin Checkbox
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { inputIsPinned = !inputIsPinned }
                            ) {
                                Checkbox(
                                    checked = inputIsPinned,
                                    onCheckedChange = { inputIsPinned = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFFFFB703))
                                )
                                Text("تثبيت الملاحظة في الأعلى", fontSize = 12.sp, color = Color.White)
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (inputTitle.isBlank() && inputBody.isBlank()) {
                                    Toast.makeText(context, "يرجى إدخال عنوان أو نص الملاحظة", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                                val nowStr = sdf.format(Date())

                                if (editingNote == null) {
                                    val newNote = NoteItem(
                                        id = UUID.randomUUID().toString(),
                                        title = inputTitle.trim(),
                                        body = inputBody.trim(),
                                        category = inputCategory,
                                        isPinned = inputIsPinned,
                                        date = nowStr
                                    )
                                    updateAndSaveNotes(listOf(newNote) + notesList)
                                } else {
                                    val updated = notesList.map {
                                        if (it.id == editingNote!!.id) {
                                            it.copy(
                                                title = inputTitle.trim(),
                                                body = inputBody.trim(),
                                                category = inputCategory,
                                                isPinned = inputIsPinned,
                                                date = nowStr
                                            )
                                        } else it
                                    }
                                    updateAndSaveNotes(updated)
                                }
                                showEditorModal = false
                                Toast.makeText(context, "تم حفظ الملاحظة بنجاح", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC), contentColor = Color.Black)
                        ) {
                            Text("حفظ الملاحظة", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEditorModal = false }) {
                            Text("إلغاء", color = Color(0xFF94A3B8))
                        }
                    },
                    containerColor = Color(0xFF0F172A),
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }
    }
}

private fun loadNotes(context: Context): List<NoteItem> {
    val prefs = context.getSharedPreferences("notes_prefs", Context.MODE_PRIVATE)
    val jsonStr = prefs.getString(PREF_NOTES_DATA, null) ?: return emptyList()
    val list = mutableListOf<NoteItem>()
    try {
        val array = JSONArray(jsonStr)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                NoteItem(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    body = obj.getString("body"),
                    category = obj.optString("category", "عام"),
                    isPinned = obj.optBoolean("isPinned", false),
                    date = obj.optString("date", "")
                )
            )
        }
    } catch (_: Exception) {}
    return list
}

private fun saveNotes(context: Context, items: List<NoteItem>) {
    val prefs = context.getSharedPreferences("notes_prefs", Context.MODE_PRIVATE)
    val array = JSONArray()
    items.forEach { note ->
        val obj = JSONObject().apply {
            put("id", note.id)
            put("title", note.title)
            put("body", note.body)
            put("category", note.category)
            put("isPinned", note.isPinned)
            put("date", note.date)
        }
        array.put(obj)
    }
    prefs.edit().putString(PREF_NOTES_DATA, array.toString()).apply()
}
