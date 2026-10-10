package com.gcg.wpchanger.worker

import android.content.Context
import android.os.PowerManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gcg.wpchanger.data.WallpaperManagerHelper
import com.gcg.wpchanger.data.WallpaperPreferences
import com.gcg.wpchanger.data.WallpaperRepository

class WallpaperChangeWorker(
    private val appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val preferences = WallpaperPreferences(appContext)
        val repository = WallpaperRepository(appContext)

        // If user has not enabled or has paused rotation (unless triggered manually)
        val isManualTrigger = tags.contains(WorkManagerScheduler.IMMEDIATE_WORK_TAG)
        val isActive = preferences.getActive()

        if (!isActive && !isManualTrigger) {
            return Result.success()
        }

        // Battery-not-low is already enforced via WorkManager's own Constraints (the OS won't
        // even dispatch this job while low), but Battery Saver has no Constraints equivalent —
        // check it ourselves and skip this cycle. The next periodic run will check again.
        val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
        if (powerManager?.isPowerSaveMode == true && !isManualTrigger) {
            return Result.success()
        }

        val lastId = preferences.getLastWallpaperId()
        val queue = preferences.getShuffleQueue()
        val stack = repository.activeStack(preferences)
        val pick = repository.pickNextRandomWallpaper(stack, lastId, queue)
        val nextWallpaper = pick.item
            ?: return Result.success() // No wallpapers found in the active stack

        val target = preferences.getTarget()

        val applyResult = WallpaperManagerHelper.applyWallpaper(
            context = appContext,
            wallpaperFile = nextWallpaper.file,
            target = target,
        )

        return if (applyResult.isSuccess) {
            preferences.recordWallpaperChange(
                id = nextWallpaper.id,
                name = nextWallpaper.name,
                timestamp = System.currentTimeMillis(),
            )
            preferences.setShuffleQueue(pick.queue)
            if (preferences.getNotifyOnChange()) {
                WallpaperNotifier.notifyWallpaperChanged(appContext, nextWallpaper)
            }
            Result.success()
        } else {
            if (runAttemptCount < 2) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
