package com.gcg.wpchanger.data

/**
 * Result of drawing the next wallpaper id from the shuffle bag: the id to show now
 * (or null if the pool is empty) and the remaining queue to persist for next time.
 */
data class ShufflePick(val id: String?, val remainingQueue: List<String>)

/**
 * A "shuffle bag" randomizer: instead of picking a fresh random photo every time (which can
 * repeat the same handful of photos while ignoring others for a long time), this hands out a
 * random permutation of the whole pool before any id repeats, refilling once exhausted.
 *
 * Pure/stateless by design (no Context, no disk access) so it's trivial to unit test; callers
 * are responsible for persisting [ShufflePick.remainingQueue] between calls.
 */
object ShuffleBag {

    fun pickNext(poolIds: List<String>, queue: List<String>, lastId: String?): ShufflePick {
        if (poolIds.isEmpty()) return ShufflePick(null, emptyList())
        if (poolIds.size == 1) return ShufflePick(poolIds.first(), emptyList())

        // Drop any queued ids that no longer exist in the pool (deleted since the queue was built).
        var remaining = queue.filter { it in poolIds }

        if (remaining.isEmpty()) {
            remaining = poolIds.shuffled()
            // Avoid the reshuffle landing the just-shown photo right back up first.
            if (remaining.first() == lastId) {
                val swapIndex = (1 until remaining.size).random()
                remaining = remaining.toMutableList().also {
                    it[0] = remaining[swapIndex]
                    it[swapIndex] = remaining[0]
                }
            }
        }

        return ShufflePick(remaining.first(), remaining.drop(1))
    }
}
