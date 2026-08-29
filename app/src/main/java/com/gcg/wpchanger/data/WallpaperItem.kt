package com.gcg.wpchanger.data

import android.net.Uri
import java.io.File

data class WallpaperItem(
    val id: String,
    val file: File,
    val name: String,
    val sizeBytes: Long,
    val addedTimestamp: Long,
) {
    val uri: Uri
        get() = Uri.fromFile(file)
}
