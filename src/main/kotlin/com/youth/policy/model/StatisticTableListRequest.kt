package com.youth.policy.model

data class StatisticTableListRequest(
    val serviceName: String = "StatisticTableList",
    val authkey: String,  // 수정: null 제거하고 필수값으로 변경
    val format: String = "xml",
    val lang: String = "kr",
    val startCount: Int = 1,
    val endCount: Int = 10,
    val statisticCode: String = "102Y004"
)