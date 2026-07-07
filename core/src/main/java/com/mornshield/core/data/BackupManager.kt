package com.mornshield.core.data

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

class BackupManager(private val context: Context) {

    private val database = MornShieldDatabase.getInstance(context)

    suspend fun createEncryptedBackup(): File? = withContext(Dispatchers.IO) {
        try {
            // Get all tasks and sleep logs from database
            val tasks = database.taskDao().getAllTasksList()
            val logs = database.sleepLogDao().getAllLogsList()
            
            val tasksArray = org.json.JSONArray().apply {
                tasks.forEach { task ->
                    put(JSONObject().apply {
                        put("id", task.id)
                        put("title", task.title)
                        put("isCompleted", task.isCompleted)
                        put("dateString", task.dateString)
                        put("markdownDetail", task.markdownDetail)
                    })
                }
            }

            val logsArray = org.json.JSONArray().apply {
                logs.forEach { log ->
                    put(JSONObject().apply {
                        put("id", log.id)
                        put("dateString", log.dateString)
                        put("startTime", log.startTime)
                        put("endTime", log.endTime)
                        put("sleepQualityScore", log.sleepQualityScore)
                        put("ritualCompleted", log.ritualCompleted)
                    })
                }
            }

            val backupObject = JSONObject().apply {
                put("version", 1)
                put("timestamp", System.currentTimeMillis())
                put("tasks_count", tasks.size)
                put("tasks", tasksArray)
                put("sleep_logs", logsArray)
            }

            val backupFile = File(context.filesDir, "mornshield_backup.enc")
            if (backupFile.exists()) backupFile.delete()

            val mainKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val encryptedFile = EncryptedFile.Builder(
                context,
                backupFile,
                mainKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()

            encryptedFile.openFileOutput().use { output ->
                output.write(backupObject.toString().toByteArray())
            }

            backupFile
        } catch (e: Exception) {
            null
        }
    }
}
