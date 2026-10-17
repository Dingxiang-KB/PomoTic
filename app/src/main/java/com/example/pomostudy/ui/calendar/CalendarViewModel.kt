package com.example.pomostudy.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.pomostudy.data.local.entity.StudyRecordEntity
import com.example.pomostudy.data.local.entity.TagEntity
import com.example.pomostudy.data.local.entity.TodoEntity
import com.example.pomostudy.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 日历页 ViewModel：提供待办、学习记录、标签，并支持在日历里增删改当日待办。 */
class CalendarViewModel(private val container: AppContainer) : ViewModel() {

    val todos: StateFlow<List<TodoEntity>> = container.todoRepository.todos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val records: StateFlow<List<StudyRecordEntity>> = container.studyRepository.records
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val tags: StateFlow<List<TagEntity>> = container.tagRepository.tags
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(todo: TodoEntity) = viewModelScope.launch {
        if (todo.id == 0L) container.todoRepository.addTodo(todo) else container.todoRepository.update(todo)
    }

    fun toggle(todo: TodoEntity) = viewModelScope.launch {
        container.todoRepository.toggleCompleted(todo)
    }

    fun deleteInstance(todo: TodoEntity) = viewModelScope.launch {
        container.todoRepository.deleteInstance(todo)
    }

    fun deleteSeries(seriesId: Long) = viewModelScope.launch {
        container.todoRepository.deleteSeries(seriesId)
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { CalendarViewModel(container) }
        }
    }
}
