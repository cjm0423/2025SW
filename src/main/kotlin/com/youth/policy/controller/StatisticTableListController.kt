package com.youth.policy.controller

import com.youth.policy.model.StatisticTableListResponse
import com.youth.policy.service.StatisticTableListService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/statistics")
class StatisticTableListController(
    private val statisticTableListService: StatisticTableListService
) {

    @GetMapping
    fun getStatisticTableList(
        @RequestParam(required = false, defaultValue = "StatisticTableList") serviceName: String,
        @RequestParam(required = false, defaultValue = "xml") format: String,
        @RequestParam(required = false, defaultValue = "kr") lang: String,
        @RequestParam(required = false, defaultValue = "1") startCount: Int,
        @RequestParam(required = false, defaultValue = "10") endCount: Int,
        @RequestParam(required = false, defaultValue = "102Y004") statisticCode: String
    ): StatisticTableListResponse {
        return statisticTableListService.getStatisticTableList(
            serviceName = serviceName,
            format = format,
            lang = lang,
            startCount = startCount,
            endCount = endCount,
            statisticCode = statisticCode
        )
    }
}
