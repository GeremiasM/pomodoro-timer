package com.matias.pomodoro.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.matias.pomodoro.MainActivity
import com.matias.pomodoro.R
import com.matias.pomodoro.timer.TimerSessionStore
import kotlinx.coroutines.*

// ─────────────────────────────────────────────────────────────────────────────
// FOREGROUND SERVICE – TEMPORIZADOR 20-20-20
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Servicio en primer plano que mantiene vivo el temporizador 20-20-20
 * incluso cuando la app está en segundo plano.
 */
class EyeRestService : Service() {

    // ── Constantes de notificación ────────────────────────────────────────────
    companion object {
        const val CHANNEL_PERSISTENT   = "pomodoro_persistent"
        const val CHANNEL_REST_ALERT   = "pomodoro_rest_alert"

        const val NOTIF_ID_PERSISTENT  = 1001
        const val NOTIF_ID_REST_ALERT  = 1002

        const val ACTION_START         = "com.matias.pomodoro.START"
        const val ACTION_STOP          = "com.matias.pomodoro.STOP"
        const val ACTION_REST_NOW      = "com.matias.pomodoro.REST_NOW"

        const val EXTRA_INTERVAL_MINUTES = "interval_minutes"

        /** Crea el intent para iniciar el servicio desde fuera */
        fun startIntent(context: Context, intervalMinutes: Int): Intent =
            Intent(context, EyeRestService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_INTERVAL_MINUTES, intervalMinutes)
            }
    }

    // ── Estado interno ────────────────────────────────────────────────────────
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val timerSessionStore by lazy { TimerSessionStore(this) }
    private var timerJob: Job? = null
    private var intervalMinutes = 20

    // ─────────────────────────────────────────────────────────────────────────
    // CICLO DE VIDA DEL SERVICIO
    // ─────────────────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        intervalMinutes = timerSessionStore.readSession().intervalMinutes
        createNotificationChannels()
        // Llamada inmediata en onCreate para cumplir con los requisitos de Android 12+ y 14+
        // y evitar ForegroundServiceDidNotStartInTimeException.
        startForegroundWithNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        if (action == ACTION_STOP) {
            timerSessionStore.stopTimer(preserveIntervalMinutes = intervalMinutes)
            stopForegroundCompat()
            stopSelf()
            return START_NOT_STICKY
        }

        // Para cualquier otra acción (incluyendo reinicios del sistema con intent nulo),
        // nos aseguramos de estar en primer plano.
        startForegroundWithNotification()

        when (action) {
            ACTION_START -> {
                val newInterval = intent?.getIntExtra(EXTRA_INTERVAL_MINUTES, 20) ?: 20
                intervalMinutes = newInterval.coerceAtLeast(1)
                timerSessionStore.startTimer(intervalMinutes)
                startForegroundWithNotification()
                startCountdownLoop()
            }
            ACTION_REST_NOW -> {
                openRestScreen()
            }
            else -> {
                // Caso por defecto o intent nulo: retomar el bucle si es necesario
                startCountdownLoop()
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        serviceScope.cancel()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NOTIFICACIONES
    // ─────────────────────────────────────────────────────────────────────────

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        NotificationChannel(
            CHANNEL_PERSISTENT,
            getString(R.string.notification_channel_timer_active_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_timer_active_description)
            setShowBadge(false)
            nm.createNotificationChannel(this)
        }

        NotificationChannel(
            CHANNEL_REST_ALERT,
            getString(R.string.notification_channel_rest_alert_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.notification_channel_rest_alert_description)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 200, 100, 200)
            nm.createNotificationChannel(this)
        }
    }

    private fun startForegroundWithNotification() {
        val session = timerSessionStore.readSession()
        intervalMinutes = session.intervalMinutes
        val secondsLeft = if (session.isRunning) {
            timerSessionStore.remainingSeconds(session.nextBreakAtMillis)
        } else {
            intervalMinutes * 60
        }
        val notification = buildPersistentNotification(secondsLeft)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIF_ID_PERSISTENT,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIF_ID_PERSISTENT, notification)
            }
        } catch (e: Exception) {
            // En Android 12+, si se intenta iniciar desde el fondo sin cumplir condiciones, 
            // lanza ForegroundServiceStartNotAllowedException.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Verificamos si es una instancia de la excepción de inicio no permitido
                // usando reflexión o el nombre para evitar problemas de compilación si el SDK no está disponible
                if (e.javaClass.name == "android.app.ForegroundServiceStartNotAllowedException") {
                    stopSelf()
                }
            }
            e.printStackTrace()
        }
    }

    private fun buildPersistentNotification(secondsLeft: Int): Notification {
        val minutes = secondsLeft / 60
        val seconds = secondsLeft % 60

        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val restNowIntent = PendingIntent.getService(
            this, 1,
            Intent(this, EyeRestService::class.java).apply { action = ACTION_REST_NOW },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_PERSISTENT)
            .setSmallIcon(R.drawable.ic_eye_notification)
            .setContentTitle(getString(R.string.notification_timer_active_title))
            .setContentText(getString(R.string.notification_next_break_in_format, minutes, seconds))
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .addAction(
                R.drawable.ic_eye_notification,
                getString(R.string.notification_action_rest_now),
                restNowIntent
            )
            .setProgress(intervalMinutes * 60, (intervalMinutes * 60 - secondsLeft).coerceAtLeast(0), false)
            .build()
    }

    private fun sendRestAlertNotification() {
        val deepLinkIntent = Intent(this, MainActivity::class.java).apply {
            action = "com.matias.pomodoro.app.OPEN_REST_SCREEN" // Consistente con Manifest
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 2, deepLinkIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_REST_ALERT)
            .setSmallIcon(R.drawable.ic_eye_notification)
            .setContentTitle(getString(R.string.notification_rest_alert_title))
            .setContentText(getString(R.string.notification_rest_alert_text_format, intervalMinutes))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(getString(R.string.notification_rest_alert_big_text_format, intervalMinutes))
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
            .build()

        try {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                NotificationManagerCompat.from(this).notify(NOTIF_ID_REST_ALERT, notification)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun startCountdownLoop() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive) {
                val session = timerSessionStore.readSession()
                if (!session.isRunning) {
                    delay(1_000L)
                    continue
                }

                intervalMinutes = session.intervalMinutes
                val remaining = timerSessionStore.remainingSeconds(session.nextBreakAtMillis)

                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(NOTIF_ID_PERSISTENT, buildPersistentNotification(remaining))

                if (remaining <= 0) {
                    sendRestAlertNotification()
                    timerSessionStore.scheduleNextCycle(intervalMinutes)
                    continue
                }

                val notifUpdateInterval = if (remaining > 60) 30_000L else 5_000L
                val delayMillis = notifUpdateInterval.coerceAtMost((remaining * 1_000L).coerceAtLeast(1_000L))
                delay(delayMillis)
            }
        }
    }

    private fun openRestScreen() {
        val intent = Intent(this, MainActivity::class.java).apply {
            action = "com.matias.pomodoro.app.OPEN_REST_SCREEN"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }
}
