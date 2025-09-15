package com.example.exitsw.repository

import android.util.Log
import com.example.exitsw.data.LocalWelfareServiceDto
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class FirebaseRepository {

    private val db = Firebase.firestore

    suspend fun getAllWelfareServices(): List<LocalWelfareServiceDto> {
        return try {
            val snapshot = db.collection("policies")
                .document("all")
                .collection("items")
                .limit(500) // 데이터를 n개로 제한합니다.
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

    // 추가: favoritesCount 기준 인기순
    suspend fun getPopularWelfareServices(limit: Int = 10): List<LocalWelfareServiceDto> {
        return try {
            val snapshot = db.collection("policies")
                .document("all")
                .collection("items")
                .whereGreaterThan("favoritesCount", 0) // 좋아요 없는 문서는 제외
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
}

