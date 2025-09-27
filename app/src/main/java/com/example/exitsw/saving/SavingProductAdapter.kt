package com.example.exitsw

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
//import coil.load // optional: Coil 사용 시 (동적 favicon 사용하려면 추가)

class SavingProductAdapter(
    private val products: List<SavingProduct>,
    private val onItemClick: (SavingProduct) -> Unit // 콜백 함수
) : RecyclerView.Adapter<SavingProductAdapter.SavingViewHolder>() {

    inner class SavingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val logoImage: ImageView = itemView.findViewById(R.id.imageBankLogo)
        private val textSavingName: TextView = itemView.findViewById(R.id.textSavingName)
        private val textBankName: TextView = itemView.findViewById(R.id.textBankName)
        private val textOnelineEx: TextView = itemView.findViewById(R.id.textOnelineEx)

        fun bind(product: SavingProduct) {
            textSavingName.text = product.fin_prdt_nm
            textBankName.text = product.kor_co_nm
            textOnelineEx.text = product.etc_note ?: "설명 없음"

            val resId = getBankLogoRes(product.kor_co_nm)
            logoImage.setImageResource(resId)
            logoImage.contentDescription = "${product.kor_co_nm ?: "금융사"} 로고"

            // 클릭 이벤트
            itemView.setOnClickListener {
                onItemClick(product)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_saving_product, parent, false)
        return SavingViewHolder(view)
    }

    override fun onBindViewHolder(holder: SavingViewHolder, position: Int) {
        holder.bind(products[position])
    }

    override fun getItemCount(): Int = products.size

    companion object {
        // 은행명 키워드 기반 매핑: 필요 시 더 많은 케이스 추가
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

        // (옵션) homp_url -> favicon 경로 생성 (단순 시도)
        fun buildFaviconUrl(hompUrl: String?): String? {
            if (hompUrl.isNullOrBlank()) return null
            return try {
                val uri = android.net.Uri.parse(hompUrl)
                val host = uri.host ?: return null
                "${uri.scheme}://$host/favicon.ico"
            } catch (_: Exception) {
                null
            }
        }
    }
}