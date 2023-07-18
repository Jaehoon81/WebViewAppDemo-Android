package kr.co.hoonproj.webviewappdemo.model.local

enum class ActionCode(val value: String) {

    GetDeviceUUID("getDeviceUUID"),
    ShowToastMessage("showToastMessage"),
    ReloadOtherTabs("reloadOtherTabs"),
    GoToAnotherTab("goToAnotherTab"),
    ShowBottomNaviView("showBottomNaviView"),
    HideBottomNaviView("hideBottomNaviView"),
    GetPhotoImages("getPhotoImages")
}

/**
 * Request Action
 * WebView(JavaScript) -> Native로 요청하는 호출동작 데이터 모델
 * @property uuid 사용자 식별값
 * @property action 호출동작 종류
 * @property params 전달받은 데이터
 */
data class RequestAction(
    val uuid: String?, val action: String, val params: List<String>?
)
