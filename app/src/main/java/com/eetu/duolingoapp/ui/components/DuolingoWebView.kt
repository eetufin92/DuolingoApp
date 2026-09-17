package com.eetu.duolingoapp.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import java.io.ByteArrayInputStream

private const val TAG = "DuolingoWebView"

private val TELEMETRY_HOSTS = setOf(
    "excess.duolingo.com",
    "app.adjust.com",
    "af4a.adj.st",
    "analytics.google.com",
    "googletagmanager.com",
    "connect.facebook.net",
    "graph.facebook.com",
    "api.branch.io"
)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DuolingoWebView(
    modifier: Modifier = Modifier,
    initialUrl: String = "https://www.duolingo.com/learn",
    blockTelemetry: Boolean = true,
    desktopUserAgent: Boolean = false,
    onLoadingStateChanged: (Boolean) -> Unit = {},
    onRequestAudioPermission: () -> Unit = {},
    webViewInstance: (WebView) -> Unit = {}
) {
    val context = LocalContext.current
    var progress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(true) }

    val injectJs = remember {
        try {
            context.assets.open("duolingo_inject.js").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load duolingo_inject.js", e)
            ""
        }
    }

    val injectCss = remember {
        try {
            context.assets.open("duolingo_inject.css").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load duolingo_inject.css", e)
            ""
        }
    }

    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                mediaPlaybackRequiresUserGesture = false
                useWideViewPort = true
                loadWithOverviewMode = true
                setSupportZoom(false)
                builtInZoomControls = false
                displayZoomControls = false
                cacheMode = WebSettings.LOAD_DEFAULT
                allowFileAccess = false
                allowContentAccess = false

                if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                    WebSettingsCompat.setAlgorithmicDarkeningAllowed(this, true)
                }
                if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                    WebSettingsCompat.setForceDark(this, WebSettingsCompat.FORCE_DARK_AUTO)
                }
            }

            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(this, true)

            // Early document-start JavaScript injection if supported
            if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT) && injectJs.isNotEmpty()) {
                try {
                    WebViewCompat.addDocumentStartJavaScript(
                        this,
                        injectJs,
                        setOf("https://*.duolingo.com", "https://duolingo.com")
                    )
                    Log.d(TAG, "DOCUMENT_START_SCRIPT successfully registered")
                } catch (e: Exception) {
                    Log.w(TAG, "DOCUMENT_START_SCRIPT registration failed", e)
                }
            }
        }
    }

    // Keep parent updated with instance
    LaunchedEffect(webView) {
        webViewInstance(webView)
    }

    // Update User-Agent dynamically when setting changes
    LaunchedEffect(desktopUserAgent) {
        if (desktopUserAgent) {
            webView.settings.userAgentString =
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
        } else {
            webView.settings.userAgentString = null // Reset to default mobile UA
        }
    }

    // WebChromeClient handles microphone requests, progress, and console
    val chromeClient = remember(context) {
        object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest?) {
                if (request == null) return
                val audioRequested = request.resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)

                if (audioRequested) {
                    val hasRecordPerm = androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.RECORD_AUDIO
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                    if (hasRecordPerm) {
                        request.grant(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE))
                        Log.d(TAG, "Granted RESOURCE_AUDIO_CAPTURE to Web Chrome")
                    } else {
                        onRequestAudioPermission()
                        request.grant(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE))
                    }
                } else {
                    request.deny()
                }
            }

            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progress = newProgress / 100f
                val loading = newProgress < 100
                if (loading != isLoading) {
                    isLoading = loading
                    onLoadingStateChanged(loading)
                }
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                if (consoleMessage != null) {
                    Log.d(TAG, "[WebConsole] ${consoleMessage.message()} -- From line ${consoleMessage.lineNumber()} of ${consoleMessage.sourceId()}")
                }
                return true
            }
        }
    }

    // WebViewClient handles URL loading, telemetry blocking, and script injection fallbacks
    val viewClient = remember(blockTelemetry, injectJs, injectCss) {
        object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString() ?: return false
                val uri = Uri.parse(url)
                val host = uri.host?.lowercase() ?: ""
                val scheme = uri.scheme?.lowercase() ?: ""

                // 1. Block and swallow store intents and market links
                if (scheme == "market" || scheme == "intent" ||
                    url.contains("play.google.com/store/apps/details?id=com.duolingo") ||
                    url.contains("apps.apple.com") ||
                    url.contains("app.adjust.com")
                ) {
                    Log.d(TAG, "Intercepted and suppressed store/app redirect: $url")
                    return true
                }

                // 2. Allow Duolingo domains and authentication providers
                if (host.endsWith("duolingo.com") || host.endsWith("duolingo.cn") ||
                    host.endsWith("google.com") || host.endsWith("facebook.com") ||
                    host.endsWith("apple.com")
                ) {
                    return false
                }

                // 3. Open other external links in standard browser
                return try {
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    context.startActivity(intent)
                    true
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to open external URL: $url", e)
                    true
                }
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                if (!blockTelemetry) return super.shouldInterceptRequest(view, request)

                val uri = request?.url ?: return super.shouldInterceptRequest(view, request)
                val host = uri.host?.lowercase() ?: ""
                val path = uri.path?.lowercase() ?: ""

                // Block known telemetry hosts or batch telemetry paths
                val isTelemetry = TELEMETRY_HOSTS.any { host.contains(it) } ||
                        (host.endsWith("duolingo.com") && path.startsWith("/batch"))

                if (isTelemetry) {
                    Log.d(TAG, "Blocked telemetry request to: $uri")
                    return WebResourceResponse(
                        "text/plain",
                        "UTF-8",
                        204,
                        "No Content",
                        mapOf(
                            "Access-Control-Allow-Origin" to "*",
                            "Access-Control-Allow-Methods" to "GET, POST, OPTIONS",
                            "Access-Control-Allow-Headers" to "*"
                        ),
                        ByteArrayInputStream(ByteArray(0))
                    )
                }

                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                injectAntiNagCode(view)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                injectAntiNagCode(view)
                // If on / splash or onboarding context, navigate forward to /learn
                if (url != null && (url.contains("context=webToApp") || url.endsWith("/nojs/splash"))) {
                    view?.loadUrl("https://www.duolingo.com/learn")
                }
            }

            private fun injectAntiNagCode(view: WebView?) {
                if (view == null) return

                // Fallback JS injection
                if (injectJs.isNotEmpty()) {
                    view.evaluateJavascript(injectJs, null)
                }

                // CSS injection via JS
                if (injectCss.isNotEmpty()) {
                    val encodedCss = android.util.Base64.encodeToString(
                        injectCss.toByteArray(),
                        android.util.Base64.NO_WRAP
                    )
                    val cssScript = """
                        (function() {
                            if (!document.getElementById('duo-anti-nag-inline-css')) {
                                var s = document.createElement('style');
                                s.id = 'duo-anti-nag-inline-css';
                                s.textContent = window.atob('$encodedCss');
                                (document.head || document.documentElement).appendChild(s);
                            }
                        })();
                    """.trimIndent()
                    view.evaluateJavascript(cssScript, null)
                }
            }
        }
    }

    DisposableEffect(webView) {
        webView.webChromeClient = chromeClient
        webView.webViewClient = viewClient
        webView.loadUrl(initialUrl)

        onDispose {
            webView.stopLoading()
            (webView.parent as? ViewGroup)?.removeView(webView)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                (webView.parent as? ViewGroup)?.removeView(webView)
                webView
            }
        )
    }
}
