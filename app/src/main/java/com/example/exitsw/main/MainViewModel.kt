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

    // 1. 데이터를 지역별로 묶어서 저장할 LiveData
    // 예: { "서울시": [정책1, 정책2], "강원도": [정책3] }
    private val _groupedWelfareData = MutableLiveData<Map<String, List<LocalWelfareServiceDto>>>()
    val groupedWelfareData: LiveData<Map<String, List<LocalWelfareServiceDto>>> get() = _groupedWelfareData

    // 2. 지역 이름 목록만 따로 저장할 LiveData
    private val _regionList = MutableLiveData<List<String>>()
    val regionList: LiveData<List<String>> get() = _regionList

    // 3. 홈 화면 미리보기를 위한 전체 목록 LiveData (필요 시 사용)
    private val _welfareList = MutableLiveData<List<LocalWelfareServiceDto>>()
    val welfareList: LiveData<List<LocalWelfareServiceDto>> get() = _welfareList


    init {
        fetchLocalWelfareData()
    }

    private fun fetchLocalWelfareData() {
        val call = RetrofitClient.instance.getLocalWelfareList(sigunguCd = "")

        call.enqueue(object : Callback<List<LocalWelfareServiceDto>> {
            override fun onResponse(
                call: Call<List<LocalWelfareServiceDto>>,
                response: Response<List<LocalWelfareServiceDto>>
            ) {
                if (response.isSuccessful) {
                    val policyList = response.body()
                    if (!policyList.isNullOrEmpty()) {
                        // 전체 목록 저장
                        _welfareList.value = policyList

                        // 받아온 정책 리스트를 'region'을 기준으로 그룹핑
                        val groupedData = policyList.groupBy { it.region ?: "기타" }
                        _groupedWelfareData.value = groupedData

                        // 그룹핑된 데이터에서 지역 이름(Key)만 뽑아서 리스트 생성
                        _regionList.value = groupedData.keys.sorted()

                        Log.d("MainViewModel", "성공: 데이터를 지역별로 그룹핑했습니다.")
                    }
                } else {
                    Log.e("MainViewModel", "오류: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<LocalWelfareServiceDto>>, t: Throwable) {
                Log.e("MainViewModel", "실패: ${t.message}")
            }
        })
    }
}
