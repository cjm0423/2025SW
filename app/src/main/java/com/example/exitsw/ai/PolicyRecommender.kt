package com.example.exitsw.ai

import com.example.exitsw.data.Policy

object PolicyRecommender {
    fun buildPrompt(
        gender: String,
        age: Int,
        region: String,
        incomeBracket: Int,
        policies: List<Policy>
    ): String {
        return """
당신은 대한민국 복지 정책을 추천하는 AI 챗봇입니다.  
아래 사용자 정보를 참고하여 Firestore에서 제공된 정책 중 가장 적합한 3개 정책을 추천하세요.  
각 정책은 **정책명 + 간단한 설명 + 추천 이유**를 포함해야 합니다.  

[사용자 정보]
- 성별: $gender
- 나이: $age
- 지역: $region
- 소득분위: $incomeBracket

[정책 후보 데이터]
${policies.joinToString("\n") { "- ${it.name}: ${it.description}" }}
""".trimIndent()
    }
}
