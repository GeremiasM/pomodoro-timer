package com.matias.pomodoro.timer

sealed class PomodoroPhase {
    data object Work : PomodoroPhase()
    data object ShortBreak : PomodoroPhase()
    data object LongBreak : PomodoroPhase()
}

enum class TimerStatus {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED
}

data class PomodoroTimerState(
    val phase: PomodoroPhase = PomodoroPhase.Work,
    val status: TimerStatus = TimerStatus.IDLE,
    val completedPhase: PomodoroPhase? = null,
    val currentSessionNumber: Int = 1,
    val completedPomodoros: Int = 0,
    val completedCycles: Int = 0,
    val totalDurationSeconds: Int = 0,
    val remainingSeconds: Int = 0,
    val progressFraction: Float = 1f
)
