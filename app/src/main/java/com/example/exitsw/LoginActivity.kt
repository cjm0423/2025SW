package com.example.exitsw

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import androidx.core.view.isVisible
import androidx.activity.addCallback

class LoginActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "LoginActivity"
        const val EXTRA_UID = "uid"
        const val EXTRA_NICKNAME = "nickname"
        private const val OIDC_PROVIDER_ID = "oidc.kakao" // Firebase 콘솔 Provider ID 기준
    }

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }


    // 중복 호출 방지 플래그
    private var signingIn = false

    private val btnKakaoLogin: ImageButton by lazy { findViewById(R.id.btnKakaoLogin) }

    private val loadingOverlay by lazy { findViewById<View>(R.id.loadingOverlay) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // 기본: prompt 미지정(불필요한 계정 선택 UI 감소)
        btnKakaoLogin.setOnClickListener {
            startKakaoOidcSignIn(forcePrompt = false, selectAccount = false)
        }

        // 진행 중이었으면 복구 시도
        auth.pendingAuthResult?.let {
            setSigningIn(true)
            it.addOnSuccessListener { res -> handleAuthResult(res) }
                .addOnFailureListener { t -> showAuthError(t) }
        }

        onBackPressedDispatcher.addCallback(this) {
            if (!signingIn) signingIn = true
            //면 무시(대기 중 뒤로가기 차단)
        }
    }

    /** 로그인/서버 처리 중 상태 토글 + 터치 차단 */
    private fun setSigningIn(inProgress: Boolean) {
        signingIn = inProgress
        btnKakaoLogin.isEnabled = !inProgress
        loadingOverlay.isVisible = inProgress
    }

    /** Google Play services 상태 점검: 미설치/구버전이면 사용자에게 안내하고 로그인 시작 차단 */
    private fun ensurePlayServicesOrExplain(): Boolean {
        val api = GoogleApiAvailability.getInstance()
        val code = api.isGooglePlayServicesAvailable(this)
        return if (code == ConnectionResult.SUCCESS) {
            true
        } else {
            api.getErrorDialog(this, code, /*requestCode=*/1001)?.show()
            false
        }
    }

    /** Firebase Auth - OIDC(Kakao) 로그인 시작 */
    private fun startKakaoOidcSignIn(
        forcePrompt: Boolean = false,
        selectAccount: Boolean = false
    ) {
        if (signingIn) return
        if (!ensurePlayServicesOrExplain()) return  // ▶ Play Services 비정상 시 중단

        setSigningIn(true)

        val provider = OAuthProvider.newBuilder(OIDC_PROVIDER_ID).apply {
            when {
                selectAccount -> addCustomParameter("prompt", "select_account")
                forcePrompt   -> addCustomParameter("prompt", "login")
            }
            // (요청대로 스코프/클레임 설정 코드 제거)
        }.build()

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
            setSigningIn(false)
            Toast.makeText(this, "로그인 실패: 사용자 정보를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // 운영: 민감정보 최소화(토큰/페이로드 파싱/로그 남기지 않음)
        Log.d(TAG, "Firebase uid=${user.uid}, name=${user.displayName.orEmpty()}")

        setSigningIn(true)
        routeByProfile(user.uid, user.displayName.orEmpty())
    }

    private fun routeByProfile(uid: String, nicknameHint: String) {
        db.collection("user").document(uid).get()
            .addOnSuccessListener { snap ->
                if (snap.exists()) {
                    startActivity(Intent(this, MainActivity::class.java))
                } else {
                    val intent = Intent(this, SignupActivity::class.java).apply {
                        putExtra(EXTRA_UID, uid)
                        putExtra(EXTRA_NICKNAME, nicknameHint)
                    }
                    startActivity(intent)
                }
                finish()
            }
            .addOnFailureListener { e ->
                setSigningIn(false) // 실패했을 때만 다시 조작 가능
                Log.e(TAG, "프로필 조회 실패", e)
                Toast.makeText(this, "프로필 확인 실패: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }



    private fun showAuthError(t: Throwable) {
        setSigningIn(false)

        val code = (t as? FirebaseAuthException)?.errorCode
        Log.e(TAG, "OIDC 로그인 실패: code=$code, msg=${t.localizedMessage}", t)

        when (code) {
            "ERROR_WEB_CONTEXT_CANCELED" -> {
                Toast.makeText(this, "로그인이 취소되었어요. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
            }
            "ERROR_NETWORK_REQUEST_FAILED" -> {
                Toast.makeText(this, "네트워크가 불안정해요. 잠시 후 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(this, "로그인에 실패했어요. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
