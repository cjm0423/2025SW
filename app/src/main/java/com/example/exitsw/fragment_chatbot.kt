package com.example.exitsw

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.exitsw.ai.GeminiClient
import com.example.exitsw.databinding.FragmentChatbotBinding
import com.google.firebase.firestore.FirebaseFirestore

class ChatbotFragment : Fragment() {

    private var _binding: FragmentChatbotBinding? = null
    private val binding get() = _binding!!

    private lateinit var chatAdapter: ChatMessageAdapter
    private lateinit var gemini: GeminiClient

    // ✅ 세션 토큰: 오래된(이전) 응답 무시용
    @Volatile private var currentSessionId: Long = 0L

    // ✅ LLM 점수 하한선: 0.60 미만은 결과에서 제거
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

        // RecyclerView
        chatAdapter = ChatMessageAdapter()
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

        // 자유입력 엔터 → 키보드 닫기
        binding.etUserText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) { hideKeyboard(binding.etUserText); true } else false
        }

        // 추천 버튼: 새 세션 시작 + AI 전용 추천
        binding.btnRecommend.setOnClickListener {
            hideKeyboard(binding.etUserText)
            val query = binding.etUserText.text?.toString()?.trim().orEmpty()

            // 세션 갱신(오래된 응답 무시)
            currentSessionId = System.currentTimeMillis()
            val sessionId = currentSessionId

            // UI 초기화
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

    /**
     * AI-전용 추천
     * - 규칙: (1) 쿼리 힌트 추출 → (2) 하드 프리필터(연령/소득) → (3) LLM 평가
     */
    private fun recommendByLLM(userQuery: String, topK: Int, sessionId: Long) {
        val db = FirebaseFirestore.getInstance()
        chatAdapter.addItem(ChatItem.BotMessage("AI가 조건을 해석하고 정책을 평가 중…"))

        // 0) 사용자 힌트 파싱
        val hints = parseUserHints(userQuery)  // age/ageBand/gender/region/incomeDecile/incomeTier

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

                // 1) 키워드 프리필터(150개) + 연령/소득 하드 컷
                val tokens = extractQueryTokens(userQuery)
                val preRanked = allDocs
                    .asSequence()
                    .filter { hardAgePass(it, hints.ageBand) }         // 🔒 20대면 ‘노인/아동’ 컷
                    .filter { hardIncomePass(it, hints.incomeTier) }   // 🔒 상위소득이면 ‘저소득’ 컷
                    .map { doc ->
                        val d = doc.data ?: emptyMap<String, Any?>()
                        val text = buildPolicyText(d)
                        val score = keywordScore(text, tokens)
                        Triple(doc, score, text)
                    }
                    .sortedByDescending { it.second }
                    .take(150)
                    .toList()

                // 2) LLM 평가 후보 30개
                var docs = preRanked.map { it.first }.take(30)

                // 3) 프롬프트(강화판) 작성: 사용자 힌트를 명시하고 ‘반드시 제외’ 규칙을 못 박는다
                var prompt = buildLLMPrompt(userQuery, hints, docs)

                // (안전) 프롬프트가 너무 길면 절반씩 줄이면서 재작성
                val MAX_PROMPT_CHARS = 120_000
                while (prompt.length > MAX_PROMPT_CHARS && docs.size > 10) {
                    docs = docs.take(docs.size / 2)
                    prompt = buildLLMPrompt(userQuery, hints, docs)
                }

                // 4) 백그라운드 LLM 호출
                Thread {
                    try {
                        val arr = gemini.generateJsonArrayPrompt(prompt)
                        if (sessionId != currentSessionId) return@Thread

                        // 5) 결과 매핑 (+ 하한선/제외 적용)
                        val results = mutableListOf<Triple<com.google.firebase.firestore.DocumentSnapshot, Double, String>>()
                        for (i in 0 until arr.length()) {
                            val o = arr.getJSONObject(i)
                            val id = o.optInt("id", -1)
                            if (id !in docs.indices) continue
                            val mustExclude = o.optBoolean("must_exclude", false)
                            val score = o.optDouble("score", 0.0)
                            if (mustExclude) continue
                            if (score < MIN_LLM_SCORE) continue      // ⬅️ 점수 하한선 0.60 적용
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

    // ------------------- 프롬프트 생성 (강화판 + ‘반드시 제외’ 명시) -------------------

    private fun buildLLMPrompt(
        userQuery: String,
        hints: UserHints,
        docs: List<com.google.firebase.firestore.DocumentSnapshot>
    ): String {
        val sb = StringBuilder()

        // 🔒 이전 대화/문맥 무시 지시
        sb.appendLine("이전 대화/문맥은 모두 무시하고, 아래 사용자 입력만 기준으로 판단하라.")
        sb.appendLine("다음은 한국의 복지/지원 정책 후보 리스트이다.")
        sb.appendLine("사용자 입력(자유문): \"$userQuery\"")

        // 사용자 힌트 요약 (명시적으로 못 박기)
        val hintLine = buildString {
            hints.age?.let { append("나이:${it}세 ") }
            hints.ageBand?.let { append("연령대:$it ") }
            hints.gender?.let { append("성별:$it ") }
            hints.region?.let { append("지역:$it ") }
            hints.incomeDecile?.let { append("소득분위:${it}분위 ") }
            hints.incomeTier?.let { append("소득구분:$it ") } // high/mid/low
        }.trim()
        if (hintLine.isNotEmpty()) sb.appendLine("※ 사용자 힌트: $hintLine")

        sb.appendLine()

        // 출력 스키마 & 검증 규칙 + 하한선 반환 규칙
        sb.appendLine("출력은 **JSON 배열만** 허용한다. 다른 텍스트를 절대 포함하지 말라.")
        sb.appendLine("스키마: [{\"id\":number, \"score\":number(0.0~1.0), \"must_exclude\":boolean, \"reason\":string}]")
        sb.appendLine("- id: 아래 [후보 정책]에 표시된 ID와 정확히 일치해야 함 (중복/누락 금지).")
        sb.appendLine("- must_exclude=true인 항목의 score는 0.00~0.05 범위로 설정.")
        sb.appendLine("- reason: 20~80자, 핵심 1~2개 근거만. 모호한 표현 금지(예: '적합해 보임').")
        sb.appendLine("- JSON 유효성: 배열 형태, 각 객체에 id/score/must_exclude/reason 모두 포함, 불필요한 키 금지.")
        sb.appendLine("반환 규칙: score가 ${"%.2f".format(MIN_LLM_SCORE)} 미만인 항목은 JSON 응답에서 **제외**하라.") // ⬅️ 0.60 명시
        sb.appendLine()

        // 평가 지침(하드 규칙 + 루브릭)
        sb.appendLine("평가 지침:")
        sb.appendLine("1) **자격요건 불일치**는 반드시 must_exclude=true:")
        sb.appendLine("   - 연령대 불일치(예: 사용자 23세/청년인데 '청소년/아동/노인' 대상).")
        sb.appendLine("   - 반대 성별 전용 정책.")
        sb.appendLine("   - 지역 불일치가 명확(예: 특정 시/도 한정인데 사용자 지역이 다른 경우).")
        sb.appendLine("   - 소득 불일치: 사용자가 상위 소득층(예: 8~10분위)이면 '저소득층/차상위/기초생활' 등 저소득 대상 정책은 must_exclude=true.")
        sb.appendLine("   - 그 밖에 명시적 신청 자격 미충족.")
        sb.appendLine("2) 확실하지 않다면 보수적으로 낮은 score를 주고 must_exclude=false로 남긴다.")
        sb.appendLine("3) 채점 루브릭(예시): 0.90~1.00=매우 적합, 0.60~0.89=부분 일치, 0.30~0.59=약한 관련.")
        sb.appendLine("4) reason은 **근거가 된 문구/태그를 직접 언급**(예: '대상: 청년, 지역: 서울특별시, 소득: 저소득 제외').")
        sb.appendLine("5) 어떤 항목도 환각으로 생성하지 말고, 주어진 정보 외 추론은 금지.")
        sb.appendLine()

        // 후보 나열 (길이 제한 적용)
        sb.appendLine("[후보 정책]")
        val MAX_TITLE = 80
        val MAX_DESC  = 300
        val MAX_META  = 140

        docs.forEachIndexed { idx, d ->
            val data = d.data ?: emptyMap<String, Any?>()
            val title = truncate((data["servNm"] as? String).orEmpty(), MAX_TITLE)
            val desc  = truncate((data["servDgst"] as? String).orEmpty(), MAX_DESC)
            val meta  = truncate(
                listOf(
                    data["lifeNmArray"] as? String ?: "",
                    data["trgterIndvdlNmArray"] as? String ?: "",
                    data["intrsThemaNmArray"] as? String ?: "",
                    data["ctpvNm"] as? String ?: ""
                ).joinToString(" "),
                MAX_META
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

    /** 연령 하드 컷: 사용자 연령대와 명백히 불일치하면 제외 */
    private fun hardAgePass(doc: com.google.firebase.firestore.DocumentSnapshot, ageBand: String?): Boolean {
        if (ageBand == null) return true
        val life = ((doc.data?.get("lifeNmArray") as? String) ?: "")
            .split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (life.isEmpty()) return true

        return when (ageBand) {
            "청년" -> !life.any { it.contains("노인") || it.contains("아동") || it.contains("청소년") }
            "중장년" -> !life.any { it.contains("노인") || it.contains("아동") || it.contains("청소년") }
            "노인" -> life.any { it.contains("노인") }
            "청소년" -> life.any { it.contains("아동") || it.contains("청소년") }
            else -> true
        }
        // 필요시 23세 → '청소년' 정책 컷 보장
    }

    /** 소득 하드 컷: 상위소득(high)이면 저소득 키워드 포함 정책은 컷 */
    private fun hardIncomePass(doc: com.google.firebase.firestore.DocumentSnapshot, tier: String?): Boolean {
        if (tier == null) return true
        val tags = listOf(
            doc.data?.get("intrsThemaNmArray") as? String ?: "",
            doc.data?.get("trgterIndvdlNmArray") as? String ?: "",
            doc.data?.get("lifeNmArray") as? String ?: "",
            doc.data?.get("servDgst") as? String ?: ""
        ).joinToString(" ")

        val lowIncomeKeywords = listOf(
            "저소득", "차상위", "기초생활", "중위소득", "생계급여", "의료급여", "긴급복지", "영세"
        )

        return when (tier) {
            "high" -> !lowIncomeKeywords.any { kw -> tags.contains(kw) } // 상위 소득 → 저소득 대상 컷
            else -> true
        }
    }

    // ------------------- 사용자 힌트 파서 -------------------

    private data class UserHints(
        val age: Int? = null,
        val ageBand: String? = null,   // 청소년/청년/중장년/노인
        val gender: String? = null,    // 남성/여성
        val region: String? = null,
        val incomeDecile: Int? = null, // 1~10
        val incomeTier: String? = null // high/mid/low
    )

    private fun parseUserHints(q: String): UserHints {
        val t = q.lowercase()

        // 나이
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

        // 성별
        val gender = when {
            t.contains("남") || t.contains("남성") || t.contains("남자") -> "남성"
            t.contains("여") || t.contains("여성") || t.contains("여자") -> "여성"
            else -> null
        }

        // 대략적 지역(간단 추출)
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

        // 소득 분위
        val incomeDecile = Regex("""(\d{1,2})\s*분위""").find(t)?.groupValues?.getOrNull(1)?.toIntOrNull()
        // 소득 구분
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

    /** 사용자 쿼리에서 간단 토큰 추출 (한글/숫자 위주) */
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
        for (t in tokens) {
            if (t.isBlank()) continue
            if (textLower.contains(t)) s += 1
        }
        return s
    }

    /** 쿼리에서 연령대 힌트(청소년/청년/중장년/노인) 추정 — 프롬프트 메모용 */
    private fun detectAgeBandFromQuery(q: String): String? {
        val t = q.lowercase()
        Regex("""(\d{1,3})\s*세""").find(t)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { age ->
            return when (age) { in 0..19 -> "청소년"; in 20..39 -> "청년"; in 40..64 -> "중장년"; else -> "노인" }
        }
        Regex("""(\d{2})\s*대""").find(t)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { d ->
            return when (d) { in 0..19 -> "청소년"; in 20..39 -> "청년"; in 40..64 -> "중장년"; else -> "노인" }
        }
        return when {
            t.contains("아동") || t.contains("청소년") -> "청소년"
            t.contains("청년") -> "청년"
            t.contains("중장년") -> "중장년"
            t.contains("노인") || t.contains("어르신") -> "노인"
            else -> null
        }
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
