package com.example.exitsw.data

/**
 * 지역 목록 화면의 각 아이템에 대한 정보를 담는 클래스
 * @param name 지역 이름 (예: "서울시")
 * @param policyCount 정책 개수. null이면 '확인 중', -1이면 '연결 실패', 0 이상이면 실제 개수.
 */
data class RegionInfo(
    val name: String,
    val policyCount: Int? = null
)
