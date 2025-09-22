package com.example.exitsw.util

import com.example.exitsw.R

object RegionIconMapper {
    fun getIconResourceId(regionName: String?): Int {
        return when (regionName) {
            "서울시" -> R.drawable.ic_category_local_seoul
            "경기도" -> R.drawable.ic_category_local_gyeonggi
            "강원도" -> R.drawable.ic_category_local_gangwon
            "충청북도" -> R.drawable.ic_category_local_chungbuk
            "충청남도" -> R.drawable.ic_category_local_chungnam
            "전라북도" -> R.drawable.ic_category_local_jeonbuk
            "전라남도" -> R.drawable.ic_category_local_jeonnam
            "경상북도" -> R.drawable.ic_category_local_gyeongbuk
            "경상남도" -> R.drawable.ic_category_local_gyeongnam
            "제주도" -> R.drawable.ic_category_local_jeju
            "제주특별자치도" -> R.drawable.ic_category_local_jeju
            else -> R.drawable.ic_category_etc
        }
    }
}