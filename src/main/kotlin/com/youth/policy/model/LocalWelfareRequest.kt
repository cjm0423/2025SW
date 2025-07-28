package com.youth.policy.model

/**
 * 지자체 복지 서비스 목록 조회 요청
 */
data class LocalWelfareListRequest(
    val pageNo: Int,
    val numOfRows: Int,
    val lifeArray: String?,
    val trgterIndvdlArray: String?,
    val intrsThemaArray: String?,
    val srchKeyCode: String?,
    val searchWrd: String?,
    val arrgOrd: String?,
    val ctpvNm: String?,
    val sggNm: String?
)

/**
 * 지자체 복지 서비스 상세 조회 요청
 */
data class LocalWelfareDetailRequest(
    val servId: String
)
