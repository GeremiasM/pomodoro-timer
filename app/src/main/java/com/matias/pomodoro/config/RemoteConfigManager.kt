package com.matias.pomodoro.config

import android.content.Context
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.matias.pomodoro.BuildConfig
import com.matias.pomodoro.R

object RemoteConfigManager {
    private lateinit var remoteConfig: FirebaseRemoteConfig

    fun init(context: Context) {
        remoteConfig = Firebase.remoteConfig
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
        val interval = if (BuildConfig.DEBUG) 0L else 3_600L
        remoteConfig.setConfigSettingsAsync(
            remoteConfigSettings {
                minimumFetchIntervalInSeconds = interval
            }
        )
    }

    fun fetchAndActivate(onComplete: () -> Unit) {
        if (!::remoteConfig.isInitialized) {
            onComplete()
            return
        }
        remoteConfig.fetchAndActivate().addOnCompleteListener { onComplete() }
    }

    val adInterstitialEnabled: Boolean
        get() = remoteConfig.getBoolean(KEY_AD_INTERSTITIAL_ENABLED)

    val adInterstitialFrequency: Int
        get() = remoteConfig.getLong(KEY_AD_INTERSTITIAL_FREQUENCY).toInt().coerceAtLeast(1)

    val adBannerEnabled: Boolean
        get() = remoteConfig.getBoolean(KEY_AD_BANNER_ENABLED)

    val featureDailyGoalEnabled: Boolean
        get() = remoteConfig.getBoolean(KEY_FEATURE_DAILY_GOAL_ENABLED)

    val featureStatsEnabled: Boolean
        get() = remoteConfig.getBoolean(KEY_FEATURE_STATS_ENABLED)

    val minSupportedVersionCode: Int
        get() = remoteConfig.getLong(KEY_MIN_SUPPORTED_VERSION_CODE).toInt()

    val motdText: String
        get() = remoteConfig.getString(KEY_MOTD_TEXT)

    val motdEnabled: Boolean
        get() = remoteConfig.getBoolean(KEY_MOTD_ENABLED)

    private const val KEY_AD_INTERSTITIAL_FREQUENCY = "ad_interstitial_frequency"
    private const val KEY_AD_INTERSTITIAL_ENABLED = "ad_interstitial_enabled"
    private const val KEY_AD_BANNER_ENABLED = "ad_banner_enabled"
    private const val KEY_FEATURE_DAILY_GOAL_ENABLED = "feature_daily_goal_enabled"
    private const val KEY_FEATURE_STATS_ENABLED = "feature_stats_enabled"
    private const val KEY_MIN_SUPPORTED_VERSION_CODE = "min_supported_version_code"
    private const val KEY_MOTD_TEXT = "motd_text"
    private const val KEY_MOTD_ENABLED = "motd_enabled"
}
