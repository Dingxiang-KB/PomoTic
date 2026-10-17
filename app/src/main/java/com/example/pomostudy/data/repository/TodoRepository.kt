package com.example.pomostudy.data.repository

import com.example.pomostudy.data.local.dao.TodoDao
import com.example.pomostudy.data.local.entity.TodoEntity
import com.example.pomostudy.domain.model.RepeatRule
import com.example.pomostudy.util.TimeUtils
import kotlinx.coroutines.flow.Flow

/** 待办数据仓库：统一管理待办的增删改查与每日重复逻辑。 */
class TodoRepository(private val todoDao: TodoDao) {

    val todos: Flow<List<TodoEntity>> = todoDao.observeAll()

    /**
     * 新增待办。若为每日重复，则赋予一个新的系列 ID（取首条实例的 id）。
     */
    suspend fun addTodo(todo: TodoEntity): Long {
        val insertEntity =
            if (todo.repeatRule == RepeatRule.DAILY && todo.seriesId == null) {
                todo.copy(seriesId = null)
            } else {
                todo
            }
        val id = todoDao.insert(insertEntity)
        if (todo.repeatRule == RepeatRule.DAILY && todo.seriesId == null) {
            todoDao.setSeriesId(id, id)
        }
        return id
    }

    suspend fun update(todo: TodoEntity) {
        // 由“一次性/其他”切换为“每日重复”且尚无系列时，补上系列 ID
        val entity =
            if (todo.repeatRule == RepeatRule.DAILY && todo.seriesId == null) {
                todo.copy(seriesId = todo.id)
            } else {
                todo
            }
        todoDao.update(entity)
    }

    suspend fun delete(todo: TodoEntity) = todoDao.delete(todo)

    /** 仅删除某一条实例（“删除当日”）。 */
    suspend fun deleteInstance(todo: TodoEntity) = todoDao.delete(todo)

    /** 删除整个每日重复系列（“删除全部”）。 */
    suspend fun deleteSeries(seriesId: Long) = todoDao.deleteSeries(seriesId)

    suspend fun getById(id: Long): TodoEntity? = todoDao.getById(id)

    suspend fun setCompleted(id: Long, completed: Boolean) = todoDao.setCompleted(id, completed)

    fun observeByDateRange(start: Long, end: Long): Flow<List<TodoEntity>> =
        todoDao.observeByDateRange(start, end)

    /**
     * 勾选/取消完成。
     * 仅“每日重复(DAILY)”在本次勾选为完成时，才生成“次日”实例；
     * 且需满足：未超过结束日期、且该日实例尚不存在（避免重复生成）。
     * 不重复任务勾选完成不会产生任何新待办。
     */
    suspend fun toggleCompleted(todo: TodoEntity) {
        val nowCompleted = !todo.isCompleted
        todoDao.setCompleted(todo.id, nowCompleted)

        if (!nowCompleted || todo.repeatRule != RepeatRule.DAILY) return

        val base = todo.dueDate ?: TimeUtils.startOfDay(System.currentTimeMillis())
        val nextDue = TimeUtils.addDays(base, 1)
        val withinEnd = todo.endDate == null || nextDue <= todo.endDate
        if (!withinEnd) return

        val series = todo.seriesId ?: todo.id
        if (todoDao.countInSeriesOnDate(series, nextDue) == 0) {
            todoDao.insert(
                todo.copy(
                    id = 0L,
                    isCompleted = false,
                    dueDate = nextDue,
                    seriesId = series
                )
            )
        }
    }
}
