package com.example.exitsw.main

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.exitsw.LoginActivity
import com.example.exitsw.R
import com.example.exitsw.SignupActivity
import com.example.exitsw.data.LocalWelfareServiceDto
import com.example.exitsw.databinding.FragmentPolicyDetailBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.security.MessageDigest
import java.util.Locale

class PolicyDetailFragment : Fragment() {

    private var _binding: FragmentPolicyDetailBinding? = null
    private val binding get() = _binding!!

    private var policy: LocalWelfareServiceDto? = null

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    // Firestore refs
    private var itemRef: DocumentReference? = null
    private var likeRef: DocumentReference? = null
    private var countListener: ListenerRegistration? = null
    private var likeListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            policy = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getParcelable("policy", LocalWelfareServiceDto::class.java)
            } else {
                @Suppress("DEPRECATION")
                it.getParcelable("policy")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPolicyDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be logged in before entering PolicyDetailFragment")

        // (선택) 프로필 없으면 가입으로 라우팅하고 싶다면 주석 해제
        // db.collection("user").document(uid).get().addOnSuccessListener { if (!it.exists()) {
        //     startActivity(Intent(requireContext(), SignupActivity::class.java).apply {
        //         putExtra(LoginActivity.EXTRA_UID, uid)
        //     })
        //     requireActivity().finish()
        // } }

        policy?.let { p ->
            // UI 바인딩
            binding.toolbar.title = p.serviceName
            binding.textPolicyName.text = p.serviceName
            binding.textPolicyAgency.text = p.department
            binding.textPolicySummary.text = p.summary

            // ★ docId를 WelfareApiToFirebase와 동일 규칙으로 생성
            val docId = resolveDocIdSameAsSync(p)
            require(docId.isNotBlank()) { "policy docId is blank" }

            // ★ 경로 일치: policies/all/items/{docId}
            itemRef = db.collection("policies").document("all")
                .collection("items").document(docId)
            likeRef = itemRef!!.collection("likes").document(uid)

            // 실시간 카운트
            countListener = itemRef!!.addSnapshotListener { snap, _ ->
                val count = snap?.getLong("favoritesCount") ?: 0L
                binding.textFavoriteNum.text = count.toString()
            }

            // 내 좋아요 상태
            likeListener = likeRef!!.addSnapshotListener { snap, _ ->
                val liked = snap?.exists() == true
                binding.btnFavorite.setImageResource(
                    if (liked) R.drawable.ic_favorite_check else R.drawable.ic_favorite_plus
                )
            }

            // 좋아요 토글
            binding.btnFavorite.setOnClickListener { toggleFavorite() }

            // "신청하러 가기"
            binding.btnGoToSite.setOnClickListener {
                p.detailLink?.let { url ->
                    if (url.isNotBlank()) {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                }
            }
        }

        // 뒤로가기
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun toggleFavorite() {
        val itemRef = requireNotNull(itemRef)
        val likeRef = requireNotNull(likeRef)

        db.runTransaction { tx ->
            val likeSnap = tx.get(likeRef)
            val liked = likeSnap.exists()
            if (liked) {
                tx.delete(likeRef)
                tx.update(itemRef, "favoritesCount", FieldValue.increment(-1))
            } else {
                tx.set(likeRef, mapOf("createdAt" to FieldValue.serverTimestamp()))
                tx.update(itemRef, "favoritesCount", FieldValue.increment(1))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countListener?.remove()
        likeListener?.remove()
        _binding = null
    }

    companion object {
        fun newInstance(policy: LocalWelfareServiceDto) =
            PolicyDetailFragment().apply {
                arguments = Bundle().apply {
                    putParcelable("policy", policy)
                }
            }

        // ---------- helpers: sync와 동일한 docId 규칙 ----------
        private fun resolveDocIdSameAsSync(dto: LocalWelfareServiceDto): String {
            // DTO를 Map으로 변환해서 키 우선순위 조회
            val gson = Gson()
            val json = gson.toJson(dto)
            val type = object : TypeToken<Map<String, Any?>>() {}.type
            val map: Map<String, Any?> = gson.fromJson(json, type)

            val candidate = listOf("svcId", "id", "serviceId", "service_id", "no")
                .firstNotNullOfOrNull { k -> map[k]?.toString()?.takeIf { it.isNotBlank() } }

            return candidate ?: sha1(json)
        }

        private fun sha1(input: String): String {
            val md = MessageDigest.getInstance("SHA-1")
            val bytes = md.digest(input.toByteArray())
            return bytes.joinToString("") { "%02x".format(it) }.lowercase(Locale.US)
        }
    }
}
