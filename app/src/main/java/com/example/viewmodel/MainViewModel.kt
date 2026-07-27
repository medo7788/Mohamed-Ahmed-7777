package com.example.viewmodel

import androidx.lifecycle.ViewModel
import com.example.model.CalcKey
import com.example.ui.theme.AppThemeKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel : ViewModel() {

    private val _currentThemeKey = MutableStateFlow(AppThemeKey.ELEGANT_DARK)
    val currentThemeKey: StateFlow<AppThemeKey> = _currentThemeKey.asStateFlow()

    private val _currentCalcKey = MutableStateFlow(CalcKey.BASIC)
    val currentCalcKey: StateFlow<CalcKey> = _currentCalcKey.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _showThemesModal = MutableStateFlow(false)
    val showThemesModal: StateFlow<Boolean> = _showThemesModal.asStateFlow()

    private val _showAboutModal = MutableStateFlow(false)
    val showAboutModal: StateFlow<Boolean> = _showAboutModal.asStateFlow()

    fun setTheme(themeKey: AppThemeKey) {
        _currentThemeKey.value = themeKey
    }

    fun setCalcKey(calcKey: CalcKey) {
        _currentCalcKey.value = calcKey
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setShowThemesModal(show: Boolean) {
        _showThemesModal.value = show
    }

    fun setShowAboutModal(show: Boolean) {
        _showAboutModal.value = show
    }
}
