package com.example.exitsw

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RecommendAdapter(
    private val items: List<RecommendItem>
) : RecyclerView.Adapter<RecommendAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val img: ImageView = view.findViewById(R.id.itemImage)
        val title: TextView = view.findViewById(R.id.itemTitle)
        val bank: TextView = view.findViewById(R.id.itemBank)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_card, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        // Glide 등으로 이미지 로딩
        holder.title.text = item.title
        holder.bank.text  = item.bank
    }

    override fun getItemCount() = items.size
}
