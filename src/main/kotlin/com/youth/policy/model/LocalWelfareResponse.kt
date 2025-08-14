package com.youth.policy.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty

@JacksonXmlRootElement(localName = "wantedList")
@JsonIgnoreProperties(ignoreUnknown = true)
data class LocalWelfareListResponse(
    @JacksonXmlProperty(localName = "resultCode")    val resultCode: String?,
    @JacksonXmlProperty(localName = "resultMessage") val resultMessage: String?,
    @JacksonXmlProperty(localName = "numOfRows")     val numOfRows: Int?,
    @JacksonXmlProperty(localName = "pageNo")        val pageNo: Int?,
    @JacksonXmlProperty(localName = "totalCount")    val totalCount: Int?,
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "servList")       
    val servList: List<LocalWelfareService> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class LocalWelfareService(
    @JacksonXmlProperty(localName = "bizChrDeptNm")          val bizChrDeptNm: String?,
    @JacksonXmlProperty(localName = "ctpvNm")                val ctpvNm: String?,
    @JacksonXmlProperty(localName = "sggNm")                 val sggNm: String?,
    @JacksonXmlProperty(localName = "servDgst")              val servDgst: String?,
    @JacksonXmlProperty(localName = "servDtlLink")           val servDtlLink: String?,
    @JacksonXmlProperty(localName = "lifeNmArray")           val lifeNmArray: String?,
    @JacksonXmlProperty(localName = "intrsThemaNmArray")     val intrsThemaNmArray: String?,
    @JacksonXmlProperty(localName = "sprtCycNm")             val sprtCycNm: String?,
    @JacksonXmlProperty(localName = "srvPvsnNm")             val srvPvsnNm: String?,
    @JacksonXmlProperty(localName = "aplyMtdNm")             val aplyMtdNm: String?,
    @JacksonXmlProperty(localName = "inqNum")                val inqNum: String?,
    @JacksonXmlProperty(localName = "lastModYmd")           val lastModYmd: String?,
    @JacksonXmlProperty(localName = "servId")                val servId: String,
    @JacksonXmlProperty(localName = "servNm")                val servNm: String,
    @JacksonXmlProperty(localName = "trgterIndvdlNmArray")   val trgterIndvdlNmArray: String?
)

@JacksonXmlRootElement(localName = "wantedDtl")
@JsonIgnoreProperties(ignoreUnknown = true)
data class LocalWelfareDetailResponse(
    @JacksonXmlProperty(localName = "resultCode")           val resultCode: String?,
    @JacksonXmlProperty(localName = "resultMessage")        val resultMessage: String?,
    @JacksonXmlProperty(localName = "servId")               val servId: String,
    @JacksonXmlProperty(localName = "servNm")               val servNm: String,
    @JacksonXmlProperty(localName = "enfcBgngYmd")          val enfcBgngYmd: String?,
    @JacksonXmlProperty(localName = "enfcEndYmd")           val enfcEndYmd: String?,
    @JacksonXmlProperty(localName = "bizChrDeptNm")         val bizChrDeptNm: String?,
    @JacksonXmlProperty(localName = "ctpvNm")               val ctpvNm: String?,
    @JacksonXmlProperty(localName = "sggNm")                val sggNm: String?,
    @JacksonXmlProperty(localName = "servDgst")             val servDgst: String?,
    @JacksonXmlProperty(localName = "lifeNmArray")          val lifeNmArray: String?,
    @JacksonXmlProperty(localName = "trgterIndvdlNmArray")  val trgterIndvdlNmArray: String?,
    @JacksonXmlProperty(localName = "intrsThemaNmArray")    val intrsThemaNmArray: String?,
    @JacksonXmlProperty(localName = "sprtCycNm")            val sprtCycNm: String?,
    @JacksonXmlProperty(localName = "srvPvsnNm")            val srvPvsnNm: String?,
    @JacksonXmlProperty(localName = "aplyMtdNm")            val aplyMtdNm: String?,
    @JacksonXmlProperty(localName = "sprtTrgtCn")           val sprtTrgtCn: String?,
    @JacksonXmlProperty(localName = "slctCritCn")           val slctCritCn: String?,
    @JacksonXmlProperty(localName = "alwServCn")            val alwServCn: String?,
    @JacksonXmlProperty(localName = "aplyMtdCn")            val aplyMtdCn: String?,
    @JacksonXmlProperty(localName = "inqNum")               val inqNum: String?,
    @JacksonXmlProperty(localName = "lastModYmd")          val lastModYmd: String?,
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "inqplCtadrList")       val inqplCtadrList: List<WelfareInfo> = emptyList(),
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "inqplHmpgReldList")    val inqplHmpgReldList: List<WelfareInfo> = emptyList(),
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "baslawList")          val baslawList: List<WelfareInfo> = emptyList(),
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "basfrmList")          val basfrmList: List<WelfareInfo> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class WelfareInfo(
    @JacksonXmlProperty(localName = "wlfareInfoDtlCd")    val wlfareInfoDtlCd: String?,
    @JacksonXmlProperty(localName = "wlfareInfoReldNm")   val wlfareInfoReldNm: String?,
    @JacksonXmlProperty(localName = "wlfareInfoReldCn")   val wlfareInfoReldCn: String?
)
