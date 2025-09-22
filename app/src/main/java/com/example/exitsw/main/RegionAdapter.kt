package com.example.exitsw.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.exitsw.data.RegionInfo
import com.example.exitsw.databinding.ItemRegionBinding
import com.example.exitsw.util.RegionIconMapper

class RegionAdapter(private val onItemClicked: (RegionInfo) -> Unit) : ListAdapter<RegionInfo, RegionAdapter.RegionViewHolder>(DiffCallback) {

    inner class RegionViewHolder(private val binding: ItemRegionBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                onItemClicked(getItem(adapterPosition))
            }
        }
        fun bind(regionInfo: RegionInfo) {
            binding.textRegionName.text = regionInfo.name

            val iconResId = RegionIconMapper.getIconResourceId(regionInfo.name)
            binding.imgRegion.setImageResource(iconResId)

            when (val count = regionInfo.policyCount) {
                null -> {
                    binding.textPolicyCount.text = "개수 확인 중..."
                    binding.textPolicyCount.setTextColor(itemView.context.getColor(android.R.color.darker_gray))
                }
                -1 -> {
                    binding.textPolicyCount.text = "연결 실패"
                    binding.textPolicyCount.setTextColor(itemView.context.getColor(android.R.color.holo_red_dark))
                }
                else -> {
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