package com.matias.pomodoro.timer

import android.content.Context

data class TimerSession(
    val isRunning: Boolean,
    val intervalMinutes: Int,
    val nextBreakAtMillis: Long
)

class TimerSessionStore(context: Context) {

    companion object {
        private const val PREFS_NAME = "pomodoro_timer_session"
        private const val KEY_IS_RUNNING = "is_running"
        private const val KEY_INTERVAL_MINUTES = "interval_minutes"
        private const val KEY_NEXT_BREAK_AT_MILLIS = "next_break_at_millis"
        private const val DEFAULT_INTERVAL_MINUTES = 20
    }

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun readSession(): TimerSession {
        val interval = prefs.getInt(KEY_INTERVAL_MINUTES, DEFAULT_INTERVAL_MINUTES).coerceAtLeast(1)
        val isRunning = prefs.getBoolean(KEY_IS_RUNNING, false)
        val nextBreakAtMillis = prefs.getLong(KEY_NEXT_BREAK_AT_MILLIS, 0L)
        return TimerSession(
            isRunning = isRunning,
            intervalMinutes = interval,
            nextBreakAtMillis = nextBreakAtMillis
        )
    }

    fun startTimer(intervalMinutes: Int, nowMillis: Long = System.currentTimeMillis()): Long {
        val safeInterval = intervalMinutes.coerceAtLeast(1)
        val nextBreakAtMillis = nowMillis + safeInterval * 60_000L
        saveSession(
            isRunning = true,
            intervalMinutes = safeInterval,
            nextBreakAtMillis = nextBreakAtMillis
        )
        return nextBreakAtMillis
    }

    fun scheduleNextCycle(intervalMinutes: Int, nowMillis: Long = System.currentTimeMillis()): Long {
        return startTimer(intervalMinutes = intervalMinutes, nowMillis = nowMillis)
    }

    fun stopTimer(preserveIntervalMinutes: Int? = null) {
        val current = readSession()
        val interval = (preserveIntervalMinutes ?: current.intervalMinutes).coerceAtLeast(1)
        saveSession(
            isRunning = false,
            intervalMinutes = interval,
            nextBreakAtMillis = 0L
        )
    }

    fun remainingSeconds(nextBreakAtMillis: Long, nowMillis: Long = System.currentTimeMillis()): Int {
        val remainingMillis = nextBreakAtMillis - nowMillis
        if (remainingMillis <= 0L) return 0
        return ((remainingMillis + 999L) / 1000L).toInt()
    }

    private fun saveSession(
        isRunning: Boolean,
        intervalMinutes: Int,
        nextBreakAtMillis: Long
    ) {
        prefs.edit()
            .putBoolean(KEY_IS_RUNNING, isRunning)
            .putInt(KEY_INTERVAL_MINUTES, intervalMinutes.coerceAtLeast(1))
            .putLong(KEY_NEXT_BREAK_AT_MILLIS, nextBreakAtMillis)
            .apply()
    }
}
