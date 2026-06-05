package com.matias.pomodoro.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private val Context.pomodoroDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "pomodoro_preferences"
)

class PomodoroPreferences(context: Context) {
    private val dataStore = context.applicationContext.pomodoroDataStore

    val settings: Flow<PomodoroSettings> = dataStore.data.map { prefs ->
        PomodoroSettings(
            workDurationMinutes = (prefs[Keys.WORK_DURATION_MINUTES] ?: DEFAULT_WORK_DURATION_MINUTES)
                .coerceIn(WORK_DURATION_MINUTES_MIN, WORK_DURATION_MINUTES_MAX),
            shortBreakMinutes = (prefs[Keys.SHORT_BREAK_MINUTES] ?: DEFAULT_SHORT_BREAK_MINUTES)
                .coerceIn(SHORT_BREAK_MINUTES_MIN, SHORT_BREAK_MINUTES_MAX),
            longBreakMinutes = (prefs[Keys.LONG_BREAK_MINUTES] ?: DEFAULT_LONG_BREAK_MINUTES)
                .coerceIn(LONG_BREAK_MINUTES_MIN, LONG_BREAK_MINUTES_MAX),
            sessionsBeforeLongBreak = (prefs[Keys.SESSIONS_BEFORE_LONG_BREAK] ?: DEFAULT_SESSIONS_BEFORE_LONG_BREAK)
                .coerceIn(SESSIONS_BEFORE_LONG_BREAK_MIN, SESSIONS_BEFORE_LONG_BREAK_MAX),
            autoStartBreaks = prefs[Keys.AUTO_START_BREAKS] ?: false,
            autoStartWork = prefs[Keys.AUTO_START_WORK] ?: false,
            vibrationEnabled = prefs[Keys.VIBRATION_ENABLED] ?: true,
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            notificationSound = sanitizeNotificationSound(prefs[Keys.NOTIFICATION_SOUND] ?: DEFAULT_NOTIFICATION_SOUND),
            selectedTheme = sanitizeSelectedTheme(prefs[Keys.SELECTED_THEME] ?: DEFAULT_SELECTED_THEME),
            dailyGoalPomodoros = (prefs[Keys.DAILY_GOAL_POMODOROS] ?: DEFAULT_DAILY_GOAL_POMODOROS)
                .coerceIn(DAILY_GOAL_POMODOROS_MIN, DAILY_GOAL_POMODOROS_MAX)
        )
    }

    suspend fun updateWorkDurationMinutes(value: Int) = updateInt(
        Keys.WORK_DURATION_MINUTES,
        value.coerceIn(WORK_DURATION_MINUTES_MIN, WORK_DURATION_MINUTES_MAX)
    )

    suspend fun updateShortBreakMinutes(value: Int) = updateInt(
        Keys.SHORT_BREAK_MINUTES,
        value.coerceIn(SHORT_BREAK_MINUTES_MIN, SHORT_BREAK_MINUTES_MAX)
    )

    suspend fun updateLongBreakMinutes(value: Int) = updateInt(
        Keys.LONG_BREAK_MINUTES,
        value.coerceIn(LONG_BREAK_MINUTES_MIN, LONG_BREAK_MINUTES_MAX)
    )

    suspend fun updateSessionsBeforeLongBreak(value: Int) = updateInt(
        Keys.SESSIONS_BEFORE_LONG_BREAK,
        value.coerceIn(SESSIONS_BEFORE_LONG_BREAK_MIN, SESSIONS_BEFORE_LONG_BREAK_MAX)
    )

    suspend fun updateAutoStartBreaks(value: Boolean) = updateBoolean(Keys.AUTO_START_BREAKS, value)

    suspend fun updateAutoStartWork(value: Boolean) = updateBoolean(Keys.AUTO_START_WORK, value)

    suspend fun updateVibrationEnabled(value: Boolean) = updateBoolean(Keys.VIBRATION_ENABLED, value)

    suspend fun updateSoundEnabled(value: Boolean) = updateBoolean(Keys.SOUND_ENABLED, value)

    suspend fun updateNotificationSound(value: String) = updateString(
        Keys.NOTIFICATION_SOUND,
        sanitizeNotificationSound(value)
    )

    suspend fun updateSelectedTheme(value: String) = updateString(
        Keys.SELECTED_THEME,
        sanitizeSelectedTheme(value)
    )

    suspend fun updateDailyGoalPomodoros(value: Int) = updateInt(
        Keys.DAILY_GOAL_POMODOROS,
        value.coerceIn(DAILY_GOAL_POMODOROS_MIN, DAILY_GOAL_POMODOROS_MAX)
    )

    private suspend fun updateInt(key: Preferences.Key<Int>, value: Int) = withContext(Dispatchers.IO) {
        dataStore.edit { it[key] = value }
    }

    private suspend fun updateBoolean(key: Preferences.Key<Boolean>, value: Boolean) = withContext(Dispatchers.IO) {
        dataStore.edit { it[key] = value }
    }

    private suspend fun updateString(key: Preferences.Key<String>, value: String) = withContext(Dispatchers.IO) {
        dataStore.edit { it[key] = value }
    }

    private object Keys {
        val WORK_DURATION_MINUTES = intPreferencesKey("work_duration_minutes")
        val SHORT_BREAK_MINUTES = intPreferencesKey("short_break_minutes")
        val LONG_BREAK_MINUTES = intPreferencesKey("long_break_minutes")
        val SESSIONS_BEFORE_LONG_BREAK = intPreferencesKey("sessions_before_long_break")
        val AUTO_START_BREAKS = booleanPreferencesKey("auto_start_breaks")
        val AUTO_START_WORK = booleanPreferencesKey("auto_start_work")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val NOTIFICATION_SOUND = stringPreferencesKey("notification_sound")
        val SELECTED_THEME = stringPreferencesKey("selected_theme")
        val DAILY_GOAL_POMODOROS = intPreferencesKey("daily_goal_pomodoros")
    }

    companion object {
        const val DEFAULT_WORK_DURATION_MINUTES = 25
        const val DEFAULT_SHORT_BREAK_MINUTES = 5
        const val DEFAULT_LONG_BREAK_MINUTES = 15
        const val DEFAULT_SESSIONS_BEFORE_LONG_BREAK = 4
        const val DEFAULT_NOTIFICATION_SOUND = "bell"
        const val DEFAULT_SELECTED_THEME = "emerald"
        const val DEFAULT_DAILY_GOAL_POMODOROS = 8

        const val WORK_DURATION_MINUTES_MIN = 5
        const val WORK_DURATION_MINUTES_MAX = 60
        const val SHORT_BREAK_MINUTES_MIN = 1
        const val SHORT_BREAK_MINUTES_MAX = 15
        const val LONG_BREAK_MINUTES_MIN = 10
        const val LONG_BREAK_MINUTES_MAX = 30
        const val SESSIONS_BEFORE_LONG_BREAK_MIN = 2
        const val SESSIONS_BEFORE_LONG_BREAK_MAX = 6
        const val DAILY_GOAL_POMODOROS_MIN = 1
        const val DAILY_GOAL_POMODOROS_MAX = 20

        val NOTIFICATION_SOUND_OPTIONS = setOf("bell", "digital", "soft")
        val SELECTED_THEME_OPTIONS = setOf("emerald", "sunset", "ocean", "forest", "lavender")

        fun sanitizeNotificationSound(value: String): String =
            if (value in NOTIFICATION_SOUND_OPTIONS) value else DEFAULT_NOTIFICATION_SOUND

        fun sanitizeSelectedTheme(value: String): String =
            if (value in SELECTED_THEME_OPTIONS) value else DEFAULT_SELECTED_THEME
    }
}
