package com.example.exitsw.data

import android.os.Parcelable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
data class  LocalWelfareServiceDto(
    @get:PropertyName("aplyMtdNm") @set:PropertyName("aplyMtdNm")
    var aplyMtdNm: String? = null,

    @get:PropertyName("bizChrDeptNm") @set:PropertyName("bizChrDeptNm")
    var bizChrDeptNm: String? = null,

    @get:PropertyName("ctpvNm") @set:PropertyName("ctpvNm")
    var ctpvNm: String? = null,

    @get:PropertyName("inqNum") @set:PropertyName("inqNum")
    var inqNum: String? = null,

    @get:PropertyName("intrsThemaNmArray") @set:PropertyName("intrsThemaNmArray")
    var intrsThemaNmArray: @RawValue Any? = null,

    @get:PropertyName("lastModYmd") @set:PropertyName("lastModYmd")
    var lastModYmd: String? = null,

    @get:PropertyName("lifeNmArray") @set:PropertyName("lifeNmArray")
    var lifeNmArray: @RawValue Any? = null,

    @get:PropertyName("servDgst") @set:PropertyName("servDgst")
    var servDgst: String? = null,

    @get:PropertyName("servDtlLink") @set:PropertyName("servDtlLink")
    var servDtlLink: String? = null,

    @get:PropertyName("servId") @set:PropertyName("servId")
    var servId: String? = null,

    @get:PropertyName("servNm") @set:PropertyName("servNm")
    var servNm: String? = null,

    @get:PropertyName("sggNm") @set:PropertyName("sggNm")
    var sggNm: String? = null,

    @get:PropertyName("source") @set:PropertyName("source")
    var source: String? = null,

    @get:PropertyName("sprtCycNm") @set:PropertyName("sprtCycNm")
    var sprtCycNm: String? = null,

    @get:PropertyName("srvPvsnNm") @set:PropertyName("srvPvsnNm")
    var srvPvsnNm: String? = null,

    // [수정] String? 에서 Timestamp? 타입으로 변경
    @get:PropertyName("syncedAt") @set:PropertyName("syncedAt")
    var syncedAt: Timestamp? = null,

    @get:PropertyName("trgterIndvdlNmArray") @set:PropertyName("trgterIndvdlNmArray")
    var trgterIndvdlNmArray: @RawValue Any? = null
) : Parcelable {
    fun getIntrsThemaNmList(): List<String> {
        return when (intrsThemaNmArray) {
            is String -> listOf(intrsThemaNmArray as String)
            is List<*> -> (intrsThemaNmArray as List<*>).filterIsInstance<String>()
            else -> emptyList()
        }
    }
    fun getLifeNmList(): List<String> {
        return when (lifeNmArray) {
            is String -> listOf(lifeNmArray as String)
            is List<*> -> (lifeNmArray as List<*>).filterIsInstance<String>()
            else -> emptyList()
        }
    }
    fun getTrgterIndvdlNmList(): List<String> {
        return when (trgterIndvdlNmArray) {
            is String -> listOf(trgterIndvdlNmArray as String)
            is List<*> -> (trgterIndvdlNmArray as List<*>).filterIsInstance<String>()
            else -> emptyList()
        }
    }
}