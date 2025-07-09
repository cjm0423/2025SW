package com.youth.policy.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement

@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "StatisticTableList")
data class StatisticTableListResponse(
    @JacksonXmlProperty(localName = "list_total_count")
    val listTotalCount: Int,

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "row")
    val rows: List<StatisticTableListRow>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class StatisticTableListRow(
    @JacksonXmlProperty(localName = "P_STAT_CODE")
    val pStatCode: String,

    @JacksonXmlProperty(localName = "STAT_CODE")
    val statCode: String,

    @JacksonXmlProperty(localName = "STAT_NAME")
    val statName: String,

    @JacksonXmlProperty(localName = "CYCLE")
    val cycle: String,

    @JacksonXmlProperty(localName = "SRCH_YN")
    val searchYn: String,

    @JacksonXmlProperty(localName = "ORG_NAME")
    val orgName: String
)
