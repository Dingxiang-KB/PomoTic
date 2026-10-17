package com.example.pomostudy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 标签表：支持自定义颜色。 */
@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    /** ARGB 颜色值，使用 Long 存储（例如 0xFFEF5350）。 */
    val color: Long
)
