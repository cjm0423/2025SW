package com.example.exitsw.mypage

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.exitsw.databinding.FragmentMypageBinding

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

        // 기존 상품 목록 데이터만 관찰합니다.
        viewModel.productList.observe(viewLifecycleOwner) { productList ->
            Log.d("MypageFragment", "성공: ${productList.size}개의 상품 데이터를 ViewModel로부터 받았습니다.")
            // TODO: 기존 상품 목록을 처리하는 로직
        }
    }
}
