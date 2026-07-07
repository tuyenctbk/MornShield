package com.mornshield.wear.presentation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.content.ContextCompat
import androidx.wear.compose.material.*
import com.mornshield.wear.R
import com.mornshield.wear.audio.FadeInAlarmService
import com.mornshield.wear.sensor.GestureDetector
import com.mornshield.wear.sleep.SleepMonitor
import com.mornshield.wear.sleep.SleepStage
import com.mornshield.core.sync.NsdSyncClient
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
            var hasPermissions by remember {
                mutableStateOf(
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
                )
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { perms ->
                hasPermissions = perms.values.all { it }
            }

            LaunchedEffect(Unit) {
                if (!hasPermissions) {
                    permissionLauncher.launch(arrayOf(Manifest.permission.BODY_SENSORS, Manifest.permission.ACTIVITY_RECOGNITION))
                }
            }

            val isAlarmPlaying by FadeInAlarmService.isAlarmPlaying.collectAsState()

            LaunchedEffect(isAlarmPlaying) {
                if (isAlarmPlaying) {
                    window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            if (hasPermissions) {
                if (isAlarmPlaying) {
                    AlarmRingingScreen()
                } else {
                    WearApp()
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Permissions required", textAlign = TextAlign.Center)
                }
            }
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

                Chip(
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
                    colors = ChipDefaults.chipColors(
                        backgroundColor = if (isMonitoring) Color(0xFFD32F2F) else Color(0xFF7A60FF)
                    ),
                    label = {
                        Text(
                            text = if (isMonitoring) stringResource(id = R.string.stop) else stringResource(id = R.string.start_tracking),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    },
                    modifier = Modifier.fillMaxWidth(0.85f)
                )
                
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

    @Composable
    fun AlarmRingingScreen() {
        var holdProgress by remember { mutableStateOf(0f) }
        val coroutineScope = rememberCoroutineScope()
        var holdJob by remember { mutableStateOf<Job?>(null) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitFirstDown()
                                // Touch detected, start progress build up
                                holdJob?.cancel()
                                holdJob = coroutineScope.launch {
                                    val startTime = System.currentTimeMillis()
                                    while (true) {
                                        val elapsed = System.currentTimeMillis() - startTime
                                        holdProgress = (elapsed / 3000f).coerceAtMost(1f)
                                        if (holdProgress >= 1f) {
                                            stopAlarmService()
                                            break
                                        }
                                        delay(30)
                                    }
                                }
                                // Wait for release or cancel
                                waitForUpOrCancellation()
                                holdJob?.cancel()
                                holdProgress = 0f
                            }
                        }
                    }
            ) {
                Text(
                    text = "WAKE UP",
                    style = MaterialTheme.typography.caption1,
                    color = Color(0xFFFF5252),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(80.dp)
                ) {
                    CircularProgressIndicator(
                        progress = holdProgress,
                        modifier = Modifier.fillMaxSize(),
                        startAngle = 270f,
                        indicatorColor = Color(0xFF7A60FF),
                        trackColor = Color.DarkGray
                    )
                    Text(
                        text = "HOLD",
                        style = MaterialTheme.typography.button,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Hold for 3s to dismiss",
                    style = MaterialTheme.typography.caption2,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
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
