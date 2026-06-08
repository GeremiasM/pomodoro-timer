package com.matias.pomodoro.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

object AnalyticsManager {
    private lateinit var analytics: FirebaseAnalytics

    fun init(context: Context) {
        analytics = FirebaseAnalytics.getInstance(context.applicationContext)
    }

    fun logPomodoroCompleted(
        sessionNumber: Int,
        durationSeconds: Int,
        theme: String,
        autoStarted: Boolean
    ) = logEvent("pomodoro_completed") {
        putInt("session_number", sessionNumber)
        putInt("phase_duration_seconds", durationSeconds)
        putString("theme", theme)
        putBoolean("auto_started", autoStarted)
    }

    fun logCycleCompleted(
        totalToday: Int,
        cycleDurationMinutes: Int,
        theme: String
    ) = logEvent("cycle_completed") {
        putInt("total_pomodoros_today", totalToday)
        putInt("cycle_duration_minutes", cycleDurationMinutes)
        putString("theme", theme)
    }

    fun logBreakSkipped(breakType: String, remainingSeconds: Int) = logEvent("break_skipped") {
        putString("break_type", breakType)
        putInt("remaining_seconds", remainingSeconds)
    }

    fun logSettingsChanged(settingName: String, newValue: String) = logEvent("settings_changed") {
        putString("setting_name", settingName)
        putString("new_value", newValue)
    }

    fun logThemeChanged(fromTheme: String, toTheme: String) = logEvent("theme_changed") {
        putString("from_theme", fromTheme)
        putString("to_theme", toTheme)
    }

    fun logDailyGoalReached(goalCount: Int, totalFocusMinutes: Int) = logEvent("daily_goal_reached") {
        putInt("goal_count", goalCount)
        putInt("total_focus_minutes", totalFocusMinutes)
    }

    fun logStatsViewed(period: String) = logEvent("stats_viewed") {
        putString("period", period)
    }

    private fun logEvent(name: String, buildParams: Bundle.() -> Unit) {
        if (!::analytics.isInitialized) return
        analytics.logEvent(name, Bundle().apply(buildParams))
    }
}
