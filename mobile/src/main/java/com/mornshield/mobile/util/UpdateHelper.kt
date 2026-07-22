package com.mornshield.mobile.util

import android.app.Activity
import android.util.Log
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability

object UpdateHelper {
    
    private const val TAG = "UpdateHelper"
    const val REQUEST_CODE_UPDATE = 1001
    
    /**
     * Checks for updates smartly using priority and version staleness,
     * avoiding interruptions if the user is in their distraction-free morning shield.
     */
    fun checkForUpdates(activity: Activity, isShieldActive: Boolean, onUpdateChecked: (Boolean) -> Unit = {}) {
        if (isShieldActive) {
            Log.d(TAG, "Shield active. Deferring update check to avoid morning interruption.")
            onUpdateChecked(false)
            return
        }

        val appUpdateManager = AppUpdateManagerFactory.create(activity)
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            val availability = appUpdateInfo.updateAvailability()
            if (availability == UpdateAvailability.UPDATE_AVAILABLE) {
                // Retrieve update priority (0 to 5, where 5 is critical/immediate)
                val priority = appUpdateInfo.updatePriority()
                // Retrieve staleness (number of days since update was published on Play Store)
                val staleness = appUpdateInfo.clientVersionStalenessDays() ?: 0
                
                // Smart rules for updates:
                // - Force immediate update if priority is 5, or if priority >= 3 and has been stale for 3+ days.
                val shouldForceImmediate = priority >= 5 || (priority >= 3 && staleness >= 3)
                
                if (shouldForceImmediate && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                    try {
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            activity,
                            AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
                            REQUEST_CODE_UPDATE
                        )
                        onUpdateChecked(true)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to start immediate update flow", e)
                        onUpdateChecked(false)
                    }
                } else if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                    // Only show flexible prompt for normal/minor updates if update is stale for at least 1 day
                    if (priority > 0 && staleness >= 1) {
                        try {
                            appUpdateManager.startUpdateFlowForResult(
                                appUpdateInfo,
                                activity,
                                AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
                                REQUEST_CODE_UPDATE
                            )
                            onUpdateChecked(true)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to start flexible update flow", e)
                            onUpdateChecked(false)
                        }
                    } else {
                        onUpdateChecked(false)
                    }
                } else {
                    onUpdateChecked(false)
                }
            } else {
                onUpdateChecked(false)
            }
        }.addOnFailureListener { exception ->
            Log.w(TAG, "Failed to check for updates", exception)
            onUpdateChecked(false)
        }
    }
}
