package com.youth.policy.client

import com.youth.policy.model.ExchangeRateRequest
import com.youth.policy.model.ExchangeRateResponse
import com.youth.policy.model.ExchangeRateResponseItem
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class ExchangeRateClient(
    private val restTemplate: RestTemplate,
    @Value("\${exchange.api.url}") private val apiUrl: String,
    @Value("\${exchange.api.key}") private val apiKey: String
) {

    fun getExchangeRates(request: ExchangeRateRequest): ExchangeRateResponse {
        val uri = buildUri(request)
        return restTemplate
            .getForObject(uri, Array<ExchangeRateResponseItem>::class.java)
            ?.toList()
            ?: throw RuntimeException("환율 API 호출 실패")
    }

    private fun buildUri(request: ExchangeRateRequest): URI {
        val builder = UriComponentsBuilder
            .fromHttpUrl(apiUrl)
            .queryParam("authkey", apiKey)
            .queryParam("data", request.data)

        request.searchDate?.let {
            builder.queryParam("searchdate", it)
        }

        return builder.build().toUri()
    }
}