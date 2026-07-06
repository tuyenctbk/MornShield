package com.mornshield.mobile.util

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.play.core.review.ReviewManagerFactory

object RatingHelper {

    private const val PREFS_NAME = "mornshield_rating_prefs"
    private const val KEY_RITUALS_DONE = "rituals_done"

    fun recordRitualCompletion(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val count = prefs.getInt(KEY_RITUALS_DONE, 0) + 1
        prefs.edit().putInt(KEY_RITUALS_DONE, count).apply()
    }

    fun triggerReviewFlow(activity: Activity, force: Boolean = false) {
        val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val ritualsDone = prefs.getInt(KEY_RITUALS_DONE, 0)

        // Only trigger after 3 successful ritual completions, or if forced via settings
        if (ritualsDone >= 3 || force) {
            val manager = ReviewManagerFactory.create(activity)
            val request = manager.requestReviewFlow()
            request.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val reviewInfo = task.result
                    manager.launchReviewFlow(activity, reviewInfo)
                } else {
                    Log.w("RatingHelper", "Review flow request failed")
                }
            }
        }
    }
}
