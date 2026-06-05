package com.matias.pomodoro.di

import com.matias.pomodoro.data.PomodoroDatabase
import com.matias.pomodoro.data.PomodoroRepository
import com.matias.pomodoro.data.preferences.PomodoroPreferences

object PomodoroContainer {
    lateinit var database: PomodoroDatabase
    lateinit var repository: PomodoroRepository
    lateinit var preferences: PomodoroPreferences
}
