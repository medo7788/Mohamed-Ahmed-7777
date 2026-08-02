package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.model.CalcKey
import com.example.ui.components.*
import com.example.ui.screens.*
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.AppThemeKey
import com.example.ui.theme.ClevCalcTheme
import com.example.ui.theme.getThemeColors
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.MoreHoriz

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            LaunchedEffect(Unit) {
                viewModel.initTheme(context)
            }

            val currentThemeKey by viewModel.currentThemeKey.collectAsState()
            val currentCalcKey by viewModel.currentCalcKey.collectAsState()
            val searchQuery by viewModel.searchQuery.collectAsState()
            val showThemesModal by viewModel.showThemesModal.collectAsState()
            val showAboutModal by viewModel.showAboutModal.collectAsState()

            val navController = rememberNavController()

            // Keep viewModel currentCalcKey in sync with NavController backstack changes
            LaunchedEffect(navController) {
                navController.currentBackStackEntryFlow.collect { backStackEntry ->
                    val route = backStackEntry.destination.route
                    if (route != null) {
                        try {
                            val key = CalcKey.valueOf(route)
                            viewModel.setCalcKey(key)
                        } catch (_: IllegalArgumentException) {}
                    }
                }
            }

            // Back handler to navigate back to Home screen if on sub-screen or pop gracefully
            BackHandler(enabled = currentCalcKey != CalcKey.HOME) {
                val isTab = currentCalcKey == CalcKey.WEATHER || currentCalcKey == CalcKey.AI || currentCalcKey == CalcKey.ADHAN_SETTINGS
                if (isTab) {
                    navController.navigate(CalcKey.HOME.name) {
                        popUpTo(CalcKey.HOME.name) { inclusive = true }
                        launchSingleTop = true
                    }
                } else {
                    if (!navController.popBackStack()) {
                        navController.navigate(CalcKey.HOME.name) {
                            popUpTo(CalcKey.HOME.name) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
            }

            val colors = getThemeColors(currentThemeKey)

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                ClevCalcTheme(themeKey = currentThemeKey) {
                    Scaffold(
                        topBar = {
                            AppHeader(
                                currentCalc = currentCalcKey,
                                colors = colors,
                                onGoHome = {
                                    if (currentCalcKey != CalcKey.HOME) {
                                        navController.navigate(CalcKey.HOME.name) {
                                            popUpTo(CalcKey.HOME.name) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
                                },
                                onToggleTheme = { viewModel.toggleTheme(context) },
                                onOpenThemes = { viewModel.setShowThemesModal(true) },
                                onOpenAbout = { viewModel.setShowAboutModal(true) }
                            )
                        },
                        bottomBar = {
                            if (currentCalcKey == CalcKey.HOME || currentCalcKey == CalcKey.WEATHER || 
                                currentCalcKey == CalcKey.AI || currentCalcKey == CalcKey.ADHAN_SETTINGS) {

                                // Beautiful modern floating navigation bar with Royal Gold indicator
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .navigationBarsPadding()
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(72.dp),
                                        shape = RoundedCornerShape(24.dp),
                                        color = colors.surface.copy(alpha = 0.75f), // Soft Frosted Crystal
                                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border.copy(alpha = 0.5f)),
                                        shadowElevation = 8.dp
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceAround,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val items = listOf(
                                                Triple("الرئيسية", CalcKey.HOME, Icons.Default.Home),
                                                Triple("الطقس", CalcKey.WEATHER, Icons.Default.Cloud),
                                                Triple("المستشار", CalcKey.AI, Icons.Default.AutoAwesome),
                                                Triple("الإعدادات", CalcKey.ADHAN_SETTINGS, Icons.Default.Settings)
                                            )

                                            items.forEach { (label, key, icon) ->
                                                val isSelected = currentCalcKey == key

                                                Column(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight()
                                                        .clip(RoundedCornerShape(16.dp))
                                                        .clickable {
                                                            if (currentCalcKey != key) {
                                                                navController.navigate(key.name) {
                                                                    popUpTo(CalcKey.HOME.name) { saveState = true }
                                                                    launchSingleTop = true
                                                                    restoreState = true
                                                                }
                                                            }
                                                        }
                                                        .padding(vertical = 6.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    // Capsule background for the selected item (Royal Gold Active Indicator)
                                                    if (isSelected) {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(16.dp))
                                                                .background(colors.accent.copy(alpha = 0.2f))
                                                                .padding(horizontal = 16.dp, vertical = 6.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                icon,
                                                                contentDescription = label,
                                                                tint = colors.accent,
                                                                modifier = Modifier.size(24.dp)
                                                            )
                                                        }
                                                    } else {
                                                        Icon(
                                                            icon,
                                                            contentDescription = label,
                                                            tint = colors.textMuted,
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(3.dp))
                                                    Text(
                                                        label,
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isSelected) colors.accent else colors.textMuted
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        containerColor = colors.appBg
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            NavHost(
                                navController = navController,
                                startDestination = CalcKey.HOME.name,
                                enterTransition = { androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) },
                                exitTransition = { androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(300)) },
                                popEnterTransition = { androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) },
                                popExitTransition = { androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(300)) }
                            ) {
                                composable(CalcKey.HOME.name) {
                                    HomeScreen(
                                        colors = colors,
                                        viewModel = viewModel,
                                        onSelectCalc = { key ->
                                            viewModel.recordToolOpened(context, key.name)
                                            navController.navigate(key.name) {
                                                popUpTo(CalcKey.HOME.name) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                }
                                composable(CalcKey.AI.name) { AIAssistantScreen(colors) }
                                composable(CalcKey.LIVE_PRICES.name) { LivePricesScreen(colors) }
                                 composable(CalcKey.ECONOMIC_INDICATORS.name) { EconomicIndicatorsScreen(colors) }
                                composable(CalcKey.WEATHER.name) { WeatherScreen(colors) }
                                composable(CalcKey.PRAYER.name) { PrayerTimesScreen(colors) }
                                composable(CalcKey.QIBLA.name) { QiblaDirectionScreen(colors) }
                                composable(CalcKey.ADHKAR.name) { AdhkarScreen(colors) }
                                composable(CalcKey.TASBIH.name) { TasbihScreen(colors) }
                                composable(CalcKey.QURAN.name) { QuranScreen(colors) }
                                composable(CalcKey.ZAKAT.name) { ZakatCalcScreen(colors) }
                                composable(CalcKey.ADHAN_SETTINGS.name) { AdhanSettingsScreen(colors, viewModel) }
                                composable(CalcKey.BASIC.name) { BasicCalculatorScreen(colors) }
                                composable(CalcKey.CURRENCY.name) { CurrencyConverterScreen(colors) }
                                composable(CalcKey.GOLD.name) { GoldCalcScreen(colors) }
                                composable(CalcKey.UNIT.name) { UnitConverterScreen(colors) }
                                composable(CalcKey.DISCOUNT.name) { DiscountCalcScreen(colors) }
                                composable(CalcKey.LOAN.name) { LoanCalcScreen(colors) }
                                composable(CalcKey.SAVINGS.name) { SavingsCalcScreen(colors) }
                                composable(CalcKey.SALES_TAX.name) { SalesTaxCalcScreen(colors) }
                                composable(CalcKey.TIP.name) { TipCalcScreen(colors) }
                                composable(CalcKey.PERCENT.name) { PercentageCalcScreen(colors) }
                                composable(CalcKey.UNIT_PRICE.name) { UnitPriceCalcScreen(colors) }
                                composable(CalcKey.WORLD_TIME.name) { WorldTimeScreen(colors) }
                                composable(CalcKey.DATE.name) { DateCalcScreen(colors) }
                                composable(CalcKey.AGE.name) { AgeCalcScreen(colors) }
                                composable(CalcKey.COUNTDOWN.name) { CountdownScreen(colors) }
                                composable(CalcKey.HEALTH.name) { HealthCalcScreen(colors) }
                                composable(CalcKey.OVULATION.name) { OvulationCalcScreen(colors) }
                                composable(CalcKey.FUEL_COST.name) { FuelCostCalcScreen(colors) }
                                composable(CalcKey.FUEL_EFF.name) { FuelEfficiencyCalcScreen(colors) }
                                composable(CalcKey.NUM_WORDS.name) { NumberToWordsScreen(colors) }
                                composable(CalcKey.GPA.name) { GPACalcScreen(colors) }
                                composable(CalcKey.HEX.name) { HexConverterScreen(colors) }
                            }
                        }
                    }

                    if (showAboutModal) {
                        AboutModal(
                            colors = colors,
                            onDismiss = { viewModel.setShowAboutModal(false) }
                        )
                    }
                }
            }
        }
    }
}
