package kr.co.hoonproj.webviewappdemo.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.Resources
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kr.co.hoonproj.webviewappdemo.WebViewAppDemo
import kr.co.hoonproj.webviewappdemo.model.MainRepository
import kr.co.hoonproj.webviewappdemo.model.remote.NetworkResponse
import kr.co.hoonproj.webviewappdemo.model.remote.ResponseEmployees
import kr.co.hoonproj.webviewappdemo.utils.USER_KEY
import kr.co.hoonproj.webviewappdemo.view.listeners.NativeViewListener
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.UUID
import javax.inject.Inject

private const val TAG: String = "[WebViewAppDemo] MainViewModel"

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val repository: MainRepository,
//    var nativeViewListener: NativeViewListener? = null
) : AndroidViewModel(application) {

    private val context: Context by lazy { application.applicationContext }
    private val resources: Resources by lazy { application.applicationContext.resources }

    var nativeViewListener: NativeViewListener? = null

    val bottomTabIndex: MutableLiveData<Int> = MutableLiveData()
    var reloadActionTabTag: String = ""

    private val _appVersion: MutableLiveData<String> = MutableLiveData()
    private val _employees: MutableLiveData<ResponseEmployees> = MutableLiveData()

    val appVersion: LiveData<String>
        get() = _appVersion
    val employees: LiveData<ResponseEmployees>
        get() = _employees

    // ==============================================================================================================
    // region MutableLiveData 및 기타 초기화
    init {
        _appVersion.value = "AppVersion ${getApplicationVersion()}"
    }
    // endregion

    // ==============================================================================================================
    // region Employees 데이터 요청 서버통신
    internal fun requestEmployeesToAPI(showAlertDialog: Boolean) = viewModelScope.launch {
        if (!hasInternetConnection()) {
            Log.w(TAG, "EmployeesCall_ResultMessage: ${NetworkResponse.NO_INTERNET_CONNECTION.resultMessage}")
            nativeViewListener?.onCompleteEmployeesCall(NetworkResponse.NO_INTERNET_CONNECTION, showAlertDialog)
            return@launch
        }
        try {
            val request = repository.receiveEmployees(getUserKey()?: "")
            request.enqueue(object : Callback<ResponseEmployees> {
                override fun onResponse(call: Call<ResponseEmployees>, response: Response<ResponseEmployees>) {
                    Log.d(TAG, "EmployeesCall_ResponseCode = ${response.code()}")
                    if (response.isSuccessful) {
                        val status = response.body()?.status!!
                        Log.d(TAG, "EmployeesCall_ResponseStatus: ${status.toUpperCase()}")

                        if (status.equals("success", true)) {
                            _employees.postValue(response.body()!!)
                            nativeViewListener?.onCompleteEmployeesCall(NetworkResponse.SUCCESS, showAlertDialog)
                        } else {
                            nativeViewListener?.onCompleteEmployeesCall(NetworkResponse.UNKNOWN, showAlertDialog)
                        }
                    } else {
                        Log.e(TAG, "EmployeesCall_ResultMessage: ${NetworkResponse.FAILURE.resultMessage}")
                        nativeViewListener?.onCompleteEmployeesCall(NetworkResponse.FAILURE, showAlertDialog)
                    }
                }
                override fun onFailure(call: Call<ResponseEmployees>, t: Throwable) {
                    Log.e(TAG, "EmployeesCall_RequestError: ${t.localizedMessage}")
                    nativeViewListener?.onCompleteEmployeesCall(NetworkResponse.FAILURE, showAlertDialog)
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, e.stackTraceToString())
        }
    }
    // endregion

    // ==============================================================================================================
    // region 기타 Helper 함수
    private fun hasInternetConnection(): Boolean {
        val connectivityManager = getApplication<WebViewAppDemo>()
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        var isInternetConnected = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val activeNetwork = connectivityManager.activeNetwork/*?: return false*/
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)/*?: return false*/
            if (capabilities != null) {
                isInternetConnected = when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                    || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                    else -> false
                }
            }
        } else {
            connectivityManager.activeNetworkInfo?.run {
                isInternetConnected = when (type) {
                    ConnectivityManager.TYPE_WIFI,
                    ConnectivityManager.TYPE_MOBILE,
                    ConnectivityManager.TYPE_ETHERNET -> true
                    else -> false
                }
            }
        }
        if (isInternetConnected == false) {
            Log.w(TAG, "No_Internet_Connection")
            Toast.makeText(context, "No_Internet_Connection", Toast.LENGTH_LONG).show()
        }
        return isInternetConnected
    }

    private fun getApplicationVersion(): String {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)

        val versionName = packageInfo.versionName
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            packageInfo.versionCode.toLong()
        }
        Log.d(TAG, "Current_AppVersion = $versionName($versionCode)")
        return "$versionName($versionCode)"
    }

    internal fun getDisplayResolution(activity: Activity): Pair<Int, Int> {
        val metrics = DisplayMetrics()
        activity.windowManager.defaultDisplay.getRealMetrics(metrics)
        val displayWidth = metrics.widthPixels
        val displayHeight = metrics.heightPixels

        Log.d(TAG, "Display_Width = $displayWidth, Height = $displayHeight")
        return Pair(displayWidth, displayHeight)
    }

    internal fun getUserKey(): String? {
        USER_KEY = WebViewAppDemo.prefs.userKey
        if (USER_KEY.isNullOrEmpty()) {
            USER_KEY = UUID.randomUUID().toString()

            WebViewAppDemo.prefs.userKey = USER_KEY
            Log.d(TAG, "Device_UUID(Random) = $USER_KEY")
        }
        return USER_KEY
    }
    // endregion
}