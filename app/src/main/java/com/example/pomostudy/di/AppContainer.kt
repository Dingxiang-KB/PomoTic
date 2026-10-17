package com.example.pomostudy.di

import android.content.Context
import androidx.room.withTransaction
import com.example.pomostudy.data.local.PomoDatabase
import com.example.pomostudy.data.local.entity.StudyRecordEntity
import com.example.pomostudy.data.local.entity.TagEntity
import com.example.pomostudy.data.local.entity.TodoEntity
import com.example.pomostudy.data.repository.SettingsRepository
import com.example.pomostudy.data.repository.StudyRepository
import com.example.pomostudy.data.repository.TagRepository
import com.example.pomostudy.data.repository.TodoRepository
import com.example.pomostudy.domain.model.Priority
import com.example.pomostudy.domain.model.RepeatRule
import com.example.pomostudy.service.TimerController
import org.json.JSONArray
import org.json.JSONObject

/**
 * 手动依赖注入容器（应用级单例）。
 * 统一提供数据库、各 Repository、计时控制器，避免在页面中直接 new 数据源。
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val database: PomoDatabase by lazy { PomoDatabase.get(appContext) }

    val tagRepository: TagRepository by lazy { TagRepository(database.tagDao()) }

    val todoRepository: TodoRepository by lazy { TodoRepository(database.todoDao()) }

    val studyRepository: StudyRepository by lazy { StudyRepository(database.studyRecordDao()) }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }

    /** 计时控制器为应用级单例，UI 与前台服务共享同一状态。 */
    val timerController: TimerController by lazy {
        TimerController(appContext, settingsRepository, studyRepository, todoRepository)
    }

    /** 清空所有本地数据（调试用）：学习记录、待办、标签，并重置计时与全局配置。 */
    suspend fun clearAllData() {
        database.withTransaction {
            database.studyRecordDao().deleteAll()
            database.todoDao().deleteAll()
            database.tagDao().deleteAll()
        }
        timerController.reset()
        settingsRepository.resetToDefaults()
    }

    /** 导出全部数据为可复制粘贴的 JSON 文本。 */
    suspend fun exportData(): String {
        val tags = database.tagDao().getAll()
        val todos = database.todoDao().getAll()
        val records = database.studyRecordDao().getAll()

        val root = JSONObject()
        root.put("app", "PomoStudy")
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val tagsArr = JSONArray()
        tags.forEach { t ->
            tagsArr.put(JSONObject().apply {
                put("id", t.id)
                put("name", t.name)
                put("color", t.color)
            })
        }
        root.put("tags", tagsArr)

        val todosArr = JSONArray()
        todos.forEach { t ->
            todosArr.put(JSONObject().apply {
                put("id", t.id)
                put("title", t.title)
                put("note", t.note)
                put("dueDate", t.dueDate ?: JSONObject.NULL)
                put("endDate", t.endDate ?: JSONObject.NULL)
                put("seriesId", t.seriesId ?: JSONObject.NULL)
                put("tagId", t.tagId ?: JSONObject.NULL)
                put("priority", t.priority.value)
                put("isCompleted", t.isCompleted)
                put("repeatRule", t.repeatRule.name)
                put("plannedMinutes", t.plannedMinutes ?: JSONObject.NULL)
                put("createdAt", t.createdAt)
            })
        }
        root.put("todos", todosArr)

        val recordsArr = JSONArray()
        records.forEach { r ->
            recordsArr.put(JSONObject().apply {
                put("id", r.id)
                put("startTime", r.startTime)
                put("endTime", r.endTime)
                put("durationSeconds", r.durationSeconds)
                put("tagId", r.tagId ?: JSONObject.NULL)
                put("todoId", r.todoId ?: JSONObject.NULL)
                put("note", r.note)
                put("mode", r.mode)
            })
        }
        root.put("records", recordsArr)

        return root.toString(2)
    }

    /**
     * 从 JSON 文本导入全部数据（整体覆盖现有数据）。
     * 解析失败会抛出异常，由调用方提示。
     */
    suspend fun importData(json: String) {
        val root = JSONObject(json)
        val tags = root.optJSONArray("tags") ?: JSONArray()
        val todos = root.optJSONArray("todos") ?: JSONArray()
        val records = root.optJSONArray("records") ?: JSONArray()

        database.withTransaction {
            database.studyRecordDao().deleteAll()
            database.todoDao().deleteAll()
            database.tagDao().deleteAll()

            for (i in 0 until tags.length()) {
                val o = tags.getJSONObject(i)
                database.tagDao().insert(
                    TagEntity(
                        id = o.getLong("id"),
                        name = o.optString("name"),
                        color = o.optLong("color", 0xFF9E9E9EL)
                    )
                )
            }
            for (i in 0 until todos.length()) {
                val o = todos.getJSONObject(i)
                database.todoDao().insert(
                    TodoEntity(
                        id = o.getLong("id"),
                        title = o.optString("title"),
                        note = o.optString("note", ""),
                        dueDate = o.longOrNull("dueDate"),
                        endDate = o.longOrNull("endDate"),
                        seriesId = o.longOrNull("seriesId"),
                        tagId = o.longOrNull("tagId"),
                        priority = Priority.fromValue(o.optInt("priority", 1)),
                        isCompleted = o.optBoolean("isCompleted", false),
                        repeatRule = runCatching { RepeatRule.valueOf(o.optString("repeatRule", "NONE")) }
                            .getOrDefault(RepeatRule.NONE),
                        plannedMinutes = o.intOrNull("plannedMinutes"),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
            for (i in 0 until records.length()) {
                val o = records.getJSONObject(i)
                database.studyRecordDao().insert(
                    StudyRecordEntity(
                        id = o.getLong("id"),
                        startTime = o.optLong("startTime"),
                        endTime = o.optLong("endTime"),
                        durationSeconds = o.optLong("durationSeconds"),
                        tagId = o.longOrNull("tagId"),
                        todoId = o.longOrNull("todoId"),
                        note = o.optString("note", ""),
                        mode = o.optString("mode", "POMODORO")
                    )
                )
            }
        }
    }
}

private fun JSONObject.longOrNull(key: String): Long? = if (isNull(key)) null else optLong(key)

private fun JSONObject.intOrNull(key: String): Int? = if (isNull(key)) null else optInt(key)
