package com.example.exitsw.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.exitsw.R
import com.example.exitsw.data.LocalWelfareServiceDto
import com.example.exitsw.databinding.ItemHomeCardBinding
import com.example.exitsw.util.PolicyIconMapper

class HomeCardAdapter(private val onItemClicked: (LocalWelfareServiceDto) -> Unit) : ListAdapter<LocalWelfareServiceDto, HomeCardAdapter.HomeCardViewHolder>(DiffCallback) {

    inner class HomeCardViewHolder(private val binding: ItemHomeCardBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                onItemClicked(getItem(adapterPosition))
            }
        }

        fun bind(item: LocalWelfareServiceDto) {
            binding.textCardTitle.text = item.servNm ?: "정보 없음"
            binding.textCardSubtitle.text = item.bizChrDeptNm ?: ""

            if (item.bizChrDeptNm == "정책 목록 보기") {
                binding.imgCard.setImageResource(R.drawable.ic_ai)
            } else {
                val iconResId = PolicyIconMapper.getIconResourceId(item)
                binding.imgCard.setImageResource(iconResId)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeCardViewHolder {
        val binding = ItemHomeCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HomeCardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HomeCardViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<LocalWelfareServiceDto>() {
            override fun areItemsTheSame(oldItem: LocalWelfareServiceDto, newItem: LocalWelfareServiceDto): Boolean {
                return oldItem.servId == newItem.servId
            }
            override fun areContentsTheSame(oldItem: LocalWelfareServiceDto, newItem: LocalWelfareServiceDto): Boolean {
                return oldItem == newItem
            }
        }
    }
}