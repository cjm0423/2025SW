package com.example.exitsw.mypage

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.exitsw.EditProfileActivity
import com.example.exitsw.R
import com.example.exitsw.data.LocalWelfareServiceDto
import com.example.exitsw.databinding.FragmentMypageBinding
import com.example.exitsw.main.PolicyDetailFragment
import com.google.firebase.auth.FirebaseAuth

class MypageFragment : Fragment() {

    private var _binding: FragmentMypageBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MypageViewModel by viewModels()
    private lateinit var adapter: FavoriteAdapter

    private val editProfileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.startObserveUser()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMypageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 관심 목록 RecyclerView
        adapter = FavoriteAdapter { dto: LocalWelfareServiceDto ->
            val fragment = PolicyDetailFragment.newInstance(dto) // ✅ dto 통째로 전달
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment) // 실제 컨테이너 id 확인 필요
                .addToBackStack(null)
                .commit()
        }

        binding.rvFavorites.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            setHasFixedSize(false)
            isNestedScrollingEnabled = false
            adapter = this@MypageFragment.adapter
        }

        val divider = DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL)
        divider.setDrawable(
            ContextCompat.getDrawable(requireContext(), R.drawable.divider_gray)!!
        )
        binding.rvFavorites.addItemDecoration(divider)

        // LiveData 구독
        viewModel.favoriteList.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
        }

        viewModel.userProfile.observe(viewLifecycleOwner) { profile ->
            if (profile != null) {
                binding.tvNickname.text = profile.nickname
                binding.tvDistrict.text = profile.district ?: "· 지역구 정보 없음"
            } else {
                binding.tvNickname.text = "로그인이 필요합니다"
                binding.tvDistrict.text = ""
            }
        }

        // 프로필 카드 → 편집
        binding.cardProfile.setOnClickListener {
            val intent = Intent(requireContext(), EditProfileActivity::class.java)
            editProfileLauncher.launch(intent)
        }

        // 로그아웃
        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            Toast.makeText(requireContext(), "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()

            // LoginActivity로 이동
            val intent = Intent(requireContext(), com.example.exitsw.LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.startObserveUser()
        viewModel.startObserveFavorites()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
