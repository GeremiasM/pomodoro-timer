package com.matias.pomodoro.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.matias.pomodoro.MainActivity
import com.matias.pomodoro.R
import com.matias.pomodoro.analytics.AnalyticsManager
import com.matias.pomodoro.data.PomodoroDateUtils
import com.matias.pomodoro.data.preferences.PomodoroSettings
import com.matias.pomodoro.di.PomodoroContainer
import com.matias.pomodoro.timer.PomodoroPhase
import com.matias.pomodoro.timer.PomodoroTimerState
import com.matias.pomodoro.timer.TimerStatus
import com.matias.pomodoro.widget.PomodoroWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PomodoroTimerService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var timerJob: Job? = null
    private var settingsJob: Job? = null
    private var statsJob: Job? = null
    private var currentSettings = PomodoroSettings()
    private var soundPool: SoundPool? = null
    private var soundIds: Map<String, Int> = emptyMap()
    private var isForeground = false
    private var phaseStartedAutomatically = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initializeSoundPool()
        observeSettings()
        observeTodayStats()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopTimerJob()
                stopForegroundCompat()
                state.update { it.copy(status = TimerStatus.IDLE) }
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_PAUSE -> {
                ensureForeground()
                pauseCurrentPhase()
            }
            ACTION_SKIP -> {
                ensureForeground()
                serviceScope.launch { completeCurrentPhase(countAsCompleted = false, alert = false) }
            }
            ACTION_RESET -> {
                ensureForeground()
                resetCurrentPhase()
            }
            ACTION_RESET_ALL -> {
                ensureForeground()
                resetAllPhases()
            }
            ACTION_START, null -> {
                ensureForeground()
                startOrResumeCurrentPhase()
            }
            else -> ensureForeground()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopTimerJob()
        settingsJob?.cancel()
        statsJob?.cancel()
        soundPool?.release()
        soundPool = null
        serviceScope.cancel()
        isForeground = false
        super.onDestroy()
    }

    private fun observeSettings() {
        settingsJob = serviceScope.launch {
            PomodoroContainer.preferences.settings.collect { settings ->
                currentSettings = settings
                state.update { current ->
                    if (current.status == TimerStatus.IDLE || current.status == TimerStatus.COMPLETED) {
                        val duration = durationForPhase(current.phase, settings)
                        current.copy(
                            totalDurationSeconds = duration,
                            remainingSeconds = duration,
                            progressFraction = 1f
                        )
                    } else {
                        current
                    }
                }
                updateNotificationIfForeground()
            }
        }
    }

    private fun observeTodayStats() {
        statsJob = serviceScope.launch {
            PomodoroContainer.repository.getTodayStats(PomodoroDateUtils.todayDate()).collect { session ->
                state.update { current ->
                    current.copy(
                        completedPomodoros = session?.completedPomodoros ?: 0,
                        completedCycles = session?.completedCycles ?: 0
                    )
                }
                updateNotificationIfForeground()
            }
        }
    }

    private fun startOrResumeCurrentPhase() {
        val current = state.value
        if (current.status != TimerStatus.PAUSED) {
            phaseStartedAutomatically = false
        }
        val duration = durationForPhase(current.phase)
        val remaining = when {
            current.status == TimerStatus.PAUSED && current.remainingSeconds > 0 -> current.remainingSeconds
            current.remainingSeconds > 0 && current.totalDurationSeconds > 0 -> current.remainingSeconds
            else -> duration
        }

        state.update {
            it.copy(
                status = TimerStatus.RUNNING,
                totalDurationSeconds = duration,
                remainingSeconds = remaining,
                progressFraction = progressFor(duration, remaining)
            )
        }
        updateNotificationIfForeground()
        startCountdownLoop()
    }

    private fun pauseCurrentPhase() {
        if (state.value.status != TimerStatus.RUNNING) return
        stopTimerJob()
        state.update { it.copy(status = TimerStatus.PAUSED) }
        updateNotificationIfForeground()
    }

    private fun resetCurrentPhase() {
        stopTimerJob()
        phaseStartedAutomatically = false
        val duration = durationForPhase(state.value.phase)
        state.update {
            it.copy(
                status = TimerStatus.IDLE,
                totalDurationSeconds = duration,
                remainingSeconds = duration,
                progressFraction = 1f
            )
        }
        updateNotificationIfForeground()
    }

    private fun resetAllPhases() {
        stopTimerJob()
        phaseStartedAutomatically = false
        val duration = currentSettings.workDurationSeconds
        state.update {
            it.copy(
                phase = PomodoroPhase.Work,
                status = TimerStatus.IDLE,
                currentSessionNumber = 1,
                totalDurationSeconds = duration,
                remainingSeconds = duration,
                progressFraction = 1f
            )
        }
        updateNotificationIfForeground()
    }

    private fun startCountdownLoop() {
        stopTimerJob()
        timerJob = serviceScope.launch(Dispatchers.Default) {
            while (isActive && state.value.status == TimerStatus.RUNNING) {
                delay(1_000L)
                val current = state.value
                if (current.status != TimerStatus.RUNNING) break

                val remaining = (current.remainingSeconds - 1).coerceAtLeast(0)
                state.update {
                    it.copy(
                        remainingSeconds = remaining,
                        progressFraction = progressFor(it.totalDurationSeconds, remaining)
                    )
                }
                updateNotificationIfForeground()

                if (remaining <= 0) {
                    completeCurrentPhase(countAsCompleted = true, alert = true)
                    break
                }
            }
        }
    }

    private suspend fun completeCurrentPhase(countAsCompleted: Boolean, alert: Boolean) {
        try {
            stopTimerJob()
            val completedState = state.value
            val settings = currentSettings
            val completedAutoStarted = phaseStartedAutomatically

            if (countAsCompleted) {
                when (completedState.phase) {
                    PomodoroPhase.Work -> {
                        PomodoroContainer.repository.recordWorkSessionCompleted(
                            focusSeconds = completedState.totalDurationSeconds
                        )
                        AnalyticsManager.logPomodoroCompleted(
                            sessionNumber = completedState.currentSessionNumber.coerceIn(1, 6),
                            durationSeconds = completedState.totalDurationSeconds,
                            theme = settings.selectedTheme,
                            autoStarted = completedAutoStarted
                        )
                    }
                    PomodoroPhase.ShortBreak -> PomodoroContainer.repository.recordBreakSessionCompleted(
                        breakSeconds = completedState.totalDurationSeconds,
                        completedCycle = false
                    )
                    PomodoroPhase.LongBreak -> {
                        PomodoroContainer.repository.recordBreakSessionCompleted(
                            breakSeconds = completedState.totalDurationSeconds,
                            completedCycle = true
                        )
                        AnalyticsManager.logCycleCompleted(
                            totalToday = completedState.completedPomodoros,
                            cycleDurationMinutes = cycleDurationMinutes(settings),
                            theme = settings.selectedTheme
                        )
                    }
                }
            } else {
                logBreakSkippedIfNeeded(completedState)
            }

            if (alert) {
                playCompletionSound()
                vibrateCompletion()
            }

            val nextPhase = nextPhaseAfter(completedState.phase, completedState.currentSessionNumber, settings)
            val nextSessionNumber = nextSessionNumberAfter(completedState.phase, completedState.currentSessionNumber, settings)
            val nextDuration = durationForPhase(nextPhase, settings)
            val shouldAutoStart = when (nextPhase) {
                PomodoroPhase.Work -> settings.autoStartWork
                PomodoroPhase.ShortBreak,
                PomodoroPhase.LongBreak -> settings.autoStartBreaks
            }
            phaseStartedAutomatically = shouldAutoStart

            state.update {
                it.copy(
                    phase = nextPhase,
                    status = if (shouldAutoStart) TimerStatus.RUNNING else TimerStatus.COMPLETED,
                    currentSessionNumber = nextSessionNumber,
                    totalDurationSeconds = nextDuration,
                    remainingSeconds = nextDuration,
                    progressFraction = 1f
                )
            }
            updateNotificationIfForeground()

            if (shouldAutoStart) {
                startCountdownLoop()
            }
        } catch (exception: Exception) {
            recordTimerException(exception)
        }
    }

    private fun nextPhaseAfter(
        phase: PomodoroPhase,
        currentSessionNumber: Int,
        settings: PomodoroSettings
    ): PomodoroPhase {
        return when (phase) {
            PomodoroPhase.Work -> if (currentSessionNumber >= settings.sessionsBeforeLongBreak) {
                PomodoroPhase.LongBreak
            } else {
                PomodoroPhase.ShortBreak
            }
            PomodoroPhase.ShortBreak,
            PomodoroPhase.LongBreak -> PomodoroPhase.Work
        }
    }

    private fun nextSessionNumberAfter(
        phase: PomodoroPhase,
        currentSessionNumber: Int,
        settings: PomodoroSettings
    ): Int {
        return when (phase) {
            PomodoroPhase.Work -> currentSessionNumber.coerceIn(1, settings.sessionsBeforeLongBreak)
            PomodoroPhase.ShortBreak -> (currentSessionNumber + 1).coerceAtMost(settings.sessionsBeforeLongBreak)
            PomodoroPhase.LongBreak -> 1
        }
    }

    private fun durationForPhase(phase: PomodoroPhase, settings: PomodoroSettings = currentSettings): Int {
        return when (phase) {
            PomodoroPhase.Work -> settings.workDurationSeconds
            PomodoroPhase.ShortBreak -> settings.shortBreakSeconds
            PomodoroPhase.LongBreak -> settings.longBreakSeconds
        }
    }

    private fun cycleDurationMinutes(settings: PomodoroSettings): Int {
        val sessions = settings.sessionsBeforeLongBreak
        val seconds = settings.workDurationSeconds * sessions +
            settings.shortBreakSeconds * (sessions - 1).coerceAtLeast(0) +
            settings.longBreakSeconds
        return seconds / 60
    }

    private fun logBreakSkippedIfNeeded(timerState: PomodoroTimerState) {
        val breakType = when (timerState.phase) {
            PomodoroPhase.Work -> return
            PomodoroPhase.ShortBreak -> "short"
            PomodoroPhase.LongBreak -> "long"
        }
        AnalyticsManager.logBreakSkipped(
            breakType = breakType,
            remainingSeconds = timerState.remainingSeconds.coerceAtLeast(0)
        )
    }

    private fun progressFor(totalDurationSeconds: Int, remainingSeconds: Int): Float {
        if (totalDurationSeconds <= 0) return 1f
        return (remainingSeconds.toFloat() / totalDurationSeconds.toFloat()).coerceIn(0f, 1f)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val notificationManager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notif_channel_description)
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun ensureForeground() {
        val notification = buildNotification()
        if (isForeground) {
            updateNotification(notification)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        isForeground = true
    }

    private fun buildNotification(): Notification {
        val current = state.value
        val pauseOrResumeAction = if (current.status == TimerStatus.RUNNING) ACTION_PAUSE else ACTION_START
        val pauseOrResumeLabel = getString(if (current.status == TimerStatus.RUNNING) R.string.notif_action_pause else R.string.notif_action_resume)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_eye_notification)
            .setContentTitle(notificationTitle(current))
            .setContentText(notificationText(current))
            .setContentIntent(mainActivityPendingIntent())
            .setOngoing(current.status == TimerStatus.RUNNING)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setProgress(
                current.totalDurationSeconds.coerceAtLeast(1),
                (current.totalDurationSeconds - current.remainingSeconds).coerceAtLeast(0),
                false
            )
            .addAction(
                R.drawable.ic_eye_notification,
                pauseOrResumeLabel,
                servicePendingIntent(requestCode = 1, action = pauseOrResumeAction)
            )
            .addAction(
                R.drawable.ic_eye_notification,
                getString(R.string.notif_action_skip),
                servicePendingIntent(requestCode = 2, action = ACTION_SKIP)
            )
            .build()
    }

    private fun notificationTitle(timerState: PomodoroTimerState): String {
        return when (timerState.phase) {
            PomodoroPhase.Work -> getString(R.string.pomodoro_notif_work)
            PomodoroPhase.ShortBreak -> getString(R.string.pomodoro_notif_short_break)
            PomodoroPhase.LongBreak -> getString(R.string.pomodoro_notif_long_break)
        }
    }

    private fun notificationText(timerState: PomodoroTimerState): String {
        return getString(R.string.pomodoro_cd_timer_circle, formatTime(timerState.remainingSeconds))
    }

    private fun formatTime(totalSeconds: Int): String {
        val minutes = totalSeconds.coerceAtLeast(0) / 60
        val seconds = totalSeconds.coerceAtLeast(0) % 60
        return "%02d:%02d".format(minutes, seconds)
    }

    private fun mainActivityPendingIntent(): PendingIntent {
        return PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun servicePendingIntent(requestCode: Int, action: String): PendingIntent {
        return PendingIntent.getService(
            this,
            requestCode,
            Intent(this, PomodoroTimerService::class.java).apply { this.action = action },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun updateNotificationIfForeground() {
        PomodoroWidgetProvider.updateAll(
            context = this,
            timerState = state.value,
            selectedTheme = currentSettings.selectedTheme
        )
        if (!isForeground) return
        updateNotification(buildNotification())
    }

    private fun updateNotification(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) return
        }
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun initializeSoundPool() {
        try {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            soundPool = SoundPool.Builder()
                .setMaxStreams(1)
                .setAudioAttributes(attributes)
                .build()
            soundIds = mapOf(
                "bell" to requireNotNull(soundPool).load(this, R.raw.bell, 1),
                "digital" to requireNotNull(soundPool).load(this, R.raw.digital, 1),
                "soft" to requireNotNull(soundPool).load(this, R.raw.soft, 1)
            )
        } catch (exception: Exception) {
            recordTimerException(exception)
        }
    }

    private fun playCompletionSound() {
        try {
            if (!currentSettings.soundEnabled) return
            val pool = soundPool ?: return
            val soundId = soundIds[currentSettings.notificationSound] ?: soundIds["bell"] ?: return
            pool.play(soundId, 1f, 1f, 1, 0, 1f)
        } catch (exception: Exception) {
            recordTimerException(exception)
        }
    }

    private fun vibrateCompletion() {
        try {
            if (!currentSettings.vibrationEnabled) return
            val pattern = longArrayOf(0, 200, 100, 200)
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = getSystemService(VibratorManager::class.java)
                manager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        } catch (exception: Exception) {
            recordTimerException(exception)
        }
    }

    private fun recordTimerException(exception: Exception) {
        val current = state.value
        FirebaseCrashlytics.getInstance().apply {
            setCustomKey("timer_phase", current.phase.toString())
            setCustomKey("timer_status", current.status.toString())
            setCustomKey("remaining_seconds", current.remainingSeconds)
            setCustomKey("selected_theme", currentSettings.selectedTheme)
            recordException(exception)
        }
    }

    private fun stopTimerJob() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun stopForegroundCompat() {
        isForeground = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    companion object {
        const val CHANNEL_ID = "pomodoro_timer"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.matias.pomodoro.timer.START"
        const val ACTION_PAUSE = "com.matias.pomodoro.timer.PAUSE"
        const val ACTION_SKIP = "com.matias.pomodoro.timer.SKIP"
        const val ACTION_RESET = "com.matias.pomodoro.timer.RESET"
        const val ACTION_RESET_ALL = "com.matias.pomodoro.timer.RESET_ALL"
        const val ACTION_STOP = "com.matias.pomodoro.timer.STOP"

        val state = MutableStateFlow(PomodoroTimerState())

        fun commandIntent(context: Context, action: String): Intent =
            Intent(context, PomodoroTimerService::class.java).apply { this.action = action }
    }
}
