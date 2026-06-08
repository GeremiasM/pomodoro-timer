package com.matias.pomodoro.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class PomodoroRepository(
    private val dao: PomodoroSessionDao
) {
    fun getTodayStats(date: String): Flow<PomodoroSession?> = dao.getTodayStats(date)

    fun getWeekStats(week: String): Flow<List<PomodoroSession>> = dao.getWeekStats(week)

    fun getMonthStats(month: String): Flow<List<PomodoroSession>> = dao.getMonthStats(month)

    suspend fun deleteOlderThan(date: String) = withContext(Dispatchers.IO) {
        dao.deleteOlderThan(date)
    }

    suspend fun recordWorkSessionCompleted(focusSeconds: Int) = withContext(Dispatchers.IO) {
        val session = todaySession()
        dao.upsertSession(
            session.copy(
                completedPomodoros = session.completedPomodoros + 1,
                totalFocusSeconds = session.totalFocusSeconds + focusSeconds.coerceAtLeast(0),
                lastUpdated = System.currentTimeMillis()
            )
        )
    }

    suspend fun recordBreakSessionCompleted(breakSeconds: Int, completedCycle: Boolean) = withContext(Dispatchers.IO) {
        val session = todaySession()
        dao.upsertSession(
            session.copy(
                completedCycles = session.completedCycles + if (completedCycle) 1 else 0,
                totalBreakSeconds = session.totalBreakSeconds + breakSeconds.coerceAtLeast(0),
                lastUpdated = System.currentTimeMillis()
            )
        )
    }

    private suspend fun todaySession(): PomodoroSession {
        val today = PomodoroDateUtils.todayDate()
        val existing = dao.getSessionByDate(today)
        if (existing != null) return existing

        val localDate = PomodoroDateUtils.parseDate(today)
        return PomodoroSession(
            date = today,
            week = PomodoroDateUtils.weekFor(localDate),
            month = PomodoroDateUtils.monthFor(localDate),
            lastUpdated = System.currentTimeMillis()
        )
    }
}
