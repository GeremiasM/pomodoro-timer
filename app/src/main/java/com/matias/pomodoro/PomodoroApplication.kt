package com.matias.pomodoro

import android.app.Application
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PomodoroApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Inicializar el SDK de anuncios de forma asíncrona
        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(this@PomodoroApplication) {}
        }
    }
}
