package com.example.exitsw.ai

data class UserProfile(
    val age: Int? = null,
    val gender: String? = null,   // "남성"|"여성"|null
    val region: String? = null,   // "서울특별시" 등
    val income: String? = null    // "1분위"|"2분위"|...|null
)

object SimpleNlu {
    private val AGE_RX = Regex("""(\d{1,3})\s*세""")
    private val INCOME_RX = Regex("""(\d{1,2})\s*분위""")

    fun parse(text: String): UserProfile {
        val t = text.lowercase()
        val age = AGE_RX.find(t)?.groupValues?.getOrNull(1)?.toIntOrNull()
        val gender = when {
            "남성" in text || "남자" in text || " 남 " in " $t " -> "남성"
            "여성" in text || "여자" in text || " 여 " in " $t " -> "여성"
            else -> null
        }
        val region = when {
            "서울" in text -> "서울특별시"
            "경기" in text -> "경기도"
            "강원" in text -> "강원특별자치도"
            "충북" in text -> "충청북도"
            "충남" in text -> "충청남도"
            "전북" in text -> "전북특별자치도"
            "전남" in text -> "전라남도"
            "경북" in text -> "경상북도"
            "대구" in text -> "대구광역시"
            "경남" in text -> "경상남도"
            "부산" in text -> "부산광역시"
            "울산" in text -> "울산광역시"
            "제주" in text -> "제주특별자치도"
            else -> null
        }
        val income = INCOME_RX.find(text)?.groupValues?.getOrNull(1)?.let { "${it}분위" }
        return UserProfile(age, gender, region, income)
    }

    fun ageBand(age: Int?): String? = when (age) {
        null -> null
        in 0..19 -> "청소년"
        in 20..39 -> "청년"
        in 40..64 -> "중장년"
        else -> "노인"
    }
}
