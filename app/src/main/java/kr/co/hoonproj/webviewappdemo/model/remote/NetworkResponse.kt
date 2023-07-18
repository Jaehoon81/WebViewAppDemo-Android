package kr.co.hoonproj.webviewappdemo.model.remote

import com.google.gson.annotations.SerializedName

enum class NetworkResponse(val resultCode: String, val resultMessage: String) {

    NO_INTERNET_CONNECTION("000", "인터넷 접속장애입니다."),
    SUCCESS("200", "서버통신 성공입니다."),
    UNKNOWN("400", "서버통신 오류입니다."),
    FAILURE("500", "서버통신 실패입니다.")
}

data class ResponseEmployees(
    @SerializedName("status") val status: String,
    @SerializedName("data") val data: List<EmployeeData>? = null
) {
    data class EmployeeData(
        @SerializedName("id") val id: Int,
        @SerializedName("employee_name") val employeeName: String,
        @SerializedName("employee_age") val employeeAge: Int,
        @SerializedName("employee_salary") val employeeSalary: Int,
        @SerializedName("profile_image") val profileImage: String
    )
}
