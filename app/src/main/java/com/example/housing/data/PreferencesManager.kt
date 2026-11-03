package com.example.housing.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeMode(val key: String, val labelAr: String) {
    SYSTEM("system", "تلقائي"),
    LIGHT("light", "فاتح"),
    DARK("dark", "داكن")
}

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("housing_settings", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(getStoredThemeMode())
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private fun getStoredThemeMode(): AppThemeMode {
        val saved = prefs.getString("theme_mode", AppThemeMode.SYSTEM.key)
        return AppThemeMode.entries.find { it.key == saved } ?: AppThemeMode.SYSTEM
    }

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString("theme_mode", mode.key).apply()
        _themeMode.value = mode
    }
}
