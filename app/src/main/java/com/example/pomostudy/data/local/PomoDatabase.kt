package com.example.pomostudy.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PomoDatabase : RoomDatabase() {

    abstract fun tagDao(): TagDao
    abstract fun todoDao(): TodoDao
    abstract fun studyRecordDao(): StudyRecordDao

    companion object {
        private const val DB_NAME = "pomo_study.db"

        /** v1 -> v2：新增待办顺延字段 carryOver（历史遗留）。 */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE todos ADD COLUMN carryOver INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** v2 -> v3：每日重复系列字段 endDate / seriesId。 */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE todos ADD COLUMN endDate INTEGER")
                db.execSQL("ALTER TABLE todos ADD COLUMN seriesId INTEGER")
            }
        }

        /** v3 -> v4：待办计划学习时长 plannedMinutes。 */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE todos ADD COLUMN plannedMinutes INTEGER")
            }
        }

        @Volatile
        private var INSTANCE: PomoDatabase? = null

        fun get(context: Context): PomoDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PomoDatabase::class.java,
                    DB_NAME
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build().also { INSTANCE = it }
            }
    }
}
