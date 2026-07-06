package com.mornshield.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mornshield.mobile.R
import com.mornshield.mobile.util.RemoteConfigHelper
import kotlinx.coroutines.delay

@Composable
fun PuzzleScreen(onPuzzleSolved: (Long) -> Unit) {
    var solution by remember { mutableStateOf("") }
    val startTime = remember { System.currentTimeMillis() }
    
    // Target word changes based on Remote Config difficulty
    val targetWord = remember { 
        when(RemoteConfigHelper.getPuzzleDifficulty()) {
            "hard" -> "SHIELD"
            "easy" -> "WAKE"
            else -> "RISE" 
        }
    }

    val gradientColors = listOf(Color(0xFF0F0C20), Color(0xFF05040B))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(gradientColors)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = stringResource(id = R.string.brain_wakeup_puzzle),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF7A60FF),
                letterSpacing = 2.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = stringResource(id = R.string.solve_to_disable),
                fontSize = 20.sp,
                fontWeight = FontWeight.Light,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Puzzle Grid
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                targetWord.forEachIndexed { index, char ->
                    val charAtPos = solution.getOrNull(index)?.toString() ?: ""
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (charAtPos.isNotEmpty()) Color(0xFF1F1A3A) else Color(0xFF0D0D1A))
                            .border(1.dp, if (charAtPos.isNotEmpty()) Color(0xFF7A60FF) else Color(0xFF2C2750), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = charAtPos.uppercase(),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Virtual Keyboard (Simulated for brevity)
            val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                alphabet.chunked(9).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { char ->
                            Box(
                                modifier = Modifier
                                    .size(width = 34.dp, height = 44.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF16132C))
                                    .clickable {
                                        if (solution.length < targetWord.length) {
                                            solution += char
                                            if (solution.uppercase() == targetWord.uppercase()) {
                                                val duration = System.currentTimeMillis() - startTime
                                                onPuzzleSolved(duration)
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(char.toString(), color = Color.White, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            TextButton(onClick = { solution = "" }) {
                Text(stringResource(id = R.string.reset_puzzle), color = Color(0xFFC0B3FF))
            }
        }
    }
}
