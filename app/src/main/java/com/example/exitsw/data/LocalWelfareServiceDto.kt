package com.example.exitsw.data

data class LocalWelfareServiceDto(
    // ✨ [변경] null 값을 받을 수 있도록 String?으로 변경
    val serviceId: String?,
    val serviceName: String?,

    // 나머지 필드는 이미 nullable이므로 그대로 유지
    val department: String?,
    val summary: String?,
    val region: String?,
    val city: String?,
    val detailLink: String?
)
