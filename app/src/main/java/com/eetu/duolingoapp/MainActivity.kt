package com.eetu.duolingoapp

import android.Manifest
import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.eetu.duolingoapp.data.DuolingoSettingsManager
import com.eetu.duolingoapp.ui.components.DuolingoWebView
import com.eetu.duolingoapp.ui.dialogs.SettingsDialog
import com.eetu.duolingoapp.ui.theme.DuolingoAppTheme
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val settingsManager = DuolingoSettingsManager(this)

        setContent {
            DuolingoAppTheme {
                MainScreen(settingsManager = settingsManager)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(settingsManager: DuolingoSettingsManager) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val blockTelemetry by settingsManager.blockTelemetryFlow.collectAsState(initial = true)
    val desktopUserAgent by settingsManager.desktopUserAgentFlow.collectAsState(initial = false)
    val enableSpeaking by settingsManager.enableSpeakingFlow.collectAsState(initial = true)

    // Audio permission launcher for speaking exercises
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            android.util.Log.d("MainActivity", "Audio permission granted by user")
        }
    }

    // Android back navigation: go back in WebView history if possible
    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            DuolingoWebView(
                modifier = Modifier.fillMaxSize(),
                initialUrl = "https://www.duolingo.com/learn",
                blockTelemetry = blockTelemetry,
                desktopUserAgent = desktopUserAgent,
                onLoadingStateChanged = { loading -> isLoading = loading },
                onRequestAudioPermission = {
                    if (enableSpeaking) {
                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                webViewInstance = { instance ->
                    webView = instance
                }
            )

            // Draggable, auto-dimming Floating Settings Button
            var offsetX by remember { mutableFloatStateOf(0f) }
            var offsetY by remember { mutableFloatStateOf(0f) }
            var isInteracting by remember { mutableStateOf(false) }

            val fabAlpha by animateFloatAsState(
                targetValue = if (isInteracting) 0.95f else 0.25f,
                label = "fabAlpha"
            )

            LaunchedEffect(isInteracting) {
                if (isInteracting) {
                    delay(3500)
                    isInteracting = false
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset {
                        IntOffset(
                            offsetX.roundToInt(),
                            offsetY.roundToInt()
                        )
                    }
                    .alpha(fabAlpha)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { isInteracting = true },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                isInteracting = true
                                offsetX += dragAmount.x
                                offsetY += dragAmount.y
                            },
                            onDragEnd = { isInteracting = true }
                        )
                    }
                    .padding(end = 6.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 4.dp
            ) {
                IconButton(
                    onClick = {
                        isInteracting = true
                        showSettingsDialog = true
                    },
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            if (showSettingsDialog) {
                SettingsDialog(
                    settingsManager = settingsManager,
                    blockTelemetry = blockTelemetry,
                    desktopUserAgent = desktopUserAgent,
                    enableSpeaking = enableSpeaking,
                    webView = webView,
                    onDismiss = { showSettingsDialog = false }
                )
            }
        }
    }
}
