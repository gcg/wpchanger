package com.gcg.wpchanger.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.gcg.wpchanger.data.TimerInterval

object WorkManagerScheduler {
    const val UNIQUE_WORK_NAME = "wallpaper_rotator_periodic_work"
    const val UNIQUE_IMMEDIATE_WORK_NAME = "wallpaper_rotator_immediate_work"
    const val IMMEDIATE_WORK_TAG = "wallpaper_rotator_immediate"

    // Skip (and auto-retry once resolved) a scheduled rotation while the battery is critically
    // low — decoding + setting a wallpaper is unnecessary work exactly when it matters least.
    private val periodicConstraints = Constraints.Builder()
        .setRequiresBatteryNotLow(true)
        .build()

    fun schedulePeriodic(context: Context, interval: TimerInterval) {
        val workRequest = PeriodicWorkRequestBuilder<WallpaperChangeWorker>(
            interval.duration,
            interval.timeUnit,
        )
            .setConstraints(periodicConstraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest,
        )
    }

    fun cancelPeriodic(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    fun triggerImmediate(context: Context) {
        val immediateRequest = OneTimeWorkRequestBuilder<WallpaperChangeWorker>()
            .addTag(IMMEDIATE_WORK_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            immediateRequest,
        )
    }
}
