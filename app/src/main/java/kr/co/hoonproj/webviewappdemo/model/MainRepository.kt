package kr.co.hoonproj.webviewappdemo.model

import android.annotation.SuppressLint
import kr.co.hoonproj.webviewappdemo.model.remote.RequestEmployees
import kr.co.hoonproj.webviewappdemo.model.remote.ResponseEmployees
import kr.co.hoonproj.webviewappdemo.services.RetrofitService
import retrofit2.Call

class MainRepository /*private */constructor(private val service: RetrofitService) {

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile private var instance: MainRepository? = null

        fun getInstance(service: RetrofitService) =
            instance?: synchronized(this) {
                instance?: MainRepository(service).also { instance = it }
            }
    }

    suspend fun receiveEmployees(userKey: String): Call<ResponseEmployees> {
        val employees = RequestEmployees(userKey)
//        return service.receiveEmployees(employees)

        return service.receiveEmployees()
    }
}