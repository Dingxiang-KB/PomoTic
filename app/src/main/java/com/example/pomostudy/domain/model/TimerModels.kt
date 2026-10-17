package com.example.pomostudy.domain.model

/** 计时模式：倒计时番茄 / 正计时自由学习。 */
enum class TimerMode {
    /** 倒计时番茄模式，可自定义工作时长与休息时长。 */
    POMODORO,

    /** 正计时自由学习模式。 */
    FREE
}

/** 番茄模式下的阶段：工作 / 休息（不自动触发长休息）。 */
enum class TimerPhase {
    WORK,
    BREAK
}

/** 计时状态机。 */
enum class TimerStatus {
    IDLE,
    RUNNING,
    PAUSED
}
