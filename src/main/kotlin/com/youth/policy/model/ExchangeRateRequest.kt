package com.youth.policy.model

data class ExchangeRateRequest(
    val searchDate: String? = null,
    val data: String = "AP01"  // 기본값: 환율
)
