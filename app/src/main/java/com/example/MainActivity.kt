package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.model.CalcKey
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.AppThemeKey
import com.example.ui.theme.ClevCalcTheme
import com.example.ui.theme.getThemeColors
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var currentThemeKey by remember { mutableStateOf(AppThemeKey.ELEGANT_DARK) }
            var currentCalcKey by remember { mutableStateOf(CalcKey.BASIC) }
            var searchQuery by remember { mutableStateOf("") }

            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val coroutineScope = rememberCoroutineScope()

            var showThemesModal by remember { mutableStateOf(false) }
            var showAboutModal by remember { mutableStateOf(false) }

            val colors = getThemeColors(currentThemeKey)

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                ClevCalcTheme(themeKey = currentThemeKey) {
                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            ModalDrawerSheet(
                                drawerContainerColor = colors.surface,
                                modifier = Modifier.width(320.dp)
                            ) {
                                AppDrawerContent(
                                    currentKey = currentCalcKey,
                                    searchQuery = searchQuery,
                                    onSearchChange = { searchQuery = it },
                                    colors = colors,
                                    onSelectCalc = { key ->
                                        currentCalcKey = key
                                        coroutineScope.launch { drawerState.close() }
                                    },
                                    onOpenThemes = {
                                        showThemesModal = true
                                        coroutineScope.launch { drawerState.close() }
                                    }
                                )
                            }
                        }
                    ) {
                        Scaffold(
                            topBar = {
                                AppHeader(
                                    currentCalc = currentCalcKey,
                                    colors = colors,
                                    onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                                    onOpenThemes = { showThemesModal = true },
                                    onOpenAbout = { showAboutModal = true }
                                )
                            },
                            containerColor = colors.appBg
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                when (currentCalcKey) {
                                    CalcKey.AI -> AIAssistantScreen(colors)
                                    CalcKey.LIVE_PRICES -> LivePricesScreen(colors)
                                    CalcKey.ECONOMIC_INDICATORS -> EconomicIndicatorsScreen(colors)
                                    CalcKey.WEATHER -> WeatherScreen(colors)
                                    CalcKey.PRAYER -> PrayerTimesScreen(colors)
                                    CalcKey.QIBLA -> QiblaDirectionScreen(colors)
                                    CalcKey.ADHKAR -> AdhkarScreen(colors)
                                    CalcKey.TASBIH -> TasbihScreen(colors)
                                    CalcKey.QURAN -> QuranScreen(colors)
                                    CalcKey.ZAKAT -> ZakatCalcScreen(colors)
                                    CalcKey.BASIC -> BasicCalculatorScreen(colors)
                                    CalcKey.CURRENCY -> CurrencyConverterScreen(colors)
                                    CalcKey.GOLD -> GoldCalcScreen(colors)
                                    CalcKey.UNIT -> UnitConverterScreen(colors)
                                    CalcKey.DISCOUNT -> DiscountCalcScreen(colors)
                                    CalcKey.LOAN -> LoanCalcScreen(colors)
                                    CalcKey.SAVINGS -> SavingsCalcScreen(colors)
                                    CalcKey.SALES_TAX -> SalesTaxCalcScreen(colors)
                                    CalcKey.TIP -> TipCalcScreen(colors)
                                    CalcKey.PERCENT -> PercentageCalcScreen(colors)
                                    CalcKey.UNIT_PRICE -> UnitPriceCalcScreen(colors)
                                    CalcKey.WORLD_TIME -> WorldTimeScreen(colors)
                                    CalcKey.DATE -> DateCalcScreen(colors)
                                    CalcKey.AGE -> AgeCalcScreen(colors)
                                    CalcKey.COUNTDOWN -> CountdownScreen(colors)
                                    CalcKey.HEALTH -> HealthCalcScreen(colors)
                                    CalcKey.OVULATION -> OvulationCalcScreen(colors)
                                    CalcKey.FUEL_COST -> FuelCostCalcScreen(colors)
                                    CalcKey.FUEL_EFF -> FuelEfficiencyCalcScreen(colors)
                                    CalcKey.NUM_WORDS -> NumberToWordsScreen(colors)
                                    CalcKey.GPA -> GPACalcScreen(colors)
                                    CalcKey.HEX -> HexConverterScreen(colors)
                                }
                            }
                        }

                        if (showThemesModal) {
                            ThemeSelectorModal(
                                currentTheme = currentThemeKey,
                                colors = colors,
                                onSelectTheme = {
                                    currentThemeKey = it
                                    showThemesModal = false
                                },
                                onDismiss = { showThemesModal = false }
                            )
                        }

                        if (showAboutModal) {
                            AboutModal(
                                colors = colors,
                                onDismiss = { showAboutModal = false }
                            )
                        }
                    }
                }
            }
        }
    }
}
