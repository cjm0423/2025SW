package com.example.exitsw.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.exitsw.data.LocalWelfareServiceDto
import com.example.exitsw.databinding.ItemPolicyBinding

// ✨ [변경] 클릭 시 LocalWelfareServiceDto 객체를 전달하도록 변경
class PolicyAdapter(private val onItemClicked: (LocalWelfareServiceDto) -> Unit) : ListAdapter<LocalWelfareServiceDto, PolicyAdapter.PolicyViewHolder>(DiffCallback) {

    inner class PolicyViewHolder(private val binding: ItemPolicyBinding) : RecyclerView.ViewHolder(binding.root) {
        // ✨ [추가] ViewHolder가 생성될 때 클릭 리스너를 설정
        init {
            itemView.setOnClickListener {
                onItemClicked(getItem(adapterPosition))
            }
        }
        fun bind(policy: LocalWelfareServiceDto) {
            binding.textPolicyName.text = policy.serviceName ?: "정보 없음"
            binding.textPolicyAgency.text = policy.department ?: ""
            binding.textPolicySummary.text = policy.summary ?: ""
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
                return oldItem.serviceId == newItem.serviceId
            }
            override fun areContentsTheSame(oldItem: LocalWelfareServiceDto, newItem: LocalWelfareServiceDto): Boolean {
                return oldItem == newItem
            }
        }
    }
}
