package com.example.pomostudy.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.pomostudy.data.local.entity.StudyRecordEntity
import com.example.pomostudy.data.local.entity.TagEntity
import com.example.pomostudy.data.local.entity.TodoEntity
import com.example.pomostudy.di.AppContainer
import com.example.pomostudy.ui.theme.TagColorPalette
import com.example.pomostudy.util.TimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** 统计维度。 */
enum class StatsPeriod { DAY, WEEK, MONTH }

/** 饼图切片：某待办在统计区间内的学习时长。 */
data class StatSlice(val label: String, val colorLong: Long, val seconds: Long)

/** 一周单日数据点（周一..周日）。 */
data class DailyPoint(val label: String, val seconds: Long)

/** 单个「有计划」待办的完成情况。 */
data class PlannedProgress(
    val title: String,
    val plannedSeconds: Long,
    val actualSeconds: Long
)

data class StatsUiState(
    val period: StatsPeriod = StatsPeriod.DAY,
    /** 回溯偏移：0 表示当前周期，-1 表示上一周期…… */
    val periodOffset: Int = 0,
    val rangeLabel: String = "",
    val isCurrent: Boolean = true,
    val totalSeconds: Long = 0L,
    val slices: List<StatSlice> = emptyList(),
    /** 当前所看周期所在自然周（周一..周日）每日学习时长，用于曲线图。 */
    val weekly: List<DailyPoint> = emptyList(),
    /** 本周期内设置了计划时长的待办数量。 */
    val plannedTodoCount: Int = 0,
    /** 本周期内计划学习总时长（秒）。 */
    val plannedSeconds: Long = 0L,
    /** 本周期内在“有计划待办”上实际投入的时长（秒）。 */
    val plannedActualSeconds: Long = 0L,
    /** 本周期内各「有计划」待办的完成明细。 */
    val plannedItems: List<PlannedProgress> = emptyList(),
    /** 周期区间展示：周维度用两段（中间灰色横线），其余用 rangeLabel。 */
    val rangeStartLabel: String = "",
    val rangeEndLabel: String? = null,
    val goalMinutes: Int = 120,
    val loading: Boolean = false
) {
    /** 是否展示学习曲线（仅日/周维度）。 */
    val showWeekly: Boolean get() = period != StatsPeriod.MONTH
}

/** 统计计算所需的全部输入（来自数据库 Flow）。 */
private data class StatsInputs(
    val period: StatsPeriod,
    val offset: Int,
    val records: List<StudyRecordEntity>,
    val todos: List<TodoEntity>,
    val tags: List<TagEntity>
)

/**
 * 统计页 ViewModel：**响应式**聚合。
 * 直接订阅学习记录/待办/标签的 Flow，任何数据变化（新增计时、勾选完成等）都会自动刷新，
 * 无需手动点“刷新”。周期切换与回溯仅改变查询区间。
 */
class StatsViewModel(container: AppContainer) : ViewModel() {

    private val period = MutableStateFlow(StatsPeriod.DAY)
    private val offset = MutableStateFlow(0)
    private val revision = MutableStateFlow(0)

    private val inputs = combine(
        period,
        offset,
        container.studyRepository.records,
        container.todoRepository.todos,
        container.tagRepository.tags
    ) { p, off, records, todos, tags ->
        StatsInputs(p, off, records, todos, tags)
    }

    val state: StateFlow<StatsUiState> = combine(inputs, revision) { inp, _ -> compute(inp) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())

    fun setPeriod(p: StatsPeriod) {
        period.value = p
        offset.value = 0
    }

    fun moveOffset(delta: Int) {
        offset.value = (offset.value + delta).coerceAtMost(0)
    }

    fun goToCurrent() {
        offset.value = 0
    }

    /** 保留“刷新”入口（数据已响应式，这里强制重算一次）。 */
    fun refresh() {
        revision.value++
    }

    private fun compute(inp: StatsInputs): StatsUiState {
        val p = inp.period
        val now = System.currentTimeMillis()
        val anchor = when (p) {
            StatsPeriod.DAY -> TimeUtils.addDays(now, inp.offset)
            StatsPeriod.WEEK -> TimeUtils.addDays(now, inp.offset * 7)
            StatsPeriod.MONTH -> TimeUtils.addMonths(now, inp.offset)
        }

        val start: Long
        val end: Long
        val label: String
        when (p) {
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

        val inRange = inp.records.filter { it.startTime in start..end }
        val total = inRange.sumOf { it.durationSeconds }

        // 按待办聚合
        val byTodo = inRange.groupBy { it.todoId }
            .map { (todoId, recs) -> todoId to recs.sumOf { it.durationSeconds } }
            .sortedByDescending { it.second }
        val todoMap = inp.todos.associateBy { it.id }
        val slices = byTodo.mapIndexed { index, (todoId, secs) ->
            val todo = todoId?.let { todoMap[it] }
            val color = todo?.tagId?.let { tid -> inp.tags.firstOrNull { it.id == tid }?.color }
                ?: TagColorPalette[index % TagColorPalette.size]
            StatSlice(
                label = todo?.title ?: "未绑定待办",
                colorLong = color,
                seconds = secs
            )
        }

        // 计划进度
        val actualByTodo = byTodo.toMap()
        val plannedTodos = inp.todos.filter { t ->
            val d = t.dueDate
            t.plannedMinutes != null && d != null && d in start..end
        }
        val plannedItems = plannedTodos.map { t ->
            PlannedProgress(
                title = t.title,
                plannedSeconds = (t.plannedMinutes ?: 0).toLong() * 60L,
                actualSeconds = actualByTodo[t.id] ?: 0L
            )
        }
        val plannedSeconds = plannedItems.sumOf { it.plannedSeconds }
        val plannedActual = plannedItems.sumOf { it.actualSeconds }

        // 学习曲线（仅日/周）：所选周期所在自然周
        val weekly: List<DailyPoint> = if (p == StatsPeriod.MONTH) {
            emptyList()
        } else {
            val weekStart = TimeUtils.startOfWeek(anchor)
            val weekEnd = TimeUtils.endOfWeek(anchor)
            val daily = LongArray(7)
            inp.records.filter { it.startTime in weekStart..weekEnd }.forEach { r ->
                val idx = TimeUtils.dayIndexFromMonday(r.startTime)
                if (idx in 0..6) daily[idx] += r.durationSeconds
            }
            (0..6).map { DailyPoint(TimeUtils.WEEKDAY_LABELS[it], daily[it]) }
        }

        val rangeEndLabel: String? =
            if (p == StatsPeriod.WEEK) TimeUtils.formatDate(end).takeLast(5) else null
        val rangeStartLabel: String =
            if (p == StatsPeriod.WEEK) TimeUtils.formatDate(start).takeLast(5) else label

        return StatsUiState(
            period = p,
            periodOffset = inp.offset,
            rangeLabel = label,
            isCurrent = inp.offset == 0,
            totalSeconds = total,
            slices = slices,
            weekly = weekly,
            plannedTodoCount = plannedTodos.size,
            plannedSeconds = plannedSeconds,
            plannedActualSeconds = plannedActual,
            plannedItems = plannedItems,
            rangeStartLabel = rangeStartLabel,
            rangeEndLabel = rangeEndLabel
        )
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { StatsViewModel(container) }
        }
    }
}
