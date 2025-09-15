package com.example.exitsw

import android.text.method.LinkMovementMethod
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView

class ChatMessageAdapter : RecyclerView.Adapter<ChatMessageAdapter.VH>() {

    private val messages = mutableListOf<ChatMessage>()

    fun addMessage(msg: ChatMessage) {
        messages.add(msg)
        notifyItemInserted(messages.size - 1)
    }

    override fun getItemViewType(position: Int): Int =
        if (messages[position].isUser) 1 else 2

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val ctx = parent.context
        val tv = TextView(ctx).apply {
            textSize = 16f
            setPadding(24, 16, 24, 16)
            movementMethod = LinkMovementMethod.getInstance() // 👈 링크 클릭 가능하게
        }

        val container = FrameLayout(ctx).apply {
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            addView(tv)
        }

        return VH(container, tv)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = messages[position]
        holder.tv.text = HtmlCompat.fromHtml(item.text, HtmlCompat.FROM_HTML_MODE_LEGACY)

        // 정렬 (사용자=오른쪽, 봇=왼쪽)
        val params = holder.tv.layoutParams as FrameLayout.LayoutParams
        params.gravity = if (item.isUser) Gravity.END else Gravity.START
        holder.tv.layoutParams = params
    }

    override fun getItemCount(): Int = messages.size

    class VH(root: FrameLayout, val tv: TextView) : RecyclerView.ViewHolder(root)
}
