package com.example.exitsw.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LocalWelfareServiceDto(
    val serviceId: String?,
    val serviceName: String?,
    val department: String?,
    val summary: String?,
    val region: String?,
    val city: String?,
    val detailLink: String?
) : Parcelable
