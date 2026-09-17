package com.honeynotify.demo

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorView: TextView
    private val websiteUri = Uri.parse(BuildConfig.WEBVIEW_URL)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        require(websiteUri.scheme == "https" && !websiteUri.host.isNullOrBlank()) {
            "Set a valid HTTPS webview_url in demo.properties"
        }

        createView()
        requestNotificationPermission()
        HoneyNotifyClient.get(applicationContext).createNotificationChannels()
        HoneyNotifyClient.register(applicationContext)
        handleNotificationIntent(intent, trackOpen = true)

        if (savedInstanceState == null && webView.url == null) {
            webView.loadUrl(BuildConfig.WEBVIEW_URL)
        } else if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent, trackOpen = true)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        webView.stopLoading()
        webView.destroy()
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    @Suppress("SetJavaScriptEnabled")
    private fun createView() {
        val container = FrameLayout(this)
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            webViewClient = DemoWebViewClient()
        }
        progressBar = ProgressBar(this).apply {
            visibility = View.GONE
        }
        errorView = TextView(this).apply {
            text = "Unable to load the website.\n\nTap to retry."
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            textSize = 18f
            setPadding(48, 48, 48, 48)
            visibility = View.GONE
            setOnClickListener {
                visibility = View.GONE
                webView.loadUrl(BuildConfig.WEBVIEW_URL)
            }
        }

        container.addView(
            webView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        container.addView(
            progressBar,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                android.view.Gravity.CENTER
            )
        )
        container.addView(
            errorView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        setContentView(container)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION)
        }
    }

    private fun handleNotificationIntent(intent: Intent, trackOpen: Boolean) {
        val data = intent.extras?.keySet()?.associateWith { key ->
            intent.extras?.get(key)?.toString().orEmpty()
        }.orEmpty()
        val notification = HoneyNotifyClient.get(applicationContext).notificationFrom(data)

        if (trackOpen && notification.id != null) {
            Thread {
                runCatching {
                    HoneyNotifyClient.get(applicationContext).trackOpened(data)
                }
            }.start()
        }

        notification.clickUrl?.let(::openUrl)
    }

    private fun openUrl(value: String) {
        val uri = Uri.parse(value)
        if (uri.scheme == "https" && uri.host.equals(websiteUri.host, ignoreCase = true)) {
            webView.loadUrl(value)
        } else {
            openExternal(uri)
        }
    }

    private fun openExternal(uri: Uri): Boolean {
        return try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    private inner class DemoWebViewClient : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            progressBar.visibility = View.VISIBLE
            errorView.visibility = View.GONE
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            progressBar.visibility = View.GONE
        }

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val uri = request.url
            return if (uri.scheme == "https" && uri.host.equals(websiteUri.host, ignoreCase = true)) {
                false
            } else {
                openExternal(uri)
            }
        }

        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: WebResourceError
        ) {
            if (request.isForMainFrame) {
                progressBar.visibility = View.GONE
                errorView.visibility = View.VISIBLE
            }
        }
    }

    companion object {
        private const val NOTIFICATION_PERMISSION = 1001
    }
}
