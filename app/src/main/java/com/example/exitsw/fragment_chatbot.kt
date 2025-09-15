package com.example.exitsw

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.exitsw.databinding.FragmentChatbotBinding
import com.google.firebase.firestore.FirebaseFirestore

class ChatbotFragment : Fragment() {

    private var _binding: FragmentChatbotBinding? = null
    private val binding get() = _binding!!

    private lateinit var chatAdapter: ChatMessageAdapter

    // ✅ 지역 매핑 테이블
    private val regionMapping = mapOf(
        "서울시" to listOf("서울특별시"),
        "경기도" to listOf("경기도"),
        "강원도" to listOf("강원특별자치도", "강원도"),
        "충청북도" to listOf("충청북도"),
        "충청남도" to listOf("충청남도"),
        "전라북도" to listOf("전라북도", "전북특별자치도"),
        "전라남도" to listOf("전라남도"),
        "경상북도" to listOf("경상북도", "대구광역시"),
        "경상남도" to listOf("경상남도", "부산광역시", "울산광역시"),
        "제주도" to listOf("제주특별자치도", "제주도"),
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatbotBinding.inflate(inflater, container, false)

        // RecyclerView 초기화
        chatAdapter = ChatMessageAdapter()
        binding.rvChatMessages.layoutManager = LinearLayoutManager(requireContext())
        binding.rvChatMessages.adapter = chatAdapter

        // 성별 Spinner 설정
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.gender_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spGender.adapter = adapter
        }

        // 지역 Spinner 설정
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.kor_regions,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spRegion.adapter = adapter
        }

        // 소득분위 Spinner 설정
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.income_brackets,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spIncome.adapter = adapter
        }

        // 정책 추천 버튼 클릭 이벤트
        binding.btnRecommend.setOnClickListener {
            val gender = binding.spGender.selectedItem.toString()
            val age = binding.etAge.text.toString().toIntOrNull() ?: -1
            val region = binding.spRegion.selectedItem.toString()
            val income = binding.spIncome.selectedItem.toString()

            if (age < 0 || region.isBlank() || income.isBlank()) {
                chatAdapter.addItem(ChatItem.BotMessage("입력값을 올바르게 입력해주세요."))
            } else {
                fetchPoliciesFromFirestore(gender, age, region, income)
            }
        }

        return binding.root
    }

    /**
     * Firestore에서 정책 가져오기
     */
    private fun fetchPoliciesFromFirestore(
        gender: String,
        age: Int,
        region: String,
        income: String
    ) {
        val db = FirebaseFirestore.getInstance()

        db.collection("policies")
            .document("all")
            .collection("items")
            .get()
            .addOnSuccessListener { result ->
                var found = false

                for (doc in result) {
                    val data = doc.data

                    val lifeNmArray = (data["lifeNmArray"] as? String)?.split(",") ?: emptyList()
                    val regionName = data["ctpvNm"] as? String ?: ""
                    val targetArray = (data["trgterIndvdlNmArray"] as? String)?.split(",") ?: emptyList()
                    val incomeArray = (data["intrsThemaNmArray"] as? String)?.split(",") ?: emptyList()

                    // 1) 나이 매칭
                    val ageMatch = when {
                        age < 20 -> lifeNmArray.any { it.contains("청소년") || it.contains("아동") }
                        age in 20..39 -> lifeNmArray.any { it.contains("청년") }
                        age in 40..64 -> lifeNmArray.any { it.contains("중장년") }
                        age >= 65 -> lifeNmArray.any { it.contains("노인") }
                        else -> true
                    }

                    // 2) 지역 매칭 (매핑 적용)
                    val regionCandidates = regionMapping[region] ?: listOf(region)
                    val regionMatch = regionCandidates.any { regionName.contains(it) }

                    // 3) 성별 매칭
                    val genderMatch = if (targetArray.isNotEmpty()) {
                        targetArray.any { it.contains(gender) }
                    } else true

                    // 4) 소득 매칭
                    val incomeMatch = if (incomeArray.isNotEmpty()) {
                        incomeArray.any { it.contains(income) }
                    } else true

                    if (ageMatch && regionMatch && genderMatch && incomeMatch) {
                        val title = data["servNm"] as? String ?: "정책명 없음"
                        val desc = data["servDgst"] as? String ?: "설명 없음"
                        val link = data["servDtlLink"] as? String ?: ""

                        chatAdapter.addItem(ChatItem.PolicyMessage(title, desc, link))
                        found = true
                    }
                }

                if (!found) {
                    chatAdapter.addItem(ChatItem.BotMessage("조건에 맞는 정책을 찾지 못했어요."))
                } else {
                    chatAdapter.addItem(ChatItem.BotMessage("✅ 추천이 모두 끝났습니다."))
                }

                binding.rvChatMessages.scrollToPosition(chatAdapter.itemCount - 1)
            }
            .addOnFailureListener {
                chatAdapter.addItem(ChatItem.BotMessage("정책 데이터를 불러오는 중 오류가 발생했어요."))
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
