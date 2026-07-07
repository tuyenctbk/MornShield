package com.mornshield.mobile.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class BriefingTextFormatterTest {

    @Test
    fun testFormatBriefing_withNoMeetingsAndNoTasks() {
        val weather = "sunny and warm"
        val meetings = emptyList<String>()
        val tasks = emptyList<String>()

        val expected = "Good morning! The weather today is sunny and warm. " +
                "Your calendar is clear this morning, with no meetings scheduled before noon. " +
                "You have completed all morning checklist items. " +
                "Remember to breathe deeply and start your day focused."

        val actual = BriefingTextFormatter.formatBriefing(weather, meetings, tasks)
        assertEquals(expected, actual)
    }

    @Test
    fun testFormatBriefing_withMeetingsAndTasks() {
        val weather = "cloudy"
        val meetings = listOf("Standup Meeting", "1-on-1 with manager")
        val tasks = listOf("Stretch", "Drink water", "Read book")

        val expected = "Good morning! The weather today is cloudy. " +
                "You have 2 meetings scheduled today. Your first one is Standup Meeting. " +
                "You have 3 tasks on your morning ritual list, including Stretch and Drink water. " +
                "Remember to breathe deeply and start your day focused."

        val actual = BriefingTextFormatter.formatBriefing(weather, meetings, tasks)
        assertEquals(expected, actual)
    }
}
