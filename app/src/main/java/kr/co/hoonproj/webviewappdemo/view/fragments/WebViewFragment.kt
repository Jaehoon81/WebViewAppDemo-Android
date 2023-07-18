package kr.co.hoonproj.webviewappdemo.view.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.view.*
import android.webkit.*
import androidx.fragment.app.Fragment
import com.google.android.material.progressindicator.LinearProgressIndicator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kr.co.hoonproj.webviewappdemo.R
import kr.co.hoonproj.webviewappdemo.WebViewAppDemo
import kr.co.hoonproj.webviewappdemo.databinding.FragmentWebViewBinding
import kr.co.hoonproj.webviewappdemo.services.SubWebViewService
import kr.co.hoonproj.webviewappdemo.services.WebViewService
import kr.co.hoonproj.webviewappdemo.utils.*
import kr.co.hoonproj.webviewappdemo.view.MainActivity
import kr.co.hoonproj.webviewappdemo.view.customs.CustomPopupView
import kr.co.hoonproj.webviewappdemo.view.customs.OnPopupChangedListener
import kr.co.hoonproj.webviewappdemo.view.listeners.OnKeyboardChangedListener
import kr.co.hoonproj.webviewappdemo.viewmodel.MainViewModel

private const val TAG: String = "[WebViewAppDemo] WebViewFragment"

@AndroidEntryPoint
class WebViewFragment : Fragment(), OnKeyboardChangedListener {

    companion object {
        fun newInstance() = WebViewFragment()
    }

    private val mBinding: FragmentWebViewBinding by lazy(LazyThreadSafetyMode.NONE) {
        FragmentWebViewBinding.inflate(layoutInflater)
    }
    private lateinit var mainViewModel: MainViewModel
    private var isFragmentPaused: Boolean = false

    private lateinit var currentWebView: WebView
    private lateinit var webViewService: WebViewService
    private val subWebViewList: ArrayList<CustomPopupView> = arrayListOf()
    private val subProgressBarList: ArrayList<LinearProgressIndicator> = arrayListOf()

    private lateinit var keyboardVisibility: KeyboardVisibility
    private var isVisibleOnShowKeyboard: Boolean = false

    private var tabTag: String? = null
    private var defaultUrl: String? = null
    private var reloadUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // WebViewFragment를 ViewPager에 추가 시, 자동으로 tag 값이 부여된다.
        // (onCreate() 순서대로 f0, f1, f2, f3)
        tag?.let { tabTag ->
            Log.i(TAG, "WebViewFragment_$tabTag:: onCreate($savedInstanceState)")

            this.tabTag = tabTag
            setFragmentResultListener(tabTag)
        }
    }

    /**
     * MainActivity 등에서 예시와 같이 setFragmentResult()를 실행하면 아래의 Listener가 호출된다.
     *
     * val targetTabTag = "f1"
     * val targetUrl = "https://www.nate.com"
     * try {
     *     supportFragmentManager.setFragmentResult(targetTabTag, bundleOf("targetUrl" to targetUrl))
     *     mainViewModel.bottomTabIndex.postValue(1)
     * } catch (e: Exception) {
     *     Log.e(TAG, e.stackTraceToString())
     * }
     */
    private fun setFragmentResultListener(tabTag: String) {
        parentFragmentManager.setFragmentResultListener(tabTag, this) { key, bundle ->
            Log.d(TAG, "FragmentResultListener_Key: $key, Bundle: $bundle")

            val targetUrl = bundle.getString("targetUrl")
            targetUrl?.let {
                defaultUrl = it
                mBinding.webView.loadUrl(it)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mainViewModel = (requireActivity() as MainActivity).mainViewModel
        mBinding.mainViewModel = mainViewModel
        mBinding.lifecycleOwner = requireActivity()

        initWebView()
        setOnScrollChangeListener()
        setKeyboardVisibility()

        // Inflate the layout for this fragment
//        return inflater.inflate(R.layout.fragment_web_view, container, false)
        return mBinding.root
    }

    @SuppressLint("JavascriptInterface")
    private fun initWebView() {
        currentWebView = mBinding.webView

        mBinding.webView.webViewClient = CustomWebViewClient()
        mBinding.webView.webChromeClient = CustomWebChromeClient()

        webViewService = WebViewService(requireContext(), mBinding.webView, mainViewModel)
        mBinding.webView.addJavascriptInterface(webViewService, "android")
        WebView.setWebContentsDebuggingEnabled(true)

        // 웹뷰(브라우저) 정보를 웹 서버에서 확인 및 구분하기 위한 커스텀 설정
        mBinding.webView.settings.userAgentString += " WebViewAppDemo(tab:${tabTag?: ""})"
        // 웹뷰가 Html의 ViewPort 메타태그를 지원한다. (Html 컨텐츠가 웹뷰에 모두 보이게)
        mBinding.webView.settings.useWideViewPort = true
        // Html 컨텐츠가 웹뷰보다 클 경우, 스크린 크기에 맞게 조정한다.
        mBinding.webView.settings.loadWithOverviewMode = true

        // DomStorage(Html의 LocalStorage에 해당) 사용여부 설정
        mBinding.webView.settings.domStorageEnabled = true
        // JavaScript 사용여부 설정
        mBinding.webView.settings.javaScriptEnabled = true
        // 웹뷰에서 새창(팝업창) 실행 허용 (window.open()의 정상 동작을 위해 필요)
        mBinding.webView.settings.javaScriptCanOpenWindowsAutomatically = true
        // 웹뷰에서 새창(팝업창)의 복수실행 허용
        mBinding.webView.settings.setSupportMultipleWindows(true)

        // 웹뷰를 통한 Content Url 접근여부 설정
        mBinding.webView.settings.allowContentAccess = true
        // 웹뷰에서 Ssl 인증서가 없는 Https 주소거나 Http 주소로 접속 시, 경고 화면이 표시되지 않게 설정
        mBinding.webView.settings.safeBrowsingEnabled = false

        // 웹뷰의 속도 개선을 위한 설정
        mBinding.webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
            mBinding.webView.settings.setRenderPriority(WebSettings.RenderPriority.HIGH)
            mBinding.webView.settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.SINGLE_COLUMN
            mBinding.webView.settings.setEnableSmoothTransition(true)
        }
        // 웹뷰의 캐시모드 설정 (캐시를 사용하되, 기간이 만료되어 없으면 네트워크에서 신규 로드)
        mBinding.webView.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
    }
    
    private fun setOnScrollChangeListener() {
        mBinding.webView.setOnScrollChangeListener { view, newX, newY, oldX, oldY ->
            if ((newY - oldY) > 5) {
                (requireActivity() as MainActivity).scrollBottomNavigationView(false)
            } else if ((newY - oldY) < -5) {
                (requireActivity() as MainActivity).scrollBottomNavigationView(true)
            }
        }
    }

    private fun setKeyboardVisibility() {
        keyboardVisibility = KeyboardVisibility(
            requireActivity().window,
            onShowKeyboard = { keyboardHeight -> onShowKeyboard(tabTag) },
            onHideKeyboard = { onHideKeyboard(tabTag) }
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "WebViewFragment_$tabTag:: onViewCreated()")

        val targetUrl = arguments?.getString("targetUrl")
        Log.d(TAG, "Target_Url($tabTag): $targetUrl")

        targetUrl?.let { mBinding.webView.loadUrl(it) }
        defaultUrl = targetUrl
        reloadUrl = targetUrl
    }

    override fun onStart() {
        super.onStart()
        Log.i(TAG, "WebViewFragment_$tabTag:: onStart()")
    }

    override fun onResume() {
        super.onResume()

        // 처음 실행 시에는 onStart() -> onResume() -> onPause() -> onResume() 순서로 호출된다.
        // 첫번째 onResume()은 로직의 중복 방지를 위해 건너뛴다.
        if (isFragmentPaused == true) {
            isFragmentPaused = false
            Log.i(TAG, "WebViewFragment_$tabTag:: onResume()")

            defaultUrl?.let {
                // 1. 각 하단 탭을 누를 때마다 해당 화면의 초기 페이지를 로드하고 싶을 경우
//                mBinding.webView.loadUrl(it)
                // 2. 각 하단 탭의 화면을 그대로 유지하고 싶은 경우
                defaultUrl = null
            }
        }
    }

    override fun onPause() {
        super.onPause()
        Log.i(TAG, "WebViewFragment_$tabTag:: onPause()")

        isFragmentPaused = true
    }

    override fun onStop() {
        super.onStop()
        Log.i(TAG, "WebViewFragment_$tabTag:: onStop()")
    }

    override fun onDestroyView() {
        Log.i(TAG, "WebViewFragment_$tabTag:: onDestroyView()")

        keyboardVisibility.detachKeyboardListener()
        super.onDestroyView()
    }

    override fun onDestroy() {
        Log.i(TAG, "WebViewFragment_$tabTag:: onDestroy()")

//        mBinding = null
        super.onDestroy()
    }

    override fun onShowKeyboard(tabTag: String?) {
        val currentTabIndex = WebViewAppDemo.prefs.bottomTabIndex
        val targetTabIndex = tabTag?.substring(1, 2)!!.toInt()
        if (currentTabIndex == targetTabIndex) {
            (requireActivity() as MainActivity).showBottomNavigationBar()

            val isBottomNaviViewVisible = (requireActivity() as MainActivity).isBottomNavigationViewVisible()
            if (isBottomNaviViewVisible == true) {
                isVisibleOnShowKeyboard = true
                EventBus.isBottomTabsVisible = false
                EventBus.post(GlobalEvent.IsBottomNaviViewHidden)
            }
        }
    }

    override fun onHideKeyboard(tabTag: String?) {
        val currentTabIndex = WebViewAppDemo.prefs.bottomTabIndex
        val targetTabIndex = tabTag?.substring(1, 2)!!.toInt()
        if (currentTabIndex == targetTabIndex) {
            (requireActivity() as MainActivity).hideBottomNavigationBar()

            if (isVisibleOnShowKeyboard == true) {
                isVisibleOnShowKeyboard = false
                EventBus.isBottomTabsVisible = true
                EventBus.post(GlobalEvent.IsBottomNaviViewHidden)
            }
        }
    }

    fun getWebView(): WebView = currentWebView

    fun canGoBack(): Boolean {
        return currentWebView.canGoBack() && subWebViewList.isEmpty()
    }

    fun goBack() {
        currentWebView.goBack()
    }

    fun reloadWebView() {
        reloadUrl?.let { mBinding.webView.loadUrl(it) }
    }

    fun loadUrl(targetUrl: String) {
        if (targetUrl.isEmpty()) {
            reloadWebView()
        } else {
            mBinding.webView.loadUrl(targetUrl)
        }
    }

    private inner class CustomWebViewClient : WebViewClient() {

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)

            mBinding.progressBar.visibility = View.VISIBLE
            mBinding.progressBar.show()
            if (subProgressBarList.isNotEmpty()) {
                subProgressBarList.last().visibility = View.VISIBLE
                subProgressBarList.last().show()
            }
        }
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)

            mBinding.progressBar.hide()
            mBinding.progressBar.visibility = View.INVISIBLE
            if (subProgressBarList.isNotEmpty()) {
                subProgressBarList.last().hide()
                subProgressBarList.last().visibility = View.INVISIBLE
            }
            CookieManager.getInstance().flush()
        }

        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            val requestUrlStr = request?.url.toString()
            val scheme = request?.url?.scheme
            Log.d(TAG, "RequestUrl_Str($tabTag): $requestUrlStr")
            Log.d(TAG, "RequestUrl_Scheme($tabTag): $scheme")

            when (scheme) {
                "http", "https", "HTTP", "HTTPS" -> {  // 일반적인 웹 주소(Url) 처리
                    view?.loadUrl(requestUrlStr)
                    return true  // 앱이 직접 Url을 처리한다.
                }
                "tel" -> {  // tel 링크 클릭 시, 내장된 기본 전화 앱 실행
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse(requestUrlStr))
                    startActivity(intent)
                    return true
                }
                "sms", "mailto" -> {  // sms/mailto 링크 클릭 시, 내장된 기본 문자/메일 앱 실행
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse(requestUrlStr))
                    startActivity(intent)
                    return true
                }
                "intent" -> {  // intent 링크 클릭 시, 특정 앱 실행 (예: 결제 서비스 앱)
                    Intent.parseUri(requestUrlStr, Intent.URI_INTENT_SCHEME)?.let { intent ->
                        runCatching {
                            startActivity(intent)  // 해당 앱으로 이동
                            return true
                        }.recoverCatching {
                            // 해당 앱 미설치 시, Google Play 스토어로 이동
                            val packageName = intent.getPackage()
                            if (!packageName.isNullOrBlank()) {
                                val marketUriStr = "market://details?id=$packageName"
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(marketUriStr)))
                                return true
                            }
                            Log.w(TAG, "${it.localizedMessage}")
                            return false
                        }
                    }
                }
                else -> {
                    Intent.parseUri(requestUrlStr, Intent.URI_INTENT_SCHEME)?.let { intent ->
                        runCatching {
                            startActivity(intent)
                            return true
                        }.recoverCatching {
                            Log.w(TAG, "${it.localizedMessage}")
                            return false
                        }
                    }
                }
            }
//            return super.shouldOverrideUrlLoading(view, request)
            return false
        }

        @SuppressLint("WebViewClientOnReceivedSslError")
        override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
//            super.onReceivedSslError(view, handler, error)
            handler?.proceed()  // SSL 인증서를 무시한다.
        }
        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
            super.onReceivedError(view, request, error)
            Log.e(TAG, "Received_Error: ${error?.description}")
        }
    }

    private inner class CustomWebChromeClient : WebChromeClient() {

        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)

            mBinding.progressBar.setProgressCompat(newProgress, true)
            if (subProgressBarList.isNotEmpty()) {
                subProgressBarList.last().setProgressCompat(newProgress, true)
            }
        }

        /**
         * WebView(JavaScript)에서 예시와 같이 새창으로 팝업을 표시한다.
         *
         * let popupUrl = "https://www.bing.com";
         * window.open(popupUrl, '_blank');
         */
        override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
//            return super.onCreateWindow(view, isDialog, isUserGesture, resultMsg)
            return if (view != null && resultMsg != null) {
                createCustomPopup(view, resultMsg)
                true
            } else { false }
        }
        /**
         * WebView(JavaScript)에서 예시와 같이 새 팝업창을 닫아준다.
         * (앞서 생성한 SubWebView 팝업창을 닫아야 함)
         *
         * window.close();
         */
        override fun onCloseWindow(window: WebView?) {
            window?.visibility = View.GONE
            window?.destroy()
            super.onCloseWindow(window)

            for (subWebView in subWebViewList) {
                if (subWebView.getWebView() == window) {
                    removeSubWebView(subWebView)
                    return
                }
            }
        }

        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
            if (consoleMessage != null) {
                Log.w(TAG, consoleMessage.message()
                        + " from line " + consoleMessage.lineNumber() + " of " + consoleMessage.sourceId())
            }
//            return super.onConsoleMessage(consoleMessage)
            return true
        }
    }

    private fun createCustomPopup(webView: WebView, message: Message) {
        CoroutineScope(Dispatchers.Main).launch {
            val subWebView = SubWebViewService().makeSubWebView(webView)
            val popupUrl = SubWebViewService().getPopupUrl(subWebView, message)
            Log.d(TAG, "CustomPopup_URL: $popupUrl")

            popupUrl?.let {
                if (popupUrl.startsWith(BottomTabs.URL_1, true)
                    || popupUrl.startsWith(BottomTabs.URL_2, true)
                    || popupUrl.startsWith(BottomTabs.URL_3, true)
                    || popupUrl.startsWith(BottomTabs.URL_4, true)) {

                    // WebViewAppDemo의 주소일 경우, 부모 웹뷰에 로드
                    webView.loadUrl(popupUrl.toString())

                } else if (popupUrl.contains("instagram.com", true)
                    || popupUrl.contains("facebook.com", true)
                    || popupUrl.contains("twitter.com", true)) {

                    // SNS 관련 주소의 경우, 내장된 기본 브라우저에 로드
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(popupUrl))
                    startActivity(intent)

                } else {
                    // 그 밖의 주소인 경우, 새 팝업창으로 로드
                    addSubWebView(subWebView)
                    subWebView.loadUrl(popupUrl.toString())
                }
            }
        }
    }

    private fun addSubWebView(subWebView: WebView) {
        // 스크롤 방향을 아래로 하여 BottomNavigationView를 숨김
        (requireActivity() as MainActivity).scrollBottomNavigationView(false)

        val popupView = CustomPopupView(requireActivity(), requireContext())
        popupView.addKeyboardListener(this, tabTag)
        popupView.addWebView(subWebView)
        popupView.popupListener = object : OnPopupChangedListener {
            override fun onClosePopup(sender: CustomPopupView) {
                removeSubWebView(sender)
            }
        }
        mBinding.webViewLayout.addView(popupView)
        subWebViewList.add(popupView)
        subProgressBarList.add(popupView.popupProgressBar!!)
        Log.d(TAG, "SubWebViewList_Count = ${subWebViewList.size}")
    }

    private fun removeSubWebView(popupView: CustomPopupView) {
        popupView.getWebView()?.visibility = View.GONE
        popupView.getWebView()?.destroy()

        mBinding.webViewLayout.removeView(popupView)
        subWebViewList.remove(popupView)
        subProgressBarList.remove(popupView.popupProgressBar!!)
        Log.d(TAG, "SubWebViewList_Count = ${subWebViewList.size}")
    }
}