// WithdrawActivity.kt
package com.example.exitsw

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.CookieManager
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import java.net.URLEncoder

class WithdrawActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "WithdrawActivity"
        private const val OIDC_PROVIDER_ID = "oidc.kakao" // Firebase 콘솔의 Provider ID
    }

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private lateinit var etPw: TextInputEditText
    private lateinit var etPw2: TextInputEditText
    private lateinit var cbAgree: CheckBox
    private lateinit var btnWithdraw: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_withdraw)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        cbAgree = findViewById(R.id.cbAgreeWithdraw)
        btnWithdraw = findViewById(R.id.btnWithdraw)

        btnWithdraw.setOnClickListener {
            if (!cbAgree.isChecked) {
                Toast.makeText(this, "탈퇴 안내에 동의해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            AlertDialog.Builder(this)
                .setTitle("회원 탈퇴")
                .setMessage("정말로 탈퇴하시겠어요? 모든 데이터가 삭제됩니다.")
                .setPositiveButton("탈퇴") { _, _ -> startWithdrawalFlow() }
                .setNegativeButton("취소", null)
                .show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun lockUi(locked: Boolean) {
        btnWithdraw.isEnabled = !locked
        btnWithdraw.text = if (locked) "처리 중…" else "회원탈퇴"
    }

    private fun startWithdrawalFlow() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }
        lockUi(true)

        // 1) OIDC 재인증(최근 로그인 요구 대비)
        reauthenticateWithKakao(
            onSuccess = {
                // 2) 카카오 연결 해제(가능 시) & 웹 쿠키 정리
                unlinkKakaoIfPossible(
                    onDone = {
                        // 3) Firestore 유저 문서 삭제
                        deleteFirestoreProfile(
                            onDone = {
                                // 4) Firebase Auth 계정 삭제
                                deleteFirebaseAccount(
                                    onDone = {
                                        // 5) 세션/쿠키 정리 + 카카오 로그아웃 URL 호출 + 로그인 화면
                                        signOutAndGoLogin()
                                    },
                                    onError = { e ->
                                        lockUi(false)
                                        Log.e(TAG, "deleteFirebaseAccount failed", e)
                                        Toast.makeText(this, "계정 삭제 실패: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            onError = { e ->
                                lockUi(false)
                                Log.e(TAG, "deleteFirestoreProfile failed", e)
                                Toast.makeText(this, "프로필 삭제 실패: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                )
            },
            onError = { e ->
                lockUi(false)
                Log.e(TAG, "reauthenticateWithKakao failed", e)
                Toast.makeText(this, "재인증 실패: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        )
    }

    /** OIDC(Kakao) 재인증 */
    private fun reauthenticateWithKakao(
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val user = auth.currentUser ?: return onError(IllegalStateException("no user"))
        val provider = OAuthProvider.newBuilder(OIDC_PROVIDER_ID)
            .addCustomParameter("prompt", "login") // 매번 계정선택/재인증 유도
            .build()

        user.startActivityForReauthenticateWithProvider(this, provider)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }

    /** Kakao SDK 있으면 unlink, 없으면 쿠키 정리만 */
    private fun unlinkKakaoIfPossible(onDone: () -> Unit) {
        try {
            val clazz = Class.forName("com.kakao.sdk.user.UserApiClient")
            val instanceField = clazz.getDeclaredField("instance")
            val instance = instanceField.get(null)
            val unlinkMethod = clazz.methods.firstOrNull { it.name == "unlink" }
            if (unlinkMethod != null) {
                unlinkMethod.invoke(instance, { error: Throwable? ->
                    if (error != null) {
                        Log.w(TAG, "Kakao unlink failed (SDK): ${error.localizedMessage}")
                    } else {
                        Log.d(TAG, "Kakao unlink success (SDK)")
                    }
                    clearCustomTabsCookies()
                    onDone()
                })
                return
            }
        } catch (_: Throwable) {
            // Kakao SDK 미포함 → 폴백 진행
        }
        clearCustomTabsCookies()
        onDone()
    }

    /** Firestore 유저 문서 삭제 (하위 컬렉션은 서버에서 처리 권장) */
    private fun deleteFirestoreProfile(onDone: () -> Unit, onError: (Throwable) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onError(IllegalStateException("no uid"))
        db.collection("user").document(uid)
            .delete()
            .addOnSuccessListener { onDone() }
            .addOnFailureListener { onError(it) }
    }

    /** Firebase Auth 계정 삭제 */
    private fun deleteFirebaseAccount(onDone: () -> Unit, onError: (Throwable) -> Unit) {
        val user = auth.currentUser ?: return onError(IllegalStateException("no user"))
        user.delete()
            .addOnSuccessListener { onDone() }
            .addOnFailureListener { onError(it) }
    }

    /** 로그아웃 및 쿠키 정리 후 로그인 화면으로 */
    private fun signOutAndGoLogin() {
        try { auth.signOut() } catch (_: Throwable) { }
        clearCustomTabsCookies()

        // 카카오 OAuth 로그아웃 URL도 호출(브라우저 세션 제거)
        try {
            val clientId = getString(R.string.kakao_rest_api_key)
            val redirect = URLEncoder.encode(getString(R.string.kakao_logout_redirect), "UTF-8")
            val kakaoLogoutUrl = "https://kauth.kakao.com/oauth/logout?client_id=$clientId&logout_redirect_uri=$redirect"
            startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(kakaoLogoutUrl)))
        } catch (_: Throwable) { /* 브라우저 미존재 등은 무시 */ }

        Toast.makeText(this, "탈퇴가 완료되었습니다.", Toast.LENGTH_LONG).show()
        val intent = Intent(this, LoginActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra("NEED_DEEP_CLEAN", true) // 로그인 화면에서 추가 세션 정리
        }
        startActivity(intent)
        finish()
    }

    /** Custom Tabs/웹 쿠키 제거 — 다음 OIDC에서 계정선택/재로그인 유도 */
    private fun clearCustomTabsCookies() {
        try {
            val cm = CookieManager.getInstance()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                cm.removeAllCookies(null)
                cm.flush()
            } else {
                @Suppress("DEPRECATION")
                cm.removeAllCookie()
            }
        } catch (_: Throwable) { }
    }
}
