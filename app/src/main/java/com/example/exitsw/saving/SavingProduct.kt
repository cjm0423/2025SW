package com.example.exitsw

import com.google.gson.annotations.SerializedName

data class SavingProduct(
    @SerializedName("fin_prdt_cd")     val fin_prdt_cd: String,
    @SerializedName("fin_prdt_nm")     val fin_prdt_nm: String,
    @SerializedName("kor_co_nm")       val kor_co_nm: String,
    @SerializedName("join_member")     val join_member: String,
    @SerializedName("etc_note")        val etc_note: String?,
    @SerializedName("intr_rate")       val intr_rate: Double?,
    @SerializedName("intr_rate2")      val intr_rate2: Double?,
    @SerializedName("homp_url")        val homp_url: String?,

    @SerializedName("join_way")  val join_way: String? = null,  // 가입 방법
    @SerializedName("mtrt_int")  val mtrt_int: String? = null,  // 만기 후 이자
    @SerializedName("spcl_cnd")  val spcl_cnd: String? = null   // 우대 조건
)