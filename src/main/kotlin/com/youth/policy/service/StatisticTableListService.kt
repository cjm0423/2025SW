package com.youth.policy.service

import com.youth.policy.client.StatisticTableListClient
import com.youth.policy.model.StatisticTableListRequest
import com.youth.policy.model.StatisticTableListResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class StatisticTableListService(
    private val statisticTableListClient: StatisticTableListClient,
    @Value("\${statistics.api.key}") private val apiKey: String  // 수정: API 키 주입 추가
) {

    fun getStatisticTableList(
        serviceName: String = "StatisticTableList",
        format: String = "xml",
        lang: String = "kr",
        startCount: Int = 1,
        endCount: Int = 10,
        statisticCode: String = "102Y004"
    ): StatisticTableListResponse {
        val request = StatisticTableListRequest(
            serviceName = serviceName,
            authkey = apiKey,  // 수정: API 키 설정
            format = format,
            lang = lang,
            startCount = startCount,
            endCount = endCount,
            statisticCode = statisticCode
        )

        return statisticTableListClient.getStatisticTableList(request)
    }
}