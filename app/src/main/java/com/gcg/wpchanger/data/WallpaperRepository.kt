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
import java.util.concurrent.ConcurrentHashMap

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

    // Each subfolder is a stack (see WallpaperStack); photos live inside their stack's folder.
    private val rootDir: File by lazy {
        File(context.filesDir, "wallpapers").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    // Deliberately doesn't create the folder: a stale name (just deleted or renamed) must not resurrect it.
    private fun stackDir(stack: String): File = File(rootDir, stack)

    private val _wallpapers = MutableStateFlow<List<WallpaperItem>>(emptyList())
    val wallpapers: StateFlow<List<WallpaperItem>> = _wallpapers.asStateFlow()

    private val _stacks = MutableStateFlow<List<WallpaperStack>>(emptyList())
    val stacks: StateFlow<List<WallpaperStack>> = _stacks.asStateFlow()

    // Per-stack cache of (file size -> content hashes) for cheap duplicate detection across imports.
    // Duplicates are only checked within a stack: the same photo may belong to several stacks.
    // Invalidated whenever the on-disk contents change outside of a single import batch.
    private val hashIndexCache = ConcurrentHashMap<String, MutableMap<Long, MutableSet<String>>>()

    /**
     * Reloads the stack list and the photos of [stack] (the one shown in the UI). Mutating calls
     * below don't refresh on their own; the caller reloads whichever stack is active by then.
     */
    suspend fun refresh(stack: String) = withContext(Dispatchers.IO) {
        _stacks.value = loadStacks()
        _wallpapers.value = loadWallpaperItems(stack)
    }

    private fun stackNames(): List<String> =
        rootDir.listFiles()
            ?.filter { it.isDirectory }
            ?.map { it.name }
            ?.sortedWith(String.CASE_INSENSITIVE_ORDER)
            ?: emptyList()

    private fun loadStacks(): List<WallpaperStack> = stackNames().map { name ->
        WallpaperStack(name, File(rootDir, name).listFiles()?.count { it.isFile && isImageFile(it.name) } ?: 0)
    }

    /**
     * Returns [preferred] if that stack exists, otherwise the first stack, creating a default one
     * if there are none. Also moves photos imported before stacks existed (loose files directly in
     * `wallpapers/`) into the default stack, keeping their ids so the shuffle queue stays valid.
     */
    suspend fun resolveStack(preferred: String): String = withContext(Dispatchers.IO) {
        val looseFiles = rootDir.listFiles()?.filter { it.isFile } ?: emptyList()
        if (looseFiles.isNotEmpty()) {
            val target = stackDir(stackNames().firstOrNull() ?: StackNames.DEFAULT).apply { mkdirs() }
            looseFiles.forEach { it.renameTo(File(target, it.name)) }
        }
        val names = stackNames()
        when {
            preferred in names -> preferred
            names.isNotEmpty() -> names.first()
            else -> StackNames.DEFAULT.also { stackDir(it).mkdirs() }
        }
    }

    suspend fun createStack(name: String): Boolean = withContext(Dispatchers.IO) {
        File(rootDir, name).mkdir()
    }

    suspend fun renameStack(oldName: String, newName: String): Boolean = withContext(Dispatchers.IO) {
        val renamed = File(rootDir, oldName).renameTo(File(rootDir, newName))
        if (renamed) hashIndexCache.remove(oldName)?.let { hashIndexCache[newName] = it }
        renamed
    }

    /** Deletes [name] and every photo in it. */
    suspend fun deleteStack(name: String): Boolean = withContext(Dispatchers.IO) {
        hashIndexCache.remove(name)
        File(rootDir, name).deleteRecursively()
    }

    private fun loadWallpaperItems(stack: String): List<WallpaperItem> {
        val files = stackDir(stack).listFiles() ?: return emptyList()
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

    suspend fun importFromUris(stack: String, uris: List<Uri>): ImportSummary = withContext(Dispatchers.IO) {
        val dir = stackDir(stack)
        val hashIndex = ensureHashIndex(stack)
        var summary = ImportSummary()
        for (uri in uris) {
            summary = summary.plus(saveUriToFile(dir, uri, hashIndex = hashIndex))
        }
        summary
    }

    suspend fun importFromFolder(stack: String, treeUri: Uri): ImportSummary = withContext(Dispatchers.IO) {
        var summary = ImportSummary()
        try {
            val dir = stackDir(stack)
            val hashIndex = ensureHashIndex(stack)
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
                        summary = summary.plus(saveUriToFile(dir, documentUri, displayName, hashIndex))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
        dir: File,
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
        val targetFile = File(dir, targetFileName)

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

    suspend fun deleteWallpaper(stack: String, id: String): Boolean = withContext(Dispatchers.IO) {
        val files = stackDir(stack).listFiles() ?: return@withContext false
        val fileToDelete = files.find { it.nameWithoutExtension == id }
        val deleted = fileToDelete?.delete() ?: false
        if (deleted) {
            hashIndexCache.remove(stack)
        }
        deleted
    }

    suspend fun clearAllWallpapers(stack: String): Int = withContext(Dispatchers.IO) {
        val files = stackDir(stack).listFiles() ?: return@withContext 0
        var deletedCount = 0
        for (f in files) {
            if (f.isFile && f.delete()) {
                deletedCount++
            }
        }
        hashIndexCache.remove(stack)
        deletedCount
    }

    /**
     * Picks the next wallpaper using a shuffle bag: every photo in the pool is shown once before
     * any of them repeat, instead of a fresh independent random pick each time (which can favor
     * the same handful of photos while starving the rest of the pool).
     */
    suspend fun pickNextRandomWallpaper(stack: String, lastId: String?, queue: List<String>): ShufflePickResult =
        withContext(Dispatchers.IO) {
            val currentList = loadWallpaperItems(stack)
            if (currentList.isEmpty()) return@withContext ShufflePickResult(null, emptyList())

            val poolIds = currentList.map { it.id }
            val pick = ShuffleBag.pickNext(poolIds, queue, lastId)
            val item = currentList.find { it.id == pick.id }
            ShufflePickResult(item, pick.remainingQueue)
        }

    private fun ensureHashIndex(stack: String): MutableMap<Long, MutableSet<String>> =
        hashIndexCache.getOrPut(stack) { buildHashIndex(stackDir(stack)) }

    private fun buildHashIndex(dir: File): MutableMap<Long, MutableSet<String>> {
        val index = mutableMapOf<Long, MutableSet<String>>()
        val files = dir.listFiles() ?: return index
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
