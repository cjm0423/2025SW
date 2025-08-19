package com.youth.policy.service

import com.youth.policy.client.WelfareClient
import com.youth.policy.client.LocalWelfareClient
import com.youth.policy.model.*
import org.springframework.stereotype.Service

@Service
class WelfareService(
    private val welfareClient: WelfareClient,
    private val localClient: LocalWelfareClient
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

    fun getLocalWelfareList(
        sigunguCd: String,
        pageNo: Int = 1,
        numOfRows: Int = 10
    ): LocalWelfareListResponse {
        val req = LocalWelfareListRequest(sigunguCd, pageNo, numOfRows)
        return localClient.getLocalWelfareList(req)
    }

    fun getLocalWelfareDetail(sigunguCd: String, servId: String): LocalWelfareDetailResponse {
        val req = LocalWelfareDetailRequest(sigunguCd = sigunguCd, servId = servId)
        return localClient.getLocalWelfareDetail(req)
    }

    fun getRepresentativeWelfareListByRegions(sigunguCdList: List<String>): List<LocalWelfareJsonResponse> {
        val representativeList = mutableListOf<LocalWelfareJsonResponse>()

        sigunguCdList.forEach { sigunguCd ->
            try {
                Thread.sleep(1000)

                val response = getLocalWelfareList(sigunguCd, pageNo = 1, numOfRows = 1)
                response.servList.firstOrNull()?.let { serv ->
                    val welfareJson = LocalWelfareJsonResponse(
                        serviceId = serv.servId,
                        serviceName = serv.servNm,
                        department = serv.bizChrDeptNm,
                        summary = serv.servDgst,
                        region = serv.ctpvNm,
                        city = serv.sggNm,
                        detailLink = serv.servDtlLink
                    )
                    representativeList.add(welfareJson)
                }
            } catch (e: Exception) {
                println("Error fetching welfare list for sigunguCd $sigunguCd: ${e.message}")
            }
        }
        return representativeList
    }
}