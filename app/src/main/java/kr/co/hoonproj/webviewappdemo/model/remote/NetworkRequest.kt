package kr.co.hoonproj.webviewappdemo.model.remote

import com.google.gson.annotations.SerializedName

data class RequestEmployees(
    @SerializedName("user_key") var userKey: String
)
