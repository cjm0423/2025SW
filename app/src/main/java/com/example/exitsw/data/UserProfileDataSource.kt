// app/src/main/java/com/example/exitsw/data/local/UserProfileDataSource.kt
package com.example.exitsw.data

import com.example.exitsw.domain.model.UserProfile
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date

class UserProfileDataSource(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun getUserProfile(uid: String?): UserProfile? {
        // 1) 후보 UID: 함수 인자 → FirebaseAuth → null
        val candidateUid = uid?.takeIf { it.isNotBlank() }
            ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid

        // 2) 문서 id == uid 시도
        candidateUid?.let { u ->
            getByDocumentId(u)?.let { return it }
        }

        // 3) auth_uid 필드로 조회 (너가 보여준 스키마에 존재)
        candidateUid?.let { u ->
            getByAuthUid(u)?.let { return it }
        }

        // 4) 그래도 없으면 null
        return null
    }

    private suspend fun getByDocumentId(docId: String): UserProfile? {
        val snap = runCatching { db.collection("user").document(docId).get().await() }.getOrNull()
            ?: return null
        if (!snap.exists()) return null
        return toDomain(snap.data ?: return null, docId)
    }

    private suspend fun getByAuthUid(authUid: String): UserProfile? {
        val qs = runCatching {
            db.collection("user").whereEqualTo("auth_uid", authUid).limit(1).get().await()
        }.getOrNull() ?: return null

        val doc = qs.documents.firstOrNull() ?: return null
        return toDomain(doc.data ?: return null, /*uid=*/authUid)
    }

    private fun toDomain(map: Map<String, Any>, uid: String): UserProfile {
        val nickname    = map["nickname"] as? String ?: ""
        val gender      = map["gender"] as? String ?: ""
        val regionLabel = map["region_label"] as? String ?: "전국"
        val income      = map["income"] as? String ?: ""
        val interest    = map["interest"] as? String ?: ""

        // age_years 없으면 birth(Timestamp)로 계산
        val ageYears = when (val ay = map["age_years"]) {
            is Number -> ay.toInt()
            else -> {
                val ts = map["birth"] as? Timestamp
                ts?.toDate()?.let { calculateAge(it) } ?: 0
            }
        }

        return UserProfile(
            uid = uid,
            nickname = nickname,
            gender = gender,
            ageYears = ageYears,
            regionLabel = regionLabel,
            income = income,
            interest = interest
        )
    }

    private fun calculateAge(birth: Date, now: Date = Date()): Int {
        val cNow = Calendar.getInstance().apply { time = now }
        val cBirth = Calendar.getInstance().apply { time = birth }
        var age = cNow.get(Calendar.YEAR) - cBirth.get(Calendar.YEAR)
        if (cNow.get(Calendar.DAY_OF_YEAR) < cBirth.get(Calendar.DAY_OF_YEAR)) age--
        return age.coerceAtLeast(0)
    }
}
