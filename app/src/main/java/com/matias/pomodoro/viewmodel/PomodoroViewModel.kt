package com.matias.pomodoro.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.matias.pomodoro.PomodoroApplication
import com.matias.pomodoro.analytics.AnalyticsManager
import com.matias.pomodoro.data.PomodoroDateUtils
import com.matias.pomodoro.data.PomodoroSession
import com.matias.pomodoro.data.preferences.PomodoroPreferences
import com.matias.pomodoro.data.preferences.PomodoroSettings
import com.matias.pomodoro.di.PomodoroContainer
import com.matias.pomodoro.service.PomodoroTimerService
import com.matias.pomodoro.timer.PomodoroPhase
import com.matias.pomodoro.timer.PomodoroTimerState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PomodoroViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = PomodoroContainer.preferences
    private val repository = PomodoroContainer.repository
    private var dailyGoalLoggedKey: String? = null
    private var previousCompletedPhase = PomodoroTimerService.state.value.completedPhase

    val timerState: StateFlow<PomodoroTimerState> = PomodoroTimerService.state

    val settings: StateFlow<PomodoroSettings> = preferences.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = PomodoroSettings()
    )

    val todayStats: StateFlow<PomodoroSession?> = repository
        .getTodayStats(PomodoroDateUtils.todayDate())
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = null
        )

    val weekStats: StateFlow<List<PomodoroSession>> = repository
        .getWeekStats(PomodoroDateUtils.currentWeek())
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyList()
        )

    val monthStats: StateFlow<List<PomodoroSession>> = repository
        .getMonthStats(PomodoroDateUtils.currentMonth())
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyList()
        )

    val dailyGoalProgress: StateFlow<Float> = combine(todayStats, settings) { stats, settings ->
        val completed = stats?.completedPomodoros ?: 0
        (completed.toFloat() / settings.dailyGoalPomodoros.toFloat()).coerceIn(0f, 1f)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = 0f
    )

    val dismissedMotdText: StateFlow<String> = preferences.dismissedMotdText.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = ""
    )

    init {
        observeLongBreakCompletionsForAds()
        observeDailyGoalReached()
    }

    fun startTimer() {
        sendServiceCommand(PomodoroTimerService.ACTION_START)
    }

    fun pauseTimer() {
        sendServiceCommand(PomodoroTimerService.ACTION_PAUSE)
    }

    fun skipPhase() {
        sendServiceCommand(PomodoroTimerService.ACTION_SKIP)
    }

    fun resetPhase() {
        sendServiceCommand(PomodoroTimerService.ACTION_RESET)
    }

    fun updateSettings(
        settingName: String,
        newValue: String,
        block: suspend PomodoroPreferences.() -> Unit
    ) {
        viewModelScope.launch {
            val previousTheme = settings.value.selectedTheme
            preferences.block()
            AnalyticsManager.logSettingsChanged(settingName, newValue)
            if (settingName == SETTING_SELECTED_THEME && previousTheme != newValue) {
                AnalyticsManager.logThemeChanged(previousTheme, newValue)
            }
        }
    }

    fun dismissMotd(text: String) {
        viewModelScope.launch {
            preferences.dismissMotd(text)
        }
    }

    private fun sendServiceCommand(action: String) {
        val application = getApplication<Application>()
        val intent = PomodoroTimerService.commandIntent(application, action).apply {
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        }
        application.startForegroundService(intent)
    }

    private fun observeLongBreakCompletionsForAds() {
        viewModelScope.launch {
            timerState.collect { state ->
                if (
                    previousCompletedPhase != PomodoroPhase.LongBreak &&
                    state.completedPhase == PomodoroPhase.LongBreak
                ) {
                    (getApplication<Application>() as? PomodoroApplication)
                        ?.adInterstitialManager
                        ?.show()
                }
                previousCompletedPhase = state.completedPhase
            }
        }
    }

    private fun observeDailyGoalReached() {
        viewModelScope.launch {
            combine(todayStats, settings) { stats, settings -> stats to settings }
                .collect { (stats, settings) ->
                    val completed = stats?.completedPomodoros ?: return@collect
                    val key = "${stats.date}:${settings.dailyGoalPomodoros}"
                    if (completed >= settings.dailyGoalPomodoros && dailyGoalLoggedKey != key) {
                        dailyGoalLoggedKey = key
                        AnalyticsManager.logDailyGoalReached(
                            goalCount = settings.dailyGoalPomodoros,
                            totalFocusMinutes = stats.totalFocusSeconds / 60
                        )
                    }
                }
        }
    }

    private companion object {
        const val SETTING_SELECTED_THEME = "selected_theme"
    }
}
