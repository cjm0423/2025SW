package com.youth.policy.service

import com.youth.policy.client.WelfareClient
import com.youth.policy.model.WelfareDetailRequest
import com.youth.policy.model.WelfareDetailResponse
import com.youth.policy.model.WelfareListRequest
import com.youth.policy.model.WelfareListResponse
import org.springframework.stereotype.Service

@Service
class WelfareService(
    private val welfareClient: WelfareClient
) {

    fun getWelfareList(
        pageNo: Int = 1,
        numOfRows: Int = 10,
        srchKeyCode: String = "001",
        searchWrd: String? = null,
        lifeArray: String? = null,
        trgterIndvdlArray: String? = null,
        intrsThemaArray: String? = null,
        age: String? = null,
        onapPsbltYn: String? = null,
        orderBy: String? = null
    ): WelfareListResponse {
        val request = WelfareListRequest(
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

        return welfareClient.getWelfareList(request)
    }

    fun getWelfareDetail(servId: String): WelfareDetailResponse {
        val request = WelfareDetailRequest(servId = servId)
        return welfareClient.getWelfareDetail(request)
    }
}