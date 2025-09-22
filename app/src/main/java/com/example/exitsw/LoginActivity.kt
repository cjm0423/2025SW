package com.example.exitsw

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.OAuthCredential
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "LoginActivity"
        const val EXTRA_UID = "uid"
        const val EXTRA_NICKNAME = "nickname"
        private const val OIDC_PROVIDER_ID = "oidc.kakao" // 콘솔 Provider ID = 'kakao'라면 'oidc.kakao'
    }

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val btnKakaoLogin = findViewById<ImageButton>(R.id.btnKakaoLogin)
        // ✅ 세션이 있어도 우회하지 말고 항상 OIDC 시작 (계정 선택창 강제)
        val loginListener = {
            startKakaoOidcSignIn(forcePrompt = true, selectAccount = true)
        }
        btnKakaoLogin.setOnClickListener { loginListener.invoke() }
    }

    /** Firebase Auth - OIDC(Kakao) 로그인 시작 */
    private fun startKakaoOidcSignIn(
        forcePrompt: Boolean = false,
        selectAccount: Boolean = false
    ) {
        val builder = OAuthProvider.newBuilder(OIDC_PROVIDER_ID).apply {
            // ⚠ OIDC 'prompt' 파라미터: select_account가 있으면 계정 선택 UI를 우선 유도
            when {
                selectAccount -> addCustomParameter("prompt", "select_account")
                forcePrompt   -> addCustomParameter("prompt", "login")
            }
            // 필요 시 스코프/클레임
            // scopes = listOf("openid", "profile")
            // addCustomParameter("max_age", "0") // 재인증 유도(지원 여부는 프로바이더에 따라 다름)
        }
        val provider = builder.build()

        val pending = auth.pendingAuthResult
        if (pending != null) {
            pending
                .addOnSuccessListener { handleAuthResult(it) }
                .addOnFailureListener { showAuthError(it) }
        } else {
            auth.startActivityForSignInWithProvider(this, provider)
                .addOnSuccessListener { handleAuthResult(it) }
                .addOnFailureListener { showAuthError(it) }
        }
    }

    private fun handleAuthResult(result: AuthResult) {
        val user = result.user
        if (user == null) {
            Toast.makeText(this, "로그인 실패: 사용자 정보를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val uid = user.uid
        val displayName = user.displayName.orEmpty()

        // (선택) Kakao 'sub' 추출
        val kakaoSub: String? = (result.credential as? OAuthCredential)?.idToken?.let { jwt ->
            try {
                val parts = jwt.split(".")
                if (parts.size >= 2) {
                    val payloadJson = String(
                        android.util.Base64.decode(
                            parts[1],
                            android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP
                        )
                    )
                    Regex(""""sub"\s*:\s*"([^"]+)"""").find(payloadJson)?.groupValues?.getOrNull(1)
                } else null
            } catch (_: Exception) { null }
        }
        Log.d(TAG, "Firebase uid=$uid, kakao sub=$kakaoSub, name=$displayName")

        routeByProfile(uid, displayName)
    }

    private fun routeByProfile(uid: String, nicknameHint: String) {
        db.collection("user").document(uid).get()
            .addOnSuccessListener { snap ->
                if (snap.exists()) {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    val intent = Intent(this, SignupActivity::class.java).apply {
                        putExtra(EXTRA_UID, uid)
                        putExtra(EXTRA_NICKNAME, nicknameHint)
                    }
                    startActivity(intent)
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "프로필 조회 실패", e)
                Toast.makeText(this, "프로필 확인 실패: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showAuthError(t: Throwable) {
        Log.e(TAG, "OIDC 로그인 실패", t)
        Toast.makeText(this, "로그인 실패: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}
