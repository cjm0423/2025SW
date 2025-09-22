package com.example.exitsw.util

import com.example.exitsw.R
import com.example.exitsw.data.LocalWelfareServiceDto

object PolicyIconMapper {
    fun getIconResourceId(item: LocalWelfareServiceDto): Int {
        val searchableText = "${item.intrsThemaNmArray ?: ""} ${item.servNm ?: ""}"

        return when {
            searchableText.contains("장애인") -> R.drawable.ic_category_disabled_support
            searchableText.contains("서민") && searchableText.contains("금융") -> R.drawable.ic_category_common_peoples_finance
            searchableText.containsAny("임신", "출산", "신생아", "산모", "육아", "난임", "산후") -> R.drawable.ic_category_pregnancy_childbirth
            searchableText.containsAny("일자리", "고용", "취업", "창업", "근로", "인턴", "실업", "구직", "직업") -> R.drawable.ic_category_employment_job
            searchableText.containsAny("교육", "보육", "학교", "자녀", "청소년", "아동", "학습", "입학", "장학", "급식", "어린이") -> R.drawable.ic_category_childcare_education
            searchableText.containsAny("의료", "건강", "병원", "보건", "질병", "진료", "수술", "약", "간병", "검진") -> R.drawable.ic_category_medical_support
            searchableText.containsAny("주거", "주택", "임대", "전세", "월세", "보증금", "공공주택", "집수리") -> R.drawable.ic_category_housing_support
            searchableText.containsAny("돌봄", "요양", "노인", "어르신", "경로", "교통약자", "이동지원") -> R.drawable.ic_category_care_support
            searchableText.containsAny("문화", "여가", "체육", "스포츠", "예술", "관람", "공연", "도서", "체험") -> R.drawable.ic_category_culture_leisure
            searchableText.containsAny("금융", "대출", "자금", "법률", "유공자", "보훈", "위로금", "의사상자", "수당", "채무", "소송") -> R.drawable.ic_category_finance_legal
            searchableText.containsAny("보호", "안전", "폭력", "학대", "범죄", "재난", "재해", "이재민", "구호", "위기") -> R.drawable.ic_category_protection_safety
            searchableText.containsAny("생계", "기초", "생활", "한부모", "수급자", "노숙인", "행려자", "장제비", "식품", "식사", "반찬", "기저귀", "분유") -> R.drawable.ic_category_livelihood_support
            else -> R.drawable.ic_category_etc
        }
    }

    private fun String.containsAny(vararg keywords: String): Boolean {
        return keywords.any { this.contains(it) }
    }
}