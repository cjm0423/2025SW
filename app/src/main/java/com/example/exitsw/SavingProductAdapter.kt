package com.example.exitsw

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SavingProductAdapter(
    private val products: List<SavingProduct>,
    private val onItemClick: (SavingProduct) -> Unit // 콜백 함수 추가
) : RecyclerView.Adapter<SavingProductAdapter.SavingViewHolder>() {

    inner class SavingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textSavingName: TextView = itemView.findViewById(R.id.textSavingName)
        val textBankName: TextView = itemView.findViewById(R.id.textBankName)
        val textOnelineEx: TextView = itemView.findViewById(R.id.textOnelineEx)
        val ratingBar: RatingBar = itemView.findViewById(R.id.ratingBar)

        fun bind(product: SavingProduct) {
            textSavingName.text = product.fin_prdt_nm
            textBankName.text = product.kor_co_nm
            textOnelineEx.text = product.etc_note ?: "설명 없음"
            ratingBar.rating = 0f // 임시. 필요시 금리 등으로 조정 가능

            // ⭐ 클릭 이벤트 연결
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
}