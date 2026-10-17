package com.example.pomostudy.data.repository

import com.example.pomostudy.data.local.dao.TodoDao
import com.example.pomostudy.data.local.entity.TodoEntity
import com.example.pomostudy.domain.model.RepeatRule
import com.example.pomostudy.util.TimeUtils
import kotlinx.coroutines.flow.Flow

/** 待办数据仓库：统一管理待办的增删改查与每日重复逻辑。 */
class TodoRepository(private val todoDao: TodoDao) {

    val todos: Flow<List<TodoEntity>> = todoDao.observeAll()

    /** 每日重复（无结束日期时）预先铺满的天数。 */
    private val horizonDays = 365

    /**
     * 新增待办。
     * - 非每日：直接插入。
     * - 每日：以首条实例的 id 作为系列 ID，并按“开始日期..结束日期（含）”**预先铺满每天**的实例。
     *   结束日期为空则向后铺 [horizonDays] 天。
     */
    suspend fun addTodo(todo: TodoEntity): Long {
        if (todo.repeatRule != RepeatRule.DAILY) {
            return todoDao.insert(todo.copy(seriesId = null))
        }
        val start = TimeUtils.startOfDay(todo.dueDate ?: System.currentTimeMillis())
        val firstId = todoDao.insert(todo.copy(id = 0L, dueDate = start, seriesId = null, isCompleted = false))
        todoDao.setSeriesId(firstId, firstId)
        // 已存在起始日，materialize 会跳过它，补齐其余日期
        materialize(firstId, todo.copy(seriesId = firstId), start, todo.endDate)
        return firstId
    }

    /**
     * 更新待办。
     * - 非每日：直接更新。
     * - 每日：更新并“重建”该系列——删除未完成实例后按新区间铺满（已完成的实例保留）。
     */
    suspend fun update(todo: TodoEntity) {
        if (todo.repeatRule != RepeatRule.DAILY) {
            todoDao.update(todo.copy(seriesId = null))
            return
        }
        val seriesId = todo.seriesId ?: todo.id
        val start = TimeUtils.startOfDay(todo.dueDate ?: System.currentTimeMillis())
        val base = todo.copy(seriesId = seriesId)
        todoDao.update(base)
        todoDao.deleteUncompletedInSeries(seriesId)
        materialize(seriesId, base.copy(id = 0L, isCompleted = false), start, todo.endDate)
    }

    /** 按区间补齐每日实例（跳过已存在日期的实例）。 */
    private suspend fun materialize(
        seriesId: Long,
        template: TodoEntity,
        start: Long,
        endDate: Long?
    ) {
        val endDay = TimeUtils.startOfDay(endDate ?: TimeUtils.addDays(start, horizonDays))
        val existing = todoDao.getSeriesDates(seriesId).toHashSet()
        var day = TimeUtils.startOfDay(start)
        var guard = 0
        val max = horizonDays + 400
        while (day <= endDay && guard < max) {
            if (day !in existing) {
                todoDao.insert(
                    template.copy(
                        id = 0L,
                        dueDate = day,
                        seriesId = seriesId,
                        isCompleted = false
                    )
                )
            }
            day = TimeUtils.addDays(day, 1)
            guard++
        }
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

    /** 勾选/取消完成（每日实例在创建时已铺满，无需再生成）。 */
    suspend fun toggleCompleted(todo: TodoEntity) {
        todoDao.setCompleted(todo.id, !todo.isCompleted)
    }
}
