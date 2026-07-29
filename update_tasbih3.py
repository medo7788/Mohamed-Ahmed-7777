import sys

new_code = """fun TasbihScreen(colors: CustomThemeColors) {
    var count by remember { mutableStateOf(0) }
    val maxCount = 33
    val haptic = LocalHapticFeedback.current

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // أنيميشن الانكماش عند الضغط
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = tween(durationMillis = 100),
        label = "PressAnimation"
    )

    val goldGradients = listOf(
        Color(0xFFF3E5AB),
        Color(0xFFD4AF37),
        Color(0xFFAA771C)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // العنوان العلوي
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "الذِّكْرُ الحَالِي",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD4AF37)
            )
        }

        // المنطقة المركزية: زر التسبيح الرقمي الدائري
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(280.dp)
                .scale(scale)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    count = (count + 1) % (maxCount + 1)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 12.dp.toPx()
                val diameter = size.minDimension - strokeWidth
                val radius = diameter / 2
                val sweepAngle = (count.toFloat() / maxCount.toFloat()) * 360f

                // الحلقة الخلفية
                drawCircle(
                    color = Color(0xFF1E293B),
                    radius = radius,
                    style = Stroke(width = strokeWidth)
                )

                // حلقة التقدم المتممة للتسبيح
                drawArc(
                    brush = Brush.sweepGradient(goldGradients),
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // قرص الزر الداخلي
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                    ),
                    radius = radius - strokeWidth
                )
            }

            // عرض الرقم في المنتصف (بدون أي نصوص توجيهية)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$count",
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD4AF37)
                )
                Text(
                    text = "من $maxCount",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }

        // أزرار التحكم الفرعية
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
}
"""

with open("app/src/main/java/com/example/ui/screens/IslamicScreens.kt", "r") as f:
    lines = f.readlines()

start_idx = -1
for i, line in enumerate(lines):
    if line.startswith("fun TasbihScreen(colors: CustomThemeColors) {"):
        start_idx = i
        break

end_idx = -1
if start_idx != -1:
    for i in range(start_idx + 1, len(lines)):
        if "fun QuranScreen(" in line:
            end_idx = i - 2
            break

if start_idx != -1 and end_idx != -1:
    with open("app/src/main/java/com/example/ui/screens/IslamicScreens.kt", "w") as f:
        f.writelines(lines[:start_idx])
        f.write(new_code + "\n")
        f.writelines(lines[end_idx+1:])
    print("Replaced successfully!")
else:
    print(f"Failed. start: {start_idx}, end: {end_idx}")

