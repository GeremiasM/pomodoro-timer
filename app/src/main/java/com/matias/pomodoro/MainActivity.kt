package com.matias.pomodoro

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.matias.pomodoro.ads.InterstitialAdManager
import com.matias.pomodoro.ui.screens.MainScreen
import com.matias.pomodoro.ui.theme.PomodoroTheme
import com.matias.pomodoro.viewmodel.PomodoroEffect
import com.matias.pomodoro.viewmodel.PomodoroIntent
import com.matias.pomodoro.viewmodel.PomodoroViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// ACTIVITY PRINCIPAL
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Activity única de Pomodoro (Single-Activity Architecture).
 */
class MainActivity : ComponentActivity() {

    companion object {
        /** Action para abrir directamente la pantalla de descanso activo */
        const val ACTION_OPEN_REST_SCREEN = "com.matias.pomodoro.app.OPEN_REST_SCREEN"
    }

    private val viewModel: PomodoroViewModel by viewModels()
    private lateinit var adManager: InterstitialAdManager
    private var restSkippedByUser = false
    private var firstContentFrameRendered = false

    // ── Launcher para solicitar permiso de notificaciones ─────────────────────
    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showAdAfterDelay(3_000L)
            viewModel.handleIntent(PomodoroIntent.StartTimer)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LIFECYCLE
    // ─────────────────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Crear el manager; la carga se difiere hasta que la UI pinte el primer frame.
        adManager = InterstitialAdManager(this)

        // Registro del callback para manejar overlays con botón atrás
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val uiState = viewModel.state.value
                if (uiState.isFullscreen) {
                    viewModel.handleIntent(PomodoroIntent.ToggleFullscreen)
                    return
                }

                if (uiState.isRestScreenVisible) {
                    viewModel.handleIntent(PomodoroIntent.DismissRestScreen)
                    return
                }

                // Salida normal sin mostrar anuncio al presionar atrás.
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                isEnabled = true
            }
        })

        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableImmersiveMode()

        handleIncomingIntent(intent)

        setContent {
            val uiState by viewModel.state.collectAsStateWithLifecycle()
            PomodoroTheme(visualScheme = uiState.colorScheme) {
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(Unit) {
                    withFrameNanos { }
                    delay(500L)
                    firstContentFrameRendered = true
                    adManager.loadAd()
                }

                LaunchedEffect(Unit) {
                    viewModel.effects.collect { effect ->
                        when (effect) {
                            is PomodoroEffect.ShowNotificationPermissionRequest ->
                                requestNotificationPermission()

                            is PomodoroEffect.RestCompleted -> {
                                launch {
                                    snackbarHostState.showSnackbar(getString(R.string.snackbar_rest_completed))
                                }
                                if (restSkippedByUser) {
                                    restSkippedByUser = false
                                } else {
                                    showAdAfterDelay(2_000L)
                                }
                            }

                            is PomodoroEffect.ShowSnackbar ->
                                snackbarHostState.showSnackbar(effect.message)
                        }
                    }
                }

                MainScreen(
                    uiState  = uiState,
                    onIntent = { intent ->
                        if (intent is PomodoroIntent.DismissRestScreen && uiState.isRestScreenVisible) {
                            restSkippedByUser = true
                            showAdAfterDelay(0L)
                        }

                        if (intent is PomodoroIntent.StartTimer && !hasNotificationPermission()) {
                            requestNotificationPermission()
                        } else {
                            if (intent is PomodoroIntent.StartTimer) {
                                showAdAfterDelay(3_000L)
                            }
                            viewModel.handleIntent(intent)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                SnackbarHost(hostState = snackbarHostState)
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enableImmersiveMode()
    }

    override fun onResume() {
        super.onResume()
        if (::adManager.isInitialized && firstContentFrameRendered) {
            adManager.loadAd()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MANEJO DE INTENTS EXTERNOS
    // ─────────────────────────────────────────────────────────────────────────

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == ACTION_OPEN_REST_SCREEN) {
            viewModel.handleIntent(PomodoroIntent.TriggerRestNow)
        }
    }

    private fun showAdAfterDelay(delayMillis: Long) {
        lifecycleScope.launch {
            delay(delayMillis)
            adManager.showAd {
                adManager.loadAd()
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PERMISOS
    // ─────────────────────────────────────────────────────────────────────────

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermission()) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun enableImmersiveMode() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}
