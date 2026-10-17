package com.example.pomostudy.ui.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.pomostudy.data.local.entity.TagEntity
import com.example.pomostudy.data.local.entity.TodoEntity
import com.example.pomostudy.di.AppContainer
import com.example.pomostudy.domain.model.Priority
import com.example.pomostudy.util.TimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 完成状态筛选。 */
enum class CompletionFilter { ALL, ACTIVE, DONE }

/**
 * 待办筛选条件。
 * [selectedDay] 为当前查看的某一天（当天 00:00 时间戳），null 表示“全部”。
 */
data class TodoFilters(
    val priorities: Set<Priority> = emptySet(),
    val tagId: Long? = null,
    val completion: CompletionFilter = CompletionFilter.ALL,
    val selectedDay: Long? = TimeUtils.startOfDay(System.currentTimeMillis())
)

/** 待办页 ViewModel：按“当周日期条 + 折叠筛选”过滤，支持增删改与每日重复删除。 */
class TodoViewModel(private val container: AppContainer) : ViewModel() {

    private val _filters = MutableStateFlow(TodoFilters())
    val filters: StateFlow<TodoFilters> = _filters.asStateFlow()

    /** 周偏移：0 为本周，-1 为上周，+1 为下周。 */
    private val _weekOffset = MutableStateFlow(0)
    val weekOffset: StateFlow<Int> = _weekOffset.asStateFlow()

    val tags: StateFlow<List<TagEntity>> = container.tagRepository.tags
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val filtered: StateFlow<List<TodoEntity>> =
        combine(container.todoRepository.todos, _filters) { list, f -> applyFilters(list, f) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun applyFilters(list: List<TodoEntity>, f: TodoFilters): List<TodoEntity> {
        return list.filter { todo ->
            val completionOk = when (f.completion) {
                CompletionFilter.ALL -> true
                CompletionFilter.ACTIVE -> !todo.isCompleted
                CompletionFilter.DONE -> todo.isCompleted
            }
            val priorityOk = f.priorities.isEmpty() || todo.priority in f.priorities
            val tagOk = f.tagId == null || todo.tagId == f.tagId
            val dateOk = f.selectedDay == null || todo.dueDate == f.selectedDay
            completionOk && priorityOk && tagOk && dateOk
        }
    }

    fun togglePriorityFilter(priority: Priority) {
        _filters.update { f ->
            val set = if (priority in f.priorities) f.priorities - priority else f.priorities + priority
            f.copy(priorities = set)
        }
    }

    fun setTagFilter(tagId: Long?) = _filters.update { it.copy(tagId = tagId) }

    fun setCompletionFilter(value: CompletionFilter) = _filters.update { it.copy(completion = value) }

    /** 选择某一天查看（传 null 表示查看全部）。 */
    fun selectDay(day: Long?) = _filters.update { it.copy(selectedDay = day) }

    /** 左右滑动切换上一周 / 下一周，选中日期随周平移。 */
    fun shiftWeek(delta: Int) {
        _weekOffset.update { it + delta }
        _filters.update { f ->
            f.selectedDay?.let { day -> f.copy(selectedDay = TimeUtils.addDays(day, delta * 7)) } ?: f
        }
    }

    fun save(todo: TodoEntity) = viewModelScope.launch {
        if (todo.id == 0L) container.todoRepository.addTodo(todo) else container.todoRepository.update(todo)
    }

    fun toggle(todo: TodoEntity) = viewModelScope.launch {
        container.todoRepository.toggleCompleted(todo)
    }

    /** 删除当天这一条实例。 */
    fun deleteInstance(todo: TodoEntity) = viewModelScope.launch {
        container.todoRepository.deleteInstance(todo)
    }

    /** 删除该每日重复系列的全部实例。 */
    fun deleteSeries(seriesId: Long) = viewModelScope.launch {
        container.todoRepository.deleteSeries(seriesId)
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { TodoViewModel(container) }
        }
    }
}
