package com.youth.policy.client

import com.youth.policy.model.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Component
class WelfareClient(
    private val restTemplate: RestTemplate,
    @Value("\${welfare.api.central.list.url}") private val listApiUrl: String,
    @Value("\${welfare.api.central.detail.url}") private val detailApiUrl: String,
    @Value("\${welfare.api.central.key}") private val serviceKey: String
) {

    fun getWelfareList(request: WelfareListRequest): WelfareListResponse {
        val uri = buildListUri(request)
        println("API 호출 URL: $uri")

        val rawResponse = restTemplate.getForObject(uri, String::class.java)
        println("전체 응답:\n$rawResponse")

        if (rawResponse?.contains("<cmmMsgHeader>") == true) {
            val errorResponse = restTemplate.getForObject(uri, WelfareErrorResponse::class.java)
            val errorMsg = errorResponse?.cmmMsgHeader?.returnAuthMsg
            val errorCode = errorResponse?.cmmMsgHeader?.returnReasonCode
            throw RuntimeException("API 오류 발생: 코드=$errorCode, 메시지=$errorMsg")
        }

        return restTemplate.getForObject(uri, WelfareListResponse::class.java)
            ?: throw RuntimeException("목록 API 응답이 null입니다.")
    }

    fun getWelfareDetail(request: WelfareDetailRequest): WelfareDetailResponse {
        val uri = buildDetailUri(request)
        println("상세 API 호출 URL: $uri")

        val rawResponse = restTemplate.getForObject(uri, String::class.java)
        println("상세 원본 응답 일부: ${rawResponse?.take(500)}")

        if (rawResponse?.contains("<cmmMsgHeader>") == true) {
            val errorResponse = restTemplate.getForObject(uri, WelfareErrorResponse::class.java)
            val errorMsg = errorResponse?.cmmMsgHeader?.returnAuthMsg
            val errorCode = errorResponse?.cmmMsgHeader?.returnReasonCode
            throw RuntimeException("상세 API 오류 발생: 코드=$errorCode, 메시지=$errorMsg")
        }

        return restTemplate.getForObject(uri, WelfareDetailResponse::class.java)
            ?: throw RuntimeException("상세 API 응답이 null입니다.")
    }

    private fun buildListUri(request: WelfareListRequest): URI {
        val encodedKey = URLEncoder.encode(serviceKey, StandardCharsets.UTF_8.toString())
        val builder = UriComponentsBuilder.fromHttpUrl(listApiUrl)
            .queryParam("serviceKey", encodedKey)
            .queryParam("callTp", "L")
            .queryParam("pageNo", request.pageNo)
            .queryParam("numOfRows", request.numOfRows)
            .queryParam("srchKeyCode", request.srchKeyCode)

        request.searchWrd?.let { builder.queryParam("searchWrd", it) }
        request.lifeArray?.let { builder.queryParam("lifeArray", it) }
        request.trgterIndvdlArray?.let { builder.queryParam("trgterIndvdlArray", it) }
        request.intrsThemaArray?.let { builder.queryParam("intrsThemaArray", it) }
        request.age?.let { builder.queryParam("age", it) }
        request.onapPsbltYn?.let { builder.queryParam("onapPsbltYn", it) }
        request.orderBy?.let { builder.queryParam("orderBy", it) }

        return builder.build(true).toUri()
    }

    private fun buildDetailUri(request: WelfareDetailRequest): URI {
        val encodedKey = URLEncoder.encode(serviceKey, StandardCharsets.UTF_8.toString())
        return UriComponentsBuilder.fromHttpUrl(detailApiUrl)
            .queryParam("serviceKey", encodedKey)
            .queryParam("callTp", "D")
            .queryParam("servId", request.servId)
            .build(true).toUri()
    }
}
