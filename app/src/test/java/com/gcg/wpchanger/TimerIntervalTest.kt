package com.gcg.wpchanger

import com.gcg.wpchanger.data.TimerInterval
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

class TimerIntervalTest {

    @Test
    fun testIntervalValues() {
        assertEquals(30L, TimerInterval.MINUTES_30.duration)
        assertEquals(TimeUnit.MINUTES, TimerInterval.MINUTES_30.timeUnit)

        assertEquals(1L, TimerInterval.HOURS_1.duration)
        assertEquals(TimeUnit.HOURS, TimerInterval.HOURS_1.timeUnit)

        assertEquals(3L, TimerInterval.HOURS_3.duration)
        assertEquals(TimeUnit.HOURS, TimerInterval.HOURS_3.timeUnit)

        assertEquals(6L, TimerInterval.HOURS_6.duration)
        assertEquals(TimeUnit.HOURS, TimerInterval.HOURS_6.timeUnit)

        assertEquals(24L, TimerInterval.DAILY.duration)
        assertEquals(TimeUnit.HOURS, TimerInterval.DAILY.timeUnit)
    }

    @Test
    fun testFromNameFallback() {
        assertEquals(TimerInterval.MINUTES_30, TimerInterval.fromName("MINUTES_30"))
        assertEquals(TimerInterval.HOURS_1, TimerInterval.fromName("HOURS_1"))
        assertEquals(TimerInterval.HOURS_3, TimerInterval.fromName("HOURS_3"))
        assertEquals(TimerInterval.HOURS_6, TimerInterval.fromName("HOURS_6"))
        assertEquals(TimerInterval.DAILY, TimerInterval.fromName("DAILY"))
        assertEquals(TimerInterval.HOURS_1, TimerInterval.fromName("INVALID_UNKNOWN"))
        assertEquals(TimerInterval.HOURS_1, TimerInterval.fromName(null))
    }
}
