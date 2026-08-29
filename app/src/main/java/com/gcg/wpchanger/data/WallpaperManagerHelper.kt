package com.gcg.wpchanger.data

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object WallpaperManagerHelper {

    suspend fun applyWallpaper(
        context: Context,
        wallpaperFile: File,
        target: WallpaperTarget,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            if (!wallpaperManager.isSetWallpaperAllowed) {
                return@withContext Result.failure(IllegalStateException("Setting wallpaper is not allowed on this device or profile."))
            }

            // Prefer the wallpaper system's own desired size: it's typically wider than the
            // screen to account for home-screen parallax scrolling, so decoding to screen size
            // alone can leave the wallpaper looking cropped or soft while scrolling. Some
            // devices report -1/0 before any wallpaper has ever been set, so fall back to the
            // screen's own metrics in that case.
            val metrics = context.resources.displayMetrics
            val fallbackWidth = metrics.widthPixels.coerceAtLeast(1080)
            val fallbackHeight = metrics.heightPixels.coerceAtLeast(1920)
            val reqWidth = wallpaperManager.desiredMinimumWidth.takeIf { it > 0 } ?: fallbackWidth
            val reqHeight = wallpaperManager.desiredMinimumHeight.takeIf { it > 0 } ?: fallbackHeight

            val bitmap = decodeSampledBitmap(wallpaperFile, reqWidth, reqHeight)
                ?: return@withContext Result.failure(IllegalArgumentException("Failed to decode image from ${wallpaperFile.name}"))

            val orientedBitmap = fixOrientation(wallpaperFile, bitmap)

            wallpaperManager.setBitmap(
                orientedBitmap,
                null,
                true,
                target.flag,
            )
            orientedBitmap.recycle()

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun decodeSampledBitmap(file: File, reqWidth: Int, reqHeight: Int): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(file.absolutePath, options)

        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        return BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
    }

    private fun fixOrientation(file: File, bitmap: Bitmap): Bitmap {
        return try {
            val exif = ExifInterface(file.absolutePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                else -> return bitmap
            }
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated != bitmap) {
                bitmap.recycle()
            }
            rotated
        } catch (e: Exception) {
            bitmap
        }
    }
}
