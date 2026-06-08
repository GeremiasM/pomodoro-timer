package com.matias.pomodoro

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.matias.pomodoro.data.PomodoroSession
import com.matias.pomodoro.data.preferences.PomodoroPreferences
import com.matias.pomodoro.data.preferences.PomodoroSettings
import com.matias.pomodoro.config.RemoteConfigManager
import com.matias.pomodoro.consent.ConsentManager
import com.matias.pomodoro.timer.PomodoroTimerState
import com.matias.pomodoro.ui.screens.PomodoroScreen
import com.matias.pomodoro.ui.screens.PomodoroStatsScreen
import com.matias.pomodoro.ui.theme.LocalPomodoroColors
import com.matias.pomodoro.ui.theme.PomodoroTheme
import com.matias.pomodoro.viewmodel.PomodoroViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: PomodoroViewModel by viewModels()
    private var remoteUiState by mutableStateOf(RemoteUiState())

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.startTimer()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            val timerState by viewModel.timerState.collectAsStateWithLifecycle()
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val todayStats by viewModel.todayStats.collectAsStateWithLifecycle()
            val weekStats by viewModel.weekStats.collectAsStateWithLifecycle()
            val monthStats by viewModel.monthStats.collectAsStateWithLifecycle()
            val dailyGoalProgress by viewModel.dailyGoalProgress.collectAsStateWithLifecycle()
            val dismissedMotdText by viewModel.dismissedMotdText.collectAsStateWithLifecycle()
            val navController = rememberNavController()

            PomodoroTheme(
                phase = timerState.phase,
                selectedTheme = settings.selectedTheme
            ) {
                val colors = LocalPomodoroColors.current
                SideEffect {
                    val systemBarColor = colors.background.toArgb()
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.dark(systemBarColor),
                        navigationBarStyle = SystemBarStyle.dark(systemBarColor)
                    )
                }

                PomodoroNavHost(
                    navController = navController,
                    timerState = timerState,
                    settings = settings,
                    todayStats = todayStats,
                    weekStats = weekStats,
                    monthStats = monthStats,
                    dailyGoalProgress = dailyGoalProgress,
                    dismissedMotdText = dismissedMotdText,
                    remoteUiState = remoteUiState,
                    onStart = { startTimerWithPermissionCheck() },
                    onPause = viewModel::pauseTimer,
                    onSkip = viewModel::skipPhase,
                    onReset = viewModel::resetPhase,
                    onDismissMotd = viewModel::dismissMotd,
                    onUpdateSettings = viewModel::updateSettings
                )

                if (remoteUiState.forceUpdateRequired) {
                    ForceUpdateDialog(onUpdate = ::openPlayStoreListing)
                }
            }
        }

        requestConsentAndRemoteConfig()
    }

    override fun onStart() {
        super.onStart()
        (application as PomodoroApplication).adInterstitialManager.attachActivity(this)
    }

    override fun onStop() {
        (application as PomodoroApplication).adInterstitialManager.detachActivity(this)
        super.onStop()
    }

    private fun startTimerWithPermissionCheck() {
        if (hasNotificationPermission()) {
            viewModel.startTimer()
        } else {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun hasNotificationPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestConsentAndRemoteConfig() {
        val app = application as PomodoroApplication
        ConsentManager(this).requestConsent { consentObtained ->
            var adsSdkInitialized = false
            var remoteConfigFetched = false

            fun updateRemoteUiState() {
                if (!remoteConfigFetched) return
                val canShowAds = consentObtained && adsSdkInitialized
                val interstitialEnabled = canShowAds && RemoteConfigManager.adInterstitialEnabled
                app.adInterstitialManager.configure(
                    interstitialEnabled = interstitialEnabled,
                    interstitialFrequency = RemoteConfigManager.adInterstitialFrequency
                )
                remoteUiState = RemoteUiState(
                    featureStatsEnabled = RemoteConfigManager.featureStatsEnabled,
                    featureDailyGoalEnabled = RemoteConfigManager.featureDailyGoalEnabled,
                    adBannerEnabled = canShowAds && RemoteConfigManager.adBannerEnabled,
                    motdText = RemoteConfigManager.motdText,
                    motdEnabled = RemoteConfigManager.motdEnabled,
                    forceUpdateRequired = BuildConfig.VERSION_CODE < RemoteConfigManager.minSupportedVersionCode
                )
            }

            if (consentObtained) {
                app.initializeMobileAds {
                    adsSdkInitialized = true
                    updateRemoteUiState()
                }
            }
            RemoteConfigManager.fetchAndActivate {
                remoteConfigFetched = true
                updateRemoteUiState()
            }
        }
    }

    private fun openPlayStoreListing() {
        val marketIntent = Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val webIntent = Intent(
            Intent.ACTION_VIEW,
            "https://play.google.com/store/apps/details?id=$packageName".toUri()
        )
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(marketIntent)
        } catch (_: ActivityNotFoundException) {
            startActivity(webIntent)
        }
    }
}

private data class RemoteUiState(
    val featureStatsEnabled: Boolean = true,
    val featureDailyGoalEnabled: Boolean = true,
    val adBannerEnabled: Boolean = false,
    val motdText: String = "",
    val motdEnabled: Boolean = false,
    val forceUpdateRequired: Boolean = false
)

@Composable
private fun ForceUpdateDialog(onUpdate: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(text = androidx.compose.ui.res.stringResource(R.string.force_update_title)) },
        text = { Text(text = androidx.compose.ui.res.stringResource(R.string.force_update_message)) },
        confirmButton = {
            TextButton(onClick = onUpdate) {
                Text(text = androidx.compose.ui.res.stringResource(R.string.force_update_button))
            }
        }
    )
}

@Composable
private fun PomodoroNavHost(
    navController: NavHostController,
    timerState: PomodoroTimerState,
    settings: PomodoroSettings,
    todayStats: PomodoroSession?,
    weekStats: List<PomodoroSession>,
    monthStats: List<PomodoroSession>,
    dailyGoalProgress: Float,
    dismissedMotdText: String,
    remoteUiState: RemoteUiState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onSkip: () -> Unit,
    onReset: () -> Unit,
    onDismissMotd: (String) -> Unit,
    onUpdateSettings: (String, String, suspend PomodoroPreferences.() -> Unit) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = ROUTE_MAIN,
        enterTransition = {
            slideInHorizontally(
                animationSpec = tween(350, easing = EaseInOutCubic),
                initialOffsetX = { it }
            )
        },
        exitTransition = {
            slideOutHorizontally(
                animationSpec = tween(350, easing = EaseInOutCubic),
                targetOffsetX = { -it }
            )
        },
        popEnterTransition = {
            slideInHorizontally(
                animationSpec = tween(350, easing = EaseInOutCubic),
                initialOffsetX = { -it }
            )
        },
        popExitTransition = {
            slideOutHorizontally(
                animationSpec = tween(350, easing = EaseInOutCubic),
                targetOffsetX = { it }
            )
        }
    ) {
        composable(ROUTE_MAIN) {
            PomodoroScreen(
                timerState = timerState,
                settings = settings,
                todayStats = todayStats,
                dailyGoalProgress = dailyGoalProgress,
                featureStatsEnabled = remoteUiState.featureStatsEnabled,
                featureDailyGoalEnabled = remoteUiState.featureDailyGoalEnabled,
                motdText = remoteUiState.motdText,
                motdEnabled = remoteUiState.motdEnabled,
                dismissedMotdText = dismissedMotdText,
                onStart = onStart,
                onPause = onPause,
                onSkip = onSkip,
                onReset = onReset,
                onOpenStats = {
                    if (remoteUiState.featureStatsEnabled) {
                        navController.navigate(ROUTE_STATS) { launchSingleTop = true }
                    }
                },
                onDismissMotd = onDismissMotd,
                onUpdateSettings = onUpdateSettings,
                modifier = Modifier.fillMaxSize()
            )
        }
        composable(ROUTE_STATS) {
            PomodoroStatsScreen(
                settings = settings,
                todayStats = todayStats,
                weekStats = weekStats,
                monthStats = monthStats,
                dailyGoalProgress = dailyGoalProgress,
                featureDailyGoalEnabled = remoteUiState.featureDailyGoalEnabled,
                adBannerEnabled = remoteUiState.adBannerEnabled,
                onBack = { navController.navigateUp() },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private const val ROUTE_MAIN = "main"
private const val ROUTE_STATS = "stats"
