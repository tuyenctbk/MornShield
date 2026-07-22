package com.mornshield.mobile.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class BriefingEngineTest {

    @Test
    fun testGetWeatherDescriptionFromCode() {
        assertEquals("clear sky", BriefingEngine.getWeatherDescriptionFromCode(0))
        assertEquals("partly cloudy", BriefingEngine.getWeatherDescriptionFromCode(1))
        assertEquals("partly cloudy", BriefingEngine.getWeatherDescriptionFromCode(2))
        assertEquals("partly cloudy", BriefingEngine.getWeatherDescriptionFromCode(3))
        assertEquals("foggy", BriefingEngine.getWeatherDescriptionFromCode(45))
        assertEquals("foggy", BriefingEngine.getWeatherDescriptionFromCode(48))
        assertEquals("drizzle", BriefingEngine.getWeatherDescriptionFromCode(51))
        assertEquals("drizzle", BriefingEngine.getWeatherDescriptionFromCode(53))
        assertEquals("drizzle", BriefingEngine.getWeatherDescriptionFromCode(55))
        assertEquals("rainy", BriefingEngine.getWeatherDescriptionFromCode(61))
        assertEquals("rainy", BriefingEngine.getWeatherDescriptionFromCode(63))
        assertEquals("rainy", BriefingEngine.getWeatherDescriptionFromCode(65))
        assertEquals("snowy", BriefingEngine.getWeatherDescriptionFromCode(71))
        assertEquals("snowy", BriefingEngine.getWeatherDescriptionFromCode(73))
        assertEquals("snowy", BriefingEngine.getWeatherDescriptionFromCode(75))
        assertEquals("rain showers", BriefingEngine.getWeatherDescriptionFromCode(80))
        assertEquals("rain showers", BriefingEngine.getWeatherDescriptionFromCode(81))
        assertEquals("rain showers", BriefingEngine.getWeatherDescriptionFromCode(82))
        assertEquals("thunderstorms", BriefingEngine.getWeatherDescriptionFromCode(95))
        assertEquals("thunderstorms", BriefingEngine.getWeatherDescriptionFromCode(96))
        assertEquals("thunderstorms", BriefingEngine.getWeatherDescriptionFromCode(99))
        
        // Default / Unknown code
        assertEquals("clear with a gentle breeze", BriefingEngine.getWeatherDescriptionFromCode(999))
        assertEquals("clear with a gentle breeze", BriefingEngine.getWeatherDescriptionFromCode(-1))
    }
}
