package com.example.ui.screens

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// 1. Utilities
fun String.normalizeArabic(): String {
    return this.lowercase()
        .replace(Regex("[أإآا]"), "ا")
        .replace(Regex("[ةه]"), "ه")
        .replace(Regex("[ىي]"), "ي")
        .trim()
}

// 2. Domain Models
sealed interface DashboardUiState {
    object Loading : DashboardUiState
    object Empty : DashboardUiState
    data class Error(val message: String) : DashboardUiState
    
    data class Success(
        val userName: String = "أحمد",
        val dateMiladi: String = "الخميس، 6 أغسطس 2026",
        val dateHijri: String = "15 محرم 1447 هـ",
        val nextPrayerName: String = "صلاة العصر",
        val nextPrayerTime12h: String = "03:45 م",
        val nextPrayerCity: String = "القاهرة، مصر",
        val remainingMinutesToPrayer: Int = 41,
        val totalPrayerIntervalMinutes: Int = 180,
        val featureTools: List<FeatureToolModel>,
        val recentTools: List<RecentToolDomainModel>,
        val searchQuery: String = "",
        val isOffline: Boolean = false,
        val hasNotificationBadge: Boolean = true
    ) : DashboardUiState
}

data class FeatureToolModel(
    val id: String,
    val title: String,
    val badgeText: String,
    val isSquareBmiBadge: Boolean = false,
    val isFullWidth: Boolean = false,
    val subtitle: String? = null,
    val accentColorHex: Long
)

data class RecentToolDomainModel(
    val id: String,
    val title: String,
    val iconType: ToolIconType
)

enum class ToolIconType { LOAN_CALC, GOLD_CALC, CURRENCY_CONVERTER, PRAYER_CALC, ZAKAT_CALC }

// 3. Production-Ready ViewModel
class DashboardViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init { loadData() }

    fun loadData() {
        _uiState.value = DashboardUiState.Loading
        _uiState.value = DashboardUiState.Success(
            featureTools = listOf(
                FeatureToolModel("TIME_DATE", "الوقت والتاريخ", "4 أدوات", accentColorHex = 0xFF00D4CC),
                FeatureToolModel("CURRENCY_TOOLS", "أدوات العملة والأسعار", "7 أدوات", accentColorHex = 0xFFF5B041),
                FeatureToolModel("FINANCE_TOOLS", "المال والأسعار", "13 أداة", accentColorHex = 0xFF8B5CF6),
                FeatureToolModel("HEALTH_FITNESS", "الصحة واللياقة", "BMI", isSquareBmiBadge = true, accentColorHex = 0xFF10B981),
                FeatureToolModel("AI_ASSISTANT", "الذكاء الاصطناعي والمساعد", "ذكاء اصطناعي", isFullWidth = true, subtitle = "مساعدك الذكي للحسابات والفتوى والمال", accentColorHex = 0xFFF5B041)
            ),
            recentTools = listOf(
                RecentToolDomainModel("1", "حاسبة القرض", ToolIconType.LOAN_CALC),
                RecentToolDomainModel("2", "سعر الذهب اليوم", ToolIconType.GOLD_CALC),
                RecentToolDomainModel("3", "محول العملات", ToolIconType.CURRENCY_CONVERTER),
                RecentToolDomainModel("4", "مواقيت الصلاة", ToolIconType.PRAYER_CALC),
                RecentToolDomainModel("5", "حاسبة الزكاة", ToolIconType.ZAKAT_CALC)
            )
        )
    }

    fun retryFetchData() { loadData() }
    
    fun onSearchQueryChanged(query: String) {
        val currentState = _uiState.value
        if (currentState is DashboardUiState.Success) {
            _uiState.value = currentState.copy(searchQuery = query)
        }
    }
}
