package com.example.exitsw.main

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.exitsw.data.LocalWelfareServiceDto
import com.example.exitsw.data.RegionInfo
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

    private val _regionInfoList = MutableLiveData<List<RegionInfo>>()
    val regionInfoList: LiveData<List<RegionInfo>> get() = _regionInfoList


    init {
        loadStaticData()
        fetchLocalWelfareData()
    }

    private fun loadStaticData() {
        // API 호출과 상관없이 항상 보여줄 서울시 + 9개 도 목록
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
        // 전체 지역 목록 화면용 RegionInfo 리스트 (초기값: 로딩 중)
        _regionInfoList.value = staticRegions.map { RegionInfo(name = it, policyCount = null) }

        // 추천/인기 상품 플레이스홀더
        val placeholder = LocalWelfareServiceDto("LOADING", "연결 확인 중...", "", null, null, null, null)
        _recommendList.value = listOf(placeholder, placeholder, placeholder)
        _popularList.value = listOf(placeholder, placeholder, placeholder)
    }

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
            else -> "기타"
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
                        val groupedData = policyList.groupBy { normalizeRegion(it.region) }
                        _groupedWelfareData.value = groupedData

                        // API 성공 시, 정책 개수를 포함한 RegionInfo 리스트로 갱신
                        val currentRegions = _regionInfoList.value?.map { it.name } ?: emptyList()
                        _regionInfoList.value = currentRegions.map { regionName ->
                            RegionInfo(name = regionName, policyCount = groupedData[regionName]?.size ?: 0)
                        }

                        // 추천/인기 상품 상태 업데이트 (예시)
                        val successPlaceholder = LocalWelfareServiceDto("SUCCESS", "0개 상품", "", null, null, null, null)
                        _recommendList.value = listOf(successPlaceholder, successPlaceholder, successPlaceholder)
                        _popularList.value = listOf(successPlaceholder, successPlaceholder, successPlaceholder)

                        Log.d("MainViewModel", "성공: 데이터를 지역별로 그룹핑하고 개수를 업데이트했습니다.")
                    }
                } else {
                    onFailure(call, Throwable("Server error with code: ${response.code()}"))
                }
            }

            override fun onFailure(call: Call<List<LocalWelfareServiceDto>>, t: Throwable) {
                // 실패 시, 모든 목록을 '연결 실패' 상태로 업데이트
                val errorPlaceholder = LocalWelfareServiceDto("ERROR", "연결 실패", "", null, null, null, null)
                _recommendList.value = listOf(errorPlaceholder, errorPlaceholder, errorPlaceholder)
                _popularList.value = listOf(errorPlaceholder, errorPlaceholder, errorPlaceholder)

                val currentRegions = _regionInfoList.value?.map { it.name } ?: emptyList()
                _regionInfoList.value = currentRegions.map { RegionInfo(name = it, policyCount = -1) }

                Log.e("MainViewModel", "실패: ${t.message}")
            }
        })
    }
}
