package com.example.exitsw.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.exitsw.data.RegionInfo
import com.example.exitsw.databinding.ItemRegionBinding

class RegionAdapter(private val onItemClicked: (RegionInfo) -> Unit) : ListAdapter<RegionInfo, RegionAdapter.RegionViewHolder>(DiffCallback) {

    inner class RegionViewHolder(private val binding: ItemRegionBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                onItemClicked(getItem(adapterPosition))
            }
        }
        fun bind(regionInfo: RegionInfo) {
            binding.textRegionName.text = regionInfo.name

            // ✨ [핵심 수정] policyCount의 상태에 따라 다른 텍스트를 표시
            when (val count = regionInfo.policyCount) {
                null -> { // null이면 로딩 중
                    binding.textPolicyCount.text = "개수 확인 중..."
                    binding.textPolicyCount.setTextColor(itemView.context.getColor(android.R.color.darker_gray))
                }
                -1 -> { // -1이면 연결 실패
                    binding.textPolicyCount.text = "연결 실패"
                    binding.textPolicyCount.setTextColor(itemView.context.getColor(android.R.color.holo_red_dark))
                }
                else -> { // 0 이상이면 실제 개수
                    binding.textPolicyCount.text = "${count}개 정책"
                    binding.textPolicyCount.setTextColor(itemView.context.getColor(android.R.color.holo_blue_dark))
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RegionViewHolder {
        val binding = ItemRegionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RegionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RegionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<RegionInfo>() {
            override fun areItemsTheSame(oldItem: RegionInfo, newItem: RegionInfo): Boolean {
                return oldItem.name == newItem.name
            }
            override fun areContentsTheSame(oldItem: RegionInfo, newItem: RegionInfo): Boolean {
                return oldItem == newItem
            }
        }
    }
}
