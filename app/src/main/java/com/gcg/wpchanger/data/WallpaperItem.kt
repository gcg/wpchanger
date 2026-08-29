package com.gcg.wpchanger.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

data class WallpaperItem(
    val id: String,
    val file: File,
    val name: String,
    val sizeBytes: Long,
    val addedTimestamp: Long,
) {
    /**
     * A shareable `content://` URI for this wallpaper, safe to hand to other apps via an Intent.
     * [file] lives in app-private storage, so a raw `file://` Uri would throw
     * FileUriExposedException on API 24+; this routes through the app's FileProvider instead.
     */
    fun contentUri(context: Context): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
