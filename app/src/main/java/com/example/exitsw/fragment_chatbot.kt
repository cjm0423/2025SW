package com.example.exitsw

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.exitsw.ai.GeminiClient
import com.example.exitsw.databinding.FragmentChatbotBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.example.exitsw.main.PolicyDetailFragment
import com.example.exitsw.main.PolicyListFragment

class ChatbotFragment : Fragment() {

    private var _binding: FragmentChatbotBinding? = null
    private val binding get() = _binding!!

    private lateinit var chatAdapter: ChatMessageAdapter
    private lateinit var gemini: GeminiClient

    @Volatile private var currentSessionId: Long = 0L

    // 하한선(60점): 0.60 미만은 표시하지 않음
    private val MIN_LLM_SCORE = 0.60

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatbotBinding.inflate(inflater, container, false)

        // Gemini
        gemini = GeminiClient(
            apiKey = BuildConfig.GEMINI_API_KEY,
            generateEndpoint = BuildConfig.GEMINI_ENDPOINT
        )

        // RecyclerView (정책 카드 클릭 시: Firestore에서 실제 정책 찾아 Detail로 연결)
        chatAdapter = ChatMessageAdapter { policy ->
            val db = FirebaseFirestore.getInstance()
            val title = policy.title.trim()

            // 1) servNm = title 매칭으로 1건 조회
            db.collection("policies").document("all")
                .collection("items")
                .whereEqualTo("servNm", title)
                .limit(1)
                .get()
                .addOnSuccessListener { snap ->
                    val doc = snap.documents.firstOrNull()
                    val dto = doc?.toObject(com.example.exitsw.data.LocalWelfareServiceDto::class.java)

                    if (dto != null) {
                        // 실제 DTO로 앱 내부 상세 열기
                        val detail = PolicyDetailFragment.newInstance(dto)
                        parentFragmentManager.beginTransaction()
                            .setReorderingAllowed(true)
                            .add(R.id.fragment_container, detail, "PolicyDetail")
                            .hide(this) // 챗봇은 숨겨 상태 유지 (뒤로가기 시 리스트/챗 기록 보존)
                            .addToBackStack("PolicyDetail")
                            .commit()
                    } else {
                        // 2) 폴백: 리스트 화면으로 이동해 제목으로 포커스 + 자동 상세 열기
                        val listFrag = PolicyListFragment.newInstanceForDeeplink(
                            regionName = null,
                            filterLink = policy.link,      // 링크가 Firestore와 매칭되면 이걸로도 잡힘
                            filterTitle = title,           // 제목 기준 포커스
                            autoOpenDetail = true
                        )
                        parentFragmentManager.beginTransaction()
                            .setReorderingAllowed(true)
                            .add(R.id.fragment_container, listFrag, "PolicyListDeeplink")
                            .hide(this)
                            .addToBackStack("PolicyListDeeplink")
                            .commit()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "정책 조회 중 오류: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        binding.rvChatMessages.layoutManager = LinearLayoutManager(requireContext())
        binding.rvChatMessages.adapter = chatAdapter

        // (미래용) 숨겨둔 고급필터 위젯 연결만
        ArrayAdapter.createFromResource(
            requireContext(), R.array.gender_options, android.R.layout.simple_spinner_item
        ).also { ad -> ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); binding.spGender.adapter = ad }
        ArrayAdapter.createFromResource(
            requireContext(), R.array.kor_regions, android.R.layout.simple_spinner_item
        ).also { ad -> ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); binding.spRegion.adapter = ad }
        ArrayAdapter.createFromResource(
            requireContext(), R.array.income_brackets, android.R.layout.simple_spinner_item
        ).also { ad -> ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); binding.spIncome.adapter = ad }

        // 엔터 → 키보드 닫기
        binding.etUserText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) { hideKeyboard(binding.etUserText); true } else false
        }

        // 추천 버튼
        binding.btnRecommend.setOnClickListener {
            hideKeyboard(binding.etUserText)
            val query = binding.etUserText.text?.toString()?.trim().orEmpty()

            currentSessionId = System.currentTimeMillis()
            val sessionId = currentSessionId

            chatAdapter.clearAll()
            chatAdapter.addItem(ChatItem.BotMessage("새 추천 세션을 시작합니다."))

            if (query.isEmpty()) {
                chatAdapter.addItem(ChatItem.BotMessage("자유 입력에 조건을 적어주세요. 예) 부산 거주 20대 여성 저소득층 정책 추천"))
                return@setOnClickListener
            }
            recommendByLLM(query, topK = 10, sessionId = sessionId)
        }

        return binding.root
    }

    /** AI-전용 추천 */
    private fun recommendByLLM(userQuery: String, topK: Int, sessionId: Long) {
        val db = FirebaseFirestore.getInstance()
        chatAdapter.addItem(ChatItem.BotMessage("AI가 조건을 해석하고 정책을 평가 중…"))

        val hints = parseUserHints(userQuery)

        db.collection("policies").document("all").collection("items")
            .get()
            .addOnSuccessListener { result ->
                if (sessionId != currentSessionId) return@addOnSuccessListener

                val allDocs = result.documents
                if (allDocs.isEmpty()) {
                    chatAdapter.addItem(ChatItem.BotMessage("정책 데이터가 비어 있어요."))
                    binding.rvChatMessages.scrollToPosition(chatAdapter.itemCount - 1)
                    return@addOnSuccessListener
                }

                // 1) 프리필터
                val tokens = extractQueryTokens(userQuery)
                val preRanked = allDocs
                    .asSequence()
                    .filter { hardAgePass(it, hints.ageBand) }
                    .filter { hardIncomePass(it, hints.incomeTier) }
                    .map { doc ->
                        val d = doc.data ?: emptyMap<String, Any?>()
                        val text = buildPolicyText(d)
                        val score = keywordScore(text, tokens)
                        Triple(doc, score, text)
                    }
                    .sortedByDescending { it.second }
                    .take(150)
                    .toList()

                // 2) LLM 평가 후보
                var docs = preRanked.map { it.first }.take(30)
                var prompt = buildLLMPrompt(userQuery, hints, docs)

                val MAX_PROMPT_CHARS = 120_000
                while (prompt.length > MAX_PROMPT_CHARS && docs.size > 10) {
                    docs = docs.take(docs.size / 2)
                    prompt = buildLLMPrompt(userQuery, hints, docs)
                }

                // 3) LLM 호출
                Thread {
                    try {
                        val arr = gemini.generateJsonArrayPrompt(prompt)
                        if (sessionId != currentSessionId) return@Thread

                        val results = mutableListOf<Triple<com.google.firebase.firestore.DocumentSnapshot, Double, String>>()
                        for (i in 0 until arr.length()) {
                            val o = arr.getJSONObject(i)
                            val id = o.optInt("id", -1)
                            if (id !in docs.indices) continue
                            val mustExclude = o.optBoolean("must_exclude", false)
                            val score = o.optDouble("score", 0.0)
                            if (mustExclude) continue
                            if (score < MIN_LLM_SCORE) continue
                            val reason = o.optString("reason", "")
                            results += Triple(docs[id], score, reason)
                        }

                        val top = results.sortedByDescending { it.second }.take(topK)

                        requireActivity().runOnUiThread {
                            if (sessionId != currentSessionId) return@runOnUiThread

                            if (top.isEmpty()) {
                                chatAdapter.addItem(
                                    ChatItem.BotMessage("조건과 맞는 정책을 찾지 못했어요. (LLM 점수 60점 이상 조건)")
                                )
                            } else {
                                top.forEach { (doc, score, reason) ->
                                    val data = doc.data ?: emptyMap<String, Any?>()
                                    val title = data["servNm"] as? String ?: "정책명 없음"
                                    val desc  = data["servDgst"] as? String ?: "설명 없음"
                                    val link  = data["servDtlLink"] as? String ?: ""
                                    val why   = if (reason.isBlank()) "AI 평가 사유 없음" else reason

                                    chatAdapter.addItem(
                                        ChatItem.PolicyMessage(
                                            title,
                                            "$desc\n\nAI 평가 이유: $why\n(LLM점수: ${"%.2f".format(score)})",
                                            link
                                        )
                                    )
                                }
                                chatAdapter.addItem(ChatItem.BotMessage("✅ 추천이 모두 끝났습니다."))
                            }
                            binding.rvChatMessages.scrollToPosition(chatAdapter.itemCount - 1)
                        }
                    } catch (e: Exception) {
                        requireActivity().runOnUiThread {
                            if (sessionId != currentSessionId) return@runOnUiThread
                            chatAdapter.addItem(ChatItem.BotMessage("AI 평가 중 오류가 발생했어요: ${e.message ?: "unknown"}"))
                            binding.rvChatMessages.scrollToPosition(chatAdapter.itemCount - 1)
                        }
                    }
                }.start()
            }
            .addOnFailureListener {
                if (sessionId != currentSessionId) return@addOnFailureListener
                chatAdapter.addItem(ChatItem.BotMessage("정책 데이터를 불러오는 중 오류가 발생했어요."))
                binding.rvChatMessages.scrollToPosition(chatAdapter.itemCount - 1)
            }
    }

    // ------------------- 프롬프트 생성 -------------------

    private fun buildLLMPrompt(
        userQuery: String,
        hints: UserHints,
        docs: List<com.google.firebase.firestore.DocumentSnapshot>
    ): String {
        val sb = StringBuilder()

        sb.appendLine("이전 대화/문맥은 모두 무시하고, 아래 사용자 입력만 기준으로 판단하라.")
        sb.appendLine("다음은 한국의 복지/지원 정책 후보 리스트이다.")
        sb.appendLine("사용자 입력(자유문): \"$userQuery\"")

        val hintLine = buildString {
            hints.age?.let { append("나이:${it}세 ") }
            hints.ageBand?.let { append("연령대:$it ") }
            hints.gender?.let { append("성별:$it ") }
            hints.region?.let { append("지역:$it ") }
            hints.incomeDecile?.let { append("소득분위:${it}분위 ") }
            hints.incomeTier?.let { append("소득구분:$it ") }
        }.trim()
        if (hintLine.isNotEmpty()) sb.appendLine("※ 사용자 힌트: $hintLine")
        sb.appendLine()

        sb.appendLine("출력은 **JSON 배열만** 허용한다. 다른 텍스트를 절대 포함하지 말라.")
        sb.appendLine("스키마: [{\"id\":number, \"score\":number(0.0~1.0), \"must_exclude\":boolean, \"reason\":string}]")
        sb.appendLine("- id는 아래 [후보 정책]의 ID와 일치.")
        sb.appendLine("- must_exclude=true이면 score는 0.00~0.05.")
        sb.appendLine("- reason: 20~80자, 근거 1~2개. 모호한 표현 금지.")
        sb.appendLine("- score < ${"%.2f".format(MIN_LLM_SCORE)} 인 항목은 **응답에서 제외**.")
        sb.appendLine()
        sb.appendLine("평가 지침:")
        sb.appendLine("1) 자격요건 불일치(연령/성별/지역/소득)는 must_exclude=true.")
        sb.appendLine("2) 확실하지 않으면 낮은 score + must_exclude=false.")
        sb.appendLine("3) 0.90~1.00=매우 적합, 0.60~0.89=부분 일치.")
        sb.appendLine("4) reason에는 근거 문구/태그를 직접 인용.")
        sb.appendLine("5) 주어진 정보 외 추론 금지.")
        sb.appendLine()

        sb.appendLine("[후보 정책]")
        val maxTitle = 80
        val maxDesc  = 300
        val maxMeta  = 140

        docs.forEachIndexed { idx, d ->
            val data = d.data ?: emptyMap<String, Any?>()
            val title = truncate((data["servNm"] as? String).orEmpty(), maxTitle)
            val desc  = truncate((data["servDgst"] as? String).orEmpty(), maxDesc)
            val meta  = truncate(
                listOf(
                    data["lifeNmArray"] as? String ?: "",
                    data["trgterIndvdlNmArray"] as? String ?: "",
                    data["intrsThemaNmArray"] as? String ?: "",
                    data["ctpvNm"] as? String ?: ""
                ).joinToString(" "),
                maxMeta
            )
            sb.appendLine("ID=$idx")
            sb.appendLine("제목: $title")
            sb.appendLine("요약: $desc")
            sb.appendLine("태그: $meta")
            sb.appendLine("---")
        }
        sb.appendLine("JSON만 출력:")
        return sb.toString()
    }

    // ------------------- 하드 프리필터 -------------------

    private fun hardAgePass(doc: com.google.firebase.firestore.DocumentSnapshot, ageBand: String?): Boolean {
        if (ageBand == null) return true
        val life = ((doc.data?.get("lifeNmArray") as? String) ?: "")
            .split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (life.isEmpty()) return true

        return when (ageBand) {
            "청년", "중장년" -> !life.any { it.contains("노인") || it.contains("아동") || it.contains("청소년") }
            "노인" -> life.any { it.contains("노인") }
            "청소년" -> life.any { it.contains("아동") || it.contains("청소년") }
            else -> true
        }
    }

    private fun hardIncomePass(doc: com.google.firebase.firestore.DocumentSnapshot, tier: String?): Boolean {
        if (tier == null) return true
        val tags = listOf(
            doc.data?.get("intrsThemaNmArray") as? String ?: "",
            doc.data?.get("trgterIndvdlNmArray") as? String ?: "",
            doc.data?.get("lifeNmArray") as? String ?: "",
            doc.data?.get("servDgst") as? String ?: ""
        ).joinToString(" ")

        val lowIncomeKeywords = listOf("저소득", "차상위", "기초생활", "중위소득", "생계급여", "의료급여", "긴급복지", "영세")
        return if (tier == "high") !lowIncomeKeywords.any { kw -> tags.contains(kw) } else true
    }

    // ------------------- 사용자 힌트 파서 -------------------

    private data class UserHints(
        val age: Int? = null,
        val ageBand: String? = null,
        val gender: String? = null,
        val region: String? = null,
        val incomeDecile: Int? = null,
        val incomeTier: String? = null
    )

    private fun parseUserHints(q: String): UserHints {
        val t = q.lowercase()

        var age: Int? = Regex("""(\d{1,3})\s*세""").find(t)?.groupValues?.getOrNull(1)?.toIntOrNull()
        if (age == null) {
            Regex("""(\d{2})\s*대""").find(t)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { decade ->
                age = when (decade) {
                    in 0..19 -> 18
                    in 20..29 -> 25
                    in 30..39 -> 35
                    in 40..49 -> 45
                    in 50..59 -> 55
                    in 60..69 -> 65
                    else -> null
                }
            }
        }
        val ageBand = when {
            age == null -> when {
                t.contains("아동") || t.contains("청소년") -> "청소년"
                t.contains("청년") -> "청년"
                t.contains("중장년") -> "중장년"
                t.contains("노인") || t.contains("어르신") -> "노인"
                else -> null
            }
            age in 0..19 -> "청소년"
            age in 20..39 -> "청년"
            age in 40..64 -> "중장년"
            else -> "노인"
        }

        val gender = when {
            t.contains("남") || t.contains("남성") || t.contains("남자") -> "남성"
            t.contains("여") || t.contains("여성") || t.contains("여자") -> "여성"
            else -> null
        }

        val possibleRegions = listOf("서울","부산","대구","인천","광주","대전","울산","세종","경기","강원","충북","충남","전북","전남","경북","경남","제주")
        val region = possibleRegions.firstOrNull { t.contains(it) }?.let {
            when (it) {
                "서울" -> "서울특별시"
                "부산" -> "부산광역시"
                "대구" -> "대구광역시"
                "인천" -> "인천광역시"
                "광주" -> "광주광역시"
                "대전" -> "대전광역시"
                "울산" -> "울산광역시"
                "세종" -> "세종특별자치시"
                "경기" -> "경기도"
                "강원" -> "강원특별자치도"
                "충북" -> "충청북도"
                "충남" -> "충청남도"
                "전북" -> "전북특별자치도"
                "전남" -> "전라남도"
                "경북" -> "경상북도"
                "경남" -> "경상남도"
                "제주" -> "제주특별자치도"
                else -> it
            }
        }

        val incomeDecile = Regex("""(\d{1,2})\s*분위""").find(t)?.groupValues?.getOrNull(1)?.toIntOrNull()
        val incomeTier = when {
            incomeDecile != null && incomeDecile >= 8 -> "high"
            incomeDecile != null && incomeDecile <= 3 -> "low"
            t.contains("고소득") || t.contains("상위") -> "high"
            t.contains("저소득") || t.contains("차상위") || t.contains("기초생활") -> "low"
            else -> null
        }

        return UserHints(age, ageBand, gender, region, incomeDecile, incomeTier)
    }

    // ------------------- 프리필터/키워드 유틸 -------------------

    /** 사용자 쿼리에서 간단 토큰 추출 */
    private fun extractQueryTokens(q: String): List<String> {
        val cleaned = q.lowercase()
        val raw = cleaned.split(Regex("""[\s,./\-()\[\]{}:"'!?]+"""))
        val stop = setOf("는","은","이","가","에","에서","에게","을","를","도","만","그리고","또는","보다","하면","좀","주세요","정책","추천")
        return raw.filter { it.isNotBlank() && it !in stop && it.length >= 1 }
    }

    /** 문서의 텍스트 결합(제목+요약+태그) — 프리필터용 */
    private fun buildPolicyText(d: Map<String, Any?>): String {
        val title = (d["servNm"] as? String).orEmpty()
        val desc  = (d["servDgst"] as? String).orEmpty()
        val tags  = listOf(
            d["lifeNmArray"] as? String ?: "",
            d["trgterIndvdlNmArray"] as? String ?: "",
            d["intrsThemaNmArray"] as? String ?: "",
            d["ctpvNm"] as? String ?: ""
        ).joinToString(" ")
        return (title + "\n" + desc + "\n" + tags).lowercase()
    }

    /** 토큰 기반 간단 점수 — 프리필터 가중치 */
    private fun keywordScore(textLower: String, tokens: List<String>): Int {
        var s = 0
        for (t in tokens) if (t.isNotBlank() && textLower.contains(t)) s += 1
        return s
    }

    private fun truncate(s: String, max: Int): String =
        if (s.length > max) s.substring(0, max) + "…" else s

    private fun hideKeyboard(anchor: View) {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(anchor.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
