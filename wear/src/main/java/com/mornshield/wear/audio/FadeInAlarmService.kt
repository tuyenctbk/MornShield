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
import kotlinx.coroutines.flow.MutableStateFlow

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
        isAlarmPlaying.value = true
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
        
        scope.launch(Dispatchers.IO) {
            try {
                playerLayer1 = MediaPlayer.create(this@FadeInAlarmService, R.raw.ambient_layer).apply { isLooping = true; setVolume(0f, 0f) }
                playerLayer2 = MediaPlayer.create(this@FadeInAlarmService, R.raw.melodic_layer).apply { isLooping = true; setVolume(0f, 0f) }
                playerLayer3 = MediaPlayer.create(this@FadeInAlarmService, R.raw.binaural_layer).apply { isLooping = true; setVolume(0f, 0f) }

                playerLayer1?.start()
                playerLayer2?.start()
                playerLayer3?.start()

                // Gradual fade-in over 60 seconds
                for (i in 1..100) {
                    if (!isActive) break
                    val volume = i / 100f
                    playerLayer1?.setVolume(volume * 0.8f, volume * 0.8f)
                    if (i > 30) playerLayer2?.setVolume((i - 30) / 70f * 0.6f, (i - 30) / 70f * 0.6f)
                    if (i > 60) playerLayer3?.setVolume((i - 60) / 40f * 0.5f, (i - 60) / 40f * 0.5f)
                    delay(600) // 100 steps * 600ms = 60 seconds
                }
            } catch (e: Exception) {
                Log.e("FadeInAlarmService", "Error in mixer: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isAlarmPlaying.value = false
        try {
            playerLayer1?.stop(); playerLayer1?.release()
            playerLayer2?.stop(); playerLayer2?.release()
            playerLayer3?.stop(); playerLayer3?.release()
        } catch (e: Exception) {
            Log.e("FadeInAlarmService", "Error releasing players: ${e.message}")
        }
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        val isAlarmPlaying = MutableStateFlow(false)
    }
}
