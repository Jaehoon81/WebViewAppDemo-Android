package kr.co.hoonproj.webviewappdemo

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import kr.co.hoonproj.webviewappdemo.utils.Preferences

@HiltAndroidApp
class WebViewAppDemo : Application() {

    companion object {
        lateinit var prefs: Preferences
    }

    override fun onCreate() {
        prefs = Preferences(applicationContext)
        super.onCreate()
    }
}