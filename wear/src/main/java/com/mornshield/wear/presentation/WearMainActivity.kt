package com.mornshield.wear.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import com.mornshield.wear.R
import com.mornshield.wear.audio.FadeInAlarmService
import com.mornshield.wear.sensor.GestureDetector
import com.mornshield.wear.sleep.SleepMonitor
import com.mornshield.wear.sleep.SleepStage
import com.mornshield.core.sync.NsdSyncClient

class WearMainActivity : ComponentActivity() {

    private lateinit var sleepMonitor: SleepMonitor
    private lateinit var gestureDetector: GestureDetector
    private lateinit var nsdSyncClient: NsdSyncClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        nsdSyncClient = NsdSyncClient(this)
        nsdSyncClient.startDiscovery()

        sleepMonitor = SleepMonitor(this) { stage ->
            if (stage == SleepStage.REM) {
                startAlarmService()
                nsdSyncClient.sendSyncEvent("REM_DETECTED")
            }
        }

        gestureDetector = GestureDetector(this) {
            stopAlarmService()
            nsdSyncClient.sendSyncEvent("ALARM_SNOOZED")
        }

        setContent {
            WearApp()
        }
    }

    private fun startAlarmService() {
        val intent = Intent(this, FadeInAlarmService::class.java)
        startForegroundService(intent)
    }

    private fun stopAlarmService() {
        val intent = Intent(this, FadeInAlarmService::class.java)
        stopService(intent)
    }

    @Composable
    fun WearApp() {
        var isMonitoring by remember { mutableStateOf(false) }

        Scaffold(
            timeText = { TimeText() },
            modifier = Modifier.background(Color.Black)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(id = R.string.app_name),
                    style = MaterialTheme.typography.caption1,
                    color = Color(0xFF7A60FF)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = if (isMonitoring) stringResource(id = R.string.monitoring_sleep) else "IDLE",
                    style = MaterialTheme.typography.button,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        isMonitoring = !isMonitoring
                        if (isMonitoring) {
                            sleepMonitor.start()
                            gestureDetector.start()
                        } else {
                            sleepMonitor.stop()
                            gestureDetector.stop()
                            stopAlarmService()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (isMonitoring) Color(0xFFD32F2F) else Color(0xFF7A60FF)
                    )
                ) {
                    Text(if (isMonitoring) stringResource(id = R.string.stop) else stringResource(id = R.string.start_tracking))
                }
                
                if (isMonitoring) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Shake to snooze",
                        style = MaterialTheme.typography.caption2,
                        color = Color.Gray
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sleepMonitor.stop()
        gestureDetector.stop()
        nsdSyncClient.stopDiscovery()
    }
}
