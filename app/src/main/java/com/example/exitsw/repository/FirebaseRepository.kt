package com.example.exitsw.repository

import android.util.Log
import com.example.exitsw.data.LocalWelfareServiceDto
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class FirebaseRepository {

    private val db = Firebase.firestore

    /** 전체 정책 일부 로드 */
    suspend fun getAllWelfareServices(): List<LocalWelfareServiceDto> {
        return try {
            val snapshot = db.collection("policies")
                .document("all")
                .collection("items")
                .limit(1000) // 데이터 불러오는 갯수
                .get()
                .await()

            val policies = snapshot.toObjects(LocalWelfareServiceDto::class.java)
            Log.d("FirebaseRepository", "성공: ${policies.size}개의 데이터를 Firestore에서 가져왔습니다.")
            policies
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "데이터 가져오기 실패", e)
            emptyList()
        }
    }

    /** 인기순 (favoritesCount 기준) */
    suspend fun getPopularWelfareServices(limit: Int = 10): List<LocalWelfareServiceDto> {
        return try {
            val snapshot = db.collection("policies")
                .document("all")
                .collection("items")
                .whereGreaterThan("favoritesCount", 0)
                .orderBy("favoritesCount", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            snapshot.toObjects(LocalWelfareServiceDto::class.java)
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "인기 정책 불러오기 실패", e)
            emptyList()
        }
    }

    /** 관심 목록 실시간 관찰: user/{uid}/favorites */
    fun observeFavorites(uid: String, onResult: (List<LocalWelfareServiceDto>) -> Unit) {
        val ref = db.collection("user").document(uid).collection("favorites")

        ref.addSnapshotListener { snap, e ->
            if (e != null || snap == null) {
                Log.e("FavoritesRepo", "listen error", e)
                onResult(emptyList())
                return@addSnapshotListener
            }

            // 문서 -> DTO 수동 매핑 (DTO 필드명: servId/servNm/… 에 맞춤)
            val list = snap.documents.map { doc ->
                val d = doc.data ?: emptyMap<String, Any?>()

                fun s(key: String) = d[key]?.toString()

                LocalWelfareServiceDto(
                    aplyMtdNm       = s("aplyMtdNm"),
                    bizChrDeptNm    = s("bizChrDeptNm"),
                    ctpvNm          = s("ctpvNm"),
                    inqNum          = s("inqNum"),
                    intrsThemaNmArray = d["intrsThemaNmArray"],
                    lastModYmd      = s("lastModYmd"),
                    lifeNmArray     = d["lifeNmArray"],
                    servDgst        = s("servDgst"),
                    servDtlLink     = s("servDtlLink"),
                    // 문서 id를 보강(필드가 없을 경우 대비)
                    servId          = s("servId") ?: doc.id,
                    servNm          = s("servNm"),
                    sggNm           = s("sggNm"),
                    source          = s("source"),
                    sprtCycNm       = s("sprtCycNm"),
                    srvPvsnNm       = s("srvPvsnNm"),
                    // syncedAt 은 Timestamp? 타입이라 그대로 둠(없으면 null)
                    syncedAt        = d["syncedAt"] as? com.google.firebase.Timestamp,
                    trgterIndvdlNmArray = d["trgterIndvdlNmArray"]
                )
            }

            Log.d("FavoritesRepo", "favorites snap size=${list.size}")
            onResult(list)
        }
    }

    /** 관심 토글: user/{uid}/favorites/{servId} */
    suspend fun toggleFavorite(uid: String, dto: LocalWelfareServiceDto): Boolean {
        val id = dto.servId ?: throw IllegalArgumentException("servId null")
        val favRef = db.collection("user").document(uid)
            .collection("favorites").document(id)

        val snap = favRef.get().await()
        return if (snap.exists()) {
            favRef.delete().await()
            Log.d("FavoritesRepo", "Removed favorite: $id")
            false
        } else {
            favRef.set(dto).await()  // DTO 필드 그대로 저장
            Log.d("FavoritesRepo", "Added favorite: $id")
            true
        }
    }
}
