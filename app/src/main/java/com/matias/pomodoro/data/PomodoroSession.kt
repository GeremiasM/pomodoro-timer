package com.matias.pomodoro.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pomodoro_sessions",
    indices = [
        Index(value = ["date"], unique = true),
        Index(value = ["week"]),
        Index(value = ["month"])
    ]
)
data class PomodoroSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val date: String,
    val week: String,
    val month: String,
    @ColumnInfo(name = "completed_pomodoros")
    val completedPomodoros: Int = 0,
    @ColumnInfo(name = "completed_cycles")
    val completedCycles: Int = 0,
    @ColumnInfo(name = "total_focus_seconds")
    val totalFocusSeconds: Int = 0,
    @ColumnInfo(name = "total_break_seconds")
    val totalBreakSeconds: Int = 0,
    @ColumnInfo(name = "last_updated")
    val lastUpdated: Long = System.currentTimeMillis()
)
