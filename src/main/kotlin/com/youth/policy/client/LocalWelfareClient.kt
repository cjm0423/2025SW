package com.youth.policy.client

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.youth.policy.exception.QuotaExceededException
import com.youth.policy.model.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
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
        try {
            val response = restTemplate.getForObject(uri, LocalWelfareListResponse::class.java)
            return response ?: createEmptyResponse(req)
        } catch (e: HttpClientErrorException) {
            val errorXml = e.responseBodyAsString
            if (errorXml.contains("<cmmMsgHeader>")) {
                val errorResponse = xmlMapper.readValue(errorXml, WelfareErrorResponse::class.java)
                val errorCode = errorResponse?.cmmMsgHeader?.returnReasonCode
                val errorMessage = errorResponse?.cmmMsgHeader?.returnAuthMsg

                if (errorCode == "22") {
                    throw QuotaExceededException("API 일일 호출 허용량을 초과했습니다.")
                }
                throw RuntimeException("지자체 목록 API 오류: $errorMessage (코드: $errorCode)")
            }
            throw RuntimeException("지자체 목록 API 호출 실패: ${e.statusCode} ${e.message}")
        }
    }

    fun getLocalWelfareDetail(req: LocalWelfareDetailRequest): LocalWelfareDetailResponse {
        val uri = buildDetailUri(req)
        try {
            val response = restTemplate.getForObject(uri, LocalWelfareDetailResponse::class.java)
            return response ?: throw RuntimeException("지자체 상세 API가 빈 응답을 반환했습니다: servId=${req.servId}")
        } catch (e: HttpClientErrorException) {
            val errorXml = e.responseBodyAsString
            if (errorXml.contains("<cmmMsgHeader>")) {
                val errorResponse = xmlMapper.readValue(errorXml, WelfareErrorResponse::class.java)
                val errorCode = errorResponse?.cmmMsgHeader?.returnReasonCode
                val errorMessage = errorResponse?.cmmMsgHeader?.returnAuthMsg
                throw RuntimeException("지자체 상세 API 오류: $errorMessage (코드: $errorCode)")
            }
            throw RuntimeException("지자체 상세 API 호출 실패: ${e.statusCode} ${e.message}")
        }
    }

    private fun createEmptyResponse(req: LocalWelfareListRequest) = LocalWelfareListResponse(
        servList = emptyList(),
        pageNo = req.pageNo,
        totalCount = 0,
        numOfRows = req.numOfRows,
        resultCode = "00",
        resultMessage = "NORMAL SERVICE (No data)."
    )

    private fun buildListUri(req: LocalWelfareListRequest): URI {
        val key = URLEncoder.encode(serviceKey, StandardCharsets.UTF_8.toString())
        return UriComponentsBuilder
            .fromHttpUrl(listApiUrl)
            .queryParam("serviceKey", key)
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
