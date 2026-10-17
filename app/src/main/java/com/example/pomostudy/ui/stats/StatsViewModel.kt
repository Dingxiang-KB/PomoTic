package com.example.pomostudy.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.pomostudy.di.AppContainer
import com.example.pomostudy.ui.theme.TagColorPalette
import com.example.pomostudy.util.TimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 统计维度。 */
enum class StatsPeriod { DAY, WEEK, MONTH }

/** 饼图切片：某待办在统计区间内的学习时长。 */
data class StatSlice(val label: String, val colorLong: Long, val seconds: Long)

/** 一周单日数据点（周一..周日）。 */
data class DailyPoint(val label: String, val seconds: Long)

data class StatsUiState(
    val period: StatsPeriod = StatsPeriod.DAY,
    /** 回溯偏移：0 表示当前周期，-1 表示上一周期…… */
    val periodOffset: Int = 0,
    val rangeLabel: String = "",
    val isCurrent: Boolean = true,
    val totalSeconds: Long = 0L,
    val slices: List<StatSlice> = emptyList(),
    /** 当前自然周（周一..周日）每日学习时长，用于曲线图。 */
    val weekly: List<DailyPoint> = emptyList(),
    /** 本周期内设置了计划时长的待办数量。 */
    val plannedTodoCount: Int = 0,
    /** 本周期内计划学习总时长（秒）。 */
    val plannedSeconds: Long = 0L,
    /** 本周期内在“有计划待办”上实际投入的时长（秒）。 */
    val plannedActualSeconds: Long = 0L,
    val goalMinutes: Int = 120,
    val loading: Boolean = false
) {
    /** 是否展示学习曲线（仅日/周维度）。 */
    val showWeekly: Boolean get() = period != StatsPeriod.MONTH
}

/** 统计页 ViewModel：按日/周/月聚合各待办学习时长，支持回溯上一周期，并给出本周曲线。 */
class StatsViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(StatsUiState())
    val state: StateFlow<StatsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            container.settingsRepository.settings.collect { s ->
                _state.update { it.copy(goalMinutes = s.dailyGoalMinutes) }
            }
        }
        refresh()
    }

    fun setPeriod(period: StatsPeriod) {
        _state.update { it.copy(period = period, periodOffset = 0) }
        refresh()
    }

    /** 回溯：delta 为 -1 表示往前一个周期，+1 表示往后（不能超过当前）。 */
    fun moveOffset(delta: Int) {
        val next = (_state.value.periodOffset + delta).coerceAtMost(0)
        _state.update { it.copy(periodOffset = next) }
        refresh()
    }

    fun goToCurrent() {
        _state.update { it.copy(periodOffset = 0) }
        refresh()
    }

    /** 重新聚合统计数据（图表支持刷新）。 */
    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        val s = _state.value
        val now = System.currentTimeMillis()

        val anchor = when (s.period) {
            StatsPeriod.DAY -> TimeUtils.addDays(now, s.periodOffset)
            StatsPeriod.WEEK -> TimeUtils.addDays(now, s.periodOffset * 7)
            StatsPeriod.MONTH -> TimeUtils.addMonths(now, s.periodOffset)
        }
        val start: Long
        val end: Long
        val label: String
        when (s.period) {
            StatsPeriod.DAY -> {
                start = TimeUtils.startOfDay(anchor)
                end = TimeUtils.endOfDay(anchor)
                label = TimeUtils.formatDate(anchor)
            }
            StatsPeriod.WEEK -> {
                start = TimeUtils.startOfWeek(anchor)
                end = TimeUtils.endOfWeek(anchor)
                label = "${TimeUtils.formatDate(start)} ~ ${TimeUtils.formatDate(end)}"
            }
            StatsPeriod.MONTH -> {
                start = TimeUtils.startOfMonth(anchor)
                end = TimeUtils.endOfMonth(anchor)
                label = TimeUtils.formatYearMonth(anchor)
            }
        }

        val total = container.studyRepository.totalSecondsInRange(start, end)
        val byTodo = container.studyRepository.totalSecondsByTodo(start, end)
        val todos = container.todoRepository.todos.first()
        val tags = container.tagRepository.tags.first()
        val slices = byTodo.mapIndexed { index, d ->
            val todo = todos.firstOrNull { it.id == d.todoId }
            val color = todo?.tagId?.let { tid -> tags.firstOrNull { it.id == tid }?.color }
                ?: TagColorPalette[index % TagColorPalette.size]
            StatSlice(
                label = todo?.title ?: "未绑定待办",
                colorLong = color,
                seconds = d.totalSeconds
            )
        }

        // 计划进度：本周期内有设置计划时长的待办，及其实际投入时长
        val plannedTodos = todos.filter { t ->
            val d = t.dueDate
            t.plannedMinutes != null && d != null && d in start..end
        }
        val plannedSeconds = plannedTodos.sumOf { (it.plannedMinutes ?: 0).toLong() * 60L }
        val plannedIds = plannedTodos.map { it.id }.toSet()
        val plannedActual = byTodo
            .filter { it.todoId != null && it.todoId in plannedIds }
            .sumOf { it.totalSeconds }

        // 学习曲线：仅在“日/周”维度展示，且跟随所选周期（其所在周）与回溯偏移；月维度不展示
        val weekly: List<DailyPoint> = if (s.period == StatsPeriod.MONTH) {
            emptyList()
        } else {
            val weekStart = TimeUtils.startOfWeek(anchor)
            val weekEnd = TimeUtils.endOfWeek(anchor)
            val weekRecords = container.studyRepository.recordsInRange(weekStart, weekEnd)
            val daily = LongArray(7)
            weekRecords.forEach { r ->
                val idx = TimeUtils.dayIndexFromMonday(r.startTime)
                if (idx in 0..6) daily[idx] += r.durationSeconds
            }
            (0..6).map { i -> DailyPoint(TimeUtils.WEEKDAY_LABELS[i], daily[i]) }
        }

        _state.update {
            it.copy(
                totalSeconds = total,
                slices = slices,
                weekly = weekly,
                plannedTodoCount = plannedTodos.size,
                plannedSeconds = plannedSeconds,
                plannedActualSeconds = plannedActual,
                rangeLabel = label,
                isCurrent = s.periodOffset == 0,
                loading = false
            )
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { StatsViewModel(container) }
        }
    }
}
