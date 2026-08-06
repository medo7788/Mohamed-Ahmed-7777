package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.AppThemeKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val SELECTED_THEME = stringPreferencesKey("selected_theme_key")
        val PREFERRED_CURRENCY = stringPreferencesKey("preferred_currency_key")
        val PREFERRED_UNIT = stringPreferencesKey("preferred_unit_key")
        val FAVORITE_TOOLS = stringSetPreferencesKey("favorite_tools_key")
        val RECENT_TOOLS = stringPreferencesKey("recent_tools_key") // comma-separated tool IDs
        val USER_NAME = stringPreferencesKey("user_name_key")
    }

    val userNameFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.USER_NAME] ?: "أحمد"
    }

    suspend fun saveUserName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_NAME] = name
        }
    }

    val selectedThemeFlow: Flow<AppThemeKey> = context.dataStore.data.map { preferences ->
        val themeName = preferences[PreferencesKeys.SELECTED_THEME] ?: AppThemeKey.ELEGANT_DARK.name
        try {
            AppThemeKey.valueOf(themeName)
        } catch (_: Exception) {
            AppThemeKey.ELEGANT_DARK
        }
    }

    suspend fun saveTheme(themeKey: AppThemeKey) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_THEME] = themeKey.name
        }
    }

    val preferredCurrencyFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.PREFERRED_CURRENCY] ?: "EGP"
    }

    suspend fun savePreferredCurrency(currency: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PREFERRED_CURRENCY] = currency
        }
    }

    // Favorites (Pinned) tools list as flow
    val favoriteToolsFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.FAVORITE_TOOLS] ?: emptySet()
    }

    suspend fun toggleFavoriteTool(toolId: String) {
        context.dataStore.edit { preferences ->
            val currentFavorites = preferences[PreferencesKeys.FAVORITE_TOOLS] ?: emptySet()
            val newFavorites = if (currentFavorites.contains(toolId)) {
                currentFavorites - toolId
            } else {
                currentFavorites + toolId
            }
            preferences[PreferencesKeys.FAVORITE_TOOLS] = newFavorites
        }
    }

    // Recently used tools as flow (comma-separated string)
    val recentToolsFlow: Flow<List<String>> = context.dataStore.data.map { preferences ->
        val recentStr = preferences[PreferencesKeys.RECENT_TOOLS] ?: ""
        if (recentStr.isBlank()) {
            emptyList()
        } else {
            recentStr.split(",").filter { it.isNotBlank() }
        }
    }

    suspend fun addRecentTool(toolId: String) {
        if (toolId == "HOME") return
        context.dataStore.edit { preferences ->
            val recentStr = preferences[PreferencesKeys.RECENT_TOOLS] ?: ""
            val currentRecents = if (recentStr.isBlank()) {
                emptyList()
            } else {
                recentStr.split(",").filter { it.isNotBlank() }
            }.toMutableList()

            // Move to front or add
            currentRecents.remove(toolId)
            currentRecents.add(0, toolId)

            // Keep max 5 recent tools
            val trimmedRecents = currentRecents.take(5)
            preferences[PreferencesKeys.RECENT_TOOLS] = trimmedRecents.joinToString(",")
        }
    }
}
