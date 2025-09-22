package com.example.exitsw.mypage

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.exitsw.data.LocalWelfareServiceDto
import com.example.exitsw.repository.FirebaseRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

/**
 * 마이페이지 ViewModel
 * - 사용자 프로필(users/{uid}) 단건 문서 구독
 * - 관심 목록 실시간 구독
 */
class MypageViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val repo = FirebaseRepository()
    private val db = Firebase.firestore

    // ------------------- 프로필 -------------------
    data class UserProfile(
        val nickname: String = "",
        val district: String? = null // "노원구" 등. 필드명이 "gu"인 경우도 대비
    )

    private val _userProfile = MutableLiveData<UserProfile?>()
    val userProfile: LiveData<UserProfile?> get() = _userProfile

    private var userListener: ListenerRegistration? = null

    /**
     * 사용자 프로필 실시간 관찰 시작
     * user/{uid} 문서를 구독해서 닉네임/지역구를 반영
     */
    fun startObserveUser() {
        val uid = auth.currentUser?.uid ?: run {
            _userProfile.postValue(null)
            return
        }

        // 기존 리스너 제거 후 재구독
        userListener?.remove()
        userListener = db.collection("user")
            .document(uid)
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    Log.w("MypageVM", "user doc listen error", e)
                    return@addSnapshotListener
                }
                if (snap != null && snap.exists()) {
                    val nickname = snap.getString("nickname") ?: ""
                    // 필드명이 district 또는 gu 중 무엇이든 대응
                    val district = snap.getString("region_label") ?: snap.getString("gu")
                    _userProfile.postValue(UserProfile(nickname = nickname, district = district))
                } else {
                    _userProfile.postValue(null)
                }
            }
    }

    // ------------------- 관심 목록 -------------------
    private val _favoriteList = MutableLiveData<List<LocalWelfareServiceDto>>()
    val favoriteList: LiveData<List<LocalWelfareServiceDto>> get() = _favoriteList

    /**
     * 관심 목록 실시간 관찰 시작
     */
    fun startObserveFavorites() {
        val uid = auth.currentUser?.uid ?: return
        repo.observeFavorites(uid) { list ->
            _favoriteList.postValue(list)
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Firestore 리스너 해제
        userListener?.remove()
        userListener = null
    }
}
