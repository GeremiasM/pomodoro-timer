package com.matias.pomodoro.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.lang.ref.WeakReference

class AdInterstitialManager(private val context: Context) {
    private var interstitialAd: InterstitialAd? = null
    private var activityRef: WeakReference<Activity>? = null
    private var isLoading = false
    private var isShowing = false
    private var enabled = false

    fun attachActivity(activity: Activity) {
        activityRef = WeakReference(activity)
    }

    fun detachActivity(activity: Activity) {
        if (activityRef?.get() === activity) {
            activityRef = null
        }
    }

    fun configure(interstitialEnabled: Boolean) {
        enabled = interstitialEnabled
        if (enabled) {
            preload()
        } else {
            interstitialAd = null
            isLoading = false
        }
    }

    private fun preload() {
        if (!enabled || isLoading || interstitialAd != null) return
        isLoading = true
        InterstitialAd.load(
            context.applicationContext,
            INTERSTITIAL_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    isLoading = false
                    interstitialAd = null
                }

                override fun onAdLoaded(ad: InterstitialAd) {
                    isLoading = false
                    interstitialAd = ad
                }
            }
        )
    }

    fun show(onFinished: () -> Unit = {}) {
        if (!enabled || isShowing) {
            onFinished()
            return
        }

        val ad = interstitialAd
        if (ad == null) {
            preload()
            onFinished()
            return
        }

        val activity = activityRef?.get()
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            preload()
            onFinished()
            return
        }

        interstitialAd = null
        isShowing = true
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                isShowing = false
                preload()
                onFinished()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                isShowing = false
                preload()
                onFinished()
            }

            override fun onAdShowedFullScreenContent() {
                interstitialAd = null
            }
        }
        ad.show(activity)
    }

    private companion object {
        const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-4712794855635991/5894516585"
    }
}
