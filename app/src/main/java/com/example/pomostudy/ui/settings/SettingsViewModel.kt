package com.example.pomostudy.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.pomostudy.data.repository.AppSettings
import com.example.pomostudy.di.AppContainer
import com.example.pomostudy.domain.model.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 设置页 ViewModel：读写全局配置（番茄时长、休息时长、每日目标、主题模式）。 */
class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    val settings: StateFlow<AppSettings> = container.settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun setWorkMinutes(minutes: Int) = viewModelScope.launch {
        container.settingsRepository.setWorkMinutes(minutes)
    }

    fun setBreakMinutes(minutes: Int) = viewModelScope.launch {
        container.settingsRepository.setBreakMinutes(minutes)
    }

    fun setDailyGoalMinutes(minutes: Int) = viewModelScope.launch {
        container.settingsRepository.setDailyGoalMinutes(minutes)
    }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch {
        container.settingsRepository.setThemeMode(mode)
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { SettingsViewModel(container) }
        }
    }
}
