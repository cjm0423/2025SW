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

}