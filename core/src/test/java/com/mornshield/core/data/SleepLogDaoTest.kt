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
class SleepLogDaoTest {

    private lateinit var db: MornShieldDatabase
    private lateinit var sleepLogDao: SleepLogDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, MornShieldDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        sleepLogDao = db.sleepLogDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun testInsertAndGetLogs() = runBlocking {
        val log = SleepLogEntity(
            dateString = "2026-07-07",
            startTime = 1718000000000L,
            endTime = 1718028800000L,
            sleepQualityScore = 8,
            ritualCompleted = true
        )
        sleepLogDao.insertLog(log)
        val allLogs = sleepLogDao.getAllLogs().first()
        assertEquals(1, allLogs.size)
        assertEquals(8, allLogs[0].sleepQualityScore)
        assertTrue(allLogs[0].ritualCompleted)
    }

    @Test
    fun testGetAllLogsList() = runBlocking {
        val log1 = SleepLogEntity(
            dateString = "2026-07-06",
            startTime = 1717900000000L,
            endTime = 1717928800000L,
            sleepQualityScore = 7
        )
        val log2 = SleepLogEntity(
            dateString = "2026-07-07",
            startTime = 1718000000000L,
            endTime = 1718028800000L,
            sleepQualityScore = 9
        )
        sleepLogDao.insertLog(log1)
        sleepLogDao.insertLog(log2)

        val logsList = sleepLogDao.getAllLogsList()
        assertEquals(2, logsList.size)
        // Ordered by startTime DESC, so log2 should be first
        assertEquals(9, logsList[0].sleepQualityScore)
        assertEquals(7, logsList[1].sleepQualityScore)
    }
}
