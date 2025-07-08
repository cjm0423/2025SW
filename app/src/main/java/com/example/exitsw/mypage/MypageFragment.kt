package com.example.exitsw.mypage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels

import androidx.recyclerview.widget.LinearLayoutManager
import com.example.exitsw.databinding.FragmentMypageBinding

class MypageFragment : Fragment() {
    private lateinit var binding: FragmentMypageBinding
    private val viewModel: MypageViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentMypageBinding.inflate(inflater, container, false)

        val adapter = MypageAdapter { item ->
            // 아이템 클릭 시 처리
        }

        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(context)

        viewModel.productList.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }

        return binding.root
    }
}
