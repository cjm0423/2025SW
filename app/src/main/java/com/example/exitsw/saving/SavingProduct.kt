package com.example.exitsw

import com.google.gson.annotations.SerializedName

data class SavingProduct(
    @SerializedName("fin_prdt_cd") val fin_prdt_cd: String,
    @SerializedName("fin_prdt_nm") val fin_prdt_nm: String,
    @SerializedName("kor_co_nm") val kor_co_nm: String,
    @SerializedName("join_member") val join_member: String,
    @SerializedName("etc_note") val etc_note: String?,
    @SerializedName("intr_rate") val intr_rate: Double?,
    @SerializedName("intr_rate2") val intr_rate2: Double?,
    @SerializedName("homp_url") val homp_url: String?
)

data class UserInfo(
    val age: Int,
    val monthlySaving: Int,
    val isSalaryTransferAvailable: Boolean,
    val isAutoTransferAvailable: Boolean,
    val isNonFaceToFaceAvailable: Boolean,
    val isYouthBenefitEligible: Boolean,
    val isStudent: Boolean,
    val hasCompletedFinancialEducation: Boolean
)