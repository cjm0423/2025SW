package com.youth.policy.client

import com.youth.policy.model.StatisticTableListRequest
import com.youth.policy.model.StatisticTableListResponse
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.http.client.BufferingClientHttpRequestFactory
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class StatisticTableListClient(
    @Value("\${statistics.api.url}")
    private val apiUrl: String
) {
    // BufferingClientHttpRequestFactory로만 버퍼링 처리
    private val restTemplate: RestTemplate =
        RestTemplate(BufferingClientHttpRequestFactory(SimpleClientHttpRequestFactory()))

    private val xmlMapper = XmlMapper()

    fun getStatisticTableList(request: StatisticTableListRequest): StatisticTableListResponse {
        val uri = buildUri(request)

        // 응답을 String으로 받고, 빈 값이면 바로 예외
        val xmlResponse = restTemplate.getForObject(uri, String::class.java)
            ?: throw RuntimeException("통계청 API 호출 실패: null 반환")
        if (xmlResponse.isBlank()) {
            throw RuntimeException("통계청 API 응답이 비어 있습니다. URI=$uri")
        }

        // 디버깅용 콘솔 출력
        println("▶▶ xmlResponse = $xmlResponse")

        return xmlMapper.readValue(xmlResponse, StatisticTableListResponse::class.java)
    }

    private fun buildUri(request: StatisticTableListRequest): URI =
        UriComponentsBuilder
            .fromHttpUrl(apiUrl)
            .pathSegment(
                request.serviceName,
                request.authkey,
                request.format,
                request.lang,
                request.startCount.toString(),
                request.endCount.toString(),
                request.statisticCode
            )
            .build()
            .toUri()
}
