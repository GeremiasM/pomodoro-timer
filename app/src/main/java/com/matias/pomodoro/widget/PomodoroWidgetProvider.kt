package com.matias.pomodoro.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.widget.RemoteViews
import com.matias.pomodoro.R
import com.matias.pomodoro.service.PomodoroTimerService
import com.matias.pomodoro.timer.PomodoroPhase
import com.matias.pomodoro.timer.PomodoroTimerState
import com.matias.pomodoro.timer.TimerStatus
import kotlin.math.roundToInt

class PomodoroWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        updateWidgets(context, manager, appWidgetIds, PomodoroTimerService.state.value, DEFAULT_THEME)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        updateWidgets(context, manager, intArrayOf(appWidgetId), PomodoroTimerService.state.value, DEFAULT_THEME)
    }

    companion object {
        private const val DEFAULT_THEME = "emerald"
        private const val LARGE_WIDGET_MIN_WIDTH_DP = 220

        fun updateAll(
            context: Context,
            timerState: PomodoroTimerState = PomodoroTimerService.state.value,
            selectedTheme: String = DEFAULT_THEME
        ) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, PomodoroWidgetProvider::class.java))
            updateWidgets(context, manager, ids, timerState, selectedTheme)
        }

        private fun updateWidgets(
            context: Context,
            manager: AppWidgetManager,
            ids: IntArray,
            timerState: PomodoroTimerState,
            selectedTheme: String
        ) {
            ids.forEach { id ->
                val options = manager.getAppWidgetOptions(id)
                val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
                val large = minWidth >= LARGE_WIDGET_MIN_WIDTH_DP
                val views = if (large) {
                    createLargeRemoteViews(context, timerState, selectedTheme)
                } else {
                    createSmallRemoteViews(context, timerState, selectedTheme)
                }
                manager.updateAppWidget(id, views)
            }
        }

        private fun createSmallRemoteViews(
            context: Context,
            timerState: PomodoroTimerState,
            selectedTheme: String
        ): RemoteViews {
            return RemoteViews(context.packageName, R.layout.widget_pomodoro_small).apply {
                applyCommonValues(context, timerState, selectedTheme)
            }
        }

        private fun createLargeRemoteViews(
            context: Context,
            timerState: PomodoroTimerState,
            selectedTheme: String
        ): RemoteViews {
            return RemoteViews(context.packageName, R.layout.widget_pomodoro_large).apply {
                applyCommonValues(context, timerState, selectedTheme)
                setTextViewText(R.id.widget_phase_name, phaseTitle(context, timerState.phase))
                setTextViewText(R.id.widget_today_count, context.getString(R.string.widget_today_count_format, timerState.completedPomodoros))
                setProgressBar(
                    R.id.widget_progress,
                    1000,
                    (timerState.progressFraction.coerceIn(0f, 1f) * 1000f).roundToInt(),
                    false
                )
                setOnClickPendingIntent(R.id.widget_skip, servicePendingIntent(context, PomodoroTimerService.ACTION_SKIP, 12))
            }
        }

        private fun RemoteViews.applyCommonValues(
            context: Context,
            timerState: PomodoroTimerState,
            selectedTheme: String
        ) {
            val primary = primaryColor(selectedTheme, timerState.phase)
            setImageViewBitmap(R.id.widget_background, roundedBackground(primary))
            setTextViewText(R.id.widget_phase_icon, phaseIcon(context, timerState.phase))
            setTextViewText(R.id.widget_time, formatTime(context, timerState.remainingSeconds))
            setImageViewResource(
                R.id.widget_play_pause,
                if (timerState.status == TimerStatus.RUNNING) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
            )
            setOnClickPendingIntent(
                R.id.widget_play_pause,
                servicePendingIntent(
                    context,
                    if (timerState.status == TimerStatus.RUNNING) PomodoroTimerService.ACTION_PAUSE else PomodoroTimerService.ACTION_START,
                    11
                )
            )
        }

        private fun servicePendingIntent(context: Context, action: String, requestCode: Int): PendingIntent {
            return PendingIntent.getService(
                context,
                requestCode,
                PomodoroTimerService.commandIntent(context, action),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        private fun phaseIcon(context: Context, phase: PomodoroPhase): String = when (phase) {
            PomodoroPhase.Work -> context.getString(R.string.widget_phase_icon_work)
            PomodoroPhase.ShortBreak,
            PomodoroPhase.LongBreak -> context.getString(R.string.widget_phase_icon_break)
        }

        private fun phaseTitle(context: Context, phase: PomodoroPhase): String = when (phase) {
            PomodoroPhase.Work -> context.getString(R.string.phase_focus_title)
            PomodoroPhase.ShortBreak -> context.getString(R.string.phase_short_break_title)
            PomodoroPhase.LongBreak -> context.getString(R.string.phase_long_break_title)
        }

        private fun formatTime(context: Context, totalSeconds: Int): String {
            val safeSeconds = totalSeconds.coerceAtLeast(0)
            val minutes = safeSeconds / 60
            val seconds = safeSeconds % 60
            return context.getString(R.string.time_minutes_seconds_format, minutes, seconds)
        }

        private fun roundedBackground(primaryColor: Int): Bitmap {
            val bitmap = Bitmap.createBitmap(720, 180, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(51, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor))
            }
            canvas.drawRoundRect(RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat()), 40f, 40f, paint)
            return bitmap
        }

        private fun primaryColor(selectedTheme: String, phase: PomodoroPhase): Int {
            val color = when (selectedTheme.lowercase()) {
                "sunset" -> when (phase) {
                    PomodoroPhase.Work -> 0xFFE05C3A
                    PomodoroPhase.ShortBreak -> 0xFFE0A03A
                    PomodoroPhase.LongBreak -> 0xFFC03A9E
                }
                "ocean" -> when (phase) {
                    PomodoroPhase.Work -> 0xFF3A7BD5
                    PomodoroPhase.ShortBreak -> 0xFF3AB5A0
                    PomodoroPhase.LongBreak -> 0xFF8B6914
                }
                "forest" -> when (phase) {
                    PomodoroPhase.Work -> 0xFF5AAE3A
                    PomodoroPhase.ShortBreak -> 0xFF3AAE8C
                    PomodoroPhase.LongBreak -> 0xFFAE9A3A
                }
                "lavender" -> when (phase) {
                    PomodoroPhase.Work -> 0xFF8A5AE0
                    PomodoroPhase.ShortBreak -> 0xFF5A80E0
                    PomodoroPhase.LongBreak -> 0xFF5AE0B4
                }
                else -> when (phase) {
                    PomodoroPhase.Work -> 0xFF00C896
                    PomodoroPhase.ShortBreak -> 0xFF00C8C8
                    PomodoroPhase.LongBreak -> 0xFF4A90D9
                }
            }
            return color.toInt()
        }
    }
}
