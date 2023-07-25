package kr.co.hoonproj.webviewappdemo.services

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import androidx.core.os.bundleOf
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.co.hoonproj.webviewappdemo.model.local.ActionCode
import kr.co.hoonproj.webviewappdemo.model.local.ErrorCode
import kr.co.hoonproj.webviewappdemo.model.local.RequestAction
import kr.co.hoonproj.webviewappdemo.model.local.ResultDefault
import kr.co.hoonproj.webviewappdemo.model.local.ResultError
import kr.co.hoonproj.webviewappdemo.model.local.ResultPhoto
import kr.co.hoonproj.webviewappdemo.utils.ACTION_SHOW_NOTI_MESSAGE
import kr.co.hoonproj.webviewappdemo.utils.BottomTabs
import kr.co.hoonproj.webviewappdemo.utils.EventBus
import kr.co.hoonproj.webviewappdemo.utils.GlobalEvent
import kr.co.hoonproj.webviewappdemo.utils.REQUEST_CODE_SHOW_NOTI_MESSAGE
import kr.co.hoonproj.webviewappdemo.utils.showNotification
import kr.co.hoonproj.webviewappdemo.view.MainActivity
import kr.co.hoonproj.webviewappdemo.viewmodel.MainViewModel

private const val TAG: String = "[WebViewAppDemo] WebViewService"

class WebViewService(
    private val context: Context,
    private val webView: WebView,
    private val mainViewModel: MainViewModel
) {

    /**
     * WebView(JavaScript) -> Native의 함수 호출
     */
    @JavascriptInterface
    fun callNative(message: String) {
        val request: RequestAction? = toDataObject(message)
        if (request == null) {
            processErrorAction("unknown", message)
            return
        }
        Log.d(TAG, "Request_Action: ${request.action}")

        when (request.action) {
            ActionCode.GetDeviceUUID.value      -> processGetDeviceUUID(request.uuid, request.action)
            ActionCode.ShowToastMessage.value   -> processShowToastMessage(request.uuid, request.action, request.params)
            ActionCode.ShowNotiMessage.value    -> processShowNotiMessage(request.uuid, request.action, request.params)
            ActionCode.ReloadOtherTabs.value    -> processReloadOtherTabs(request.uuid, request.action)
            ActionCode.GoToAnotherTab.value     -> processGoToAnotherTab(request.uuid, request.action, request.params)
            ActionCode.ShowBottomNaviView.value -> processBottomNaviView(request.uuid, request.action, true)
            ActionCode.HideBottomNaviView.value -> processBottomNaviView(request.uuid, request.action, false)
            ActionCode.GetPhotoImages.value     -> processGetPhotoImages(request.uuid, request.action)
            else -> processInvalidAction(request.uuid, request.action)
        }
    }

    private inline fun <reified T> toDataObject(json: String): T? {
        return try {
            Gson().fromJson(json, T::class.java)
        } catch (e: Exception) {
            Log.e(TAG, e.stackTraceToString())
            null
        }
    }

    // ==============================================================================================================
    // 단말기 고유 ID(=UUID)를 웹뷰로 전달한다.
    private fun processGetDeviceUUID(uuid: String?, action: String) {
        val userKey = mainViewModel.getUserKey()
        if (userKey.isNullOrEmpty()) {
            val resultJsonStr = toErrorJson(uuid, action, ErrorCode.EmptyData)
            callJavaScript(resultJsonStr)
            return
        }
        val resultData = ResultDefault(uuid, action, userKey!!, false)
        val resultJsonStr = toResultJson(resultData)?: ErrorCode.JsonDataFailure.description
        callJavaScript(resultJsonStr)
    }

    // 토스트 메시지를 화면에 노출한다.
    private fun processShowToastMessage(uuid: String?, action: String, params: List<String>?) {
        if (params.isNullOrEmpty()) {
            val resultJsonStr = toErrorJson(uuid, action, ErrorCode.InvalidParameter)
            callJavaScript(resultJsonStr)
        } else {
            Toast.makeText(context, params[0], Toast.LENGTH_LONG).show()

            val resultData = ResultDefault(uuid, action, params[0], false)
            val resultJsonStr = toResultJson(resultData)?: ErrorCode.JsonDataFailure.description
            callJavaScript(resultJsonStr)
        }
    }

    // 노티 메시지를 화면에 노출한다.
    private fun processShowNotiMessage(uuid: String?, action: String, params: List<String>?) {
        if (params.isNullOrEmpty()) {
            val resultJsonStr = toErrorJson(uuid, action, ErrorCode.InvalidParameter)
            callJavaScript(resultJsonStr)
        } else {
            val intent = Intent(context, MainActivity::class.java)
            intent.action = ACTION_SHOW_NOTI_MESSAGE
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val extras = if (params.count() == 1) { bundleOf("title" to params[0]) }
            else { bundleOf("title" to params[0], "message" to params[1]) }
            intent.putExtras(extras)

            val message = if (params.count() == 1) { null } else { params[1] }
            val flags = PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            val pendingIntent = PendingIntent.getActivity(context, REQUEST_CODE_SHOW_NOTI_MESSAGE, intent, flags)
            showNotification(context, params[0], message, pendingIntent)

            val resultData = ResultDefault(uuid, action, params[0], false)
            val resultJsonStr = toResultJson(resultData)?: ErrorCode.JsonDataFailure.description
            callJavaScript(resultJsonStr)
        }
    }

    // 현재 탭의 웹뷰를 제외하고, 나머지 웹뷰를 리로드한다.
    private fun processReloadOtherTabs(uuid: String?, action: String) {
        mainViewModel.reloadActionTabTag = EventBus.currentTabTag
        EventBus.post(GlobalEvent.OtherTabsDidReload)

        val resultData = ResultDefault(uuid, action, "", false)
        val resultJsonStr = toResultJson(resultData)?: ErrorCode.JsonDataFailure.description
        callJavaScript(resultJsonStr)
    }

    // 다른 탭으로 이동하면서 특정 Url을 로드한다.
    private fun processGoToAnotherTab(uuid: String?, action: String, params: List<String>?) {
        if (params.isNullOrEmpty()) {
            val resultJsonStr = toErrorJson(uuid, action, ErrorCode.InvalidParameter)
            callJavaScript(resultJsonStr)
        } else {
            EventBus.tabTagToMove = params[0]
            EventBus.targetUrl = /*BottomTabs.URL_1 + */params[1]
            EventBus.post(GlobalEvent.AnotherTabDidMove)

            val resultData = ResultDefault(uuid, action, "", false)
            val resultJsonStr = toResultJson(resultData)?: ErrorCode.JsonDataFailure.description
            callJavaScript(resultJsonStr)
        }
    }

    // 하단 탭 영역을 보여주거나 숨긴다.
    private fun processBottomNaviView(uuid: String?, action: String, isVisible: Boolean) {
        EventBus.isBottomTabsVisible = isVisible
        EventBus.post(GlobalEvent.IsBottomNaviViewHidden)

        val resultData = ResultDefault(uuid, action, "", false)
        val resultJsonStr = toResultJson(resultData)?: ErrorCode.JsonDataFailure.description
        callJavaScript(resultJsonStr)
    }

    // 사진 앨범에서 이미지를 선택하여 웹뷰로 전달한다.
    private fun processGetPhotoImages(uuid: String?, action: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Step 0. 사진권한 요청
                // (사용자가 거부하면 Throwable 발생 -> catch 블록 Exception)
                val isAuthorized = PhotoImageService(context).requestAuthorization()
                if (isAuthorized == true) {
                    withContext(Dispatchers.IO) {
                        // Step 1. 사진 앨범을 열어서 이미지 선택
                        // (선택된 사진이 없으면 Throwable 발생)
                        val uriList = PhotoImageService(context).openPhotoPicker(2)
                        if (!uriList.isNullOrEmpty()) {
                            val photoData = ArrayList<ResultPhoto.PhotoData>()
                            for ((index, uri) in uriList.withIndex()) {
                                // Step 2. uri로부터 이미지 획득
                                // 이미지 크기는 width or height 중에 큰 것이 Maximum 1_000이 되고,
                                // 나머지 작은 것은 비율에 따라 값이 정해진다.
                                // (획득한 사진이 없거나, 알수 없는 오류일 경우 Throwable 발생)
                                val image = PhotoImageService(context).getPhotoImage(uri, 1_000, 1_000)
                                // Step 3. 각 image를 Base64 이미지로 변환하고, 웹뷰로 전달할 사진 이미지 데이터 모델을 생성한다.
                                val name = "사진앨범 선택 이미지(${index + 1})"
                                val base64Image = PhotoImageService(context).convertImageToBase64(image)?: ""
                                photoData.add(ResultPhoto.PhotoData(name, base64Image))
                            }
                            val resultData = ResultPhoto(uuid, action, photoData, false)
                            val resultJsonStr = toResultJson(resultData)?: ErrorCode.JsonDataFailure.description
                            callJavaScript(resultJsonStr)
                        } else {
                            val resultJsonStr = toErrorJson(uuid, action, ErrorCode.EmptyData)
                            callJavaScript(resultJsonStr)
                        }
                    }
                } else {
                    val resultJsonStr = toErrorJson(uuid, action, ErrorCode.DeniedPermission)
                    callJavaScript(resultJsonStr)
                }
            } catch (e: Exception) {
                Log.e(TAG, e.stackTraceToString())

                val resultJsonStr = toErrorJson(uuid, action, e.message)
                callJavaScript(resultJsonStr)
            }
        }
    }

    // ==============================================================================================================

    private fun processInvalidAction(uuid: String?, action: String) {
        val resultJsonStr = toErrorJson(uuid, action, ErrorCode.InvalidAction)
        callJavaScript(resultJsonStr)
    }

    private fun processErrorAction(uuid: String?, action: String) {
        val resultJsonStr = toErrorJson(uuid, action, ErrorCode.UnknownError)
        callJavaScript(resultJsonStr)
    }

    // ==============================================================================================================
    /**
     * Native -> WebView(JavaScript)의 함수 호출
     */
    private fun callJavaScript(param: String?) {
        if (param != null) {
            CoroutineScope(Dispatchers.Main).launch {
                javaScript("calledByNative", listOf(param))
            }
        }
    }

    private fun callJavaScript(params: List<String>?) {
        if (!params.isNullOrEmpty()) {
            CoroutineScope(Dispatchers.Main).launch {
                javaScript("calledByNative", params)
            }
        }
    }

    private fun javaScript(callName: String, params: List<String>) {
        var javaScriptStr = "javascript:$callName("
        for (index in 0 until params.count()) {
            javaScriptStr += if (index == (params.count() - 1)) { "'${params[index]}'" }
            else { "'${params[index]}', " }
        }
        javaScriptStr += ");"

        webView.evaluateJavascript(javaScriptStr) { result ->
            if (result == null) {
                Log.w(TAG, "JavaScript function call error: ${ErrorCode.CancelAction.description}")
            }
        }
    }

    // ==============================================================================================================

    private fun toErrorJson(uuid: String?, action: String, errorCode: ErrorCode): String {
        val errorMessage = errorCode.description
        Log.w(TAG, "Process($action)_Error: $errorMessage")

        val resultData = ResultError(uuid, action, errorMessage)
        val resultJsonStr = toResultJson(resultData)?: ErrorCode.JsonDataFailure.description
        return resultJsonStr
    }

    private fun toErrorJson(uuid: String?, action: String, errorMessage: String?): String {
        val errorMessage = errorMessage?: ErrorCode.UnknownError.description
        Log.w(TAG, "Process($action)_Error: $errorMessage")

        val resultData = ResultError(uuid, action, errorMessage)
        val resultJsonStr = toResultJson(resultData)?: ErrorCode.JsonDataFailure.description
        return resultJsonStr
    }

    private fun toResultJson(data: Any): String? {
        var resultJsonStr = Gson().toJson(data)?.toString()
        resultJsonStr = resultJsonStr?.replace("\\n", "\\\\n")
        resultJsonStr = resultJsonStr?.replace("'", "\\'")
        resultJsonStr = resultJsonStr?.replace("\"", "\\\"")
        return resultJsonStr
    }
}