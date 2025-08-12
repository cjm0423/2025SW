package com.youth.policy.service

import com.youth.policy.client.WelfareClient
import com.youth.policy.client.LocalWelfareClient
import com.youth.policy.model.*
import org.springframework.stereotype.Service
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

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

    // ─── 추가된 메서드: 여러 시군구코드를 한 번에 조회 후 그룹핑 ───
    fun getLocalWelfareListByRegions(
        sigunguCdList: List<String>,
        pageNo: Int = 1,
        numOfRows: Int = 10
    ): Map<String, LocalWelfareListResponse> {
        return sigunguCdList.associateWith { code ->
            val req = LocalWelfareListRequest(sigunguCd = code, pageNo = pageNo, numOfRows = numOfRows)
            localClient.getLocalWelfareList(req)
        }
    }

    fun getLocalWelfareListAsJson(sigunguCd: String, pageNo: Int = 1, numOfRows: Int = 10): List<LocalWelfareJsonResponse> {
        val req = LocalWelfareListRequest(sigunguCd, pageNo, numOfRows)

        // ✨ [최종 해결책] 가장 안정적으로 동작하는 기존 Client 메서드를 호출합니다.
        val parsedData: LocalWelfareListResponse = localClient.getLocalWelfareList(req)

        // 안정적으로 받아온 데이터를 우리가 원하는 최종 JSON 형태로 변환합니다.
        return parsedData.servList.map { serv ->
            LocalWelfareJsonResponse(
                serviceId = serv.servId,
                serviceName = serv.servNm,
                department = serv.bizChrDeptNm,
                summary = serv.servDgst,
                region = serv.ctpvNm,
                city = serv.sggNm,
                detailLink = serv.servDtlLink
            )
        }
    }
}
