package com.example.exitsw.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.exitsw.data.LocalWelfareServiceDto
import com.example.exitsw.databinding.ItemHomeCardBinding

class HomeCardAdapter : ListAdapter<LocalWelfareServiceDto, HomeCardAdapter.HomeCardViewHolder>(DiffCallback) {

    // ViewHolder: 각 카드 아이템의 뷰를 보관하는 홀더
    inner class HomeCardViewHolder(private val binding: ItemHomeCardBinding) : RecyclerView.ViewHolder(binding.root) {
        // 데이터를 뷰에 바인딩하는 함수
        fun bind(item: LocalWelfareServiceDto) {
            binding.textCardTitle.text = item.serviceName
            binding.textCardSubtitle.text = item.department
            // TODO: 이미지 로딩 라이브러리(Glide, Coil 등)를 사용해 이미지 설정
        }
    }

    // ViewHolder가 생성될 때 호출
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeCardViewHolder {
        val binding = ItemHomeCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HomeCardViewHolder(binding)
    }

    // ViewHolder에 데이터가 바인딩될 때 호출
    override fun onBindViewHolder(holder: HomeCardViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // DiffUtil: 리스트가 변경될 때 효율적으로 업데이트하기 위한 콜백
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
