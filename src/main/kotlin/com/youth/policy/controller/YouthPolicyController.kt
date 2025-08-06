package com.youth.policy.controller

import com.youth.policy.model.YouthPolicyResponse
import com.youth.policy.service.YouthPolicyService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/youth/policies")
class YouthPolicyController(
    private val youthPolicyService: YouthPolicyService
) {

    @GetMapping
    fun getYouthPolicies(
        @RequestParam(required = false) pageNum: Int?,
        @RequestParam(required = false) pageSize: Int?,
        @RequestParam(required = false) pageType: String?,
        @RequestParam(required = false) plcyNo: String?,
        @RequestParam(required = false) rtnType: String?,
        @RequestParam(required = false) plcyKywdNm: String?,
        @RequestParam(required = false) plcyExplnCn: String?,
        @RequestParam(required = false) plcyNm: String?,
        @RequestParam(required = false) zipCd: String?,
        @RequestParam(required = false) lclsfNm: String?,
        @RequestParam(required = false) mclsfNm: String?
    ): YouthPolicyResponse {
        return youthPolicyService.getYouthPolicies(
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
    }
}
