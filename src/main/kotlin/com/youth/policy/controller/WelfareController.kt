package com.youth.policy.controller

import com.youth.policy.model.*
import com.youth.policy.service.WelfareService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.http.MediaType

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

    @GetMapping("/local/services")
    fun getLocalWelfareList(
        @RequestParam sigunguCd: String,
        @RequestParam(defaultValue = "1") pageNo: Int,
        @RequestParam(defaultValue = "10") numOfRows: Int
    ): LocalWelfareListResponse =
        welfareService.getLocalWelfareList(sigunguCd, pageNo, numOfRows)

    @GetMapping("/local/services/{servId}")
    fun getLocalWelfareDetail(
        @PathVariable servId: String,
        @RequestParam sigunguCd: String
    ): LocalWelfareDetailResponse =
        welfareService.getLocalWelfareDetail(sigunguCd, servId)

    @GetMapping("/local/services/representatives", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getRepresentativeWelfareList(): List<LocalWelfareJsonResponse> {
        val regionsToSearch = listOf(
            "1111000" // 서울특별시
            // "4100000", // 경기도
            // "5100000", // 강원특별자치도
            // "4300000", // 충청북도
            // "4400000", // 충청남도
            // "4500000", // 전북특별자치도
            // "4600000", // 전라남도
            // "4700000", // 경상북도
            // "4800000", // 경상남도
            // "5000000"  // 제주특별자치도
        )
        return welfareService.getRepresentativeWelfareListByRegions(regionsToSearch)
    }
}