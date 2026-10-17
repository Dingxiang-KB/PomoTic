package com.example.pomostudy.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.pomostudy.data.local.entity.StudyRecordEntity
import kotlinx.coroutines.flow.Flow

/** 按标签聚合的时长查询结果。tagId 为空表示“未分类”。 */
data class TagDuration(
    val tagId: Long?,
    val totalSeconds: Long
)

/** 按待办聚合的时长查询结果。todoId 为空表示“未绑定待办”。 */
data class TodoDuration(
    val todoId: Long?,
    val totalSeconds: Long
)

@Dao
interface StudyRecordDao {

    @Insert
    suspend fun insert(record: StudyRecordEntity): Long

    @Delete
    suspend fun delete(record: StudyRecordEntity)

    @Query("SELECT * FROM study_records ORDER BY startTime DESC")
    fun observeAll(): Flow<List<StudyRecordEntity>>

    @Query("SELECT * FROM study_records WHERE startTime >= :start AND startTime <= :end ORDER BY startTime DESC")
    fun observeInRange(start: Long, end: Long): Flow<List<StudyRecordEntity>>

    @Query("SELECT * FROM study_records WHERE startTime >= :start AND startTime <= :end ORDER BY startTime DESC")
    suspend fun getInRange(start: Long, end: Long): List<StudyRecordEntity>

    @Query("SELECT SUM(durationSeconds) FROM study_records WHERE startTime >= :start AND startTime <= :end")
    suspend fun totalSecondsInRange(start: Long, end: Long): Long?

    @Query(
        "SELECT tagId AS tagId, SUM(durationSeconds) AS totalSeconds FROM study_records " +
            "WHERE startTime >= :start AND startTime <= :end GROUP BY tagId ORDER BY totalSeconds DESC"
    )
    suspend fun totalSecondsByTag(start: Long, end: Long): List<TagDuration>

    @Query(
        "SELECT todoId AS todoId, SUM(durationSeconds) AS totalSeconds FROM study_records " +
            "WHERE startTime >= :start AND startTime <= :end GROUP BY todoId ORDER BY totalSeconds DESC"
    )
    suspend fun totalSecondsByTodo(start: Long, end: Long): List<TodoDuration>

    @Query("SELECT * FROM study_records WHERE todoId = :todoId ORDER BY startTime DESC")
    fun observeByTodo(todoId: Long): Flow<List<StudyRecordEntity>>
}
