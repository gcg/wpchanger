package com.gcg.wpchanger.data

import android.app.WallpaperManager

enum class WallpaperTarget(val label: String, val flag: Int) {
    BOTH("Home & Lock", WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK),
    HOME("Home Screen", WallpaperManager.FLAG_SYSTEM),
    LOCK("Lock Screen", WallpaperManager.FLAG_LOCK),
    ;

    companion object {
        fun fromName(name: String?): WallpaperTarget {
            return entries.find { it.name == name } ?: BOTH
        }
    }
}
