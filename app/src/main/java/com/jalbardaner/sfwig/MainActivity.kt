package com.jalbardaner.sfwig

import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts


class MainActivity : ComponentActivity() {
    private lateinit var myWebView: WebView

    // Hides the doom-scrolling entry points and adds chat gestures, see assets/sfwig.js
    private val pageScript by lazy { assets.open("sfwig.js").bufferedReader().use { it.readText() } }

    private val backCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            // Overlays such as a reel opened from a chat have no history entry of their own, so
            // close them first instead of going back past the chat
            myWebView.evaluateJavascript(CLOSE_OVERLAY_SCRIPT) { closed ->
                if (closed == "true") return@evaluateJavascript
                if (myWebView.canGoBack()) {
                    myWebView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        }
    }

    private var lastFollowingRedirect = 0L

    // Pending <input type="file"> request from Instagram's upload dialogs (posts, stories, profile picture)
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    private val fileChooser = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        filePathCallback?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data))
        filePathCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        myWebView = findViewById(R.id.webview)

        myWebView.settings.apply {
            javaScriptEnabled = true
            // Instagram keeps the dark mode choice and "pop-up already dismissed" flags in
            // localStorage, which WebView disables by default
            domStorageEnabled = true
        }

        myWebView.webViewClient = object : WebViewClient() {
            // Inject as soon as the page is drawn so blocked items barely flash, and again when it
            // finishes in case the first attempt was too early. The script ignores repeated injections.
            override fun onPageCommitVisible(view: WebView, url: String?) {
                super.onPageCommitVisible(view, url)
                view.evaluateJavascript(pageScript, null)
            }

            override fun onPageFinished(view: WebView, url: String?) {
                super.onPageFinished(view, url)
                view.evaluateJavascript(pageScript, null)
            }

            // Instagram's root page is the "For you" feed (it lands there after login, and going back
            // reaches it), so swap it for "Following" in place, leaving no For you entry in the history
            override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
                super.doUpdateVisitedHistory(view, url, isReload)
                val uri = Uri.parse(url ?: return)
                val isForYou = uri.host == "www.instagram.com" && uri.path.orEmpty().trimEnd('/').isEmpty() &&
                    uri.getQueryParameter("variant") != "following"
                // The time check stops a redirect loop in case Instagram ever drops the parameter
                val now = SystemClock.elapsedRealtime()
                if (isForYou && now - lastFollowingRedirect > 5000) {
                    lastFollowingRedirect = now
                    view.evaluateJavascript("location.replace('$HOME_URL')", null)
                }
            }
        }

        myWebView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView,
                callback: ValueCallback<Array<Uri>>,
                params: FileChooserParams
            ): Boolean {
                filePathCallback?.onReceiveValue(null)
                filePathCallback = callback
                return try {
                    fileChooser.launch(params.createIntent())
                    true
                } catch (e: android.content.ActivityNotFoundException) {
                    filePathCallback = null
                    false
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, backCallback)

        if (savedInstanceState == null || myWebView.restoreState(savedInstanceState) == null) {
            myWebView.loadUrl(HOME_URL)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        myWebView.saveState(outState)
    }

    override fun onResume() {
        super.onResume()
        myWebView.onResume()
    }

    override fun onPause() {
        // Write the login cookies to disk now, otherwise they can be lost if the app is killed
        CookieManager.getInstance().flush()
        myWebView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        myWebView.destroy()
        super.onDestroy()
    }

    companion object {
        private const val HOME_URL = "https://www.instagram.com/?variant=following"

        // Closes the topmost overlay and returns true, or returns false when there is none: a dialog
        // (reaction picker, post, menu), or else a full-screen viewer like a reel opened from a chat,
        // which is not a dialog but a fixed layer with a Close button. If the same overlay survived
        // the previous attempt, give up and return false so Back never gets stuck.
        private val CLOSE_OVERLAY_SCRIPT = """
            (function () {
                function visible(e) { return e.getClientRects().length > 0; }
                var dialog = null, viewerClose = null;
                document.querySelectorAll('[role="dialog"]').forEach(function (d) {
                    if (visible(d)) dialog = d;
                });
                if (!dialog) document.querySelectorAll('[aria-label="Close"]').forEach(function (c) {
                    if (!visible(c)) return;
                    for (var e = c, i = 0; e && i < 6; e = e.parentElement, i++) {
                        if (getComputedStyle(e).position === 'fixed') { viewerClose = c; return; }
                    }
                });
                var overlay = dialog || viewerClose;
                if (!overlay || overlay === window.__sfwigLastOverlay) return false;
                window.__sfwigLastOverlay = overlay;
                var close = dialog ? dialog.querySelector('[aria-label="Close"]') : viewerClose;
                if (close) {
                    (close.closest('button, [role="button"]') || close)
                        .dispatchEvent(new MouseEvent('click', { bubbles: true }));
                } else {
                    // Instagram listens for Escape on the focused element inside the dialog
                    (document.activeElement || document.body).dispatchEvent(
                        new KeyboardEvent('keydown', { key: 'Escape', code: 'Escape', keyCode: 27, bubbles: true }));
                }
                return true;
            })();
        """.trimIndent()
    }
}
