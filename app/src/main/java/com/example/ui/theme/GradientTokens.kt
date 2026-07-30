package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object GradientTokens {
    // Purple AI Gradient
    val AI = Brush.linearGradient(
        colors = listOf(Color(0xFF8B5CF6), Color(0xFF6366F1))
    )

    // Green Premium / Success / Live Prices Gradient
    val LivePrices = Brush.linearGradient(
        colors = listOf(Color(0xFF10B981), Color(0xFF059669))
    )

    // Blue Economic / Professional Gradient
    val Economic = Brush.linearGradient(
        colors = listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))
    )

    // Main App Deep Teal Gradient
    val MainBackground = Brush.verticalGradient(
        colors = listOf(Color(0xFF0D2C28), Color(0xFF1A4D46))
    )

    // Gold / Warm Premium / Fallback Gradient
    val PremiumGold = Brush.linearGradient(
        colors = listOf(Color(0xFFF59E0B), Color(0xFFD97706))
    )

    // Metallic Charcoal Dark Gradient
    val DarkMetallic = Brush.linearGradient(
        colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
    )

    // Vibrant Red / Sunset Gradient
    val Sunset = Brush.linearGradient(
        colors = listOf(Color(0xFFEF4444), Color(0xFFB91C1C))
    )
}
