package com.example.exitsw.main

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.exitsw.R
import com.example.exitsw.data.LocalWelfareServiceDto
import com.example.exitsw.databinding.FragmentPolicyDetailBinding
import com.example.exitsw.repository.FirebaseRepository
import com.example.exitsw.util.PolicyIconMapper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.Locale

class PolicyDetailFragment : Fragment() {

    private var _binding: FragmentPolicyDetailBinding? = null
    private val binding get() = _binding!!

    private var policy: LocalWelfareServiceDto? = null

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private val repo = FirebaseRepository()

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
            ?: throw IllegalStateException("로그인 필요")

        // DTO가 정상 전달되었는지 확인
        val p = policy
        if (p == null) {
            // 안전장치: id만 넘어오는 케이스 대비 폴백을 넣고 싶다면 여기서 구현(B안)
            // 지금은 A안이므로 간단히 리턴/토스트 정도
            Toast.makeText(requireContext(), "정책 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // 전달된 DTO로 바로 UI 바인딩
        val iconResId = PolicyIconMapper.getIconResourceId(p)
        binding.imgPolicy.setImageResource(iconResId)
        binding.toolbar.title = p.servNm
        binding.textPolicyName.text = p.servNm
        binding.textPolicyAgency.text = p.bizChrDeptNm
        binding.textPolicySummary.text = p.servDgst

        // 좋아요 카운트/상태 리스너 설정(네 기존 코드 재사용)
        val docId = resolveDocIdSameAsSync(p)
        itemRef = db.collection("policies").document("all")
            .collection("items").document(docId)
        likeRef = itemRef!!.collection("likes").document(uid)

        countListener = itemRef!!.addSnapshotListener { snap, _ ->
            val count = snap?.getLong("favoritesCount") ?: 0L
            binding.textFavoriteNum.text = count.toString()
        }
        likeListener = likeRef!!.addSnapshotListener { snap, _ ->
            val liked = snap?.exists() == true
            binding.btnFavorite.setImageResource(
                if (liked) R.drawable.ic_favorite_check else R.drawable.ic_favorite_plus
            )
        }

        binding.btnFavorite.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                // user/{uid}/favorites 토글 (repo 버전 또는 프래그먼트 내 구현 중 택1)
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("user").document(uid)
                    .collection("favorites").document(p.servId ?: return@launch)
                    .get().await()
                    .let { doc ->
                        if (doc.exists()) {
                            doc.reference.delete().await()
                            itemRef?.update("favoritesCount",
                                com.google.firebase.firestore.FieldValue.increment(-1))
                        } else {
                            doc.reference.set(p).await()
                            itemRef?.update("favoritesCount",
                                com.google.firebase.firestore.FieldValue.increment(1))
                        }
                    }
            }
        }

        binding.btnGoToSite.setOnClickListener {
            p.servDtlLink?.takeIf { it.isNotBlank() }?.let { url ->
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        }

        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countListener?.remove()
        likeListener?.remove()
        _binding = null
    }

    /**
     * user/{uid}/favorites 서브컬렉션에 정책 추가/삭제
     * 동시에 policies/all/items/{docId}.favoritesCount 카운트 업데이트
     */
    private suspend fun toggleFavorite(uid: String, dto: LocalWelfareServiceDto) {
        val favRef = db.collection("user").document(uid)
            .collection("favorites").document(dto.servId ?: return)

        val snap = favRef.get().await()
        if (snap.exists()) {
            favRef.delete().await()
            itemRef?.update("favoritesCount", FieldValue.increment(-1))
        } else {
            favRef.set(dto).await()
            itemRef?.update("favoritesCount", FieldValue.increment(1))
        }
    }

    companion object {
        fun newInstance(policy: LocalWelfareServiceDto) =
            PolicyDetailFragment().apply {
                arguments = Bundle().apply {
                    putParcelable("policy", policy)
                }
            }

        private fun resolveDocIdSameAsSync(dto: LocalWelfareServiceDto): String {
            val gson = Gson()
            val json = gson.toJson(dto)
            val type = object : TypeToken<Map<String, Any?>>() {}.type
            val map: Map<String, Any?> = gson.fromJson(json, type)

            val candidate = listOf("servId", "svcId", "id", "service_id", "no")
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
