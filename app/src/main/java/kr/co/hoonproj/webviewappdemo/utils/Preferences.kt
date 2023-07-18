package kr.co.hoonproj.webviewappdemo.utils

import android.content.Context

class Preferences(context: Context) {

    private val authInfoPrefs = context.getSharedPreferences("authInfo", Context.MODE_PRIVATE)
    private val appDataPrefs = context.getSharedPreferences("appData", Context.MODE_PRIVATE)

    var accessToken: String?
        get() = authInfoPrefs.getString("accessToken", "")
        set(value) = authInfoPrefs.edit().putString("accessToken", value).apply()

    var userKey: String?
        get() = authInfoPrefs.getString("userKey", "")
        set(value) = authInfoPrefs.edit().putString("userKey", value).apply()

    var bottomTabIndex: Int
        get() = appDataPrefs.getInt("bottomTabIndex", 0)
        set(value) = appDataPrefs.edit().putInt("bottomTabIndex", value).apply()
}