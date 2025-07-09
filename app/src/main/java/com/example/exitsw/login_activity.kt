package com.example.exitsw

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.user.UserApiClient

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // findViewById 로 뷰 참조
        val btnKakaoLogin = findViewById<MaterialButton>(R.id.btnKakaoLogin)
        btnKakaoLogin.setOnClickListener {
            if (UserApiClient.instance.isKakaoTalkLoginAvailable(this)) {
                loginWithKakaoTalk()
            } else {
                loginWithKakaoAccount()
            }
        }
    }

    private fun loginWithKakaoTalk() {
        UserApiClient.instance.loginWithKakaoTalk(this) { token, error ->
            handleResult(token, error)
        }
    }

    private fun loginWithKakaoAccount() {
        UserApiClient.instance.loginWithKakaoAccount(this) { token, error ->
            handleResult(token, error)
        }
    }

    private fun handleResult(token: OAuthToken?, error: Throwable?) {
        when {
            error != null -> {
                Toast.makeText(this, "로그인 실패: ${error.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
            token != null -> {
                // 로그인 성공: 토큰.token.accessToken 활용 가능
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }
}
