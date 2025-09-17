package com.example.chatferit.ui.theme

import androidx.datastore.preferences.core.stringPreferencesKey

enum class ThemeSetting {
    SYSTEM, LIGHT, DARK
}

object ThemePreferenceKeys {
    val THEME_SETTING = stringPreferencesKey("theme_setting")
}