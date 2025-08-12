package com.example.exitsw.network

import com.example.exitsw.data.LocalWelfareServiceDto
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface WelfareApiService {
    // GET 방식으로 "/api/welfare/local/services/json" 주소에 요청을 보냅니다.
    @GET("/api/welfare/local/services/json")
    fun getLocalWelfareList(
        // 요청에 필요한 파라미터들을 @Query 어노테이션으로 정의합니다.
        @Query("sigunguCd") sigunguCd: String,
        @Query("pageNo") pageNo: Int = 1,
        @Query("numOfRows") numOfRows: Int = 10
    ): Call<List<LocalWelfareServiceDto>> // API 응답은 LocalWelfareServiceDto 객체의 리스트 형태입니다.
}