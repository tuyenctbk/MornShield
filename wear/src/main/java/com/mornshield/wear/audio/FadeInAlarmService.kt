package com.mornshield.wear.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.mornshield.wear.R
import com.mornshield.wear.presentation.WearMainActivity
import kotlinx.coroutines.*
import java.util.Timer
import java.util.TimerTask

class FadeInAlarmService : Service() {

    private val CHANNEL_ID = "MornShieldAlarmChannel"
    private val NOTIFICATION_ID = 4004

    private var playerLayer1: MediaPlayer? = null
    private var playerLayer2: MediaPlayer? = null
    private var playerLayer3: MediaPlayer? = null
    
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MornShield Alarm",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Waking sequence audio"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        startAcousticMixer()
        return START_STICKY
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, WearMainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MornShield Waking")
            .setContentText("A gradual morning soundscape is playing.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun startAcousticMixer() {
        Log.d("FadeInAlarmService", "MornShield Mixer started with three layers")
        // Logic to load raw resources and perform fade-in
        // playerLayer1 = MediaPlayer.create(this, R.raw.layer_birds) ...
    }

    override fun onDestroy() {
        super.onDestroy()
        playerLayer1?.stop()
        playerLayer2?.stop()
        playerLayer3?.stop()
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
