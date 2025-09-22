package com.example.exitsw.data

import kotlin.math.ln

/* ========================= 유틸 ========================= */

/** 조회수 문자열 → 0~100 대략 정규화 (지금은 사용처 없음. 보관) */
private fun normalizePopularity(inqStr: String?): Double {
    val v = inqStr?.toDoubleOrNull() ?: 0.0
    return (20.0 * ln(1.0 + v)).coerceIn(0.0, 100.0)
}

/** 텍스트에서 나이 범위 추정 (없으면 기본 0~100) */
private fun guessAgeRange(
    lifeTags: List<String>,
    description: String?
): Pair<Int, Int> {
    val text = (description ?: "").replace(" ", "")

    // 숫자 패턴 우선: "만19세이상", "만34세이하", "18~39세"
    val r3 = Regex("""만?(\d{1,2})세?~만?(\d{1,2})세""").find(text)?.let {
        it.groupValues[1].toInt() to it.groupValues[2].toInt()
    }
    if (r3 != null) return r3

    val r1 = Regex("""만?(\d{1,2})세이상""").find(text)?.groupValues?.getOrNull(1)?.toIntOrNull()
    val r2 = Regex("""만?(\d{1,2})세이하""").find(text)?.groupValues?.getOrNull(1)?.toIntOrNull()
    if (r1 != null && r2 != null) return r1 to r2
    if (r1 != null) return r1 to 100
    if (r2 != null) return 0 to r2

    // 숫자 없으면 lifeTags로 대략 추정
    return when {
        lifeTags.any { it.contains("청소년") } -> 13 to 18
        lifeTags.any { it.contains("청년") }   -> 19 to 34
        lifeTags.any { it.contains("중장년") } -> 35 to 64
        lifeTags.any { it.contains("노년") || it.contains("고령") } -> 65 to 100
        else -> 0 to 100
    }
}

/** 텍스트에서 소득 구간 상한(%) 대략 추정 → Policy.incomeBracket(Int)로 사용 */
private fun guessIncomeBracketPercent(
    description: String?,
    trgterTags: List<String>
): Int {
    val text = ((description ?: "") + " " + trgterTags.joinToString(",")).lowercase()

    // 명시 퍼센트 우선 (ex: 중위소득 50% 이하)
    Regex("""중위소득\s*(\d{1,3})\s*%?\s*이하""").find(text)?.let {
        return it.groupValues[1].toInt().coerceIn(0, 100)
    }
    Regex("""중위소득\s*(\d{1,3})\s*%?\s*미만""").find(text)?.let {
        return (it.groupValues[1].toInt() - 1).coerceIn(0, 100)
    }
    Regex("""중위소득\s*(\d{1,3})\s*%?\s*이상""").find(text)?.let {
        // '이상'이면 상한이 넓으니 100으로 취급
        return 100
    }

    // 키워드 기반 휴리스틱
    return when {
        "수급자" in text || "기초" in text -> 30   // 매우 저소득
        "차상위" in text -> 50
        "저소득" in text -> 60
        else -> 100
    }
}

/* ========================= 확장 함수 ========================= */

/** 광역만 사용하는 매핑: LocalWelfareServiceDto -> Policy */
fun LocalWelfareServiceDto.toDomainProvinceOnly(): Policy {
    val region = if (!ctpvNm.isNullOrBlank()) ctpvNm!! else "전국"

    val lifeTags = getLifeNmList()
    val trgterTags = getTrgterIndvdlNmList()
    // intrsThema는 여기선 사용 X. 필요하면 description에 덧붙여 사용 가능.

    val (ageMin, ageMax) = guessAgeRange(lifeTags, servDgst)
    val incomeBracket = guessIncomeBracketPercent(servDgst, trgterTags)

    return Policy(
        name = servNm ?: "",
        description = servDgst ?: "",
        ageMin = ageMin,
        ageMax = ageMax,
        region = region,
        incomeBracket = incomeBracket
    )
}

/** 리스트 응답 -> Policy 리스트 (광역만) */
fun LocalWelfareListResponse.toDomainListProvinceOnly(): List<Policy> =
    servList.map { it.toDomainProvinceOnly() }

/** (옵션) 시군구까지 고려하는 일반 매핑: 필요 시 사용 */
fun LocalWelfareServiceDto.toDomain(): Policy {
    val region = when {
        !sggNm.isNullOrBlank() -> sggNm!!
        !ctpvNm.isNullOrBlank() -> ctpvNm!!
        else -> "전국"
    }

    val lifeTags = getLifeNmList()
    val trgterTags = getTrgterIndvdlNmList()

    val (ageMin, ageMax) = guessAgeRange(lifeTags, servDgst)
    val incomeBracket = guessIncomeBracketPercent(servDgst, trgterTags)

    return Policy(
        name = servNm ?: "",
        description = servDgst ?: "",
        ageMin = ageMin,
        ageMax = ageMax,
        region = region,
        incomeBracket = incomeBracket
    )
}
