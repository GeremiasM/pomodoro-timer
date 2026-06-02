package com.matias.pomodoro.ads

import android.app.Activity
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.FullScreenContentCallback

class InterstitialAdManager(private val activity: Activity) {
    private var interstitialAd: InterstitialAd? = null
    private var adLoadedAtElapsedMs: Long = 0L
    private val TAG = "PomodoroAds"
    private var isLoadingAd = false
    private var isShowingAd = false
    private var pendingShowRequest = false
    private var pendingOnComplete: (() -> Unit)? = null
    private var failedLoadAttempts = 0
    private val adExpirationMs = 55 * 60 * 1_000L

    // ID DE PRUEBA de Google (Úsalo para verificar que funciona)
    //private val adUnitId = "ca-app-pub-3940256099942544/1033173712"
    
    // Tu ID Real (Cámbialo aquí cuando el de prueba funcione)
    private val adUnitId = "ca-app-pub-4712794855635991/4854958191"

    fun loadAd() {
        if (isLoadingAd) return
        discardExpiredAdIfNeeded()
        if (interstitialAd != null) return

        isLoadingAd = true
        val adRequest = AdRequest.Builder().build()
        Log.d(TAG, "Solicitando carga de anuncio...")
        
        InterstitialAd.load(activity, adUnitId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e(TAG, "Error al cargar: ${adError.message}. Código: ${adError.code}")
                isLoadingAd = false
                interstitialAd = null
                adLoadedAtElapsedMs = 0L
                failedLoadAttempts++
                retryLoadIfNeeded()
            }

            override fun onAdLoaded(ad: InterstitialAd) {
                Log.d(TAG, "¡Anuncio cargado con éxito!")
                isLoadingAd = false
                interstitialAd = ad
                adLoadedAtElapsedMs = SystemClock.elapsedRealtime()
                failedLoadAttempts = 0

                if (pendingShowRequest && !isShowingAd) {
                    pendingShowRequest = false
                    showLoadedAd(ad, consumePendingOnComplete())
                }
            }
        })
    }

    fun showAd(onComplete: () -> Unit) {
        if (isShowingAd) {
            enqueueShow(onComplete)
            return
        }

        discardExpiredAdIfNeeded()
        val ad = interstitialAd
        if (ad == null) {
            Log.d(TAG, "Anuncio no listo. Encolando show para cuando termine de cargar.")
            enqueueShow(onComplete)
            loadAd()
            return
        }

        showLoadedAd(ad, onComplete)
    }

    private fun showLoadedAd(ad: InterstitialAd, onComplete: () -> Unit) {
        interstitialAd = null
        adLoadedAtElapsedMs = 0L
        isShowingAd = true
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Anuncio cerrado.")
                isShowingAd = false
                onComplete()
                loadAd()
                flushQueuedShowIfAny()
            }

            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                Log.e(TAG, "Error al mostrar: ${adError.message}")
                isShowingAd = false
                onComplete()
                loadAd()
                flushQueuedShowIfAny()
            }
        }
        Log.d(TAG, "Mostrando anuncio...")
        ad.show(activity)
    }

    private fun discardExpiredAdIfNeeded() {
        if (interstitialAd == null) return
        val ageMs = SystemClock.elapsedRealtime() - adLoadedAtElapsedMs
        if (ageMs >= adExpirationMs) {
            Log.d(TAG, "Anuncio expirado en caché (${ageMs}ms). Se descarta y recarga.")
            interstitialAd = null
            adLoadedAtElapsedMs = 0L
        }
    }

    private fun enqueueShow(onComplete: () -> Unit) {
        pendingShowRequest = true
        val previous = pendingOnComplete
        pendingOnComplete = {
            previous?.invoke()
            onComplete()
        }
    }

    private fun flushQueuedShowIfAny() {
        if (!pendingShowRequest || isShowingAd) return

        val ad = interstitialAd
        if (ad != null) {
            pendingShowRequest = false
            showLoadedAd(ad, consumePendingOnComplete())
        } else {
            loadAd()
        }
    }

    private fun consumePendingOnComplete(): () -> Unit {
        val callback = pendingOnComplete ?: {}
        pendingOnComplete = null
        return callback
    }

    private fun retryLoadIfNeeded() {
        if (!pendingShowRequest) return
        if (isLoadingAd || interstitialAd != null) return
        if (failedLoadAttempts > 3) {
            Log.w(TAG, "No fue posible cargar anuncio tras varios intentos.")
            pendingShowRequest = false
            consumePendingOnComplete().invoke()
            return
        }

        loadAd()
    }
}
