package com.mornshield.mobile.util

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.play.core.review.ReviewManagerFactory

object RatingHelper {

    private const val PREFS_NAME = "mornshield_rating_prefs"
    private const val KEY_RITUALS_DONE = "rituals_done"
    private const val KEY_INSTALL_TIME = "install_time"
    private const val KEY_LAST_PROMPT_TIME = "last_prompt_time"
    private const val KEY_HAS_SHOWN_REVIEW = "has_shown_review"

    fun recordRitualCompletion(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val count = prefs.getInt(KEY_RITUALS_DONE, 0) + 1
        
        val editor = prefs.edit()
        editor.putInt(KEY_RITUALS_DONE, count)
        if (prefs.getLong(KEY_INSTALL_TIME, 0L) == 0L) {
            editor.putLong(KEY_INSTALL_TIME, System.currentTimeMillis())
        }
        editor.apply()
    }

    /**
     * Triggers the review flow smartly when the user is likely to rate highly.
     * Checks install age, completions, and rate-limits review prompt requests.
     * Prevents prompting while Morning Shield is active.
     */
    fun triggerReviewFlow(activity: Activity, isShieldActive: Boolean, force: Boolean = false) {
        if (isShieldActive && !force) {
            Log.d("RatingHelper", "Shield is active. Deferring review prompt to protect focus.")
            return
        }

        val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val ritualsDone = prefs.getInt(KEY_RITUALS_DONE, 0)
        val hasShownReview = prefs.getBoolean(KEY_HAS_SHOWN_REVIEW, false)
        val installTime = prefs.getLong(KEY_INSTALL_TIME, System.currentTimeMillis())
        val lastPromptTime = prefs.getLong(KEY_LAST_PROMPT_TIME, 0L)

        val daysSinceInstall = (System.currentTimeMillis() - installTime) / (24 * 60 * 60 * 1000)
        val daysSinceLastPrompt = (System.currentTimeMillis() - lastPromptTime) / (24 * 60 * 60 * 1000)

        // Smart rules to prompt for review (satisfaction peaks):
        // 1. Install age is at least 3 days (habit-formed).
        // 2. Minimum 3 ritual completions (high engagement).
        // 3. Haven't shown review yet (or force=true).
        // 4. Rate-limit prompts to at most once every 30 days to avoid annoyance.
        val shouldShow = force || (
            !hasShownReview &&
            daysSinceInstall >= 3 &&
            ritualsDone >= 3 &&
            daysSinceLastPrompt >= 30
        )

        if (shouldShow) {
            val manager = ReviewManagerFactory.create(activity)
            val request = manager.requestReviewFlow()
            request.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val reviewInfo = task.result
                    manager.launchReviewFlow(activity, reviewInfo)
                    
                    val editor = prefs.edit()
                    editor.putLong(KEY_LAST_PROMPT_TIME, System.currentTimeMillis())
                    if (!force) {
                        editor.putBoolean(KEY_HAS_SHOWN_REVIEW, true)
                    }
                    editor.apply()
                } else {
                    Log.w("RatingHelper", "Review flow request failed")
                }
            }
        }
    }
}
