package com.example.pomostudy.service

import android.content.Context
import com.example.pomostudy.data.local.entity.StudyRecordEntity
import com.example.pomostudy.data.repository.SettingsRepository
import com.example.pomostudy.data.repository.StudyRepository
import com.example.pomostudy.data.repository.TimerSnapshot
import com.example.pomostudy.data.repository.TodoRepository
import com.example.pomostudy.domain.model.TimerMode
import com.example.pomostudy.domain.model.TimerPhase
import com.example.pomostudy.domain.model.TimerStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 计时状态（供 UI 与前台服务共享）。
 * 显示时间、进度均由墙钟时间推算，避免进程被回收后丢时。
 */
data class TimerUiState(
    val mode: TimerMode = TimerMode.POMODORO,
    val phase: TimerPhase = TimerPhase.WORK,
    val status: TimerStatus = TimerStatus.IDLE,
    val totalSeconds: Long = 0L,
    val elapsedSeconds: Long = 0L,
    val workMinutes: Int = 25,
    val breakMinutes: Int = 5,
    val tagId: Long? = null,
    val boundTodoId: Long? = null,
    val boundTodoTitle: String? = null,
    val note: String = "",
    val autoCompleteTodo: Boolean = false
) {
    val isActive: Boolean
        get() = status == TimerStatus.RUNNING || status == TimerStatus.PAUSED

    val remainingSeconds: Long
        get() = (totalSeconds - elapsedSeconds).coerceAtLeast(0L)

    /** 界面展示用的秒数：番茄模式显示剩余，自由模式显示已用。 */
    val displaySeconds: Long
        get() = if (mode == TimerMode.POMODORO) remainingSeconds else elapsedSeconds

    val progress: Float
        get() = when {
            mode == TimerMode.FREE -> 0f
            totalSeconds <= 0L -> 0f
            else -> (elapsedSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
        }
}

/**
 * 计时控制器：应用级单例，负责计时状态机、墙钟推算、持久化快照、结束写记录与通知。
 *
 * 计时在 [TimerService] 前台服务存活期间持续运行；本控制器通过墙钟计算已用时长，
 * 因此即使进程被系统回收，也能在下次启动时恢复或补算。
 */
class TimerController(
    context: Context,
    private val settingsRepository: SettingsRepository,
    private val studyRepository: StudyRepository,
    private val todoRepository: TodoRepository
) {
    private val appContext = context.applicationContext
    private val notificationHelper = NotificationHelper(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(TimerUiState())
    val state: StateFlow<TimerUiState> = _state.asStateFlow()

    private var tickJob: Job? = null

    /** 当前运行段的开始时刻（暂停时为 0）。 */
    private var segmentStartMillis: Long = 0L

    /** 暂停前累计的秒数。 */
    private var accumulatedSeconds: Long = 0L

    /** 本次会话的开始时刻（用于写记录）。 */
    private var sessionStartMillis: Long = 0L

    init {
        notificationHelper.createChannels()
        // 同步默认工作时长 / 休息时长
        scope.launch {
            settingsRepository.settings.collect { s ->
                _state.update { cur ->
                    if (cur.isActive) cur
                    else cur.copy(workMinutes = s.workMinutes, breakMinutes = s.breakMinutes)
                        .let { recalcTotal(it) }
                }
            }
        }
        // 恢复未完成的计时
        scope.launch { restoreSession() }
    }

    private fun recalcTotal(s: TimerUiState): TimerUiState {
        if (s.isActive) return s
        val total = when {
            s.mode == TimerMode.FREE -> 0L
            s.phase == TimerPhase.WORK -> s.workMinutes * 60L
            else -> s.breakMinutes * 60L
        }
        return s.copy(totalSeconds = total)
    }

    private fun currentElapsedSeconds(): Long {
        val s = _state.value
        return if (s.status == TimerStatus.RUNNING && segmentStartMillis > 0L) {
            accumulatedSeconds + (System.currentTimeMillis() - segmentStartMillis) / 1000L
        } else {
            accumulatedSeconds
        }
    }

    // ---------------- 配置类操作（仅空闲时允许） ----------------

    fun setMode(mode: TimerMode) {
        if (_state.value.isActive) return
        _state.update { recalcTotal(it.copy(mode = mode, elapsedSeconds = 0L, totalSeconds = 0L)) }
    }

    fun setPhase(phase: TimerPhase) {
        if (_state.value.isActive) return
        _state.update { recalcTotal(it.copy(phase = phase, elapsedSeconds = 0L, totalSeconds = 0L)) }
    }

    fun setTag(tagId: Long?) = _state.update { it.copy(tagId = tagId) }

    fun setNote(note: String) = _state.update { it.copy(note = note) }

    fun setBoundTodo(todoId: Long?, title: String?) =
        _state.update { it.copy(boundTodoId = todoId, boundTodoTitle = title) }

    fun setAutoComplete(value: Boolean) = _state.update { it.copy(autoCompleteTodo = value) }

    // ---------------- 计时操作 ----------------

    fun start() {
        if (_state.value.isActive) return
        val s = _state.value
        val total = when {
            s.mode == TimerMode.FREE -> 0L
            s.phase == TimerPhase.WORK -> s.workMinutes * 60L
            else -> s.breakMinutes * 60L
        }
        accumulatedSeconds = 0L
        segmentStartMillis = System.currentTimeMillis()
        sessionStartMillis = segmentStartMillis
        _state.update { it.copy(status = TimerStatus.RUNNING, totalSeconds = total, elapsedSeconds = 0L) }
        startTicker()
        persist()
    }

    fun pause() {
        if (_state.value.status != TimerStatus.RUNNING) return
        accumulatedSeconds = currentElapsedSeconds()
        segmentStartMillis = 0L
        stopTicker()
        _state.update { it.copy(status = TimerStatus.PAUSED, elapsedSeconds = accumulatedSeconds) }
        persist()
    }

    fun resume() {
        if (_state.value.status != TimerStatus.PAUSED) return
        segmentStartMillis = System.currentTimeMillis()
        _state.update { it.copy(status = TimerStatus.RUNNING) }
        startTicker()
        persist()
    }

    /** 重置：清空当前计时，回到空闲状态。 */
    fun reset() {
        stopTicker()
        accumulatedSeconds = 0L
        segmentStartMillis = 0L
        sessionStartMillis = 0L
        _state.update { recalcTotal(it.copy(status = TimerStatus.IDLE, elapsedSeconds = 0L, totalSeconds = 0L)) }
        scope.launch { settingsRepository.clearTimerSnapshot() }
    }

    /** 手动结束计时（写入记录）。 */
    fun finish() {
        val s = _state.value
        if (!s.isActive) return
        val elapsed = currentElapsedSeconds()
        stopTicker()
        accumulatedSeconds = elapsed
        segmentStartMillis = 0L
        endSession(elapsed)
    }

    private fun startTicker() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (isActive) {
                val elapsed = currentElapsedSeconds()
                _state.update { it.copy(elapsedSeconds = elapsed) }
                val s = _state.value
                if (s.mode == TimerMode.POMODORO && s.totalSeconds > 0L && elapsed >= s.totalSeconds) {
                    stopTicker()
                    accumulatedSeconds = s.totalSeconds
                    segmentStartMillis = 0L
                    endSession(s.totalSeconds)
                    break
                }
                delay(500L)
            }
        }
    }

    private fun stopTicker() {
        tickJob?.cancel()
        tickJob = null
    }

    /**
     * 结束一次计时会话：
     * 1) 工作阶段且时长有效时写入学习记录；
     * 2) 若开启“自动完成待办”，标记绑定待办完成；
     * 3) 发送系统通知；
     * 4) 回到空闲并清除快照。
     */
    private fun endSession(durationSeconds: Long) {
        val s = _state.value
        val isWork = s.mode == TimerMode.FREE || s.phase == TimerPhase.WORK
        val endTime = System.currentTimeMillis()
        val startTime = if (sessionStartMillis > 0L) sessionStartMillis else endTime - durationSeconds * 1000L

        if (isWork && durationSeconds >= 1L) {
            scope.launch {
                studyRepository.add(
                    StudyRecordEntity(
                        startTime = startTime,
                        endTime = endTime,
                        durationSeconds = durationSeconds,
                        tagId = s.tagId,
                        todoId = s.boundTodoId,
                        note = s.note,
                        mode = s.mode.name
                    )
                )
                val todoId = s.boundTodoId
                if (s.autoCompleteTodo && todoId != null) {
                    val todo = todoRepository.getById(todoId)
                    if (todo != null && !todo.isCompleted) {
                        todoRepository.toggleCompleted(todo)
                    }
                }
            }
        }

        // 通知：工作结束 / 休息结束
        if (durationSeconds >= 1L) {
            val title: String
            val text: String
            if (s.mode == TimerMode.FREE) {
                title = "学习计时结束"
                text = "本次自由学习 ${formatMinutes(durationSeconds)}"
            } else if (s.phase == TimerPhase.WORK) {
                title = "番茄计时结束"
                text = "本次专注 ${formatMinutes(durationSeconds)}，休息一下吧"
            } else {
                title = "休息结束"
                text = "休息时间到，继续加油！"
            }
            notificationHelper.notifySessionFinished(title, text)
        }

        _state.update {
            recalcTotal(it.copy(status = TimerStatus.IDLE, elapsedSeconds = 0L, totalSeconds = 0L))
        }
        scope.launch { settingsRepository.clearTimerSnapshot() }
    }

    private fun formatMinutes(seconds: Long): String {
        val m = seconds / 60
        val s = seconds % 60
        return if (m > 0) "$m 分 $s 秒" else "$s 秒"
    }

    // ---------------- 持久化与恢复 ----------------

    private fun persist() {
        val s = _state.value
        val snapshot = TimerSnapshot(
            mode = s.mode.name,
            phase = s.phase.name,
            status = s.status.name,
            totalSeconds = s.totalSeconds,
            accumulatedSeconds = currentElapsedSeconds(),
            segmentStartMillis = segmentStartMillis,
            startTimeMillis = sessionStartMillis,
            tagId = s.tagId,
            todoId = s.boundTodoId,
            note = s.note,
            autoCompleteTodo = s.autoCompleteTodo
        )
        scope.launch { settingsRepository.saveTimerSnapshot(snapshot) }
    }

    private suspend fun restoreSession() {
        val snap = settingsRepository.loadTimerSnapshot() ?: return
        val mode = runCatching { TimerMode.valueOf(snap.mode) }.getOrDefault(TimerMode.POMODORO)
        val phase = runCatching { TimerPhase.valueOf(snap.phase) }.getOrDefault(TimerPhase.WORK)
        val status = runCatching { TimerStatus.valueOf(snap.status) }.getOrDefault(TimerStatus.IDLE)
        if (status == TimerStatus.IDLE) return

        accumulatedSeconds = snap.accumulatedSeconds
        segmentStartMillis = snap.segmentStartMillis
        sessionStartMillis = snap.startTimeMillis

        _state.update {
            it.copy(
                mode = mode,
                phase = phase,
                status = status,
                totalSeconds = snap.totalSeconds,
                elapsedSeconds = snap.accumulatedSeconds,
                tagId = snap.tagId,
                boundTodoId = snap.todoId,
                note = snap.note,
                autoCompleteTodo = snap.autoCompleteTodo
            )
        }
        snap.todoId?.let { id ->
            todoRepository.getById(id)?.let { t ->
                _state.update { s -> s.copy(boundTodoTitle = t.title) }
            }
        }

        if (status == TimerStatus.RUNNING) {
            val elapsed = currentElapsedSeconds()
            _state.update { it.copy(elapsedSeconds = elapsed) }
            if (mode == TimerMode.POMODORO && snap.totalSeconds > 0L && elapsed >= snap.totalSeconds) {
                accumulatedSeconds = snap.totalSeconds
                segmentStartMillis = 0L
                endSession(snap.totalSeconds)
            } else {
                startTicker()
            }
        }
    }
}
