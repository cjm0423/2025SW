package com.youth.policy.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class ExchangeRateResponseItem(
    @JsonProperty("result") val result: Int,
    @JsonProperty("cur_unit") val curUnit: String,
    @JsonProperty("cur_nm") val curName: String,
    @JsonProperty("ttb") val ttb: String,
    @JsonProperty("tts") val tts: String,
    @JsonProperty("deal_bas_r") val dealBaseRate: String,
    @JsonProperty("bkpr") val bkpr: String,
    @JsonProperty("yy_efee_r") val yearlyEfeeRate: String,
    @JsonProperty("ten_dd_efee_r") val tenDdEfeeRate: String,
    @JsonProperty("kftc_deal_bas_r") val kftcDealBaseRate: String,
    @JsonProperty("kftc_bkpr") val kftcBkpr: String
)

typealias ExchangeRateResponse = List<ExchangeRateResponseItem>
