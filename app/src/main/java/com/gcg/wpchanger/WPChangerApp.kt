package com.gcg.wpchanger

import android.app.Application
import com.gcg.wpchanger.data.WallpaperPreferences
import com.gcg.wpchanger.worker.WorkManagerScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WPChangerApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Ensure scheduler is active if user previously enabled auto rotation
        val preferences = WallpaperPreferences(this)
        applicationScope.launch {
            if (preferences.getActive()) {
                val interval = preferences.getInterval()
                WorkManagerScheduler.schedulePeriodic(this@WPChangerApp, interval)
            }
        }
    }
}
