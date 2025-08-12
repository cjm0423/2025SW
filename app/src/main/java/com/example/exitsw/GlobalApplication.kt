package com.example.exitsw

import android.app.Application
import com.example.exitsw.network.RetrofitClient
import com.kakao.sdk.common.KakaoSdk

class GlobalApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        KakaoSdk.init(this, getString(R.string.kakao_native_app_key))

        RetrofitClient.initialize(this)
    }
}
