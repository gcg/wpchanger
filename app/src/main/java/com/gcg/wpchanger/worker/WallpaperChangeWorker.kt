package com.gcg.wpchanger.worker

import android.content.Context
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

        val lastId = preferences.getLastWallpaperId()
        val queue = preferences.getShuffleQueue()
        val pick = repository.pickNextRandomWallpaper(lastId, queue)
        val nextWallpaper = pick.item
            ?: return Result.success() // No wallpapers found in pool

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
