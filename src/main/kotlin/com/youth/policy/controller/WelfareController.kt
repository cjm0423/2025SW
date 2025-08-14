package com.youth.policy.controller

import com.youth.policy.model.WelfareDetailResponse
import com.youth.policy.model.WelfareListResponse
import com.youth.policy.model.LocalWelfareDetailResponse
import com.youth.policy.model.LocalWelfareListResponse
import com.youth.policy.service.WelfareService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import com.youth.policy.model.LocalWelfareJsonResponse
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
        try {
            println("복지 서비스 목록 API 호출됨 - 파라미터: pageNo=$pageNo, numOfRows=$numOfRows, srchKeyCode=$srchKeyCode")
            val result = welfareService.getWelfareList(
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
            println("복지 서비스 목록 API 처리 완료")
            return result
        } catch (e: Exception) {
            println("복지 서비스 목록 API 에러 발생: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    @GetMapping("/services/{servId}")
    fun getWelfareDetail(
        @PathVariable servId: String
    ): WelfareDetailResponse {
        try {
            println("복지 서비스 상세 API 호출됨 - servId: $servId")
            val result = welfareService.getWelfareDetail(servId)
            println("복지 서비스 상세 API 처리 완료")
            return result
        } catch (e: Exception) {
            println("복지 서비스 상세 API 에러 발생: ${e.message}")
            e.printStackTrace()
            throw e
        }
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

    @GetMapping("/local/services/json", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getLocalWelfareListAsJson(
        @RequestParam(defaultValue = "1") pageNo: Int,
        @RequestParam(defaultValue = "1") numOfRows: Int
    ): Map<String, LocalWelfareListResponse> {
        val regionsToSearch = listOf(
            "1100000", "4100000", "5100000", "4300000", "4400000",
            "4500000", "4600000", "4700000", "4800000", "5000000"
        )
        println("JSON 통합 복지 서비스 목록 API 호출됨 - 각 지역별 ${numOfRows}개")
        return welfareService.getLocalWelfareListByRegions(
            sigunguCdList = regionsToSearch,
            pageNo = pageNo,
            numOfRows = numOfRows
        )
    }
}