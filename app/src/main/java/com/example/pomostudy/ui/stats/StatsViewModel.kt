package com.example.pomostudy.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.pomostudy.di.AppContainer
import com.example.pomostudy.util.TimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 统计维度。 */
enum class StatsPeriod { DAY, WEEK, MONTH }

/** 饼图切片：某标签在统计区间内的学习时长。 */
data class TagSlice(val label: String, val colorLong: Long, val seconds: Long)

data class StatsUiState(
    val period: StatsPeriod = StatsPeriod.DAY,
    val totalSeconds: Long = 0L,
    val slices: List<TagSlice> = emptyList(),
    val goalMinutes: Int = 120,
    val loading: Boolean = false
)

/** 统计页 ViewModel：按日/周/月聚合各标签学习时长，并展示每日目标进度。 */
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
        _state.update { it.copy(period = period) }
        refresh()
    }

    /** 重新聚合统计数据（图表支持刷新）。 */
    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        val now = System.currentTimeMillis()
        val (start, end) = when (_state.value.period) {
            StatsPeriod.DAY -> TimeUtils.startOfDay(now) to TimeUtils.endOfDay(now)
            StatsPeriod.WEEK -> TimeUtils.startOfWeek(now) to TimeUtils.endOfWeek(now)
            StatsPeriod.MONTH -> TimeUtils.startOfMonth(now) to TimeUtils.endOfMonth(now)
        }
        val total = container.studyRepository.totalSecondsInRange(start, end)
        val byTag = container.studyRepository.totalSecondsByTag(start, end)
        val tags = container.tagRepository.tags.first()
        val slices = byTag.map { d ->
            val tag = tags.firstOrNull { it.id == d.tagId }
            TagSlice(
                label = tag?.name ?: "未分类",
                colorLong = tag?.color ?: 0xFF9E9E9E,
                seconds = d.totalSeconds
            )
        }
        _state.update { it.copy(totalSeconds = total, slices = slices, loading = false) }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { StatsViewModel(container) }
        }
    }
}
