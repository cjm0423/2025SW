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
import androidx.recyclerview.widget.RecyclerView

class ChatMessageAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<ChatItem>()

    fun addItem(item: ChatItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)
    }

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is ChatItem.UserMessage -> 1
        is ChatItem.BotMessage -> 2
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
                }
                val container = FrameLayout(ctx).apply {
                    layoutParams = RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
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
                    val params = layoutParams as FrameLayout.LayoutParams
                    params.gravity = Gravity.END
                    layoutParams = params
                }
            }
            is ChatItem.BotMessage -> {
                (holder as MessageVH).tv.apply {
                    text = item.text
                    val params = layoutParams as FrameLayout.LayoutParams
                    params.gravity = Gravity.START
                    layoutParams = params
                }
            }
            is ChatItem.PolicyMessage -> {
                (holder as PolicyVH).bind(item)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    class MessageVH(root: FrameLayout, val tv: TextView) : RecyclerView.ViewHolder(root)

    class PolicyVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title = itemView.findViewById<TextView>(R.id.tvPolicyTitle)
        private val desc = itemView.findViewById<TextView>(R.id.tvPolicyDesc)
        private val linkBtn = itemView.findViewById<TextView>(R.id.btnPolicyLink)
        private val imgLink = itemView.findViewById<ImageView>(R.id.imgLink)

        fun bind(item: ChatItem.PolicyMessage) {
            title.text = item.title
            desc.text = item.desc
            val clickListener = View.OnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.link))
                itemView.context.startActivity(intent)
            }
            linkBtn.setOnClickListener(clickListener)
            imgLink.setOnClickListener(clickListener)
        }
    }
}
