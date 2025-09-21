package com.example.exitsw.mypage

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.exitsw.data.LocalWelfareServiceDto
import com.example.exitsw.repository.FirebaseRepository
import com.google.firebase.auth.FirebaseAuth


class MypageViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val repo = FirebaseRepository()

    // LiveData: 관심 목록
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
}
