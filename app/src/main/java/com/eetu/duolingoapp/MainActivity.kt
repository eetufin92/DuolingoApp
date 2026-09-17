package com.eetu.duolingoapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.eetu.duolingoapp.data.DuolingoSettingsManager
import com.eetu.duolingoapp.ui.components.DuolingoWebView
import com.eetu.duolingoapp.ui.dialogs.SettingsDialog
import com.eetu.duolingoapp.ui.theme.DuoGreen
import com.eetu.duolingoapp.ui.theme.DuolingoAppTheme

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

            // Minimalist quick action buttons in top-right corner
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 6.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                shadowElevation = 3.dp
            ) {
                androidx.compose.foundation.layout.Row {
                    IconButton(
                        onClick = { webView?.reload() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reload",
                            tint = DuoGreen
                        )
                    }

                    IconButton(
                        onClick = { showSettingsDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
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
