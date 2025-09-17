package com.example.chatferit.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatferit.data.repository.ThemeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themeRepository: ThemeRepository
) : ViewModel() {

    val currentThemeSetting: StateFlow<ThemeSetting> = themeRepository.themeSettingFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeSetting.SYSTEM
        )

    fun setThemeSetting(newSetting: ThemeSetting) {
        viewModelScope.launch {
            themeRepository.setThemeSetting(newSetting)
        }
    }

    fun getNextThemeSetting(): ThemeSetting {
        return when (currentThemeSetting.value) {
            ThemeSetting.SYSTEM -> ThemeSetting.LIGHT
            ThemeSetting.LIGHT -> ThemeSetting.DARK
            ThemeSetting.DARK -> ThemeSetting.SYSTEM
        }
    }
}