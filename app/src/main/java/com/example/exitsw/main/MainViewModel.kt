package com.example.exitsw.main // 패키지 이름이 main으로 변경되었습니다.

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
    // 지자체 복지 목록을 담을 LiveData
    private val _welfareList = MutableLiveData<List<LocalWelfareServiceDto>>()
    val welfareList: LiveData<List<LocalWelfareServiceDto>> get() = _welfareList

    init {
        // ViewModel이 생성될 때 API 데이터를 호출
        fetchLocalWelfareData()
    }

    // API를 호출하는 함수
    private fun fetchLocalWelfareData() {
        val call = RetrofitClient.instance.getLocalWelfareList(sigunguCd = "")

        call.enqueue(object : Callback<List<LocalWelfareServiceDto>> {
            override fun onResponse(
                call: Call<List<LocalWelfareServiceDto>>,
                response: Response<List<LocalWelfareServiceDto>>
            ) {
                if (response.isSuccessful) {
                    _welfareList.value = response.body()
                    Log.d("MainViewModel", "성공: 데이터를 LiveData에 저장했습니다.")
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
