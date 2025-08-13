package com.example.exitsw.main

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.exitsw.data.LocalWelfareServiceDto
import com.example.exitsw.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainViewModel(application: Application) : AndroidViewModel(application) {

    // 홈 화면 카드 목록을 위한 LiveData
    private val _recommendList = MutableLiveData<List<LocalWelfareServiceDto>>()
    val recommendList: LiveData<List<LocalWelfareServiceDto>> get() = _recommendList

    private val _popularList = MutableLiveData<List<LocalWelfareServiceDto>>()
    val popularList: LiveData<List<LocalWelfareServiceDto>> get() = _popularList

    private val _homeRegionList = MutableLiveData<List<LocalWelfareServiceDto>>()
    val homeRegionList: LiveData<List<LocalWelfareServiceDto>> get() = _homeRegionList

    // 지역 목록 화면을 위한 LiveData
    private val _groupedWelfareData = MutableLiveData<Map<String, List<LocalWelfareServiceDto>>>()
    val groupedWelfareData: LiveData<Map<String, List<LocalWelfareServiceDto>>> get() = _groupedWelfareData

    private val _regionList = MutableLiveData<List<String>>()
    val regionList: LiveData<List<String>> get() = _regionList


    init {
        loadStaticData()
        fetchLocalWelfareData()
    }

    private fun loadStaticData() {
        val placeholder = LocalWelfareServiceDto("LOADING", "로딩 중...", "데이터를 불러오는 중", null, null, null, null)
        _recommendList.value = listOf(placeholder, placeholder, placeholder, placeholder, placeholder)
        _popularList.value = listOf(placeholder, placeholder, placeholder, placeholder, placeholder)

        // ✨ [핵심 수정] API 호출과 상관없이 항상 보여줄 전국 팔도 목록
        val staticRegions = listOf(
            "서울시", "경기도", "강원도", "충청북도", "충청남도",
            "전라북도", "전라남도", "경상북도", "경상남도", "제주도"
        )
        // 홈 화면 미리보기용 DTO 생성
        _homeRegionList.value = staticRegions.map { regionName ->
            LocalWelfareServiceDto(
                serviceId = regionName, // ID로 지역 이름을 임시 사용
                serviceName = regionName,
                department = "정책 목록 보기",
                summary = null, region = regionName, city = null, detailLink = null
            )
        }
        // 전체 지역 목록 화면용 String 리스트
        _regionList.value = staticRegions
    }

    // ✨ [핵심 수정] API에서 오는 다양한 지역명을 표준화하는 함수
    private fun normalizeRegion(apiRegion: String?): String {
        return when {
            apiRegion == null -> "기타"
            apiRegion.contains("서울") -> "서울시"
            apiRegion.contains("경기") -> "경기도"
            apiRegion.contains("강원") -> "강원도"
            apiRegion.contains("충북") || apiRegion.contains("충청북도") -> "충청북도"
            apiRegion.contains("충남") || apiRegion.contains("충청남도") -> "충청남도"
            apiRegion.contains("전북") || apiRegion.contains("전라북도") -> "전라북도"
            apiRegion.contains("전남") || apiRegion.contains("전라남도") || apiRegion.contains("광주") -> "전라남도"
            apiRegion.contains("경북") || apiRegion.contains("경상북도") || apiRegion.contains("대구") -> "경상북도"
            apiRegion.contains("경남") || apiRegion.contains("경상남도") || apiRegion.contains("부산") || apiRegion.contains("울산") -> "경상남도"
            apiRegion.contains("제주") -> "제주도"
            else -> "기타" // 인천, 대전, 세종 등은 기타로 분류
        }
    }

    private fun fetchLocalWelfareData() {
        val call = RetrofitClient.getInstance(getApplication()).getLocalWelfareList(sigunguCd = "")

        call.enqueue(object : Callback<List<LocalWelfareServiceDto>> {
            override fun onResponse(
                call: Call<List<LocalWelfareServiceDto>>,
                response: Response<List<LocalWelfareServiceDto>>
            ) {
                if (response.isSuccessful) {
                    val policyList = response.body()
                    if (policyList != null) {
                        // ✨ [핵심 수정] 표준화된 지역명으로 데이터를 그룹핑합니다.
                        val groupedData = policyList.groupBy { normalizeRegion(it.region) }
                        _groupedWelfareData.value = groupedData
                        // API 성공 시, 실제 데이터가 있는 지역 목록으로만 갱신
                        _regionList.value = groupedData.keys.sorted()
                        Log.d("MainViewModel", "성공: 데이터를 지역별로 그룹핑했습니다.")
                    }
                }
            }

            override fun onFailure(call: Call<List<LocalWelfareServiceDto>>, t: Throwable) {
                // 실패 시에는 미리 설정된 staticRegions가 그대로 유지됨
                Log.e("MainViewModel", "실패: ${t.message}")
            }
        })
    }
}
