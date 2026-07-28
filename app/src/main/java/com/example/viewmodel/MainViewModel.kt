package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.example.model.CalcKey
import com.example.ui.theme.AppThemeKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel : ViewModel() {

    private val PREFS_NAME = "clevcalc_theme_prefs"
    private val KEY_THEME = "selected_theme_key"

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

    fun initTheme(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedThemeStr = prefs.getString(KEY_THEME, null)
        if (savedThemeStr != null) {
            try {
                _currentThemeKey.value = AppThemeKey.valueOf(savedThemeStr)
            } catch (_: Exception) {}
        }
    }

    fun setTheme(context: Context, themeKey: AppThemeKey) {
        _currentThemeKey.value = themeKey
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME, themeKey.name).apply()
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
