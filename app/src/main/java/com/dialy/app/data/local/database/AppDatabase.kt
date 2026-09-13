package com.dialy.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dialy.app.data.local.dao.DailyPlannerDao
import com.dialy.app.data.local.dao.GratitudeDao
import com.dialy.app.data.local.dao.PriorityDao
import com.dialy.app.data.local.dao.ReminderDao
import com.dialy.app.data.local.dao.ScheduleDao
import com.dialy.app.data.local.dao.SelfCareDao
import com.dialy.app.data.local.dao.TodoDao
import com.dialy.app.data.local.entities.DailyPlannerEntity
import com.dialy.app.data.local.entities.GratitudeEntity
import com.dialy.app.data.local.entities.PriorityEntity
import com.dialy.app.data.local.entities.ReminderEntity
import com.dialy.app.data.local.entities.ScheduleEntity
import com.dialy.app.data.local.entities.SelfCareEntity
import com.dialy.app.data.local.entities.TodoEntity

/**
 * Main Room Database for the Smart Diary application.
 */
@Database(
    entities = [
        DailyPlannerEntity::class,
        TodoEntity::class,
        PriorityEntity::class,
        ScheduleEntity::class,
        SelfCareEntity::class,
        ReminderEntity::class,
        GratitudeEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun dailyPlannerDao(): DailyPlannerDao
    abstract fun todoDao(): TodoDao
    abstract fun priorityDao(): PriorityDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun selfCareDao(): SelfCareDao
    abstract fun reminderDao(): ReminderDao
    abstract fun gratitudeDao(): GratitudeDao

    companion object {
        private const val DATABASE_NAME = "smart_diary.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
