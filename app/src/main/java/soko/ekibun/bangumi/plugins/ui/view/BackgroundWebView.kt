package soko.ekibun.bangumi.plugins.ui.view

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.util.Log
import android.webkit.*

class BackgroundWebView(context: Context): WebView(context) {
    var onPageFinished = {_:String?->}
    var onInterceptRequest={_: WebResourceRequest ->}

    var uiHandler: Handler = Handler{true}

    override fun loadUrl(url: String) {
        Log.v("BGWebView", "load url: $url")
        super.loadUrl(url)
    }

    fun finish(){
        if(Thread.currentThread() != uiHandler.looper.thread){
            uiHandler.post{ finish() }
            return
        }
        onPause()
        clearCache(true)
        clearHistory()
        destroy()
    }

    init{
        @SuppressLint("SetJavaScriptEnabled")
        settings.javaScriptEnabled = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.setSupportMultipleWindows(true)
        settings.domStorageEnabled = true
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        settings.blockNetworkImage = true
        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                onPageFinished(url)
                super.onPageFinished(view, url)
            }
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url.toString()
                if(!url.startsWith("http")){
                    return true
                }
                return false
            }
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                Log.v("BGWebView", "intercept ${request.url} ${request.requestHeaders}")
                onInterceptRequest(request)
                return super.shouldInterceptRequest(view, request)
            }
        }
    }
}
