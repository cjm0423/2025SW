package com.youth.policy.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement

@JacksonXmlRootElement(localName = "wantedList")
@JsonIgnoreProperties(ignoreUnknown = true)
data class WelfareListResponse(
    @JacksonXmlProperty(localName = "totalCount")
    val totalCount: Int,

    @JacksonXmlProperty(localName = "pageNo")
    val pageNo: Int,

    @JacksonXmlProperty(localName = "numOfRows")
    val numOfRows: Int,

    @JacksonXmlProperty(localName = "resultCode")
    val resultCode: String,

    @JacksonXmlProperty(localName = "resultMessage")
    val resultMessage: String,

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "servList")
    val servList: List<WelfareService> = emptyList()
)

@JacksonXmlRootElement(localName = "wantedDtl")
@JsonIgnoreProperties(ignoreUnknown = true)
data class WelfareDetailResponse(
    @JacksonXmlProperty(localName = "servId")
    val servId: String,

    @JacksonXmlProperty(localName = "servNm")
    val servNm: String,

    @JacksonXmlProperty(localName = "jurMnofNm")
    val jurMnofNm: String? = null,

    @JacksonXmlProperty(localName = "tgtrDtlCn")
    val tgtrDtlCn: String? = null,

    @JacksonXmlProperty(localName = "slctCritCn")
    val slctCritCn: String? = null,

    @JacksonXmlProperty(localName = "alwServCn")
    val alwServCn: String? = null,

    @JacksonXmlProperty(localName = "lifeArray")
    val lifeArray: String? = null,

    @JacksonXmlProperty(localName = "intrsThemaArray")
    val intrsThemaArray: String? = null,

    @JacksonXmlProperty(localName = "trgterIndvdlArray")
    val trgterIndvdlArray: String? = null,

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "servSeDetailList")
    val servSeDetailList: List<ServSeDetail> = emptyList()
)

@JacksonXmlRootElement(localName = "OpenAPI_ServiceResponse")
@JsonIgnoreProperties(ignoreUnknown = true)
data class WelfareErrorResponse(
    @JacksonXmlProperty(localName = "cmmMsgHeader")
    val cmmMsgHeader: ErrorHeader
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ErrorHeader(
    @JacksonXmlProperty(localName = "errMsg")
    val errMsg: String,

    @JacksonXmlProperty(localName = "returnAuthMsg")
    val returnAuthMsg: String,

    @JacksonXmlProperty(localName = "returnReasonCode")
    val returnReasonCode: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class WelfareService(
    @JacksonXmlProperty(localName = "servId")
    val servId: String,

    @JacksonXmlProperty(localName = "servNm")
    val servNm: String,

    @JacksonXmlProperty(localName = "servDtlLink")
    val servDtlLink: String? = null,

    @JacksonXmlProperty(localName = "lifeArray")
    val lifeArray: String? = null,

    @JacksonXmlProperty(localName = "trgterIndvdlArray")
    val trgterIndvdlArray: String? = null,

    @JacksonXmlProperty(localName = "intrsThemaArray")
    val intrsThemaArray: String? = null,

    @JacksonXmlProperty(localName = "sprtCycNm")
    val sprtCycNm: String? = null,

    @JacksonXmlProperty(localName = "srvPvsnNm")
    val srvPvsnNm: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ServSeDetail(
    @JacksonXmlProperty(localName = "servSeCode")
    val servSeCode: String? = null,

    @JacksonXmlProperty(localName = "servSeDetailLink")
    val servSeDetailLink: String? = null,

    @JacksonXmlProperty(localName = "servSeDetailNm")
    val servSeDetailNm: String? = null
)
