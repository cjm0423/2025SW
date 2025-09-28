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
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.Locale

class WithdrawActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "WithdrawActivity"
        private const val OIDC_PROVIDER_ID = "oidc.kakao" // Firebase 콘솔의 Provider ID
        private const val BATCH_LIMIT = 500
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

        // 코루틴으로 순서대로 처리
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                // 1) 재인증
                reauthenticateWithKakaoAwait()

                // 2) 카카오 연결 해제(가능 시) + 쿠키 정리
                unlinkKakaoIfPossible()

                // 3) 관심상품 서브컬렉션 및 카운터/likes 정리 (★ 추가된 단계)
                val uid = auth.currentUser?.uid ?: throw IllegalStateException("no uid")
                deleteUserFavoritesAndCounters(uid)

                // 4) Firestore 유저 문서 삭제
                deleteFirestoreProfileAwait(uid)

                // 5) Firebase Auth 계정 삭제
                deleteFirebaseAccountAwait()

                // 6) 로그아웃/쿠키 정리 및 로그인 화면으로
                signOutAndGoLogin()
            } catch (e: Throwable) {
                lockUi(false)
                Log.e(TAG, "withdrawal failed", e)
                Toast.makeText(
                    this@WithdrawActivity,
                    "탈퇴 처리 실패: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /** OIDC(Kakao) 재인증 (await 버전) */
    private suspend fun reauthenticateWithKakaoAwait() {
        val user = auth.currentUser ?: throw IllegalStateException("no user")
        val provider = OAuthProvider.newBuilder(OIDC_PROVIDER_ID)
            .addCustomParameter("prompt", "login")
            .build()
        user.startActivityForReauthenticateWithProvider(this, provider).await()
    }

    /** 카카오 unlink 시도(있으면) + Custom Tabs 쿠키 정리 */
    private fun unlinkKakaoIfPossible() {
        try {
            val clazz = Class.forName("com.kakao.sdk.user.UserApiClient")
            val instanceField = clazz.getDeclaredField("instance")
            val instance = instanceField.get(null)
            val unlinkMethod = clazz.methods.firstOrNull { it.name == "unlink" }
            if (unlinkMethod != null) {
                // 비동기 콜백 기반 → 그냥 호출만 하고, 결과와 무관하게 진행
                unlinkMethod.invoke(instance, { error: Throwable? ->
                    if (error != null) Log.w(TAG, "Kakao unlink failed (SDK): ${error.localizedMessage}")
                    else Log.d(TAG, "Kakao unlink success (SDK)")
                    clearCustomTabsCookies()
                })
                return
            }
        } catch (_: Throwable) {
            // SDK 미포함 → 폴백
        }
        clearCustomTabsCookies()
    }

    /** ★ NEW: user/{uid}/favorites 전체 삭제 + policies 카운터 및 likes/{uid} 정리 */
    private suspend fun deleteUserFavoritesAndCounters(uid: String) {
        val favCol = db.collection("user").document(uid).collection("favorites")
        var lastDoc: QueryDocumentSnapshot? = null
        var totalDeleted = 0

        while (true) {
            var query = favCol.limit(BATCH_LIMIT.toLong())
            if (lastDoc != null) query = query.startAfter(lastDoc)
            val snap = query.get().await()
            if (snap.isEmpty) break

            val batch = db.batch()

            for (doc in snap.documents) {
                // 1) policies/all/items/{docId} 카운터 감소 & likes/{uid} 제거 시도
                val data = doc.data ?: emptyMap<String, Any?>()
                val docId = resolveDocIdSameAsSync(data)
                val itemRef = db.collection("policies").document("all")
                    .collection("items").document(docId)

                batch.update(itemRef, "favoritesCount", FieldValue.increment(-1))
                // likes/{uid} 문서가 존재할 수도 있으니 삭제 시도 (존재 안해도 에러 아님)
                val likeRef = itemRef.collection("likes").document(uid)
                batch.delete(likeRef)

                // 2) user/{uid}/favorites/{servId} 문서 삭제
                batch.delete(doc.reference)
                totalDeleted++
            }

            batch.commit().await()
            lastDoc = snap.documents.last() as QueryDocumentSnapshot
        }

        Log.d(TAG, "Deleted favorites: $totalDeleted for uid=$uid")
    }

    /** Firestore 유저 문서 삭제 (await 버전) */
    private suspend fun deleteFirestoreProfileAwait(uid: String) {
        db.collection("user").document(uid).delete().await()
    }

    /** Firebase Auth 계정 삭제 (await 버전) */
    private suspend fun deleteFirebaseAccountAwait() {
        val user = auth.currentUser ?: throw IllegalStateException("no user")
        user.delete().await()
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

    // ---------- 아래는 PolicyDetailFragment의 문서 ID 계산 로직을 복사(동일해야 카운터 대상이 정확히 일치) ----------

    private fun resolveDocIdSameAsSync(map: Map<String, Any?>): String {
        val candidate = listOf("servId", "svcId", "id", "service_id", "no")
            .firstNotNullOfOrNull { k ->
                map[k]?.toString()?.takeIf { it.isNotBlank() }
            }
        return candidate ?: sha1(Gson().toJson(map))
    }

    private fun sha1(input: String): String {
        val md = MessageDigest.getInstance("SHA-1")
        val bytes = md.digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }.lowercase(Locale.US)
    }
}
