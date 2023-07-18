package kr.co.hoonproj.webviewappdemo.model.local

enum class ErrorCode(val description: String) {

    InvalidAction("잘못된 명령입니다."),
    InvalidParameter("유효하지 않은 Parameter 값입니다."),
    DeniedPermission("권한이 거부되었습니다."),
    JsonDataFailure("Json 데이터 생성에 실패하였습니다."),
    ImageDataFailure("Image 데이터 생성에 실패하였습니다."),
    UnknownError("알수 없는 오류입니다."),
    EmptyParameter("Parameter 값이 없습니다."),
    EmptyData("데이터가 없습니다."),
    CancelAction("명령이 취소되었습니다.")
}

/**
 * Result Error
 * Native -> WebView(JavaScript)로 전달하는 에러 형태의 데이터 모델
 * @property uuid 사용자 식별값
 * @property action 웹으로부터 호출된 동작종류
 * @property result 에러 메시지
 * @property isError 에러 여부 (기본값 true)
 */
data class ResultError(
    val uuid: String?, val action: String, val result: String, val isError: Boolean = true
)
