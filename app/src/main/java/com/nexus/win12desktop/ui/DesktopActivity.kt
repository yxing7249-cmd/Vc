package com.nexus.win12desktop.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.nexus.win12desktop.NexusApplication
import com.nexus.win12desktop.R
import com.nexus.win12desktop.mouse.VirtualMouseManager

/**
 * Loads the official Win12 Online desktop (1:1 web UI)
 * and overlays a TV remote virtual mouse.
 *
 * Source: https://win12-online.github.io/win12/desktop.html
 */
class DesktopActivity : AppCompatActivity() {

    private lateinit var rootContainer: FrameLayout
    private lateinit var webView: WebView
    private lateinit var loadingOverlay: View

    private var mouseManager: VirtualMouseManager? = null

    companion object {
        private const val WIN12_URL = "https://win12-online.github.io/win12/desktop.html"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )

        setContentView(R.layout.activity_desktop)

        rootContainer = findViewById(R.id.root_container)
        webView = findViewById(R.id.win12_webview)
        loadingOverlay = findViewById(R.id.loading_overlay)

        setupWebView()
        setupMouse()

        webView.loadUrl(WIN12_URL)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.builtInZoomControls = false
        settings.displayZoomControls = false
        settings.setSupportZoom(false)
        settings.userAgentString = settings.userAgentString + " NexusWin12TV/1.0"

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                loadingOverlay.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                loadingOverlay.visibility = View.GONE
                view?.evaluateJavascript(
                    "(function(){try{document.documentElement.classList.add('dark');}catch(e){}})();",
                    null
                )
                mouseManager?.updateScreenSize()
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString() ?: return false
                return if (url.contains("win12-online.github.io") || url.contains("win12")) {
                    false
                } else {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, request?.url))
                    } catch (_: Exception) {}
                    true
                }
            }
        }

        webView.webChromeClient = WebChromeClient()
    }

    private fun setupMouse() {
        mouseManager = VirtualMouseManager(this, rootContainer).apply {
            listener = object : VirtualMouseManager.MouseListener {
                override fun onLeftClick(x: Float, y: Float) {
                    injectClickToWebView(x, y, rightClick = false)
                }

                override fun onRightClick(x: Float, y: Float) {
                    injectClickToWebView(x, y, rightClick = true)
                }

                override fun onMouseMove(x: Float, y: Float) {}
            }
            isEnabled = NexusApplication.getPrefs(this@DesktopActivity)
                .getBoolean("mouse_enabled", true)
        }
    }

    private fun injectClickToWebView(screenX: Float, screenY: Float, rightClick: Boolean) {
        val loc = IntArray(2)
        webView.getLocationOnScreen(loc)
        val x = screenX - loc[0]
        val y = screenY - loc[1]
        if (x < 0 || y < 0 || x > webView.width || y > webView.height) return

        val density = resources.displayMetrics.density
        val cssX = x / density
        val cssY = y / density

        val button = if (rightClick) 2 else 0
        val js = """
            (function(){
              var el = document.elementFromPoint($cssX, $cssY);
              if (!el) return;
              var opts = {bubbles:true, cancelable:true, view:window, clientX:$cssX, clientY:$cssY, button:$button};
              el.dispatchEvent(new MouseEvent('mousedown', opts));
              el.dispatchEvent(new MouseEvent('mouseup', opts));
              el.dispatchEvent(new MouseEvent('click', opts));
              if ($button === 2) {
                el.dispatchEvent(new MouseEvent('contextmenu', opts));
              }
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event == null) return super.onKeyDown(keyCode, event)

        mouseManager?.let {
            if (it.isEnabled && it.onKeyDown(keyCode, event)) return true
        }

        return when (keyCode) {
            KeyEvent.KEYCODE_HOME -> true
            KeyEvent.KEYCODE_BACK -> {
                if (webView.canGoBack()) {
                    webView.goBack()
                    true
                } else {
                    Toast.makeText(this, "\u5df2\u5728 Windows 12 \u684c\u9762", Toast.LENGTH_SHORT).show()
                    true
                }
            }
            KeyEvent.KEYCODE_MENU -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (event == null) return super.onKeyUp(keyCode, event)
        mouseManager?.let {
            if (it.isEnabled && it.onKeyUp(keyCode, event)) return true
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
        mouseManager?.updateScreenSize()
    }

    override fun onPause() {
        webView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        mouseManager?.destroy()
        webView.destroy()
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack()
        else Toast.makeText(this, "NEXUS WIN12 DESKTOP", Toast.LENGTH_SHORT).show()
    }
}
