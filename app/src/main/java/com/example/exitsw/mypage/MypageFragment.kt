package com.example.exitsw.mypage

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.exitsw.LoginActivity
import com.example.exitsw.databinding.FragmentMypageBinding
import com.google.firebase.auth.FirebaseAuth

class MypageFragment : Fragment() {
    private lateinit var binding: FragmentMypageBinding
    private val viewModel: MypageViewModel by viewModels()

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

        // 🔒 로그아웃
        binding.btnLogout.setOnClickListener {
            // Firebase Auth 로그아웃
            FirebaseAuth.getInstance().signOut()

            Toast.makeText(requireContext(), "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()

            // 로그인 화면으로 이동 + 백스택 초기화
            val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
        }
    }
}
