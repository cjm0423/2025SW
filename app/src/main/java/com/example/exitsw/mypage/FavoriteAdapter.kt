package com.example.exitsw.mypage

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.exitsw.data.LocalWelfareServiceDto
import com.example.exitsw.databinding.ItemMypageFavoriteBinding

class FavoriteAdapter(
    private val onItemClick: (LocalWelfareServiceDto) -> Unit
) : ListAdapter<LocalWelfareServiceDto, FavoriteAdapter.VH>(DIFF) {

    class VH(
        private val binding: ItemMypageFavoriteBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LocalWelfareServiceDto, onItemClick: (LocalWelfareServiceDto) -> Unit) {
            binding.textTitle.text = item.servNm
            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemMypageFavoriteBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position), onItemClick)
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<LocalWelfareServiceDto>() {
            override fun areItemsTheSame(
                oldItem: LocalWelfareServiceDto,
                newItem: LocalWelfareServiceDto
            ) = oldItem.servId == newItem.servId

            override fun areContentsTheSame(
                oldItem: LocalWelfareServiceDto,
                newItem: LocalWelfareServiceDto
            ) = oldItem == newItem
        }
    }
}
