package com.mornshield.core.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM daily_tasks ORDER BY id ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM daily_tasks WHERE dateString = :date ORDER BY id ASC")
    fun getTasksForDate(date: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM daily_tasks WHERE dateString = :date ORDER BY id ASC")
    suspend fun getTasksForDateList(date: String): List<TaskEntity>

    @Query("SELECT * FROM daily_tasks ORDER BY id ASC")
    suspend fun getAllTasksList(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM daily_tasks")
    suspend fun clearTasks()
}

@Dao
interface SleepLogDao {
    @Query("SELECT * FROM sleep_logs ORDER BY startTime DESC")
    fun getAllLogs(): Flow<List<SleepLogEntity>>

    @Query("SELECT * FROM sleep_logs ORDER BY startTime DESC")
    suspend fun getAllLogsList(): List<SleepLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: SleepLogEntity): Long

    @Query("DELETE FROM sleep_logs")
    suspend fun clearLogs()
}
