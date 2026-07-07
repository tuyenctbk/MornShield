package com.mornshield.mobile.audio

object BriefingTextFormatter {

    /**
     * Formats the text briefing for morning routines.
     */
    fun formatBriefing(weatherDesc: String, meetings: List<String>, tasks: List<String>): String {
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

        return "Good morning! $weatherText $meetingsText $tasksText Remember to breathe deeply and start your day focused."
    }
}
