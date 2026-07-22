package com.mornshield.mobile.audio

import android.content.Context
import android.provider.CalendarContract
import android.speech.tts.TextToSpeech
import android.util.Log
import com.mornshield.mobile.util.RemoteConfigHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
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

// --- Open-Meteo Failover API Definition ---
interface GeocodingApi {
    @GET("v1/search")
    suspend fun search(
        @Query("name") city: String,
        @Query("count") count: Int = 1
    ): GeocodingResponse
}

data class GeocodingResponse(val results: List<GeocodingResult>?)
data class GeocodingResult(val latitude: Double, val longitude: Double)

interface OpenMeteoApi {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current_weather") current: Boolean = true
    ): OpenMeteoResponse
}

data class OpenMeteoResponse(val current_weather: CurrentWeather)
data class CurrentWeather(val temperature: Float, val weathercode: Int)

// --- Gemini Content Generation API Definition ---
interface GeminiApi {
    @POST("v1beta/models/gemini-1.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

data class GeminiRequest(val contents: List<GeminiContent>)
data class GeminiContent(val parts: List<GeminiPart>)
data class GeminiPart(val text: String)

data class GeminiResponse(val candidates: List<GeminiCandidate>?)
data class GeminiCandidate(val content: GeminiContentInfo?)
data class GeminiContentInfo(val parts: List<GeminiPartInfo>?)
data class GeminiPartInfo(val text: String?)

class BriefingEngine(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var pendingText: String? = null
    
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val weatherApi = Retrofit.Builder()
        .baseUrl("https://api.openweathermap.org/data/2.5/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(WeatherApi::class.java)

    private val geocodingApi = Retrofit.Builder()
        .baseUrl("https://geocoding-api.open-meteo.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GeocodingApi::class.java)

    private val openMeteoApi = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OpenMeteoApi::class.java)

    private val geminiApi = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GeminiApi::class.java)

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
        scope.cancel()
    }

    companion object {
        /**
         * Translates Open-Meteo weather code to a readable format.
         */
        fun getWeatherDescriptionFromCode(code: Int): String {
            return when (code) {
                0 -> "clear sky"
                1, 2, 3 -> "partly cloudy"
                45, 48 -> "foggy"
                51, 53, 55 -> "drizzle"
                61, 63, 65 -> "rainy"
                71, 73, 75 -> "snowy"
                80, 81, 82 -> "rain showers"
                95, 96, 99 -> "thunderstorms"
                else -> "clear with a gentle breeze"
            }
        }
    }

    /**
     * Retrieve weather description from available APIs (OpenWeatherMap with Open-Meteo fallback)
     */
    private suspend fun getWeatherData(city: String): String {
        val openWeatherKey = RemoteConfigHelper.getWeatherApiKey()
        if (city.isNotEmpty() && openWeatherKey.isNotEmpty() && openWeatherKey != "REPLACE_ME") {
            try {
                val response = weatherApi.getCurrentWeather(city, openWeatherKey) 
                return "${response.main.temp.toInt()} degrees, ${response.weather.first().description}"
            } catch (e: Exception) {
                Log.e("BriefingEngine", "OpenWeatherMap failed: ${e.message}. Trying Open-Meteo.")
            }
        }

        if (city.isNotEmpty()) {
            try {
                val geoResponse = geocodingApi.search(city)
                val result = geoResponse.results?.firstOrNull()
                if (result != null) {
                    val forecast = openMeteoApi.getForecast(result.latitude, result.longitude)
                    val temp = forecast.current_weather.temperature
                    val desc = getWeatherDescriptionFromCode(forecast.current_weather.weathercode)
                    return "${temp.toInt()} degrees, $desc"
                }
            } catch (e: Exception) {
                Log.e("BriefingEngine", "Open-Meteo failover failed: ${e.message}")
            }
        }

        // Fallback to default values based on time of day
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return if (hour < 8) "cool and peaceful" else "bright and energetic"
    }

    /**
     * Requests a personalized greeting prompt using the Gemini model.
     */
    private suspend fun generateGeminiBriefing(
        apiKey: String,
        weatherDesc: String,
        meetings: List<String>,
        tasks: List<String>
    ): String {
        val prompt = "Create a peaceful, encouraging morning briefing under 100 words using this info: Weather is $weatherDesc. Upcoming calendar events: ${meetings.joinToString()}. Tasks to complete: ${tasks.joinToString()}."
        try {
            val response = geminiApi.generateContent(
                apiKey = apiKey,
                request = GeminiRequest(
                    contents = listOf(
                        GeminiContent(
                            parts = listOf(
                                GeminiPart(text = prompt)
                            )
                        )
                    )
                )
            )
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!text.isNullOrEmpty()) {
                return text.trim()
            }
        } catch (e: Exception) {
            Log.e("BriefingEngine", "Gemini API failed: ${e.message}. Falling back to standard BriefingTextFormatter.")
        }
        return BriefingTextFormatter.formatBriefing(weatherDesc, meetings, tasks)
    }

    /**
     * Synthesizes and plays the morning briefing audio.
     */
    fun playMorningBrief(city: String, tasks: List<String>) {
        scope.launch {
            val weatherDesc = getWeatherData(city)
            val meetings = getUpcomingMeetings()
            
            val geminiKey = RemoteConfigHelper.getGeminiApiKey()
            val briefScript = if (geminiKey.isNotEmpty() && geminiKey != "REPLACE_ME") {
                generateGeminiBriefing(geminiKey, weatherDesc, meetings, tasks)
            } else {
                BriefingTextFormatter.formatBriefing(weatherDesc, meetings, tasks)
            }
            
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
