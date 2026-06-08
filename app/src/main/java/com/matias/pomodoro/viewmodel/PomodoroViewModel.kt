package com.matias.pomodoro.viewmodel

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.matias.pomodoro.PomodoroApplication
import com.matias.pomodoro.R
import com.matias.pomodoro.analytics.AnalyticsManager
import com.matias.pomodoro.data.PomodoroDateUtils
import com.matias.pomodoro.data.PomodoroSession
import com.matias.pomodoro.data.preferences.PomodoroPreferences
import com.matias.pomodoro.data.preferences.PomodoroSettings
import com.matias.pomodoro.di.PomodoroContainer
import com.matias.pomodoro.graphics.ColorScheme
import com.matias.pomodoro.graphics.LavaMode
import com.matias.pomodoro.service.PomodoroTimerService
import com.matias.pomodoro.timer.PomodoroTimerState
import com.matias.pomodoro.timer.TimerStatus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Temporary UI-facing state kept only so the existing Compose screens compile until the UI rewrite.
 * The business timer state is exposed separately as [timerState].
 */
data class PomodoroUiState(
    val isTimerRunning: Boolean = false,
    val intervalMinutes: Int = 25,
    val secondsUntilNextBreak: Int = 25 * 60,
    val lavaMode: LavaMode = LavaMode.LAVA,
    val colorScheme: ColorScheme = ColorScheme.MIDNIGHT_EMERALD,
    val isRestScreenVisible: Boolean = false,
    val restCountdownSeconds: Int = REST_DURATION_SECONDS,
    val completedBreaks: Int = 0,
    @StringRes val restMessageResId: Int = DEFAULT_REST_MESSAGE_RES_ID,
    val isFullscreen: Boolean = false
) {
    val timerProgress: Float
        get() {
            val total = intervalMinutes * 60
            return if (total == 0) 0f else 1f - (secondsUntilNextBreak.toFloat() / total)
        }

    val formattedTimeLeft: String
        get() {
            val minutes = secondsUntilNextBreak / 60
            val seconds = secondsUntilNextBreak % 60
            return "%02d:%02d".format(minutes, seconds)
        }
}

sealed interface PomodoroIntent {
    data object StartTimer : PomodoroIntent
    data object StopTimer : PomodoroIntent
    data class SetInterval(val minutes: Int) : PomodoroIntent
    data class SetLavaMode(val mode: LavaMode) : PomodoroIntent
    data class SetColorScheme(val scheme: ColorScheme) : PomodoroIntent
    data object DismissRestScreen : PomodoroIntent
    data object TriggerRestNow : PomodoroIntent
    data object ToggleFullscreen : PomodoroIntent
}

sealed interface PomodoroEffect {
    data object ShowNotificationPermissionRequest : PomodoroEffect
    data object RestCompleted : PomodoroEffect
    data class ShowSnackbar(val message: String) : PomodoroEffect
}

private const val REST_DURATION_SECONDS = 20
private val DEFAULT_REST_MESSAGE_RES_ID = R.string.rest_message_look_far_breathe

private data class UiPlaceholderState(
    val lavaMode: LavaMode = LavaMode.LAVA,
    val colorScheme: ColorScheme = ColorScheme.MIDNIGHT_EMERALD,
    val isFullscreen: Boolean = false,
    val isRestScreenVisible: Boolean = false,
    @StringRes val restMessageResId: Int = DEFAULT_REST_MESSAGE_RES_ID
)

class PomodoroViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = PomodoroContainer.preferences
    private val repository = PomodoroContainer.repository
    private val uiPlaceholderState = MutableStateFlow(UiPlaceholderState())
    private var dailyGoalLoggedKey: String? = null
    private var previousTimerStatus = PomodoroTimerService.state.value.status

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

    val last12MonthsStats: StateFlow<List<PomodoroSession>> = repository
        .getLast12MonthsStats(PomodoroDateUtils.monthStartForLast12Months())
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

    val state: StateFlow<PomodoroUiState> = combine(
        timerState,
        settings,
        todayStats,
        uiPlaceholderState
    ) { timer, settings, stats, placeholder ->
        PomodoroUiState(
            isTimerRunning = timer.status == TimerStatus.RUNNING,
            intervalMinutes = settings.workDurationMinutes,
            secondsUntilNextBreak = timer.remainingSeconds.takeIf { it > 0 } ?: settings.workDurationSeconds,
            lavaMode = placeholder.lavaMode,
            colorScheme = placeholder.colorScheme,
            isRestScreenVisible = placeholder.isRestScreenVisible,
            restCountdownSeconds = REST_DURATION_SECONDS,
            completedBreaks = stats?.completedPomodoros ?: timer.completedPomodoros,
            restMessageResId = placeholder.restMessageResId,
            isFullscreen = placeholder.isFullscreen
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = PomodoroUiState()
    )

    private val _effects = MutableSharedFlow<PomodoroEffect>(extraBufferCapacity = 8)
    val effects: SharedFlow<PomodoroEffect> = _effects.asSharedFlow()

    init {
        observeTimerCompletionsForAds()
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

    fun resetAll() {
        uiPlaceholderState.update { it.copy(isFullscreen = false, isRestScreenVisible = false) }
        sendServiceCommand(PomodoroTimerService.ACTION_RESET_ALL)
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

    fun handleIntent(intent: PomodoroIntent) {
        when (intent) {
            PomodoroIntent.StartTimer -> startTimer()
            PomodoroIntent.StopTimer -> pauseTimer()
            is PomodoroIntent.SetInterval -> updateSettings("work_duration_minutes", intent.minutes.toString()) {
                updateWorkDurationMinutes(intent.minutes)
            }
            is PomodoroIntent.SetLavaMode -> uiPlaceholderState.update {
                it.copy(lavaMode = intent.mode)
            }
            is PomodoroIntent.SetColorScheme -> uiPlaceholderState.update {
                it.copy(colorScheme = intent.scheme)
            }
            PomodoroIntent.DismissRestScreen -> uiPlaceholderState.update {
                it.copy(isRestScreenVisible = false)
            }
            PomodoroIntent.TriggerRestNow -> skipPhase()
            PomodoroIntent.ToggleFullscreen -> uiPlaceholderState.update {
                it.copy(isFullscreen = !it.isFullscreen)
            }
        }
    }

    private fun sendServiceCommand(action: String) {
        val application = getApplication<Application>()
        val intent = PomodoroTimerService.commandIntent(application, action).apply {
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            application.startForegroundService(intent)
        } else {
            application.startService(intent)
        }
    }

    private fun observeTimerCompletionsForAds() {
        viewModelScope.launch {
            timerState.collect { state ->
                if (previousTimerStatus != TimerStatus.COMPLETED && state.status == TimerStatus.COMPLETED) {
                    (getApplication<Application>() as? PomodoroApplication)
                        ?.adInterstitialManager
                        ?.onPhaseCompleted()
                }
                previousTimerStatus = state.status
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
