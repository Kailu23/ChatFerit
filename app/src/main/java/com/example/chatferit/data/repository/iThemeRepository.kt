package com.example.chatferit.data.repository

import com.example.chatferit.ui.theme.ThemeSetting
import kotlinx.coroutines.flow.Flow

interface iThemeRepository {
    val themeSettingFlow: Flow<ThemeSetting>
    suspend fun setThemeSetting(themeSetting: ThemeSetting)
}