package com.example.exitsw.mypage

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.exitsw.R
import android.widget.RatingBar

class MypageAdapter(
    private val onClick: (ProductItem) -> Unit
) : ListAdapter<ProductItem, MypageAdapter.ViewHolder>(diffUtil) {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: ProductItem) {
            itemView.findViewById<TextView>(R.id.textSavingName).text = item.name
            itemView.findViewById<TextView>(R.id.textBankName).text = item.bank
            itemView.findViewById<TextView>(R.id.textOnelineEx).text = item.description
            itemView.findViewById<RatingBar>(R.id.ratingBar).rating = item.rating
            itemView.setOnClickListener { onClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_saving_product, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        val diffUtil = object : DiffUtil.ItemCallback<ProductItem>() {
            override fun areItemsTheSame(oldItem: ProductItem, newItem: ProductItem): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: ProductItem, newItem: ProductItem): Boolean =
                oldItem == newItem
        }
    }
}