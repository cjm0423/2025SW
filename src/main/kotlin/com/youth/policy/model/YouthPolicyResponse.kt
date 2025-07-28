package com.youth.policy.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class YouthPolicyResponse(
    val resultCode: Int,
    val resultMessage: String,
    val result: Result
)

data class Result(
    val pagging: Pagging,
    val youthPolicyList: List<YouthPolicy>
)

data class Pagging(
    val totCount: Int,
    val pageNum: Int,
    val pageSize: Int
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class YouthPolicy(
    val plcyNo: String? = null,
    val plcyNm: String? = null,
    val plcyKywdNm: String? = null,
    val plcyExplnCn: String? = null,
    val lclsfNm: String? = null,
    val mclsfNm: String? = null,
    val sprvsnInstCdNm: String? = null,
    val bizPrdBgngYmd: String? = null,
    val bizPrdEndYmd: String? = null,
    val aplyYmd: String? = null,
    val aplyUrlAddr: String? = null,
    val plcySprtCn: String? = null,
    val sprtTrgtMinAge: String? = null,
    val sprtTrgtMaxAge: String? = null,
    val earnEtcCn: String? = null,
    val refUrlAddr1: String? = null,
    val sprtTrgtAgeLmtYn: String? = null,
    // 필요한 항목을 추가로 포함할 수 있습니다
)