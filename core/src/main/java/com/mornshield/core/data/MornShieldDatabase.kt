package com.mornshield.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TaskEntity::class, SleepLogEntity::class], version = 1, exportSchema = false)
abstract class MornShieldDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun sleepLogDao(): SleepLogDao

    companion object {
        @Volatile
        private var INSTANCE: MornShieldDatabase? = null

        fun getInstance(context: Context): MornShieldDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MornShieldDatabase::class.java,
                    "mornshield_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
