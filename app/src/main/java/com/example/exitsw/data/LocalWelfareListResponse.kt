package com.example.exitsw.data

data class LocalWelfareListResponse(
    val totalCount: Int,
    val pageNo: Int,
    val numOfRows: Int,
    val servList: List<LocalWelfareServiceDto>
)