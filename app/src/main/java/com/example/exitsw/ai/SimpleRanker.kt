package com.example.exitsw.ai

import com.google.firebase.firestore.DocumentSnapshot
import kotlin.math.exp

object SimpleRanker {
    // 문자열 "a,b,c" -> ["a","b","c"]
    private fun splitCsv(s: String?): List<String> =
        s?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()

    fun score(p: UserProfile, doc: DocumentSnapshot): Pair<Double,List<String>> {
        var s = 0.0
        val reasons = mutableListOf<String>()

        val life = splitCsv(doc.getString("lifeNmArray"))               // 연령대 키워드
        val target = splitCsv(doc.getString("trgterIndvdlNmArray"))     // 성별/대상 키워드
        val thema = splitCsv(doc.getString("intrsThemaNmArray"))        // (프로젝트에서 소득/주제 등 혼재)
        val regionName = doc.getString("ctpvNm").orEmpty()              // 지역 문자열

        // 1) 연령대 매칭
        SimpleNlu.ageBand(p.age)?.let { band ->
            if (life.any { it.contains(band) }) { s += 0.6; reasons.add("연령대 일치") }
        }

        // 2) 성별 매칭 (타겟 비우거나 '무관' 비슷 키워드가 있으면 패스)
        p.gender?.let { g ->
            val ok = target.isEmpty() || target.any { it.contains("무관") || it.contains(g) }
            if (ok) { s += 0.2; reasons.add("성별 적합") }
        }

        // 3) 지역 매칭 (부분 포함 허용)
        p.region?.let { r ->
            if (regionName.contains(r) || r.contains(regionName)) { s += 0.8; reasons.add("지역 일치") }
        }

        // 4) 소득/대상 키워드 (intrsThemaNmArray에 종종 포함됨 → 완벽하진 않아도 가점)
        p.income?.let { ic ->
            if (thema.any { it.contains(ic) }) { s += 0.2; reasons.add("소득 조건 적합") }
        }
        // 저소득/기초생활/차상위 등 키워드 힌트 가점
        if (thema.any { it.contains("저소득") || it.contains("차상위") || it.contains("기초생활") }) {
            reasons.add("취약계층 대상"); s += 0.05
        }

        // 5) 신선도(있으면 가점) – 없으면 0 처리
        val upd = (doc.getLong("updatedAt") ?: 0L).toDouble()
        if (upd > 0.0) {
            val now = System.currentTimeMillis()/1000.0
            s += 0.05 * exp(- (now - upd) / (90*24*3600.0))
        }

        return s to reasons
    }
}
