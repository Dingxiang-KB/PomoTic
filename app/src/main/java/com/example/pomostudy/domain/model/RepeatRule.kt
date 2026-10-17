package com.example.pomostudy.domain.model

/**
 * 待办重复规则：不重复 / 每日 / 每月。
 * 重复待办勾选完成时仅标记当前实例，并生成下一条实例。
 */
enum class RepeatRule {
    NONE,
    DAILY,
    MONTHLY
}
