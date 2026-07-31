package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.UserPreferencesRepository
import com.example.model.CalcKey
import com.example.ui.theme.AppThemeKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private var prefsRepository: UserPreferencesRepository? = null

    private val _currentThemeKey = MutableStateFlow<AppThemeKey>(AppThemeKey.ELEGANT_DARK)
    val currentThemeKey: StateFlow<AppThemeKey> = _currentThemeKey.asStateFlow()

    private val _currentCalcKey = MutableStateFlow(CalcKey.HOME)
    val currentCalcKey: StateFlow<CalcKey> = _currentCalcKey.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _showThemesModal = MutableStateFlow(false)
    val showThemesModal: StateFlow<Boolean> = _showThemesModal.asStateFlow()

    private val _showAboutModal = MutableStateFlow(false)
    val showAboutModal: StateFlow<Boolean> = _showAboutModal.asStateFlow()

    // Persistent favorites & recents
    private val _favoriteTools = MutableStateFlow<Set<String>>(emptySet())
    val favoriteTools: StateFlow<Set<String>> = _favoriteTools.asStateFlow()

    private val _recentTools = MutableStateFlow<List<String>>(emptyList())
    val recentTools: StateFlow<List<String>> = _recentTools.asStateFlow()

    fun initTheme(context: Context) {
        if (prefsRepository == null) {
            val repo = UserPreferencesRepository(context.applicationContext)
            prefsRepository = repo
            viewModelScope.launch {
                repo.selectedThemeFlow.collectLatest { themeKey ->
                    _currentThemeKey.value = themeKey
                }
            }
            viewModelScope.launch {
                repo.favoriteToolsFlow.collectLatest { favorites ->
                    _favoriteTools.value = favorites
                }
            }
            viewModelScope.launch {
                repo.recentToolsFlow.collectLatest { recents ->
                    _recentTools.value = recents
                }
            }
        }
    }

    fun setTheme(context: Context, themeKey: AppThemeKey) {
        _currentThemeKey.value = themeKey
        viewModelScope.launch {
            val repo = prefsRepository ?: UserPreferencesRepository(context.applicationContext).also { prefsRepository = it }
            repo.saveTheme(themeKey)
        }
    }

    fun toggleTheme(context: Context) {
        val nextTheme = if (_currentThemeKey.value == AppThemeKey.ELEGANT_DARK) {
            AppThemeKey.LIGHT
        } else {
            AppThemeKey.ELEGANT_DARK
        }
        setTheme(context, nextTheme)
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

    // Toggle favorite tool status
    fun toggleFavorite(context: Context, toolId: String) {
        viewModelScope.launch {
            val repo = prefsRepository ?: UserPreferencesRepository(context.applicationContext).also { prefsRepository = it }
            repo.toggleFavoriteTool(toolId)
        }
    }

    // Record tool opening
    fun recordToolOpened(context: Context, toolId: String) {
        if (toolId == "HOME" || toolId == "home") return
        viewModelScope.launch {
            val repo = prefsRepository ?: UserPreferencesRepository(context.applicationContext).also { prefsRepository = it }
            repo.addRecentTool(toolId)
        }
    }
}
