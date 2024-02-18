package com.jalbardaner.sfwig

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.ValueCallback
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import kotlinx.coroutines.*


class MainActivity : ComponentActivity() {
    private lateinit var myWebView: WebView

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        myWebView = findViewById(R.id.webview)
        myWebView.getSettings().setJavaScriptEnabled(true);

        // Check if WebView supports dark mode
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            // Enable dark mode for web content
            WebSettingsCompat.setForceDark(myWebView.settings, WebSettingsCompat.FORCE_DARK_ON)
        }

        myWebView.loadUrl("https://www.instagram.com/?variant=following")

        myWebView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                // Schedule the periodic HTML update
                scheduleHtmlUpdate()
            }
        }
    }

    private fun scheduleHtmlUpdate() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                // Fetch the HTML content and update the TextView
                updateHtmlTextView()

                // Schedule the next update after 3 seconds
                handler.postDelayed(this, 1000)
            }
        }, 1000)
    }

    private fun updateHtmlTextView() {
        val script = """
            var conversation = document.querySelector('[aria-label="Conversation information"]');
            var notifications = document.querySelector('[aria-label="Notifications"]');
            var element = document.querySelector('[aria-label="Back"]');
            if(element && notifications)element.parentElement.parentElement.parentElement.parentElement.parentElement.parentElement.parentElement.parentElement.parentElement.remove();
            else if (element && !conversation) element.parentElement.parentElement.parentElement.parentElement.parentElement.remove();
            element = document.querySelector('[aria-label="Reels"]');
            if(element)element.parentElement.parentElement.parentElement.parentElement.parentElement.parentElement.parentElement.parentElement.remove();
            element = document.querySelector('[aria-label="Explore"]');
            if(element)element.parentElement.parentElement.parentElement.parentElement.parentElement.parentElement.parentElement.parentElement.remove();
            element = document.querySelector('[aria-label="Home"]');
            if(element)element.parentElement.parentElement.parentElement.parentElement.parentElement.parentElement.parentElement.parentElement.parentElement.remove();
        """

        // Execute the JavaScript in the WebView and receive the result in the callback
        myWebView.evaluateJavascript(script, null)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove any pending callbacks to avoid memory leaks
        handler.removeCallbacksAndMessages(null)
    }

    override fun onBackPressed() {
        // Check if the WebView can go back
        if (myWebView.canGoBack()) {
            // If yes, go back in the WebView
            myWebView.goBack()
        } else {
            // If not, perform the default back action (exit the app)
            super.onBackPressed()
        }
    }

}




