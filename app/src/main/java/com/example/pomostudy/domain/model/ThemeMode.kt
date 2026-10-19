package com.example.pomostudy.domain.model

/** 主题模式：跟随系统 / 浅色 / 深色 / 马卡龙（马卡龙为浅色系的柔和配色）。 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    MACARON;

    companion object {
        fun fromName(name: String?): ThemeMode =
            entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}
