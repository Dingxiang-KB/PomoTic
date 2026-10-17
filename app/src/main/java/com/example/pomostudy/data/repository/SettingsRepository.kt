package com.example.pomostudy.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.pomostudy.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** 全局配置（默认番茄/休息时长、主题模式、每日学习目标等）。 */
data class AppSettings(
    val workMinutes: Int = 25,
    val breakMinutes: Int = 5,
    val dailyGoalMinutes: Int = 120,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)

/** 计时会话快照，用于进程被系统回收后恢复计时。 */
data class TimerSnapshot(
    val mode: String,
    val phase: String,
    val status: String,
    val totalSeconds: Long,
    val accumulatedSeconds: Long,
    val segmentStartMillis: Long,
    val startTimeMillis: Long,
    val tagId: Long?,
    val todoId: Long?,
    val note: String,
    val autoCompleteTodo: Boolean
)

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "pomo_settings")

/**
 * 基于 DataStore(Preferences) 的配置仓库：
 * - 全局配置：工作时长、休息时长、每日目标、主题模式。
 * - 计时快照：随时持久化正在进行的计时，用于后台恢复。
 */
class SettingsRepository(context: Context) {

    private val store = context.applicationContext.settingsDataStore

    private object Keys {
        val WORK = intPreferencesKey("work_minutes")
        val BREAK = intPreferencesKey("break_minutes")
        val GOAL = intPreferencesKey("daily_goal_minutes")
        val THEME = stringPreferencesKey("theme_mode")

        val T_ACTIVE = booleanPreferencesKey("timer_active")
        val T_MODE = stringPreferencesKey("timer_mode")
        val T_PHASE = stringPreferencesKey("timer_phase")
        val T_STATUS = stringPreferencesKey("timer_status")
        val T_TOTAL = longPreferencesKey("timer_total")
        val T_ACCUM = longPreferencesKey("timer_accumulated")
        val T_SEGMENT_START = longPreferencesKey("timer_segment_start")
        val T_START_TIME = longPreferencesKey("timer_start_time")
        val T_TAG_ID = longPreferencesKey("timer_tag_id")
        val T_TODO_ID = longPreferencesKey("timer_todo_id")
        val T_NOTE = stringPreferencesKey("timer_note")
        val T_AUTO = booleanPreferencesKey("timer_auto_complete")
    }

    val settings: Flow<AppSettings> = store.data.map { p ->
        AppSettings(
            workMinutes = p[Keys.WORK] ?: 25,
            breakMinutes = p[Keys.BREAK] ?: 5,
            dailyGoalMinutes = p[Keys.GOAL] ?: 120,
            themeMode = ThemeMode.fromName(p[Keys.THEME])
        )
    }

    suspend fun setWorkMinutes(minutes: Int) = store.edit { it[Keys.WORK] = minutes.coerceIn(1, 180) }

    suspend fun setBreakMinutes(minutes: Int) = store.edit { it[Keys.BREAK] = minutes.coerceIn(1, 60) }

    suspend fun setDailyGoalMinutes(minutes: Int) = store.edit { it[Keys.GOAL] = minutes.coerceIn(1, 1440) }

    suspend fun setThemeMode(mode: ThemeMode) = store.edit { it[Keys.THEME] = mode.name }

    suspend fun saveTimerSnapshot(snapshot: TimerSnapshot) = store.edit { p ->
        p[Keys.T_ACTIVE] = true
        p[Keys.T_MODE] = snapshot.mode
        p[Keys.T_PHASE] = snapshot.phase
        p[Keys.T_STATUS] = snapshot.status
        p[Keys.T_TOTAL] = snapshot.totalSeconds
        p[Keys.T_ACCUM] = snapshot.accumulatedSeconds
        p[Keys.T_SEGMENT_START] = snapshot.segmentStartMillis
        p[Keys.T_START_TIME] = snapshot.startTimeMillis
        p[Keys.T_TAG_ID] = snapshot.tagId ?: -1L
        p[Keys.T_TODO_ID] = snapshot.todoId ?: -1L
        p[Keys.T_NOTE] = snapshot.note
        p[Keys.T_AUTO] = snapshot.autoCompleteTodo
    }

    suspend fun loadTimerSnapshot(): TimerSnapshot? {
        val p = store.data.first()
        if (p[Keys.T_ACTIVE] != true) return null
        fun idOrNull(key: androidx.datastore.preferences.core.Preferences.Key<Long>): Long? =
            (p[key] ?: -1L).takeIf { it >= 0 }
        return TimerSnapshot(
            mode = p[Keys.T_MODE] ?: "POMODORO",
            phase = p[Keys.T_PHASE] ?: "WORK",
            status = p[Keys.T_STATUS] ?: "IDLE",
            totalSeconds = p[Keys.T_TOTAL] ?: 0L,
            accumulatedSeconds = p[Keys.T_ACCUM] ?: 0L,
            segmentStartMillis = p[Keys.T_SEGMENT_START] ?: 0L,
            startTimeMillis = p[Keys.T_START_TIME] ?: System.currentTimeMillis(),
            tagId = idOrNull(Keys.T_TAG_ID),
            todoId = idOrNull(Keys.T_TODO_ID),
            note = p[Keys.T_NOTE] ?: "",
            autoCompleteTodo = p[Keys.T_AUTO] ?: false
        )
    }

    suspend fun clearTimerSnapshot() = store.edit { p ->
        p[Keys.T_ACTIVE] = false
    }
}
