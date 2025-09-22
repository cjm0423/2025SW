package com.example.exitsw.network

import com.example.exitsw.data.LocalWelfareListResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface WelfareApiService {

    @GET("/api/welfare/local/services")
    suspend fun getLocalWelfareList(
        @Query("sigunguCd") sigunguCd: String,
        @Query("pageNo") pageNo: Int = 1,
        @Query("numOfRows") numOfRows: Int = 10
    ): LocalWelfareListResponse

    // (신규) 광역명 기반
    @GET("/api/welfare/local/services")
    suspend fun getProvinceWelfareList(
        @Query("ctpvNm") ctpvNm: String,            // 예: "경상북도", "서울특별시"
        @Query("pageNo") pageNo: Int = 1,
        @Query("numOfRows") numOfRows: Int = 200    // 넉넉히 받아서 점수화
    ): LocalWelfareListResponse

}