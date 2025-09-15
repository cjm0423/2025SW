package com.example.exitsw.data

object PolicyMapper {

    fun fromFirestore(doc: Map<String, Any>): Policy {
        return Policy(
            name = doc["name"] as? String
                ?: doc["정책명"] as? String
                ?: "이름 없음",

            description = doc["description"] as? String
                ?: doc["지원내용"] as? String
                ?: doc["내용"] as? String
                ?: "",

            ageMin = (doc["ageMin"] as? Long)?.toInt()
                ?: parseAgeMin(doc["지원대상"] as? String)
                ?: 0,

            ageMax = (doc["ageMax"] as? Long)?.toInt()
                ?: parseAgeMax(doc["지원대상"] as? String)
                ?: 100,

            region = doc["region"] as? String
                ?: doc["지역"] as? String
                ?: "전국",

            incomeBracket = (doc["incomeBracket"] as? Long)?.toInt()
                ?: parseIncome(doc["지원대상"] as? String)
                ?: 10
        )
    }

    private fun parseAgeMin(text: String?): Int? {
        if (text == null) return null
        val regex = """(\d{1,2})세 이상""".toRegex()
        return regex.find(text)?.groupValues?.get(1)?.toInt()
    }

    private fun parseAgeMax(text: String?): Int? {
        if (text == null) return null
        val regex = """(\d{1,2})세 이하""".toRegex()
        return regex.find(text)?.groupValues?.get(1)?.toInt()
    }

    private fun parseIncome(text: String?): Int? {
        if (text == null) return null
        val regex = """소득\s*(\d)분위""".toRegex()
        return regex.find(text)?.groupValues?.get(1)?.toInt()
    }
}
