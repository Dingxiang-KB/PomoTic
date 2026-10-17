package com.example.pomostudy.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.pomostudy.data.local.entity.TodoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {

    /** 未完成优先、优先级高在前、有截止日期在前、时间升序。 */
    @Query(
        "SELECT * FROM todos ORDER BY isCompleted ASC, priority DESC, " +
            "CASE WHEN dueDate IS NULL THEN 1 ELSE 0 END ASC, dueDate ASC, id DESC"
    )
    fun observeAll(): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE id = :id")
    suspend fun getById(id: Long): TodoEntity?

    @Query(
        "SELECT * FROM todos WHERE dueDate IS NOT NULL AND dueDate >= :start AND dueDate <= :end " +
            "ORDER BY priority DESC, id DESC"
    )
    fun observeByDateRange(start: Long, end: Long): Flow<List<TodoEntity>>

    @Insert
    suspend fun insert(todo: TodoEntity): Long

    @Update
    suspend fun update(todo: TodoEntity)

    @Delete
    suspend fun delete(todo: TodoEntity)

    @Query("UPDATE todos SET isCompleted = :completed WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean)

    @Query("UPDATE todos SET dueDate = :dueDate WHERE id = :id")
    suspend fun setDueDate(id: Long, dueDate: Long)

    /** 未完成、开启顺延且已过期的待办。 */
    @Query(
        "SELECT * FROM todos WHERE isCompleted = 0 AND carryOver = 1 " +
            "AND dueDate IS NOT NULL AND dueDate < :threshold"
    )
    suspend fun getOverdueCarryOver(threshold: Long): List<TodoEntity>
}
