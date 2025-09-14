package com.example.exitsw.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.exitsw.data.LocalWelfareServiceDto
import com.example.exitsw.databinding.ItemPolicyBinding

class PolicyAdapter(private val onItemClicked: (LocalWelfareServiceDto) -> Unit) : ListAdapter<LocalWelfareServiceDto, PolicyAdapter.PolicyViewHolder>(DiffCallback) {

    inner class PolicyViewHolder(private val binding: ItemPolicyBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                onItemClicked(getItem(adapterPosition))
            }
        }
        fun bind(policy: LocalWelfareServiceDto) {
            // [수정] DTO 클래스의 변경된 변수 이름으로 교체
            binding.textPolicyName.text = policy.servNm ?: "정보 없음"
            binding.textPolicyAgency.text = policy.bizChrDeptNm ?: ""
            binding.textPolicySummary.text = policy.servDgst ?: ""
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PolicyViewHolder {
        val binding = ItemPolicyBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PolicyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PolicyViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<LocalWelfareServiceDto>() {
            override fun areItemsTheSame(oldItem: LocalWelfareServiceDto, newItem: LocalWelfareServiceDto): Boolean {
                // [수정] DTO 클래스의 변경된 변수 이름으로 교체
                return oldItem.servId == newItem.servId
            }
            override fun areContentsTheSame(oldItem: LocalWelfareServiceDto, newItem: LocalWelfareServiceDto): Boolean {
                return oldItem == newItem
            }
        }
    }
}