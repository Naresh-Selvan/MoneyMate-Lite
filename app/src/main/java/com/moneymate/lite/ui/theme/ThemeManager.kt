package com.moneymate.lite.ui.theme

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages the theme preference (System, Light, Dark) for the app.
 * Persists the setting using SharedPreferences.
 */
class ThemeManager(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    private val _themeMode = MutableStateFlow(getSavedThemeMode())
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _isDarkMode = MutableStateFlow(shouldBeDarkMode(getSavedThemeMode()))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private fun getSavedThemeMode(): AppThemeMode {
        val savedName = prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.name)
        return try {
            AppThemeMode.valueOf(savedName ?: AppThemeMode.SYSTEM.name)
        } catch (e: Exception) {
            AppThemeMode.SYSTEM
        }
    }

    private fun shouldBeDarkMode(mode: AppThemeMode): Boolean {
        return when (mode) {
            AppThemeMode.SYSTEM -> isSystemInDarkTheme(context)
            AppThemeMode.LIGHT -> false
            AppThemeMode.DARK -> true
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        _isDarkMode.value = shouldBeDarkMode(mode)
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun toggleDarkMode() {
        val current = _themeMode.value
        val nextMode = if (current == AppThemeMode.DARK) AppThemeMode.LIGHT else AppThemeMode.DARK
        setThemeMode(nextMode)
    }

    companion object {
        private const val PREFS_NAME = "moneymate_theme_prefs"
        private const val KEY_THEME_MODE = "app_theme_mode"

        private fun isSystemInDarkTheme(context: Context): Boolean {
            val mode = context.resources.configuration.uiMode and
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK
            return mode == android.content.res.Configuration.UI_MODE_NIGHT_YES
        }
    }
}

/**
 * CompositionLocal for accessing ThemeManager throughout the app.
 */
val LocalThemeManager = staticCompositionLocalOf<ThemeManager> {
    error("No ThemeManager provided")
}

