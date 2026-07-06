package com.mornshield.mobile.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.CopyOnWriteArrayList

class MornShieldNotificationListenerService : NotificationListenerService() {

    private val CHANNEL_ID = "MornShieldShieldChannel"
    private val NOTIFICATION_ID = 3003

    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    data class SuppressedNotification(
        val id: String,
        val packageName: String,
        val title: String,
        val text: String,
        val timestamp: Long
    )

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MornShield Notification Shield",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun updateForegroundState(active: Boolean) {
        // Toggle System-Level DND (Hard Shield)
        try {
            if (notificationManager.isNotificationPolicyAccessGranted) {
                val filter = if (active) {
                    NotificationManager.INTERRUPTION_FILTER_NONE
                } else {
                    NotificationManager.INTERRUPTION_FILTER_ALL
                }
                notificationManager.setInterruptionFilter(filter)
                Log.d(TAG, "System DND filter set to: $filter")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set DND filter: ${e.message}")
        }

        if (active) {
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Morning Shield Active")
                .setContentText("Distractions are being suppressed until you wake your brain.")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
            
            notificationManager.notify(NOTIFICATION_ID, notification)
        } else {
            notificationManager.cancel(NOTIFICATION_ID)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG, "Notification Listener bound")
        return super.onBind(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == "UPDATE_SHIELD_STATE") {
            updateForegroundState(isShieldActive.value)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName
        if (isShieldActive.value) {
            // Check if package is a known distraction app
            if (isDistraction(packageName)) {
                // Cancel notification programmatically
                cancelNotification(sbn.key)
                
                val title = sbn.notification.extras.getCharSequence("android.title")?.toString() ?: ""
                val text = sbn.notification.extras.getCharSequence("android.text")?.toString() ?: ""
                
                val suppressed = SuppressedNotification(
                    id = sbn.key,
                    packageName = packageName,
                    title = title,
                    text = text,
                    timestamp = sbn.postTime
                )
                
                _suppressedQueue.value = _suppressedQueue.value + suppressed
                Log.d(TAG, "Suppressed notification from $packageName: $title")
            }
        }
    }

    private fun isDistraction(packageName: String): Boolean {
        val lower = packageName.lowercase()
        return lower.contains("slack") || 
               lower.contains("gmail") || 
               lower.contains("whatsapp") || 
               lower.contains("facebook") || 
               lower.contains("instagram") || 
               lower.contains("twitter") || 
               lower.contains("skype") || 
               lower.contains("linkedin") || 
               lower.contains("telegram") || 
               lower.contains("snapchat") ||
               lower.contains("tiktok")
    }

    companion object {
        private const val TAG = "MornShieldNLS"

        // State indicating whether notification shielding is active
        val isShieldActive = MutableStateFlow(false)

        private val _suppressedQueue = MutableStateFlow<List<SuppressedNotification>>(emptyList())
        val suppressedNotifications: StateFlow<List<SuppressedNotification>> = _suppressedQueue

        fun getSuppressedNotifications(): List<SuppressedNotification> {
            return _suppressedQueue.value
        }

        fun clearSuppressedNotifications() {
            _suppressedQueue.value = emptyList()
        }

        fun setShieldActive(context: Context, active: Boolean) {
            isShieldActive.value = active
            if (active) {
                _suppressedQueue.value = emptyList()
            }
            
            // Notify service to update foreground notification
            val intent = Intent(context, MornShieldNotificationListenerService::class.java).apply {
                action = "UPDATE_SHIELD_STATE"
            }
            context.startService(intent)
        }
    }
}
