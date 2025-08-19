package com.youth.policy.client

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.youth.policy.exception.QuotaExceededException
import com.youth.policy.model.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Component
class LocalWelfareClient(
    private val restTemplate: RestTemplate,
    private val xmlMapper: XmlMapper,
    @Value("\${welfare.api.local.list.url}") private val listApiUrl: String,
    @Value("\${welfare.api.local.detail.url}") private val detailApiUrl: String,
    @Value("\${welfare.api.local.key}") private val serviceKey: String
) {

    fun getLocalWelfareList(req: LocalWelfareListRequest): LocalWelfareListResponse {
        val uri = buildListUri(req)
        val rawXml = restTemplate.getForObject(uri, String::class.java)

        if (rawXml.isNullOrBlank()) {
            println("Warning: API for sigunguCd ${req.sigunguCd} returned an empty response.")
            return LocalWelfareListResponse(
                servList = emptyList(),
                pageNo = req.pageNo,
                totalCount = 0,
                numOfRows = req.numOfRows,
                resultCode = "00",
                resultMessage = "NORMAL SERVICE."
            )
        }

        if (rawXml.contains("<cmmMsgHeader>")) {
            val errorResponse = xmlMapper.readValue(rawXml, WelfareErrorResponse::class.java)
            val errorCode = errorResponse?.cmmMsgHeader?.returnReasonCode
            val errorMessage = errorResponse?.cmmMsgHeader?.returnAuthMsg

            if (errorCode == "22") {
                throw QuotaExceededException("API 일일 호출 허용량을 초과했습니다.")
            }
            throw RuntimeException("지자체 목록 API 오류: $errorMessage (코드: $errorCode)")
        }

        return xmlMapper.readValue(rawXml, LocalWelfareListResponse::class.java)
    }

    fun getLocalWelfareDetail(req: LocalWelfareDetailRequest): LocalWelfareDetailResponse {
        val uri = buildDetailUri(req)
        val rawXml = restTemplate.getForObject(uri, String::class.java)

        if (rawXml.isNullOrBlank()) {
            println("Warning: API for servId ${req.servId} returned an empty response.")
            throw RuntimeException("지자체 상세 API가 빈 응답을 반환했습니다: servId=${req.servId}")
        }

        if (rawXml.contains("<cmmMsgHeader>")) {
            val errorResponse = xmlMapper.readValue(rawXml, WelfareErrorResponse::class.java)
            val errorCode = errorResponse?.cmmMsgHeader?.returnReasonCode
            val errorMessage = errorResponse?.cmmMsgHeader?.returnAuthMsg

            if (errorCode == "22") {
                throw QuotaExceededException("API 일일 호출 허용량을 초과했습니다.")
            }
            throw RuntimeException("지자체 상세 API 오류: $errorMessage (코드: $errorCode)")
        }

        return xmlMapper.readValue(rawXml, LocalWelfareDetailResponse::class.java)
    }

    private fun buildListUri(req: LocalWelfareListRequest): URI {
        val key = URLEncoder.encode(serviceKey, StandardCharsets.UTF_8.toString())
        return UriComponentsBuilder
            .fromHttpUrl(listApiUrl)
            .queryParam("serviceKey", key)
            .queryParam("callTp", "L")
            .queryParam("sigunguCd", req.sigunguCd)
            .queryParam("pageNo", req.pageNo)
            .queryParam("numOfRows", req.numOfRows)
            .build(true).toUri()
    }

    private fun buildDetailUri(req: LocalWelfareDetailRequest): URI {
        val key = URLEncoder.encode(serviceKey, StandardCharsets.UTF_8.toString())
        return UriComponentsBuilder
            .fromHttpUrl(detailApiUrl)
            .queryParam("serviceKey", key)
            .queryParam("callTp", "D")
            .queryParam("sigunguCd", req.sigunguCd)
            .queryParam("servId", req.servId)
            .build(true).toUri()
    }
}