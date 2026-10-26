package com.art.drawingkids

import android.os.Bundle
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val webView = WebView(this).apply {
            settings.apply {
                javaScriptEnabled = false
                allowFileAccess = true
                builtInZoomControls = false
                displayZoomControls = false
            }
            loadUrl("file:///android_asset/index.html")
        }
        setContentView(webView)

        supportActionBar?.title = getString(R.string.about)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
