package com.gcg.wpchanger

import com.gcg.wpchanger.data.WallpaperTarget
import org.junit.Assert.assertEquals
import org.junit.Test

class WallpaperTargetTest {

    @Test
    fun testFromNameFallback() {
        assertEquals(WallpaperTarget.BOTH, WallpaperTarget.fromName("BOTH"))
        assertEquals(WallpaperTarget.HOME, WallpaperTarget.fromName("HOME"))
        assertEquals(WallpaperTarget.LOCK, WallpaperTarget.fromName("LOCK"))
        assertEquals(WallpaperTarget.BOTH, WallpaperTarget.fromName("UNKNOWN"))
        assertEquals(WallpaperTarget.BOTH, WallpaperTarget.fromName(null))
    }
}
