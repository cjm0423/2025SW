package com.example.exitsw

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.util.Utility
import com.kakao.sdk.user.UserApiClient
import android.content.pm.ApplicationInfo

class LoginActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "LoginActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // ImageButton for Kakao Login
        val btnKakaoLogin = findViewById<ImageButton>(R.id.btnKakaoLogin)
        // MaterialButton for "카카오로 시작하기"
        val btnKakaoStart = findViewById<MaterialButton>(R.id.btnKakaoStart)

        val loginListener = {
            // 카카오톡 앱 로그인 가능 여부에 따라 분기
            if (UserApiClient.instance.isKakaoTalkLoginAvailable(this)) {
                loginWithKakaoTalk()
            } else {
                loginWithKakaoAccount()
            }
        }

        btnKakaoLogin.setOnClickListener { loginListener.invoke() }
        btnKakaoStart.setOnClickListener { loginListener.invoke() }
    }

    /**
     * 카카오톡으로 로그인 시도
     */
    private fun loginWithKakaoTalk() {
        UserApiClient.instance.loginWithKakaoTalk(this) { token: OAuthToken?, error: Throwable? ->
            if (error != null) {
                Log.e(TAG, "카카오톡 로그인 실패", error)
                Toast.makeText(this, "카카오톡 로그인 실패: ${error.localizedMessage}", Toast.LENGTH_SHORT).show()
                // 계정 로그인으로 대체
                loginWithKakaoAccount()
            } else if (token != null) {
                Log.i(TAG, "카카오톡 로그인 성공 토큰: ${token.accessToken}")
                fetchUserAndNavigate()
            }
        }
    }

    /**
     * 카카오계정으로 로그인 시도
     */
    private fun loginWithKakaoAccount() {
        UserApiClient.instance.loginWithKakaoAccount(this) { token: OAuthToken?, error: Throwable? ->
            if (error != null) {
                Log.e(TAG, "카카오 계정 로그인 실패", error)
                Toast.makeText(this, "카카오 계정 로그인 실패: ${error.localizedMessage}", Toast.LENGTH_SHORT).show()
            } else if (token != null) {
                Log.i(TAG, "카카오 계정 로그인 성공 토큰: ${token.accessToken}")
                fetchUserAndNavigate()
            }
        }
    }

    /**
     * 로그인 후 메인 화면으로 이동
     */
    private fun fetchUserAndNavigate() {
        UserApiClient.instance.me { user, error ->
            if (error != null || user == null) {
                Log.e(TAG, "사용자 정보 요청 실패", error)
                Toast.makeText(this, "사용자 정보 요청 실패", Toast.LENGTH_SHORT).show()
                return@me
            }
            // 닉네임을 SignupActivity로 전달
            val nickname = user.kakaoAccount?.profile?.nickname.orEmpty()
            val intent = Intent(this, SignupActivity::class.java).apply {
                putExtra("nickname", nickname)
            }
            startActivity(intent)
            finish()
        }
    }
}
