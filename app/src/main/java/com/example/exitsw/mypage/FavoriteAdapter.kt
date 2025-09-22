package com.example.exitsw.mypage

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.exitsw.databinding.ItemMypageFavoriteBinding

// FavoriteAdapter.kt 맨 위의 데이터 클래스 수정
data class FavoritePolicy(
    val servId: String = "",
    val servNm: String = "",

    // 아래는 카드 표시엔 꼭 필요 없으니 기본값을 둬서 선택적으로 받자
    val btchDeptNm: String = "",
    val ctpvNm: String = "",
    val favoritesCount: Int = 0,
    val inqNum: String = "",
    val intrsThemaNm: String = "",
    val servDgst: String = "",
    val servDtlLink: String = ""
    // 필요하면 더 추가
)


class FavoriteAdapter(
    private val onItemClick: (FavoritePolicy) -> Unit
) : ListAdapter<FavoritePolicy, FavoriteAdapter.VH>(DIFF) {

    class VH(private val binding: ItemMypageFavoriteBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: FavoritePolicy, onItemClick: (FavoritePolicy) -> Unit) {
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
        val item = getItem(position)
        holder.bind(item, onItemClick)
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<FavoritePolicy>() {
            override fun areItemsTheSame(oldItem: FavoritePolicy, newItem: FavoritePolicy) =
                oldItem.servId == newItem.servId
            override fun areContentsTheSame(oldItem: FavoritePolicy, newItem: FavoritePolicy) =
                oldItem == newItem
        }
    }
}
