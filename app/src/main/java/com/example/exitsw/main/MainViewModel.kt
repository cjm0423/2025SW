package com.example.exitsw.main

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.exitsw.data.LocalWelfareServiceDto
import com.example.exitsw.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainViewModel : ViewModel() {
    private val _welfareList = MutableLiveData<List<LocalWelfareServiceDto>>()
    val welfareList: LiveData<List<LocalWelfareServiceDto>> get() = _welfareList

    private val _recommendList = MutableLiveData<List<LocalWelfareServiceDto>>()
    val recommendList: LiveData<List<LocalWelfareServiceDto>> get() = _recommendList

    private val _popularList = MutableLiveData<List<LocalWelfareServiceDto>>()
    val popularList: LiveData<List<LocalWelfareServiceDto>> get() = _popularList

    init {
        loadPlaceholderData()
        fetchLocalWelfareData()
    }

    private fun loadPlaceholderData() {
        // ✨ [변경] 각 목록에 맞는 플레이схолдер 데이터를 생성합니다.
        val recommendPlaceholder = LocalWelfareServiceDto("REC_LOADING", "추천 상품 로딩중...", "", null, null, null, null)
        val popularPlaceholder = LocalWelfareServiceDto("POP_LOADING", "인기 상품 로딩중...", "", null, null, null, null)
        val policyPlaceholder = LocalWelfareServiceDto("POL_LOADING", "정책 로딩중...", "", null, null, null, null)

        _recommendList.value = listOf(recommendPlaceholder, recommendPlaceholder, recommendPlaceholder)
        _popularList.value = listOf(popularPlaceholder, popularPlaceholder, popularPlaceholder)
        _welfareList.value = listOf(policyPlaceholder, policyPlaceholder, policyPlaceholder)
    }

    private fun fetchLocalWelfareData() {
        val call = RetrofitClient.instance.getLocalWelfareList(sigunguCd = "")

        call.enqueue(object : Callback<List<LocalWelfareServiceDto>> {
            override fun onResponse(
                call: Call<List<LocalWelfareServiceDto>>,
                response: Response<List<LocalWelfareServiceDto>>
            ) {
                if (response.isSuccessful) {
                    // ✨ [변경] 이제 오직 welfareList만 실제 데이터로 업데이트합니다.
                    _welfareList.value = response.body()
                    Log.d("MainViewModel", "성공: '지역별 지원 정책' 데이터를 업데이트했습니다.")
                } else {
                    Log.e("MainViewModel", "오류: ${response.code()}.")
                }
            }

            override fun onFailure(call: Call<List<LocalWelfareServiceDto>>, t: Throwable) {
                Log.e("MainViewModel", "실패: ${t.message}.")
            }
        })
    }
}
