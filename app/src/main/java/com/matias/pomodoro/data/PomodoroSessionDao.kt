package com.matias.pomodoro.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class PomodoroSessionDao {
    @Query("SELECT * FROM pomodoro_sessions WHERE date = :date LIMIT 1")
    abstract fun getTodayStats(date: String): Flow<PomodoroSession?>

    @Query("SELECT * FROM pomodoro_sessions WHERE week = :week ORDER BY date ASC")
    abstract fun getWeekStats(week: String): Flow<List<PomodoroSession>>

    @Query("SELECT * FROM pomodoro_sessions WHERE month = :month ORDER BY date ASC")
    abstract fun getMonthStats(month: String): Flow<List<PomodoroSession>>

    @Transaction
    open suspend fun upsertSession(session: PomodoroSession) {
        val existing = getSessionByDate(session.date)
        if (existing == null) {
            val insertedId = insertSession(session.copy(id = 0L))
            if (insertedId == -1L) {
                val conflicted = getSessionByDate(session.date) ?: return
                updateSession(session.copy(id = conflicted.id))
            }
        } else {
            updateSession(session.copy(id = existing.id))
        }
    }

    @Query("DELETE FROM pomodoro_sessions WHERE date < :date")
    abstract suspend fun deleteOlderThan(date: String)

    @Query("SELECT * FROM pomodoro_sessions WHERE date = :date LIMIT 1")
    abstract suspend fun getSessionByDate(date: String): PomodoroSession?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertSession(session: PomodoroSession): Long

    @Update
    abstract suspend fun updateSession(session: PomodoroSession)
}
