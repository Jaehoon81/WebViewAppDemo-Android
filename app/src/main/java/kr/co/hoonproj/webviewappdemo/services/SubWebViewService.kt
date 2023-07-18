package kr.co.hoonproj.webviewappdemo.services

import android.os.Message
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class SubWebViewService {

    fun makeSubWebView(parentWebView: WebView): WebView {
        val subWebView = WebView(parentWebView.context).apply {
            layoutParams = parentWebView.layoutParams

            webViewClient = WebViewCompat.getWebViewClient(parentWebView)
            webChromeClient = WebViewCompat.getWebChromeClient(parentWebView)
            settings.run {
                domStorageEnabled = true
                javaScriptEnabled = true
                javaScriptCanOpenWindowsAutomatically = true
                setSupportMultipleWindows(true)
            }
        }
        return subWebView
    }

    suspend fun getPopupUrl(dummyWebView: WebView, message: Message): String? = suspendCancellableCoroutine { continuation ->
        val tempWebViewClient = WebViewCompat.getWebViewClient(dummyWebView)
        dummyWebView.run {
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    // 무거운 웹 페이지를 새 팝업창으로 로드할 경우, Crash가 발생할 수 있으므로 DummyWebView 방식 사용
                    dummyWebView.webViewClient = tempWebViewClient
                    continuation.resume(url)
                    view?.stopLoading()

//                    return super.shouldOverrideUrlLoading(view, request)
                    return true
                }
            }
        }
        val transport = message.obj as WebView.WebViewTransport
        transport.webView = dummyWebView
        message.sendToTarget()
    }
}