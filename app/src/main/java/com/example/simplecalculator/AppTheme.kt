package com.example.simplecalculator

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit

object AppTheme {
    private const val PREFS_NAME = "calculator_prefs"
    private const val KEY_THEME_MODE = "theme_mode"

    const val MODE_LIGHT = AppCompatDelegate.MODE_NIGHT_NO
    const val MODE_DARK = AppCompatDelegate.MODE_NIGHT_YES

    fun applySavedMode(context: Context) {
        AppCompatDelegate.setDefaultNightMode(getThemeMode(context))
    }

    fun getThemeMode(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }

    fun setThemeMode(context: Context, mode: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_THEME_MODE, mode)
            .apply()
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}
