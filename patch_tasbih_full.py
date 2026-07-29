import re

with open("app/src/main/java/com/example/ui/screens/IslamicScreens.kt", "r") as f:
    content = f.read()

start_idx = content.find("fun TasbihScreen(colors: CustomThemeColors) {")
if start_idx != -1:
    end_idx = content.find("@Composable\nfun QuranScreen(", start_idx)
    
    if end_idx != -1:
        new_tasbih = """@OptIn(ExperimentalMaterial3Api::class)
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
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = tween(durationMillis = 100),
        label = "PressAnimation"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top section: Target Count & Lifetime Count
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = colors.surface,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.clickable { targetCount = if (targetCount == 33) 99 else if (targetCount == 99) 1000 else 33 }
            ) {
                Text(
                    text = "الهدف: $targetCount",
                    color = colors.text,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    fontSize = 14.sp
                )
            }
            
            Surface(
                color = colors.surface,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "المجموع: $lifetimeCount",
                    color = colors.text,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    fontSize = 14.sp
                )
            }
        }

        // Current Dhikr Name & Selector
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "الذِّكْرُ الحَالِي",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = colors.surface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.clickable { showDhikrSelector = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dhikrName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD4AF37),
                        textAlign = TextAlign.Center
                    )
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "تغيير الذكر",
                        tint = Color.Gray,
                        modifier = Modifier.padding(start = 8.dp).size(20.dp)
                    )
                }
            }
        }

        // Central Bead Clicker
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(280.dp)
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
                val strokeWidth = 12.dp.toPx()
                val diameter = size.minDimension - strokeWidth
                val radius = diameter / 2
                
                // Thread
                drawCircle(
                    color = Color(0xFFD4AF37).copy(alpha = 0.3f),
                    radius = radius,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Beads
                val beadCount = if (targetCount > 100) 33 else targetCount
                for (i in 0 until beadCount) {
                    val angleStr = (i.toFloat() / beadCount.toFloat()) * 360f - 90f
                    val angleRad = Math.toRadians(angleStr.toDouble())
                    val cx = center.x + radius * Math.cos(angleRad).toFloat()
                    val cy = center.y + radius * Math.sin(angleRad).toFloat()
                    
                    val isCounted = if (count == 0) false else if (count % beadCount == 0) true else i < (count % beadCount)
                    val beadColor = if (isCounted) Color(0xFFD4AF37) else Color(0xFF1E293B)
                    val beadRadius = if (isCounted) 10.dp.toPx() else 8.dp.toPx()
                    
                    drawCircle(
                        color = beadColor,
                        radius = beadRadius,
                        center = Offset(cx, cy)
                    )
                    
                    drawCircle(
                        color = Color.White.copy(alpha = if (isCounted) 0.5f else 0.1f),
                        radius = beadRadius * 0.3f,
                        center = Offset(cx - beadRadius * 0.3f, cy - beadRadius * 0.3f)
                    )
                }

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF1E293B).copy(alpha=0.5f), Color.Transparent)
                    ),
                    radius = radius - strokeWidth * 2
                )
            }

            Text(
                text = "$count",
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD4AF37)
            )
        }

        // Reset Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedIconButton(
                onClick = {
                    count = 0
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "إعادة ضبط",
                    tint = Color(0xFFD4AF37)
                )
            }
        }
    }

    if (showDhikrSelector) {
        AlertDialog(
            onDismissRequest = { showDhikrSelector = false },
            containerColor = colors.surface,
            title = { Text("اختر الذكر", color = colors.text, fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                    val allDhikrs = defaultDhikrs + customDhikrs
                    items(allDhikrs) { dhikr ->
                        Text(
                            text = dhikr,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    dhikrName = dhikr
                                    count = 0
                                    showDhikrSelector = false
                                }
                                .padding(vertical = 12.dp),
                            color = colors.text,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showAddDialog = true; showDhikrSelector = false },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("إضافة ذكر مخصص", color = Color.White)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDhikrSelector = false }) {
                    Text("إغلاق", color = colors.accent)
                }
            }
        )
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = colors.surface,
            title = { Text("ذكر جديد", color = colors.text, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newDhikrText,
                    onValueChange = { newDhikrText = it },
                    label = { Text("أدخل الذكر هنا") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        focusedLabelColor = colors.accent,
                        unfocusedBorderColor = Color.Gray
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newDhikrText.isNotBlank()) {
                        val updated = customDhikrs + newDhikrText
                        customDhikrs = updated
                        val arr = org.json.JSONArray()
                        for (item in updated) arr.put(item)
                        prefs.edit().putString("custom_dhikrs", arr.toString()).apply()
                        dhikrName = newDhikrText
                        count = 0
                        newDhikrText = ""
                        showAddDialog = false
                    }
                }) {
                    Text("إضافة", color = colors.accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("إلغاء", color = Color.Gray)
                }
            }
        )
    }
}
\n"""
        
        with open("app/src/main/java/com/example/ui/screens/IslamicScreens.kt", "w") as f:
            f.write(content[:start_idx] + new_tasbih + content[end_idx:])
        print("Tasbih replaced successfully!")
