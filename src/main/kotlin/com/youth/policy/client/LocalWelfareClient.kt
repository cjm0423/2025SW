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
class LocalWelfareClient(
    private val restTemplate: RestTemplate,
    @Value("\${welfare.api.local.list.url}")   private val listApiUrl: String,
    @Value("\${welfare.api.local.detail.url}") private val detailApiUrl: String,
    @Value("\${welfare.api.local.key}")              private val serviceKey: String
) {

    fun getLocalWelfareList(req: LocalWelfareListRequest): LocalWelfareListResponse {
        val uri = buildListUri(req)
        val raw = restTemplate.getForObject(uri, String::class.java) ?: ""
        if (raw.contains("<cmmMsgHeader>")) {
            val err = restTemplate
                .getForObject(uri, WelfareErrorResponse::class.java)
            throw RuntimeException("지자체 목록 API 오류: ${err?.cmmMsgHeader?.returnAuthMsg}")
        }
        return restTemplate.getForObject(uri, LocalWelfareListResponse::class.java)
            ?: throw RuntimeException("지자체 목록 응답이 null입니다.")
    }

    fun getLocalWelfareDetail(req: LocalWelfareDetailRequest): LocalWelfareDetailResponse {
        val uri = buildDetailUri(req)
        val raw = restTemplate.getForObject(uri, String::class.java) ?: ""
        if (raw.contains("<cmmMsgHeader>")) {
            val err = restTemplate
                .getForObject(uri, WelfareErrorResponse::class.java)
            throw RuntimeException("지자체 상세 API 오류: ${err?.cmmMsgHeader?.returnAuthMsg}")
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

    fun getLocalWelfareListAsXml(req: LocalWelfareListRequest): String {
        val uri = buildListUri(req) // 기존에 있던 메서드를 재활용합니다.
        val rawXml = restTemplate.getForObject(uri, String::class.java)
            ?: throw RuntimeException("지자체 목록 API 응답이 비어있습니다.")

        if (rawXml.contains("<cmmMsgHeader>")) {
            throw RuntimeException("지자체 목록 API 오류 발생. 응답: $rawXml")
        }
        return rawXml
    }
}
