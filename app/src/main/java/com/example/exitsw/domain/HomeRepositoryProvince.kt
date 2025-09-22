package com.example.exitsw.domain

import com.example.exitsw.data.LocalWelfareListResponse
import com.example.exitsw.data.toDomainListProvinceOnly
import com.example.exitsw.data.Policy
import com.example.exitsw.data.UserProfileDataSource // 패키지 맞춰 수정
import com.example.exitsw.network.WelfareApiService
import com.example.exitsw.util.ProvinceMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HomeRepositoryProvince(
    private val userDs: UserProfileDataSource,
    private val api: WelfareApiService
) {
    suspend fun loadHome(uid: String): Triple<List<Policy>, List<Policy>, List<Policy>> =
        withContext(Dispatchers.IO) {
            val user = userDs.getUserProfile(uid)
                ?: return@withContext Triple(emptyList(), emptyList(), emptyList())

            val province = ProvinceMapper.toProvinceName(user.regionLabel)
                ?: return@withContext Triple(emptyList(), emptyList(), emptyList())

            val resp: LocalWelfareListResponse = api.getProvinceWelfareList(
                ctpvNm = province, pageNo = 1, numOfRows = 200
            )

            val all: List<Policy> = resp.toDomainListProvinceOnly()

            // 점수화 → 추천 상위 N
            val scored = all.map { it to RecommendationEngine.score(it, user) }
                .sortedByDescending { it.second }

            val recommendTop = scored.take(15).map { it.first }
            val popularTop   = all.sortedByDescending { it.incomeBracket } // 간단 대용(조회수 없으니 임시 지표로 바꿔도 됨)
            val regionTop    = all.take(15)

            Triple(recommendTop, popularTop.take(15), regionTop)
        }
}
