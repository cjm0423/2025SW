package com.example.exitsw.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import okhttp3.Cache
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File

object RetrofitClient {
    // 안드로이드 에뮬레이터에서 PC의 localhost에 접속하기 위한 주소
    private const val BASE_URL = "http://10.0.2.2:8080"

    // Retrofit 인스턴스를 나중에 초기화할 수 있도록 lateinit 사용
    lateinit var instance: WelfareApiService
        private set // 외부에서 수정할 수 없도록 private set

    // 앱이 시작될 때 Application Context를 받아 초기화하는 함수
    fun initialize(context: Context) {
        val cacheSize = (10 * 1024 * 1024).toLong() // 10MB 캐시 사이즈
        val cache = Cache(File(context.cacheDir, "http-cache"), cacheSize)

        val okHttpClient = OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor { chain ->
                var request = chain.request()
                // 네트워크가 연결되어 있으면 1분 동안 캐시된 데이터를 사용
                if (isNetworkAvailable(context)) {
                    request = request.newBuilder().header("Cache-Control", "public, max-age=" + 60).build()
                } else {
                    // 네트워크가 연결되어 있지 않으면 7일 동안 캐시된 데이터를 사용
                    request = request.newBuilder().header("Cache-Control", "public, only-if-cached, max-stale=" + 60 * 60 * 24 * 7).build()
                }
                chain.proceed(request)
            }
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient) // 캐시 설정이 적용된 OkHttpClient를 사용
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        instance = retrofit.create(WelfareApiService::class.java)
    }

    // 네트워크 연결 상태를 확인하는 함수
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            else -> false
        }
    }
}
