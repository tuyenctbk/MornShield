package com.mornshield.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import androidx.lifecycle.lifecycleScope
import com.mornshield.core.data.MornShieldDatabase
import com.mornshield.core.data.TaskEntity
import com.mornshield.tv.R
import com.mornshield.tv.sync.NsdSyncServer
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TvMainActivity : ComponentActivity() {

    private lateinit var database: MornShieldDatabase
    private lateinit var syncServer: NsdSyncServer

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        database = MornShieldDatabase.getInstance(this)

        setContent {
            MaterialTheme {
                TvDashboardScreen(database)
            }
        }

        // Start local network sync server
        syncServer = NsdSyncServer(this) { json ->
            val event = json.optString("event")
            val data = json.optJSONObject("data")
            
            if (event == "TASK_UPDATED" && data != null) {
                val title = data.optString("title")
                val isCompleted = data.optBoolean("isCompleted")
                val dateString = data.optString("dateString")
                
                lifecycleScope.launch {
                    val taskDao = database.taskDao()
                    val existingTasks = taskDao.getTasksForDateList(dateString)
                    val task = existingTasks.find { it.title == title }
                    if (task != null) {
                        taskDao.updateTask(task.copy(isCompleted = isCompleted))
                    } else {
                        taskDao.insertTask(TaskEntity(title = title, isCompleted = isCompleted, dateString = dateString))
                    }
                }
            } else if (event == "REM_DETECTED") {
                // TV could react to REM detection (e.g., wake up screen)
            }
        }
        syncServer.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        syncServer.stop()
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvDashboardScreen(database: MornShieldDatabase) {
    val todayDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    var tasks by remember { mutableStateOf(emptyList<TaskEntity>()) }

    LaunchedEffect(Unit) {
        database.taskDao().getTasksForDate(todayDate).collectLatest {
            tasks = it
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF0C091A), Color(0xFF1A1535))
                )
            )
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Left Panel: Time & Status
            Column(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight()
                    .padding(48.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(id = R.string.mornshield_ambient),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF7A60FF),
                    letterSpacing = 4.sp
                )
                
                var currentTime by remember { mutableStateOf(Date()) }
                LaunchedEffect(Unit) {
                    while (true) {
                        currentTime = Date()
                        kotlinx.coroutines.delay(1000)
                    }
                }
                
                Text(
                    text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(currentTime),
                    style = MaterialTheme.typography.displayLarge,
                    fontSize = 120.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                
                Text(
                    text = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(currentTime),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color(0xFFC0B3FF)
                )
            }

            // Right Panel: Morning Rituals
            Column(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight()
                    .padding(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.3f))
                    .padding(32.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.morning_ritual_checklist),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                if (tasks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(id = R.string.good_morning_vietnam),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(tasks) { task ->
                            TvTaskItem(task)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvTaskItem(task: TaskEntity) {
    Surface(
        onClick = {},
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (task.isCompleted) Color(0xFF1A1A2E) else Color(0xFF252545),
            contentColor = Color.White
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.Info,
                contentDescription = null,
                tint = if (task.isCompleted) Color(0xFF2E7D32) else Color(0xFF7A60FF)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (task.isCompleted) FontWeight.Normal else FontWeight.Medium,
                color = if (task.isCompleted) Color.Gray else Color.White
            )
        }
    }
}
