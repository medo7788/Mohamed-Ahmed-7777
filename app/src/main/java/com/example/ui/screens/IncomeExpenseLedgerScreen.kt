package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CalcKey
import com.example.ui.components.ToolScreenScaffold
import com.example.ui.theme.AppIcons
import com.example.ui.theme.CustomThemeColors
import org.json.JSONArray
import org.json.JSONObject
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

private data class LedgerItem(
    val id: String,
    val title: String,
    val amount: Double,
    val isExpense: Boolean, // true for expense, false for income
    val category: String,
    val date: String,
    val note: String
)

private val PREF_LEDGER_DATA = "clevcalc_ledger_items_v1"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeExpenseLedgerScreen(colors: CustomThemeColors) {
    val context = LocalContext.current
    var itemsList by remember { mutableStateOf(loadLedgerItems(context)) }
    var filterType by remember { mutableStateOf("ALL") } // "ALL", "INCOME", "EXPENSE"
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    // Dialog form state
    var inputTitle by remember { mutableStateOf("") }
    var inputAmount by remember { mutableStateOf("") }
    var inputIsExpense by remember { mutableStateOf(true) }
    var inputCategory by remember { mutableStateOf("عام") }
    var inputNote by remember { mutableStateOf("") }

    val categoriesIncome = listOf("راتب", "تجارة", "استثمار", "هدية", "عمل حر", "أخرى")
    val categoriesExpense = listOf("طعام وتسوق", "فواتير وسكن", "مواصلات", "صحة وعلاج", "تعليم", "ترفيه", "أخرى")

    // Save list helper
    fun updateAndSaveItems(newList: List<LedgerItem>) {
        itemsList = newList
        saveLedgerItems(context, newList)
    }

    // Calculations
    val totalIncome = itemsList.filter { !it.isExpense }.sumOf { it.amount }
    val totalExpenses = itemsList.filter { it.isExpense }.sumOf { it.amount }
    val netBalance = totalIncome - totalExpenses

    val filteredItems = itemsList.filter { item ->
        val matchesFilter = when (filterType) {
            "INCOME" -> !item.isExpense
            "EXPENSE" -> item.isExpense
            else -> true
        }
        val matchesSearch = searchQuery.isBlank() ||
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.category.contains(searchQuery, ignoreCase = true) ||
                item.note.contains(searchQuery, ignoreCase = true)
        matchesFilter && matchesSearch
    }

    val fmt = DecimalFormat("#,##0.00")

    ToolScreenScaffold(
        colors = colors,
        icon = AppIcons.forCalc(CalcKey.LEDGER),
        title = "دفتر المصروفات والإيرادات",
        subtitle = "إدارة ميزانيتك اليومية وحساب الدخل والمصاريف بسهولة",
        isScrollable = false
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 100.dp)
            ) {
                // 1. Balance Summary Card
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = Color(0xFF0F172A).copy(alpha = 0.90f),
                        border = BorderStroke(1.dp, Color(0xFF00FFCC).copy(alpha = 0.35f)),
                        shadowElevation = 10.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "صافي الميزانية المتبقية",
                                fontSize = 13.sp,
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${fmt.format(netBalance)} ج.م",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = if (netBalance >= 0) Color(0xFF00FFCC) else Color(0xFFFF5252)
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Income Pill
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDownward,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text("إجمالي الدخل", fontSize = 10.sp, color = Color(0xFFA7F3D0))
                                            Text("${fmt.format(totalIncome)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                // Expense Pill
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFFEF4444).copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowUpward,
                                            contentDescription = null,
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text("إجمالي المصاريف", fontSize = 10.sp, color = Color(0xFFFCA5A5))
                                            Text("${fmt.format(totalExpenses)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Filter & Search Controls
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("بحث في المعاملات...", fontSize = 12.sp, color = Color(0xFF64748B)) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF00FFCC)) },
                            modifier = Modifier
                                .weight(1f)
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

                        if (itemsList.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    updateAndSaveItems(emptyList())
                                    Toast.makeText(context, "تم مسح السجل بنجاح", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                                    .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                            ) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = "مسح الكل", tint = Color(0xFFEF4444))
                            }
                        }
                    }
                }

                // 3. Category Filter Chips
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "ALL" to "الكل (${itemsList.size})",
                            "INCOME" to "الدخل فقط",
                            "EXPENSE" to "المصاريف فقط"
                        ).forEach { (type, label) ->
                            val selected = filterType == type
                            FilterChip(
                                selected = selected,
                                onClick = { filterType = type },
                                label = { Text(label, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF00FFCC).copy(alpha = 0.25f),
                                    selectedLabelColor = Color(0xFF00FFCC),
                                    containerColor = Color(0xFF1E293B),
                                    labelColor = Color(0xFF94A3B8)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = if (selected) Color(0xFF00FFCC) else Color(0xFF334155),
                                    selectedBorderColor = Color(0xFF00FFCC),
                                    enabled = true,
                                    selected = selected
                                )
                            )
                        }
                    }
                }

                // 4. Transactions List Header
                item {
                    Text(
                        text = "سجل المعاملات اليومية (${filteredItems.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // 5. Items List
                if (filteredItems.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (itemsList.isEmpty()) "لا توجد معاملات مسجلة بعد" else "لا توجد نتائج مطابقة للبحث",
                                    fontSize = 13.sp,
                                    color = Color(0xFF94A3B8)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "اضغط على زر '+' بالأسفل لإضافة معاملة جديدة",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                } else {
                    items(filteredItems, key = { it.id }) { item ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF1E293B).copy(alpha = 0.85f),
                            border = BorderStroke(
                                1.dp,
                                if (item.isExpense) Color(0xFFEF4444).copy(alpha = 0.25f) else Color(0xFF10B981).copy(alpha = 0.25f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (item.isExpense) Color(0xFFEF4444).copy(alpha = 0.20f) else Color(0xFF10B981).copy(alpha = 0.20f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (item.isExpense) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                            contentDescription = null,
                                            tint = if (item.isExpense) Color(0xFFEF4444) else Color(0xFF10B981),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = item.title,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFF334155)
                                            ) {
                                                Text(
                                                    text = item.category,
                                                    fontSize = 10.sp,
                                                    color = Color(0xFFCBD5E1),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = item.date,
                                                fontSize = 10.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                        if (item.note.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = item.note,
                                                fontSize = 11.sp,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${if (item.isExpense) "-" else "+"}${fmt.format(item.amount)}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (item.isExpense) Color(0xFFEF4444) else Color(0xFF10B981)
                                    )

                                    IconButton(
                                        onClick = {
                                            val updated = itemsList.filterNot { it.id == item.id }
                                            updateAndSaveItems(updated)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "حذف",
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Floating Add Button
            Surface(
                onClick = {
                    inputTitle = ""
                    inputAmount = ""
                    inputIsExpense = true
                    inputCategory = "طعام وتسوق"
                    inputNote = ""
                    showAddDialog = true
                },
                color = Color(0xFF0F172A).copy(alpha = 0.90f),
                shape = RoundedCornerShape(30.dp),
                border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(Color(0xFF00FFCC), Color(0xFFFFB703)))),
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
                            .background(Brush.linearGradient(listOf(Color(0xFF00FFCC), Color(0xFFFFB703)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "إضافة معاملة جديدة",
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "إضافة معاملة جديدة +",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
            }

            // Add Transaction Dialog
            if (showAddDialog) {
                AlertDialog(
                    onDismissRequest = { showAddDialog = false },
                    title = {
                        Text(
                            text = "تسجيل معاملة جديدة",
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
                            // Type Selector (Income vs Expense)
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Surface(
                                    onClick = {
                                        inputIsExpense = true
                                        inputCategory = categoriesExpense.first()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (inputIsExpense) Color(0xFFEF4444).copy(alpha = 0.25f) else Color(0xFF1E293B),
                                    border = BorderStroke(1.dp, if (inputIsExpense) Color(0xFFEF4444) else Color(0xFF334155))
                                ) {
                                    Text(
                                        text = "مصروف (-)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (inputIsExpense) Color(0xFFEF4444) else Color(0xFF94A3B8),
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    onClick = {
                                        inputIsExpense = false
                                        inputCategory = categoriesIncome.first()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (!inputIsExpense) Color(0xFF10B981).copy(alpha = 0.25f) else Color(0xFF1E293B),
                                    border = BorderStroke(1.dp, if (!inputIsExpense) Color(0xFF10B981) else Color(0xFF334155))
                                ) {
                                    Text(
                                        text = "إيراد / دخل (+)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!inputIsExpense) Color(0xFF10B981) else Color(0xFF94A3B8),
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }

                            // Title Field
                            OutlinedTextField(
                                value = inputTitle,
                                onValueChange = { inputTitle = it },
                                label = { Text("عنوان المعاملة (مثال: راتب الشهر / بقالة)", fontSize = 11.sp, color = Color(0xFF94A3B8)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )

                            // Amount Field
                            OutlinedTextField(
                                value = inputAmount,
                                onValueChange = { inputAmount = it },
                                label = { Text("المبلغ (ج.م)", fontSize = 11.sp, color = Color(0xFF94A3B8)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )

                            // Category Chips
                            Text("التصنيف:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val currentCatList = if (inputIsExpense) categoriesExpense else categoriesIncome
                                currentCatList.take(3).forEach { cat ->
                                    val sel = inputCategory == cat
                                    FilterChip(
                                        selected = sel,
                                        onClick = { inputCategory = cat },
                                        label = { Text(cat, fontSize = 10.sp) }
                                    )
                                }
                            }

                            // Note Field
                            OutlinedTextField(
                                value = inputNote,
                                onValueChange = { inputNote = it },
                                label = { Text("ملاحظات إضافية (اختياري)", fontSize = 11.sp, color = Color(0xFF94A3B8)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                maxLines = 2
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val amt = inputAmount.toDoubleOrNull()
                                if (inputTitle.isBlank() || amt == null || amt <= 0) {
                                    Toast.makeText(context, "يرجى إدخال عنوان ومبلغ صحيح", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                                val newItem = LedgerItem(
                                    id = UUID.randomUUID().toString(),
                                    title = inputTitle.trim(),
                                    amount = amt,
                                    isExpense = inputIsExpense,
                                    category = inputCategory,
                                    date = sdf.format(Date()),
                                    note = inputNote.trim()
                                )
                                updateAndSaveItems(listOf(newItem) + itemsList)
                                showAddDialog = false
                                Toast.makeText(context, "تمت إضافة المعاملة بنجاح", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC), contentColor = Color.Black)
                        ) {
                            Text("حفظ المعاملة", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddDialog = false }) {
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

private fun loadLedgerItems(context: Context): List<LedgerItem> {
    val prefs = context.getSharedPreferences("ledger_prefs", Context.MODE_PRIVATE)
    val jsonStr = prefs.getString(PREF_LEDGER_DATA, null) ?: return emptyList()
    val list = mutableListOf<LedgerItem>()
    try {
        val array = JSONArray(jsonStr)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                LedgerItem(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    amount = obj.getDouble("amount"),
                    isExpense = obj.getBoolean("isExpense"),
                    category = obj.optString("category", "عام"),
                    date = obj.optString("date", ""),
                    note = obj.optString("note", "")
                )
            )
        }
    } catch (_: Exception) {}
    return list
}

private fun saveLedgerItems(context: Context, items: List<LedgerItem>) {
    val prefs = context.getSharedPreferences("ledger_prefs", Context.MODE_PRIVATE)
    val array = JSONArray()
    items.forEach { item ->
        val obj = JSONObject().apply {
            put("id", item.id)
            put("title", item.title)
            put("amount", item.amount)
            put("isExpense", item.isExpense)
            put("category", item.category)
            put("date", item.date)
            put("note", item.note)
        }
        array.put(obj)
    }
    prefs.edit().putString(PREF_LEDGER_DATA, array.toString()).apply()
}
