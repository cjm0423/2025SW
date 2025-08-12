package com.example.exitsw

import android.app.Application
import com.kakao.sdk.common.KakaoSdk

class GlobalApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        KakaoSdk.init(this, "d064ad34cc301b513869a65a6177ead1")
    }
}