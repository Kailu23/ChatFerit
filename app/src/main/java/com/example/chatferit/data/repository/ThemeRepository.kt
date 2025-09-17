package com.example.chatferit.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.chatferit.ui.theme.ThemePreferenceKeys
import com.example.chatferit.ui.theme.ThemeSetting
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject


class ThemeRepository @Inject constructor(
    private val themeDataStore: DataStore<Preferences>
) : iThemeRepository{
    override val themeSettingFlow: Flow<ThemeSetting> = themeDataStore.data
        .map { preferences ->
            val themeName = preferences[ThemePreferenceKeys.THEME_SETTING] ?: ThemeSetting.SYSTEM.name
            try {
                ThemeSetting.valueOf(themeName)
            } catch (e: IllegalArgumentException) {
                ThemeSetting.SYSTEM
            }
        }

    override suspend fun setThemeSetting(themeSetting: ThemeSetting) {
        themeDataStore.edit { setting ->
            setting[ThemePreferenceKeys.THEME_SETTING] = themeSetting.name
        }
    }
}