package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
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
}
