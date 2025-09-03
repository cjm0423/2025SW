package com.youth.policy.controller

import com.youth.policy.model.*
import com.youth.policy.service.WelfareService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/welfare")
class WelfareController(
    private val welfareService: WelfareService
) {

    @GetMapping("/services")
    fun getWelfareList(
        @RequestParam(defaultValue = "1") pageNo: Int,
        @RequestParam(defaultValue = "10") numOfRows: Int,
        @RequestParam(defaultValue = "001") srchKeyCode: String,
        @RequestParam(required = false) searchWrd: String?,
        @RequestParam(required = false) lifeArray: String?,
        @RequestParam(required = false) trgterIndvdlArray: String?,
        @RequestParam(required = false) intrsThemaArray: String?,
        @RequestParam(required = false) age: String?,
        @RequestParam(required = false) onapPsbltYn: String?,
        @RequestParam(required = false) orderBy: String?
    ): WelfareListResponse {
        return welfareService.getWelfareList(
            pageNo = pageNo,
            numOfRows = numOfRows,
            srchKeyCode = srchKeyCode,
            searchWrd = searchWrd,
            lifeArray = lifeArray,
            trgterIndvdlArray = trgterIndvdlArray,
            intrsThemaArray = intrsThemaArray,
            age = age,
            onapPsbltYn = onapPsbltYn,
            orderBy = orderBy
        )
    }

    @GetMapping("/services/{servId}")
    fun getWelfareDetail(
        @PathVariable servId: String
    ): WelfareDetailResponse {
        return welfareService.getWelfareDetail(servId)
    }

    @GetMapping("/local/services", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getLocalWelfareList(
        @RequestParam sigunguCd: String,
        @RequestParam(defaultValue = "1") pageNo: Int,
        @RequestParam(defaultValue = "10") numOfRows: Int
    ): LocalWelfareListResponse {
        return welfareService.getLocalWelfareList(sigunguCd, pageNo, numOfRows)
    }

    @GetMapping("/local/services/{servId}")
    fun getLocalWelfareDetail(
        @PathVariable servId: String,
        @RequestParam sigunguCd: String
    ): LocalWelfareDetailResponse {
        return welfareService.getLocalWelfareDetail(sigunguCd, servId)
    }
}