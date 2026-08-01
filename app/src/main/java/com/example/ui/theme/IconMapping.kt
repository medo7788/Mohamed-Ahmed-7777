package com.example.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.model.CalcKey
import com.example.model.CategoryKey

/**
 * Unified vector icon system.
 *
 * Every icon used across ClevCalc Pro is a Material vector (already bundled via
 * material-icons-extended — zero extra APK weight, renders crisp at any density,
 * and looks the same on every device instead of relying on the OS emoji font).
 *
 * This is the single source of truth for "what icon represents this tool" — do not
 * inline emoji or icons anywhere else in the app; add the mapping here instead.
 */
object AppIcons {

    fun forCategory(category: CategoryKey): ImageVector = when (category) {
        CategoryKey.ISLAMIC -> Icons.Filled.Mosque
        CategoryKey.FINANCE -> Icons.Filled.AccountBalance
        CategoryKey.DATE_TIME -> Icons.Filled.CalendarMonth
        CategoryKey.HEALTH -> Icons.Filled.Favorite
        CategoryKey.UTILITIES -> Icons.Filled.Build
    }

    fun forCalc(calc: CalcKey): ImageVector = when (calc) {
        CalcKey.HOME -> Icons.Filled.Home

        // Featured
        CalcKey.AI -> Icons.Filled.AutoAwesome
        CalcKey.LIVE_PRICES -> Icons.Filled.ShowChart
        CalcKey.ECONOMIC_INDICATORS -> Icons.Filled.TrendingUp
        CalcKey.WEATHER -> Icons.Filled.WbSunny

        // Islamic
        CalcKey.PRAYER -> Icons.Filled.Mosque
        CalcKey.QIBLA -> Icons.Filled.Explore
        CalcKey.ADHKAR -> Icons.Filled.MenuBook
        CalcKey.TASBIH -> Icons.Filled.FiberManualRecord
        CalcKey.QURAN -> Icons.Filled.ImportContacts
        CalcKey.ZAKAT -> Icons.Filled.VolunteerActivism
        CalcKey.ADHAN_SETTINGS -> Icons.Filled.NotificationsActive

        // Calculators & currencies
        CalcKey.BASIC -> Icons.Filled.Calculate
        CalcKey.CURRENCY -> Icons.Filled.CurrencyExchange
        CalcKey.GOLD -> Icons.Filled.Diamond
        CalcKey.UNIT -> Icons.Filled.Straighten

        // Finance
        CalcKey.DISCOUNT -> Icons.Filled.Sell
        CalcKey.LOAN -> Icons.Filled.AccountBalance
        CalcKey.SAVINGS -> Icons.Filled.Savings
        CalcKey.SALES_TAX -> Icons.Filled.Receipt
        CalcKey.TIP -> Icons.Filled.Payments
        CalcKey.PERCENT -> Icons.Filled.Percent
        CalcKey.UNIT_PRICE -> Icons.Filled.ShoppingCart

        // Dates & time
        CalcKey.WORLD_TIME -> Icons.Filled.Public
        CalcKey.DATE -> Icons.Filled.CalendarMonth
        CalcKey.AGE -> Icons.Filled.Cake
        CalcKey.COUNTDOWN -> Icons.Filled.Timer

        // Health
        CalcKey.HEALTH -> Icons.Filled.MonitorHeart
        CalcKey.OVULATION -> Icons.Filled.Spa

        // Vehicle
        CalcKey.FUEL_COST -> Icons.Filled.LocalGasStation
        CalcKey.FUEL_EFF -> Icons.Filled.DirectionsCar

        // Utility
        CalcKey.NUM_WORDS -> Icons.Filled.EditNote
        CalcKey.GPA -> Icons.Filled.School
        CalcKey.HEX -> Icons.Filled.Tag
    }

    // Small set of general-purpose icons reused by shared components
    val Search = Icons.Filled.Search
    val Mic = Icons.Filled.Mic
    val Back = Icons.AutoMirrored.Filled.ArrowBack
    val Theme = Icons.Filled.Palette
    val Sun = Icons.Filled.WbSunny
    val Moon = Icons.Filled.DarkMode
    val More = Icons.Filled.MoreVert
    val Info = Icons.Filled.Info
    val Check = Icons.Filled.Check
    val Location = Icons.Filled.MyLocation
    val LocationSearching = Icons.Filled.LocationSearching
    val LocationOff = Icons.Filled.LocationOff
    val Refresh = Icons.Filled.Refresh
    val Notifications = Icons.Filled.Notifications
    val NotificationsOff = Icons.Filled.NotificationsOff
    val Vibration = Icons.Filled.Vibration
    val VolumeUp = Icons.Filled.VolumeUp
    val Settings = Icons.Filled.Settings
    val PlayCircle = Icons.Filled.PlayCircle
    val Warning = Icons.Filled.WarningAmber

    fun forWeather(iconId: String): ImageVector = when (iconId) {
        "sunny" -> Icons.Filled.WbSunny
        "partly_cloudy" -> Icons.Filled.WbCloudy
        "cloudy" -> Icons.Filled.Cloud
        "overcast" -> Icons.Filled.CloudQueue
        "fog" -> Icons.Filled.Cloud // Fallback
        "rain" -> Icons.Filled.WaterDrop
        "heavy_rain" -> Icons.Filled.Umbrella
        "thunderstorm" -> Icons.Filled.Thunderstorm
        "snow" -> Icons.Filled.AcUnit
        else -> Icons.Filled.WbSunny
    }

    val EconomicOverview = Icons.Filled.BarChart
    val EconomicAdvisor = Icons.Filled.SupportAgent
    val Inflation = Icons.Filled.ShowChart
    val InterestRate = Icons.Filled.AccountBalance
    val Growth = Icons.Filled.Speed
    val Unemployment = Icons.Filled.Groups
    val StockMarket = Icons.Filled.Store
    val Summary = Icons.Filled.FactCheck
    val Humidity = Icons.Filled.InvertColors
    val Wind = Icons.Filled.Air
    val Rain = Icons.Filled.Water
    val Temperature = Icons.Filled.Thermostat
    val Gold = Icons.Filled.Diamond
    val AI = Icons.Filled.AutoAwesome

    fun forCommodity(symbol: String): ImageVector = when (symbol) {
        "oil" -> Icons.Filled.OilBarrel
        "gasoline" -> Icons.Filled.LocalGasStation
        "natural_gas" -> Icons.Filled.LocalFireDepartment
        "copper" -> Icons.Filled.Hardware
        else -> Icons.Filled.Inventory2
    }
}
