package com.youth.policy.client

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
    @Value("\${welfare.api.local.list.url}")   private val listApiUrl: String,
    @Value("\${welfare.api.local.detail.url}") private val detailApiUrl: String,
    @Value("\${welfare.api.local.key}")        private val serviceKey: String
) {

    fun getLocalWelfareList(req: LocalWelfareListRequest): LocalWelfareListResponse {
        val uri = buildListUri(req)
        val rawXml = restTemplate.getForObject(uri, String::class.java)
            ?: throw RuntimeException("지자체 목록 API 응답이 비어있습니다.")

        if (rawXml.contains("<cmmMsgHeader>")) {
            val errorResponse = restTemplate.getForObject(uri, WelfareErrorResponse::class.java)
            val errorCode = errorResponse?.cmmMsgHeader?.returnReasonCode
            val errorMessage = errorResponse?.cmmMsgHeader?.returnAuthMsg

            if (errorCode == "22") {
                throw QuotaExceededException("API 일일 호출 허용량을 초과했습니다.")
            }
            throw RuntimeException("지자체 목록 API 오류: $errorMessage (코드: $errorCode)")
        }

        return restTemplate.getForObject(uri, LocalWelfareListResponse::class.java)
            ?: throw RuntimeException("지자체 목록 응답이 null입니다.")
    }

    fun getLocalWelfareDetail(req: LocalWelfareDetailRequest): LocalWelfareDetailResponse {
        val uri = buildDetailUri(req)
        val rawXml = restTemplate.getForObject(uri, String::class.java)
            ?: throw RuntimeException("지자체 상세 API 응답이 비어있습니다.")

        if (rawXml.contains("<cmmMsgHeader>")) {
            val errorResponse = restTemplate.getForObject(uri, WelfareErrorResponse::class.java)
            val errorCode = errorResponse?.cmmMsgHeader?.returnReasonCode
            val errorMessage = errorResponse?.cmmMsgHeader?.returnAuthMsg

            if (errorCode == "22") {
                throw QuotaExceededException("API 일일 호출 허용량을 초과했습니다.")
            }
            throw RuntimeException("지자체 상세 API 오류: $errorMessage (코드: $errorCode)")
        }

        return restTemplate.getForObject(uri, LocalWelfareDetailResponse::class.java)
            ?: throw RuntimeException("지자체 상세 응답이 null입니다.")
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
