package com.example.exitsw.util

object ProvinceMapper {
    // UI 표기 → API에서 요구하는 표준 광역명
    private val labelToProvince = mapOf(
        "서울시"   to "서울특별시",
        "경기도"   to "경기도",
        "강원도"   to "강원특별자치도",
        "충청북도" to "충청북도",
        "충청남도" to "충청남도",
        "전라북도" to "전북특별자치도",
        "전라남도" to "전라남도",
        "경상북도" to "경상북도",
        "경상남도" to "경상남도",
        "제주도"   to "제주특별자치도"
    )

    /** UI 라벨을 API용 광역명으로 변환 (없으면 null) */
    fun toProvinceName(uiLabel: String?): String? =
        uiLabel?.trim()?.let { labelToProvince[it] }
}
