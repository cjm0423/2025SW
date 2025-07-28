package com.youth.policy.model

data class LocalWelfareListRequest(
    val sigunguCd: String,
    val pageNo: Int = 1,
    val numOfRows: Int = 10
)
