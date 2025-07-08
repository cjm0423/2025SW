package com.example.exitsw.mypage

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.exitsw.R

class MypageAdapter(
    private val onClick: (ProductItem) -> Unit
) : ListAdapter<ProductItem, MypageAdapter.ViewHolder>(diffUtil) {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: ProductItem) {
            itemView.findViewById<TextView>(R.id.tvProductName).text = item.name
            itemView.setOnClickListener { onClick(item) }
        }
    }

    override fun onCreateViewHolder(...) = ViewHolder(...)
    override fun onBindViewHolder(...) = holder.bind(getItem(position))
}
