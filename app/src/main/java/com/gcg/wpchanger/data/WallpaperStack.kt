package com.gcg.wpchanger.data

/**
 * A named group of wallpapers. Each stack is a subfolder of `filesDir/wallpapers/` and its
 * [name] is the folder name, so renaming a stack is just a folder rename.
 */
data class WallpaperStack(val name: String, val count: Int)

object StackNames {
    const val DEFAULT = "My Photos"
    const val MAX_LENGTH = 40

    /** Returns an error message for [name], or null if it's usable as a new stack name. */
    fun validate(name: String, existing: List<String>): String? {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> "Name can't be empty"
            trimmed.length > MAX_LENGTH -> "Name must be $MAX_LENGTH characters or fewer"
            trimmed == "." || trimmed == ".." || trimmed.any { it == '/' || it == '\u0000' } -> "Name contains invalid characters"
            existing.any { it.equals(trimmed, ignoreCase = true) } -> "A stack with this name already exists"
            else -> null
        }
    }
}
