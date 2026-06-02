package com.matias.pomodoro

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.matias.pomodoro.data.PomodoroDatabase
import com.matias.pomodoro.data.PomodoroDateUtils
import com.matias.pomodoro.data.PomodoroRepository
import com.matias.pomodoro.data.preferences.PomodoroPreferences
import com.matias.pomodoro.di.PomodoroContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PomodoroApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        PomodoroContainer.database = PomodoroDatabase.getInstance(this)
        PomodoroContainer.repository = PomodoroRepository(
            PomodoroContainer.database.pomodoroSessionDao()
        )
        PomodoroContainer.preferences = PomodoroPreferences(this)

        applicationScope.launch {
            PomodoroContainer.repository.deleteOlderThan(PomodoroDateUtils.dateDaysAgo(365))
        }

        applicationScope.launch {
            MobileAds.initialize(this@PomodoroApplication) {}
        }
    }
}
