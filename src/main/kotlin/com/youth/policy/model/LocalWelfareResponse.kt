package com.youth.policy.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.dataformat.xml.annotation.*

@JacksonXmlRootElement(localName = "wantedList")
@JsonIgnoreProperties(ignoreUnknown = true)
data class LocalWelfareListResponse(
    @JacksonXmlProperty(localName = "totalCount") val totalCount: Int,
    @JacksonXmlProperty(localName = "pageNo")     val pageNo: Int,
    @JacksonXmlProperty(localName = "numOfRows")  val numOfRows: Int,
    @JacksonXmlProperty(localName = "resultCode")    val resultCode: String,
    @JacksonXmlProperty(localName = "resultMessage") val resultMessage: String,
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "servList")
    val servList: List<LocalWelfareService> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class LocalWelfareService(
    @JacksonXmlProperty(localName = "servId")           val servId: String,
    @JacksonXmlProperty(localName = "servNm")           val servNm: String,
    @JacksonXmlProperty(localName = "jrsdInsttNm")      val jrsdInsttNm: String?,
    @JacksonXmlProperty(localName = "lifeArray")        val lifeArray: String?,
    @JacksonXmlProperty(localName = "trgterIndvdlArray")val trgterIndvdlArray: String?,
    @JacksonXmlProperty(localName = "intrsThemaArray")  val intrsThemaArray: String?
)

@JacksonXmlRootElement(localName = "wantedDtl")
@JsonIgnoreProperties(ignoreUnknown = true)
data class LocalWelfareDetailResponse(
    @JacksonXmlProperty(localName = "servId")    val servId: String,
    @JacksonXmlProperty(localName = "servNm")    val servNm: String,
    @JacksonXmlProperty(localName = "tgtrDtlCn") val tgtrDtlCn: String?,
    @JacksonXmlProperty(localName = "srvPvsnNm") val srvPvsnNm: String?
)
