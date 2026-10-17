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
import com.example.pomostudy.domain.model.RepeatRule
import com.example.pomostudy.util.TimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 完成状态筛选。 */
enum class CompletionFilter { ALL, ACTIVE, DONE }

/** 日期筛选。 */
enum class DateFilter { ALL, TODAY, WEEK }

/** 待办筛选条件。priorities 为空表示不限优先级。 */
data class TodoFilters(
    val priorities: Set<Priority> = emptySet(),
    val tagId: Long? = null,
    val completion: CompletionFilter = CompletionFilter.ALL,
    val date: DateFilter = DateFilter.ALL
)

/** 待办页 ViewModel：加载待办、执行筛选与增删改。 */
class TodoViewModel(private val container: AppContainer) : ViewModel() {

    private val _filters = MutableStateFlow(TodoFilters())
    val filters: StateFlow<TodoFilters> = _filters.asStateFlow()

    init {
        // 进入待办页时，处理“顺延到次日”的过期未完成待办
        viewModelScope.launch { container.todoRepository.carryOverOverdueTodos() }
    }

    val tags: StateFlow<List<TagEntity>> = container.tagRepository.tags
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val filtered: StateFlow<List<TodoEntity>> =
        combine(container.todoRepository.todos, _filters) { list, f -> applyFilters(list, f) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun applyFilters(list: List<TodoEntity>, f: TodoFilters): List<TodoEntity> {
        val now = System.currentTimeMillis()
        val dayStart = TimeUtils.startOfDay(now)
        val dayEnd = TimeUtils.endOfDay(now)
        val weekStart = TimeUtils.startOfWeek(now)
        val weekEnd = TimeUtils.endOfWeek(now)
        return list.filter { todo ->
            val completionOk = when (f.completion) {
                CompletionFilter.ALL -> true
                CompletionFilter.ACTIVE -> !todo.isCompleted
                CompletionFilter.DONE -> todo.isCompleted
            }
            val priorityOk = f.priorities.isEmpty() || todo.priority in f.priorities
            val tagOk = f.tagId == null || todo.tagId == f.tagId
            val dateOk = when (f.date) {
                DateFilter.ALL -> true
                DateFilter.TODAY -> todo.dueDate?.let { it in dayStart..dayEnd } == true
                DateFilter.WEEK -> todo.dueDate?.let { it in weekStart..weekEnd } == true
            }
            completionOk && priorityOk && tagOk && dateOk
        }
    }

    fun togglePriorityFilter(priority: Priority) {
        _filters.value = _filters.value.let { f ->
            val set = if (priority in f.priorities) f.priorities - priority else f.priorities + priority
            f.copy(priorities = set)
        }
    }

    fun setTagFilter(tagId: Long?) {
        _filters.value = _filters.value.copy(tagId = tagId)
    }

    fun setCompletionFilter(value: CompletionFilter) {
        _filters.value = _filters.value.copy(completion = value)
    }

    fun setDateFilter(value: DateFilter) {
        _filters.value = _filters.value.copy(date = value)
    }

    fun add(
        title: String,
        note: String,
        dueDate: Long?,
        tagId: Long?,
        priority: Priority,
        repeatRule: RepeatRule,
        carryOver: Boolean
    ) = viewModelScope.launch {
        container.todoRepository.add(
            TodoEntity(
                title = title,
                note = note,
                dueDate = dueDate,
                tagId = tagId,
                priority = priority,
                repeatRule = repeatRule,
                carryOver = carryOver
            )
        )
    }

    fun update(todo: TodoEntity) = viewModelScope.launch {
        container.todoRepository.update(todo)
    }

    fun delete(todo: TodoEntity) = viewModelScope.launch {
        container.todoRepository.delete(todo)
    }

    fun toggle(todo: TodoEntity) = viewModelScope.launch {
        container.todoRepository.toggleCompleted(todo)
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { TodoViewModel(container) }
        }
    }
}
