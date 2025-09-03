package com.example.exitsw.data

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class LocalWelfareServiceDto(
    @SerializedName("servId")
    val serviceId: String?,

    @SerializedName("servNm")
    val serviceName: String?,

    @SerializedName("bizChrDeptNm")
    val department: String?,

    @SerializedName("servDgst")
    val summary: String?,

    @SerializedName("ctpvNm")
    val region: String?,

    @SerializedName("sggNm")
    val city: String?,

    @SerializedName("servDtlLink")
    val detailLink: String?
) : Parcelable