package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.model.CalcKey
import com.example.ui.theme.CustomThemeColors

@Composable
fun HealthHubScreen(
    colors: CustomThemeColors,
    favoriteTools: Set<String>,
    onToggleFavorite: (CalcKey) -> Unit,
    onToolClick: (CalcKey) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    UltimateHealthDashboard(
        colors = colors,
        onBackClick = onBackClick,
        modifier = modifier
    )
}
