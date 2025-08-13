package com.example.exitsw.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.exitsw.data.LocalWelfareServiceDto
import com.example.exitsw.databinding.ItemHomeCardBinding

// ✨ [변경] 생성자에 클릭 리스너를 받도록 추가
class HomeCardAdapter(private val onItemClicked: (LocalWelfareServiceDto) -> Unit) : ListAdapter<LocalWelfareServiceDto, HomeCardAdapter.HomeCardViewHolder>(DiffCallback) {

    inner class HomeCardViewHolder(private val binding: ItemHomeCardBinding) : RecyclerView.ViewHolder(binding.root) {
        // ✨ [추가] ViewHolder가 생성될 때 클릭 리스너를 설정
        init {
            itemView.setOnClickListener {
                // 현재 위치의 아이템을 클릭 리스너에 전달
                onItemClicked(getItem(adapterPosition))
            }
        }

        fun bind(item: LocalWelfareServiceDto) {
            binding.textCardTitle.text = item.serviceName ?: "정보 없음"
            binding.textCardSubtitle.text = item.department ?: ""
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
                return oldItem.serviceId == newItem.serviceId
            }
            override fun areContentsTheSame(oldItem: LocalWelfareServiceDto, newItem: LocalWelfareServiceDto): Boolean {
                return oldItem == newItem
            }
        }
    }
}
