package kr.co.hoonproj.webviewappdemo.services

import kr.co.hoonproj.webviewappdemo.utils.ACCESS_TOKEN
import kr.co.hoonproj.webviewappdemo.utils.UnsafeOkHttpClient
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class RetrofitInstance private constructor() {

    companion object {
        private const val BASE_URL = "http://dummy.restapiexample.com"

        // 일반적인 HTTP/HTTPS 주소(URL)에 접속 가능한 Client 생성
        private val okHttpClient = OkHttpClient.Builder().addInterceptor { chain ->
            val newRequest = chain.request().newBuilder()
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .build()
            chain.proceed(newRequest)
        }
        .build()

        // 인증서가 없는 HTTPS(SSL) 주소(URL)에 접속 가능한 Client 생성
        private val unsafeOkHttpClient = UnsafeOkHttpClient.getBuilder().addInterceptor { chain ->
            val newRequest = chain.request().newBuilder()
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", ACCESS_TOKEN?: "")
                .build()
            chain.proceed(newRequest)
        }
        .connectTimeout(60L, TimeUnit.SECONDS)
        .readTimeout(60L, TimeUnit.SECONDS)
        .writeTimeout(60L, TimeUnit.SECONDS)
        .build()

        fun getInstance(): Retrofit {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
    }
}