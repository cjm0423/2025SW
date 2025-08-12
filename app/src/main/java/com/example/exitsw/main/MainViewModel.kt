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
    // LiveData 선언 (기존과 동일)
    private val _welfareList = MutableLiveData<List<LocalWelfareServiceDto>>()
    val welfareList: LiveData<List<LocalWelfareServiceDto>> get() = _welfareList

    private val _recommendList = MutableLiveData<List<LocalWelfareServiceDto>>()
    val recommendList: LiveData<List<LocalWelfareServiceDto>> get() = _recommendList

    private val _popularList = MutableLiveData<List<LocalWelfareServiceDto>>()
    val popularList: LiveData<List<LocalWelfareServiceDto>> get() = _popularList

    init {
        // ✨ [변경] 1. 임시 데이터를 먼저 로드합니다.
        loadPlaceholderData()
        // 2. 실제 API 데이터를 호출합니다.
        fetchLocalWelfareData()
    }

    // ✨ [변경] 임시 데이터를 생성하는 함수
    private fun loadPlaceholderData() {
        val placeholder = LocalWelfareServiceDto(
            serviceId = "LOADING",
            serviceName = "로딩 중...",
            department = "데이터를 불러오고 있습니다.",
            summary = null, region = null, city = null, detailLink = null
        )
        // 3개의 임시 카드를 만들어 LiveData에 할당
        val placeholderList = listOf(placeholder, placeholder, placeholder)

        _recommendList.value = placeholderList
        _popularList.value = placeholderList
        _welfareList.value = placeholderList
    }

    private fun fetchLocalWelfareData() {
        val call = RetrofitClient.instance.getLocalWelfareList(sigunguCd = "")

        call.enqueue(object : Callback<List<LocalWelfareServiceDto>> {
            override fun onResponse(
                call: Call<List<LocalWelfareServiceDto>>,
                response: Response<List<LocalWelfareServiceDto>>
            ) {
                if (response.isSuccessful) {
                    // ✨ [변경] 3. API 호출 성공 시, 임시 데이터를 실제 데이터로 교체합니다.
                    val realData = response.body()
                    _welfareList.value = realData

                    // TODO: 추천/인기 상품도 실제 API로 교체 필요
                    _recommendList.value = realData?.take(3)
                    _popularList.value = realData?.shuffled()?.take(3)

                    Log.d("MainViewModel", "성공: 실제 데이터로 업데이트했습니다.")
                } else {
                    Log.e("MainViewModel", "오류: ${response.code()}. 플레이схолдер 데이터를 유지합니다.")
                }
            }

            override fun onFailure(call: Call<List<LocalWelfareServiceDto>>, t: Throwable) {
                // ✨ [변경] 4. API 호출 실패 시, 아무것도 하지 않아 임시 데이터가 그대로 유지됩니다.
                Log.e("MainViewModel", "실패: ${t.message}. 플레이схолдер 데이터를 유지합니다.")
            }
        })
    }
}
