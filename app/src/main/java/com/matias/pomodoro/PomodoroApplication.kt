package com.matias.pomodoro

import android.app.Application
import android.os.Handler
import android.os.Looper
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.matias.pomodoro.ads.AdInterstitialManager
import com.matias.pomodoro.analytics.AnalyticsManager
import com.matias.pomodoro.config.RemoteConfigManager
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
    lateinit var adInterstitialManager: AdInterstitialManager
        private set
    private val mobileAdsInitializationCallbacks = mutableListOf<() -> Unit>()
    private var mobileAdsInitializationStarted = false
    private var mobileAdsInitialized = false

    override fun onCreate() {
        super.onCreate()

        val database = PomodoroDatabase.getInstance(this)
        PomodoroContainer.repository = PomodoroRepository(
            database.pomodoroSessionDao()
        )
        PomodoroContainer.preferences = PomodoroPreferences(this)
        AnalyticsManager.init(this)
        RemoteConfigManager.init()
        adInterstitialManager = AdInterstitialManager(this)

        applicationScope.launch {
            PomodoroContainer.repository.deleteOlderThan(PomodoroDateUtils.dateDaysAgo(365))
        }
    }

    fun initializeMobileAds(onInitialized: () -> Unit = {}) {
        if (mobileAdsInitialized) {
            onInitialized()
            return
        }

        mobileAdsInitializationCallbacks += onInitialized
        if (mobileAdsInitializationStarted) return
        mobileAdsInitializationStarted = true

        if (BuildConfig.DEBUG) {
            val requestConfiguration = RequestConfiguration.Builder()
                .setTestDeviceIds(listOf(DEBUG_TEST_DEVICE_ID))
                .build()
            MobileAds.setRequestConfiguration(requestConfiguration)
        }

        MobileAds.initialize(this) {
            Handler(Looper.getMainLooper()).post {
                mobileAdsInitialized = true
                val callbacks = mobileAdsInitializationCallbacks.toList()
                mobileAdsInitializationCallbacks.clear()
                callbacks.forEach { it() }
            }
        }
    }

    private companion object {
        const val DEBUG_TEST_DEVICE_ID = "992B532AFED7E70954E089CA6D78F721"
    }
}
