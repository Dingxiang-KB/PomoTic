package com.example.pomostudy.data.repository

import com.example.pomostudy.data.local.dao.StudyRecordDao
import com.example.pomostudy.data.local.dao.TagDuration
import com.example.pomostudy.data.local.entity.StudyRecordEntity
import kotlinx.coroutines.flow.Flow

/** 学习记录数据仓库。 */
class StudyRepository(private val studyRecordDao: StudyRecordDao) {

    val records: Flow<List<StudyRecordEntity>> = studyRecordDao.observeAll()

    suspend fun add(record: StudyRecordEntity): Long = studyRecordDao.insert(record)

    suspend fun delete(record: StudyRecordEntity) = studyRecordDao.delete(record)

    fun observeInRange(start: Long, end: Long): Flow<List<StudyRecordEntity>> =
        studyRecordDao.observeInRange(start, end)

    fun observeByTodo(todoId: Long): Flow<List<StudyRecordEntity>> =
        studyRecordDao.observeByTodo(todoId)

    suspend fun recordsInRange(start: Long, end: Long): List<StudyRecordEntity> =
        studyRecordDao.getInRange(start, end)

    suspend fun totalSecondsInRange(start: Long, end: Long): Long =
        studyRecordDao.totalSecondsInRange(start, end) ?: 0L

    /** 按标签聚合时间段内学习总时长。 */
    suspend fun totalSecondsByTag(start: Long, end: Long): List<TagDuration> =
        studyRecordDao.totalSecondsByTag(start, end)
}
