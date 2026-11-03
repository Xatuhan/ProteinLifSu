package com.proteinlifsu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.proteinlifsu.ui.CustomFoodScreen
import com.proteinlifsu.ui.HistoryScreen
import com.proteinlifsu.ui.HistoryViewModel
import com.proteinlifsu.ui.SettingsScreen
import com.proteinlifsu.ui.TrackerScreen
import com.proteinlifsu.ui.TrackerViewModel
import com.proteinlifsu.ui.i18n.LocalStrings
import com.proteinlifsu.ui.i18n.stringsFor
import com.proteinlifsu.ui.showEvent
import com.proteinlifsu.ui.theme.LocalAppColors
import com.proteinlifsu.ui.theme.ProteinLifSuTheme
import com.proteinlifsu.ui.theme.isDarkTheme
import kotlinx.coroutines.launch

private enum class Screen { TRACKER, CALENDAR, SETTINGS, NEW_FOOD }

class MainActivity : ComponentActivity() {

    private val viewModel: TrackerViewModel by viewModels { TrackerViewModel.Factory }
    private val historyViewModel: HistoryViewModel by viewModels { HistoryViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // İlk kareler çizilip ana iş parçacığı boşaldığında USDA dizinini arka planda yüklemeye başla.
        android.os.Looper.myQueue().addIdleHandler {
            (application as ProteinLifSuApp).foodIndex.preload()
            false // bir kez çalışsın
        }
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            // Ayarlar okunana kadar (birkaç ms) hiçbir şey çizilmez: yanlış dil/tema yanıp sönmez.
            val current = settings ?: return@setContent
            val dark = isDarkTheme(current.theme)
            val strings = stringsFor(current.language)

            ProteinLifSuTheme(dark = dark) {
                val colors = LocalAppColors.current
                DisposableEffect(dark) {
                    val bar = colors.background.toArgb()
                    val style = if (dark) SystemBarStyle.dark(bar) else SystemBarStyle.light(bar, colors.textPrimary.toArgb())
                    enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                    onDispose {}
                }
                CompositionLocalProvider(LocalStrings provides strings) {
                    var screen by rememberSaveable { mutableStateOf(Screen.TRACKER) }
                    val snackbar = remember { SnackbarHostState() }
                    val scope = rememberCoroutineScope()

                    LaunchedEffect(viewModel, strings) {
                        viewModel.events.collect { e ->
                            scope.launch { showEvent(e, snackbar, strings, viewModel::undoDelete) }
                        }
                    }
                    BackHandler(enabled = screen != Screen.TRACKER) { screen = Screen.TRACKER }

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = colors.background,
                        snackbarHost = { SnackbarHost(snackbar) },
                    ) { inner ->
                        Box(Modifier.fillMaxSize().background(colors.background)) {
                            when (screen) {
                                Screen.TRACKER -> TrackerScreen(
                                    viewModel = viewModel,
                                    contentPadding = inner,
                                    onOpenCalendar = { screen = Screen.CALENDAR },
                                    onOpenSettings = { screen = Screen.SETTINGS },
                                    onCreateFood = { screen = Screen.NEW_FOOD },
                                )
                                Screen.CALENDAR -> HistoryScreen(
                                    viewModel = historyViewModel,
                                    contentPadding = inner,
                                    onBack = { screen = Screen.TRACKER },
                                    onOpenDay = { day ->
                                        viewModel.goToDay(day)
                                        screen = Screen.TRACKER
                                    },
                                )
                                Screen.SETTINGS -> SettingsScreen(
                                    viewModel = viewModel,
                                    settings = current,
                                    contentPadding = inner,
                                    onBack = { screen = Screen.TRACKER },
                                )
                                Screen.NEW_FOOD -> CustomFoodScreen(
                                    viewModel = viewModel,
                                    contentPadding = inner,
                                    onBack = { screen = Screen.TRACKER },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
