package com.gcg.wpchanger

import com.gcg.wpchanger.data.StackNames
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class StackNamesTest {

    @Test
    fun testValidate() {
        val existing = listOf("Landscapes", "Friends")
        assertNull(StackNames.validate("  Wallpapers ", existing))
        assertNull(StackNames.validate("Sevgilim ❤️", existing))
        assertNotNull(StackNames.validate("   ", existing))
        assertNotNull(StackNames.validate("a".repeat(StackNames.MAX_LENGTH + 1), existing))
        assertNotNull(StackNames.validate("..", existing))
        assertNotNull(StackNames.validate("a/b", existing))
        assertNotNull(StackNames.validate("friends", existing))
    }
}
