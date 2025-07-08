package com.youth.policy.service

import com.youth.policy.client.ExchangeRateClient
import com.youth.policy.model.ExchangeRateRequest
import com.youth.policy.model.ExchangeRateResponse
import org.springframework.stereotype.Service

@Service
class ExchangeRateService(
    private val exchangeRateClient: ExchangeRateClient
) {

    fun getExchangeRates(
        searchDate: String? = null,
        data: String = "AP01"
    ): ExchangeRateResponse {
        val request = ExchangeRateRequest(
            searchDate = searchDate,
            data = data
        )

        return exchangeRateClient.getExchangeRates(request)
    }
}
