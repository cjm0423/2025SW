package com.example.exitsw.data

data class LocalWelfareServiceDto(
    val serviceId: String,
    val serviceName: String,
    val department: String?,
    val summary: String?,
    val region: String?,
    val city: String?,
    val detailLink: String?
)