package kr.co.hoonproj.webviewappdemo.services

import kr.co.hoonproj.webviewappdemo.model.remote.RequestEmployees
import kr.co.hoonproj.webviewappdemo.model.remote.ResponseEmployees
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface RetrofitService {

//    @GET("/api/v1/employees")
//    fun receiveEmployees(
////        @Query("user_key") userKey: String
//    ): Call<ResponseEmployees>

    @POST("/api/v1/employees")
    fun receiveEmployees(
//        @Body employees: RequestEmployees
    ): Call<ResponseEmployees>
}