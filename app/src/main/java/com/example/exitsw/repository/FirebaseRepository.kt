package com.example.exitsw.repository

import android.util.Log
import com.example.exitsw.data.LocalWelfareServiceDto
import com.google.firebase.firestore.ktx.firestore
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
}

