package com.example.cryptick

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {
    private const val PREF_NAME = "theme_preferences"
    private const val KEY_THEME = "theme"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun setTheme(context: Context, isDark: Boolean) {
        getPreferences(context).edit().putString(KEY_THEME, if (isDark) "dark" else "light").apply()
        applyTheme(isDark)
    }

    fun loadSavedTheme(context: Context) {
        val theme = getPreferences(context).getString(KEY_THEME, "light")
        applyTheme(theme == "dark")
    }

    private fun applyTheme(isDark: Boolean) {
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
} 