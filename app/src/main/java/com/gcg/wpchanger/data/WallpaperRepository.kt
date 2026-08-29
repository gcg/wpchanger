package com.gcg.wpchanger.data

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class WallpaperRepository(private val context: Context) {

    private val wallpaperDir: File by lazy {
        File(context.filesDir, "wallpapers").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    private val _wallpapers = MutableStateFlow<List<WallpaperItem>>(emptyList())
    val wallpapers: StateFlow<List<WallpaperItem>> = _wallpapers.asStateFlow()

    suspend fun refresh() = withContext(Dispatchers.IO) {
        val items = loadWallpaperItems()
        _wallpapers.value = items
    }

    private fun loadWallpaperItems(): List<WallpaperItem> {
        val files = wallpaperDir.listFiles() ?: return emptyList()
        return files
            .filter { it.isFile && isImageFile(it.name) }
            .map { file ->
                val id = file.nameWithoutExtension
                val cleanName = parseDisplayName(file.name)
                WallpaperItem(
                    id = id,
                    file = file,
                    name = cleanName,
                    sizeBytes = file.length(),
                    addedTimestamp = file.lastModified(),
                )
            }
            .sortedByDescending { it.addedTimestamp }
    }

    suspend fun importFromUris(uris: List<Uri>): Int = withContext(Dispatchers.IO) {
        var successCount = 0
        for (uri in uris) {
            if (saveUriToFile(uri)) {
                successCount++
            }
        }
        if (successCount > 0) {
            refresh()
        }
        successCount
    }

    suspend fun importFromFolder(treeUri: Uri): Int = withContext(Dispatchers.IO) {
        var count = 0
        try {
            val contentResolver = context.contentResolver
            val treeDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                treeUri,
                treeDocumentId,
            )

            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
            )

            val cursor: Cursor? = contentResolver.query(
                childrenUri,
                projection,
                null,
                null,
                null,
            )

            cursor?.use { c ->
                val idIndex = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val mimeIndex = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val nameIndex = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)

                while (c.moveToNext()) {
                    val docId = c.getString(idIndex)
                    val mimeType = c.getString(mimeIndex)
                    val displayName = c.getString(nameIndex) ?: "photo.jpg"

                    if (mimeType != null && (mimeType.startsWith("image/") || isImageFile(displayName))) {
                        val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                        if (saveUriToFile(documentUri, displayName)) {
                            count++
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (count > 0) {
            refresh()
        }
        count
    }

    private fun saveUriToFile(uri: Uri, preferredName: String? = null): Boolean {
        return try {
            val resolver = context.contentResolver
            val originalName = preferredName ?: queryDisplayName(uri) ?: "wallpaper.jpg"
            val extension = getExtension(originalName, uri)
            val uniqueId = "wp_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
            val sanitizedName = originalName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val targetFileName = "${uniqueId}__$sanitizedName.$extension"
            val targetFile = File(wallpaperDir, targetFileName)

            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            targetFile.exists() && targetFile.length() > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteWallpaper(id: String): Boolean = withContext(Dispatchers.IO) {
        val files = wallpaperDir.listFiles() ?: return@withContext false
        val fileToDelete = files.find { it.nameWithoutExtension.startsWith(id) || it.name.startsWith(id) }
        val deleted = fileToDelete?.delete() ?: false
        if (deleted) {
            refresh()
        }
        deleted
    }

    suspend fun clearAllWallpapers(): Int = withContext(Dispatchers.IO) {
        val files = wallpaperDir.listFiles() ?: return@withContext 0
        var deletedCount = 0
        for (f in files) {
            if (f.isFile && f.delete()) {
                deletedCount++
            }
        }
        refresh()
        deletedCount
    }

    suspend fun pickNextRandomWallpaper(lastId: String?): WallpaperItem? = withContext(Dispatchers.IO) {
        val currentList = loadWallpaperItems()
        if (currentList.isEmpty()) return@withContext null
        if (currentList.size == 1) return@withContext currentList.first()

        val candidates = if (!lastId.isNullOrBlank()) {
            currentList.filter { it.id != lastId }
        } else {
            currentList
        }

        val pool = if (candidates.isNotEmpty()) candidates else currentList
        pool.randomOrNull()
    }

    private fun queryDisplayName(uri: Uri): String? {
        return try {
            var name: String? = null
            if (uri.scheme == "content") {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index != -1) {
                            name = it.getString(index)
                        }
                    }
                }
            }
            name ?: uri.lastPathSegment
        } catch (e: Exception) {
            null
        }
    }

    private fun getExtension(filename: String, uri: Uri): String {
        val dotIndex = filename.lastIndexOf('.')
        if (dotIndex != -1 && dotIndex < filename.length - 1) {
            return filename.substring(dotIndex + 1).lowercase()
        }
        val mime = context.contentResolver.getType(uri)
        if (mime != null) {
            val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
            if (!ext.isNullOrBlank()) return ext.lowercase()
        }
        return "jpg"
    }

    private fun parseDisplayName(fileName: String): String {
        val separatorIndex = fileName.indexOf("__")
        return if (separatorIndex != -1 && separatorIndex + 2 < fileName.length) {
            val afterSeparator = fileName.substring(separatorIndex + 2)
            val dotIndex = afterSeparator.lastIndexOf('.')
            if (dotIndex != -1) afterSeparator.substring(0, dotIndex) else afterSeparator
        } else {
            val dotIndex = fileName.lastIndexOf('.')
            if (dotIndex != -1) fileName.substring(0, dotIndex) else fileName
        }
    }

    private fun isImageFile(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
            lower.endsWith(".png") || lower.endsWith(".webp") ||
            lower.endsWith(".heic") || lower.endsWith(".bmp")
    }
}
