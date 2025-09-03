package com.example.exitsw.data

import com.google.gson.annotations.SerializedName

data class LocalWelfareListResponse(
    @SerializedName("totalCount")
    val totalCount: Int,

    @SerializedName("pageNo")
    val pageNo: Int,

    @SerializedName("numOfRows")
    val numOfRows: Int,

    @SerializedName("servList")
    val servList: List<LocalWelfareServiceDto>
)