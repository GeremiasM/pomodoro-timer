package com.matias.pomodoro.data.preferences

data class PomodoroSettings(
    val workDurationMinutes: Int = PomodoroPreferences.DEFAULT_WORK_DURATION_MINUTES,
    val shortBreakMinutes: Int = PomodoroPreferences.DEFAULT_SHORT_BREAK_MINUTES,
    val longBreakMinutes: Int = PomodoroPreferences.DEFAULT_LONG_BREAK_MINUTES,
    val sessionsBeforeLongBreak: Int = PomodoroPreferences.DEFAULT_SESSIONS_BEFORE_LONG_BREAK,
    val autoStartBreaks: Boolean = false,
    val autoStartWork: Boolean = false,
    val vibrationEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val notificationSound: String = PomodoroPreferences.DEFAULT_NOTIFICATION_SOUND,
    val selectedTheme: String = PomodoroPreferences.DEFAULT_SELECTED_THEME,
    val dailyGoalPomodoros: Int = PomodoroPreferences.DEFAULT_DAILY_GOAL_POMODOROS
) {
    val workDurationSeconds: Int get() = workDurationMinutes * 60
    val shortBreakSeconds: Int get() = shortBreakMinutes * 60
    val longBreakSeconds: Int get() = longBreakMinutes * 60
}
