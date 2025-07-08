package com.youth.policy.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.dataformat.xml.annotation.*

/**
 * 지자체 복지 서비스 목록 응답
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "wantedList")
data class LocalWelfareListResponse(
    @JacksonXmlProperty(localName = "resultCode")
    val resultCode: String? = null,

    @JacksonXmlProperty(localName = "resultMessage")
    val resultMessage: String? = null,

    @JacksonXmlProperty(localName = "numOfRows")
    val numOfRows: Int? = null,

    @JacksonXmlProperty(localName = "pageNo")
    val pageNo: Int? = null,

    @JacksonXmlProperty(localName = "totalCount")
    val totalCount: Int? = null,

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "wantedList")
    val wantedList: List<LocalWelfareDetailItem> = emptyList()
)

/**
 * 목록 아이템 하나
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class LocalWelfareDetailItem(
    @JacksonXmlProperty(localName = "servId")
    val servId: String? = null,

    @JacksonXmlProperty(localName = "servNm")
    val servNm: String? = null,

    @JacksonXmlProperty(localName = "ctpvNm")
    val ctpvNm: String? = null,

    @JacksonXmlProperty(localName = "sggNm")
    val sggNm: String? = null,

    @JacksonXmlProperty(localName = "apiSvcCd")
    val apiSvcCd: String? = null,

    @JacksonXmlProperty(localName = "wlfareInfoDtlCd")
    val wlfareInfoDtlCd: String? = null
)

/**
 * 지자체 복지 서비스 상세 응답
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "servSeDetail")
data class LocalWelfareDetailResponse(
    @JacksonXmlProperty(localName = "resultCode")
    val resultCode: String? = null,

    @JacksonXmlProperty(localName = "resultMessage")
    val resultMessage: String? = null,

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "servSeDetail")
    val servSeDetail: List<LocalWelfareServSeDetail> = emptyList()
)

/**
 * 상세 내역 아이템 하나
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class LocalWelfareServSeDetail(
    @JacksonXmlProperty(localName = "servSeCode")
    val servSeCode: String? = null,

    @JacksonXmlProperty(localName = "servSeDetailLink")
    val servSeDetailLink: String? = null,

    @JacksonXmlProperty(localName = "wlfareInfoDtlNm")
    val wlfareInfoDtlNm: String? = null,

    @JacksonXmlProperty(localName = "servDgst")
    val servDgst: String? = null
)
