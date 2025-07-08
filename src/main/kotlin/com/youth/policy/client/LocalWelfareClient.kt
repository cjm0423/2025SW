package com.youth.policy.client

import com.youth.policy.model.LocalWelfareDetailRequest
import com.youth.policy.model.LocalWelfareDetailResponse
import com.youth.policy.model.LocalWelfareListRequest
import com.youth.policy.model.LocalWelfareListResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.MediaType
import org.springframework.http.client.ClientHttpResponse
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter
import org.springframework.stereotype.Component
import org.springframework.web.client.ResponseErrorHandler
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.time.Duration
import java.util.Optional

@Component
open class LocalWelfareClient(
    @Value("\${local.welfare.api.list.url}")   private val listApiUrl: String,
    @Value("\${local.welfare.api.detail.url}") private val detailApiUrl: String,
    @Value("\${local.welfare.api.key}")        private val serviceKey: String
) {
    private val log = LoggerFactory.getLogger(LocalWelfareClient::class.java)

    // RestTemplate 에 XML 컨버터 등록 (text/xml 도 포함)
    private val rest: RestTemplate = RestTemplateBuilder()
        .setConnectTimeout(Duration.ofSeconds(5))
        .setReadTimeout(Duration.ofSeconds(5))
        .additionalMessageConverters(
            MappingJackson2XmlHttpMessageConverter().apply {
                // 기본 APPLICATION_XML 외에 TEXT_XML 응답도 처리하도록 추가
                supportedMediaTypes = listOf(MediaType.APPLICATION_XML, MediaType.TEXT_XML)
            }
        )
        .errorHandler(object : ResponseErrorHandler {
            override fun hasError(response: ClientHttpResponse) = false
            override fun handleError(response: ClientHttpResponse) { /* no-op */ }
        })
        .build()

    open fun getLocalWelfareList(request: LocalWelfareListRequest): LocalWelfareListResponse {
        val uri = UriComponentsBuilder
            .fromHttpUrl(listApiUrl)
            .queryParam("serviceKey", serviceKey)
            .queryParam("pageNo", request.pageNo)
            .queryParam("numOfRows", request.numOfRows)
            .queryParamIfPresent("lifeArray",         Optional.ofNullable(request.lifeArray))
            .queryParamIfPresent("trgterIndvdlArray", Optional.ofNullable(request.trgterIndvdlArray))
            .queryParamIfPresent("intrsThemaArray",   Optional.ofNullable(request.intrsThemaArray))
            .queryParamIfPresent("srchKeyCode",       Optional.ofNullable(request.srchKeyCode))
            .queryParamIfPresent("searchWrd",         Optional.ofNullable(request.searchWrd))
            .queryParamIfPresent("arrgOrd",           Optional.ofNullable(request.arrgOrd))
            .queryParam("ctpvNm", request.ctpvNm)
            .queryParamIfPresent("sggNm", Optional.ofNullable(request.sggNm))
            .build()
            .toUri()

        log.info("📡 지자체 복지 목록 API 호출: {}", uri)
        return rest.getForObject(uri, LocalWelfareListResponse::class.java)
            ?: throw IllegalStateException("지자체 목록 응답이 비었습니다")
    }

    open fun getLocalWelfareDetail(request: LocalWelfareDetailRequest): LocalWelfareDetailResponse {
        val uri = UriComponentsBuilder
            .fromHttpUrl(detailApiUrl)
            .queryParam("serviceKey", serviceKey)
            .queryParam("servId", request.servId)
            .build()
            .toUri()

        log.info("📡 지자체 복지 상세 API 호출: {}", uri)
        return rest.getForObject(uri, LocalWelfareDetailResponse::class.java)
            ?: throw IllegalStateException("지자체 상세 응답이 비었습니다")
    }
}
