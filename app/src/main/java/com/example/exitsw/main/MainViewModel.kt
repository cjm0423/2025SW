package com.example.exitsw.main

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.exitsw.data.LocalWelfareServiceDto
import com.example.exitsw.data.RegionInfo
import com.example.exitsw.network.RetrofitClient
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _recommendList = MutableLiveData<List<LocalWelfareServiceDto>>()
    val recommendList: LiveData<List<LocalWelfareServiceDto>> get() = _recommendList

    private val _popularList = MutableLiveData<List<LocalWelfareServiceDto>>()
    val popularList: LiveData<List<LocalWelfareServiceDto>> get() = _popularList

    private val _homeRegionList = MutableLiveData<List<LocalWelfareServiceDto>>()
    val homeRegionList: LiveData<List<LocalWelfareServiceDto>> get() = _homeRegionList

    private val _groupedWelfareData = MutableLiveData<Map<String, List<LocalWelfareServiceDto>>>()
    val groupedWelfareData: LiveData<Map<String, List<LocalWelfareServiceDto>>> get() = _groupedWelfareData

    private val _regionInfoList = MutableLiveData<List<RegionInfo>>()
    val regionInfoList: LiveData<List<RegionInfo>> get() = _regionInfoList


    init {
        loadStaticData()
        fetchLocalWelfareData()
    }

    private fun loadStaticData() {
        val staticRegions = listOf(
            "서울시", "경기도", "강원도", "충청북도", "충청남도",
            "전라북도", "전라남도", "경상북도", "경상남도", "제주도"
        )
        _homeRegionList.value = staticRegions.map { regionName ->
            LocalWelfareServiceDto(
                serviceId = regionName,
                serviceName = regionName,
                department = "정책 목록 보기",
                summary = null, region = regionName, city = null, detailLink = null
            )
        }
        _regionInfoList.value = staticRegions.map { RegionInfo(name = it, policyCount = null) }

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
        viewModelScope.launch {
            try {
                // sigunguCd에 빈 값을 넣어 전국 데이터를 한번에 호출합니다.
                val response = RetrofitClient.getInstance(getApplication()).getLocalWelfareList(
                    sigunguCd = "",
                    numOfRows = 100 // 예: 100개 데이터 요청
                )

                val policyList = response.servList

                val groupedData = policyList.groupBy { normalizeRegion(it.region) }
                _groupedWelfareData.value = groupedData

                val currentRegions = _regionInfoList.value?.map { it.name } ?: emptyList()
                _regionInfoList.value = currentRegions.map { regionName ->
                    RegionInfo(name = regionName, policyCount = groupedData[regionName]?.size ?: 0)
                }

                _recommendList.value = policyList.take(3)
                _popularList.value = policyList.shuffled().take(3)

                Log.d("MainViewModel", "성공: ${policyList.size}개의 데이터를 가져왔습니다.")

            } catch (e: Exception) {
                Log.e("MainViewModel", "데이터 로딩 실패: ${e.message}")

                val errorPlaceholder = LocalWelfareServiceDto("ERROR", "연결 실패", e.message, null, null, null, null)
                _recommendList.value = listOf(errorPlaceholder, errorPlaceholder, errorPlaceholder)
                _popularList.value = listOf(errorPlaceholder, errorPlaceholder, errorPlaceholder)

                val currentRegions = _regionInfoList.value?.map { it.name } ?: emptyList()
                _regionInfoList.value = currentRegions.map { RegionInfo(name = it, policyCount = -1) }
            }
        }
    }
}