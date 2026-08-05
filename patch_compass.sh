sed -i '/Center Astrolabe Pivot Dot/,$d' app/src/main/java/com/example/ui/screens/QiblaDirectionScreenRedesign.kt
echo '            // Central Compass Rose & Needle
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(dialRotation + qiblaAngle),
                contentAlignment = Alignment.Center
            ) {
                // Central Needle (Drawn with Canvas)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    
                    // Golden Needle pointing to Qibla (Up in this local coordinate space)
                    val pathGolden = androidx.compose.ui.graphics.Path().apply {
                        moveTo(center.x - 8.dp.toPx(), center.y)
                        lineTo(center.x, center.y - 110.dp.toPx())
                        lineTo(center.x + 8.dp.toPx(), center.y)
                        close()
                    }
                    drawPath(
                        path = pathGolden,
                        brush = Brush.linearGradient(
                            colors = listOf(AmberGold, ChampagneGold),
                            start = Offset(center.x - 8.dp.toPx(), center.y),
                            end = Offset(center.x + 8.dp.toPx(), center.y)
                        )
                    )
                    
                    // Silver tail of the needle
                    val pathSilver = androidx.compose.ui.graphics.Path().apply {
                        moveTo(center.x - 6.dp.toPx(), center.y)
                        lineTo(center.x, center.y + 70.dp.toPx())
                        lineTo(center.x + 6.dp.toPx(), center.y)
                        close()
                    }
                    drawPath(
                        path = pathSilver,
                        brush = Brush.linearGradient(
                            colors = listOf(Color.LightGray, Color.DarkGray),
                            start = Offset(center.x - 6.dp.toPx(), center.y),
                            end = Offset(center.x + 6.dp.toPx(), center.y)
                        )
                    )
                    
                    // Center Pivot
                    drawCircle(
                        color = Color(0xFF1E2638),
                        radius = 12.dp.toPx(),
                        center = center
                    )
                    drawCircle(
                        color = ChampagneGold,
                        radius = 12.dp.toPx(),
                        center = center,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                    )
                    drawCircle(
                        color = AmberGold,
                        radius = 4.dp.toPx(),
                        center = center
                    )
                }
            }

            // Fixed Phone Heading Pointer (Top Arrow)
            Icon(
                imageVector = Icons.Default.ArrowDropUp,
                contentDescription = "اتجاه أعلى الجوال",
                tint = if (isAligned) AmberGold else IceCyan,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-4).dp)
                    .size(36.dp)
            )
        }
    }
}
