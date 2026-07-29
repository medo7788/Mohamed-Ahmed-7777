package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.model.CalcKey
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.AppThemeKey
import com.example.ui.theme.ClevCalcTheme
import com.example.ui.theme.getThemeColors
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.launch

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

            // Back handler to navigate back to Home screen if on sub-screen
            BackHandler(enabled = currentCalcKey != CalcKey.HOME) {
                navController.navigate(CalcKey.HOME.name) {
                    popUpTo(CalcKey.HOME.name) { inclusive = true }
                    launchSingleTop = true
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
                                onOpenThemes = { viewModel.setShowThemesModal(true) },
                                onOpenAbout = { viewModel.setShowAboutModal(true) }
                            )
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
                                    HomeScreen(colors) { key ->
                                        navController.navigate(key.name) {
                                            popUpTo(CalcKey.HOME.name) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
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

                    if (showThemesModal) {
                        ThemeSelectorModal(
                            currentTheme = currentThemeKey,
                            colors = colors,
                            onSelectTheme = {
                                viewModel.setTheme(context, it)
                                viewModel.setShowThemesModal(false)
                            },
                            onDismiss = { viewModel.setShowThemesModal(false) }
                        )
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
