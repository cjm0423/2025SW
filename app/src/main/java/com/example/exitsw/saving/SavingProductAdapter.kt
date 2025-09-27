package com.example.exitsw

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.exitsw.saving.BankLogoUtil

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

            val resId = BankLogoUtil().getBankLogoRes(product.kor_co_nm)
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