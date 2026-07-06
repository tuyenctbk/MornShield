package com.mornshield.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val isCompleted: Boolean = false,
    val dateString: String, // YYYY-MM-DD format
    val markdownDetail: String = "" // Optional markdown styling detail
)

@Entity(tableName = "sleep_logs")
data class SleepLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val dateString: String, // YYYY-MM-DD format
    val startTime: Long,
    val endTime: Long,
    val sleepQualityScore: Int, // 1 - 10 scale
    val ritualCompleted: Boolean = false // Was the shield successfully unlocked and morning task finished?
)
