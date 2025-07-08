package com.youth.policy.controller

import com.youth.policy.model.ExchangeRateResponse
import com.youth.policy.service.ExchangeRateService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/exchange-rates")
class ExchangeRateController(
    private val exchangeRateService: ExchangeRateService
) {

    @GetMapping
    fun getExchangeRates(
        @RequestParam(required = false) searchDate: String?,
        @RequestParam(required = false, defaultValue = "AP01") data: String
    ): ExchangeRateResponse {
        return exchangeRateService.getExchangeRates(
            searchDate = searchDate,
            data = data
        )
    }
}