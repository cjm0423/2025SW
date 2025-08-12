package com.youth.policy.model

data class LocalWelfareJsonResponse(
    val serviceId: String,
    val serviceName: String,
    val department: String?,
    val summary: String?,
    val region: String?,
    val city: String?,
    val detailLink: String?
)