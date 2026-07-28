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

            
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val coroutineScope = rememberCoroutineScope()
            
            BackHandler(enabled = drawerState.isOpen || currentCalcKey != CalcKey.BASIC) {
                if (drawerState.isOpen) {
                    coroutineScope.launch { drawerState.close() }
                } else if (currentCalcKey != CalcKey.BASIC) {
                    viewModel.setCalcKey(CalcKey.BASIC)
                }
            }


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
                                    onSearchChange = { viewModel.setSearchQuery(it) },
                                    colors = colors,
                                    onSelectCalc = { key ->
                                        viewModel.setCalcKey(key)
                                        coroutineScope.launch { drawerState.close() }
                                    },
                                    onOpenThemes = {
                                        viewModel.setShowThemesModal(true)
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
}
