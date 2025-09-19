package com.example.exitsw

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class WithdrawActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "WithdrawActivity"
        private const val OIDC_PROVIDER_ID = "oidc.kakao" // Firebase 콘솔의 Provider ID
    }

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

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
            // 비밀번호 UI가 있더라도 카카오 OIDC 계정이면 비밀번호값은 보통 없습니다.
            // (이 앱은 OIDC 카카오 로그인이므로, 아래는 OIDC 재인증 경로를 사용합니다.)

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

        // 1) (가능하면) 카카오 OIDC로 재인증 — 최근 로그인 요구 대비
        reauthenticateWithKakao(
            onSuccess = {
                // 2) 카카오 토큰/연결 해제 (SDK가 있다면 unlink, 없으면 로그아웃/쿠키 정리)
                unlinkKakaoIfPossible(
                    onDone = {
                        // 3) Firestore에서 유저 문서 삭제
                        deleteFirestoreProfile(
                            onDone = {
                                // 4) Firebase Auth 계정 삭제
                                deleteFirebaseAccount(
                                    onDone = {
                                        // 5) 세션/쿠키 정리 & 로그인 화면으로 이동
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

    /** OIDC(Kakao)로 최근 로그인 요구를 만족시키기 위한 재인증 */
    private fun reauthenticateWithKakao(
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val user = auth.currentUser ?: return onError(IllegalStateException("no user"))
        val provider = OAuthProvider.newBuilder(OIDC_PROVIDER_ID)
            .addCustomParameter("prompt", "login") // 매번 계정선택/재인증 유도
            .build()

        // Android용 Firebase는 reauthenticateWithCredential(OAuthCredential) 외에
        // **startActivityForReauthenticateWithProvider** 가 제공됩니다.
        user.startActivityForReauthenticateWithProvider(this, provider)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }

    /** 카카오 연결 해제: Kakao SDK가 있으면 진짜 unlink, 없으면 signOut + CustomTabs 쿠키 정리 */
    private fun unlinkKakaoIfPossible(onDone: () -> Unit) {
        // (선택) Kakao SDK 사용 시
        // implementation "com.kakao.sdk:v2-user:<version>"
        // 아래 코드는 SDK가 프로젝트에 포함되어 있을 때만 유효합니다.
        try {
            val clazz = Class.forName("com.kakao.sdk.user.UserApiClient")
            val instanceField = clazz.getDeclaredField("instance")
            val instance = instanceField.get(null)

            val unlinkMethod = clazz.methods.firstOrNull { it.name == "unlink" }
            if (unlinkMethod != null) {
                // UserApiClient.instance.unlink { error -> ... }
                unlinkMethod.invoke(instance, { error: Throwable? ->
                    if (error != null) {
                        Log.w(TAG, "Kakao unlink failed (SDK): ${error.localizedMessage}")
                    } else {
                        Log.d(TAG, "Kakao unlink success (SDK)")
                    }
                    // 어떤 경우든 계속 진행
                    clearCustomTabsCookies()
                    onDone()
                })
                return
            }
        } catch (_: Throwable) {
            // Kakao SDK 미포함이거나 리플렉션 실패 → 아래로 폴백
        }

        // SDK가 없으면, 최소한 로그인 세션 흔적 제거
        clearCustomTabsCookies()
        onDone()
    }

    /** Firestore 유저 문서 삭제 (하위 컬렉션은 별도 처리 필요) */
    private fun deleteFirestoreProfile(
        onDone: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: return onError(IllegalStateException("no uid"))
        db.collection("user").document(uid)
            .delete()
            .addOnSuccessListener { onDone() }
            .addOnFailureListener { onError(it) }
    }

    /** Firebase Auth 계정 삭제 */
    private fun deleteFirebaseAccount(
        onDone: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val user = auth.currentUser ?: return onError(IllegalStateException("no user"))
        user.delete()
            .addOnSuccessListener { onDone() }
            .addOnFailureListener { e ->
                // 최근 로그인 요구 시도중인데도 실패하면 메시지 안내
                onError(e)
            }
    }

    /** 로그아웃 및 쿠키 정리 후 로그인 화면으로 */
    private fun signOutAndGoLogin() {
        try { auth.signOut() } catch (_: Throwable) { /* no-op */ }
        clearCustomTabsCookies()

        Toast.makeText(this, "탈퇴가 완료되었습니다.", Toast.LENGTH_LONG).show()
        val intent = Intent(this, LoginActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
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
        } catch (_: Throwable) { /* ignore */ }
    }
}
