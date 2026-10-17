package com.example.pomostudy.data.local

import androidx.room.TypeConverter
import com.example.pomostudy.domain.model.Priority
import com.example.pomostudy.domain.model.RepeatRule

/** Room 类型转换器：枚举 <-> 基础类型。 */
class Converters {

    @TypeConverter
    fun priorityToInt(priority: Priority): Int = priority.value

    @TypeConverter
    fun intToPriority(value: Int): Priority = Priority.fromValue(value)

    @TypeConverter
    fun repeatRuleToString(rule: RepeatRule): String = rule.name

    @TypeConverter
    fun stringToRepeatRule(value: String): RepeatRule =
        runCatching { RepeatRule.valueOf(value) }.getOrDefault(RepeatRule.NONE)
}
