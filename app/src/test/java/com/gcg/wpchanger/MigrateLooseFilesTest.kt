package com.gcg.wpchanger

import com.gcg.wpchanger.data.StackNames
import com.gcg.wpchanger.data.migrateLooseFiles
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class MigrateLooseFilesTest {

    @Test
    fun testLoosePhotosMoveIntoDefaultStackAndStacksAreUntouched() {
        val root = createTempDirectory().toFile()
        try {
            File(root, "wp_1_a__beach.jpg").writeText("a")
            File(root, "wp_2_b__hills.png").writeText("b")
            File(root, "Friends").mkdirs()
            File(root, "Friends/wp_3_c__party.jpg").writeText("c")

            migrateLooseFiles(root)
            migrateLooseFiles(root) // second run is a no-op

            assertEquals(setOf("Friends", StackNames.DEFAULT), root.list()!!.toSet())
            assertEquals(setOf("wp_1_a__beach.jpg", "wp_2_b__hills.png"), File(root, StackNames.DEFAULT).list()!!.toSet())
            assertEquals("a", File(root, "${StackNames.DEFAULT}/wp_1_a__beach.jpg").readText())
            assertEquals(listOf("wp_3_c__party.jpg"), File(root, "Friends").list()!!.toList())
        } finally {
            root.deleteRecursively()
        }
    }
}
