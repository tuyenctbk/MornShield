package com.mornshield.mobile.audio

import android.content.Context
import android.provider.CalendarContract
import android.speech.tts.TextToSpeech
import android.util.Log
import com.mornshield.mobile.util.RemoteConfigHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.Calendar
import java.util.Locale

interface WeatherApi {
    @GET("weather")
    suspend fun getCurrentWeather(
        @Query("q") city: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): WeatherResponse
}

data class WeatherResponse(
    val main: Main,
    val weather: List<WeatherInfo>
)

data class Main(val temp: Float)
data class WeatherInfo(val description: String)

class BriefingEngine(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var pendingText: String? = null

    private val weatherApi = Retrofit.Builder()
        .baseUrl("https://api.openweathermap.org/data/2.5/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(WeatherApi::class.java)

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("BriefingEngine", "Language US is not supported or missing data.")
            } else {
                isInitialized = true
                Log.d("BriefingEngine", "TTS Initialized successfully")
                
                // Configure default speech settings from Remote Config
                val pitch = RemoteConfigHelper.getBriefingDefaultPitch()
                tts?.setPitch(pitch)
                tts?.setSpeechRate(0.95f) // Slightly slower for peaceful morning vibes

                pendingText?.let {
                    speak(it)
                    pendingText = null
                }
            }
        } else {
            Log.e("BriefingEngine", "Initialization of TextToSpeech failed.")
        }
    }

    fun speak(text: String) {
        if (isInitialized) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "MornShieldBriefing")
        } else {
            pendingText = text
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        isInitialized = false
    }

    /**
     * Synthesizes and plays the morning briefing audio.
     */
    fun playMorningBrief(city: String, tasks: List<String>) {
        CoroutineScope(Dispatchers.IO).launch {
            val weatherDesc = try {
                val apiKey = RemoteConfigHelper.getWeatherApiKey()
                if (city.isNotEmpty() && apiKey != "REPLACE_ME") {
                    val response = weatherApi.getCurrentWeather(city, apiKey) 
                    "${response.main.temp.toInt()} degrees, ${response.weather.first().description}"
                } else {
                    // Default peaceful morning descriptions
                    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                    if (hour < 8) "cool and peaceful" else "bright and energetic"
                }
            } catch (e: Exception) {
                "clear with a gentle breeze"
            }

            val meetings = getUpcomingMeetings()
            
            val weatherText = "The weather today is $weatherDesc."

            val meetingsText = if (meetings.isNotEmpty()) {
                "You have ${meetings.size} meetings scheduled today. Your first one is ${meetings.first()}."
            } else {
                "Your calendar is clear this morning, with no meetings scheduled before noon."
            }

            val tasksText = if (tasks.isNotEmpty()) {
                "You have ${tasks.size} tasks on your morning ritual list, including ${tasks.take(2).joinToString(" and ")}."
            } else {
                "You have completed all morning checklist items."
            }

            val briefScript = "Good morning! $weatherText $meetingsText $tasksText Remember to breathe deeply and start your day focused."
            
            withContext(Dispatchers.Main) {
                speak(briefScript)
            }
        }
    }

    private fun getUpcomingMeetings(): List<String> {
        val meetings = mutableListOf<String>()
        try {
            val projection = arrayOf(
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DTSTART
            )
            
            val now = System.currentTimeMillis()
            val endOfDay = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
            }.timeInMillis

            val selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?"
            val selectionArgs = arrayOf(now.toString(), endOfDay.toString())

            val cursor = context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${CalendarContract.Events.DTSTART} ASC"
            )

            cursor?.use {
                val titleIndex = it.getColumnIndex(CalendarContract.Events.TITLE)
                while (it.moveToNext() && meetings.size < 3) {
                    meetings.add(it.getString(titleIndex))
                }
            }
        } catch (e: SecurityException) {
            Log.w("BriefingEngine", "Calendar permission not granted")
        } catch (e: Exception) {
            Log.e("BriefingEngine", "Error querying calendar: ${e.message}")
        }
        return meetings
    }
}
