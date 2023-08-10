package kr.co.hoonproj.webviewappdemo.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.webkit.CookieManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.normal.TedPermission
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kr.co.hoonproj.webviewappdemo.WebViewAppDemo
import kr.co.hoonproj.webviewappdemo.databinding.ActivityMainBinding
import kr.co.hoonproj.webviewappdemo.model.MainRepository
import kr.co.hoonproj.webviewappdemo.services.RetrofitInstance
import kr.co.hoonproj.webviewappdemo.services.RetrofitService
import kr.co.hoonproj.webviewappdemo.utils.ACTION_SHOW_NOTI_MESSAGE
import kr.co.hoonproj.webviewappdemo.utils.BottomTabs
import kr.co.hoonproj.webviewappdemo.utils.EventBus
import kr.co.hoonproj.webviewappdemo.utils.GlobalEvent
import kr.co.hoonproj.webviewappdemo.utils.finishApplication
import kr.co.hoonproj.webviewappdemo.view.fragments.NativeViewFragment
import kr.co.hoonproj.webviewappdemo.view.fragments.WebViewFragment
import kr.co.hoonproj.webviewappdemo.viewmodel.MainViewModel
import kr.co.hoonproj.webviewappdemo.viewmodel.MainViewModelFactory
import java.net.URLDecoder

private const val TAG: String = "[WebViewAppDemo] MainActivity"

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private var mBinding: ActivityMainBinding? = null

//    lateinit var mainViewModel: MainViewModel
    val mainViewModel: MainViewModel by viewModels()

    private val BOTTOM_TAB_COUNT: Int = 4
    private var currentTabTag: String = ""
    private var isBottomNaviViewAnimating: Boolean = false

    private var backPressedTime: Long = 0L
    private val backPressedToast: Toast by lazy {
        Toast.makeText(this, "한번 더 누르면 앱이 종료됩니다.", Toast.LENGTH_LONG)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "MainActivity:: onCreate($savedInstanceState)")

        if (savedInstanceState != null) {
            currentTabTag = savedInstanceState.getString("currentTabTag", "")
            Log.d(TAG, "Saved_CurrentTabTag = $currentTabTag")
        }
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding?.root!!)

        // DIAppModule 클래스의 @Provides 메서드로 대체 (Dependency Injection)
//        val service = RetrofitInstance.getInstance().create(RetrofitService::class.java)
//        val repository = MainRepository.getInstance(service)
//        val factory = MainViewModelFactory(application, repository)
//        mainViewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        configureAppSettings()
        setupWebViewPager()
        setupBottomNavigationView(initialTabIndex = WebViewAppDemo.prefs.bottomTabIndex)
        setupEventBus()

        // onCreate()가 중복 호출되면 한번만 권한을 요청한다.
        // (예: Samsung Galaxy 시리즈의 경우, 처음 앱을 설치하면 onCreate()가 두번 호출됨)
        if (savedInstanceState == null) {
            requestPermissions(intent)
        }
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        Log.i(TAG, "MainActivity:: onSaveInstanceState()")

        // onCreate()가 다시 호출됐을 때, 유지하고 싶은 전역변수 값을 저장한다.
        outState.putString("currentTabTag", currentTabTag)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.i(TAG, "MainActivity:: onConfigurationChanged()")

        when (newConfig.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> {
                // 세로모드로 전환 시, 필요한 코드 적용
            }
            Configuration.ORIENTATION_LANDSCAPE -> {
                // 가로모드로 전환 시, 필요한 코드 적용
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.i(TAG, "MainActivity:: onNewIntent($intent)")

        setIntent(intent)
        checkDeepLink(intent)
        checkNotification(intent)
    }

    private fun checkDeepLink(intent: Intent?) {
        val deepLinkData: Uri? = intent?.data
        if (deepLinkData != null) {
            deepLinkData.getQueryParameter("target")?.toInt()?.let { targetTabIndex ->
                deepLinkData.getQueryParameter("url")?.let { encodedUrl ->
                    // DeepLink 데이터에서 파라미터 정보를 추출한 후, 다른 탭으로 이동하면서 특정 Url을 로드한다.
                    try {
                        val targetTabTag = "f$targetTabIndex"
                        val targetUrl = "https://" + URLDecoder.decode(encodedUrl, "UTF-8")
                        supportFragmentManager.setFragmentResult(
                            targetTabTag, bundleOf("targetUrl" to targetUrl))
                    } catch (e: Exception) {
                        Log.e(TAG, e.stackTraceToString())
                    }
                }
                mainViewModel.bottomTabIndex.postValue(targetTabIndex)
            }
        } else {
            Log.d(TAG, "DeepLink data is a null value.")
        }
    }

    private fun checkNotification(intent: Intent?) {
        when (intent?.action) {
            ACTION_SHOW_NOTI_MESSAGE -> {
                val notificationData: Bundle? = intent?.extras
                if (notificationData != null) {
                    // 수신된 노티 메시지 데이터를 활용하기 위한 코드작성
                    var notificationDataStr = ""
                    for (key in notificationData.keySet()) {
                        notificationDataStr += "[Key: ${key}, Value: ${notificationData.getString(key)}]"
                    }
                    Log.d(TAG, "Notification_Data = $notificationDataStr")
                } else {
                    Log.d(TAG, "Notification data is a null value.")
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Log.i(TAG, "MainActivity:: onStart()")
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "MainActivity:: onResume()")

        Handler(Looper.getMainLooper()).postDelayed({
            hideBottomNavigationBar()
        }, 500L)
        hideBottomNavigationBar()
    }

    override fun onPause() {
        super.onPause()
        Log.i(TAG, "MainActivity:: onPause()")
    }

    override fun onStop() {
        super.onStop()
        Log.i(TAG, "MainActivity:: onStop()")
    }

    override fun onDestroy() {
        Log.i(TAG, "MainActivity:: onDestroy()")

        mainViewModel.bottomTabIndex.removeObservers(this)
        mBinding = null
        super.onDestroy()
    }

    fun showBottomNavigationBar() {
        val decorView: View = window.decorView
        var uiOptions: Int = decorView.systemUiVisibility
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            uiOptions = View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
        decorView.systemUiVisibility = uiOptions
    }

    fun hideBottomNavigationBar() {
        // 하단 내비게이션 바를 항상 보이게 하려면 아래 코드를 주석 처리한다.
//        val decorView: View = window.decorView
//        var uiOptions: Int = decorView.systemUiVisibility
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
//            uiOptions = uiOptions or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//        }
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
//            uiOptions = uiOptions or View.SYSTEM_UI_FLAG_FULLSCREEN
//        }
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//            uiOptions = uiOptions or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
//        }
//        decorView.systemUiVisibility = uiOptions
    }

    fun isBottomNavigationViewVisible(): Boolean =
        (mBinding?.bottomNavigationView?.visibility == View.VISIBLE)

    fun scrollBottomNavigationView(isUp: Boolean) {
        if (isBottomNaviViewAnimating == false) {
            isBottomNaviViewAnimating = true

            if (isUp == true) {
                mBinding?.bottomNavigationView!!.animate().translationY(0F).withEndAction {
                    isBottomNaviViewAnimating = false
                }.duration = 200L
            } else {
                val height = mBinding?.bottomNavigationView!!.height.toFloat()
                mBinding?.bottomNavigationView!!.animate().translationY(height).withEndAction {
                    isBottomNaviViewAnimating = false
                }.duration = 200L
            }
        }
    }

    private fun configureAppSettings() {
        // 앱이 하드웨어 가속을 하도록 설정
        window.setFlags(
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
        // 앱의 화면을 꺼지지 않게 유지
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // 가상 키보드가 팝업되면 화면 UI(EditText)를 가리지 않도록 설정
//        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
//        window.decorView.systemUiVisibility =
//            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        // 앱을 이전에 종료했을 시 남아있던 세션 제거
        CookieManager.getInstance().removeSessionCookies(null)

        // 하단 내비게이션 바의 뒤로가기 버튼 설정
        onBackPressedDispatcher.addCallback(this, backPressedCallback)
    }

    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            val webViewFragment = supportFragmentManager.findFragmentByTag(currentTabTag) as? WebViewFragment
            if (webViewFragment != null && webViewFragment.canGoBack()) {
                // 하단 내비게이션 바 버튼을 이용한 웹 페이지 뒤로가기 실행
                webViewFragment.goBack()
            } else {
                if (System.currentTimeMillis() > backPressedTime + 2_000L) {
                    backPressedTime = System.currentTimeMillis()
                    backPressedToast.show()
                } else {
                    Log.i(TAG, "MainActivity:: handleOnBackPressed()")
                    backPressedToast.cancel()
                    finishApplication(this@MainActivity)
                }
            }
        }
    }

    private fun setupWebViewPager() {
        val webViewPagerAdapter = WebViewPagerAdapter(this)
        mBinding?.viewPager!!.adapter = webViewPagerAdapter

        // offscreenPageLimit를 설정한 만큼 ViewPager의 Fragments를 메모리에 유지
        // (onDestroy()가 실행되지 않음)
        mBinding?.viewPager!!.offscreenPageLimit = BOTTOM_TAB_COUNT
        // 사용자 터치 or 스와이프 시, ViewPager와의 상호작용 여부
        mBinding?.viewPager!!.isUserInputEnabled = false

        // ViewPager-WebView와 BottomNavigationView를 연동
        mainViewModel.bottomTabIndex.observe(this) { index ->
            if (index < mBinding?.bottomNavigationView!!.menu.size()) {
                mBinding?.bottomNavigationView!!.menu.getItem(index).isChecked = true
                mBinding?.viewPager!!.setCurrentItem(index, false)

                currentTabTag = "f$index"
                EventBus.currentTabTag = currentTabTag
                WebViewAppDemo.prefs.bottomTabIndex = index
            }
        }
    }

    private fun setupBottomNavigationView(initialTabIndex: Int = 0) {
        // 하단 탭을 눌렀을 때, Pressed 이미지를 따로 사용한다면 itemIconTintList = null 필요
//        mBinding?.bottomNavigationView!!.itemIconTintList = null

        // 하단 탭을 눌렀을 경우 실행할 리스너 등록
        mBinding?.bottomNavigationView!!.setOnItemSelectedListener { menuItem ->
            val index = menuItem.order
            Log.d(TAG, "Selected_BottomTabIndex = $index")

            // 현재 탭을 다시 눌렀을 때는 해당 웹뷰의 초기 Url로 리로드한다.
            if ("f$index" == currentTabTag) {
                val currentFragment = supportFragmentManager.findFragmentByTag(currentTabTag)
                if (index == 0 || index == 1 || index == 2) {
                    (currentFragment as? WebViewFragment)?.reloadWebView()
                } else {  // 네이티브 화면(f3)일 경우
                    (currentFragment as? NativeViewFragment)?.refreshNativeView()
                }
            }
            mainViewModel.bottomTabIndex.postValue(index)
            true
        }
        // 앱을 이전에 종료했을 시 마지막으로 노출(저장)했던 탭으로 시작
        mainViewModel.bottomTabIndex.postValue(initialTabIndex)
    }

    private fun setupEventBus() {
        CoroutineScope(Dispatchers.Main).launch {
            EventBus.subscribe(GlobalEvent.OtherTabsDidReload) {

                val bottomTabTagList = arrayOf("f0", "f1", "f2", "f3")
                for ((index, bottomTabTag) in bottomTabTagList.withIndex()) {
                    // 현재 탭을 제외하고, 나머지 웹뷰를 초기 Url로 리로드한다.
                    if (bottomTabTag == EventBus.currentTabTag) { continue }
                    else {
                        val targetFragment = supportFragmentManager.findFragmentByTag(bottomTabTag)
                        if (index == 0 || index == 1 || index == 2) {
                            (targetFragment as? WebViewFragment)?.reloadWebView()
                        } else {  // 네이티브 화면(f3)일 경우
                            (targetFragment as? NativeViewFragment)?.refreshNativeView()
                        }
                    }
                }
            }
        }
        CoroutineScope(Dispatchers.Main).launch {
            EventBus.subscribe(GlobalEvent.AnotherTabDidMove) {

                if (EventBus.tabTagToMove.isNotEmpty()) {
                    val index = EventBus.tabTagToMove.substring(1, 2).toInt()
                    if (EventBus.targetUrl.isNotEmpty()) {
                        // 다른 탭으로 이동하고, 웹뷰의 경우 특정 Url을 로드한다.
                        val targetFragment = supportFragmentManager.findFragmentByTag(EventBus.tabTagToMove)
                        if (index == 0 || index == 1 || index == 2) {
                            (targetFragment as? WebViewFragment)?.loadUrl(EventBus.targetUrl)
                        } else {  // 네이티브 화면(f3)일 경우
                            (targetFragment as? NativeViewFragment)?.refreshNativeView()
                        }
                    }
                    mainViewModel.bottomTabIndex.postValue(index)
                }
            }
        }
        CoroutineScope(Dispatchers.Main).launch {
            EventBus.subscribe(GlobalEvent.IsBottomNaviViewHidden) {

                mBinding?.bottomNavigationView?.visibility =
                    if (EventBus.isBottomTabsVisible == true) { View.VISIBLE }
                    else { View.INVISIBLE }
            }
        }
    }

    @SuppressLint("SuspiciousIndentation")
    private fun requestPermissions(intent: Intent?) {
        val permissionBuilder = TedPermission.create()
            .setPermissionListener(object : PermissionListener {
                override fun onPermissionGranted() {
                    Log.i(TAG, "MainActivity:: onPermissionGranted()")
                    checkDeepLink(intent)
                    checkNotification(intent)
                }
                override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
                    Log.w(TAG, "MainActivity:: onPermissionDenied()")
                    // 요청한 권한 거부 시, 앱 종료
                    finish()
                }
            })
            .setRationaleMessage("앱을 이용하기 위해서는 '알림' 권한이 필요합니다.")
            .setDeniedMessage("요청한 권한을 거부할 경우, 앱을 이용할 수 없습니다.\n설정의 앱 정보에서 해당 권한을 활성화해주세요.")

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionBuilder.setPermissions(
                Manifest.permission.POST_NOTIFICATIONS
            ).check()
//        }
    }

    private inner class WebViewPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {

        override fun getItemCount(): Int = BOTTOM_TAB_COUNT

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> makeWebViewFragment(BottomTabs.URL_1)
                1 -> makeWebViewFragment(BottomTabs.URL_2)
                2 -> makeWebViewFragment(BottomTabs.URL_3)

//                3 -> makeWebViewFragment(BottomTabs.URL_4)
                else -> NativeViewFragment.newInstance()
            }
        }
    }

    private fun makeWebViewFragment(targetUrl: String): WebViewFragment {
        val webViewFragment = WebViewFragment.newInstance()
        webViewFragment.arguments = Bundle().apply {
            putString("targetUrl", targetUrl)
        }
        return webViewFragment
    }
}