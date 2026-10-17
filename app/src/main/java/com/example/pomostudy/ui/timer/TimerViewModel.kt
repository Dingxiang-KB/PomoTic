package com.example.pomostudy.ui.timer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.pomostudy.data.local.entity.StudyRecordEntity
import com.example.pomostudy.data.local.entity.TagEntity
import com.example.pomostudy.data.local.entity.TodoEntity
import com.example.pomostudy.data.repository.AppSettings
import com.example.pomostudy.di.AppContainer
import com.example.pomostudy.domain.model.TimerMode
import com.example.pomostudy.domain.model.TimerPhase
import com.example.pomostudy.service.TimerService
import com.example.pomostudy.service.TimerUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** 计时页 ViewModel：转发计时命令到 [com.example.pomostudy.service.TimerController] 并管理前后台服务。 */
class TimerViewModel(
    application: Application,
    private val container: AppContainer
) : AndroidViewModel(application) {

    val timerState: StateFlow<TimerUiState> = container.timerController.state

    val settings: StateFlow<AppSettings> = container.settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    val tags: StateFlow<List<TagEntity>> = container.tagRepository.tags
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val todos: StateFlow<List<TodoEntity>> = container.todoRepository.todos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val records: StateFlow<List<StudyRecordEntity>> = container.studyRepository.records
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setMode(mode: TimerMode) = container.timerController.setMode(mode)

    fun setPhase(phase: TimerPhase) = container.timerController.setPhase(phase)

    fun setTag(id: Long?) = container.timerController.setTag(id)

    fun setNote(note: String) = container.timerController.setNote(note)

    fun setBoundTodo(todo: TodoEntity?) =
        container.timerController.setBoundTodo(todo?.id, todo?.title)

    fun setAutoComplete(value: Boolean) = container.timerController.setAutoComplete(value)

    fun start() {
        container.timerController.start()
        TimerService.start(getApplication())
    }

    fun pause() = container.timerController.pause()

    fun resume() {
        container.timerController.resume()
        TimerService.start(getApplication())
    }

    fun reset() {
        container.timerController.reset()
        TimerService.stop(getApplication())
    }

    fun finish() {
        container.timerController.finish()
        TimerService.stop(getApplication())
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                TimerViewModel(app, container)
            }
        }
    }
}
