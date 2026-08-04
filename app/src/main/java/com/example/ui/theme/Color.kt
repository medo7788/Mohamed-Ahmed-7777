package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Custom User Requested Colors
val ObsidianBlack = Color(0xFF0B0E13)
val CardColor = Color(0xFF131922)
val MintCyan = Color(0xFF63F4DD)
val RoyalGold = Color(0xFFD8B56A)
val DangerRed = Color(0xFFD85B66)

// Premium FinTech & Soft Frosted Crystal Dark Theme Colors
val PremiumBackgroundDark = Color(0xFF121417)
val PremiumSurfaceDark = Color(0xFF1A1E22)
val PremiumCardDark = Color(0xFF20262B)
val PremiumGlassOverlayDark = Color(0xC01A1E22) // 75% Opacity representation
val EmeraldGreen = Color(0xFF22B573)
val SoftCrimson = Color(0xFFE45B5B)
val IceCyan = Color(0xFF38BDF8)
val SoftRose = Color(0xFFF472B6)
val Lavender = Color(0xFFC084FC)

// Text and Dividers
val TextPrimaryDark = Color(0xFFFFFFFF)
val TextSecondaryDark = Color(0xFFB9C0C9)
val DividerDark = Color(0x14FFFFFF) // rgba(255,255,255,0.08)

// Compatibility aliases for existing classes
val SurfaceTeal = PremiumSurfaceDark
val SurfaceBright = PremiumCardDark
val DeepBackground = ObsidianBlack // Updated to new background
val GoldAccent = RoyalGold
val Primary = EmeraldGreen
val OnSurface = TextPrimaryDark
val CriticalRed = SoftCrimson
val SuccessGreen = EmeraldGreen
val Background = ObsidianBlack // Updated to new background
val Surface = PremiumSurfaceDark
val GlassBorder = RoyalGold.copy(alpha = 0.3f)
val OnSurfaceVariant = TextSecondaryDark

val DeepTeal = SurfaceTeal
val MediumTeal = Primary
val DarkEmerald = SurfaceBright
val PremiumGold = GoldAccent
val OffWhite = OnSurface
val DarkBackground = Background
val DarkSurface = Surface
val LightBackground = OnSurface
val LightSurface = Surface
val TealPrimary = Primary
val GoldSecondary = GoldAccent
val TealAccent = Primary
