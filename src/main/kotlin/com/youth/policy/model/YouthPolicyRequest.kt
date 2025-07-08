package com.youth.policy.model

data class YouthPolicyRequest(
    val apiKeyNm: String,
    val pageNum: Int? = null,
    val pageSize: Int? = null,
    val pageType: String? = null,
    val plcyNo: String? = null,
    val rtnType: String? = null,
    val plcyKywdNm: String? = null,
    val plcyExplnCn: String? = null,
    val plcyNm: String? = null,
    val zipCd: String? = null,
    val lclsfNm: String? = null,
    val mclsfNm: String? = null
)