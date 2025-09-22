package com.example.exitsw.mypage

data class FavoritePolicy(
    val id: String,            // servId
    val title: String,         // servNm
    val department: String?,   // bizChrDeptNm
    val summary: String?,      // servDgst
    val region: String?,       // ctpvNm
    val city: String?,         // sggNm
    val detailLink: String?    // servDtlLink
)
