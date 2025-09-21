// app/src/main/java/com/example/exitsw/domain/RecommendationEngine.kt
package com.example.exitsw.domain

import com.example.exitsw.data.Policy
import com.example.exitsw.domain.model.UserProfile

object RecommendationEngine {
    data class Weights(
        val wRegion: Double = 3.0,
        val wAge: Double = 2.0,
        val wIncome: Double = 2.0,
        val wInterest: Double = 1.0
    )

    fun score(p: Policy, u: UserProfile, w: Weights = Weights()): Double {
        var s = 0.0

        // ✅ 지역 명칭 정규화해서 비교
        val pr = normRegion(p.region)
        val ur = normRegion(u.regionLabel)
        if (pr == "전국" || pr == ur) s += w.wRegion

        // 나이/소득 동일
        if (u.ageYears in p.ageMin..p.ageMax) s += w.wAge
        val userIncomePct = parseIncomeBracketPercent(u.income) ?: 100
        if (userIncomePct <= p.incomeBracket) s += w.wIncome

        return s
    }

    private fun normRegion(s: String?): String {
        val t = s?.trim().orEmpty()
        return when {
            t.contains("서울") -> "서울시"
            t.contains("경기") -> "경기도"
            t.contains("강원") -> "강원도"
            t.contains("충북") || t.contains("충청북도") -> "충청북도"
            t.contains("충남") || t.contains("충청남도") -> "충청남도"
            t.contains("전북") || t.contains("전라북") || t.contains("특별자치도") && t.contains("전북") -> "전라북도"
            t.contains("전남") || t.contains("전라남") -> "전라남도"
            t.contains("경북") || t.contains("경상북") -> "경상북도"
            t.contains("경남") || t.contains("경상남") -> "경상남도"
            t.contains("제주") -> "제주도"
            t == "전국" || t.isBlank() || t == "-" -> "전국"
            else -> t
        }
    }

    /**
     * 사용자 소득 라벨 → 상한 % (예: "0~30%" -> 30, "중위소득 50% 이하" -> 50, "6분위" -> 60)
     */
    internal fun parseIncomeBracketPercent(label: String?): Int? {
        if (label.isNullOrBlank()) return null

        // "0~30%" / "30~50%"
        Regex("""(\d{1,3})\s*~\s*(\d{1,3})\s*%""").find(label)?.let {
            val b = it.groupValues[2].toInt()
            return b.coerceIn(0, 100)
        }
        // "중위소득 50% 이하"
        Regex("""중위소득\s*(\d{1,3})\s*%?\s*이하""").find(label)?.let {
            return it.groupValues[1].toInt().coerceIn(0, 100)
        }
        // "70%" 형태
        Regex("""(\d{1,3})\s*%""").find(label)?.let {
            return it.groupValues[1].toInt().coerceIn(0, 100)
        }
        // "6분위" → 60, "8분위" → 80
        Regex("""(\d{1,2})\s*분위""").find(label)?.let {
            val n = it.groupValues[1].toInt()
            return (n * 10).coerceIn(0, 100)
        }

        return null
    }
}
