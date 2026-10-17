package com.huijui.sunext.ui
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.huijui.sunext.domain.model.StartupState
import com.huijui.sunext.domain.usecase.ApplyLanguageUseCase
import com.huijui.sunext.domain.usecase.EnsureManagerInstalledUseCase
import com.huijui.sunext.domain.usecase.ObserveStartupStateUseCase
import com.huijui.sunext.ui.activity.util.ThemeUtils
import com.huijui.sunext.ui.theme.KernelSUTheme
import com.huijui.sunext.ui.viewmodel.HomeUiAction
import com.huijui.sunext.ui.viewmodel.HomeViewModel
import com.huijui.sunext.ui.viewmodel.ModuleUiAction
import com.huijui.sunext.ui.viewmodel.ModuleViewModel
import com.huijui.sunext.ui.viewmodel.SettingsUiAction
import com.huijui.sunext.ui.viewmodel.SettingsUiEvent
import com.huijui.sunext.ui.viewmodel.SettingsViewModel
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private val homeViewModel: HomeViewModel by viewModel()
    private val moduleViewModel: ModuleViewModel by viewModel()
    private val settingsViewModel: SettingsViewModel by viewModel()
    private val observeStartupState: ObserveStartupStateUseCase by inject()
    private val ensureManagerInstalled: EnsureManagerInstalledUseCase by inject()
    private val themeUtils: ThemeUtils by inject()
    private val applyLanguage: ApplyLanguageUseCase by inject()
    private val startupState by lazy { observeStartupState() }

    private var isInitialized = false

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let(applyLanguage::invoke))
    }

    private val intentChannel = Channel<Intent>(capacity = Channel.BUFFERED)

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            val splashScreen = installSplashScreen()

            // Enable edge to edge
            enableEdgeToEdge()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }

            super.onCreate(savedInstanceState)

            splashScreen.setKeepOnScreenCondition {
                when (startupState.value) {
                    StartupState.Loading -> true
                    StartupState.Ready -> !homeViewModel.uiState.value.isInitialDataLoaded
                    is StartupState.Failed -> false
                }
            }

            // Keep the home state active even when an incoming intent opens another screen.
            lifecycleScope.launch {
                homeViewModel.uiState.first { it.isInitialDataLoaded }
            }

            lifecycleScope.launch { ensureManagerInstalled() }
            lifecycleScope.launch {
                settingsViewModel.events.collect { event ->
                    when (event) {
                        is SettingsUiEvent.Error -> if (event.message.isNotBlank()) {
                            Toast.makeText(this@MainActivity, event.message, Toast.LENGTH_LONG)
                                .show()
                        }

                        is SettingsUiEvent.Message -> {
                            val message = event.formatArg?.let {
                                getString(event.stringResource, it)
                            } ?: getString(event.stringResource)
                            Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                        }

                        SettingsUiEvent.RestartActivity -> recreate()
                    }
                }
            }

            // Initialize app state once.
            if (!isInitialized) {
                initializeData()
                isInitialized = true
            }

            if (savedInstanceState == null) intent?.let { intentChannel.trySend(it) }

            setContent {
                val prefs = this@MainActivity.getSharedPreferences("huijui_prefs", android.content.Context.MODE_PRIVATE)
                var hasSeenOnboarding by androidx.compose.runtime.remember {
                    androidx.compose.runtime.mutableStateOf(prefs.getBoolean("has_seen_onboarding", false))
                }

                KernelSUTheme {
                    if (!hasSeenOnboarding) {
                        com.huijui.sunext.ui.screen.OnboardingScreen(
                            onFinish = {
                                prefs.edit().putBoolean("has_seen_onboarding", true).apply()
                                hasSeenOnboarding = true
                            }
                        )
                    } else {
                        androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.huijui.sunext.R.drawable.huijui_wallpaper),
                            contentDescription = "Background",
                            modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        when (val state = startupState.collectAsStateWithLifecycle().value) {
                        is StartupState.Failed -> StartupFailureContent(state.message)
                        else -> NavContainer(settingsViewModel, intentChannel)
                        }
                    }
                }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intentChannel.trySend(intent)
    }

    private fun initializeData() {
        lifecycleScope.launch {
            try {
                homeViewModel.dispatch(HomeUiAction.Refresh(showIndicator = false))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Initialize theme settings.
        themeUtils.initializeThemeSettings(settingsViewModel)
    }

    override fun onResume() {
        try {
            super.onResume()
            themeUtils.onActivityResume()
            synchronizeUiSettings()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun synchronizeUiSettings() {
        if (!isInitialized) return

        settingsViewModel.dispatch(SettingsUiAction.Initialize)
        moduleViewModel.dispatch(ModuleUiAction.ReloadSettings)
    }

    override fun onPause() {
        try {
            super.onPause()
            themeUtils.onActivityPause()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@androidx.compose.runtime.Composable
private fun StartupFailureContent(reason: String) {
    Column(
        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = reason,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
