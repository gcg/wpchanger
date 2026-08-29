package com.gcg.wpchanger.data

import java.util.concurrent.TimeUnit

enum class TimerInterval(
    val label: String,
    val durationMinutes: Long,
    val duration: Long,
    val timeUnit: TimeUnit,
) {
    MINUTES_30("30 min", 30, 30, TimeUnit.MINUTES),
    HOURS_1("1 hour", 60, 1, TimeUnit.HOURS),
    HOURS_3("3 hours", 180, 3, TimeUnit.HOURS),
    HOURS_6("6 hours", 360, 6, TimeUnit.HOURS),
    DAILY("Daily", 1440, 24, TimeUnit.HOURS),
    ;

    companion object {
        fun fromName(name: String?): TimerInterval {
            return entries.find { it.name == name } ?: HOURS_1
        }
    }
}
