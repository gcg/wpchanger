package com.gcg.wpchanger.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.gcg.wpchanger.MainActivity
import com.gcg.wpchanger.R
import com.gcg.wpchanger.data.WallpaperItem
import com.gcg.wpchanger.data.WallpaperManagerHelper

/**
 * Posts a notification whenever [WallpaperChangeWorker] rotates the wallpaper in the background.
 * Manual "Change Wallpaper Now" taps don't go through here — the user is already looking at the
 * app in that case, so the in-app snackbar is enough.
 */
object WallpaperNotifier {
    const val CHANNEL_ID = "wallpaper_rotation"
    private const val NOTIFICATION_ID = 1001
    private const val THUMBNAIL_SIZE_PX = 256

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Wallpaper rotation",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Lets you know when your wallpaper changes in the background"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    suspend fun notifyWallpaperChanged(context: Context, wallpaper: WallpaperItem) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val thumbnail = WallpaperManagerHelper.decodeThumbnail(wallpaper.file, THUMBNAIL_SIZE_PX)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_refresh)
            .setContentTitle("Wallpaper changed")
            .setContentText(wallpaper.name)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (thumbnail != null) {
            builder
                .setLargeIcon(thumbnail)
                .setStyle(NotificationCompat.BigPictureStyle().bigPicture(thumbnail))
        }

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            // Permission could theoretically be revoked between the check above and here.
            e.printStackTrace()
        }
    }
}
