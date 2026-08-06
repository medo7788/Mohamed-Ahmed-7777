package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CustomThemeColors
import com.example.ui.components.ToolScreenScaffold
import com.example.ui.theme.AppIcons
import com.example.ui.theme.Spacing
import com.example.model.CalcKey
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.border



// DateCalcScreen is now implemented in DateCalcScreen.kt with full commercial grade date intelligence features

// AgeCalcScreen is now implemented in AgeCalcScreen.kt with full commercial grade features

// CountdownScreen is now implemented in CountdownScreen.kt with full commercial grade live duration engine features

@Composable
fun HealthCalcScreen(colors: CustomThemeColors) {
    UltimateHealthDashboard(colors = colors)
}

@Composable
fun OvulationCalcScreen(colors: CustomThemeColors) {
    UltimateHealthDashboard(colors = colors)
}
