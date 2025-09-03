package com.example.exitsw

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.user.UserApiClient

class LoginActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "LoginActivity"
        const val EXTRA_KAKAO_ID = "kakaoId"
        const val EXTRA_NICKNAME = "nickname"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val btnKakaoLogin = findViewById<ImageButton>(R.id.btnKakaoLogin)
        val btnKakaoStart = findViewById<MaterialButton>(R.id.btnKakaoStart)

        val loginListener = {
            if (UserApiClient.instance.isKakaoTalkLoginAvailable(this)) {
                loginWithKakaoTalk()
            } else {
                loginWithKakaoAccount()
            }
        }
        btnKakaoLogin.setOnClickListener { loginListener.invoke() }
        btnKakaoStart.setOnClickListener { loginListener.invoke() }
    }

    /** 카카오톡으로 로그인 */
    private fun loginWithKakaoTalk() {
        UserApiClient.instance.loginWithKakaoTalk(this) { token: OAuthToken?, error: Throwable? ->
            if (error != null) {
                Log.e(TAG, "카카오톡 로그인 실패", error)
                Toast.makeText(this, "카카오톡 로그인 실패: ${error.localizedMessage}", Toast.LENGTH_SHORT).show()
                loginWithKakaoAccount()
            } else if (token != null) {
                fetchUserAndNavigate()
            }
        }
    }

    /** 카카오계정으로 로그인 */
    private fun loginWithKakaoAccount() {
        UserApiClient.instance.loginWithKakaoAccount(this) { token: OAuthToken?, error: Throwable? ->
            if (error != null) {
                Log.e(TAG, "카카오 계정 로그인 실패", error)
                Toast.makeText(this, "카카오 계정 로그인 실패: ${error.localizedMessage}", Toast.LENGTH_SHORT).show()
            } else if (token != null) {
                fetchUserAndNavigate()
            }
        }
    }

    /** 사용자 정보 받아서 SignupActivity로 이동 (kakaoId + nickname 전달) */
    private fun fetchUserAndNavigate() {
        UserApiClient.instance.me { user, error ->
            if (error != null || user == null) {
                Log.e(TAG, "사용자 정보 요청 실패", error)
                Toast.makeText(this, "사용자 정보 요청 실패", Toast.LENGTH_SHORT).show()
                return@me
            }

            val kakaoId = user.id?.toString()
            if (kakaoId.isNullOrBlank()) {
                Toast.makeText(this, "카카오 ID를 가져오지 못했습니다. 다시 로그인해주세요.", Toast.LENGTH_SHORT).show()
                return@me
            }

            val nickname = user.kakaoAccount?.profile?.nickname.orEmpty()
            Log.d(TAG, "kakaoId=$kakaoId, nickname=$nickname")

            val intent = Intent(this, SignupActivity::class.java).apply {
                putExtra(EXTRA_KAKAO_ID, kakaoId)
                putExtra(EXTRA_NICKNAME, nickname)
            }
            startActivity(intent)
            finish()
        }
    }
}
