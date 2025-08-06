package com.youth.policy.service

import com.youth.policy.client.YouthPolicyClient
import com.youth.policy.model.YouthPolicyRequest
import com.youth.policy.model.YouthPolicyResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class YouthPolicyService(
    private val youthPolicyClient: YouthPolicyClient,
    @Value("\${youth.policy.api.key}") private val apiKey: String
) {

    fun getYouthPolicies(
        pageNum: Int? = null,
        pageSize: Int? = null,
        pageType: String? = null,
        plcyNo: String? = null,
        rtnType: String? = "json",
        plcyKywdNm: String? = null,
        plcyExplnCn: String? = null,
        plcyNm: String? = null,
        zipCd: String? = null,
        lclsfNm: String? = null,
        mclsfNm: String? = null
    ): YouthPolicyResponse {
        val request = YouthPolicyRequest(
            apiKeyNm = apiKey,
            pageNum = pageNum,
            pageSize = pageSize,
            pageType = pageType,
            plcyNo = plcyNo,
            rtnType = rtnType,
            plcyKywdNm = plcyKywdNm,
            plcyExplnCn = plcyExplnCn,
            plcyNm = plcyNm,
            zipCd = zipCd,
            lclsfNm = lclsfNm,
            mclsfNm = mclsfNm
        )

        return youthPolicyClient.getYouthPolicies(request)
    }
}
