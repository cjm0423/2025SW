package com.example.exitsw.saving

import com.example.exitsw.R

class BankLogoUtil {
    fun getBankLogoRes(bankName: String?): Int {
        val key = bankName?.trim()?.lowercase().orEmpty()
        return when {
            key.contains("우리") -> R.drawable.ic_logo_woori
            key.contains("국민") || key.contains("kb") -> R.drawable.ic_logo_kb
            key.contains("신한") -> R.drawable.ic_logo_shinhan
            key.contains("하나") -> R.drawable.ic_logo_hana
            key.contains("기업") || key.contains("ibk") -> R.drawable.ic_logo_ibk
            key.contains("농협") || key.contains("nh") -> R.drawable.ic_logo_nh
            key.contains("스탠다드") || key.contains("차타드") || key.contains("sc") -> R.drawable.ic_logo_sc
            key.contains("카카오") -> R.drawable.ic_logo_kakao1
            key.contains("토스") -> R.drawable.ic_logo_toss
            key.contains("케이") || key.contains("케이뱅크") || key.contains("kbank") -> R.drawable.ic_logo_kbank
            key.contains("아이엠") -> R.drawable.ic_logo_im
            key.contains("전북") -> R.drawable.ic_logo_jb
            key.contains("경남") -> R.drawable.ic_logo_gn
            key.contains("한국산업")-> R.drawable.ic_logo_kdb
            key.contains("수협")-> R.drawable.ic_logo_sh
            key.contains("부산") -> R.drawable.ic_logo_bs
            key.contains("광주") -> R.drawable.ic_logo_gj
            key.contains("제주")-> R.drawable.ic_logo_jeju

            else -> R.drawable.ic_defaultimg
        }
    }
}