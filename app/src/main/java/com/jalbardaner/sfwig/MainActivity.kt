package com.jalbardaner.sfwig

import android.net.Uri
import android.os.Bundle
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

    private val backCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            myWebView.goBack()
        }
    }

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
                view.evaluateJavascript(FILTER_SCRIPT, null)
            }

            override fun onPageFinished(view: WebView, url: String?) {
                super.onPageFinished(view, url)
                view.evaluateJavascript(FILTER_SCRIPT, null)
            }

            override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
                super.doUpdateVisitedHistory(view, url, isReload)
                backCallback.isEnabled = view.canGoBack()
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

        // Hides (instead of removing) the doom-scrolling entry points whenever the page changes.
        // Removing nodes that React owns breaks Instagram's navigation, and re-evaluating on every
        // change means a Back button hidden too early is shown again once the chat has loaded.
        private val FILTER_SCRIPT = """
            (function () {
                if (window.__sfwig) return;
                window.__sfwig = true;

                var hidden = [];

                function ancestor(el, levels) {
                    while (el && levels-- > 0) el = el.parentElement;
                    return el;
                }

                // Only ever hide small nav bar items, never something that holds the page itself
                // (login form, cookie dialog, feed) even while it is still loading
                function isSafe(target) {
                    return target && target !== document.body && target !== document.documentElement &&
                        !target.querySelector('main, [role="main"], [role="dialog"], article, form, input, textarea') &&
                        target.getElementsByTagName('*').length * 2 < document.body.getElementsByTagName('*').length;
                }

                function collect(label, levels, out) {
                    document.querySelectorAll('[aria-label="' + label + '"]').forEach(function (el) {
                        // Skip icons inside something already hidden, like the old remove() did
                        if (out.some(function (t) { return t.contains(el); })) return;
                        var target = ancestor(el, levels);
                        if (isSafe(target)) out.push(target);
                    });
                }

                function update() {
                    var targets = [];
                    collect('Reels', 8, targets);
                    collect('Explore', 8, targets);
                    collect('Home', 9, targets);

                    var inConversation = location.pathname.indexOf('/direct/t/') === 0 ||
                        document.querySelector('[aria-label="Conversation information"]');
                    if (document.querySelector('[aria-label="Notifications"]')) collect('Back', 9, targets);
                    else if (!inConversation) collect('Back', 5, targets);

                    hidden.forEach(function (el) {
                        if (targets.indexOf(el) < 0) el.style.removeProperty('display');
                    });
                    targets.forEach(function (el) {
                        el.style.setProperty('display', 'none', 'important');
                    });
                    hidden = targets;
                }

                var pending = false;
                new MutationObserver(function () {
                    if (pending) return;
                    pending = true;
                    setTimeout(function () {
                        pending = false;
                        update();
                    }, 100);
                }).observe(document, { childList: true, subtree: true });
            })();
        """.trimIndent()
    }
}
