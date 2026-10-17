package com.example.pomostudy.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.pomostudy.domain.model.Priority
import com.example.pomostudy.domain.model.RepeatRule

/**
 * 待办表。
 * - 与标签表外键关联，标签删除时 tagId 自动置空（ON DELETE SET NULL）。
 * - 重复待办勾选完成时仅标记当前实例，后续由 Repository 生成下一条实例。
 */
@Entity(
    tableName = "todos",
    foreignKeys = [
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("tagId"), Index("dueDate")]
)
data class TodoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val note: String = "",
    /** 截止日期（当天 00:00 的时间戳），可为空。 */
    val dueDate: Long? = null,
    val tagId: Long? = null,
    val priority: Priority = Priority.MEDIUM,
    val isCompleted: Boolean = false,
    val repeatRule: RepeatRule = RepeatRule.NONE,
    val createdAt: Long = System.currentTimeMillis()
)
