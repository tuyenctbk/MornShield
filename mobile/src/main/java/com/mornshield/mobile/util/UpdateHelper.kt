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
    
    fun checkForUpdates(activity: Activity, onUpdateChecked: (Boolean) -> Unit = {}) {
        val appUpdateManager = AppUpdateManagerFactory.create(activity)
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
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
            } else if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
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
        }.addOnFailureListener { exception ->
            Log.w(TAG, "Failed to check for updates", exception)
            onUpdateChecked(false)
        }
    }
}
