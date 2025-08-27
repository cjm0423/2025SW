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

    /**
     * 코루틴을 사용하여 API로부터 지역 복지 데이터를 가져옵니다.
     */
    private fun fetchLocalWelfareData() {
        // viewModelScope.launch를 사용해 코루틴 컨텍스트에서 API를 호출합니다.
        viewModelScope.launch {
            try {
                // 1. API 호출 (suspend 함수는 코루틴 내에서 직접 호출)
                val response = RetrofitClient.getInstance(getApplication()).getLocalWelfareList(sigunguCd = "")

                // 2. 응답 객체(LocalWelfareListResponse)에서 실제 데이터 리스트(servList)를 추출합니다.
                val policyList = response.servList

                // 3. 데이터를 지역별로 그룹핑합니다.
                val groupedData = policyList.groupBy { normalizeRegion(it.region) }
                _groupedWelfareData.value = groupedData // LiveData 업데이트 (메인 스레드이므로 .value 사용)

                // 4. API 성공 시, 정책 개수를 포함한 RegionInfo 리스트로 갱신합니다.
                val currentRegions = _regionInfoList.value?.map { it.name } ?: emptyList()
                _regionInfoList.value = currentRegions.map { regionName ->
                    RegionInfo(name = regionName, policyCount = groupedData[regionName]?.size ?: 0)
                }

                // 5. 추천/인기 상품 상태를 성공 상태로 업데이트합니다. (예시)
                //    실제로는 policyList에서 데이터를 가공하여 채워야 합니다.
                _recommendList.value = policyList.take(3) // 예시: 받아온 리스트의 앞 3개
                _popularList.value = policyList.shuffled().take(3) // 예시: 받아온 리스트를 섞어서 3개

                Log.d("MainViewModel", "성공: ${policyList.size}개의 데이터를 가져왔습니다.")

            } catch (e: Exception) {
                // 6. API 호출 실패 또는 예외 발생 시 처리
                Log.e("MainViewModel", "데이터 로딩 실패: ${e.message}")

                val errorPlaceholder = LocalWelfareServiceDto("ERROR", "연결 실패", e.message, null, null, null, null)
                _recommendList.value = listOf(errorPlaceholder, errorPlaceholder, errorPlaceholder)
                _popularList.value = listOf(errorPlaceholder, errorPlaceholder, errorPlaceholder)

                val currentRegions = _regionInfoList.value?.map { it.name } ?: emptyList()
                _regionInfoList.value = currentRegions.map { RegionInfo(name = it, policyCount = -1) } // -1을 실패 코드로 사용
            }
        }
    }
}
