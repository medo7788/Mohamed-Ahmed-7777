package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.ui.theme.AppIcons
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

private data class BottomNavItem(
    val label: String,
    val key: CalcKey,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalLayoutApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            LaunchedEffect(Unit) {
                viewModel.initTheme(context)
            }

            val currentThemeKey by viewModel.currentThemeKey.collectAsState()

            LaunchedEffect(currentThemeKey) {
                val isDark = currentThemeKey == AppThemeKey.ELEGANT_DARK
                enableEdgeToEdge(
                    statusBarStyle = if (isDark) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                    },
                    navigationBarStyle = if (isDark) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                    }
                )
            }
            val currentCalcKey by viewModel.currentCalcKey.collectAsState()
            val searchQuery by viewModel.searchQuery.collectAsState()
            val showThemesModal by viewModel.showThemesModal.collectAsState()
            val showAboutModal by viewModel.showAboutModal.collectAsState()

            val navController = rememberNavController()

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

            BackHandler(enabled = currentCalcKey != CalcKey.HOME) {
                navController.popBackStack()
            }

            val colors = getThemeColors(currentThemeKey)

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                ClevCalcTheme(themeKey = currentThemeKey) {
                    Scaffold(
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        topBar = {
                            if (currentCalcKey != CalcKey.HOME) {
                                AppHeader(
                                    currentCalc = currentCalcKey,
                                    colors = colors,
                                    onGoHome = {
                                        if (navController.previousBackStackEntry != null) {
                                            navController.popBackStack()
                                        } else if (currentCalcKey != CalcKey.HOME) {
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
                            }
                        },
                        bottomBar = {
                            val isImeVisible = WindowInsets.isImeVisible
                            if (!isImeVisible) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .navigationBarsPadding()
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(68.dp),
                                        shape = RoundedCornerShape(32.dp),
                                        color = Color(0xFF0F1422).copy(alpha = 0.94f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.35f)),
                                        shadowElevation = 12.dp
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceEvenly,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // 1. الرئيسية
                                            val isHomeSelected = currentCalcKey == CalcKey.HOME
                                            BottomNavItemColumn(
                                                label = "الرئيسية",
                                                icon = AppIcons.forCalc(CalcKey.HOME),
                                                isSelected = isHomeSelected,
                                                onClick = {
                                                    if (!isHomeSelected) {
                                                        navController.navigate(CalcKey.HOME.name) {
                                                            popUpTo(CalcKey.HOME.name) { inclusive = true }
                                                            launchSingleTop = true
                                                        }
                                                    }
                                                }
                                            )

                                            // 2. العبادات والأذان
                                            val isPrayerSelected = currentCalcKey in listOf(
                                                CalcKey.PRAYER, CalcKey.QIBLA, CalcKey.QURAN,
                                                CalcKey.ADHKAR, CalcKey.TASBIH, CalcKey.ZAKAT
                                            )
                                            BottomNavItemColumn(
                                                label = "العبادات",
                                                icon = AppIcons.forCalc(CalcKey.PRAYER),
                                                isSelected = isPrayerSelected,
                                                onClick = {
                                                    if (!isPrayerSelected) {
                                                        navController.navigate(CalcKey.PRAYER.name) {
                                                            popUpTo(CalcKey.HOME.name) { saveState = true }
                                                            launchSingleTop = true
                                                            restoreState = true
                                                        }
                                                    }
                                                }
                                            )

                                            // 3. المساعد الذكي
                                            val isAiSelected = currentCalcKey == CalcKey.AI
                                            BottomNavItemColumn(
                                                label = "المساعد الذكي",
                                                icon = AppIcons.forCalc(CalcKey.AI),
                                                isSelected = isAiSelected,
                                                onClick = {
                                                    if (!isAiSelected) {
                                                        navController.navigate(CalcKey.AI.name) {
                                                            popUpTo(CalcKey.HOME.name) { saveState = true }
                                                            launchSingleTop = true
                                                            restoreState = true
                                                        }
                                                    }
                                                }
                                            )

                                            // 4. الإعدادات
                                            val isSettingsSelected = currentCalcKey == CalcKey.SETTINGS
                                            BottomNavItemColumn(
                                                label = "الإعدادات",
                                                icon = AppIcons.forCalc(CalcKey.SETTINGS),
                                                isSelected = isSettingsSelected,
                                                onClick = {
                                                    if (!isSettingsSelected) {
                                                        navController.navigate(CalcKey.SETTINGS.name) {
                                                            popUpTo(CalcKey.HOME.name) { saveState = true }
                                                            launchSingleTop = true
                                                            restoreState = true
                                                        }
                                                    }
                                                }
                                            )
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
                                                launchSingleTop = true
                                            }
                                        }
                                    )
                                }
                                composable(CalcKey.AI.name) { AIAssistantScreen(colors) }
                                composable(CalcKey.LIVE_PRICES.name) { LivePricesScreen(colors) }
                                composable(CalcKey.ECONOMIC_INDICATORS.name) { EconomicIndicatorsScreen(colors) }
                                composable(CalcKey.WEATHER.name) { WeatherScreen(colors) }
                                composable(CalcKey.PRAYER.name) {
                                    PrayerTimesScreen(
                                        colors = colors,
                                        onNavigate = { key ->
                                            viewModel.recordToolOpened(context, key.name)
                                            navController.navigate(key.name) {
                                                launchSingleTop = true
                                            }
                                        }
                                    )
                                }
                                composable(CalcKey.QIBLA.name) {
                                    QiblaCompassScreen(
                                        colors = colors,
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                                composable(CalcKey.ADHKAR.name) { AdhkarScreen(colors) }
                                composable(CalcKey.TASBIH.name) { TasbihScreen(colors) }
                                composable(CalcKey.QURAN.name) { QuranScreen(colors) }
                                composable(CalcKey.ZAKAT.name) { ZakatCalcScreen(colors) }
                                composable(CalcKey.SETTINGS.name) {
                                    SettingsScreen(
                                        colors = colors,
                                        viewModel = viewModel,
                                        onOpenAdhanSettings = { navController.navigate(CalcKey.ADHAN_SETTINGS.name) },
                                        onOpenAbout = { viewModel.setShowAboutModal(true) },
                                        onOpenPrivacy = { /* privacy policy */ }
                                    )
                                }
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
                                composable(CalcKey.LEDGER.name) { IncomeExpenseLedgerScreen(colors) }
                                composable(CalcKey.WORLD_TIME.name) { WorldTimeScreen(colors) }
                                composable(CalcKey.DATE.name) { DateCalcScreen(colors) }
                                composable(CalcKey.AGE.name) { AgeCalcScreen(colors) }
                                composable(CalcKey.COUNTDOWN.name) { CountdownScreen(colors) }
                                composable(CalcKey.HEALTH.name) { UltimateHealthDashboard(colors) }
                                composable(CalcKey.OVULATION.name) { UltimateHealthDashboard(colors) }
                                composable(CalcKey.FUEL_COST.name) { FuelCostCalcScreen(colors) }
                                composable(CalcKey.FUEL_EFF.name) { FuelEfficiencyCalcScreen(colors) }
                                composable(CalcKey.NUM_WORDS.name) { NumberToWordsScreen(colors) }
                                composable(CalcKey.GPA.name) { GPACalcScreen(colors) }
                                composable(CalcKey.HEX.name) { HexConverterScreen(colors) }
                                composable(CalcKey.NOTES.name) { NotepadScreen(colors) }
                                composable(CalcKey.QR_TOOL.name) { QrScannerToolScreen(colors) }
                                composable(CalcKey.NEWS_BROWSER.name) { NewsAndWebBrowserScreen(colors) }
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

@Composable
private fun BottomNavItemColumn(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        androidx.compose.ui.graphics.Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFD4AF37).copy(alpha = 0.45f),
                                Color(0xFFD4AF37).copy(alpha = 0.12f)
                            )
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color(0xFFD4AF37),
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
            color = if (isSelected) Color(0xFFD4AF37) else Color(0xFF94A3B8)
        )
    }
}
