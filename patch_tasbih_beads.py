import re

with open("app/src/main/java/com/example/ui/screens/IslamicScreens.kt", "r") as f:
    content = f.read()

old_canvas = """            Canvas(modifier = Modifier.fillMaxSize()) {
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
            }"""

new_canvas = """            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 12.dp.toPx()
                val diameter = size.minDimension - strokeWidth
                val radius = diameter / 2
                
                // Draw a beautiful thread
                drawCircle(
                    color = Color(0xFFD4AF37).copy(alpha = 0.3f),
                    radius = radius,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Draw 33 beads
                for (i in 0 until maxCount) {
                    val angleStr = (i.toFloat() / maxCount.toFloat()) * 360f - 90f
                    val angleRad = Math.toRadians(angleStr.toDouble())
                    val cx = center.x + radius * Math.cos(angleRad).toFloat()
                    val cy = center.y + radius * Math.sin(angleRad).toFloat()
                    
                    val isCounted = i < count
                    val beadColor = if (isCounted) Color(0xFFD4AF37) else Color(0xFF1E293B)
                    val beadRadius = if (isCounted) 10.dp.toPx() else 8.dp.toPx()
                    
                    drawCircle(
                        color = beadColor,
                        radius = beadRadius,
                        center = Offset(cx, cy)
                    )
                    
                    // Add a small highlight to make it look 3D
                    drawCircle(
                        color = Color.White.copy(alpha = if (isCounted) 0.5f else 0.1f),
                        radius = beadRadius * 0.3f,
                        center = Offset(cx - beadRadius * 0.3f, cy - beadRadius * 0.3f)
                    )
                }

                // Inner elegant circle
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF1E293B).copy(alpha=0.5f), Color.Transparent)
                    ),
                    radius = radius - strokeWidth * 2
                )
            }

            // عرض الرقم فقط ليكون المنظر جميلاً
            Text(
                text = "$count",
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD4AF37)
            )"""

if old_canvas in content:
    content = content.replace(old_canvas, new_canvas)
    with open("app/src/main/java/com/example/ui/screens/IslamicScreens.kt", "w") as f:
        f.write(content)
    print("Replaced Tasbih canvas with beads")
else:
    print("Could not find old_canvas in IslamicScreens")
