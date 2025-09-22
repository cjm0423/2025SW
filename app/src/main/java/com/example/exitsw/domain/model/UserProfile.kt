package com.example.exitsw.domain.model

/**
 * RecommendationEngine 에서 참조하는 최소 필드만 정의.
 * - regionLabel: 가입 화면의 10개 라벨(서울시/경기도/...) 중 하나
 * - income: "0~30%", "30~50%", "중위소득 50% 이하" 등 사용자 입력 라벨
 */
data class UserProfile(
    val uid: String,
    val nickname: String = "",
    val gender: String = "",
    val ageYears: Int = 0,
    val regionLabel: String = "전국",
    val income: String = "",
    val interest: String = ""
)
