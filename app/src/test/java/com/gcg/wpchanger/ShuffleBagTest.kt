package com.gcg.wpchanger

import com.gcg.wpchanger.data.ShuffleBag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShuffleBagTest {

    @Test
    fun testEmptyPoolReturnsNullAndEmptyQueue() {
        val pick = ShuffleBag.pickNext(poolIds = emptyList(), queue = emptyList(), lastId = null)
        assertNull(pick.id)
        assertTrue(pick.remainingQueue.isEmpty())
    }

    @Test
    fun testSingleItemPoolAlwaysReturnsThatItemEvenIfLastShown() {
        val pick = ShuffleBag.pickNext(poolIds = listOf("a"), queue = emptyList(), lastId = "a")
        assertEquals("a", pick.id)
        assertTrue(pick.remainingQueue.isEmpty())
    }

    @Test
    fun testNonEmptyQueueIsConsumedWithoutReshuffling() {
        val pick = ShuffleBag.pickNext(poolIds = listOf("a", "b", "c"), queue = listOf("b", "c"), lastId = "a")
        assertEquals("b", pick.id)
        assertEquals(listOf("c"), pick.remainingQueue)
    }

    @Test
    fun testQueueEntriesNoLongerInPoolAreDroppedBeforeDrawing() {
        // "z" was deleted from the pool since the queue was persisted.
        val pick = ShuffleBag.pickNext(poolIds = listOf("a", "b"), queue = listOf("z", "b"), lastId = "a")
        assertEquals("b", pick.id)
        assertTrue(pick.remainingQueue.isEmpty())
    }

    @Test
    fun testExhaustedQueueReshufflesFullPool() {
        val pick = ShuffleBag.pickNext(poolIds = listOf("a", "b", "c"), queue = emptyList(), lastId = "a")
        val drawn = (listOf(pick.id) + pick.remainingQueue).filterNotNull().toSet()
        assertEquals(setOf("a", "b", "c"), drawn)
    }

    @Test
    fun testReshuffleNeverImmediatelyRepeatsLastShownId() {
        repeat(200) {
            val pick = ShuffleBag.pickNext(poolIds = listOf("a", "b", "c", "d"), queue = emptyList(), lastId = "a")
            assertTrue("drew lastId right after reshuffle", pick.id != "a")
        }
    }

    @Test
    fun testEveryIdAppearsOnceEachCycleWithoutRepeats() {
        val pool = listOf("a", "b", "c", "d", "e")
        var queue = emptyList<String>()
        var lastId: String? = null
        val seenThisCycle = mutableSetOf<String>()

        repeat(pool.size) {
            val pick = ShuffleBag.pickNext(pool, queue, lastId)
            val id = requireNotNull(pick.id)
            assertTrue("id repeated within a single cycle: $id", seenThisCycle.add(id))
            queue = pick.remainingQueue
            lastId = id
        }
        assertEquals(pool.toSet(), seenThisCycle)
    }
}
