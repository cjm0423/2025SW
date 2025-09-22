package com.example.exitsw.main

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.exitsw.data.LocalWelfareServiceDto
import com.example.exitsw.data.Policy
import com.example.exitsw.data.RegionInfo
import com.example.exitsw.data.UserProfileDataSource
import com.example.exitsw.data.toDomainProvinceOnly
import com.example.exitsw.domain.RecommendationEngine
import com.example.exitsw.domain.model.UserProfile
import com.example.exitsw.repository.FirebaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val LOG_RECO = true // 관심사 매칭/점수 디버그 로그 토글
    }

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

    // ---- 내부 상태 ----
    private val firebaseRepository = FirebaseRepository()
    private val userProfileDs = UserProfileDataSource() // Firestore에서 프로필 읽기
    private var cachedUserProfile: UserProfile? = null
    private var cachedAllPolicies: List<LocalWelfareServiceDto> = emptyList()

    init {
        setupPlaceholders()
        fetchPopularWelfareData()      // 인기
        fetchLocalWelfareData()        // 지역/정책 묶음
        fetchUserProfileAndRecommend() // 프로필 + 추천
    }

    /* ================= 인기 ================= */
    private fun fetchPopularWelfareData() {
        viewModelScope.launch {
            try {
                val popular = firebaseRepository.getPopularWelfareServices(limit = 30)
                _popularList.value = popular
                popular.forEach { item ->
                    Log.d("MainViewModel", "[인기상품] ${item.servNm} / ${item.intrsThemaNmArray}")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "인기상품 로딩 실패: ${e.message}")
            }
        }
    }

    /* ============== 플레이스홀더 ============== */
    private fun setupPlaceholders() {
        val staticRegions = listOf(
            "서울시", "경기도", "강원도", "충청북도", "충청남도",
            "전라북도", "전라남도", "경상북도", "경상남도", "제주도"
        )

        _homeRegionList.value = staticRegions.map { regionName ->
            LocalWelfareServiceDto(
                servId = regionName,
                servNm = regionName,
                bizChrDeptNm = "정책 목록 보기"
            )
        }
        _regionInfoList.value = staticRegions.map { RegionInfo(name = it, policyCount = null) }

        val placeholder = LocalWelfareServiceDto(
            servId = "Loading",
            servNm = "로딩중..",
            bizChrDeptNm = "로딩중.."
        )
        _recommendList.value = listOf(placeholder, placeholder, placeholder)
        _popularList.value = listOf(placeholder, placeholder, placeholder)
    }

    /* ============== 지역 이름 정규화 ============== */
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

    /* ============== 지역별 정책/카운트 로드 ============== */
    private fun fetchLocalWelfareData() {
        viewModelScope.launch {
            try {
                val policyList = firebaseRepository.getAllWelfareServices()
                cachedAllPolicies = policyList // 추천 재계산에 사용

                val groupedData = policyList.groupBy { normalizeRegion(it.ctpvNm) }
                _groupedWelfareData.value = groupedData

                val currentRegions = _regionInfoList.value?.map { it.name } ?: emptyList()
                _regionInfoList.value = currentRegions.map { regionName ->
                    RegionInfo(name = regionName, policyCount = groupedData[regionName]?.size ?: 0)
                }

                Log.d("MainViewModel", "성공 (지역 정책): ${policyList.size}건 Firestore에서 수신")

                recalcRecommendationsIfReady()

            } catch (e: Exception) {
                Log.e("MainViewModel", "지역 정책 로딩 실패: ${e.message}")
                val currentRegions = _regionInfoList.value?.map { it.name } ?: emptyList()
                _regionInfoList.value = currentRegions.map { RegionInfo(name = it, policyCount = -1) }
            }
        }
    }

    /* ============== 프로필 + 추천 계산 ============== */
    private fun fetchUserProfileAndRecommend() {
        viewModelScope.launch {
            val uid = readUidFromPrefs()
            if (uid == null) {
                Log.w("MainViewModel", "UID 없음: 추천 불가")
                return@launch
            }
            try {
                val profile = withContext(Dispatchers.IO) {
                    userProfileDs.getUserProfile(uid)
                }
                if (profile == null) {
                    Log.w("MainViewModel", "프로필 미존재: 추천 불가")
                    return@launch
                }
                cachedUserProfile = profile
                Log.d(
                    "MainViewModel",
                    "프로필 로드: region=${profile.regionLabel}, age=${profile.ageYears}, income=${profile.income}, interest=${profile.interest}"
                )

                recalcRecommendationsIfReady()

            } catch (e: Exception) {
                Log.e("MainViewModel", "프로필 로드 실패: ${e.message}")
            }
        }
    }

    /** ✅ 외부(프래그먼트/액티비티)에서 호출 가능: 프로필 수정 후 재계산 트리거 */
    fun refreshRecommendations(forceReloadPolicies: Boolean = false) {
        if (forceReloadPolicies || cachedAllPolicies.isEmpty()) {
            fetchLocalWelfareData()        // 정책 목록이 없으면 다시 로드
        }
        fetchUserProfileAndRecommend()     // 항상 최신 프로필로 재계산
    }

    private fun readUidFromPrefs(): String? {
        val sp = getApplication<Application>()
            .getSharedPreferences("auth", Context.MODE_PRIVATE)

        // 1) SharedPreferences
        sp.getString("uid", null)?.let { if (it.isNotBlank()) return it }

        // 2) FirebaseAuth (로그인 상태 안전망)
        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            ?.let { if (it.isNotBlank()) return it }

        // 3) 못 찾으면 null (UserProfileDataSource에서 auth_uid 조회까지 시도)
        return null
    }

    /** 프로필과 정책이 모두 준비된 뒤에만 추천 계산 */
    private fun recalcRecommendationsIfReady() {
        val user = cachedUserProfile ?: return
        if (cachedAllPolicies.isEmpty()) return

        viewModelScope.launch(Dispatchers.Default) {
            try {
                val scored: List<Pair<LocalWelfareServiceDto, Double>> =
                    cachedAllPolicies.map { dto ->
                        val p: Policy = dto.toDomainProvinceOnly() // 광역만(region=ctpvNm)
                        val base = RecommendationEngine.score(p, user)
                        val boost = interestMatchBoost(dto, user.interest)
                        val total = base + boost                    // ✅ 보너스 합산

                        if (LOG_RECO) {
                            Log.d(
                                "Reco",
                                "score base=$base boost=$boost total=$total title='${dto.servNm}' interest='${user.interest}'"
                            )
                        }
                        dto to total
                    }.sortedByDescending { it.second }

                val topRecommended = scored.take(15).map { it.first }
                _recommendList.postValue(topRecommended)

                if (LOG_RECO) {
                    val topLog = scored.take(10)
                        .joinToString("\n") { (d, s) -> "  ${"%.2f".format(s)}  ${d.servNm}" }
                    Log.d("Reco", "TOP after interest='${user.interest}':\n$topLog")
                }

                Log.d("MainViewModel", "추천 리스트 계산 완료: top=${topRecommended.size}")
            } catch (e: Exception) {
                Log.e("MainViewModel", "추천 계산 실패: ${e.message}")
            }
        }
    }

    private fun interestMatchBoost(
        dto: LocalWelfareServiceDto,
        userInterestRaw: String?
    ): Double {
        val interest = userInterestRaw?.trim().orEmpty()
        if (interest.isEmpty()) return 0.0

        // 딕셔너리 키/검색어 모두 소문자 정규화
        val dictOriginal: Map<String, List<String>> = mapOf(
            "주거" to listOf("주거","전월세","보증금","임차","임대","청년주거","주택","원룸","월세","전세"),
            "일자리" to listOf("일자리","고용","취업","근로","자활","창업","창직","인턴","채용"),
            "의료·건강" to listOf("의료","건강","보건","진료","치료","검진","의약","병원"),
            "생활지원" to listOf("생활","생계","바우처","지원금","복지","돌봄","교육비","교통"),
            "교육" to listOf("교육","훈련","학습","장학","대학","자격","교육비"),
            "금융" to listOf("금융","대출","이자","보증","신용"),
            "문화" to listOf("문화","체육","여가","관광","공연","전시"),
            "출산·양육" to listOf("출산","양육","보육","아이","장려금","아동","육아")
        )
        val dict = dictOriginal
            .mapKeys { it.key.lowercase() }
            .mapValues { (_, v) -> v.map { it.lowercase() } }

        // DTO 텍스트 풀 (소문자)
        val haystack = buildString {
            append(dto.servNm ?: ""); append(' ')
            append(dto.servDgst ?: ""); append(' ')
            dto.getIntrsThemaNmList().forEach { append(it); append(' ') }
            dto.getLifeNmList().forEach { append(it); append(' ') }
            dto.getTrgterIndvdlNmList().forEach { append(it); append(' ') }
        }.lowercase()

        val interestLc = interest.lowercase()

        // 1) 라벨 그대로 포함되면 보너스
        if (haystack.contains(interestLc)) {
            if (LOG_RECO) Log.d("Reco", "Direct interest hit: '$interest' in '${dto.servNm}'")
            return 1.5
        }

        // 2) 딕셔너리 기반 매칭
        val dictKeywords = dict[interestLc].orEmpty()
        if (dictKeywords.any { haystack.contains(it) }) {
            if (LOG_RECO) Log.d("Reco", "Dict interest hit: '$interest' -> $dictKeywords in '${dto.servNm}'")
            return 1.5
        }

        // 3) 토큰 분해 후 일부라도 매칭
        val tokens = interestLc.split("·", "/", " ", ",").map { it.trim() }.filter { it.isNotEmpty() }
        if (tokens.any { haystack.contains(it) }) {
            if (LOG_RECO) Log.d("Reco", "Token interest hit: tokens=$tokens in '${dto.servNm}'")
            return 1.0
        }

        if (LOG_RECO) Log.d("Reco", "No interest hit: '$interest' in '${dto.servNm}'")
        return 0.0
    }
}
