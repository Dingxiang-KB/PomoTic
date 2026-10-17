package com.example.pomostudy.di

import android.content.Context
import com.example.pomostudy.data.local.PomoDatabase
import com.example.pomostudy.data.repository.SettingsRepository
import com.example.pomostudy.data.repository.StudyRepository
import com.example.pomostudy.data.repository.TagRepository
import com.example.pomostudy.data.repository.TodoRepository
import com.example.pomostudy.service.TimerController

/**
 * 手动依赖注入容器（应用级单例）。
 * 统一提供数据库、各 Repository、计时控制器，避免在页面中直接 new 数据源。
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val database: PomoDatabase by lazy { PomoDatabase.get(appContext) }

    val tagRepository: TagRepository by lazy { TagRepository(database.tagDao()) }

    val todoRepository: TodoRepository by lazy { TodoRepository(database.todoDao()) }

    val studyRepository: StudyRepository by lazy { StudyRepository(database.studyRecordDao()) }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }

    /** 计时控制器为应用级单例，UI 与前台服务共享同一状态。 */
    val timerController: TimerController by lazy {
        TimerController(appContext, settingsRepository, studyRepository, todoRepository)
    }
}
