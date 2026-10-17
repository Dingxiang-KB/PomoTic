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

/** 设置页 ViewModel：主题、数据导入导出、清空数据。 */
class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    val settings: StateFlow<AppSettings> = container.settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch {
        container.settingsRepository.setThemeMode(mode)
    }

    /** 导出全部数据为 JSON 文本。 */
    fun exportData(onResult: (Result<String>) -> Unit) = viewModelScope.launch {
        onResult(runCatching { container.exportData() })
    }

    /** 从 JSON 文本导入数据（覆盖现有）。 */
    fun importData(json: String, onResult: (Boolean) -> Unit) = viewModelScope.launch {
        val ok = runCatching { container.importData(json) }.isSuccess
        onResult(ok)
    }

    /** 清空所有本地数据（调试用）。 */
    fun clearAllData() = viewModelScope.launch {
        container.clearAllData()
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { SettingsViewModel(container) }
        }
    }
}
