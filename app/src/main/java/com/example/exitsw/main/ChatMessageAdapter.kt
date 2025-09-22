package com.example.exitsw

import android.content.Intent
import android.net.Uri
import android.text.method.LinkMovementMethod
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView

class ChatMessageAdapter(
    /** 정책 카드 클릭 시 내부 화면으로 연결하기 위한 콜백 (없으면 외부 브라우저로 폴백) */
    private val onPolicyClick: ((ChatItem.PolicyMessage) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<ChatItem>()

    fun addItem(item: ChatItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)
    }

    /** ✅ 새 추천 시작 시 이전 메시지 전부 삭제 */
    fun clearAll() {
        if (items.isEmpty()) return
        val size = items.size
        items.clear()
        notifyItemRangeRemoved(0, size)
    }

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is ChatItem.UserMessage   -> 1
        is ChatItem.BotMessage    -> 2
        is ChatItem.PolicyMessage -> 3
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val ctx = parent.context
        return when (viewType) {
            1, 2 -> {
                val tv = TextView(ctx).apply {
                    textSize = 16f
                    setPadding(24, 16, 24, 16)
                    movementMethod = LinkMovementMethod.getInstance()
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    )
                }
                val container = FrameLayout(ctx).apply {
                    layoutParams = RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    setPadding(12, 6, 12, 6)
                    addView(tv)
                }
                MessageVH(container, tv)
            }
            3 -> {
                val view = LayoutInflater.from(ctx)
                    .inflate(R.layout.item_policy_message, parent, false)
                PolicyVH(view)
            }
            else -> throw IllegalArgumentException("Unknown viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ChatItem.UserMessage -> {
                (holder as MessageVH).tv.apply {
                    text = item.text
                    val lp = (layoutParams as FrameLayout.LayoutParams).apply {
                        gravity = Gravity.END
                        marginStart = 48
                    }
                    layoutParams = lp
                }
            }
            is ChatItem.BotMessage -> {
                (holder as MessageVH).tv.apply {
                    text = item.text
                    val lp = (layoutParams as FrameLayout.LayoutParams).apply {
                        gravity = Gravity.START
                        marginEnd = 48
                    }
                    layoutParams = lp
                }
            }
            is ChatItem.PolicyMessage -> {
                (holder as PolicyVH).bind(item)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    class MessageVH(root: FrameLayout, val tv: TextView) : RecyclerView.ViewHolder(root)

    inner class PolicyVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title = itemView.findViewById<TextView>(R.id.tvPolicyTitle)
        private val desc = itemView.findViewById<TextView>(R.id.tvPolicyDesc)
        private val linkBtn = itemView.findViewById<TextView>(R.id.btnPolicyLink)
        private val imgLink = itemView.findViewById<ImageView>(R.id.imgLink)

        fun bind(item: ChatItem.PolicyMessage) {
            title.text = item.title
            desc.text = item.desc

            // 내부 화면으로 보낼 거라 버튼은 항상 보이게
            linkBtn.isVisible = true
            imgLink.isVisible = true

            val clickListener = View.OnClickListener {
                // 1) 내부 화면 콜백 우선
                if (onPolicyClick != null) {
                    onPolicyClick.invoke(item)
                    return@OnClickListener
                }
                // 2) 폴백: 콜백이 없으면 외부 브라우저로 링크 열기
                val link = item.link
                if (!link.isNullOrBlank()) {
                    runCatching {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                        itemView.context.startActivity(intent)
                    }
                }
            }
            linkBtn.setOnClickListener(clickListener)
            imgLink.setOnClickListener(clickListener)
        }
    }
}
