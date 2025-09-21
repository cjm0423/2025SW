package com.example.exitsw.mypage

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.exitsw.databinding.ItemMypageFavoriteBinding

class FavoriteAdapter(
    private val onItemClick: (FavoritePolicy) -> Unit
) : ListAdapter<FavoritePolicy, FavoriteAdapter.VH>(DIFF) {

    class VH(private val binding: ItemMypageFavoriteBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: FavoritePolicy, onItemClick: (FavoritePolicy) -> Unit) {
            binding.textTitle.text = item.title
            binding.textTitle.setOnClickListener { onItemClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemMypageFavoriteBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        var item = getItem(position)
        holder.itemView.setOnClickListener{
            onItemClick(item)
        }

        holder.bind(getItem(position), onItemClick)
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<FavoritePolicy>() {
            override fun areItemsTheSame(o: FavoritePolicy, n: FavoritePolicy) = o.id == n.id
            override fun areContentsTheSame(o: FavoritePolicy, n: FavoritePolicy) = o == n
        }
    }
}
