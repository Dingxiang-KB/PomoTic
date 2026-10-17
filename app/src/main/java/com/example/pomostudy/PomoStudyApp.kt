package com.example.pomostudy

import android.app.Application
import com.example.pomostudy.di.AppContainer

/** 应用入口：初始化依赖容器（数据库 / DataStore / 计时控制器）。 */
class PomoStudyApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
