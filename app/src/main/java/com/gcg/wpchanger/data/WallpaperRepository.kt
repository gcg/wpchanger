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
import java.security.MessageDigest
import java.util.UUID

enum class ImportOutcome {
    SAVED,
    DUPLICATE,
    UNSUPPORTED,
    FAILED,
}

data class ImportSummary(
    val saved: Int = 0,
    val duplicates: Int = 0,
    val unsupported: Int = 0,
    val failed: Int = 0,
) {
    val total: Int get() = saved + duplicates + unsupported + failed
}

data class ShufflePickResult(val item: WallpaperItem?, val queue: List<String>)

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

    // Cache of (file size -> content hashes) for cheap duplicate detection across imports.
    // Invalidated whenever the on-disk contents change outside of a single import batch.
    private var hashIndexCache: MutableMap<Long, MutableSet<String>>? = null

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

    suspend fun importFromUris(uris: List<Uri>): ImportSummary = withContext(Dispatchers.IO) {
        val hashIndex = ensureHashIndex()
        var summary = ImportSummary()
        for (uri in uris) {
            summary = summary.plus(saveUriToFile(uri, hashIndex = hashIndex))
        }
        if (summary.saved > 0) {
            refresh()
        }
        summary
    }

    suspend fun importFromFolder(treeUri: Uri): ImportSummary = withContext(Dispatchers.IO) {
        var summary = ImportSummary()
        try {
            val hashIndex = ensureHashIndex()
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
                        summary = summary.plus(saveUriToFile(documentUri, displayName, hashIndex))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (summary.saved > 0) {
            refresh()
        }
        summary
    }

    private fun ImportSummary.plus(outcome: ImportOutcome): ImportSummary = when (outcome) {
        ImportOutcome.SAVED -> copy(saved = saved + 1)
        ImportOutcome.DUPLICATE -> copy(duplicates = duplicates + 1)
        ImportOutcome.UNSUPPORTED -> copy(unsupported = unsupported + 1)
        ImportOutcome.FAILED -> copy(failed = failed + 1)
    }

    private fun saveUriToFile(
        uri: Uri,
        preferredName: String? = null,
        hashIndex: MutableMap<Long, MutableSet<String>>,
    ): ImportOutcome {
        val originalName = preferredName ?: queryDisplayName(uri) ?: "wallpaper.jpg"
        val extension = getExtension(originalName, uri)
        if (!isSupportedExtension(extension)) {
            return ImportOutcome.UNSUPPORTED
        }

        val resolver = context.contentResolver
        val uniqueId = "wp_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
        val sanitizedName = originalName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        val targetFileName = "${uniqueId}__$sanitizedName.$extension"
        val targetFile = File(wallpaperDir, targetFileName)

        return try {
            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }

            if (!targetFile.exists() || targetFile.length() <= 0) {
                targetFile.delete()
                return ImportOutcome.FAILED
            }

            val hash = hashFile(targetFile)
            val existingHashesForSize = hashIndex.getOrPut(targetFile.length()) { mutableSetOf() }
            if (!existingHashesForSize.add(hash)) {
                // Same size + same content hash as something already in the pool: skip the copy.
                targetFile.delete()
                return ImportOutcome.DUPLICATE
            }

            ImportOutcome.SAVED
        } catch (e: Exception) {
            e.printStackTrace()
            targetFile.delete()
            ImportOutcome.FAILED
        }
    }

    suspend fun deleteWallpaper(id: String): Boolean = withContext(Dispatchers.IO) {
        val files = wallpaperDir.listFiles() ?: return@withContext false
        val fileToDelete = files.find { it.nameWithoutExtension == id }
        val deleted = fileToDelete?.delete() ?: false
        if (deleted) {
            invalidateHashIndex()
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
        invalidateHashIndex()
        refresh()
        deletedCount
    }

    /**
     * Picks the next wallpaper using a shuffle bag: every photo in the pool is shown once before
     * any of them repeat, instead of a fresh independent random pick each time (which can favor
     * the same handful of photos while starving the rest of the pool).
     */
    suspend fun pickNextRandomWallpaper(lastId: String?, queue: List<String>): ShufflePickResult =
        withContext(Dispatchers.IO) {
            val currentList = loadWallpaperItems()
            if (currentList.isEmpty()) return@withContext ShufflePickResult(null, emptyList())

            val poolIds = currentList.map { it.id }
            val pick = ShuffleBag.pickNext(poolIds, queue, lastId)
            val item = currentList.find { it.id == pick.id }
            ShufflePickResult(item, pick.remainingQueue)
        }

    private fun ensureHashIndex(): MutableMap<Long, MutableSet<String>> =
        hashIndexCache ?: buildHashIndex().also { hashIndexCache = it }

    private fun invalidateHashIndex() {
        hashIndexCache = null
    }

    private fun buildHashIndex(): MutableMap<Long, MutableSet<String>> {
        val index = mutableMapOf<Long, MutableSet<String>>()
        val files = wallpaperDir.listFiles() ?: return index
        for (file in files) {
            if (file.isFile) {
                index.getOrPut(file.length()) { mutableSetOf() }.add(hashFile(file))
            }
        }
        return index
    }

    private fun hashFile(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
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
        val extension = name.substringAfterLast('.', missingDelimiterValue = "")
        return extension.isNotEmpty() && isSupportedExtension(extension)
    }

    private fun isSupportedExtension(extension: String): Boolean = extension.lowercase() in SUPPORTED_EXTENSIONS

    companion object {
        private val SUPPORTED_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "heic", "bmp")
    }
}
