package com.example.pomostudy.data.repository

import com.example.pomostudy.data.local.dao.TodoDao
import com.example.pomostudy.data.local.entity.TodoEntity
import com.example.pomostudy.domain.model.RepeatRule
import com.example.pomostudy.util.TimeUtils
import kotlinx.coroutines.flow.Flow

/** 待办数据仓库：统一管理待办的增删改查与重复规则处理。 */
class TodoRepository(private val todoDao: TodoDao) {

    val todos: Flow<List<TodoEntity>> = todoDao.observeAll()

    suspend fun add(todo: TodoEntity): Long = todoDao.insert(todo)

    suspend fun update(todo: TodoEntity) = todoDao.update(todo)

    suspend fun delete(todo: TodoEntity) = todoDao.delete(todo)

    suspend fun getById(id: Long): TodoEntity? = todoDao.getById(id)

    suspend fun setCompleted(id: Long, completed: Boolean) = todoDao.setCompleted(id, completed)

    fun observeByDateRange(start: Long, end: Long): Flow<List<TodoEntity>> =
        todoDao.observeByDateRange(start, end)

    /**
     * 勾选/取消完成。
     * 重复待办（每日/每月）在“本次勾选为完成”时，仅标记当前这一条实例完成，
     * 并且另外生成下一条待完成实例，因此不影响后续生成的重复任务。
     */
    suspend fun toggleCompleted(todo: TodoEntity) {
        val nowCompleted = !todo.isCompleted
        todoDao.setCompleted(todo.id, nowCompleted)

        if (nowCompleted && todo.repeatRule != RepeatRule.NONE) {
            val base = todo.dueDate ?: System.currentTimeMillis()
            val nextDue = when (todo.repeatRule) {
                RepeatRule.DAILY -> TimeUtils.addDays(base, 1)
                RepeatRule.MONTHLY -> TimeUtils.addOneMonth(base)
                RepeatRule.NONE -> base
            }
            todoDao.insert(
                todo.copy(
                    id = 0L,
                    isCompleted = false,
                    dueDate = nextDue,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }
}
