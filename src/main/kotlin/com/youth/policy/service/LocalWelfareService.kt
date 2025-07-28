package com.youth.policy.service

import com.youth.policy.client.LocalWelfareClient
import com.youth.policy.model.LocalWelfareDetailRequest
import com.youth.policy.model.LocalWelfareDetailResponse
import com.youth.policy.model.LocalWelfareListRequest
import com.youth.policy.model.LocalWelfareListResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
open class LocalWelfareService(
    private val client: LocalWelfareClient
) {
    private val log = LoggerFactory.getLogger(LocalWelfareService::class.java)

    open fun getLocalWelfareList(req: LocalWelfareListRequest): LocalWelfareListResponse {
        log.info("▶ 지자체 복지 서비스 목록 조회 요청: $req")
        return client.getLocalWelfareList(req)
    }

    open fun getLocalWelfareDetail(req: LocalWelfareDetailRequest): LocalWelfareDetailResponse {
        log.info("▶ 지자체 복지 서비스 상세 조회 요청: $req")
        return client.getLocalWelfareDetail(req)
    }
}
