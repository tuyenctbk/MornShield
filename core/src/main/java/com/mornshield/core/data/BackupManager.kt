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
            // Logic to get data from DAO
            val tasks = database.taskDao().getTasksForDateList("") 
            
            val backupObject = JSONObject().apply {
                put("version", 1)
                put("timestamp", System.currentTimeMillis())
                put("tasks_count", tasks.size)
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
