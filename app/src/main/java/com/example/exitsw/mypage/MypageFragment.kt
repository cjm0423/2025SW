package com.example.exitsw.mypage

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.exitsw.EditProfileActivity
import com.example.exitsw.LoginActivity
import com.example.exitsw.R
import com.example.exitsw.data.LocalWelfareServiceDto
import com.example.exitsw.databinding.FragmentMypageBinding
import com.example.exitsw.main.PolicyDetailFragment // 패키지 경로 맞게 조정
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
            val updated = result.data?.getBooleanExtra("updated", false) ?: false
            if (updated) {
                Toast.makeText(requireContext(), "프로필이 갱신되었어요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMypageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = FavoriteAdapter { item ->
            val dto = LocalWelfareServiceDto(
                servId      = item.id,
                servNm      = item.title,
                bizChrDeptNm= item.department,
                servDgst    = item.summary,
                ctpvNm      = item.region,
                sggNm       = item.city,
                servDtlLink = item.detailLink
            )
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, PolicyDetailFragment.newInstance(dto))
                .addToBackStack(null)
                .commit()
        }


        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        binding.btnFavorite.setOnClickListener {
            Log.d("MypageFragment", "btn_favorite clicked → observeFavorites()")
            viewModel.startObserveFavorites()
        }

        viewModel.favoriteList.observe(viewLifecycleOwner) { list ->
            adapter.submitList(
                list.map { dto ->
                    FavoritePolicy(
                        id         = dto.servId.orEmpty(),
                        title      = dto.servNm.orEmpty(),
                        department = dto.bizChrDeptNm,
                        summary    = dto.servDgst,
                        region     = dto.ctpvNm,
                        city       = dto.sggNm,
                        detailLink = dto.servDtlLink
                    )
                }
            )
        }

        // 프로필 수정
        binding.btnEditProfile.setOnClickListener {
            editProfileLauncher.launch(
                android.content.Intent(requireContext(), EditProfileActivity::class.java)
            )
        }

        // 로그아웃
        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            Toast.makeText(requireContext(), "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()
            startActivity(
                android.content.Intent(requireContext(), LoginActivity::class.java).apply {
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                            android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
