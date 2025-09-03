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
    // private const val BASE_URL = "http://20.22.129.6:8080"
    private const val BASE_URL = "http://172.20.10.4:8080"

    private var instance: WelfareApiService? = null

    // 필요할 때 Context를 받아 안전하게 인스턴스를 생성하거나 반환하는 함수
    fun getInstance(context: Context): WelfareApiService {
        if (instance == null) {
            val cacheSize = (10 * 1024 * 1024).toLong() // 10MB 캐시 사이즈
            val cache = Cache(File(context.cacheDir, "http-cache"), cacheSize)

            val okHttpClient = OkHttpClient.Builder()
                .cache(cache)
                .addInterceptor { chain ->
                    var request = chain.request()
                    if (isNetworkAvailable(context)) {
                        request = request.newBuilder().header("Cache-Control", "public, max-age=" + 60).build()
                    } else {
                        request = request.newBuilder().header("Cache-Control", "public, only-if-cached, max-stale=" + 60 * 60 * 24 * 7).build()
                    }
                    chain.proceed(request)
                }
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            instance = retrofit.create(WelfareApiService::class.java)
        }
        return instance!!
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
