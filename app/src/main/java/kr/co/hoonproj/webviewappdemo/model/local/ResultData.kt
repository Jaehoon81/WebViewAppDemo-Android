package kr.co.hoonproj.webviewappdemo.model.local

/**
 * Result Default
 * Native -> WebView(JavaScript)로 전달하는 기본 형태의 데이터 모델
 * @property uuid 사용자 식별값
 * @property action 웹으로부터 호출된 동작종류
 * @property result 결과 데이터
 * @property isError 에러 여부
 */
data class ResultDefault(
    val uuid: String?, val action: String, val result: String, val isError: Boolean
)

/**
 * Result Photo
 * Native -> WebView(JavaScript)로 전달하는 사진 이미지 데이터 모델
 * @property uuid 사용자 식별값
 * @property action 웹으로부터 호출된 동작종류
 * @property result 사진 이미지 데이터
 * @property isError 에러 여부
 */
data class ResultPhoto(
    val uuid: String?, val action: String, val result: ArrayList<PhotoData>, val isError: Boolean
) {
    data class PhotoData(
        var name: String,
        var base64Image: String
    )
}
