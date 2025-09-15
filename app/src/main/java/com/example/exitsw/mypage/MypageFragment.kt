package com.example.exitsw.mypage

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.exitsw.EditProfileActivity
import com.example.exitsw.LoginActivity
import com.example.exitsw.databinding.FragmentMypageBinding
import com.google.firebase.auth.FirebaseAuth

class MypageFragment : Fragment() {
    private lateinit var binding: FragmentMypageBinding
    private val viewModel: MypageViewModel by viewModels()

    // 프로필 수정 결과 받기 (RESULT_OK면 갱신 토스트)
    private val editProfileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val updated = result.data?.getBooleanExtra("updated", false) ?: false
            if (updated) {
                Toast.makeText(requireContext(), "프로필이 갱신되었어요.", Toast.LENGTH_SHORT).show()
                // TODO: 필요 시 여기서 프로필 재조회/UI 갱신 호출
                // 예: viewModel.reloadProfile()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMypageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 기존 상품 목록 데이터 관찰
        viewModel.productList.observe(viewLifecycleOwner) { productList ->
            Log.d("MypageFragment", "성공: ${productList.size}개의 상품 데이터를 ViewModel로부터 받았습니다.")
            // TODO: 기존 상품 목록 처리 로직
        }

        // ✅ 프로필 수정 버튼 → EditProfileActivity 실행
        binding.btnEditProfile.setOnClickListener {
            val intent = Intent(requireContext(), EditProfileActivity::class.java)
            editProfileLauncher.launch(intent)
        }

        // 🔒 로그아웃
        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            Toast.makeText(requireContext(), "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()
            val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
        }
    }
}
