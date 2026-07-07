package com.mornshield.mobile

import android.app.Application
import com.mornshield.mobile.util.AdsHelper
import com.mornshield.mobile.util.RemoteConfigHelper

class MornShieldApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        Thread {
            try {
                // Initialize AdMob
                AdsHelper.initialize(this)
                
                // Initialize Remote Config
                RemoteConfigHelper.fetchAndActivate()
            } catch (e: Exception) {}
        }.start()
    }
}
