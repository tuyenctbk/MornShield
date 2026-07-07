package com.mornshield.mobile.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mornshield.mobile.R
import kotlinx.coroutines.launch

data class OnboardingStep(
    val title: String,
    val subtitle: String,
    val description: String
)

@Preview(showBackground = true, backgroundColor = 0xFF0C091A)
@Composable
fun PreviewOnboarding() {
    OnboardingScreen(
        onRequestPermissions = {},
        hasPermissions = false,
        onFinishOnboarding = {}
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onRequestPermissions: () -> Unit,
    hasPermissions: Boolean,
    onFinishOnboarding: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val steps = remember {
        listOf(
            OnboardingStep(
                title = context.getString(R.string.onboarding_title_1),
                subtitle = context.getString(R.string.onboarding_subtitle_1),
                description = context.getString(R.string.onboarding_desc_1)
            ),
            OnboardingStep(
                title = context.getString(R.string.onboarding_title_2),
                subtitle = context.getString(R.string.onboarding_subtitle_2),
                description = context.getString(R.string.onboarding_desc_2)
            ),
            OnboardingStep(
                title = context.getString(R.string.onboarding_title_3),
                subtitle = context.getString(R.string.onboarding_subtitle_3),
                description = context.getString(R.string.onboarding_desc_3)
            ),
            OnboardingStep(
                title = context.getString(R.string.onboarding_title_4),
                subtitle = context.getString(R.string.onboarding_subtitle_4),
                description = context.getString(R.string.onboarding_desc_4)
            )
        )
    }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { steps.size }
    )

    // Deep sensory-friendly dark theme gradient (HSL derived)
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F0C20), // Deep indigo
            Color(0xFF15102A), // Dark purple
            Color(0xFF0A0915)  // Near black
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top branding
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFF15102A))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(id = R.string.app_name).uppercase(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFC0B3FF),
                    letterSpacing = 4.sp
                )
            }

            // Dynamic Step Content (Swipeable Pager)
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                val step = steps[page]
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = step.title,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        lineHeight = 36.sp
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = step.subtitle,
                        fontSize = 15.sp,
                        color = Color(0xFFC0B3FF),
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = step.description,
                        fontSize = 14.sp,
                        color = Color(0xFFB0AFC0),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    if (page == 2) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    try {
                                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        })
                                    } catch (e: Exception) {
                                        context.startActivity(Intent(Settings.ACTION_SETTINGS))
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7A60FF)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(stringResource(id = R.string.grant_listener), color = Color.White)
                            }

                            Button(
                                onClick = {
                                    try {
                                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        })
                                    } catch (e: Exception) {
                                        context.startActivity(Intent(Settings.ACTION_SETTINGS))
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7A60FF)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(stringResource(id = R.string.grant_dnd_access), color = Color.White)
                            }
                        }
                    }

                    if (page == 3) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onRequestPermissions,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (hasPermissions) Color(0xFF2E7D32) else Color(0xFF7A60FF)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (hasPermissions) stringResource(id = R.string.permissions_granted) else stringResource(id = R.string.grant_system_perms),
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Bottom Navigation Indicators & Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Indicators
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(bottom = 32.dp)
                ) {
                    steps.forEachIndexed { index, _ ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(width = if (index == pagerState.currentPage) 24.dp else 8.dp, height = 8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (index == pagerState.currentPage) Color(0xFF7A60FF) else Color(0xFF423E5D))
                        )
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (pagerState.currentPage > 0) {
                        TextButton(onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }) {
                            Text(stringResource(id = R.string.back), color = Color(0xFFB0AFC0))
                        }
                    } else {
                        Spacer(modifier = Modifier.width(60.dp))
                    }

                    Button(
                        onClick = {
                            if (pagerState.currentPage < 3) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            } else {
                                onFinishOnboarding()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7A60FF)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .height(50.dp)
                            .width(150.dp)
                    ) {
                        Text(
                            text = if (pagerState.currentPage == 3) stringResource(id = R.string.get_started) else stringResource(id = R.string.next),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
