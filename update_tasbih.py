import re

with open("app/src/main/java/com/example/ui/screens/IslamicScreens.kt", "r") as f:
    content = f.read()

start_idx = content.find("@OptIn(ExperimentalMaterial3Api::class)\nfun TasbihScreen(colors: CustomThemeColors) {")
if start_idx == -1:
    start_idx = content.find("fun TasbihScreen(colors: CustomThemeColors) {")

end_idx = content.find("@Composable\nfun QuranScreen(", start_idx)

new_tasbih = """@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasbihScreen(colors: CustomThemeColors) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val prefs = context.getSharedPreferences("tasbih_prefs", Context.MODE_PRIVATE)

    var count by remember { mutableStateOf(0) }
    var targetCount by remember { mutableStateOf(33) }
    var dhikrName by remember { mutableStateOf("سُبْحَانَ اللهِ") }
    
    var lifetimeCount by remember { mutableStateOf(prefs.getInt("lifetime_count", 0)) }
    
    var customDhikrs by remember { 
        mutableStateOf(
            try {
                val arr = org.json.JSONArray(prefs.getString("custom_dhikrs", "[]"))
                val list = mutableListOf<String>()
                for (i in 0 until arr.length()) list.add(arr.getString(i))
                list
            } catch (e: Exception) { emptyList() }
        ) 
    }
    
    val defaultDhikrs = listOf("سُبْحَانَ اللهِ", "الْحَمْدُ لِلَّهِ", "اللهُ أَكْبَرُ", "لَا إِلٰهَ إِلَّا اللهُ", "أَسْتَغْفِرُ اللهَ")
    
    var showDhikrSelector by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newDhikrText by remember { mutableStateOf("") }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = tween(durationMillis = 150),
        label = "PressAnimation"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.appBg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top section: Counters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = colors.surface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier.clickable { targetCount = if (targetCount == 33) 99 else if (targetCount == 99) 1000 else 33 }
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("الهدف", color = colors.textMuted, fontSize = 12.sp)
                    Text("$targetCount", color = colors.accent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Surface(
                color = colors.surface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, colors.border)
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("المجموع", color = colors.textMuted, fontSize = 12.sp)
                    Text("$lifetimeCount", color = colors.accentSecondary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Current Dhikr Name & Selector
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "الذِّكْرُ الحَالِي",
                style = MaterialTheme.typography.labelMedium,
                color = colors.textMuted
            )
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                color = colors.surface,
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 8.dp,
                modifier = Modifier.clickable { showDhikrSelector = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dhikrName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.accent,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "تغيير الذكر",
                        tint = colors.textMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Central Bead Clicker - Modern 3D Design
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(300.dp)
                .scale(scale)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    count++
                    lifetimeCount++
                    prefs.edit().putInt("lifetime_count", lifetimeCount).apply()
                    if (count > targetCount) count = 1
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 14.dp.toPx()
                val diameter = size.minDimension - strokeWidth
                val radius = diameter / 2
                
                // Outer beautiful glowing ring
                drawCircle(
                    color = colors.accent.copy(alpha = 0.15f),
                    radius = radius + 8.dp.toPx()
                )

                // Thread
                drawCircle(
                    color = colors.border,
                    radius = radius,
                    style = Stroke(width = 3.dp.toPx())
                )

                // Beads
                val beadCount = if (targetCount > 100) 33 else targetCount
                for (i in 0 until beadCount) {
                    val angleStr = (i.toFloat() / beadCount.toFloat()) * 360f - 90f
                    val angleRad = Math.toRadians(angleStr.toDouble())
                    val cx = center.x + radius * Math.cos(angleRad).toFloat()
                    val cy = center.y + radius * Math.sin(angleRad).toFloat()
                    
                    val isCounted = if (count == 0) false else if (count % beadCount == 0) true else i < (count % beadCount)
                    val beadColor = if (isCounted) colors.accent else colors.surface2
                    val beadRadius = if (isCounted) 12.dp.toPx() else 8.dp.toPx()
                    
                    // Main bead
                    drawCircle(
                        color = beadColor,
                        radius = beadRadius,
                        center = Offset(cx, cy)
                    )
                    
                    // Highlight for 3D effect
                    drawCircle(
                        color = Color.White.copy(alpha = if (isCounted) 0.6f else 0.2f),
                        radius = beadRadius * 0.35f,
                        center = Offset(cx - beadRadius * 0.3f, cy - beadRadius * 0.3f)
                    )
                    
                    // Shadow for 3D effect
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.2f),
                        radius = beadRadius * 0.8f,
                        center = Offset(cx + beadRadius * 0.1f, cy + beadRadius * 0.1f),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }

                // Inner Elegant Center
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(colors.surface.copy(alpha=0.9f), colors.surface.copy(alpha=0.4f), Color.Transparent)
                    ),
                    radius = radius - strokeWidth * 2
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$count",
                    fontSize = 84.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.text
                )
                if (targetCount <= 100) {
                    Text(
                        text = "من $targetCount",
                        fontSize = 16.sp,
                        color = colors.textMuted
                    )
                }
            }
        }

        // Reset Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = colors.surface,
                shadowElevation = 4.dp,
                modifier = Modifier.size(64.dp)
            ) {
                IconButton(
                    onClick = {
                        count = 0
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "إعادة ضبط",
                        tint = colors.accent,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }

    if (showDhikrSelector) {
        AlertDialog(
            onDismissRequest = { showDhikrSelector = false },
            containerColor = colors.appBg,
            title = { Text("اختر الذكر", color = colors.text, fontWeight = FontWeight.Bold, fontSize = 22.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp)) {
                    items(defaultDhikrs) { dhikr ->
                        Surface(
                            color = colors.surface,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable {
                                    dhikrName = dhikr
                                    count = 0
                                    showDhikrSelector = false
                                }
                        ) {
                            Text(
                                text = dhikr,
                                modifier = Modifier.padding(16.dp),
                                color = colors.text,
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    if (customDhikrs.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("أذكاري المخصصة", color = colors.textMuted, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                        }
                        
                        items(customDhikrs) { dhikr ->
                            Surface(
                                color = colors.surface,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        dhikrName = dhikr
                                        count = 0
                                        showDhikrSelector = false
                                    }.padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = dhikr,
                                        color = colors.accent,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Start
                                    )
                                    IconButton(onClick = {
                                        val updated = customDhikrs.filter { it != dhikr }
                                        customDhikrs = updated
                                        val arr = org.json.JSONArray()
                                        for (item in updated) arr.put(item)
                                        prefs.edit().putString("custom_dhikrs", arr.toString()).apply()
                                        if (dhikrName == dhikr) dhikrName = defaultDhikrs[0]
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red.copy(alpha = 0.7f))
                                    }
                                }
                            }
                        }
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showAddDialog = true; showDhikrSelector = false },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("إضافة ذكر مخصص", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDhikrSelector = false }) {
                    Text("إغلاق", color = colors.textMuted)
                }
            }
        )
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = colors.appBg,
            title = { Text("ذكر جديد", color = colors.text, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
            text = {
                OutlinedTextField(
                    value = newDhikrText,
                    onValueChange = { newDhikrText = it },
                    label = { Text("أدخل الذكر هنا") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        focusedLabelColor = colors.accent,
                        unfocusedBorderColor = colors.border,
                        unfocusedLabelColor = colors.textMuted,
                        focusedTextColor = colors.text,
                        unfocusedTextColor = colors.text
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newDhikrText.isNotBlank()) {
                            if (!customDhikrs.contains(newDhikrText)) {
                                val updated = customDhikrs + newDhikrText
                                customDhikrs = updated
                                val arr = org.json.JSONArray()
                                for (item in updated) arr.put(item)
                                prefs.edit().putString("custom_dhikrs", arr.toString()).apply()
                            }
                            dhikrName = newDhikrText
                            count = 0
                            newDhikrText = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("إضافة", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("إلغاء", color = colors.textMuted)
                }
            }
        )
    }
}
\n"""

content = content[:start_idx] + new_tasbih + content[end_idx:]
with open("app/src/main/java/com/example/ui/screens/IslamicScreens.kt", "w") as f:
    f.write(content)
