package com.mornshield.mobile

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.google.firebase.analytics.FirebaseAnalytics
import com.mornshield.core.data.MornShieldDatabase
import com.mornshield.core.data.SleepLogEntity
import com.mornshield.core.data.TaskEntity
import com.mornshield.core.sync.NsdSyncClient
import com.mornshield.mobile.audio.BriefingEngine
import com.mornshield.mobile.service.MornShieldNotificationListenerService
import com.mornshield.mobile.ui.OnboardingScreen
import com.mornshield.mobile.ui.PuzzleScreen
import com.mornshield.mobile.util.AdsHelper
import com.mornshield.mobile.util.HealthHelper
import com.mornshield.mobile.util.RatingHelper
import com.mornshield.mobile.util.RemoteConfigHelper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppScreen {
    ONBOARDING,
    DASHBOARD,
    PUZZLE,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var database: MornShieldDatabase
    private lateinit var briefingEngine: BriefingEngine
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var nsdSyncClient: NsdSyncClient

    private val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
    private val KEY_PREMIUM = "premium_unlocked"

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        sharedPreferences = getSharedPreferences("mornshield_prefs", MODE_PRIVATE)
        database = MornShieldDatabase.getInstance(this)
        briefingEngine = BriefingEngine(this)
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        nsdSyncClient = NsdSyncClient(this)
        nsdSyncClient.startDiscovery()

        setContent {
            var currentScreen by remember {
                mutableStateOf(
                    if (sharedPreferences.getBoolean(KEY_ONBOARDING_COMPLETE, false)) {
                        AppScreen.DASHBOARD
                    } else {
                        AppScreen.ONBOARDING
                    }
                )
            }

            var hasPermissions by remember {
                mutableStateOf(
                    ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED &&
                    (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q || ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
                )
            }

            val requestPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { perms ->
                hasPermissions = perms[Manifest.permission.POST_NOTIFICATIONS] == true &&
                                 (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q || perms[Manifest.permission.ACTIVITY_RECOGNITION] == true) &&
                                 perms[Manifest.permission.READ_CALENDAR] == true &&
                                 (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S || 
                                  (perms[Manifest.permission.BLUETOOTH_SCAN] == true && perms[Manifest.permission.BLUETOOTH_CONNECT] == true))
            }

            var isPremium by remember {
                mutableStateOf(sharedPreferences.getBoolean(KEY_PREMIUM, false))
            }

            var alarmHour by remember { mutableStateOf(sharedPreferences.getInt("alarm_hour", 6)) }
            var alarmMinute by remember { mutableStateOf(sharedPreferences.getInt("alarm_minute", 30)) }
            var windowSize by remember { mutableStateOf(sharedPreferences.getInt("alarm_window", 15)) }

            val taskDao = database.taskDao()
            val sleepLogDao = database.sleepLogDao()

            val todayDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
            
            var todayTasks by remember { mutableStateOf(emptyList<TaskEntity>()) }
            var sleepLogs by remember { mutableStateOf(emptyList<SleepLogEntity>()) }
            
            val isShieldActive by MornShieldNotificationListenerService.isShieldActive.collectAsState()
            val suppressedNotifications by MornShieldNotificationListenerService.suppressedNotifications.collectAsState()

            // Observe Tasks & Logs
            LaunchedEffect(Unit) {
                launch {
                    taskDao.getTasksForDate(todayDate).collectLatest {
                        todayTasks = it
                    }
                }
                launch {
                    sleepLogDao.getAllLogs().collectLatest {
                        sleepLogs = it
                    }
                }
                launch(kotlinx.coroutines.Dispatchers.IO) {
                    val existingTasks = taskDao.getTasksForDateList(todayDate)
                    if (existingTasks.isEmpty()) {
                        taskDao.insertTask(TaskEntity(title = "Hydrate (500ml water)", dateString = todayDate))
                        taskDao.insertTask(TaskEntity(title = "Stretching (5 mins)", dateString = todayDate))
                        taskDao.insertTask(TaskEntity(title = "Read 2 pages of a book", dateString = todayDate))
                    }
                    val existingLogs = sleepLogDao.getAllLogsList()
                    if (existingLogs.isEmpty()) {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val calendar = java.util.Calendar.getInstance()
                        val scores = listOf(8, 7, 9, 6, 8, 7, 9)
                        for (i in 6 downTo 0) {
                            calendar.time = Date()
                            calendar.add(java.util.Calendar.DAY_OF_YEAR, -i)
                            val dateStr = sdf.format(calendar.time)
                            
                            calendar.set(java.util.Calendar.HOUR_OF_DAY, 22)
                            calendar.set(java.util.Calendar.MINUTE, 30)
                            val startMs = calendar.timeInMillis
                            
                            calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
                            calendar.set(java.util.Calendar.HOUR_OF_DAY, 6)
                            calendar.set(java.util.Calendar.MINUTE, 30)
                            val endMs = calendar.timeInMillis
                            
                            val score = scores[i % scores.size]
                            sleepLogDao.insertLog(
                                SleepLogEntity(
                                    dateString = dateStr,
                                    startTime = startMs,
                                    endTime = endMs,
                                    sleepQualityScore = score,
                                    ritualCompleted = true
                                )
                            )
                        }
                    }
                }
            }

            AnimatedContent(
                targetState = currentScreen,
                label = "ScreenTransition",
                transitionSpec = {
                    if (targetState.ordinal > initialState.ordinal) {
                        (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut())
                    } else {
                        (slideInHorizontally { -it } + fadeIn()) togetherWith (slideOutHorizontally { it } + fadeOut())
                    }
                }
            ) { screen ->
                LaunchedEffect(screen) {
                    logScreen(screen.name)
                }
                when (screen) {
                    AppScreen.ONBOARDING -> {
                        OnboardingScreen(
                            onRequestPermissions = { 
                                val permissions = mutableListOf(
                                    Manifest.permission.POST_NOTIFICATIONS, 
                                    Manifest.permission.READ_CALENDAR,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                )
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                    permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
                                }
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                    permissions.add(Manifest.permission.BLUETOOTH_SCAN)
                                    permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
                                    permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
                                }
                                requestPermissionLauncher.launch(permissions.toTypedArray())
                            },
                            hasPermissions = hasPermissions,
                            onFinishOnboarding = {
                                sharedPreferences.edit().putBoolean(KEY_ONBOARDING_COMPLETE, true).apply()
                                currentScreen = AppScreen.DASHBOARD
                                logEvent("onboarding_completed", null)
                            }
                        )
                    }
                    AppScreen.DASHBOARD -> {
                        DashboardScreen(
                            todayTasks = todayTasks,
                            sleepLogs = sleepLogs,
                            isPremium = isPremium,
                            isShieldActive = isShieldActive,
                            suppressedCount = suppressedNotifications.size,
                            alarmHour = alarmHour,
                            alarmMinute = alarmMinute,
                            windowSize = windowSize,
                            onToggleShield = {
                                if (isShieldActive) {
                                    currentScreen = AppScreen.PUZZLE
                                } else {
                                    MornShieldNotificationListenerService.setShieldActive(this@MainActivity, true)
                                    logEvent("notification_shield_toggled", Bundle().apply { putBoolean("active", true) })
                                }
                            },
                            onAddTask = { text ->
                                lifecycleScope.launch {
                                    val task = TaskEntity(title = text, dateString = todayDate)
                                    taskDao.insertTask(task)
                                    val data = org.json.JSONObject().apply {
                                        put("title", task.title)
                                        put("isCompleted", task.isCompleted)
                                        put("dateString", task.dateString)
                                    }
                                    nsdSyncClient.sendSyncEvent("TASK_ADDED", data)
                                    logEvent("task_added", null)
                                }
                            },
                            onToggleTask = { task ->
                                lifecycleScope.launch {
                                    val nextState = !task.isCompleted
                                    val updatedTask = task.copy(isCompleted = nextState)
                                    taskDao.updateTask(updatedTask)
                                    
                                    val data = org.json.JSONObject().apply {
                                        put("title", updatedTask.title)
                                        put("isCompleted", updatedTask.isCompleted)
                                        put("dateString", updatedTask.dateString)
                                    }
                                    nsdSyncClient.sendSyncEvent("TASK_UPDATED", data)
                                    logEvent("task_toggled", Bundle().apply { putBoolean("completed", nextState) })
                                }
                            },
                            onDeleteTask = { task ->
                                lifecycleScope.launch {
                                    taskDao.deleteTask(task)
                                    val data = org.json.JSONObject().apply {
                                        put("title", task.title)
                                        put("dateString", task.dateString)
                                    }
                                    nsdSyncClient.sendSyncEvent("TASK_DELETED", data)
                                    logEvent("task_deleted", null)
                                }
                            },
                            onNavigateToSettings = { currentScreen = AppScreen.SETTINGS }
                        )
                    }
                    AppScreen.PUZZLE -> {
                        PuzzleScreen(
                            onPuzzleSolved = { solveDuration ->
                                if (solveDuration == 0L) {
                                    logEvent("emergency_bypass_triggered", null)
                                }
                                MornShieldNotificationListenerService.setShieldActive(this@MainActivity, false)
                                RatingHelper.recordRitualCompletion(this@MainActivity)
                                HealthHelper.syncRitualWithHealthConnect(this@MainActivity, "Morning Wordle")
                                logEvent("morning_ritual_completed", Bundle().apply {
                                    putInt("suppressed_notifications", suppressedNotifications.size)
                                    putLong("solve_duration_ms", solveDuration)
                                })
                                currentScreen = AppScreen.DASHBOARD
                                
                                // Launch TTS briefing
                                val taskTitles = todayTasks.map { it.title }
                                briefingEngine.playMorningBrief("", taskTitles)

                                // Trigger Play Store rating dialog if conditions are met
                                RatingHelper.triggerReviewFlow(this@MainActivity)
                                
                                // Show interstitial ad upon ritual completion
                                AdsHelper.showInterstitial(this@MainActivity, isPremium)
                            }
                        )
                    }
                    AppScreen.SETTINGS -> {
                        SettingsScreen(
                            isPremium = isPremium,
                            alarmHour = alarmHour,
                            alarmMinute = alarmMinute,
                            windowSize = windowSize,
                            suppressedNotifications = suppressedNotifications,
                            onSaveAlarm = { h, m, w ->
                                alarmHour = h
                                alarmMinute = m
                                windowSize = w
                                sharedPreferences.edit()
                                    .putInt("alarm_hour", h)
                                    .putInt("alarm_minute", m)
                                    .putInt("alarm_window", w)
                                    .apply()
                                logEvent("alarm_settings_saved", Bundle().apply {
                                    putInt("hour", h)
                                    putInt("minute", m)
                                    putInt("window", w)
                                })
                                Toast.makeText(this@MainActivity, getString(R.string.alarm_saved), Toast.LENGTH_SHORT).show()
                            },
                            onTogglePremium = {
                                isPremium = !isPremium
                                sharedPreferences.edit().putBoolean(KEY_PREMIUM, isPremium).apply()
                                logEvent("premium_toggled", Bundle().apply { putBoolean("premium", isPremium) })
                            },
                            onTriggerReview = {
                                RatingHelper.triggerReviewFlow(this@MainActivity, force = true)
                            },
                            onBack = { currentScreen = AppScreen.DASHBOARD }
                        )
                    }
                }
            }
        }
    }

    private fun logEvent(name: String, bundle: Bundle?) {
        try {
            firebaseAnalytics.logEvent(name, bundle)
        } catch (e: Exception) {}
    }

    private fun logScreen(screenName: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, "MainActivity")
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
        Log.d("MornShieldAnalytics", "Screen View: $screenName")
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat?.contains(packageName) == true
    }

    private fun requestSpecialPermissions() {
        if (!isNotificationListenerEnabled()) {
            try {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
            return
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        if (!notificationManager.isNotificationPolicyAccessGranted) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        briefingEngine.shutdown()
        nsdSyncClient.stopDiscovery()
    }
}

@Composable
fun DashboardScreen(
    todayTasks: List<TaskEntity>,
    sleepLogs: List<SleepLogEntity>,
    isPremium: Boolean,
    isShieldActive: Boolean,
    suppressedCount: Int,
    alarmHour: Int,
    alarmMinute: Int,
    windowSize: Int,
    onToggleShield: () -> Unit,
    onAddTask: (String) -> Unit,
    onToggleTask: (TaskEntity) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    var newTaskText by remember { mutableStateOf("") }
    val gradientColors = listOf(Color(0xFF0F0C20), Color(0xFF0A0915))
    val isTablet = LocalConfiguration.current.screenWidthDp >= 600

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(gradientColors))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "Logo",
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF15102A))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(id = R.string.app_name).uppercase(),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFC0B3FF),
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = stringResource(id = R.string.slogan),
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(id = R.string.settings),
                        tint = Color(0xFFC0B3FF)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isTablet) {
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Left Column (Controls & Analytics)
                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Alarm Status Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF15102A).copy(alpha = 0.6f)),
                            border = boxBorder()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(stringResource(id = R.string.cycle_synced_alarm), color = Color(0xFFC0B3FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = String.format(Locale.US, "%02d:%02d AM", alarmHour, alarmMinute),
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                    
                                    val startH = if (alarmMinute >= windowSize) alarmHour else (alarmHour + 23) % 24
                                    val startM = if (alarmMinute >= windowSize) alarmMinute - windowSize else 60 + alarmMinute - windowSize
                                    val startTimeStr = String.format(Locale.US, "%02d:%02d", startH, startM)
                                    
                                    Text(
                                        text = stringResource(id = R.string.rem_window, windowSize, startTimeStr),
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF7A60FF).copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Alarm,
                                        contentDescription = "Alarm Active",
                                        tint = Color(0xFF7A60FF),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        // Notification Shield Control Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isShieldActive) Color(0xFF4A1525).copy(alpha = 0.5f) else Color(0xFF16132C).copy(alpha = 0.6f)
                            ),
                            border = boxBorder()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(id = if (isShieldActive) R.string.shield_active else R.string.shield_inactive),
                                            color = if (isShieldActive) Color(0xFFFF5252) else Color(0xFFC0B3FF),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (isShieldActive) 
                                                stringResource(id = R.string.suppressing_distractions, suppressedCount)
                                            else 
                                                stringResource(id = R.string.notifications_flowing),
                                            fontSize = 13.sp,
                                            color = Color.White
                                        )
                                    }

                                    Button(
                                        onClick = onToggleShield,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isShieldActive) Color(0xFFFF5252) else Color(0xFF7A60FF)
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(stringResource(id = if (isShieldActive) R.string.unlock else R.string.shield_on), color = Color.White, fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        // Streaks mapping
                        Column {
                            Text(
                                text = stringResource(id = R.string.morning_ritual_streaks),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC0B3FF),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val last7Days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                                last7Days.forEachIndexed { idx, day ->
                                    val isDone = idx < 4 
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(if (isDone) Color(0xFF2E7D32) else Color(0xFF221F35))
                                                .border(1.dp, if (isDone) Color.Green else Color.Gray.copy(alpha = 0.2f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isDone) {
                                                Icon(Icons.Default.Check, contentDescription = stringResource(id = R.string.done), tint = Color.White, modifier = Modifier.size(16.dp))
                                            } else {
                                                Text(day.first().toString(), color = Color.Gray, fontSize = 12.sp)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(day, fontSize = 10.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }

                        // Advanced Analytics Chart
                        Column {
                            Text(
                                text = stringResource(id = R.string.sleep_quality_trend),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC0B3FF),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp)
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                val displayLogs = sleepLogs.takeLast(7)
                                displayLogs.forEach { log ->
                                    val barHeightFactor = log.sleepQualityScore / 10f
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(barHeightFactor.coerceAtLeast(0.1f))
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(Color(0xFF7A60FF), Color(0xFFC0B3FF).copy(alpha = 0.5f))
                                                )
                                            )
                                    )
                                }
                                
                                repeat(7 - displayLogs.size) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(Color.Gray.copy(alpha = 0.1f))
                                    )
                                }
                            }
                        }
                    }

                    // Right Column (Checklist)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        ChecklistContent(
                            todayTasks = todayTasks,
                            newTaskText = newTaskText,
                            onNewTaskTextChange = { newTaskText = it },
                            onAddTask = onAddTask,
                            onToggleTask = onToggleTask,
                            onDeleteTask = onDeleteTask,
                            listModifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                // Phone Layout (Single column)
                Column(modifier = Modifier.weight(1f)) {
                    // Alarm Status Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF15102A).copy(alpha = 0.6f)),
                        border = boxBorder()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(stringResource(id = R.string.cycle_synced_alarm), color = Color(0xFFC0B3FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = String.format(Locale.US, "%02d:%02d AM", alarmHour, alarmMinute),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                
                                val startH = if (alarmMinute >= windowSize) alarmHour else (alarmHour + 23) % 24
                                val startM = if (alarmMinute >= windowSize) alarmMinute - windowSize else 60 + alarmMinute - windowSize
                                val startTimeStr = String.format(Locale.US, "%02d:%02d", startH, startM)
                                
                                Text(
                                    text = stringResource(id = R.string.rem_window, windowSize, startTimeStr),
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF7A60FF).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Alarm,
                                    contentDescription = "Alarm Active",
                                    tint = Color(0xFF7A60FF),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Notification Shield Control Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isShieldActive) Color(0xFF4A1525).copy(alpha = 0.5f) else Color(0xFF16132C).copy(alpha = 0.6f)
                        ),
                        border = boxBorder()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(id = if (isShieldActive) R.string.shield_active else R.string.shield_inactive),
                                        color = if (isShieldActive) Color(0xFFFF5252) else Color(0xFFC0B3FF),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isShieldActive) 
                                            stringResource(id = R.string.suppressing_distractions, suppressedCount)
                                        else 
                                            stringResource(id = R.string.notifications_flowing),
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                }

                                Button(
                                    onClick = onToggleShield,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isShieldActive) Color(0xFFFF5252) else Color(0xFF7A60FF)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(stringResource(id = if (isShieldActive) R.string.unlock else R.string.shield_on), color = Color.White, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Streaks mapping
                    Text(
                        text = stringResource(id = R.string.morning_ritual_streaks),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC0B3FF),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val last7Days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                        last7Days.forEachIndexed { idx, day ->
                            val isDone = idx < 4 
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isDone) Color(0xFF2E7D32) else Color(0xFF221F35))
                                        .border(1.dp, if (isDone) Color.Green else Color.Gray.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isDone) {
                                        Icon(Icons.Default.Check, contentDescription = stringResource(id = R.string.done), tint = Color.White, modifier = Modifier.size(16.dp))
                                    } else {
                                        Text(day.first().toString(), color = Color.Gray, fontSize = 12.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(day, fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Advanced Analytics Chart
                    Text(
                        text = stringResource(id = R.string.sleep_quality_trend),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC0B3FF),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val displayLogs = sleepLogs.takeLast(7)
                        displayLogs.forEach { log ->
                            val barHeightFactor = log.sleepQualityScore / 10f
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(barHeightFactor.coerceAtLeast(0.1f))
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(0xFF7A60FF), Color(0xFFC0B3FF).copy(alpha = 0.5f))
                                        )
                                    )
                            )
                        }
                        
                        repeat(7 - displayLogs.size) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.Gray.copy(alpha = 0.1f))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Checklist
                    ChecklistContent(
                        todayTasks = todayTasks,
                        newTaskText = newTaskText,
                        onNewTaskTextChange = { newTaskText = it },
                        onAddTask = onAddTask,
                        onToggleTask = onToggleTask,
                        onDeleteTask = onDeleteTask,
                        listModifier = Modifier.weight(1f)
                    )
                }
            }

            // AdMob Adaptive Banner Ad
            AdsHelper.BannerAd(isPremium = isPremium)
        }
    }
}

@Composable
fun ChecklistContent(
    todayTasks: List<TaskEntity>,
    newTaskText: String,
    onNewTaskTextChange: (String) -> Unit,
    onAddTask: (String) -> Unit,
    onToggleTask: (TaskEntity) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit,
    listModifier: Modifier
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(id = R.string.today_rituals, todayTasks.count { it.isCompleted }, todayTasks.size),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFC0B3FF),
            letterSpacing = 1.sp
        )
        
        if (todayTasks.isNotEmpty()) {
            val progress = todayTasks.count { it.isCompleted }.toFloat() / todayTasks.size
            Text(
                text = "${(progress * 100).toInt()}%",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (progress == 1f) Color.Green else Color(0xFF7A60FF)
            )
        }
    }

    if (todayTasks.isNotEmpty()) {
        Spacer(modifier = Modifier.height(6.dp))
        val progress = todayTasks.count { it.isCompleted }.toFloat() / todayTasks.size
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = Color(0xFF7A60FF),
            trackColor = Color(0xFF221F35)
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Add Task row
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = newTaskText,
            onValueChange = onNewTaskTextChange,
            placeholder = { Text(stringResource(id = R.string.add_task_hint), color = Color.Gray, fontSize = 13.sp) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF7A60FF),
                unfocusedBorderColor = Color(0xFF2C2750)
            ),
            modifier = Modifier
                .weight(1f)
                .height(50.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = {
                if (newTaskText.trim().isNotEmpty()) {
                    onAddTask(newTaskText.trim())
                }
            },
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF7A60FF))
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.add_task), tint = Color.White)
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Tasks List
    LazyColumn(modifier = listModifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(todayTasks) { task ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF16132C).copy(alpha = 0.5f))
                    .border(1.dp, Color(0xFF2C2750), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Checkbox(
                        checked = task.isCompleted,
                        onCheckedChange = { onToggleTask(task) },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF7A60FF))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Enhanced markdown rendering: **bold**, *italic*
                    val annotatedTitle = remember(task.title) {
                        buildAnnotatedString {
                            val text = task.title
                            val regex = "(\\d+)|(\\*\\*.*?\\*\\*)|(\\*.*?\\*)|(_.*?_)".toRegex()
                            var lastIndex = 0
                            regex.findAll(text).forEach { match ->
                                append(text.substring(lastIndex, match.range.first))
                                val matchText = match.value
                                when {
                                    matchText.startsWith("**") -> {
                                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                            append(matchText.removeSurrounding("**"))
                                        }
                                    }
                                    matchText.startsWith("*") -> {
                                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                            append(matchText.removeSurrounding("*"))
                                        }
                                    }
                                    matchText.startsWith("_") -> {
                                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                            append(matchText.removeSurrounding("_"))
                                        }
                                    }
                                    else -> append(matchText)
                                }
                                lastIndex = match.range.last + 1
                            }
                            append(text.substring(lastIndex))
                        }
                    }
                    
                    Text(
                        text = annotatedTitle,
                        color = if (task.isCompleted) Color.Gray else Color.White,
                        fontSize = 14.sp
                    )
                }

                IconButton(onClick = { onDeleteTask(task) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252).copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    isPremium: Boolean,
    alarmHour: Int,
    alarmMinute: Int,
    windowSize: Int,
    suppressedNotifications: List<MornShieldNotificationListenerService.SuppressedNotification>,
    onSaveAlarm: (Int, Int, Int) -> Unit,
    onTogglePremium: () -> Unit,
    onTriggerReview: () -> Unit,
    onBack: () -> Unit
) {
    var hInput by remember { mutableStateOf(alarmHour.toString()) }
    var mInput by remember { mutableStateOf(alarmMinute.toString()) }
    var wInput by remember { mutableStateOf(windowSize.toString()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0F0C20), Color(0xFF05040B))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFFC0B3FF))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(id = R.string.settings).uppercase(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Alarm Configuration Inputs
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF15102A).copy(alpha = 0.5f)),
                border = boxBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(id = R.string.alarm_settings), color = Color(0xFFC0B3FF), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = hInput,
                            onValueChange = { hInput = it },
                            label = { Text(stringResource(id = R.string.hour), color = Color.Gray, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        OutlinedTextField(
                            value = mInput,
                            onValueChange = { mInput = it },
                            label = { Text(stringResource(id = R.string.minute), color = Color.Gray, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        OutlinedTextField(
                            value = wInput,
                            onValueChange = { wInput = it },
                            label = { Text(stringResource(id = R.string.window), color = Color.Gray, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Button(
                        onClick = {
                            val h = hInput.toIntOrNull() ?: 6
                            val m = mInput.toIntOrNull() ?: 30
                            val w = wInput.toIntOrNull() ?: 15
                            onSaveAlarm(h.coerceIn(0, 23), m.coerceIn(0, 59), w.coerceIn(5, 60))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7A60FF)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(id = R.string.save_alarm))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Soundscape Selection (Phase 3)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF15102A).copy(alpha = 0.5f)),
                border = boxBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(id = R.string.soundscape_mixer), color = Color(0xFFC0B3FF), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val soundscapes = listOf("Rain & Forest", "Wind & Waves", "Deep Space")
                    var selectedIndex by remember { mutableStateOf(0) }
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        soundscapes.forEachIndexed { index, name ->
                            FilterChip(
                                selected = selectedIndex == index,
                                onClick = { selectedIndex = index },
                                label = { Text(name, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF7A60FF),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Premium Toggle Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF15102A).copy(alpha = 0.5f)),
                border = boxBorder()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(stringResource(id = R.string.premium_upgrade), color = Color(0xFFC0B3FF), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            text = if (isPremium) stringResource(id = R.string.premium_unlocked) else stringResource(id = R.string.standard_plan),
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }

                    Switch(
                        checked = isPremium,
                        onCheckedChange = { onTogglePremium() },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF7A60FF))
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Play Store Review Prompt Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF15102A).copy(alpha = 0.5f)),
                border = boxBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(id = R.string.app_evaluation), color = Color(0xFFC0B3FF), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(id = R.string.trigger_rating),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onTriggerReview,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37354A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(id = R.string.trigger_rating))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Privacy & Zero-Data-Leak Card (Phase 3)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D1A).copy(alpha = 0.8f)),
                border = BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = stringResource(id = R.string.security), tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(id = R.string.zero_data_leak_guarantee), color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(id = R.string.privacy_desc),
                        fontSize = 11.sp,
                        color = Color.Gray,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Suppressed Notifications List Title
            if (suppressedNotifications.isNotEmpty()) {
                Text(
                    text = stringResource(id = R.string.suppressed_distractions, suppressedNotifications.size),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC0B3FF),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                    items(suppressedNotifications) { alert ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF221F35).copy(alpha = 0.5f))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = alert.packageName.substringAfterLast("."),
                                    color = Color(0xFF7A60FF),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(alert.timestamp)),
                                    color = Color.Gray,
                                    fontSize = 10.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(alert.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(alert.text, color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun boxBorder() = BorderStroke(1.dp, Color(0xFFC0B3FF).copy(alpha = 0.15f))

@Preview(showBackground = true, backgroundColor = 0xFF0C091A)
@Composable
fun PreviewDashboard() {
    DashboardScreen(
        todayTasks = listOf(
            TaskEntity(title = "Morning Coffee", dateString = "2024-01-01", isCompleted = true),
            TaskEntity(title = "Stretching", dateString = "2024-01-01"),
            TaskEntity(title = "Read 10 pages", dateString = "2024-01-01")
        ),
        sleepLogs = emptyList(),
        isPremium = false,
        isShieldActive = true,
        suppressedCount = 5,
        alarmHour = 6,
        alarmMinute = 30,
        windowSize = 15,
        onToggleShield = {},
        onAddTask = {},
        onToggleTask = {},
        onDeleteTask = {},
        onNavigateToSettings = {}
    )
}
