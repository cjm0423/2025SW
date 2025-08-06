package com.youth.policy.model

data class LocalWelfareListRequest(
    val sigunguCd: String,
    val pageNo: Int = 1,
    val numOfRows: Int = 10
)

data class LocalWelfareDetailRequest(
    val sigunguCd: String,
    val servId: String
)
