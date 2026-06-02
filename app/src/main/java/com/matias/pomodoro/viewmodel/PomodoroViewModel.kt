package com.matias.pomodoro.viewmodel

import android.app.Application
import android.content.Intent
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.matias.pomodoro.R
import com.matias.pomodoro.graphics.ColorScheme
import com.matias.pomodoro.graphics.LavaMode
import com.matias.pomodoro.service.EyeRestService
import com.matias.pomodoro.timer.TimerSessionStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

// ─────────────────────────────────────────────────────────────────────────────
// ESTADO DE LA UI
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Estado inmutable de la pantalla principal.
 * Sigue el patrón UDF (Unidirectional Data Flow) de MVVM.
 */
data class PomodoroUiState(
    /** ¿Está el temporizador 20-20-20 activo? */
    val isTimerRunning: Boolean = false,

    /** Intervalo configurado en minutos (default 20 según regla 20-20-20) */
    val intervalMinutes: Int = 20,

    /** Segundos restantes hasta la próxima notificación */
    val secondsUntilNextBreak: Int = 0,

    /** Modo actual de la animación de lámpara de lava */
    val lavaMode: LavaMode = LavaMode.LAVA,

    /** Paleta de colores activa */
    val colorScheme: ColorScheme = ColorScheme.MIDNIGHT_EMERALD,

    /** ¿Está mostrándose la pantalla de descanso activo? */
    val isRestScreenVisible: Boolean = false,

    /** Cuenta regresiva del descanso activo (20 segundos según regla 20-20-20) */
    val restCountdownSeconds: Int = REST_DURATION_SECONDS,

    /** Número de descansos completados en la sesión actual */
    val completedBreaks: Int = 0,

    /** Mensaje motivacional durante el descanso */
    @StringRes val restMessageResId: Int = DEFAULT_REST_MESSAGE_RES_ID,

    /** ¿Está la animación en pantalla completa? */
    val isFullscreen: Boolean = false
) {
    /** Progreso del temporizador principal [0.0, 1.0] */
    val timerProgress: Float
        get() {
            val total = intervalMinutes * 60
            return if (total == 0) 0f
            else 1f - (secondsUntilNextBreak.toFloat() / total)
        }

    /** Texto formateado del tiempo restante (mm:ss) */
    val formattedTimeLeft: String
        get() {
            val m = secondsUntilNextBreak / 60
            val s = secondsUntilNextBreak % 60
            return "%02d:%02d".format(m, s)
        }
}

// ─────────────────────────────────────────────────────────────────────────────
// EVENTOS DE LA UI → ViewModel
// ─────────────────────────────────────────────────────────────────────────────

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

// ─────────────────────────────────────────────────────────────────────────────
// EFECTOS DE LADO (eventos únicos hacia la UI)
// ─────────────────────────────────────────────────────────────────────────────

sealed interface PomodoroEffect {
    data object ShowNotificationPermissionRequest : PomodoroEffect
    data object RestCompleted : PomodoroEffect
    data class ShowSnackbar(val message: String) : PomodoroEffect
}

// ─────────────────────────────────────────────────────────────────────────────
// CONSTANTES
// ─────────────────────────────────────────────────────────────────────────────

private const val REST_DURATION_SECONDS = 20
private val DEFAULT_REST_MESSAGE_RES_ID = R.string.rest_message_look_far_breathe

private val REST_MESSAGE_RES_IDS = listOf(
    R.string.rest_message_look_far_breathe,
    R.string.rest_message_blink_slowly,
    R.string.rest_message_follow_bubbles,
    R.string.rest_message_relax_neck_shoulders,
    R.string.rest_message_close_eyes,
    R.string.rest_message_focus_far_window
)

// ─────────────────────────────────────────────────────────────────────────────
// VIEWMODEL
// ─────────────────────────────────────────────────────────────────────────────

class PomodoroViewModel(application: Application) : AndroidViewModel(application) {

    private val timerSessionStore = TimerSessionStore(application)

    private val _state = MutableStateFlow(PomodoroUiState())
    val state: StateFlow<PomodoroUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<PomodoroEffect>(extraBufferCapacity = 8)
    val effects: SharedFlow<PomodoroEffect> = _effects.asSharedFlow()

    private var timerJob: Job? = null
    private var restJob: Job? = null

    init {
        restoreTimerState()
    }

    fun handleIntent(intent: PomodoroIntent) {
        when (intent) {
            is PomodoroIntent.StartTimer        -> startTimer()
            is PomodoroIntent.StopTimer         -> stopTimer()
            is PomodoroIntent.SetInterval       -> setInterval(intent.minutes)
            is PomodoroIntent.SetLavaMode       -> updateState { copy(lavaMode = intent.mode) }
            is PomodoroIntent.SetColorScheme    -> updateState { copy(colorScheme = intent.scheme) }
            is PomodoroIntent.DismissRestScreen -> dismissRestScreen()
            is PomodoroIntent.TriggerRestNow    -> triggerRest()
            is PomodoroIntent.ToggleFullscreen  -> updateState { copy(isFullscreen = !isFullscreen) }
        }
    }

    private fun startTimer() {
        if (_state.value.isTimerRunning) return

        val intervalMinutes = _state.value.intervalMinutes
        val nextBreakAtMillis = timerSessionStore.startTimer(intervalMinutes)
        val totalSeconds = timerSessionStore.remainingSeconds(nextBreakAtMillis)

        updateState {
            copy(
                isTimerRunning          = true,
                intervalMinutes         = intervalMinutes,
                secondsUntilNextBreak   = totalSeconds
            )
        }

        startEyeRestService()
        startTimerLoop()
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        restJob?.cancel()
        restJob = null
        timerSessionStore.stopTimer(preserveIntervalMinutes = _state.value.intervalMinutes)
        stopEyeRestService()

        updateState {
            copy(
                isTimerRunning        = false,
                isRestScreenVisible   = false,
                isFullscreen          = false, // Quitar pantalla completa al parar
                secondsUntilNextBreak = intervalMinutes * 60
            )
        }
    }

    private fun setInterval(minutes: Int) {
        val wasRunning = _state.value.isTimerRunning
        if (wasRunning) stopTimer()

        updateState {
            copy(
                intervalMinutes       = minutes,
                secondsUntilNextBreak = minutes * 60
            )
        }

        if (wasRunning) startTimer()
    }

    fun triggerRest() {
        val messageResId = REST_MESSAGE_RES_IDS.random()
        updateState {
            copy(
                isRestScreenVisible   = true,
                restCountdownSeconds  = REST_DURATION_SECONDS,
                restMessageResId      = messageResId,
                lavaMode              = LavaMode.FOCUS_POINT_CIRCLE
            )
        }

        restJob?.cancel()
        restJob = viewModelScope.launch {
            var countdown = REST_DURATION_SECONDS

            while (countdown > 0 && isActive) {
                delay(1_000L)
                countdown--
                updateState { copy(restCountdownSeconds = countdown) }
            }

            if (isActive) {
                completeRest()
            }
        }
    }

    private fun completeRest() {
        val wasRunning = _state.value.isTimerRunning
        updateState {
            copy(
                isRestScreenVisible   = false,
                completedBreaks       = completedBreaks + 1,
                lavaMode              = LavaMode.LAVA,
                secondsUntilNextBreak = intervalMinutes * 60
            )
        }

        viewModelScope.launch {
            _effects.emit(PomodoroEffect.RestCompleted)
        }

        if (wasRunning) {
            restartTimerCycleFromNow()
        }
    }

    private fun dismissRestScreen() {
        restJob?.cancel()
        completeRest()
    }

    private fun startEyeRestService() {
        val intent = Intent(getApplication(), EyeRestService::class.java).apply {
            action = EyeRestService.ACTION_START
            putExtra(EyeRestService.EXTRA_INTERVAL_MINUTES, _state.value.intervalMinutes)
        }
        getApplication<Application>().startForegroundService(intent)
    }

    private fun stopEyeRestService() {
        val intent = Intent(getApplication(), EyeRestService::class.java).apply {
            action = EyeRestService.ACTION_STOP
        }
        getApplication<Application>().startService(intent)
    }

    private fun restoreTimerState() {
        val session = timerSessionStore.readSession()
        if (!session.isRunning) {
            updateState {
                copy(
                    intervalMinutes = session.intervalMinutes,
                    secondsUntilNextBreak = session.intervalMinutes * 60
                )
            }
            return
        }

        val remaining = timerSessionStore.remainingSeconds(session.nextBreakAtMillis)
        if (remaining > 0) {
            updateState {
                copy(
                    isTimerRunning = true,
                    intervalMinutes = session.intervalMinutes,
                    secondsUntilNextBreak = remaining
                )
            }
        } else {
            val nextBreakAtMillis = timerSessionStore.scheduleNextCycle(session.intervalMinutes)
            val restartedRemaining = timerSessionStore.remainingSeconds(nextBreakAtMillis)
            updateState {
                copy(
                    isTimerRunning = true,
                    intervalMinutes = session.intervalMinutes,
                    secondsUntilNextBreak = restartedRemaining
                )
            }
        }

        ensureEyeRestServiceRunning()
        startTimerLoop()
    }

    private fun startTimerLoop() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive && _state.value.isTimerRunning) {
                val session = timerSessionStore.readSession()
                if (!session.isRunning) {
                    updateState {
                        copy(
                            isTimerRunning = false,
                            isRestScreenVisible = false,
                            isFullscreen = false,
                            secondsUntilNextBreak = intervalMinutes * 60
                        )
                    }
                    break
                }

                val remaining = timerSessionStore.remainingSeconds(session.nextBreakAtMillis)
                updateState {
                    copy(
                        intervalMinutes = session.intervalMinutes,
                        secondsUntilNextBreak = remaining
                    )
                }

                if (remaining <= 0) {
                    triggerRest()
                    break
                }

                delay(1_000L)
            }
        }
    }

    private fun restartTimerCycleFromNow() {
        val intervalMinutes = _state.value.intervalMinutes
        val nextBreakAtMillis = timerSessionStore.scheduleNextCycle(intervalMinutes)
        val remaining = timerSessionStore.remainingSeconds(nextBreakAtMillis)

        updateState {
            copy(
                isTimerRunning = true,
                secondsUntilNextBreak = remaining
            )
        }

        startEyeRestService()
        startTimerLoop()
    }

    private fun ensureEyeRestServiceRunning() {
        val intent = Intent(getApplication(), EyeRestService::class.java)
        getApplication<Application>().startForegroundService(intent)
    }

    private fun updateState(block: PomodoroUiState.() -> PomodoroUiState) {
        _state.update { it.block() }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        restJob?.cancel()
    }
}
