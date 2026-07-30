package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
    darkColorScheme(
        primary = GoldSecondary,
        onPrimary = DarkBackground,
        secondary = TealAccent,
        onSecondary = Color.White,
        tertiary = GoldAccent,
        background = DarkBackground,
        surface = DarkSurface,
        onBackground = Color.White,
        onSurface = Color.White
    )

private val LightColorScheme =
    lightColorScheme(
        primary = TealPrimary,
        onPrimary = Color.White,
        secondary = GoldSecondary,
        onSecondary = Color.Black,
        tertiary = GoldAccent,
        background = LightBackground,
        surface = LightSurface,
        onBackground = TealPrimary,
        onSurface = TealPrimary
    )

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Disable dynamic color to maintain consistent branding
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
