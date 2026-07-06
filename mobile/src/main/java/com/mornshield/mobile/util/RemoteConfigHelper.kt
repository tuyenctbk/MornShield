package com.mornshield.mobile.util

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

object RemoteConfigHelper {

    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

    init {
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600)
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(mapOf(
            "briefing_pitch" to 1.0f,
            "puzzle_difficulty" to "normal",
            "weather_api_key" to "REPLACE_ME"
        ))
    }

    fun fetchAndActivate() {
        remoteConfig.fetchAndActivate()
    }

    fun getBriefingDefaultPitch(): Float = remoteConfig.getDouble("briefing_pitch").toFloat()
    
    fun getPuzzleDifficulty(): String = remoteConfig.getString("puzzle_difficulty")

    fun getWeatherApiKey(): String = remoteConfig.getString("weather_api_key")
}
