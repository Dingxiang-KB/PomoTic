package com.example.pomostudy.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.pomostudy.data.local.dao.StudyRecordDao
import com.example.pomostudy.data.local.dao.TagDao
import com.example.pomostudy.data.local.dao.TodoDao
import com.example.pomostudy.data.local.entity.StudyRecordEntity
import com.example.pomostudy.data.local.entity.TagEntity
import com.example.pomostudy.data.local.entity.TodoEntity

/**
 * Room 数据库（底层 SQLite），离线本地存储。
 * APP 启动时由 [com.example.pomostudy.di.AppContainer] 懒加载并自动建表。
 */
@Database(
    entities = [TagEntity::class, TodoEntity::class, StudyRecordEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PomoDatabase : RoomDatabase() {

    abstract fun tagDao(): TagDao
    abstract fun todoDao(): TodoDao
    abstract fun studyRecordDao(): StudyRecordDao

    companion object {
        private const val DB_NAME = "pomo_study.db"

        @Volatile
        private var INSTANCE: PomoDatabase? = null

        fun get(context: Context): PomoDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PomoDatabase::class.java,
                    DB_NAME
                ).build().also { INSTANCE = it }
            }
    }
}
