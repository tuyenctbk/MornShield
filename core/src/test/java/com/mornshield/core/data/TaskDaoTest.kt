package com.mornshield.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class TaskDaoTest {

    private lateinit var db: MornShieldDatabase
    private lateinit var taskDao: TaskDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, MornShieldDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        taskDao = db.taskDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun testInsertAndGetTasks() = runBlocking {
        val task = TaskEntity(title = "Morning Coffee", dateString = "2026-07-07")
        taskDao.insertTask(task)
        val allTasks = taskDao.getAllTasks().first()
        assertEquals(1, allTasks.size)
        assertEquals("Morning Coffee", allTasks[0].title)
    }

    @Test
    fun testGetTasksForDate() = runBlocking {
        val task1 = TaskEntity(title = "Task 1", dateString = "2026-07-07")
        val task2 = TaskEntity(title = "Task 2", dateString = "2026-07-08")
        taskDao.insertTask(task1)
        taskDao.insertTask(task2)

        val todayTasks = taskDao.getTasksForDateList("2026-07-07")
        assertEquals(1, todayTasks.size)
        assertEquals("Task 1", todayTasks[0].title)
    }

    @Test
    fun testUpdateTask() = runBlocking {
        val task = TaskEntity(title = "Task 1", dateString = "2026-07-07", isCompleted = false)
        taskDao.insertTask(task)
        
        val inserted = taskDao.getTasksForDateList("2026-07-07").first()
        val updated = inserted.copy(isCompleted = true)
        taskDao.updateTask(updated)

        val afterUpdate = taskDao.getTasksForDateList("2026-07-07").first()
        assertTrue(afterUpdate.isCompleted)
    }

    @Test
    fun testGetAllTasksList() = runBlocking {
        val task1 = TaskEntity(title = "Task 1", dateString = "2026-07-07")
        val task2 = TaskEntity(title = "Task 2", dateString = "2026-07-08")
        taskDao.insertTask(task1)
        taskDao.insertTask(task2)

        val allTasks = taskDao.getAllTasksList()
        assertEquals(2, allTasks.size)
    }
}
