package com.youth.policy.controller

import com.youth.policy.model.LocalWelfareDetailRequest
import com.youth.policy.model.LocalWelfareDetailResponse
import com.youth.policy.model.LocalWelfareListRequest
import com.youth.policy.model.LocalWelfareListResponse
import com.youth.policy.service.LocalWelfareService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/local/welfare/services")
open class LocalWelfareController(
    private val service: LocalWelfareService
) {

    @GetMapping
    open fun getLocalWelfareList(
        @RequestParam(defaultValue = "1") pageNo: Int,
        @RequestParam(defaultValue = "10") numOfRows: Int,
        @RequestParam(defaultValue = "001") lifeArray: String,
        @RequestParam(defaultValue = "010") trgterIndvdlArray: String?,
        @RequestParam(defaultValue = "") intrsThemaArray: String?,
        @RequestParam(defaultValue = "", required = false) srchKeyCode: String?,
        @RequestParam(defaultValue = "", required = false) searchWrd: String?,
        @RequestParam(defaultValue = "", required = false) arrgOrd: String?,
        @RequestParam(defaultValue = "",required = false) ctpvNm: String?,
        @RequestParam(defaultValue = "", required = false) sggNm: String?
    ): LocalWelfareListResponse {
        val req = LocalWelfareListRequest(
            pageNo            = pageNo,
            numOfRows         = numOfRows,
            lifeArray         = lifeArray?.ifBlank       { null },
            trgterIndvdlArray = trgterIndvdlArray?.ifBlank { null },
            intrsThemaArray   = intrsThemaArray?.ifBlank  { null },
            srchKeyCode       = srchKeyCode?.ifBlank      { null },
            searchWrd         = searchWrd?.ifBlank        { null },
            arrgOrd           = arrgOrd?.ifBlank          { null },
            ctpvNm            = ctpvNm?.ifBlank           { null },
            sggNm             = sggNm?.ifBlank           { null }
        )
        return service.getLocalWelfareList(req)
    }

    @GetMapping("/{servId}")
    open fun getLocalWelfareDetail(@PathVariable servId: String): LocalWelfareDetailResponse {
        return service.getLocalWelfareDetail(LocalWelfareDetailRequest(servId))
    }
}
