package com.mornshield.wear.sleep

import android.content.Context
import android.util.Log

enum class SleepStage {
    DEEP, REM, AWAKE
}

class SleepMonitor(private val context: Context, private val onStageChanged: (SleepStage) -> Unit) {
    fun start() {
        Log.d("SleepMonitor", "MornShield Sleep Tracking Started")
    }
    fun stop() {}
}
