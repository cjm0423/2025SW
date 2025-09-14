package com.example.exitsw

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.exitsw.BuildConfig
import com.example.exitsw.databinding.FragmentChatbotBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ChatbotFragment : Fragment() {

    private var _binding: FragmentChatbotBinding? = null
    private val binding get() = _binding!!

    private val messages = mutableListOf<ChatMessage>()
    private lateinit var adapter: ChatAdapter

    private val gemini by lazy {
        GeminiClient(
            apiKey = BuildConfig.GEMINI_API_KEY,
            endpoint = BuildConfig.GEMINI_ENDPOINT, // e.g. .../gemini-1.5-flash:generateContent
            timeoutMs = 15_000L,
            maxRetries = 3,
            retryDelayMs = 3_000L // 3초 예시(429/5xx 시)
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatbotBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = ChatAdapter(messages)
        binding.rvChatMessages.apply {
            layoutManager = LinearLayoutManager(requireContext()).apply { stackFromEnd = true }
            adapter = this@ChatbotFragment.adapter
        }

        binding.btnSend.setOnClickListener { sendMessage() }
        binding.etChatInput.setOnEditorActionListener { _, actionId, event ->
            val isSend = actionId == EditorInfo.IME_ACTION_SEND ||
                    (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            if (isSend) { sendMessage(); true } else false
        }

        binding.btnChatBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun sendMessage() {
        val prompt = binding.etChatInput.text?.toString()?.trim().orEmpty()
        if (prompt.isEmpty()) return

        setInputsEnabled(false)
        append(ChatMessage.User(prompt))
        binding.etChatInput.text?.clear()

        // 타이핑 표시
        val typingIndex = append(ChatMessage.Typing)

        viewLifecycleOwner.lifecycleScope.launch {
            val reply = withContext(Dispatchers.IO) {
                try {
                    gemini.generate(prompt)
                } catch (t: Throwable) {
                    "오류가 발생했어요: ${t.localizedMessage ?: "알 수 없는 오류"}"
                }
            }

            removeAt(typingIndex)
            append(ChatMessage.Bot(reply))
            scrollBottom()
            setInputsEnabled(true)
        }
    }

    private fun setInputsEnabled(enabled: Boolean) {
        binding.etChatInput.isEnabled = enabled
        binding.btnSend.isEnabled = enabled
    }

    private fun append(msg: ChatMessage): Int {
        messages.add(msg)
        adapter.notifyItemInserted(messages.lastIndex)
        scrollBottom()
        return messages.lastIndex
    }

    private fun removeAt(index: Int) {
        if (index in messages.indices) {
            messages.removeAt(index)
            adapter.notifyItemRemoved(index)
        }
    }

    private fun scrollBottom() {
        binding.rvChatMessages.post {
            binding.rvChatMessages.scrollToPosition(adapter.itemCount - 1)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/* ----------------- 모델 ----------------- */
sealed class ChatMessage {
    data class User(val text: String) : ChatMessage()
    data class Bot(val text: String) : ChatMessage()
    data object Typing : ChatMessage()
}

/* ----------------- 어댑터(심플 텍스트 버블) ----------------- */
private class ChatAdapter(
    private val items: List<ChatMessage>
) : RecyclerView.Adapter<ChatAdapter.VH>() {

    companion object {
        private const val TYPE_USER = 1
        private const val TYPE_BOT = 2
        private const val TYPE_TYPING = 3
    }

    // 말풍선 뷰홀더: FrameLayout(가로 MATCH_PARENT) 안에 TextView(WRAP_CONTENT)
    class VH(val root: android.widget.FrameLayout, val tv: android.widget.TextView)
        : RecyclerView.ViewHolder(root)

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is ChatMessage.User -> TYPE_USER
        is ChatMessage.Bot  -> TYPE_BOT
        ChatMessage.Typing  -> TYPE_TYPING
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val ctx = parent.context

        val container = android.widget.FrameLayout(ctx).apply {
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val tv = android.widget.TextView(ctx).apply {
            setPadding(dp(ctx, 12), dp(ctx, 8), dp(ctx, 12), dp(ctx, 8))
            textSize = 16f
            // 필요 시 말풍선 배경을 지정해도 됨
            // setBackgroundResource(R.drawable.bg_bubble_user / bg_bubble_bot)
        }

        container.addView(tv)
        return VH(container, tv)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]

        // 공통: 화면 폭의 75%로 말풍선 최대 너비 제한
        val screenW = holder.itemView.resources.displayMetrics.widthPixels
        holder.tv.maxWidth = (screenW * 0.75f).toInt()

        // 정렬/마진 설정
        val lp = android.widget.FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(dp(holder.itemView.context, 8), dp(holder.itemView.context, 6),
                dp(holder.itemView.context, 8), dp(holder.itemView.context, 6))
        }

        when (item) {
            is ChatMessage.User -> {
                holder.tv.text = item.text
                lp.gravity = android.view.Gravity.END   // 👉 오른쪽 정렬
                holder.tv.layoutParams = lp
            }
            is ChatMessage.Bot -> {
                holder.tv.text = item.text
                lp.gravity = android.view.Gravity.START // 👈 왼쪽 정렬
                holder.tv.layoutParams = lp
            }
            ChatMessage.Typing -> {
                holder.tv.text = "AI가 입력 중..."
                lp.gravity = android.view.Gravity.START // 👈 왼쪽 정렬
                holder.tv.layoutParams = lp
            }
        }
    }

    override fun getItemCount(): Int = items.size

    private fun dp(ctx: android.content.Context, value: Int): Int =
        (value * ctx.resources.displayMetrics.density).toInt()
}
private class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view)

/* ----------------- Gemini HTTP 클라이언트 ----------------- */
private class GeminiClient(
    private val apiKey: String,
    private val endpoint: String,              // ".../gemini-1.5-xxx:generateContent"
    timeoutMs: Long,
    private val maxRetries: Int,
    private val retryDelayMs: Long
) {
    init {
        require(apiKey.isNotBlank()) { "GEMINI_API_KEY가 비어 있습니다." }
        require(endpoint.isNotBlank()) { "GEMINI_ENDPOINT가 비어 있습니다." }
    }

    private val client = OkHttpClient.Builder()
        .callTimeout(timeoutMs, TimeUnit.MILLISECONDS)
        .build()

    /**
     * Gemini generateContent 호출
     * 요청: { "contents": [{ "parts": [{ "text": "..." }]}] }
     * 응답: candidates[0].content.parts[0].text
     */
    suspend fun generate(prompt: String): String {
        val url = "$endpoint?key=$apiKey"
        val reqJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })
            })
        }.toString()

        val body = reqJson.toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).post(body).build()

        repeat(maxRetries) { attempt ->
            val response = client.newCall(request).execute()
            response.use { resp ->
                val bodyStr = resp.body?.string().orEmpty()
                if (resp.isSuccessful) {
                    return parseText(bodyStr)
                        ?: "응답 포맷이 예상과 다릅니다."
                }
                // 429/5xx → 재시도
                if ((resp.code == 429 || resp.code in 500..599) && attempt < maxRetries - 1) {
                    delay(retryDelayMs)
                } else {
                    return "API 오류: HTTP ${resp.code}"
                }
            }
        }
        return "최대 재시도 횟수를 초과했습니다."
    }

    private fun parseText(json: String): String? {
        // candidates[0].content.parts[0].text
        val root = JSONObject(json)
        val candidates = root.optJSONArray("candidates") ?: return null
        if (candidates.length() == 0) return null
        val content = candidates.optJSONObject(0)?.optJSONObject("content") ?: return null
        val parts = content.optJSONArray("parts") ?: return null
        if (parts.length() == 0) return null
        val text = parts.optJSONObject(0)?.optString("text")
        return text?.takeIf { it.isNotBlank() }?.trim()
    }
}
