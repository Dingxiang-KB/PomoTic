package com.example.pomostudy.domain.model

/**
 * 待办优先级：高 / 中 / 低 三档。
 * value 用于持久化与排序（数值越大优先级越高）。
 */
enum class Priority(val value: Int) {
    LOW(0),
    MEDIUM(1),
    HIGH(2);

    companion object {
        fun fromValue(value: Int): Priority = entries.firstOrNull { it.value == value } ?: LOW
    }
}
