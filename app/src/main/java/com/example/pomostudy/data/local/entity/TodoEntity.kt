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
 * - 重复规则：不重复(NONE) / 每日(DAILY)。
 *   · 每日重复拆成“每天一条实例”，由 [com.example.pomostudy.data.repository.TodoRepository] 在完成时生成次日实例；
 *   · [endDate] 为每日重复的结束日期（含当天），为空表示每天都添加；
 *   · [seriesId] 标识同一每日重复系列（同系列所有实例共享），用于“删除全部”。
 * - [carryOver] 为历史遗留字段，保留以兼容旧数据库，不再使用。
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
    /** 计划日期（当天 00:00 的时间戳）：每日实例=该实例所在日期；一次性任务可为空（清单待办）。 */
    val dueDate: Long? = null,
    /** 每日重复的结束日期（当天 00:00），为空表示不结束（每天都添加）。 */
    val endDate: Long? = null,
    /** 每日重复系列标识；非重复为 null。 */
    val seriesId: Long? = null,
    val tagId: Long? = null,
    val priority: Priority = Priority.MEDIUM,
    val isCompleted: Boolean = false,
    val repeatRule: RepeatRule = RepeatRule.NONE,
    /** 计划学习时长（分钟），用于统计“计划完成进度”；为空表示未设置计划。 */
    val plannedMinutes: Int? = null,
    /** 历史遗留字段，保留兼容，不再使用。 */
    val carryOver: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
