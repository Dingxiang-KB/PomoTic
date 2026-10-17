package com.example.pomostudy.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 学习记录表（番茄/自由计时结束后写入）。
 * - tagId / todoId 均可为空，标签或待办删除时自动置空（ON DELETE SET NULL）。
 */
@Entity(
    tableName = "study_records",
    foreignKeys = [
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = TodoEntity::class,
            parentColumns = ["id"],
            childColumns = ["todoId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("tagId"), Index("todoId"), Index("startTime")]
)
data class StudyRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    /** 开始时间（epoch millis）。 */
    val startTime: Long,
    /** 结束时间（epoch millis）。 */
    val endTime: Long,
    /** 持续时长（秒）。 */
    val durationSeconds: Long,
    val tagId: Long? = null,
    val todoId: Long? = null,
    val note: String = "",
    /** 计时模式：POMODORO / FREE。 */
    val mode: String = "POMODORO"
)
