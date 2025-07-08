package com.youth.policy.client

import com.youth.policy.model.YouthPolicyRequest
import com.youth.policy.model.YouthPolicyResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class YouthPolicyClient(
    private val restTemplate: RestTemplate,
    @Value("\${youth.policy.api.url}") private val apiUrl: String
) {

    fun getYouthPolicies(request: YouthPolicyRequest): YouthPolicyResponse {
        val uri = buildUri(request)
        return restTemplate.getForObject(uri, YouthPolicyResponse::class.java)
            ?: throw RuntimeException("청년 정책 API 호출 실패")
    }

    private fun buildUri(request: YouthPolicyRequest): URI {
        val builder = UriComponentsBuilder.fromHttpUrl(apiUrl)

        builder.queryParam("apiKeyNm", request.apiKeyNm)

        request.pageNum?.let { builder.queryParam("pageNum", it) }
        request.pageSize?.let { builder.queryParam("pageSize", it) }
        request.pageType?.let { builder.queryParam("pageType", it) }
        request.plcyNo?.let { builder.queryParam("plcyNo", it) }
        request.rtnType?.let { builder.queryParam("rtnType", it) }
        request.plcyKywdNm?.let { builder.queryParam("plcyKywdNm", it) }
        request.plcyExplnCn?.let { builder.queryParam("plcyExplnCn", it) }
        request.plcyNm?.let { builder.queryParam("plcyNm", it) }
        request.zipCd?.let { builder.queryParam("zipCd", it) }
        request.lclsfNm?.let { builder.queryParam("lclsfNm", it) }
        request.mclsfNm?.let { builder.queryParam("mclsfNm", it) }

        return builder.build().toUri()
    }
}