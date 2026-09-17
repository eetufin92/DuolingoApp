package com.eetu.duolingoapp.ui.dialogs

import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eetu.duolingoapp.data.DuolingoSettingsManager
import com.eetu.duolingoapp.ui.theme.DuoBlue
import com.eetu.duolingoapp.ui.theme.DuoGreen
import kotlinx.coroutines.launch

@Composable
fun SettingsDialog(
    settingsManager: DuolingoSettingsManager,
    blockTelemetry: Boolean,
    desktopUserAgent: Boolean,
    enableSpeaking: Boolean,
    webView: WebView?,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Duolingo Settings",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Block Telemetry
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Block Telemetry",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Blocks tracker calls to excess.duolingo.com, Adjust, etc.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    Switch(
                        checked = blockTelemetry,
                        onCheckedChange = { checked ->
                            scope.launch { settingsManager.setBlockTelemetry(checked) }
                        }
                    )
                }

                HorizontalDivider()

                // Enable Speaking Practice
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Speaking Practice",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Allows microphone access for speech recognition",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    Switch(
                        checked = enableSpeaking,
                        onCheckedChange = { checked ->
                            scope.launch { settingsManager.setEnableSpeaking(checked) }
                        }
                    )
                }

                HorizontalDivider()

                // Desktop User Agent
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Desktop User-Agent",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Emulates Linux desktop browser to avoid mobile traps",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    Switch(
                        checked = desktopUserAgent,
                        onCheckedChange = { checked ->
                            scope.launch {
                                settingsManager.setDesktopUserAgent(checked)
                                webView?.reload()
                            }
                        }
                    )
                }

                HorizontalDivider()

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            webView?.loadUrl("https://www.duolingo.com/learn")
                            onDismiss()
                        }
                    ) {
                        Text("Go to Learn")
                    }

                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            webView?.clearCache(true)
                            webView?.reload()
                            onDismiss()
                        }
                    ) {
                        Text("Reload Page")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = DuoGreen)
            ) {
                Text("Done")
            }
        }
    )
}
